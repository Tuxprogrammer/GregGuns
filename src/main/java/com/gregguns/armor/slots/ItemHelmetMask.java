package com.gregguns.armor.slots;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.util.EnumHelper;

import com.gregguns.api.Tiered;

import gregtech.api.hazards.Hazard;
import gregtech.api.hazards.IHazardProtector;

/**
 * Base of the masks worn in the vanilla <em>helmet</em> slot (Gas Mask, Tactical Mask). They are armor items because
 * GT's hazard check reads only the four armor slots (design 0009 decision 2; GGS-05-09). A mask has no armor value of
 * its own and takes no wear from hits; what it gives is the hazard answers below and the tick effects driven by
 * {@link SlotEquipment}.
 *
 * <p>
 * The vanilla material is a private {@code EnumHelper} one, never {@code CHAIN} (GGS-05-10), with enchantability 0.
 * Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/hazard-protection.md (GGS-05-08, GGS-05-09, GGS-05-10, GGS-05-12)
// Source: IHazardProtector contract (partial vs full protection):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/IHazardProtector.java#L8-L21
public abstract class ItemHelmetMask extends ItemArmor implements ISpecialArmor, IHazardProtector, Tiered {

    // Source: Forge 1.7.10 EnumHelper.addArmorMaterial(name, durability, reductionAmounts, enchantability); all zero.
    private static final ArmorMaterial MATERIAL = EnumHelper
        .addArmorMaterial("GREGGUNS_MASK", 1, new int[] { 0, 0, 0, 0 }, 0);

    protected ItemHelmetMask() {
        super(MATERIAL, 0, 0);
        setMaxStackSize(1);
    }

    /**
     * @param stack  the worn mask
     * @param hazard a GT hazard
     * @return whether the mask counts toward GT's full-set rule for it (GT {@code protectsAgainst}); {@code SPACE} is
     *         never claimed (GGS-05-12)
     */
    protected abstract boolean partiallyProtects(ItemStack stack, Hazard hazard);

    /**
     * @param stack  the worn mask
     * @param hazard a GT hazard
     * @return whether the mask alone covers it (GT {@code protectsAgainstFully})
     */
    protected abstract boolean fullyProtects(ItemStack stack, Hazard hazard);

    @Override
    public boolean protectsAgainst(ItemStack stack, Hazard hazard) {
        return hazard != Hazard.SPACE && (fullyProtects(stack, hazard) || partiallyProtects(stack, hazard));
    }

    @Override
    public boolean protectsAgainstFully(EntityLivingBase entity, ItemStack stack, Hazard hazard) {
        return hazard != Hazard.SPACE && fullyProtects(stack, hazard);
    }

    // --- ISpecialArmor: no protection, no wear ---------------------------------------------------------------------

    @Override
    public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage,
        int slot) {
        return new ArmorProperties(0, 0.0, 0);
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        return 0;
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot) {}

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
        return "gregguns:textures/models/armor/" + layerTexture(stack) + ".png";
    }

    /**
     * @param stack the mask
     * @return the layer texture base name under {@code textures/models/armor/}
     */
    protected abstract String layerTexture(ItemStack stack);
}
