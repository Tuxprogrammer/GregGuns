package com.gregguns.machine.repair;

import com.gregguns.api.Tier;

import gregtech.api.enums.TierEU;

/**
 * Time, power and tier rules for repairs, as plain arithmetic. A repair takes {@code 2 s + 8 s x damage fraction} and
 * draws half the recipe voltage of the machine's tier; an item above the machine's tier is refused. Both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/machine-roster.md (Repair Bench EU and timing, GGS-08-50),
// docs/gregguns-spec/08-machines-production/multiblocks.md (Armory Gantry, process)
// Deviation: the sheets' "2 s plus 8 s times the damage fraction" is a duration. The bench is click-driven with no
// recipe, so it charges the whole job's energy (eut x duration) from its buffer at the click instead of running for
// that long. The Gantry runs the duration for real.
public final class RepairMath {

    /** The fixed part of a repair, in ticks (2 s). */
    public static final int BASE_TICKS = 40;
    /** The part that scales with the damage fraction, in ticks (8 s at fraction 1). */
    public static final int SCALED_TICKS = 160;

    private RepairMath() {}

    /**
     * @param damage    item damage (0 is new)
     * @param maxDamage the item's durability
     * @return {@code damage / (maxDamage - 1)} limited to 0..1; 0 when the item has no durability range
     */
    public static double damageFraction(int damage, int maxDamage) {
        if (damage <= 0 || maxDamage <= 1) {
            return 0.0;
        }
        return Math.min(1.0, (double) damage / (maxDamage - 1));
    }

    /**
     * @param fraction the (largest) damage fraction of the job, 0..1
     * @return the job's duration in ticks, {@code 40 + round(160 x fraction)}
     */
    public static int durationTicks(double fraction) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        return BASE_TICKS + (int) Math.round(SCALED_TICKS * clamped);
    }

    /**
     * @param tier a machine tier
     * @return GT's recipe voltage for the tier ({@code TierEU.RECIPE_*})
     */
    public static long recipeEut(Tier tier) {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java
        switch (tier) {
            case STEAM:
                return TierEU.RECIPE_ULV;
            case LV:
                return TierEU.RECIPE_LV;
            case MV:
                return TierEU.RECIPE_MV;
            case HV:
                return TierEU.RECIPE_HV;
            case EV:
                return TierEU.RECIPE_EV;
            case IV:
                return TierEU.RECIPE_IV;
            case LUV:
                return TierEU.RECIPE_LuV;
            case ZPM:
                return TierEU.RECIPE_ZPM;
            default:
                throw new IllegalArgumentException("tier " + tier);
        }
    }

    /**
     * @param tier a machine tier
     * @return the EU/t one repaired item draws: half the recipe voltage, at least 1
     */
    public static long euPerItemPerTick(Tier tier) {
        return Math.max(1L, recipeEut(tier) / 2);
    }

    /**
     * @param tier     the bench tier
     * @param fraction the item's damage fraction
     * @return the energy the bench charges for one item: {@code euPerItemPerTick x durationTicks}
     */
    public static long benchEu(Tier tier, double fraction) {
        return euPerItemPerTick(tier) * durationTicks(fraction);
    }

    /**
     * @param machine the machine's tier (the Gantry: its lowest Energy Hatch)
     * @param item    the item's tier
     * @return true when the machine may repair the item (machine tier at least the item's)
     */
    public static boolean tierAllows(Tier machine, Tier item) {
        return machine.compareTo(item) >= 0;
    }
}
