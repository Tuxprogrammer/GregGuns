package com.gregguns.gun.logic;

/**
 * Trigger handling, fire cooldown and per-shot ammo use. Deterministic: it advances only when {@link #tick} is
 * called and never reads a clock.
 *
 * <h2>Driving it</h2>
 * One instance per player, shared by every gun that player holds (the cooldown is player-wide, as {@code fireDelay}
 * is in Techguns). {@link GunController} owns one and is the normal entry point.
 * <ul>
 * <li><b>Held-trigger path</b> (server, or a client mirror): call {@link #triggerPressed} and
 * {@link #triggerReleased} from the validated input, then each tick call {@link #tick} and {@link #step}.</li>
 * <li><b>Per-shot path</b> (a client-predicted shot arriving as a packet): call {@link #requestShot} instead. It
 * accepts a shot that arrives up to {@code latencyGraceTicks} early so a predicted shot is not silently dropped
 * (GUN-32), and carries the early time forward as a debt so the sustained rate never exceeds the fire rate.</li>
 * </ul>
 *
 * <h2>Bugs it does not reproduce</h2>
 * <ul>
 * <li>GUN-29: a semi-auto click made during the cooldown is remembered for {@code inputBufferTicks} and fires as
 * soon as the cooldown clears, instead of being swallowed.</li>
 * <li>GUN-11: running dry is reported once per trigger pull, so a held full-auto trigger cannot spam the "out of
 * ammo" reaction every tick.</li>
 * <li>GUN-32: see {@link #requestShot}.</li>
 * </ul>
 *
 * <p>
 * Spec: gun-lifecycle.md sections 3 to 5 and 8.
 */
public final class FireControl {

    /** Default window in ticks during which an early click is remembered. */
    public static final int DEFAULT_INPUT_BUFFER_TICKS = 3;
    /** Default number of ticks a packet-driven shot may arrive before the server cooldown has elapsed. */
    public static final int DEFAULT_LATENCY_GRACE_TICKS = 2;

    private final int inputBufferTicks;
    private final int latencyGraceTicks;

    private int cooldown;
    private boolean triggerHeld;
    private int pendingPress;
    private int burstRemaining;
    private boolean dryReported;
    private int alternateParity;

    /** Creates a control with the default buffer and grace windows. */
    public FireControl() {
        this(DEFAULT_INPUT_BUFFER_TICKS, DEFAULT_LATENCY_GRACE_TICKS);
    }

    /**
     * Creates a control.
     *
     * @param inputBufferTicks  how long an early click is remembered (0 disables buffering)
     * @param latencyGraceTicks how early a packet-driven shot may arrive (0 disables the grace)
     */
    public FireControl(int inputBufferTicks, int latencyGraceTicks) {
        this.inputBufferTicks = Math.max(0, inputBufferTicks);
        this.latencyGraceTicks = Math.max(0, latencyGraceTicks);
    }

    /** The trigger went down. A press is remembered for the input buffer window even if released at once. */
    public void triggerPressed() {
        triggerHeld = true;
        pendingPress = Math.max(1, inputBufferTicks);
        dryReported = false;
    }

    /** The trigger went up. A buffered press still fires; a held auto trigger stops. */
    public void triggerReleased() {
        triggerHeld = false;
        dryReported = false;
    }

    /** Drops all input state (weapon switched away, player logged in, trigger state lost). Keeps the cooldown. */
    public void clearInput() {
        triggerHeld = false;
        pendingPress = 0;
        burstRemaining = 0;
        dryReported = false;
    }

    /** Advances the cooldown by one tick. Call once per tick, before {@link #step}. */
    public void tick() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /** Ticks until the next shot may fire; 0 or less means ready. */
    public int cooldownTicks() {
        return Math.max(0, cooldown);
    }

    /** True if the cooldown has elapsed. */
    public boolean isReady() {
        return cooldown <= 0;
    }

    /** Forces a cooldown, for example after an action that locks out firing. Never shortens an existing one. */
    public void lockout(int ticks) {
        cooldown = Math.max(cooldown, ticks);
    }

    /** True if the current input asks for a shot under this gun's fire mode. */
    public boolean wantsShot(GunStats stats) {
        return (stats.fireMode() == FireMode.AUTO && triggerHeld) || pendingPress > 0 || burstRemaining > 0;
    }

    /**
     * Evaluates the held-trigger input once. Call after {@link #tick} each tick while the gun is the main-hand item
     * and not reloading.
     *
     * @param stats        the held gun
     * @param state        its state; on {@link ShotResult.Outcome#FIRED} the rounds are already removed
     * @param infiniteAmmo true for creative players and NPCs: no rounds are spent
     */
    public ShotResult step(GunStats stats, GunState state, boolean infiniteAmmo) {
        return step(stats, new RoundSupply(stats, state), infiniteAmmo);
    }

    /**
     * Like {@link #step(GunStats, GunState, boolean)} for an energy gun: a shot drains
     * {@link GunStats#euPerShot()} from {@code charge} instead of removing rounds.
     *
     * <p>
     * Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-12, GGS-02-13).
     */
    public ShotResult stepEnergy(GunStats stats, ChargeSource charge, boolean infiniteCharge) {
        return step(stats, new EnergySupply(stats, charge), infiniteCharge);
    }

    private ShotResult step(GunStats stats, Supply supply, boolean infiniteAmmo) {
        if (!wantsShot(stats)) {
            return ShotResult.none();
        }
        if (cooldown > 0) {
            // GUN-29: keep the click, but only for the buffer window.
            if (pendingPress > 0) {
                pendingPress--;
            }
            return ShotResult.cooldown();
        }
        if (!infiniteAmmo && !supply.canFire()) {
            pendingPress = 0;
            burstRemaining = 0;
            if (dryReported) {
                return ShotResult.none();
            }
            dryReported = true;
            return ShotResult.empty();
        }
        return fire(stats, supply, infiniteAmmo, 0);
    }

    /**
     * Handles one packet-driven shot request (the client already predicted it). The request carries no ammo count
     * and no zoom flag; the server decides everything from its own state.
     *
     * <p>
     * A request is accepted if the cooldown is at most {@code latencyGraceTicks} away. The early time becomes a debt
     * added to the next cooldown, so a client that fires early every time is still held to the fire rate (GUN-32).
     */
    public ShotResult requestShot(GunStats stats, GunState state, boolean infiniteAmmo) {
        return requestShot(stats, new RoundSupply(stats, state), infiniteAmmo);
    }

    /** Like {@link #requestShot(GunStats, GunState, boolean)} for an energy gun (see {@link #stepEnergy}). */
    public ShotResult requestEnergyShot(GunStats stats, ChargeSource charge, boolean infiniteCharge) {
        return requestShot(stats, new EnergySupply(stats, charge), infiniteCharge);
    }

    private ShotResult requestShot(GunStats stats, Supply supply, boolean infiniteAmmo) {
        if (cooldown > latencyGraceTicks) {
            return ShotResult.cooldown();
        }
        if (!infiniteAmmo && !supply.canFire()) {
            return ShotResult.empty();
        }
        return fire(stats, supply, infiniteAmmo, Math.max(0, cooldown));
    }

    private ShotResult fire(GunStats stats, Supply supply, boolean infiniteAmmo, int debt) {
        int attackType = 0;
        if (stats.akimbo() && !stats.doubleShot()) {
            attackType = infiniteAmmo ? alternateParity : supply.parityBasis() % 2;
            alternateParity ^= 1;
        }
        int used = infiniteAmmo ? 0 : supply.spend();
        dryReported = false;
        pendingPress = 0;

        int base;
        if (stats.fireMode() == FireMode.BURST) {
            if (burstRemaining > 0) {
                burstRemaining--;
            } else {
                burstRemaining = stats.burstShots() - 1;
            }
            base = burstRemaining > 0 ? stats.burstDelayTicks() : stats.fireRateTicks();
        } else {
            base = stats.fireRateTicks();
        }
        cooldown = base + debt;
        return ShotResult.fired(attackType, used);
    }

    /** What a shot draws on: rounds in the gun, or charge in the battery. */
    private interface Supply {

        boolean canFire();

        /** A count whose parity alternates the akimbo hands (it drops by one with each shot). */
        int parityBasis();

        /** Spends one shot's worth; returns the rounds it used. */
        int spend();
    }

    private static final class RoundSupply implements Supply {

        private final GunStats stats;
        private final GunState state;

        RoundSupply(GunStats stats, GunState state) {
            this.stats = stats;
            this.state = state;
        }

        @Override
        public boolean canFire() {
            return state.canFire(stats);
        }

        @Override
        public int parityBasis() {
            return state.loaded();
        }

        @Override
        public int spend() {
            return state.use(stats.ammoPerShot());
        }
    }

    private static final class EnergySupply implements Supply {

        private final GunStats stats;
        private final ChargeSource charge;

        EnergySupply(GunStats stats, ChargeSource charge) {
            this.stats = stats;
            this.charge = charge;
        }

        @Override
        public boolean canFire() {
            return EnergyMath.canFire(charge.stored(), stats.euPerShot());
        }

        @Override
        public int parityBasis() {
            return (int) Math.min(Integer.MAX_VALUE, EnergyMath.shotsLeft(charge.stored(), stats.euPerShot()));
        }

        @Override
        public int spend() {
            return charge.drain(stats.euPerShot()) >= stats.euPerShot() ? stats.ammoPerShot() : 0;
        }
    }
}
