package com.gregguns.armor;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.util.EnumHelper;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.ArmorMaterialDef;
import com.gregguns.armor.logic.ArmorProtection;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.HazardFlag;
import com.gregguns.armor.logic.PieceDef;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.combat.DamageType;
import com.gregguns.combat.TypedArmor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.hazards.Hazard;
import gregtech.api.hazards.IHazardProtector;

/**
 * One piece of GregGuns armor. All of its numbers come from its {@link SetDef} and {@link PieceDef}; the class itself
 * is
 * the same for every set.
 *
 * <ul>
 * <li>Protection: implements Forge's {@link ISpecialArmor}, answering {@code points / 25} for the hit's
 * {@link DamageType}; and {@link TypedArmor}, which the damage handler reads for penetration resistance once per hit.
 * <li>Durability: every piece has {@code 13.75 * base} points and never breaks; with one point left it gives no
 * protection and no bonuses.
 * <li>Hazards: implements GT's {@link IHazardProtector} from the piece's flags. Never {@code SPACE}.
 * <li>Camo: the camo index lives in the stack NBT ({@link #getCamo}); the layer texture follows it.
 * </ul>
 *
 * <p>
 * Vanilla material: one per set, built with {@code EnumHelper}, never {@code CHAIN} (GT would give a CHAIN material
 * electrical protection by accident, GGS-05-10) and with enchantability 0 (Techguns rule).
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md sections 1 to 3; docs/gregguns-spec/05-armor/hazard-protection.md
public final class ItemGregGunsArmor extends ItemArmor implements ISpecialArmor, TypedArmor, IHazardProtector, Tiered {

    private static final String CAMO_TAG = "camo";

    private final SetDef set;
    private final PieceDef piece;

    /**
     * @param set   the set
     * @param piece the piece (its slot decides the vanilla armor type)
     */
    public ItemGregGunsArmor(SetDef set, PieceDef piece) {
        super(
            materialFor(set),
            0,
            piece.slot()
                .armorType());
        this.set = set;
        this.piece = piece;
        setMaxStackSize(1);
        setMaxDamage(
            set.material()
                .pieceDurability());
    }

    private static ArmorMaterial materialFor(SetDef set) {
        ArmorMaterialDef m = set.material();
        int[] reduction = new int[4];
        for (ArmorSlot slot : ArmorSlot.values()) {
            reduction[slot.armorType()] = ArmorProtection.vanillaPoints(m, slot);
        }
        // Source: Forge 1.7.10 net.minecraftforge.common.util.EnumHelper.addArmorMaterial(name, durability,
        // reductionAmounts, enchantability); the durability here is replaced by setMaxDamage in the constructor.
        return EnumHelper.addArmorMaterial(
            "GREGGUNS_" + m.id()
                .toUpperCase(java.util.Locale.ROOT),
            m.pieceDurability() / 4,
            reduction,
            0);
    }

    /** @return the set */
    public SetDef set() {
        return set;
    }

    /** @return the piece definition */
    public PieceDef piece() {
        return piece;
    }

    @Override
    public Tier getTier() {
        return set.tier();
    }

    // --- camo ---------------------------------------------------------------------------------------------------

    /**
     * @param stack a piece of this set
     * @return the stored camo index, clamped to the set's range (ARM-12); 0 when none is stored (ARM-11)
     */
    public int getCamo(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : set.clampCamo(tag.getInteger(CAMO_TAG));
    }

    /**
     * Stores a camo index.
     *
     * @param stack a piece of this set
     * @param camo  the index; out-of-range values store 0
     */
    public void setCamo(ItemStack stack, int camo) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(CAMO_TAG, set.clampCamo(camo));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
        return set.layerTexture(this.piece.slot(), getCamo(stack));
    }

    // --- protection -----------------------------------------------------------------------------------------------

    private boolean broken(ItemStack stack) {
        return ArmorProtection.isBroken(stack.getItemDamage(), getMaxDamage());
    }

    /** @return true when the stack is broken: no protection, no bonuses */
    public boolean isBroken(ItemStack stack) {
        return broken(stack);
    }

    @Override
    public double armorValue(ItemStack stack, DamageType type) {
        return ArmorProtection.pieceArmor(set.material(), piece.slot(), type, stack.getItemDamage(), getMaxDamage());
    }

    @Override
    public double penetrationResistance(ItemStack stack, DamageType type) {
        return ArmorProtection
            .piecePenetrationResistance(set.material(), piece.slot(), stack.getItemDamage(), getMaxDamage());
    }

    @Override
    public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage,
        int slot) {
        if (source.isUnblockable()) {
            return new ArmorProperties(0, 0.0, 0);
        }
        double ratio = ArmorProtection
            .absorbRatio(set.material(), piece.slot(), DamageTypes.of(source), armor.getItemDamage(), getMaxDamage());
        return new ArmorProperties(0, ratio, ArmorProtection.maxAbsorb(armor.getItemDamage(), getMaxDamage()));
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        return ArmorProtection.displayPoints(set.material(), piece.slot(), armor.getItemDamage(), getMaxDamage());
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot) {
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode) {
            return;
        }
        stack.setItemDamage(ArmorProtection.wear(stack.getItemDamage(), getMaxDamage(), damage));
    }

    // --- repair ---------------------------------------------------------------------------------------------------

    @Override
    public boolean getIsRepairable(ItemStack armor, ItemStack repairItem) {
        return ArmorRepair.isRepairItem(piece.repair(), repairItem);
    }

    // --- hazards ----------------------------------------------------------------------------------------------------

    private static @Nullable Hazard toGt(HazardFlag flag) {
        switch (flag) {
            case BIOLOGICAL:
                return Hazard.BIOLOGICAL;
            case FROST:
                return Hazard.FROST;
            case HEAT:
                return Hazard.HEAT;
            case RADIOLOGICAL:
                return Hazard.RADIOLOGICAL;
            case ELECTRICAL:
                return Hazard.ELECTRICAL;
            case GAS:
                return Hazard.GAS;
            default:
                return null;
        }
    }

    private boolean covers(java.util.Set<HazardFlag> flags, Hazard hazard) {
        for (HazardFlag f : flags) {
            if (toGt(f) == hazard) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean protectsAgainst(ItemStack itemStack, Hazard hazard) {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/IHazardProtector.java#L8-L21
        // A broken piece protects against nothing. SPACE is never claimed (GGS-05-12): no HazardFlag maps to it.
        return !broken(itemStack) && (covers(piece.partialHazards(), hazard) || covers(piece.fullHazards(), hazard));
    }

    @Override
    public boolean protectsAgainstFully(@Nullable EntityLivingBase entity, ItemStack itemStack, Hazard hazard) {
        return !broken(itemStack) && covers(piece.fullHazards(), hazard);
    }

    // --- tooltip ----------------------------------------------------------------------------------------------------

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        if (broken(stack)) {
            tooltip.add(StatCollector.translateToLocal("gregguns.armor.tooltip.broken"));
        }
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.armor.tooltip.protection",
                trim(
                    set.material()
                        .pieceValue(DamageType.PHYSICAL, piece.slot())),
                trim(
                    set.material()
                        .pieceValue(DamageType.ENERGY, piece.slot()))));
        for (java.util.Map.Entry<ArmorBonus, Double> e : piece.bonuses()
            .asMap()
            .entrySet()) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "gregguns.armor.tooltip.bonus",
                    StatCollector.translateToLocal(
                        e.getKey()
                            .langKey()),
                    trim(e.getValue())));
        }
        if (set.camoCount() > 1) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "gregguns.armor.tooltip.camo",
                    StatCollector.translateToLocal("item.gregguns." + set.id() + ".camoname." + getCamo(stack))));
        }
    }

    private static String trim(double v) {
        return new java.text.DecimalFormat("#.###", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT))
            .format(v);
    }
}
