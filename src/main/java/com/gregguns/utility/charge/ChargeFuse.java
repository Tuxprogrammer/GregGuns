package com.gregguns.utility.charge;

/**
 * The fuse clock: one step per server tick while armed. It beeps once per second and detonates at zero. Pure.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (Every tick while armed)
public final class ChargeFuse {

    /** What the tile does on a tick. */
    public enum Tick {
        /** Nothing. */
        NONE,
        /** Play the tick sound. */
        BEEP,
        /** Detonate now. */
        DETONATE
    }

    /** The result of one step. */
    public static final class Result {

        /** Ticks left after the step; never below zero. */
        public final int remaining;
        /** The action for this tick. */
        public final Tick tick;

        Result(int remaining, Tick tick) {
            this.remaining = remaining;
            this.tick = tick;
        }
    }

    private ChargeFuse() {}

    /**
     * @param fuse ticks left before this tick
     * @return the new count and the action: {@code BEEP} when the new count is above zero and divisible by 20,
     *         {@code DETONATE} when it reaches zero
     */
    public static Result advance(int fuse) {
        int next = Math.max(0, fuse - 1);
        if (next <= 0) {
            return new Result(0, Tick.DETONATE);
        }
        return new Result(next, next % 20 == 0 ? Tick.BEEP : Tick.NONE);
    }
}
