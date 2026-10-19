package com.gregguns.armor.slots;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.armor.slots.logic.OxygenGearRules;
import com.gregguns.core.config.EquipmentConfig;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.hazards.Hazard;
import ic2.api.item.IElectricItem;
import micdoodle8.mods.galacticraft.api.item.IBreathableArmor;

/**
 * The Tactical Mask: night vision goggles, gas mask and oxygen mask in one helmet-slot item, an HV electric item. It
 * protects fully against {@code GAS} and partly against {@code BIOLOGICAL} whether or not it has charge (passive flags,
 * GGS-05-11); with charge it gives night vision, removes Poison and Wither (each costing the night-vision drain in EU)
 * and, where Galacticraft is loaded, supplies the mask and gear components of an oxygen setup (never a tank,
 * GGS-05-26). Four camos.
 *
 * <p>
 * Server and client.
 */
// Spec: docs/techguns-spec/05-armor/special-equipment.md 3.3; docs/gregguns-spec/05-armor/oxygen-gear.md
// (GGS-05-25, GGS-05-26); docs/gregguns-spec/05-armor/recipes.md (HV, GGS-05-39)
// Deviation: hazard-protection.md lists the Tactical Mask at MV; recipes.md and 0010 decision 5 put it at HV because it
// consumes the HV Oxygen Mask. HV is used.
@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.item.IBreathableArmor", modid = "GalacticraftCore")
public final class ItemTacticalMask extends ItemHelmetMask implements IElectricItem, NightVisionGear, IBreathableArmor {

    /** Registry name. */
    public static final String NAME = "tactical_mask";

    /** Layer texture bases per camo, camo 0 first (Techguns camo order: black, green, white, light green). */
    public static final List<String> CAMOS = Collections.unmodifiableList(
        Arrays.asList("tactical_mask", "tactical_mask_green", "tactical_mask_white", "tactical_mask_lightgreen"));

    private static final String CAMO_TAG = "camo";

    /** Creates the mask; it has no durability of its own. */
    public ItemTacticalMask() {
        setMaxDamage(0);
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    // --- camo (the Camo Bench of M4 calls these) -------------------------------------------------------------------

    /**
     * @param stack a Tactical Mask
     * @return the stored camo index, clamped to the valid range; 0 when none
     */
    public int getCamo(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        int camo = tag == null ? 0 : tag.getInteger(CAMO_TAG);
        return camo >= 0 && camo < CAMOS.size() ? camo : 0;
    }

    /**
     * @param stack a Tactical Mask
     * @param camo  the index; out-of-range values store 0
     */
    public void setCamo(ItemStack stack, int camo) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(CAMO_TAG, camo >= 0 && camo < CAMOS.size() ? camo : 0);
    }

    @Override
    protected String modelId() {
        return NAME;
    }

    @Override
    protected String layerTexture(ItemStack stack) {
        return CAMOS.get(getCamo(stack));
    }

    // --- hazards -----------------------------------------------------------------------------------------------------

    @Override
    protected boolean fullyProtects(ItemStack stack, Hazard hazard) {
        return hazard == Hazard.GAS;
    }

    @Override
    protected boolean partiallyProtects(ItemStack stack, Hazard hazard) {
        return hazard == Hazard.BIOLOGICAL;
    }

    // --- night vision and cure ------------------------------------------------------------------------------------

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

    /**
     * @param stack the mask
     * @return whether it has any charge
     */
    public boolean powered(ItemStack stack) {
        return ElectricGear.charge(stack) > 0.0;
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
        return EquipmentConfig.tacticalMaskCapacityEu;
    }

    @Override
    public int getTier(ItemStack stack) {
        return ElectricGear.ic2Tier(getTier());
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return ElectricGear.transferLimit(getTier());
    }

    // --- Galacticraft: mask and gear components only -----------------------------------------------------------------

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public boolean handleGearType(IBreathableArmor.EnumGearType type) {
        return OxygenGearRules.handles(OxygenGearRules.fromName(type.name()));
    }

    @Override
    @Optional.Method(modid = "GalacticraftCore")
    public boolean canBreathe(ItemStack stack, EntityPlayer player, IBreathableArmor.EnumGearType type) {
        return OxygenGearRules.canBreathe(OxygenGearRules.fromName(type.name()), powered(stack));
    }

    // --- display
    // -------------------------------------------------------------------------------------------------------

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ElectricGear.addCreativeVariants(item, EquipmentConfig.tacticalMaskCapacityEu, getTier(), list);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        ElectricGear.addTooltip(tooltip, stack, EquipmentConfig.tacticalMaskCapacityEu, true);
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.armor.tooltip.camo",
                StatCollector.translateToLocal("item.gregguns." + NAME + ".camoname." + getCamo(stack))));
        tooltip.add(StatCollector.translateToLocal("gregguns.equipment.tooltip.space_suit_note"));
    }
}
