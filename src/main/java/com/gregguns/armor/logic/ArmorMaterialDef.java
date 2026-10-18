package com.gregguns.armor.logic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.gregguns.combat.DamageType;

/**
 * A set's material: base durability, one armor total per {@link DamageType}, and a penetration resistance. A piece
 * carries {@code total * slot share} of each; a full set carries the totals. Plain Java, both sides.
 *
 * <p>
 * Build one with {@link #uniform}, which follows the Techguns single-value constructor: physical and projectile get the
 * value, fire, explosion, energy, ice and lightning get 75% of it, poison and radiation get 0. Chained {@link #with}
 * calls override single types.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 2 (TGArmorMaterial)
public final class ArmorMaterialDef {

    private static final double DURABILITY_FACTOR = 0.25 * 55.0;

    private final String id;
    private final int baseDurability;
    private final double penetrationResistance;
    private final EnumMap<DamageType, Double> totals;

    private ArmorMaterialDef(String id, int baseDurability, double penetrationResistance,
        EnumMap<DamageType, Double> totals) {
        this.id = id;
        this.baseDurability = baseDurability;
        this.penetrationResistance = penetrationResistance;
        this.totals = totals;
    }

    /**
     * @param id             material id, for example {@code "t1_combat"}; also the vanilla material name
     * @param baseDurability Techguns base durability (60 for T1)
     * @param armor          the single armor value A
     * @return a material following the Techguns single-value rule
     */
    public static ArmorMaterialDef uniform(String id, int baseDurability, double armor) {
        EnumMap<DamageType, Double> m = new EnumMap<>(DamageType.class);
        for (DamageType t : DamageType.values()) {
            m.put(t, 0.0);
        }
        m.put(DamageType.PHYSICAL, armor);
        m.put(DamageType.PROJECTILE, armor);
        for (DamageType t : new DamageType[] { DamageType.FIRE, DamageType.EXPLOSION, DamageType.ENERGY, DamageType.ICE,
            DamageType.LIGHTNING }) {
            m.put(t, 0.75 * armor);
        }
        return new ArmorMaterialDef(id, baseDurability, 0.0, m);
    }

    /**
     * @param type  a damage type
     * @param total the full-set value for it
     * @return a copy with the override
     */
    public ArmorMaterialDef with(DamageType type, double total) {
        EnumMap<DamageType, Double> m = new EnumMap<>(totals);
        m.put(type, total);
        return new ArmorMaterialDef(id, baseDurability, penetrationResistance, m);
    }

    /**
     * @param resistance penetration resistance of a full set, 0..1
     * @return a copy with it set
     */
    public ArmorMaterialDef withPenetrationResistance(double resistance) {
        return new ArmorMaterialDef(id, baseDurability, resistance, totals);
    }

    /** @return the material id */
    public String id() {
        return id;
    }

    /** @return the Techguns base durability */
    public int baseDurability() {
        return baseDurability;
    }

    /**
     * @return the durability of every piece: {@code round(0.25 * 55 * base)}, the same for all four slots (825 for T1)
     */
    public int pieceDurability() {
        return (int) Math.round(DURABILITY_FACTOR * baseDurability);
    }

    /**
     * @param type a damage type
     * @return the full-set armor value against it
     */
    public double total(DamageType type) {
        Double v = totals.get(type);
        return v == null ? 0.0 : v;
    }

    /**
     * @param type a damage type
     * @param slot the slot
     * @return one piece's armor value, {@code total * slot share}
     */
    public double pieceValue(DamageType type, ArmorSlot slot) {
        return total(type) * slot.share();
    }

    /**
     * @param slot the slot
     * @return one piece's share of the penetration resistance; the four pieces add up to the full-set value
     */
    public double piecePenetrationResistance(ArmorSlot slot) {
        return penetrationResistance * slot.share();
    }

    /** @return the full-set penetration resistance */
    public double penetrationResistance() {
        return penetrationResistance;
    }

    /** @return a read-only view of the totals, for tooltips and tests */
    public Map<DamageType, Double> totals() {
        return Collections.unmodifiableMap(totals);
    }
}
