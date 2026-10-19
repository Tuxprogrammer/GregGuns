package com.gregguns.gun.logic;

/**
 * What a released charge produced. Immutable. {@link #units()} is the rounds actually spent and, for a LEVELS gun,
 * also the level of the shot (GUN-02 fix: never more than was loaded).
 */
public final class ChargeRelease {

    private final double fraction;
    private final int wanted;
    private final int units;

    ChargeRelease(double fraction, int wanted, int units) {
        this.fraction = fraction;
        this.wanted = wanted;
        this.units = units;
    }

    /** @return the charge in 0..1 at release */
    public double fraction() {
        return fraction;
    }

    /** @return the rounds the charge asked for */
    public int wantedUnits() {
        return wanted;
    }

    /** @return the rounds spent, between 1 and {@link #wantedUnits()}; also the shot's level for a LEVELS gun */
    public int units() {
        return units;
    }

    /** @return true if the shooter lacked the rounds the charge asked for */
    public boolean limited() {
        return units < wanted;
    }
}
