package com.gregguns.gun.logic;

/**
 * One shooter's hold-to-charge state: whether the charge button is down and for how many ticks. Server and client
 * mirror each keep one. The tick count is the only clock, so tests need no wall time.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (GenericGunCharge). Fixes GUN-02 (rounds never exceed what is
 * loaded) through {@link #release}.
 */
public final class ChargeState {

    private boolean charging;
    private int heldTicks;

    /** @return true while the charge button is held and accepted */
    public boolean isCharging() {
        return charging;
    }

    /** @return ticks held since {@link #begin}, capped at the gun's full-charge time by {@link #tick} */
    public int heldTicks() {
        return heldTicks;
    }

    /**
     * Starts a charge.
     *
     * @param allowed false when the gun is empty, cooling down or reloading: the press is ignored
     * @return true if charging started
     */
    public boolean begin(boolean allowed) {
        if (!allowed || charging) {
            return false;
        }
        charging = true;
        heldTicks = 0;
        return true;
    }

    /** Advances one tick; the hold time stops growing at a full charge. */
    public void tick(GunStats stats) {
        if (charging && heldTicks < stats.chargeFullTicks()) {
            heldTicks++;
        }
    }

    /** @return the current charge in 0..1, or 0 when not charging */
    public double fraction(GunStats stats) {
        return charging ? ChargeMath.fraction(heldTicks, stats.chargeFullTicks()) : 0.0;
    }

    /**
     * Ends the charge and works out the shot.
     *
     * @param stats     the gun; must be a charge gun
     * @param available rounds the shooter can spend ({@link Integer#MAX_VALUE} in creative); at least 1 to fire
     * @return the release, or {@code null} if nothing was charging or no round is available
     */
    public ChargeRelease release(GunStats stats, int available) {
        if (!charging) {
            return null;
        }
        double fraction = ChargeMath.fraction(heldTicks, stats.chargeFullTicks());
        cancel();
        if (available < 1) {
            return null;
        }
        int wanted = ChargeMath.wantedUnits(stats.chargeMode(), fraction, stats.chargeMaxUnits());
        return new ChargeRelease(fraction, wanted, Math.min(wanted, available));
    }

    /** Drops the charge without firing: weapon switch, death, empty gun. */
    public void cancel() {
        charging = false;
        heldTicks = 0;
    }
}
