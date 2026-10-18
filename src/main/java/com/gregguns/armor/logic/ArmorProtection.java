package com.gregguns.armor.logic;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;

/**
 * The protection math of one armor piece, with no Minecraft types. A worn piece reports an armor value per damage type;
 * the damage handler turns the sum of those values into a reduction ({@link DamageModel}) once per hit. Unit-tested
 * against the Techguns tables.
 *
 * <p>
 * A piece with one durability point left (or less) is <em>broken</em>: it gives no protection and no bonuses, but never
 * actually breaks (Techguns rule, kept).
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md sections 2 and 3; docs/design/0004-damage-model.md
public final class ArmorProtection {

    private ArmorProtection() {}

    /**
     * @param damage    the stack's damage
     * @param maxDamage the piece's durability
     * @return true when at most one durability point is left
     */
    public static boolean isBroken(int damage, int maxDamage) {
        return damage >= maxDamage - 1;
    }

    /**
     * @param material  the set's material
     * @param slot      the piece's slot
     * @param type      the hit's damage type
     * @param damage    the stack's damage
     * @param maxDamage the piece's durability
     * @return the piece's armor points against the type; 0 when broken or for an armor-bypassing type
     */
    public static double pieceArmor(ArmorMaterialDef material, ArmorSlot slot, DamageType type, int damage,
        int maxDamage) {
        if (isBroken(damage, maxDamage) || type.bypassesArmor()) {
            return 0.0;
        }
        return material.pieceValue(type, slot);
    }

    /**
     * @param material  the set's material
     * @param slot      the piece's slot
     * @param damage    the stack's damage
     * @param maxDamage the piece's durability
     * @return the piece's share of the penetration resistance; 0 when broken
     */
    public static double piecePenetrationResistance(ArmorMaterialDef material, ArmorSlot slot, int damage,
        int maxDamage) {
        return isBroken(damage, maxDamage) ? 0.0 : material.piecePenetrationResistance(slot);
    }

    /**
     * The ratio Forge's {@code ArmorProperties} wants for this piece against this hit: {@code points / 25}. Forge adds
     * the ratios of all four pieces, so a full set of {@code A} points absorbs {@code A / 25} of the armored share,
     * the same as vanilla's {@code (25 - A) / 25} pass-through.
     *
     * @param material  the set's material
     * @param slot      the piece's slot
     * @param type      the hit's damage type
     * @param damage    the stack's damage
     * @param maxDamage the piece's durability
     * @return the absorb ratio, 0..1
     */
    public static double absorbRatio(ArmorMaterialDef material, ArmorSlot slot, DamageType type, int damage,
        int maxDamage) {
        return DamageModel.pieceAbsorbRatio(pieceArmor(material, slot, type, damage, maxDamage));
    }

    /**
     * @param material the set's material
     * @param slot     the slot
     * @return the vanilla armor-bar points of an intact piece: the physical value rounded half up (4/5/4/3 for T1
     *         Combat)
     */
    public static int vanillaPoints(ArmorMaterialDef material, ArmorSlot slot) {
        return (int) Math.floor(material.pieceValue(DamageType.PHYSICAL, slot) + 0.5);
    }

    /**
     * @param material  the set's material
     * @param slot      the slot
     * @param damage    the stack's damage
     * @param maxDamage the piece's durability
     * @return the points the armor bar shows for this stack: 0 when broken
     */
    public static int displayPoints(ArmorMaterialDef material, ArmorSlot slot, int damage, int maxDamage) {
        return isBroken(damage, maxDamage) ? 0 : vanillaPoints(material, slot);
    }

    /**
     * @param damage    the stack's damage
     * @param maxDamage the piece's durability
     * @return the most damage the piece can soak before the next hit passes through, as vanilla armor reports it
     */
    public static int maxAbsorb(int damage, int maxDamage) {
        return Math.max(0, maxDamage + 1 - damage);
    }

    /**
     * Wears a piece. The piece never breaks: at least one durability point is always left.
     *
     * @param damage    the stack's current damage
     * @param maxDamage the piece's durability
     * @param incoming  durability points of wear from the hit
     * @return the new damage value, at most {@code maxDamage - 1}
     */
    public static int wear(int damage, int maxDamage, int incoming) {
        int cap = Math.max(0, maxDamage - 1);
        return Math.min(cap, Math.max(damage, 0) + Math.max(incoming, 0));
    }

    /**
     * The armor points of a set of intact pieces against one type: the sum of {@link #pieceArmor}, which for a full set
     * equals the material total.
     *
     * @param material the material
     * @param type     the damage type
     * @param slots    the worn, intact slots
     * @return the total armor points, before the {@link DamageModel#MAX_ARMOR} clamp
     */
    public static double total(ArmorMaterialDef material, DamageType type, ArmorSlot... slots) {
        double sum = 0.0;
        for (ArmorSlot s : slots) {
            sum += material.pieceValue(type, s);
        }
        return type.bypassesArmor() ? 0.0 : sum;
    }
}
