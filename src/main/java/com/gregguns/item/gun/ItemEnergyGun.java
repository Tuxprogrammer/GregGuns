package com.gregguns.item.gun;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.core.config.EnergyConfig;
import com.gregguns.core.text.TierText;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.logic.EnergyCapacity;
import com.gregguns.gun.logic.EnergyMath;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.IElectricItem;

/**
 * An energy gun: an {@link ItemGun} that is also an IC2 electric item at the gun's tier, so GT chargers and battery
 * buffers fill it and the shot spends EU instead of a round. The charge lives in IC2's stack NBT and is touched only
 * through {@link ElectricGear}. It never provides energy back to a machine. A GT battery buffer or charger accepts it
 * only at exactly its tier (design 0008).
 *
 * <p>
 * Capacity is the definition's default scaled or overridden by {@link EnergyConfig} ({@link EnergyCapacity}). The
 * durability bar shows the charge; there is no item wear. The creative tab lists an empty and a full variant.
 * Both sides; the tooltip is client only.
 */
// Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-09 to GGS-02-11, GGS-02-15, GGS-02-16)
// Source: GT's MechArmorBase electric methods, getTier = VoltageIndex and getTransferLimit = GTValues.V[tier]:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L484-L519
// Source: GT battery buffers accept an item at exactly their tier (GTModHandler.isElectricItem):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L2295-L2298
public class ItemEnergyGun extends ItemGun implements IElectricItem {

    /**
     * @param definition an energy gun's definition
     * @throws IllegalArgumentException if the definition's stats are not energy stats
     */
    public ItemEnergyGun(GunDefinition definition) {
        super(definition);
        if (!definition.stats()
            .isEnergy()) {
            throw new IllegalArgumentException(definition.name() + " is not an energy gun");
        }
        // No item damage: IC2 shows the charge as a bar only when the item has no durability of its own.
        setMaxDamage(0);
    }

    /** @return capacity in EU after the scale and per-gun override of the config */
    public long capacityEu() {
        return EnergyCapacity.resolve(
            stats().id(),
            stats().capacityEu(),
            stats().euPerShot(),
            EnergyConfig.capacityScalePercent,
            EnergyConfig.capacityOverrides);
    }

    /**
     * @param stack a stack of this gun
     * @return its stored EU
     */
    public long chargeOf(ItemStack stack) {
        return (long) Math.floor(ElectricGear.charge(stack));
    }

    // --- IElectricItem ------------------------------------------------------------------------------------------

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
        return capacityEu();
    }

    @Override
    public int getTier(ItemStack stack) {
        return ElectricGear.ic2Tier(getTier());
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return ElectricGear.transferLimit(getTier());
    }

    // --- display ------------------------------------------------------------------------------------------------

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return EnergyMath.barFraction((long) Math.floor(ElectricGear.chargeReadOnly(stack)), capacityEu());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ElectricGear.addCreativeVariants(item, capacityEu(), getTier(), list);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void addPowerTooltip(ItemStack stack, List tooltip) {
        long charge = chargeOf(stack);
        ElectricGear.addTooltip(tooltip, stack, capacityEu(), false);
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.gun.tooltip.shots_left",
                EnergyMath.shotsLeft(charge, stats().euPerShot()),
                stats().euPerShot()));
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.gun.tooltip.charger",
                TierText.colored(getTier()),
                getTier().voltage()));
    }
}
