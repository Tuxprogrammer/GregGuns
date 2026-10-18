package com.gregguns.gun.logic;

/** The outcome of one fire attempt. Immutable. */
public final class ShotResult {

    /** What happened. */
    public enum Outcome {
        /** The trigger did not ask for a shot this tick. */
        NONE,
        /** A shot was wanted but the cooldown has not elapsed. */
        COOLDOWN,
        /** A shot was wanted but too few rounds are loaded. Reported once per trigger pull (GUN-11). */
        EMPTY,
        /** A shot was fired: rounds were spent and the glue spawns the projectiles. */
        FIRED
    }

    private static final ShotResult NONE = new ShotResult(Outcome.NONE, 0, 0);
    private static final ShotResult COOLDOWN = new ShotResult(Outcome.COOLDOWN, 0, 0);
    private static final ShotResult EMPTY = new ShotResult(Outcome.EMPTY, 0, 0);

    private final Outcome outcome;
    private final int attackType;
    private final int roundsUsed;

    private ShotResult(Outcome outcome, int attackType, int roundsUsed) {
        this.outcome = outcome;
        this.attackType = attackType;
        this.roundsUsed = roundsUsed;
    }

    static ShotResult none() {
        return NONE;
    }

    static ShotResult cooldown() {
        return COOLDOWN;
    }

    static ShotResult empty() {
        return EMPTY;
    }

    static ShotResult fired(int attackType, int roundsUsed) {
        return new ShotResult(Outcome.FIRED, attackType, roundsUsed);
    }

    /** What happened. */
    public Outcome outcome() {
        return outcome;
    }

    /** True if a shot was fired. */
    public boolean wasFired() {
        return outcome == Outcome.FIRED;
    }

    /** Hand for alternating akimbo guns: 0 right, 1 left. Always 0 for other guns. Meaningful when fired. */
    public int attackType() {
        return attackType;
    }

    /** Rounds taken from the gun by this shot (0 for infinite ammo). */
    public int roundsUsed() {
        return roundsUsed;
    }
}
