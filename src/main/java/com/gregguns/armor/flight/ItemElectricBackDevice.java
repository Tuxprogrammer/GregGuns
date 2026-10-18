package com.gregguns.armor.flight;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.armor.slots.ItemBackDevice;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.IElectricItem;

/**
 * A back device that is an IC2 electric item at its recipe tier: the Jump Pack (HV) and the Anti Gravity Device (IV).
 * The charge is IC2's standard stack NBT, {@code getTier} is the GT voltage index and {@code getTransferLimit} is
 * {@code GTValues.V[tier]}, so GT chargers and battery buffers accept it only at its exact tier (GGS-05-14, design
 * 0008). It is charged by taking it off and using a charger; nothing charges it in the slot. Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-14, GGS-05-21); docs/design/0008-exact-tier-charging.md
public abstract class ItemElectricBackDevice extends ItemBackDevice implements IElectricItem {

    /** @return the capacity in EU, from the config */
    protected abstract double capacity();

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
        return capacity();
    }

    @Override
    public int getTier(ItemStack stack) {
        return ElectricGear.ic2Tier(getTier());
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return ElectricGear.transferLimit(getTier());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ElectricGear.addCreativeVariants(item, capacity(), getTier(), list);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        ElectricGear.addTooltip(tooltip, stack, capacity(), false);
        super.addInformation(stack, player, tooltip, advanced);
    }
}
