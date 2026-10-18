package com.gregguns.gun.logic;

/**
 * Where one projectile of a shot goes, relative to the shooter's look direction. Immutable.
 */
public final class ShotOffset {

    private final double yawDegrees;
    private final double pitchDegrees;
    private final double forwardTickFraction;
    private final boolean leftHand;

    ShotOffset(double yawDegrees, double pitchDegrees, double forwardTickFraction, boolean leftHand) {
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
        this.forwardTickFraction = forwardTickFraction;
        this.leftHand = leftHand;
    }

    /** Offset added to the shooter's yaw, in degrees. */
    public double yawDegrees() {
        return yawDegrees;
    }

    /** Offset added to the shooter's pitch, in degrees. */
    public double pitchDegrees() {
        return pitchDegrees;
    }

    /**
     * Fraction of one tick's travel to shift the projectile forward, for in-line pellets (0 for everything else).
     */
    public double forwardTickFraction() {
        return forwardTickFraction;
    }

    /** True if the projectile spawns from the left hand (akimbo). */
    public boolean leftHand() {
        return leftHand;
    }
}
