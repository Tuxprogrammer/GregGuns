package com.gregguns.utility.turret;

/**
 * The Turret base's state machine: upkeep, healing, the repair after the entity dies, and the respawn. Pure logic with
 * no Minecraft types and no clock; the MTE calls {@link #tick} once per server tick and acts on the {@link Result}.
 * Costs are in EU and come from {@code UtilityConfig}.
 *
 * <p>
 * Flow: while the entity lives and the redstone mode allows it, the base pays the upkeep every tick (the turret sleeps
 * when it cannot) and, every {@link #HEAL_COOLDOWN_TICKS} ticks, pays {@code euPerHp} to heal one hit point of a hurt
 * turret. When the entity dies the base waits {@code repairTicks}, paying the repair draw each tick; without power the
 * countdown stalls. At zero a new turret spawns with {@link #RESPAWN_HEALTH} hit points.
 */
// Spec: docs/techguns-spec/09-machines-utility/turret.md (Per-tick logic), docs/gregguns-spec/09-machines-utility/
// turret.md (GGS-09-07), docs/gregguns-spec/09-machines-utility/recipes.md
// Deviation: costs are EU (4 EU/t upkeep, 120 EU per HP, 10 EU/t repair) instead of 5 RF/t, 500 RF and 50 RF.
public final class TurretLogic {

    /** Ticks between two heals of one hit point. */
    public static final int HEAL_COOLDOWN_TICKS = 60;
    /** Hit points of a respawned turret. */
    public static final float RESPAWN_HEALTH = 10.0F;
    /** Hit points of a turret spawned at placement. */
    public static final float FULL_HEALTH = 30.0F;

    /** Pays energy from the base's buffer. */
    public interface Power {

        /**
         * @param eu energy wanted
         * @return true and deduct it when the buffer holds at least {@code eu}; false and deduct nothing otherwise
         */
        boolean pay(long eu);
    }

    /** What the base must do after one tick. */
    public static final class Result {

        /** Re-add the turret's AI. */
        public final boolean activate;
        /** Strip the turret's AI. */
        public final boolean deactivate;
        /** Heal the turret by one hit point. */
        public final boolean heal;
        /** Spawn a new turret with {@link #RESPAWN_HEALTH}. */
        public final boolean respawn;

        Result(boolean activate, boolean deactivate, boolean heal, boolean respawn) {
            this.activate = activate;
            this.deactivate = deactivate;
            this.heal = heal;
            this.respawn = respawn;
        }
    }

    private static final Result NOTHING = new Result(false, false, false, false);

    private final int upkeepEuPerTick;
    private final int euPerHp;
    private final int repairEuPerTick;
    private final int repairTicks;

    private boolean dead;
    private int repairLeft;
    private int healCooldown;
    private boolean active = true;

    /**
     * @param upkeepEuPerTick EU per tick while guarding
     * @param euPerHp         EU per healed hit point
     * @param repairEuPerTick EU per tick during the repair
     * @param repairTicks     ticks of the repair
     */
    public TurretLogic(int upkeepEuPerTick, int euPerHp, int repairEuPerTick, int repairTicks) {
        this.upkeepEuPerTick = Math.max(0, upkeepEuPerTick);
        this.euPerHp = Math.max(0, euPerHp);
        this.repairEuPerTick = Math.max(0, repairEuPerTick);
        this.repairTicks = Math.max(1, repairTicks);
    }

    /** @return true while the entity is dead and the base is repairing */
    public boolean isDead() {
        return dead;
    }

    /** @return ticks of repair left; 0 when not dead */
    public int repairLeft() {
        return repairLeft;
    }

    /** @return ticks until the next heal may happen */
    public int healCooldown() {
        return healCooldown;
    }

    /** @return true while the turret's AI is on */
    public boolean isActive() {
        return active;
    }

    /**
     * Restores saved state.
     *
     * @param dead         whether the entity is dead
     * @param repairLeft   ticks of repair left
     * @param healCooldown ticks until the next heal
     * @param active       whether the AI is on
     */
    public void restore(boolean dead, int repairLeft, int healCooldown, boolean active) {
        this.dead = dead;
        this.repairLeft = dead ? Math.max(0, repairLeft) : 0;
        this.healCooldown = Math.max(0, healCooldown);
        this.active = active;
    }

    /** The entity died: start the repair. Calling it twice restarts the repair from the full length. */
    public void onTurretDeath() {
        dead = true;
        repairLeft = repairTicks;
        active = true;
    }

    /** A new entity exists (placement or respawn): it starts awake. */
    public void onTurretSpawned() {
        dead = false;
        repairLeft = 0;
        active = true;
    }

    /**
     * Advances one server tick.
     *
     * @param power         the base's energy
     * @param enabled       whether the redstone mode allows work
     * @param turretPresent whether a live, linked turret entity exists
     * @param hurt          whether that turret is below full health
     * @return what the base must do; never null
     */
    public Result tick(Power power, boolean enabled, boolean turretPresent, boolean hurt) {
        if (dead) {
            if (enabled && power.pay(repairEuPerTick)) {
                repairLeft--;
                if (repairLeft <= 0) {
                    dead = false;
                    repairLeft = 0;
                    active = true;
                    return new Result(false, false, false, true);
                }
            }
            return NOTHING;
        }
        if (!turretPresent) {
            return NOTHING;
        }
        if (!enabled) {
            if (active) {
                active = false;
                return new Result(false, true, false, false);
            }
            return NOTHING;
        }
        boolean activate = false;
        boolean deactivate = false;
        if (power.pay(upkeepEuPerTick)) {
            if (!active) {
                active = true;
                activate = true;
            }
        } else if (active) {
            active = false;
            deactivate = true;
        }
        if (healCooldown > 0) {
            healCooldown--;
        }
        boolean heal = false;
        if (healCooldown == 0 && hurt && active && power.pay(euPerHp)) {
            heal = true;
            healCooldown = HEAL_COOLDOWN_TICKS;
        }
        return activate || deactivate || heal ? new Result(activate, deactivate, heal, false) : NOTHING;
    }

    /**
     * The tier gate (GGS-08-50): a Turret fires guns up to its own tier.
     *
     * @param gunTierIndex    the gun's GT voltage index
     * @param turretTierIndex the Turret's GT voltage index
     * @return true when the gun may be mounted
     */
    public static boolean gunAllowed(int gunTierIndex, int turretTierIndex) {
        return gunTierIndex <= turretTierIndex;
    }
}
