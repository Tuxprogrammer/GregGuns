package com.gregguns.projectile;

/**
 * The pure state of one shooter's continuous beam (the Nuclear Death Ray): when it started, when it was last
 * refreshed, and what each refresh should do. No Minecraft types; the caller passes the world tick.
 *
 * <p>
 * The beam lives {@link #LIFETIME} ticks and every shot while the trigger is held refreshes it, so it ends 10 ticks
 * after the trigger is released. A refresh reports whether the beam has just begun (play the start sound and the
 * muzzle FX), whether the loop sound is due again, and whether this tick may still deal damage: a second refresh in
 * the same tick (two guns, a repeated call) does not, which keeps it to one damage tick per entity per tick.
 *
 * <pre>
 * {@code
 * MovingBeam.Step step = beam.refresh(world.getTotalWorldTime());
 * if (step.dealsDamage()) { ... }
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (Nuclear Death Ray: BEAM_LIFETIME = 10);
// docs/techguns-spec/04-projectiles-fx/fx-catalog.md (BeamGunMuzzleFX, BeamGunImpactFX)
public final class MovingBeam {

    /** Ticks the beam outlives its last refresh. */
    public static final int LIFETIME = 10;
    /** Ticks between loop sound restarts: the loop sample is about one second long. */
    public static final int LOOP_TICKS = 20;
    /** How far the beam reaches each tick, blocks. */
    public static final double RANGE = 48.0;

    private boolean everRefreshed;
    private long lastRefresh;
    private long lastLoop;

    /** What one refresh asks of the caller. Immutable. */
    public static final class Step {

        private final boolean started;
        private final boolean loopDue;
        private final boolean dealsDamage;

        Step(boolean started, boolean loopDue, boolean dealsDamage) {
            this.started = started;
            this.loopDue = loopDue;
            this.dealsDamage = dealsDamage;
        }

        /** @return true when this refresh began a new beam: play the start sound and the muzzle FX */
        public boolean started() {
            return started;
        }

        /** @return true when the loop sound should be played again */
        public boolean loopDue() {
            return loopDue;
        }

        /** @return false when the beam already dealt its damage this tick */
        public boolean dealsDamage() {
            return dealsDamage;
        }
    }

    /**
     * @param now the world tick
     * @return true while the beam is alive: refreshed less than {@link #LIFETIME} ticks ago
     */
    public boolean isActive(long now) {
        return everRefreshed && now - lastRefresh < LIFETIME;
    }

    /**
     * Refreshes the beam for this tick.
     *
     * @param now the world tick; should not go backwards
     * @return what to do this tick
     */
    public Step refresh(long now) {
        boolean started = !isActive(now);
        if (started) {
            lastLoop = now;
        }
        boolean sameTick = !started && now == lastRefresh;
        boolean loopDue = !started && !sameTick && now - lastLoop >= LOOP_TICKS;
        if (loopDue) {
            lastLoop = now;
        }
        everRefreshed = true;
        lastRefresh = now;
        return new Step(started, loopDue, !sameTick);
    }
}
