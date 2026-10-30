package com.gregguns.client.anim;

/**
 * Decides when a spinning rotor's wind-up and wind-down sounds start and stop, from the rotor's speed alone (a pure
 * state machine, one per shooter, fed once per client tick with the speed as a fraction of full speed, 0 to 1).
 *
 * <ul>
 * <li>{@link Cue#SPIN_UP}: the rotor began to speed up from rest, or from a wind-down.</li>
 * <li>{@link Cue#SPIN_DOWN}: the rotor began to slow down from full speed or from a wind-up.</li>
 * <li>{@link Cue#STOP}: whatever is playing must end now (the rotor was stopped dead: gun put away, reload begun,
 * shooter gone; or it came back to full speed while a wind-down was playing).</li>
 * </ul>
 *
 * A normal coast to rest gives no cue: the wind-down sound ends by itself. Spec: docs/gregguns-spec/02-guns/index.md
 * (Minigun, spin sounds).
 */
public final class RotorSoundCue {

    /** What the sound side should do this tick. */
    public enum Cue {
        NONE,
        SPIN_UP,
        SPIN_DOWN,
        STOP
    }

    private enum Phase {
        IDLE,
        UP,
        FULL,
        DOWN
    }

    private static final double EPS = 1e-9;
    /** A fall this big in one tick is a dead stop, not a coast (a coast loses 1/downTicks per tick, 5% or less). */
    private static final double DEAD_STOP = 0.2;

    private Phase phase = Phase.IDLE;
    private double last;

    /**
     * Advances one tick.
     *
     * @param fraction the rotor speed over its full speed, 0 to 1
     * @return the cue for this tick
     */
    public Cue next(double fraction) {
        double prev = last;
        last = fraction;
        if (fraction <= EPS) {
            boolean dead = phase != Phase.IDLE && prev - fraction > DEAD_STOP;
            phase = Phase.IDLE;
            return dead ? Cue.STOP : Cue.NONE;
        }
        if (fraction > prev + EPS) {
            if (fraction >= 1.0 - EPS) {
                Phase before = phase;
                phase = Phase.FULL;
                return before == Phase.DOWN ? Cue.STOP : Cue.NONE;
            }
            if (phase != Phase.UP) {
                phase = Phase.UP;
                return Cue.SPIN_UP;
            }
            return Cue.NONE;
        }
        if (fraction < prev - EPS) {
            if (phase != Phase.DOWN) {
                phase = Phase.DOWN;
                return Cue.SPIN_DOWN;
            }
            return Cue.NONE;
        }
        if (fraction >= 1.0 - EPS) {
            phase = Phase.FULL;
        }
        return Cue.NONE;
    }

    /** @return the speed fraction of the last tick */
    public double level() {
        return last;
    }
}
