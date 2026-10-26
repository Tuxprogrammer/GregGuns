package com.gregguns.gun.logic;

/**
 * Per-player composition of {@link FireControl}, {@link ReloadStateMachine} and {@link ZoomState}: the one object
 * the item glue talks to.
 *
 * <h2>Lifetime and sides</h2>
 * Create one per player on the <b>server</b>; discard it on logout. A client may keep a mirror for prediction (muzzle
 * flash, recoil, the reload animation), but only the server's controller changes the stack, and the client takes the
 * synced tag back. Nothing here reads a client-supplied ammo count or zoom flag.
 *
 * <h2>Per tick (server)</h2>
 * 
 * <pre>
 * GunState state = GunNbt.read(stack.getTagCompound(), stats);
 * GunTickResult r = controller.tick(stats, state, inventoryAdapter, isMainHandHeld, creative);
 * GunNbt.write(tag, state);          // when anything changed
 * if (r.shot().wasFired()) { roll offsets with SpreadModel, spawn projectiles, send the fired message }
 * switch (r.reload()) { STARTED: play sound, send reload message; ... }
 * </pre>
 * 
 * Call it every server tick while the player carries the gun, passing {@code held=false} when it is in the
 * inventory but not the main-hand item. When the stack leaves the inventory entirely, call {@link #reset}.
 *
 * <h2>From packets (server, after validation)</h2>
 * <ul>
 * <li>trigger down / up: {@link #triggerPressed}, {@link #triggerReleased}</li>
 * <li>client-predicted shot: {@link #requestShot} (instead of the trigger calls, never both)</li>
 * <li>right-click: {@link #toggleZoom}</li>
 * <li>force reload key: {@link #forceReload}</li>
 * </ul>
 *
 * <p>
 * Spec: gun-lifecycle.md sections 4, 5, 7.
 */
public final class GunController {

    private final FireControl fire;
    private final ReloadStateMachine reload = new ReloadStateMachine();
    private final ZoomState zoom = new ZoomState();
    private SpinGate spinGate;

    /** Creates a controller with default buffer and grace windows. */
    public GunController() {
        this(new FireControl());
    }

    /** Creates a controller around a configured {@link FireControl}. */
    public GunController(FireControl fire) {
        this.fire = fire;
    }

    /** The fire control, for read access to the cooldown. */
    public FireControl fire() {
        return fire;
    }

    /** The reload machine, for read access to progress. */
    public ReloadStateMachine reload() {
        return reload;
    }

    /** The zoom state. */
    public ZoomState zoom() {
        return zoom;
    }

    /** @return the spin gate of the last spinning gun ticked (null until one is), for read access to the spin */
    public SpinGate spinGate() {
        return spinGate;
    }

    /**
     * The spin of a gun with a spin-up, for the animation; 0 for any other gun.
     *
     * @param stats the held gun
     */
    public double spin(GunStats stats) {
        return stats.hasSpinUp() && spinGate != null ? spinGate.spin() : 0.0;
    }

    /** Validated "trigger down" input. */
    public void triggerPressed() {
        fire.triggerPressed();
    }

    /** Validated "trigger up" input. */
    public void triggerReleased() {
        fire.triggerReleased();
    }

    /**
     * Validated right-click on a gun. Refused while reloading (a reload resets zoom).
     *
     * @return true if the zoom state changed and the client should be told
     */
    public boolean toggleZoom(GunStats stats) {
        return !reload.isReloading() && zoom.toggle(stats);
    }

    /**
     * Validated right-click on a gun that may also aim down its sights. Refused while reloading.
     *
     * @param adsCapable true if the gun is on the {@link AdsSupport} list and ADS is enabled
     * @return true if the aim state changed and the client should be told
     */
    public boolean toggleAim(GunStats stats, boolean adsCapable) {
        return !reload.isReloading() && zoom.toggle(stats, adsCapable);
    }

    /**
     * Validated force-reload key. Does something only if the cooldown has elapsed and the gun is not full.
     *
     * @return the reload event ({@code STARTED} or a {@code REJECTED_*})
     */
    public ReloadStateMachine.Event forceReload(GunStats stats, GunState state, AmmoInventory ammo) {
        if (stats.isEnergy()) {
            return ReloadStateMachine.Event.NONE; // Q5: the reload key does nothing on an energy gun
        }
        if (!fire.isReady()) {
            return ReloadStateMachine.Event.REJECTED_BUSY;
        }
        return startReload(stats, state, ammo);
    }

    /**
     * Handles a client-predicted shot packet. Cancels a running reload if the gun still holds a full shot, because
     * firing interrupts a top-up; otherwise the shot is refused while reloading.
     */
    public GunTickResult requestShot(GunStats stats, GunState state, AmmoInventory ammo, boolean infiniteAmmo) {
        if (stats.hasSpinUp() && !gateFor(stats).isReadyWithGrace(fire.latencyGraceTicks())) {
            // Spin-up gun: a request before the barrel is (nearly) at speed is refused, with the latency forgiven.
            return new GunTickResult(ShotResult.cooldown(), ReloadStateMachine.Event.NONE, false);
        }
        if (stats.isEnergy()) {
            ShotResult shot = fire.requestEnergyShot(stats, ammo.charge(), infiniteAmmo);
            return new GunTickResult(shot, ReloadStateMachine.Event.NONE, shot.outcome() == ShotResult.Outcome.EMPTY);
        }
        ReloadStateMachine.Event ev = ReloadStateMachine.Event.NONE;
        if (reload.isReloading()) {
            if (!state.canFire(stats)) {
                return new GunTickResult(ShotResult.none(), ev, false);
            }
            ev = reload.cancel(ReloadStateMachine.CancelReason.FIRE);
        }
        ShotResult shot = fire.requestShot(stats, state, infiniteAmmo);
        boolean dry = false;
        if (shot.outcome() == ShotResult.Outcome.EMPTY) {
            ev = startReload(stats, state, ammo);
            dry = ev == ReloadStateMachine.Event.REJECTED_NO_AMMO;
        }
        return new GunTickResult(shot, ev, dry);
    }

    /**
     * One server tick.
     *
     * @param stats        the gun in the player's inventory
     * @param state        its state, mutated in place
     * @param ammo         the shooter's ammo source
     * @param held         true if the gun is the main-hand item this tick
     * @param infiniteAmmo true for creative players: no rounds are spent
     */
    public GunTickResult tick(GunStats stats, GunState state, AmmoInventory ammo, boolean held, boolean infiniteAmmo) {
        fire.tick();
        if (stats.isEnergy()) {
            return tickEnergy(stats, ammo.charge(), held, infiniteAmmo);
        }
        if (!held) {
            ReloadStateMachine.Event ev = reload.cancel(ReloadStateMachine.CancelReason.WEAPON_SWITCH);
            zoom.reset();
            fire.clearInput();
            resetSpin();
            return new GunTickResult(ShotResult.none(), ev, false);
        }
        ReloadStateMachine.Event ev = ReloadStateMachine.Event.NONE;
        boolean spinReady = true;
        if (stats.hasSpinUp()) {
            // The barrel turns while the trigger is held, even on an empty gun; a reload stops it dead.
            SpinGate gate = gateFor(stats);
            if (reload.isReloading()) {
                gate.reset();
            } else {
                gate.tick(fire.isTriggerHeld());
            }
            spinReady = gate.isReady();
        }
        if (reload.isReloading()) {
            if (spinReady && fire.wantsShot(stats) && state.canFire(stats)) {
                ev = reload.cancel(ReloadStateMachine.CancelReason.FIRE);
            } else {
                ev = reload.tick(stats, state, ammo);
                return new GunTickResult(ShotResult.none(), ev, false);
            }
        }
        if (!spinReady) {
            fire.discardBufferedPress();
            return new GunTickResult(ShotResult.none(), ev, false);
        }
        ShotResult shot = fire.step(stats, state, infiniteAmmo);
        boolean dry = false;
        if (shot.outcome() == ShotResult.Outcome.EMPTY) {
            ev = startReload(stats, state, ammo);
            dry = ev == ReloadStateMachine.Event.REJECTED_NO_AMMO;
        }
        return new GunTickResult(shot, ev, dry);
    }

    /**
     * One server tick of an energy gun: no reload phase, the shot spends EU from {@code charge}. An empty battery
     * reports {@code outOfAmmo} once per trigger pull (the dry click) and never starts a reload.
     */
    private GunTickResult tickEnergy(GunStats stats, ChargeSource charge, boolean held, boolean infiniteCharge) {
        if (!held) {
            zoom.reset();
            fire.clearInput();
            return new GunTickResult(ShotResult.none(), ReloadStateMachine.Event.NONE, false);
        }
        ShotResult shot = fire.stepEnergy(stats, charge, infiniteCharge);
        return new GunTickResult(shot, ReloadStateMachine.Event.NONE, shot.outcome() == ShotResult.Outcome.EMPTY);
    }

    /** Drops every transient state: the gun left the inventory, the player died or logged out. */
    public void reset() {
        reload.cancel(ReloadStateMachine.CancelReason.OTHER);
        zoom.reset();
        fire.clearInput();
        resetSpin();
    }

    private SpinGate gateFor(GunStats stats) {
        if (spinGate == null || spinGate.upTicks() != stats.spinUpTicks()
            || spinGate.downTicks() != stats.spinDownTicks()) {
            spinGate = new SpinGate(stats.spinUpTicks(), stats.spinDownTicks());
        }
        return spinGate;
    }

    private void resetSpin() {
        if (spinGate != null) {
            spinGate.reset();
        }
    }

    private ReloadStateMachine.Event startReload(GunStats stats, GunState state, AmmoInventory ammo) {
        ReloadStateMachine.Event ev = reload.start(stats, state, ammo);
        if (ev == ReloadStateMachine.Event.STARTED) {
            zoom.reset();
            resetSpin();
        }
        return ev;
    }
}
