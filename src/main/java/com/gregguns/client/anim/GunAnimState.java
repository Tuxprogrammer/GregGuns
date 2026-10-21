package com.gregguns.client.anim;

/**
 * The client animation record of one shooter (one player or mob): recoil, muzzle-flash and reload windows, the
 * attack type, and which gun they belong to. Pure; the owner supplies {@code now} as {@code tick + partialTick}.
 *
 * <p>
 * Every window is tied to a gun tag (the registry id of the item). Reading a window with a different tag returns
 * {@link AnimWindow#NONE}, so switching guns during a flash never draws it on the new gun (GVA-19).
 *
 * <p>
 * Spec: rendering-framework.md section 3; BUGS GVA-03 (server echo restarts recoil) and GVA-19 (global flash timer).
 */
public final class GunAnimState {

    /** A server echo within this many ticks of a predicted shot is ignored (GVA-03). Latency is far below it. */
    public static final double ECHO_GRACE_TICKS = 40.0;
    /** The rotor is driven while a shot arrived within this many ticks. */
    public static final int SPIN_DRIVE_TICKS = 2;

    private final AnimWindow recoil = new AnimWindow();
    private final AnimWindow flash = new AnimWindow();
    private final AnimWindow reload = new AnimWindow();
    private int attackType;
    private int tag = -1;
    private double lastPredictedShot = Double.NEGATIVE_INFINITY;
    private double lastShot = Double.NEGATIVE_INFINITY;
    /** The rotor's spin, only for a gun that has one; null otherwise. */
    private RotorSpin spin;
    private int spinTag = -1;
    private boolean shotSinceSpinTick;
    /** Trigger-held input for the rotor, set only for the local shooter; null means the rotor follows shots. */
    private Boolean triggerInput;
    private int ticksSinceSpinShot;

    /**
     * Records a shot.
     *
     * @param now         {@code tick + partialTick}
     * @param recoilTicks recoil length, 0 for none
     * @param flashTicks  muzzle flash length, 0 for none
     * @param attackType  0 normally; the gun index for alternating akimbo guns
     * @param origin      see {@link ShotOrigin}
     * @param gunTag      the registry id of the gun that fired
     * @return false if the shot was an ignorable server echo
     */
    public boolean shot(double now, int recoilTicks, int flashTicks, int attackType, ShotOrigin origin, int gunTag) {
        return shot(now, recoilTicks, flashTicks, attackType, origin, gunTag, false);
    }

    /**
     * Records a shot, optionally keeping a recoil window that is still running (Techguns' {@code checkRecoil}: the
     * PDW and Minigun fire faster than their recoil, and restarting every shot would freeze the animation at its
     * start; the Minigun's rotor would never turn).
     *
     * @param checkRecoil true to leave a running recoil window of the same gun alone
     * @return false if the shot was an ignorable server echo
     */
    public boolean shot(double now, int recoilTicks, int flashTicks, int attackType, ShotOrigin origin, int gunTag,
        boolean checkRecoil) {
        if (origin == ShotOrigin.SERVER_ECHO && now - lastPredictedShot < ECHO_GRACE_TICKS) {
            return false;
        }
        if (origin == ShotOrigin.PREDICTED) {
            lastPredictedShot = now;
        }
        lastShot = now;
        boolean keepRecoil = checkRecoil && tag == gunTag && recoil.isActive(now);
        tag = gunTag;
        this.attackType = attackType;
        if (!keepRecoil) {
            recoil.begin(now, recoilTicks);
        }
        flash.begin(now, flashTicks);
        return true;
    }

    /**
     * Notes that this shooter fired a gun with a spinning rotor, so the rotor keeps spinning up. Switching to another
     * spinning gun starts that gun's rotor from rest (spin never carries between guns).
     *
     * @param spec   the gun's rotor numbers
     * @param gunTag the registry id of the gun that fired
     */
    public void spinDriven(RotorSpin.Spec spec, int gunTag) {
        if (spin == null || spinTag != gunTag) {
            spin = spec.create();
            spinTag = gunTag;
        }
        shotSinceSpinTick = true;
    }

    /**
     * Drives the rotor from the trigger instead of from shots, so the barrels spin up before the first shot leaves
     * and coast down after the last (the local shooter, whose client mirror runs the same spin gate as the server).
     * Remote shooters have no trigger signal and keep following shots.
     *
     * @param spec   the gun's rotor numbers
     * @param gunTag the registry id of the held gun
     * @param drive  true while the trigger is held and the gun is not reloading
     */
    public void spinTrigger(RotorSpin.Spec spec, int gunTag, boolean drive) {
        if (spin == null || spinTag != gunTag) {
            if (!drive) {
                return;
            }
            spin = spec.create();
            spinTag = gunTag;
        }
        triggerInput = drive;
    }

    /**
     * Advances the rotor one client tick. The rotor is driven while a shot arrived within the last
     * {@link #SPIN_DRIVE_TICKS} ticks (a shot per tick, with one tick of slack for a late packet).
     *
     * @param holdsSpinGun false if the shooter no longer holds the gun that spins: the rotor stops at once
     */
    public void spinTick(boolean holdsSpinGun) {
        if (spin == null) {
            return;
        }
        if (!holdsSpinGun) {
            spinReset();
            return;
        }
        if (triggerInput != null) {
            shotSinceSpinTick = false;
            spin.tick(triggerInput);
            return;
        }
        if (shotSinceSpinTick) {
            ticksSinceSpinShot = 0;
            shotSinceSpinTick = false;
        } else if (ticksSinceSpinShot < SPIN_DRIVE_TICKS) {
            ticksSinceSpinShot++;
        }
        spin.tick(ticksSinceSpinShot < SPIN_DRIVE_TICKS);
    }

    /** Stops the rotor at angle 0 (a reload began, or the gun was put away). */
    public void spinReset() {
        if (spin != null) {
            spin.reset();
        }
        shotSinceSpinTick = false;
        ticksSinceSpinShot = SPIN_DRIVE_TICKS;
        triggerInput = null;
    }

    /** @return the gun tag whose rotor this record spins, or -1 */
    public int spinTag() {
        return spin == null ? -1 : spinTag;
    }

    /**
     * @param gunTag  the registry id of the gun being drawn
     * @param partial the partial tick of the frame
     * @return the rotor angle in turns (0 to below 1); 0 at rest, and for another gun than the one that spins
     */
    public double rotorTurns(int gunTag, double partial) {
        return spin == null || spinTag != gunTag ? 0.0 : spin.turns(partial);
    }

    /** Starts a reload. Replaces a running one. */
    public void reloadStarted(double now, int durationTicks, int gunTag) {
        tag = gunTag;
        reload.begin(now, durationTicks);
    }

    /** Cancels a running reload (the gun was switched, dropped or the player died). */
    public void reloadCanceled() {
        reload.clear();
    }

    /** @return recoil progress in [0, 1), or {@link AnimWindow#NONE}; also NONE for another gun's window */
    public double recoilProgress(double now, int gunTag) {
        return tag == gunTag ? recoil.progress(now) : AnimWindow.NONE;
    }

    /** @return muzzle-flash progress in [0, 1), or {@link AnimWindow#NONE}; also NONE for another gun's window */
    public double flashProgress(double now, int gunTag) {
        return tag == gunTag ? flash.progress(now) : AnimWindow.NONE;
    }

    /** @return reload progress in [0, 1), or {@link AnimWindow#NONE}; also NONE for another gun's window */
    public double reloadProgress(double now, int gunTag) {
        return tag == gunTag ? reload.progress(now) : AnimWindow.NONE;
    }

    /** @return the attack type of the last shot (0 unless an akimbo gun set it) */
    public int attackType() {
        return attackType;
    }

    /** @return ticks since the last shot (any origin), or a huge number if none */
    public double ticksSinceShot(double now) {
        return now - lastShot;
    }

    /** @return true when nothing is running and no recent shot needs remembering; the owner may drop the record */
    public boolean isIdle(double now) {
        return !recoil.isActive(now) && !flash.isActive(now)
            && !reload.isActive(now)
            && now - lastPredictedShot >= ECHO_GRACE_TICKS
            && (spin == null || spin.atRest());
    }
}
