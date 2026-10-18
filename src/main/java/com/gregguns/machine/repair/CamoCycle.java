package com.gregguns.machine.repair;

/**
 * The Camo Bench's index arithmetic and click targets, as plain Java. Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/camo-bench.md (button handling),
// docs/gregguns-spec/09-machines-utility/index.md (Camo Bench)
// Deviation: Techguns numbers 14 buttons (1/2 bench slot, 3 to 10 armor, 11 to 14 back and face slots). GregGuns has
// no extended back or face slots, so the packet carries a target (the bench slot or one worn piece) and a direction.
public final class CamoCycle {

    /** What a camo click acts on. */
    public enum Target {

        /** The item in the bench's slot. */
        BENCH_SLOT(-1),
        /** The worn helmet. */
        HELMET(0),
        /** The worn chestplate. */
        CHESTPLATE(1),
        /** The worn leggings. */
        LEGGINGS(2),
        /** The worn boots. */
        BOOTS(3);

        private final int armorType;

        Target(int armorType) {
            this.armorType = armorType;
        }

        /** @return vanilla's armor type 0..3 for a worn piece, -1 for the bench slot */
        public int armorType() {
            return armorType;
        }

        /**
         * @param ordinal a wire value
         * @return the target, or null when out of range
         */
        public static Target fromOrdinal(int ordinal) {
            Target[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
        }
    }

    private CamoCycle() {}

    /**
     * @param current the current index
     * @param count   how many camos the item has
     * @param back    true to step backward
     * @return the next index, wrapping inside {@code [0, count)}; 0 when the item has one camo or none
     */
    public static int next(int current, int count, boolean back) {
        if (count <= 1) {
            return 0;
        }
        return Math.floorMod(current + (back ? -1 : 1), count);
    }

    /**
     * @param index a stored index
     * @param count how many camos the item has
     * @return the index, or 0 when it is outside {@code [0, count)}
     */
    public static int clamp(int index, int count) {
        return index >= 0 && index < count ? index : 0;
    }
}
