package com.gregguns.armor.slots;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.armor.slots.logic.PowerUse;

import gregtech.api.enums.GTValues;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItemManager;

/**
 * Shared helpers for GregGuns equipment that is an IC2 electric item, the way GT's own electric armor is (design 0003,
 * 0008). The charge lives in IC2's standard stack NBT and is read and written only through
 * {@link ElectricItem#manager}, so GT chargers and battery buffers see it. Server and client (tooltips).
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-14); docs/design/0008-exact-tier-charging.md
// Source: IC2 API ic2.api.item.ElectricItem / IElectricItemManager (api-source, IC2 2.2.828-experimental)
// Source: GT's MechArmorBase electric methods, getTier = VoltageIndex and getTransferLimit = GTValues.V[tier]:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L484-L519
public final class ElectricGear {

    private static final String NV_TAG = "nightVision";

    /**
     * IC2's NBT key for an item's stored EU. Only {@link #chargeReadOnly} reads it, for the durability bar; every write
     * goes through IC2's manager.
     */
    // Decompiled: IC2 2.2.828 ic2.core.item.ElectricItemManager keeps the stored EU as a double under "charge" in the
    // stack's own NBT, removes the key when the charge reaches 0, and reads a missing key as 0. IC2 has no public
    // source; only that storage layout was relied on, and gun.energy.item fails if it drifts.
    static final String CHARGE_KEY = "charge";

    private ElectricGear() {}

    /**
     * @param tier the item's tier
     * @return IC2's tier number, which equals GT's voltage index
     */
    public static int ic2Tier(Tier tier) {
        return tier.voltageIndex();
    }

    /**
     * @param tier the item's tier
     * @return the most EU per tick a charger may push in: the tier's voltage
     */
    public static double transferLimit(Tier tier) {
        return GTValues.V[tier.voltageIndex()];
    }

    /**
     * @param stack an electric item
     * @return its stored EU, 0 when IC2 has no manager (unit tests)
     */
    public static double charge(ItemStack stack) {
        IElectricItemManager m = ElectricItem.manager;
        return m == null ? 0.0 : m.getCharge(stack);
    }

    /**
     * The stored EU read straight from the stack's NBT, without creating any tag (IC2 keeps it under {@code charge}).
     * The durability bar runs every frame, and a getter that writes NBT makes NEI log a forced tag update.
     *
     * @param stack an electric item
     * @return its stored EU, 0 when it has no tag yet
     */
    public static double chargeReadOnly(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return 0.0;
        }
        return stack.getTagCompound()
            .getDouble(CHARGE_KEY);
    }

    /**
     * Draws EU from an item.
     *
     * @param stack  the item
     * @param amount EU to draw; 0 or less always succeeds
     * @param user   the wearer
     * @return true when the full amount was drawn
     */
    public static boolean use(ItemStack stack, double amount, EntityLivingBase user) {
        if (amount <= 0.0) {
            return true;
        }
        IElectricItemManager m = ElectricItem.manager;
        return m != null && m.use(stack, amount, user);
    }

    /**
     * Fills an item. Server or scenario use.
     *
     * @param stack  the item
     * @param amount EU to add
     * @param tier   the tier the charge is offered at
     * @return the EU accepted
     */
    public static double charge(ItemStack stack, double amount, Tier tier) {
        IElectricItemManager m = ElectricItem.manager;
        return m == null ? 0.0 : m.charge(stack, amount, ic2Tier(tier), true, false);
    }

    /**
     * @param stack a stack that may carry the night-vision switch
     * @return the switch; a stack that never stored one is on
     */
    public static boolean nightVisionOn(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        boolean present = tag != null && tag.hasKey(NV_TAG);
        return PowerUse.switchState(present && tag.getBoolean(NV_TAG), present);
    }

    /**
     * @param stack the stack
     * @param on    the new night-vision switch
     */
    public static void setNightVision(ItemStack stack, boolean on) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setBoolean(NV_TAG, on);
    }

    /**
     * Pays one tick of night-vision upkeep.
     *
     * @param stack  the worn gear
     * @param wearer the wearer
     * @param cost   EU per tick
     * @return true when the switch is on and the EU was paid
     */
    public static boolean tickNightVision(ItemStack stack, EntityLivingBase wearer, double cost) {
        if (!nightVisionOn(stack) || !PowerUse.works(true, charge(stack), cost)) {
            return false;
        }
        return use(stack, PowerUse.drain(true, charge(stack), cost), wearer);
    }

    /**
     * Adds the charge and switch lines to a tooltip. Client only.
     *
     * @param tooltip the tooltip
     * @param stack   the stack
     * @param max     capacity in EU
     * @param hasNv   whether to show the night-vision switch
     */
    public static void addTooltip(List<String> tooltip, ItemStack stack, double max, boolean hasNv) {
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.equipment.tooltip.energy",
                Math.round(charge(stack)),
                Math.round(max)));
        if (hasNv) {
            tooltip.add(
                StatCollector.translateToLocal(
                    nightVisionOn(stack) ? "gregguns.equipment.tooltip.nv_on" : "gregguns.equipment.tooltip.nv_off"));
        }
    }

    /**
     * Adds an empty and a full variant to a creative listing.
     *
     * @param item the item
     * @param max  capacity in EU
     * @param tier the item's tier
     * @param list the listing
     */
    public static void addCreativeVariants(Item item, double max, Tier tier, List<ItemStack> list) {
        list.add(new ItemStack(item));
        ItemStack full = new ItemStack(item);
        charge(full, max, tier);
        list.add(full);
    }
}
