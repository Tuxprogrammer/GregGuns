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

    private final AnimWindow recoil = new AnimWindow();
    private final AnimWindow flash = new AnimWindow();
    private final AnimWindow reload = new AnimWindow();
    private int attackType;
    private int tag = -1;
    private double lastPredictedShot = Double.NEGATIVE_INFINITY;
    private double lastShot = Double.NEGATIVE_INFINITY;

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
        if (origin == ShotOrigin.SERVER_ECHO && now - lastPredictedShot < ECHO_GRACE_TICKS) {
            return false;
        }
        if (origin == ShotOrigin.PREDICTED) {
            lastPredictedShot = now;
        }
        lastShot = now;
        tag = gunTag;
        this.attackType = attackType;
        recoil.begin(now, recoilTicks);
        flash.begin(now, flashTicks);
        return true;
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
            && now - lastPredictedShot >= ECHO_GRACE_TICKS;
    }
}
