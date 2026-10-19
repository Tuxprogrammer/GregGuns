package com.gregguns.gun.logic;

/**
 * Pure arithmetic for energy guns: shots left, the charge bar and the default capacities. EU are {@code long}.
 *
 * <p>
 * Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-13, GGS-02-14), design 0003 and 0001.
 */
public final class EnergyMath {

    /**
     * Default capacity in EU of a gun of each GT tier index (index 0 = ULV, 1 = LV ... 7 = ZPM): the capacity of the
     * GT item the spec names as the baseline (Lithium batteries LV to HV, an EV battery hull, then Lapotronic Orb,
     * Lapotronic Orb II and the Energy Module). ULV has no energy guns and holds 0.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/MetaGeneratedItem01.java#L5208-L5246
    private static final long[] DEFAULT_CAPACITY_BY_TIER_INDEX = { 0L, 100_000L, 400_000L, 1_600_000L, 6_400_000L,
        100_000_000L, 1_000_000_000L, 10_000_000_000L };

    private EnergyMath() {}

    /** Default capacity for a GT tier index (LV = 1 to ZPM = 7); 0 for an index outside that table. */
    public static long defaultCapacityEu(int gtTierIndex) {
        if (gtTierIndex < 0 || gtTierIndex >= DEFAULT_CAPACITY_BY_TIER_INDEX.length) {
            return 0L;
        }
        return DEFAULT_CAPACITY_BY_TIER_INDEX[gtTierIndex];
    }

    /**
     * EU per shot that makes a full {@code capacityEu} last {@code rounds} shots (Q9: capacity / rounds), at least 1.
     */
    public static long euPerShotFor(long capacityEu, int rounds) {
        return Math.max(1L, capacityEu / Math.max(1, rounds));
    }

    /** Whole shots a charge pays for. */
    public static long shotsLeft(long chargeEu, long euPerShot) {
        return euPerShot <= 0 ? 0L : Math.max(0L, chargeEu) / euPerShot;
    }

    /** An energy gun is "loaded" while its charge covers one shot. */
    public static boolean canFire(long chargeEu, long euPerShot) {
        return euPerShot > 0 && chargeEu >= euPerShot;
    }

    /** Charge as a fraction of capacity, clamped to 0 to 1. */
    public static double chargeFraction(long chargeEu, long capacityEu) {
        if (capacityEu <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) chargeEu / (double) capacityEu));
    }

    /** The HUD figure: charge as a whole percent, rounded down, 0 to 100. */
    public static int percent(long chargeEu, long capacityEu) {
        return (int) Math.floor(chargeFraction(chargeEu, capacityEu) * 100.0 + 1e-9);
    }

    /** The item bar fill in the same sense as {@link GunState#barFraction}: 0 full, 1 empty. */
    public static double barFraction(long chargeEu, long capacityEu) {
        return 1.0 - chargeFraction(chargeEu, capacityEu);
    }
}
