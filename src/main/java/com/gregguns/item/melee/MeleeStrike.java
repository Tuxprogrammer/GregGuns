package com.gregguns.item.melee;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;

/**
 * A player's melee hit with a GregGuns tool, dealt through {@link DamageSources#strike} as {@link DamageType#PHYSICAL}
 * so armor and resistances use the GregGuns model. It stands in for vanilla's hit: the tool's
 * {@code onLeftClickEntity} calls it and returns true, which stops vanilla from dealing the damage a second time and
 * keeps GregGuns from ever cancelling an attack event. Server side.
 */
// Spec: docs/techguns-spec/06-items/special-items.md (Combat Knife: damage-system hook);
// docs/gregguns-spec/02-guns/damage-interop.md
// Deviation: Techguns swaps the source inside LivingAttackEvent; GregGuns builds the source here instead.
public final class MeleeStrike {

    private MeleeStrike() {}

    /**
     * Hits a target with the held tool: damage from the attack attribute (so Strength and Weakness count), enchantment
     * bonus, critical, knockback, durability. Does nothing the target refuses ({@code canAttackWithItem},
     * {@code hitByEntity}).
     *
     * @param stack  the held tool
     * @param player the attacker
     * @param target the entity hit
     * @return true when the target took damage
     */
    public static boolean attack(ItemStack stack, EntityPlayer player, Entity target) {
        if (!target.canAttackWithItem() || target.hitByEntity(player)) {
            return false;
        }
        float attribute = (float) player.getEntityAttribute(SharedMonsterAttributes.attackDamage)
            .getAttributeValue();
        float enchant = target instanceof EntityLivingBase
            ? EnchantmentHelper.getEnchantmentModifierLiving(player, (EntityLivingBase) target)
            : 0.0F;
        int knock = target instanceof EntityLivingBase ? MeleeMath
            .knockback(EnchantmentHelper.getKnockbackModifier(player, (EntityLivingBase) target), player.isSprinting())
            : 0;
        if (attribute <= 0.0F && enchant <= 0.0F) {
            return false;
        }
        boolean critical = player.fallDistance > 0.0F && !player.onGround
            && !player.isOnLadder()
            && !player.isInWater()
            && !player.isPotionActive(Potion.blindness)
            && player.ridingEntity == null
            && target instanceof EntityLivingBase;
        float amount = MeleeMath.damage(attribute, enchant, critical);
        int fire = EnchantmentHelper.getFireAspectModifier(player);
        boolean wasBurning = target.isBurning();
        if (fire > 0 && !wasBurning) {
            target.setFire(1);
        }
        boolean taken = DamageSources.strike(target, DamageType.PHYSICAL, player, amount, 0.0);
        if (!taken) {
            if (fire > 0 && !wasBurning) {
                target.extinguish();
            }
            return false;
        }
        if (knock > 0) {
            target.addVelocity(
                -MathHelper.sin(player.rotationYaw * (float) Math.PI / 180.0F) * knock * 0.5F,
                0.1,
                MathHelper.cos(player.rotationYaw * (float) Math.PI / 180.0F) * knock * 0.5F);
            player.motionX *= 0.6;
            player.motionZ *= 0.6;
            player.setSprinting(false);
        }
        if (critical) {
            player.onCriticalHit(target);
        }
        if (enchant > 0.0F) {
            player.onEnchantmentCritical(target);
        }
        if (fire > 0) {
            target.setFire(fire * 4);
        }
        if (target instanceof EntityLivingBase) {
            stack.hitEntity((EntityLivingBase) target, player);
            if (stack.stackSize <= 0) {
                player.destroyCurrentEquippedItem();
            }
        }
        player.addExhaustion(0.3F);
        return true;
    }
}
