package com.gregguns.gun.logic;

/**
 * Fuel and air drain of a tool gun, as pure arithmetic. The gun's rounds are whole numbers, but an action may cost a
 * fraction of one (the Chainsaw's 300 rounds over a 1,000 mB tank is 3.33 mB a round; a cheap action may cost half a
 * round). The fraction already used from the current round is the <em>remainder</em>, in thousandths, stored in the
 * stack's NBT, so rounding never short-changes a tank. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/02-guns/fluid-ammo.md ("Fractional mB are carried in the gun's NBT as a remainder")
public final class ToolDrain {

    /** Thousandths in a round. */
    public static final int MILLI = 1000;

    private ToolDrain() {}

    /** The outcome of one {@link #spend}. */
    public static final class Spend {

        private final boolean paid;
        private final int rounds;
        private final int remainderMilli;

        private Spend(boolean paid, int rounds, int remainderMilli) {
            this.paid = paid;
            this.rounds = rounds;
            this.remainderMilli = remainderMilli;
        }

        /** @return true when there was a round to pay with; false means the gun was empty and nothing changed */
        public boolean paid() {
            return paid;
        }

        /** @return whole rounds to remove from the gun */
        public int rounds() {
            return rounds;
        }

        /** @return the remainder to store, 0 to 999 */
        public int remainderMilli() {
            return remainderMilli;
        }
    }

    /**
     * Spends part of a round. The action is paid whenever at least one round is loaded; the last round is consumed
     * whole, and the remainder is then 0.
     *
     * @param loaded         rounds loaded
     * @param remainderMilli the stored remainder, thousandths of the current round already used (out-of-range values
     *                       are clamped)
     * @param costMilli      the action's cost in thousandths of a round (0 or less costs nothing)
     * @return the result
     */
    public static Spend spend(int loaded, int remainderMilli, int costMilli) {
        int rem = Math.max(0, Math.min(MILLI - 1, remainderMilli));
        if (loaded <= 0) {
            return new Spend(false, 0, 0);
        }
        if (costMilli <= 0) {
            return new Spend(true, 0, rem);
        }
        long total = (long) rem + costMilli;
        long whole = total / MILLI;
        if (whole >= loaded) {
            return new Spend(true, loaded, 0);
        }
        return new Spend(true, (int) whole, (int) (total % MILLI));
    }

    /**
     * The throttle of the swing sound.
     *
     * @param lastTick the world tick of the previous swing sound, or {@link LoopStart#NEVER}
     * @param nowTick  the world tick of this swing
     * @param gapTicks the minimum gap
     * @return true when this swing may play its sound
     */
    public static boolean swingSoundAllowed(long lastTick, long nowTick, int gapTicks) {
        return nowTick - lastTick >= gapTicks;
    }
}
