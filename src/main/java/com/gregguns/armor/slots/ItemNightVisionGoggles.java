package com.gregguns.armor.slots;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.api.Tier;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.core.config.EquipmentConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.IElectricItem;

/**
 * Night Vision Goggles: a Baubles {@code head} item and an MV electric item. While worn, switched on and charged they
 * draw {@code equipment.nightVisionDrainEuPerTick} EU per tick and give night vision through the armor module's
 * {@link com.gregguns.armor.logic.NightVisionPolicy} (so a potion's night vision is never removed, ARM-29). The switch
 * is a key ({@link EquipmentKeybinds}); the stack comes out of the Assembler empty.
 *
 * <p>
 * Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-02, GGS-05-06); docs/gregguns-spec/05-armor/recipes.md
// (Night Vision Goggles, MV electric item, GGS-05-39); docs/techguns-spec/05-armor/special-equipment.md 3.2
public final class ItemNightVisionGoggles extends ItemBauble implements IElectricItem, NightVisionGear {

    /** Registry name. */
    public static final String NAME = "night_vision_goggles";

    @Override
    public BaubleSlot slot() {
        return BaubleSlot.HEAD;
    }

    @Override
    public Tier getTier() {
        return Tier.MV;
    }

    // --- NightVisionGear -------------------------------------------------------------------------------------------

    @Override
    public boolean nightVisionEnabled(ItemStack stack) {
        return ElectricGear.nightVisionOn(stack);
    }

    @Override
    public void setNightVisionEnabled(ItemStack stack, boolean on) {
        ElectricGear.setNightVision(stack, on);
    }

    @Override
    public boolean tickNightVision(ItemStack stack, EntityLivingBase wearer) {
        return ElectricGear.tickNightVision(stack, wearer, EquipmentConfig.nightVisionDrainEuPerTick);
    }

    // --- IElectricItem ---------------------------------------------------------------------------------------------

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public Item getChargedItem(ItemStack stack) {
        return this;
    }

    @Override
    public Item getEmptyItem(ItemStack stack) {
        return this;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return EquipmentConfig.nightVisionCapacityEu;
    }

    @Override
    public int getTier(ItemStack stack) {
        return ElectricGear.ic2Tier(getTier());
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return ElectricGear.transferLimit(getTier());
    }

    // --- display -----------------------------------------------------------------------------------------------------

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ElectricGear.addCreativeVariants(item, EquipmentConfig.nightVisionCapacityEu, getTier(), list);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        ElectricGear.addTooltip(tooltip, stack, EquipmentConfig.nightVisionCapacityEu, true);
        super.addInformation(stack, player, tooltip, advanced);
    }
}
