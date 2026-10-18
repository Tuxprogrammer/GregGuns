package com.gregguns.combat;

import java.util.function.DoubleUnaryOperator;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;

/**
 * The Minecraft half of the damage model: gathers the target's armour and penetration resistance, runs
 * {@link DamageModel#resolve}, and settles armour so vanilla does not apply it a second time.
 *
 * <p>
 * Why armour is applied here and not by vanilla: Forge 1.7.10 fires {@code LivingHurtEvent} from
 * {@code damageEntity} <em>before</em> armour, and vanilla then runs {@code EntityPlayer}'s
 * {@code ArmorProperties.ApplyArmor} (players) or {@code applyArmorCalculations} (everything else) on the amount the
 * event leaves. Armour must only touch the non-penetrating share, so the handler applies armour to that share itself,
 * then flags the source as unblockable ({@link HitProperties#isArmourSettled()}): vanilla's armour step is skipped
 * for {@code isUnblockable()} sources, and Forge's {@code ApplyArmor} skips vanilla {@code ItemArmor} and (by
 * contract, as in GT's armour) {@code ISpecialArmor} pieces for them too. Armour is applied exactly once per hit.
 * Server side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (GGS-02-20); docs/design/0009 decision 3
// Source (Forge 10.13.4.1614, patches EntityLivingBase / EntityPlayer .java.patch): damageEntity calls
// ForgeHooks.onLivingHurt first; then EntityPlayer calls ArmorProperties.ApplyArmor, other entities call
// applyArmorCalculations, which skips everything when source.isUnblockable().
final class HitResolver {

    private HitResolver() {}

    /**
     * Computes the final damage for a hit, wearing armour as a side effect (players), and settles the source.
     *
     * @param target the entity being hurt
     * @param source the GregGuns source
     * @param amount the raw damage the event carries, before armour
     * @return the damage that should reach health before potions and absorption
     */
    static float resolve(EntityLivingBase target, GunDamageSource source, float amount) {
        HitProperties hit = source.hit();
        DamageType type = hit.type();
        DamageSource vanilla = source.asDamageSource();
        // Reset first: a source reused for a second hit must let our own ApplyArmor call see a blockable source.
        hit.setArmourSettled(false);

        double pen = DamageModel
            .effectivePenetration(hit.penetration(), hit.penetrationScale(), penetrationResistance(target, type));
        DoubleUnaryOperator armour = armourFor(target, type, vanilla);
        float result = (float) DamageModel.resolve(type, amount, pen, armour);
        hit.setArmourSettled(true);
        return result;
    }

    private static DoubleUnaryOperator armourFor(EntityLivingBase target, DamageType type, DamageSource vanilla) {
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            // Same call vanilla makes for players: Forge applies each piece's ISpecialArmor properties, or vanilla's
            // ratio, and wears the pieces. Mod armour that honours isUnblockable behaves as it would for any hit.
            return share -> ArmorProperties.ApplyArmor(player, player.inventory.armorInventory, vanilla, share);
        }
        double points = target instanceof TypedProtection ? ((TypedProtection) target).armourValue(type)
            : target.getTotalArmorValue();
        double factor = DamageModel.vanillaArmourFactor(points);
        return share -> share * factor;
    }

    private static double penetrationResistance(EntityLivingBase target, DamageType type) {
        double total = 0.0;
        if (target instanceof TypedProtection) {
            total += ((TypedProtection) target).penetrationResistance(type);
        }
        // Worn GregGuns armour, summed once for the whole set (GUN-39/ARM-02: never compounded per piece).
        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = target.getEquipmentInSlot(slot);
            if (stack != null && stack.getItem() instanceof TypedArmour) {
                total += ((TypedArmour) stack.getItem()).penetrationResistance(stack, type);
            }
        }
        return total;
    }
}
