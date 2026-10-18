package com.gregguns.armor.logic;

/**
 * The four armor slots with their share of a material's protection. The ordinal equals vanilla's
 * {@code ItemArmor.armorType} (0 helmet to 3 boots). Plain Java, both sides.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 2 (per-slot split)
public enum ArmorSlot {

    /** Head. */
    HELMET(0.25),
    /** Torso. */
    CHESTPLATE(0.30),
    /** Legs. Uses the second armor layer texture ({@code _2}). */
    LEGGINGS(0.25),
    /** Feet. */
    BOOTS(0.20);

    private final double share;

    ArmorSlot(double share) {
        this.share = share;
    }

    /** @return the fraction of a material's per-type total that this slot carries; the four add up to 1 */
    public double share() {
        return share;
    }

    /** @return vanilla's {@code armorType} index, 0 for the helmet to 3 for the boots */
    public int armorType() {
        return ordinal();
    }

    /**
     * @param armorType vanilla's {@code armorType}
     * @return the slot
     * @throws IllegalArgumentException outside 0..3
     */
    public static ArmorSlot fromArmorType(int armorType) {
        ArmorSlot[] all = values();
        if (armorType < 0 || armorType >= all.length) {
            throw new IllegalArgumentException("armorType " + armorType);
        }
        return all[armorType];
    }

    /** @return the lower-case id used in registry names: helmet, chestplate, leggings, boots */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
