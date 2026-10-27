package com.gregguns.explosion.nuclear;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;
import gregtech.api.util.GTUtility;

/**
 * What irradiated earth drops. Radioactive from the item itself: while it is in a player's inventory it calls GT's
 * {@code GTUtility.applyRadioactivity} on the same 120-tick cadence and with the same stack-size scaling as GT's own
 * carried-item check, and GT skips anyone in full radiological hazmat. It deliberately carries <b>no</b> GT item data:
 * data naming uranium-235 would make GT generate recycling recipes (macerator, arc furnace) that turn free rubble into
 * U-235 dust. The centrifuge recipes that process it are {@link FalloutRecipes}; damage 1 is hot rubble. Borrows the
 * vanilla flint icon, tinted green.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
// Source: GTProxy.applyItemEffects, the carried-item check this mirrors
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTProxy.java#L1286-L1311
public final class ItemIrradiatedRubble extends Item implements com.gregguns.api.Tiered {

    /** Ticks between applications (GT's carried-item check runs every 120 ticks). */
    public static final int PERIOD_TICKS = 120;

    /**
     * @return the GT radioactivity level of the rubble: that of uranium-235, which the rubble used to claim through GT
     *         item data (read the way {@code GTUtility.getRadioactivityLevel} reads it)
     */
    // Source: GTUtility.getRadioactivityLevel
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1971-L1980
    public static int level() {
        return Math.max(1, Materials.Uranium235.mArmorEnchantmentLevel);
    }

    /**
     * Irradiates a holder as carrying the stack does. Creative players are skipped, as GT skips them; GT's own check
     * skips hazmat, undead and arthropods.
     *
     * @param holder who carries the stack
     * @param stack  the rubble
     * @return true when the holder was irradiated
     */
    public static boolean irradiate(EntityLivingBase holder, ItemStack stack) {
        if (holder instanceof EntityPlayer && ((EntityPlayer) holder).capabilities.isCreativeMode) {
            return false;
        }
        return GTUtility.applyRadioactivity(holder, level(), stack.stackSize);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity holder, int slot, boolean held) {
        if (!world.isRemote && holder instanceof EntityPlayer && holder.ticksExisted % PERIOD_TICKS == 0) {
            irradiate((EntityLivingBase) holder, stack);
        }
    }

    @SideOnly(Side.CLIENT)
    private IIcon icon;

    @Override
    public boolean getHasSubtypes() {
        return true;
    }

    @Override
    public com.gregguns.api.Tier getTier() {
        return com.gregguns.api.Tier.ZPM;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icon = register.registerIcon("gregguns:thirdparty/ntm/irradiated_rubble");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return stack.getItemDamage() == 1 ? HOT_TINT : BlockIrradiatedEarth.TINT;
    }

    /** The tint of hot rubble (damage 1), from the inner fallout of a ZPM or better blast. */
    public static final int HOT_TINT = 0xE8F04A;

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return stack.getItemDamage() == 1 ? getUnlocalizedName() + ".hot" : getUnlocalizedName();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("gregguns.fallout.rubble.tooltip"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }
}
