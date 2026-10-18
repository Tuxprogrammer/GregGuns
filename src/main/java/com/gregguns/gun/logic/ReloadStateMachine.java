package com.gregguns.gun.logic;

/**
 * The reload lifecycle: idle, reloading, then completed or cancelled. Timed in ticks, with no clock.
 *
 * <h2>Rules</h2>
 * <ul>
 * <li>{@link #start} only checks that a reload is possible (gun not full, ammo available). It moves nothing, so
 * nothing can be lost if the reload is cancelled, the player logs out or the server stops.</li>
 * <li>{@link #tick} counts down. On the last tick the ammo moves atomically: the new ammo is withdrawn first, and
 * only if that succeeds are the rounds loaded and the old rounds and empty containers handed back. If the ammo is
 * gone by then the reload is cancelled and the gun is unchanged.</li>
 * <li>A <em>forced</em> reload (rounds still loaded, {@link ReloadMode#MAGAZINE}) hands the remaining rounds back
 * with {@link AmmoInventory#depositRounds}. They are never thrown away (GUN-13).</li>
 * <li>Interrupts: {@link #cancel} with {@link CancelReason#WEAPON_SWITCH} or {@link CancelReason#FIRE}. Firing may
 * interrupt only when the gun still holds a full shot (see {@link GunController}).</li>
 * </ul>
 *
 * <h2>Driving it</h2>
 * Server only, through {@link GunController}. The client mirror may run its own copy for the animation and
 * prediction, but the server's completion is what changes the stack.
 *
 * <p>
 * Deviation: Techguns adds the rounds at reload start and uses the reload time only as a fire lockout. GregGuns
 * loads at completion so a reload can be interrupted. Spec: gun-lifecycle.md section 7.
 */
public final class ReloadStateMachine {

    /** Current phase. */
    public enum Phase {
        /** Not reloading. */
        IDLE,
        /** A reload is counting down. */
        RELOADING
    }

    /** What a call changed. */
    public enum Event {
        /** Nothing happened. */
        NONE,
        /** A reload began. */
        STARTED,
        /** The reload finished and the rounds are loaded. */
        COMPLETED,
        /** The reload was cancelled; see {@link ReloadStateMachine#lastCancelReason()}. */
        CANCELLED,
        /** Refused: the gun is already full. */
        REJECTED_FULL,
        /** Refused: the inventory has no ammo for this gun. */
        REJECTED_NO_AMMO,
        /** Refused: a reload is already running. */
        REJECTED_BUSY
    }

    /** Why a reload was cancelled. */
    public enum CancelReason {
        /** The player changed the held item or dropped the gun. */
        WEAPON_SWITCH,
        /** The player fired with the rounds still loaded. */
        FIRE,
        /** The ammo disappeared before the reload completed. */
        NO_AMMO,
        /** Cancelled by game logic, for example death. */
        OTHER
    }

    private Phase phase = Phase.IDLE;
    private int ticksRemaining;
    private int ticksTotal;
    private boolean forced;
    private CancelReason lastCancelReason;

    /** The current phase. */
    public Phase phase() {
        return phase;
    }

    /** True while a reload is counting down. */
    public boolean isReloading() {
        return phase == Phase.RELOADING;
    }

    /** True if the running reload started with rounds still in the gun. */
    public boolean isForced() {
        return forced;
    }

    /** Ticks left; 0 when idle. */
    public int ticksRemaining() {
        return ticksRemaining;
    }

    /** Progress of the running reload from 0 to 1; 0 when idle. For the client animation. */
    public double progress() {
        return isReloading() ? 1.0 - (double) ticksRemaining / ticksTotal : 0.0;
    }

    /** The reason of the most recent cancel, or null if none happened yet. */
    public CancelReason lastCancelReason() {
        return lastCancelReason;
    }

    /**
     * Tries to start a reload. Works for an automatic reload on an empty gun and for a forced one.
     *
     * @param stats the gun
     * @param state its state (read only here)
     * @param ammo  the shooter's ammo source
     * @return {@link Event#STARTED} or a {@code REJECTED_*} reason
     */
    public Event start(GunStats stats, GunState state, AmmoInventory ammo) {
        if (isReloading()) {
            return Event.REJECTED_BUSY;
        }
        if (state.isFull(stats)) {
            return Event.REJECTED_FULL;
        }
        int needed = stats.reloadMode() == ReloadMode.MAGAZINE ? stats.ammoItemsPerReload() : 1;
        if (ammo.count(stats.ammoType()) < needed) {
            return Event.REJECTED_NO_AMMO;
        }
        phase = Phase.RELOADING;
        ticksTotal = Math.max(1, stats.reloadTicks());
        ticksRemaining = ticksTotal;
        forced = !state.isEmpty();
        return Event.STARTED;
    }

    /**
     * Advances a running reload by one tick. Returns {@link Event#NONE} while counting, {@link Event#COMPLETED} when
     * the rounds were loaded, or {@link Event#CANCELLED} if the ammo was gone at the end.
     */
    public Event tick(GunStats stats, GunState state, AmmoInventory ammo) {
        if (!isReloading()) {
            return Event.NONE;
        }
        ticksRemaining--;
        if (ticksRemaining > 0) {
            return Event.NONE;
        }
        boolean ok = stats.reloadMode() == ReloadMode.MAGAZINE ? swapMagazine(stats, state, ammo)
            : topUp(stats, state, ammo);
        if (!ok) {
            return cancel(CancelReason.NO_AMMO);
        }
        finish();
        return Event.COMPLETED;
    }

    /** Cancels a running reload without changing the gun or the inventory. No effect (NONE) when idle. */
    public Event cancel(CancelReason reason) {
        if (!isReloading()) {
            return Event.NONE;
        }
        lastCancelReason = reason;
        finish();
        return Event.CANCELLED;
    }

    private void finish() {
        phase = Phase.IDLE;
        ticksRemaining = 0;
        forced = false;
    }

    private static boolean swapMagazine(GunStats stats, GunState state, AmmoInventory ammo) {
        int items = stats.ammoItemsPerReload();
        if (ammo.count(stats.ammoType()) < items) {
            return false;
        }
        int got = ammo.withdraw(stats.ammoType(), items);
        if (got < items) {
            ammo.depositItems(stats.ammoType(), got);
            return false;
        }
        int old = state.loaded();
        if (old > 0) {
            // GUN-13: a forced reload keeps the loaded rounds instead of discarding them.
            ammo.depositRounds(stats.looseAmmoType(), old);
        }
        if (stats.emptyContainerType() != null) {
            ammo.depositItems(stats.emptyContainerType(), items);
        }
        state.setLoaded(stats, stats.magazineSize());
        return true;
    }

    private static boolean topUp(GunStats stats, GunState state, AmmoInventory ammo) {
        int perItem = stats.roundsPerItem();
        int missing = stats.magazineSize() - state.loaded();
        int wanted = Math.min(stats.ammoItemsPerReload(), (missing + perItem - 1) / perItem);
        if (wanted <= 0) {
            return false;
        }
        int got = ammo.withdraw(stats.ammoType(), wanted);
        if (got <= 0) {
            return false;
        }
        if (stats.emptyContainerType() != null) {
            ammo.depositItems(stats.emptyContainerType(), got);
        }
        state.setLoaded(stats, state.loaded() + got * perItem);
        return true;
    }
}
