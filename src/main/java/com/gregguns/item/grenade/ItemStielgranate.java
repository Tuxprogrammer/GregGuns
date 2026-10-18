package com.gregguns.item.grenade;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.combat.DamageType;
import com.gregguns.projectile.GrenadeMath;
import com.gregguns.projectile.GrenadeProjectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Stielgranate (stick grenade). Hold the use button to wind up, release to throw: the longer the hold the faster
 * and flatter the throw (full at 20 ticks). The thrown {@link GrenadeProjectile} bounces three times and explodes
 * after 200 ticks, on touching an entity, or on a fourth hard hit.
 *
 * <p>
 * Deviation: the throw consumes one grenade from the stack being held, not the first matching stack in the inventory
 * (GUN-30), and a short wind-up cannot divide gravity by zero (GUN-22, see {@link GrenadeMath#gravity}). The use
 * duration is the 72000 ticks the vanilla bow uses (GUN-23: the 144000 Techguns stored was ignored).
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Stielgranate); docs/design/0001-tier-map.md
public final class ItemStielgranate extends Item implements Tiered {

    /** Damage at the centre of the blast, half hearts. */
    public static final float DAMAGE = 20.0F;
    /** Blast radius, blocks. */
    public static final float RADIUS = 5.0F;
    /** Stack size. */
    public static final int STACK_SIZE = 16;
    /** Use duration, ticks. */
    public static final int USE_DURATION = 72000;

    /** Creates the item. */
    public ItemStielgranate() {
        setMaxStackSize(STACK_SIZE);
        setNoRepair();
    }

    @Override
    public Tier getTier() {
        return Tier.LV;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.bow;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        int held = getMaxItemUseDuration(stack) - timeLeft;
        double charge = GrenadeMath.charge(held);
        player.swingItem();
        if (world.isRemote) {
            return;
        }
        GrenadeProjectile grenade = new GrenadeProjectile(world);
        grenade.throwFrom(player, charge, DAMAGE, RADIUS, GrenadeMath.STIELGRANATE_BOUNCES, "stielgranate");
        // Source: Techguns plays vanilla random.bow at this pitch (sheet section 8).
        world.playSoundAtEntity(
            player,
            "random.bow",
            1.0F,
            1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + 0.5F * (float) charge);
        world.spawnEntityInWorld(grenade);
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
            if (stack.stackSize <= 0) {
                player.destroyCurrentEquippedItem();
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("gregguns.gun.tooltip.damage", String.format(Locale.ROOT, "%.1f", DAMAGE)));
        if (!GuiScreen.isShiftKeyDown()) {
            tooltip.add(StatCollector.translateToLocal("gregguns.gun.tooltip.shift"));
            return;
        }
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.gun.tooltip.damage_type",
                StatCollector.translateToLocal(DamageType.EXPLOSION.nameLangKey())));
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.grenade.tooltip.radius",
                String.format(Locale.ROOT, "%.1f", RADIUS)));
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("gregguns.grenade.tooltip.bounces", GrenadeMath.STIELGRANATE_BOUNCES));
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.grenade.tooltip.fuse",
                String.format(Locale.ROOT, "%.0f", GrenadeMath.FUSE_TICKS / 20.0)));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gregguns.grenade.tooltip.hold"));
    }
}
