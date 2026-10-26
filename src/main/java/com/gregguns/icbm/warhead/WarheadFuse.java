package com.gregguns.icbm.warhead;

/**
 * The fuse of a placed warhead, as pure logic: an int tick count (ICBM's byte overflowed above 127), one step per
 * server tick. Reuses the shape of the explosive charge fuse: a beep each second and a detonation at zero.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Use as a demolition charge; GGS-ICBM-02-14)
public final class WarheadFuse {

    /** Seconds a hand-lit fuse gives to react, before the radius term (D51). */
    public static final int BASE_SECONDS = 3;
    /** Blocks of blast radius that add one second to a hand-lit fuse (D51). */
    public static final double BLOCKS_PER_SECOND = 3.0;
    /** The fuse of a warhead primed by a nearby explosion. */
    public static final int CHAIN_TICKS = 10;

    /** What a step does. */
    public enum Action {
        /** Nothing. */
        NONE,
        /** Beep. */
        BEEP,
        /** Go off now. */
        DETONATE
    }

    /** The result of one step. */
    public static final class Step {

        /** Ticks left. */
        public final int remaining;
        /** What to do this tick. */
        public final Action action;

        Step(int remaining, Action action) {
            this.remaining = remaining;
            this.action = action;
        }
    }

    private WarheadFuse() {}

    /**
     * The fuse of a warhead lit by redstone or by hand (D51): {@code 20 * (3 + ceil(radius / 3))} ticks, so a player
     * can outrun the blast. At least one tick, whatever the multiplier.
     *
     * @param radius     the warhead's actual blast radius at its fill, blocks
     * @param multiplier the {@code icbm.fuseMultiplier} config value
     * @return the fuse in ticks
     */
    public static int litTicks(double radius, double multiplier) {
        int seconds = BASE_SECONDS + (int) Math.ceil(Math.max(0.0, radius) / BLOCKS_PER_SECOND);
        return Math.max(1, (int) Math.round(20.0 * seconds * Math.max(0.0, multiplier)));
    }

    /**
     * @param fuse ticks left before this tick
     * @return the new count and the action
     */
    public static Step advance(int fuse) {
        int next = Math.max(0, fuse - 1);
        if (next == 0) {
            return new Step(0, Action.DETONATE);
        }
        return new Step(next, next % 20 == 0 ? Action.BEEP : Action.NONE);
    }

    /**
     * @param current the current fuse, 0 when not armed
     * @param ticks   a requested fuse
     * @return the fuse to use: an armed warhead never gets a longer fuse than it has
     */
    public static int arm(int current, int ticks) {
        return current <= 0 ? ticks : Math.min(current, ticks);
    }
}
