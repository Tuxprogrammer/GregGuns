package com.gregguns.armor.logic;

/**
 * What it takes to repair a piece: a metal part named by GT ore prefix and material (or none), a cloth part named by
 * GregGuns registry name, how the total is split, and how many parts a fully worn piece costs. Plain Java, both sides.
 *
 * <p>
 * Deviation: Techguns names its own iron ingot and heavy cloth; GregGuns names a GT part (GGS-09-20), and the metal is
 * the set's gate material rather than iron.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 3 (getRepairMats);
// docs/gregguns-spec/09-machines-utility/repair-bench.md
public final class RepairSpec {

    /** The parts one repair consumes. */
    public static final class Cost {

        private final int metal;
        private final int cloth;

        Cost(int metal, int cloth) {
            this.metal = metal;
            this.cloth = cloth;
        }

        /** @return metal parts, 0 when the spec has no metal */
        public int metal() {
            return metal;
        }

        /** @return cloth parts */
        public int cloth() {
            return cloth;
        }
    }

    private final String metalPrefix;
    private final String metalMaterial;
    private final String clothItem;
    private final double metalRatio;
    private final int totalMats;

    /**
     * @param metalPrefix   GT ore prefix of the metal part, for example {@code "plate"}; null for none
     * @param metalMaterial GT material name, for example {@code "Steel"}; null for none
     * @param clothItem     registry name of the cloth part (for example {@code "part_heavy_cloth"})
     * @param metalRatio    share of the parts that is metal, 0..1; ignored when there is no metal
     * @param totalMats     parts needed to repair a piece with one durability point left
     */
    public RepairSpec(String metalPrefix, String metalMaterial, String clothItem, double metalRatio, int totalMats) {
        this.metalPrefix = metalPrefix;
        this.metalMaterial = metalMaterial;
        this.clothItem = clothItem;
        this.metalRatio = metalPrefix == null ? 0.0 : metalRatio;
        this.totalMats = totalMats;
    }

    /** @return GT ore prefix of the metal, or null */
    public String metalPrefix() {
        return metalPrefix;
    }

    /** @return GT material name of the metal, or null */
    public String metalMaterial() {
        return metalMaterial;
    }

    /** @return registry name of the cloth part */
    public String clothItem() {
        return clothItem;
    }

    /** @return metal share, 0..1 */
    public double metalRatio() {
        return metalRatio;
    }

    /** @return parts for a fully worn piece */
    public int totalMats() {
        return totalMats;
    }

    /**
     * The repair cost: {@code count = ceil(total * damage / (max - 1))}, {@code metal = ceil(count * ratio)},
     * {@code cloth = count - metal}. A piece with no damage costs nothing.
     *
     * @param damage    current damage of the stack
     * @param maxDamage the piece's durability
     * @return the parts to consume
     */
    public Cost cost(int damage, int maxDamage) {
        if (damage <= 0 || maxDamage <= 1) {
            return new Cost(0, 0);
        }
        double fraction = Math.min(1.0, (double) damage / (maxDamage - 1));
        int count = (int) Math.ceil(totalMats * fraction - 1e-9);
        int metal = (int) Math.ceil(count * metalRatio - 1e-9);
        return new Cost(metal, count - metal);
    }
}
