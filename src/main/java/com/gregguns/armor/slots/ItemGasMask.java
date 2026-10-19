package com.gregguns.armor.slots;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.armor.slots.logic.FilterWear;
import com.gregguns.core.config.EquipmentConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.hazards.Hazard;

/**
 * The Gas Mask: a helmet-slot item that fully protects against GT's {@code GAS} hazard (pollution) and partly against
 * {@code BIOLOGICAL}, and that removes Poison and Wither while its filter lasts. The filter is the mask's durability
 * ({@code equipment.gasMaskFilterTicks}); a spent filter gives no cure and no GAS protection, and a new Gas Mask Filter
 * refills it in the crafting grid ({@link EquipmentRefillRecipe}).
 *
 * <p>
 * Server and client; the cure is applied by {@link SlotEquipment#tick}.
 */
// Spec: docs/gregguns-spec/05-armor/hazard-protection.md (GGS-05-09, GGS-05-13); docs/techguns-spec/05-armor/
// special-equipment.md 3.1
// Deviation: Techguns kept the mask effective while "not empty" including its last point; a GregGuns piece never
// breaks and one point is always left, so "spent" is one point earlier (FilterWear.spent).
public final class ItemGasMask extends ItemHelmetMask {

    /** Registry name. */
    public static final String NAME = "gas_mask";

    /** Creates the mask with the configured filter size. */
    public ItemGasMask() {
        setMaxDamage(EquipmentConfig.gasMaskFilterTicks);
    }

    @Override
    public Tier getTier() {
        return Tier.MV;
    }

    /**
     * @param stack a Gas Mask
     * @return whether the filter is spent
     */
    public boolean filterSpent(ItemStack stack) {
        return FilterWear.spent(stack.getItemDamage(), getMaxDamage());
    }

    @Override
    protected boolean fullyProtects(ItemStack stack, Hazard hazard) {
        return hazard == Hazard.GAS && !filterSpent(stack);
    }

    @Override
    protected boolean partiallyProtects(ItemStack stack, Hazard hazard) {
        return hazard == Hazard.BIOLOGICAL && !filterSpent(stack);
    }

    @Override
    protected String modelId() {
        return NAME;
    }

    @Override
    protected String layerTexture(ItemStack stack) {
        return "gas_mask";
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        int left = Math.max(0, getMaxDamage() - 1 - stack.getItemDamage());
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                filterSpent(stack) ? "gregguns.equipment.tooltip.filter_spent" : "gregguns.equipment.tooltip.filter",
                left,
                getMaxDamage() - 1));
    }
}
