package com.gregguns.icbm.emp;

/**
 * The numbers and gates of the EMP tower and the EMP warhead (D16): the EU cost as a continuous function of the
 * radius, the radius clamp, the gate every way of firing shares, and the pick of one maintenance issue. No Minecraft
 * types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md
public final class EmpRules {

    /** The smallest radius a pulse may have, blocks. */
    public static final int MIN_RADIUS = 4;
    /** Ticks the tower refuses another pulse after one (ICBM's 10 seconds). */
    public static final int COOLDOWN_TICKS = 200;
    /**
     * EU per square block of radius. ICBM charged 100 FE per square block; four FE to the EU gives 25 (the same
     * conversion as the Launch Control's shot).
     */
    public static final double EU_PER_AREA = 25.0;
    /**
     * How many maintenance issues a multiblock can have (wrench, screwdriver, soft mallet, hard hammer, solder,
     * crowbar).
     */
    public static final int MAINTENANCE_KINDS = 6;

    private EmpRules() {}

    /**
     * Clamps a requested radius on both sides. ICBM clamped the typed range only from above, so a client could send a
     * negative or zero one (ICBM-05-11).
     *
     * @param radius the requested radius, blocks
     * @param max    the configured maximum, blocks
     * @return the radius from {@link #MIN_RADIUS} to {@code max}
     */
    public static int clampRadius(int radius, int max) {
        return Math.max(MIN_RADIUS, Math.min(Math.max(MIN_RADIUS, max), radius));
    }

    /**
     * The EU one pulse costs. Continuous in the radius: the radius is squared as a real number, never divided as an
     * integer first (1.6.4 and 1.7 did, so every radius under the maximum cost the minimum; D16).
     *
     * @param radius the pulse radius, blocks
     * @return EU, rounded up
     */
    public static long euCost(double radius) {
        double r = Math.max(0.0, radius);
        return (long) Math.ceil(r * r * EU_PER_AREA);
    }

    /** Why a pulse is allowed or refused. */
    public enum Gate {

        /** Every check passed. */
        READY("gregguns.icbm.emp.status.ready", true),
        /** The ICBM module is off. */
        MODULE_OFF("gregguns.icbm.emp.status.off", false),
        /** The tower is still cooling down from its last pulse. */
        COOLING("gregguns.icbm.emp.status.cooling", false),
        /** The tower holds less than the pulse costs. */
        NO_POWER("gregguns.icbm.emp.status.power", false);

        private final String langKey;
        private final boolean good;

        Gate(String langKey, boolean good) {
            this.langKey = langKey;
            this.good = good;
        }

        /** @return the lang key of the status line */
        public String langKey() {
            return langKey;
        }

        /** @return true when the pulse may go */
        public boolean isGood() {
            return good;
        }
    }

    /**
     * The one gate every way of firing passes: the button, redstone, a cover and a computer (GGI-01-04).
     *
     * @param moduleActive whether the ICBM module is on
     * @param coolingDown  whether the cooldown is still running
     * @param storedEu     EU in the tower
     * @param costEu       EU the pulse costs
     * @return the first failing check, or {@link Gate#READY}
     */
    public static Gate gate(boolean moduleActive, boolean coolingDown, long storedEu, long costEu) {
        if (!moduleActive) {
            return Gate.MODULE_OFF;
        }
        if (coolingDown) {
            return Gate.COOLING;
        }
        return storedEu < costEu ? Gate.NO_POWER : Gate.READY;
    }

    /**
     * Picks which maintenance issue to cause, among those the multiblock does not have yet, so a pulse always adds
     * one while any is left to add.
     *
     * @param workingMask bit i set when part i is still in order (bits 0 to 5)
     * @param roll        a random integer, any value (a negative one is fine)
     * @return the part to break, 0 to 5, or -1 when every part is already broken
     */
    public static int pickIssue(int workingMask, int roll) {
        int mask = workingMask & ((1 << MAINTENANCE_KINDS) - 1);
        int count = Integer.bitCount(mask);
        if (count == 0) {
            return -1;
        }
        int nth = Math.floorMod(roll, count);
        for (int i = 0; i < MAINTENANCE_KINDS; i++) {
            if ((mask & (1 << i)) != 0 && nth-- == 0) {
                return i;
            }
        }
        return -1;
    }
}
