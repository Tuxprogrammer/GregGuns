package com.gregguns.gun.logic;

/**
 * The reload lifecycle: idle, reloading, then completed or canceled. Timed in ticks, with no clock.
 *
 * <h2>Rules</h2>
 * <ul>
 * <li>{@link #start} only checks that a reload is possible (gun not full, ammo available). It moves nothing, so
 * nothing can be lost if the reload is canceled, the player logs out or the server stops.</li>
 * <li>{@link #tick} counts down. On the last tick the ammo moves atomically: the new ammo is withdrawn first, and
 * only if that succeeds are the rounds loaded and the old rounds and empty containers handed back. If the ammo is
 * gone by then the reload is canceled and the gun is unchanged.</li>
 * <li>A <em>forced</em> reload (rounds still loaded, {@link ReloadMode#MAGAZINE}) hands the remaining rounds back
 * with {@link AmmoInventory#depositRounds}. They are never thrown away (GUN-13).</li>
 * <li>Interrupts: {@link #cancel} with {@link CancelReason#WEAPON_SWITCH} or {@link CancelReason#FIRE}. Firing may
 * interrupt only when the gun still holds a full shot (see {@link GunController}).</li>
 * <li><b>Single rounds</b> ({@link Load#SINGLE_ROUND}): a magazine-fed gun ({@link GunStats#loadsSingleRounds()}) with
 * fewer magazines at hand than a reload takes, but with its loose rounds, loads one shot's worth of loose rounds (one
 * round, two for the double-shot Akimbo Mac10s) for the full reload time, one loose item per round. Nothing is handed
 * back.</li>
 * <li><b>Chambers</b> ({@link Load#CHAMBER}, {@link ReloadMode#CHAMBER}): one run loads chamber after chamber, each
 * taking {@link GunStats#chamberTicks}, while ammo lasts and the gun is not full. A chamber goes in at the end of its
 * own time, so a cancel keeps every chamber already loaded. The run is planned at start (how many chambers the ammo
 * and the chamber credit cover), so the server and the client mirror animate the same total time.</li>
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

    /** What the running reload loads. */
    public enum Load {
        /** Nothing: idle. */
        NONE,
        /** A full magazine swap. */
        MAGAZINE,
        /** The shell-by-shell top-up of {@link ReloadMode#INCREMENTAL}. */
        TOP_UP,
        /** One shot's worth of loose rounds into a magazine-fed gun with no magazine at hand. */
        SINGLE_ROUND,
        /** Chamber by chamber ({@link ReloadMode#CHAMBER}). */
        CHAMBER
    }

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
        /** A chamber was loaded and the next one began; the reload goes on. */
        CHAMBERED,
        /** The reload was canceled; see {@link ReloadStateMachine#lastCancelReason()}. */
        CANCELED,
        /** Refused: the gun is already full. */
        REJECTED_FULL,
        /** Refused: the inventory has no ammo for this gun. */
        REJECTED_NO_AMMO,
        /** Refused: a reload is already running. */
        REJECTED_BUSY
    }

    /** Why a reload was canceled. */
    public enum CancelReason {
        /** The player changed the held item or dropped the gun. */
        WEAPON_SWITCH,
        /** The player fired with the rounds still loaded. */
        FIRE,
        /** The ammo disappeared before the reload completed. */
        NO_AMMO,
        /** Canceled by game logic, for example death. */
        OTHER
    }

    private Phase phase = Phase.IDLE;
    private int ticksRemaining;
    private int ticksTotal;
    private boolean forced;
    private Load load = Load.NONE;
    private int chambersLeft;
    private int chamberTicksLeft;
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

    /** What the running reload loads; {@link Load#NONE} when idle. */
    public Load load() {
        return load;
    }

    /**
     * Length of the running reload in ticks, what the reload animation and message use: the gun's reload time, or for
     * a chamber run the time of the chambers it planned. 0 when idle.
     */
    public int totalTicks() {
        return isReloading() ? ticksTotal : 0;
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
        Load chosen = choose(stats, state, ammo);
        if (chosen == Load.NONE) {
            return Event.REJECTED_NO_AMMO;
        }
        phase = Phase.RELOADING;
        load = chosen;
        if (chosen == Load.CHAMBER) {
            chambersLeft = plannedChambers(stats, state, ammo);
            ticksTotal = Math.max(1, stats.chamberTicksBetween(state.loaded(), state.loaded() + chambersLeft));
            chamberTicksLeft = stats.chamberTicks(state.loaded());
        } else {
            ticksTotal = Math.max(1, stats.reloadTicks());
        }
        ticksRemaining = ticksTotal;
        forced = !state.isEmpty();
        return Event.STARTED;
    }

    /**
     * The reload a gun would get now, without starting it: the pure decision behind {@link #start}. A magazine gun
     * takes a full swap when a reload's magazines are at hand, otherwise single loose rounds if it is magazine-fed; a
     * chamber gun loads chambers when its credit or one item covers a round.
     *
     * @param stats the gun
     * @param state its state
     * @param ammo  the shooter's ammo source (only counted)
     * @return the load, or {@link Load#NONE} when there is nothing to load with (or the gun is full)
     */
    public static Load choose(GunStats stats, GunState state, AmmoInventory ammo) {
        if (state.isFull(stats)) {
            return Load.NONE;
        }
        switch (stats.reloadMode()) {
            case INCREMENTAL:
                return ammo.count(stats.ammoType()) >= 1 ? Load.TOP_UP : Load.NONE;
            case CHAMBER:
                return state.credit() > 0 || ammo.count(stats.ammoType()) >= 1 ? Load.CHAMBER : Load.NONE;
            default:
                if (ammo.count(stats.ammoType()) >= stats.ammoItemsPerReload()) {
                    return Load.MAGAZINE;
                }
                if (stats.loadsSingleRounds() && ammo.count(stats.looseAmmoType()) >= singleRounds(stats, state)) {
                    return Load.SINGLE_ROUND;
                }
                return Load.NONE;
        }
    }

    /**
     * Rounds a single-round reload adds: one shot's worth (one round; two for a gun that spends two per shot), never
     * past the magazine size. Each costs one loose item.
     */
    public static int singleRounds(GunStats stats, GunState state) {
        return Math.max(1, Math.min(stats.ammoPerShot(), stats.magazineSize() - state.loaded()));
    }

    /** Chambers the credit and the ammo at hand cover, up to the empty chambers. */
    private static int plannedChambers(GunStats stats, GunState state, AmmoInventory ammo) {
        long rounds = state.credit() + (long) ammo.count(stats.ammoType()) * Math.max(1, stats.roundsPerItem());
        return (int) Math.max(1, Math.min(stats.magazineSize() - state.loaded(), rounds));
    }

    /**
     * Advances a running reload by one tick. Returns {@link Event#NONE} while counting, {@link Event#COMPLETED} when
     * the rounds were loaded, or {@link Event#CANCELED} if the ammo was gone at the end.
     */
    public Event tick(GunStats stats, GunState state, AmmoInventory ammo) {
        if (!isReloading()) {
            return Event.NONE;
        }
        ticksRemaining--;
        if (load == Load.CHAMBER) {
            return tickChamber(stats, state, ammo);
        }
        if (ticksRemaining > 0) {
            return Event.NONE;
        }
        boolean ok;
        switch (load) {
            case MAGAZINE:
                ok = swapMagazine(stats, state, ammo);
                break;
            case SINGLE_ROUND:
                ok = loadSingle(stats, state, ammo);
                break;
            default:
                ok = topUp(stats, state, ammo);
                break;
        }
        if (!ok) {
            return cancel(CancelReason.NO_AMMO);
        }
        finish();
        return Event.COMPLETED;
    }

    private Event tickChamber(GunStats stats, GunState state, AmmoInventory ammo) {
        chamberTicksLeft--;
        if (chamberTicksLeft > 0) {
            return Event.NONE;
        }
        if (!loadChamber(stats, state, ammo)) {
            // The ammo went away mid-run: the chambers already loaded stay.
            return cancel(CancelReason.NO_AMMO);
        }
        chambersLeft--;
        if (chambersLeft <= 0 || state.isFull(stats)) {
            finish();
            return Event.COMPLETED;
        }
        chamberTicksLeft = stats.chamberTicks(state.loaded());
        return Event.CHAMBERED;
    }

    /** Cancels a running reload without changing the gun or the inventory. No effect (NONE) when idle. */
    public Event cancel(CancelReason reason) {
        if (!isReloading()) {
            return Event.NONE;
        }
        lastCancelReason = reason;
        finish();
        return Event.CANCELED;
    }

    private void finish() {
        phase = Phase.IDLE;
        ticksRemaining = 0;
        forced = false;
        load = Load.NONE;
        chambersLeft = 0;
        chamberTicksLeft = 0;
    }

    /**
     * One shot's worth of loose rounds, one loose item each. All or nothing: a short withdrawal is handed back and the
     * reload fails.
     */
    private static boolean loadSingle(GunStats stats, GunState state, AmmoInventory ammo) {
        int rounds = singleRounds(stats, state);
        if (state.isFull(stats)) {
            return false;
        }
        int got = ammo.withdraw(stats.looseAmmoType(), rounds);
        if (got < rounds) {
            ammo.depositItems(stats.looseAmmoType(), got);
            return false;
        }
        state.setLoaded(stats, state.loaded() + rounds);
        return true;
    }

    /**
     * One chamber: from the credit when there is some, otherwise one item, whose other rounds become credit. An empty
     * container (none of the chamber guns has one today) would come back per item.
     */
    private static boolean loadChamber(GunStats stats, GunState state, AmmoInventory ammo) {
        if (state.isFull(stats)) {
            return false;
        }
        if (state.credit() > 0) {
            state.setCredit(stats, state.credit() - 1);
        } else {
            if (ammo.withdraw(stats.ammoType(), 1) < 1) {
                return false;
            }
            if (stats.emptyContainerType() != null) {
                ammo.depositItems(stats.emptyContainerType(), 1);
            }
            state.setCredit(stats, stats.roundsPerItem() - 1);
        }
        state.setLoaded(stats, state.loaded() + 1);
        return true;
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
