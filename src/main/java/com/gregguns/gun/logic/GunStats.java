package com.gregguns.gun.logic;

/**
 * Immutable definition of one gun. Every value the Techguns lifecycle sheet uses is carried here.
 *
 * <p>
 * This class is pure data. Damage and drop-off values are carried for the combat module, which owns the formula.
 * Durations are in game ticks (20 per second), distances in blocks.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/gun-catalog.md (constructor and setters), gun-lifecycle.md sections 1 to 8.
 */
public final class GunStats {

    /** The ammo type id of an energy gun: a marker, since no item of this type exists. */
    public static final String ENERGY_AMMO_TYPE = "energy";

    private final String id;
    private final double damage;
    private final double damageMin;
    private final double dropStart;
    private final double dropEnd;
    private final double spread;
    private final double zoomFactor;
    private final double zoomSpreadMultiplier;
    private final FireMode fireMode;
    private final int fireRateTicks;
    private final int burstShots;
    private final int burstDelayTicks;
    private final int ammoPerShot;
    private final int magazineSize;
    private final String ammoType;
    private final String looseAmmoType;
    private final String emptyContainerType;
    private final ReloadMode reloadMode;
    private final int ammoItemsPerReload;
    private final int reloadTicks;
    private final double projectileSpeed;
    private final int lifetimeTicks;
    private final int bulletsPerShot;
    private final double pelletSpread;
    private final boolean pelletsInLine;
    private final ShotPattern pattern;
    private final double penetration;
    private final boolean akimbo;
    private final boolean doubleShot;
    private final boolean silenced;
    private final ProjectileKind projectileKind;
    private final double knockback;
    private final double blastRadius;
    private final double projectileGravity;
    private final int recoilTicks;
    private final int muzzleFlashTicks;
    private final double aiRange;
    private final int aiAttackTime;
    private final int aiBurstCount;
    private final int aiBurstDelay;
    private final long euPerShot;
    private final long capacityEu;
    private final int chargeFullTicks;
    private final ChargeMode chargeMode;
    private final int chargeMaxUnits;

    private GunStats(Builder b) {
        id = b.id;
        damage = b.damage;
        damageMin = b.damageMin < 0 ? b.damage : b.damageMin;
        dropStart = b.dropSet ? b.dropStart : b.projectileSpeed * b.lifetimeTicks;
        dropEnd = b.dropSet ? b.dropEnd : dropStart;
        spread = b.spread;
        zoomFactor = b.zoomFactor;
        zoomSpreadMultiplier = b.zoomSpreadMultiplier;
        fireMode = b.fireMode;
        fireRateTicks = b.fireRateTicks;
        burstShots = b.burstShots;
        burstDelayTicks = b.burstDelayTicks;
        ammoPerShot = b.ammoPerShot;
        magazineSize = b.euPerShot > 0 ? (int) Math.min(Integer.MAX_VALUE, b.capacityEu / b.euPerShot) : b.magazineSize;
        ammoType = b.euPerShot > 0 && b.ammoType == null ? ENERGY_AMMO_TYPE : b.ammoType;
        looseAmmoType = b.looseAmmoType;
        emptyContainerType = b.emptyContainerType;
        reloadMode = b.reloadMode;
        ammoItemsPerReload = b.ammoItemsPerReload;
        reloadTicks = b.reloadTicks;
        projectileSpeed = b.projectileSpeed;
        lifetimeTicks = b.lifetimeTicks;
        bulletsPerShot = b.bulletsPerShot;
        pelletSpread = b.pelletSpread;
        pelletsInLine = b.pelletsInLine;
        pattern = b.pattern;
        penetration = b.penetration;
        akimbo = b.akimbo;
        doubleShot = b.doubleShot;
        silenced = b.silenced;
        projectileKind = b.projectileKind;
        knockback = b.knockback;
        blastRadius = b.blastRadius;
        projectileGravity = b.projectileGravity;
        recoilTicks = b.recoilTicks;
        muzzleFlashTicks = b.muzzleFlashTicks;
        aiRange = b.aiRange;
        aiAttackTime = b.aiAttackTime;
        aiBurstCount = b.aiBurstCount;
        aiBurstDelay = b.aiBurstDelay;
        euPerShot = b.euPerShot;
        capacityEu = b.capacityEu;
        chargeFullTicks = b.chargeFullTicks;
        chargeMode = b.chargeMode;
        chargeMaxUnits = b.chargeMaxUnits;
        validate();
    }

    private void validate() {
        require(id != null && !id.isEmpty(), "id");
        require(damage >= 0, "damage");
        require(damageMin >= 0 && damageMin <= damage, "damageMin must be in [0, damage]");
        require(dropStart >= 0 && dropEnd >= dropStart, "drop-off distances");
        require(spread >= 0 && pelletSpread >= 0, "spread");
        require(zoomFactor > 0 && zoomFactor <= 1, "zoomFactor must be in (0, 1]");
        require(zoomSpreadMultiplier >= 0, "zoomSpreadMultiplier");
        require(fireRateTicks >= 1, "fireRateTicks");
        require(fireMode != FireMode.BURST || burstShots >= 2, "BURST needs burstShots >= 2");
        require(burstDelayTicks >= 1, "burstDelayTicks");
        require(ammoPerShot >= 1, "ammoPerShot");
        require(magazineSize >= ammoPerShot, "magazineSize");
        require(ammoType != null && !ammoType.isEmpty(), "ammoType");
        require(euPerShot == 0 || capacityEu >= euPerShot, "energy capacity must cover one shot");
        require(ammoItemsPerReload >= 1, "ammoItemsPerReload");
        require(
            reloadMode != ReloadMode.INCREMENTAL || magazineSize % ammoItemsPerReload == 0,
            "INCREMENTAL magazineSize must divide evenly by ammoItemsPerReload");
        require(reloadTicks >= 1, "reloadTicks");
        require(projectileSpeed > 0 && lifetimeTicks >= 1, "projectile speed / lifetime");
        require(bulletsPerShot >= 1, "bulletsPerShot");
        require(penetration >= 0 && penetration <= 1, "penetration");
        require(pattern != null, "pattern");
        require(projectileKind != null, "projectileKind");
        require(knockback >= 0, "knockback");
        require(blastRadius >= 0 && projectileGravity >= 0, "blast radius / projectile gravity");
        require(chargeFullTicks >= 0 && chargeMaxUnits >= 1, "charge");
    }

    private static void require(boolean ok, String what) {
        if (!ok) {
            throw new IllegalArgumentException("Invalid GunStats: " + what);
        }
    }

    /** Starts a builder. The id is the registry-style name, for example {@code "pistol"}. */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** The gun's name, also the registry name without the mod id. */
    public String id() {
        return id;
    }

    /** Maximum damage per projectile (half hearts). Applied by the combat module. */
    public double damage() {
        return damage;
    }

    /** Minimum damage beyond {@link #dropEnd()}. Equal to {@link #damage()} when the gun has no drop-off. */
    public double damageMin() {
        return damageMin;
    }

    /** Distance in blocks up to which damage is full. The combat module owns the formula. */
    public double dropStart() {
        return dropStart;
    }

    /** Distance in blocks at which damage reaches {@link #damageMin()}. */
    public double dropEnd() {
        return dropEnd;
    }

    /** Spread value ("accuracy"); a shot's yaw and pitch each get a uniform offset of +-spread*40 degrees. */
    public double spread() {
        return spread;
    }

    /** FOV multiplier while zoomed; 1.0 means the gun has no scope. */
    public double zoomFactor() {
        return zoomFactor;
    }

    /** Multiplier on spread while zoomed (Techguns "zoombonus"). */
    public double zoomSpreadMultiplier() {
        return zoomSpreadMultiplier;
    }

    /** True if right-click zoom does anything for this gun (GUN-47: an unscoped gun is never "zooming"). */
    public boolean hasScope() {
        return zoomFactor < 1.0;
    }

    /** Semi, auto or burst. */
    public FireMode fireMode() {
        return fireMode;
    }

    /** Ticks between shots (Techguns {@code minFiretime}). */
    public int fireRateTicks() {
        return fireRateTicks;
    }

    /** Shots per trigger pull in {@link FireMode#BURST}; 1 otherwise. */
    public int burstShots() {
        return burstShots;
    }

    /** Ticks between the shots inside a burst. */
    public int burstDelayTicks() {
        return burstDelayTicks;
    }

    /** Rounds used per trigger pull (2 for double-shot akimbo). */
    public int ammoPerShot() {
        return ammoPerShot;
    }

    /** Rounds in a full gun. */
    public int magazineSize() {
        return magazineSize;
    }

    /** Ammo type id consumed by a reload. Must equal the ammo module's id. */
    public String ammoType() {
        return ammoType;
    }

    /** Ammo type id for loose rounds handed back on a forced reload; defaults to {@link #ammoType()}. */
    public String looseAmmoType() {
        return looseAmmoType == null ? ammoType : looseAmmoType;
    }

    /** Ammo type id of the empty container returned by a reload, or null if the gun returns none. */
    public String emptyContainerType() {
        return emptyContainerType;
    }

    /** How a reload moves ammo. */
    public ReloadMode reloadMode() {
        return reloadMode;
    }

    /** Items taken per reload (stack size for a magazine, shell count for incremental guns). */
    public int ammoItemsPerReload() {
        return ammoItemsPerReload;
    }

    /**
     * Rounds worth of one reload item: clip / items per reload. A single gun takes one item per reload, so this is
     * its whole magazine; an akimbo pair takes two (one per gun), so each item is half the clip (36 rounds from two
     * 18-round pistol magazines is 18 per item). Getting this right keeps the forced-reload refund of an akimbo gun
     * at the single gun's rate (GUN-14 is the same factor gone wrong).
     */
    public int roundsPerItem() {
        return magazineSize / ammoItemsPerReload;
    }

    /** Reload duration in ticks, from start to the rounds being loaded. */
    public int reloadTicks() {
        return reloadTicks;
    }

    /** Projectile speed in blocks per tick. */
    public double projectileSpeed() {
        return projectileSpeed;
    }

    /** Projectile lifetime in ticks (Techguns {@code TTL}); not a distance (GUN-18). */
    public int lifetimeTicks() {
        return lifetimeTicks;
    }

    /** Nominal reach in blocks: speed times lifetime. */
    public double reachBlocks() {
        return projectileSpeed * lifetimeTicks;
    }

    /** Projectiles per round fired (1 plus the pellet count). */
    public int bulletsPerShot() {
        return bulletsPerShot;
    }

    /** Spread for the extra pellets. */
    public double pelletSpread() {
        return pelletSpread;
    }

    /** True if extra pellets are spaced along the flight path (burst-style) instead of spawned together. */
    public boolean pelletsInLine() {
        return pelletsInLine;
    }

    /**
     * How a round becomes projectiles when it is not the plain main shot plus pellets: a burst line, ring volley or
     * fan. {@link ShotPattern#NONE} for most guns.
     */
    public ShotPattern pattern() {
        return pattern;
    }

    /** Armor penetration, 0 to 1. */
    public double penetration() {
        return penetration;
    }

    /** True if the gun is an akimbo pair (one item, two guns). */
    public boolean akimbo() {
        return akimbo;
    }

    /** True if an akimbo gun fires both guns per trigger (otherwise the hands alternate). */
    public boolean doubleShot() {
        return doubleShot;
    }

    /**
     * True for a silenced gun (the Infiltrator): its shot plays the quieter fire event and its tooltip says so.
     * Techguns
     * only reads the flag in the mining dummy, so nothing else (NPC aggro, projectile effects) depends on it.
     */
    public boolean silenced() {
        return silenced;
    }

    /**
     * True for an energy gun: a shot spends {@link #euPerShot()} from the gun's battery instead of a round, and there
     * is no reload (energy-electric-items.md GGS-02-13). {@link #magazineSize()} is then the shots in a full default
     * charge and {@link #ammoType()} is {@link #ENERGY_AMMO_TYPE}.
     */
    public boolean isEnergy() {
        return euPerShot > 0;
    }

    /** EU one trigger pull costs; 0 for a gun that uses rounds. */
    public long euPerShot() {
        return euPerShot;
    }

    /** True for a gun with a hold-to-charge shot on the right button (Bio Gun, Power Hammer). */
    public boolean isCharge() {
        return chargeFullTicks > 0;
    }

    /** Ticks of holding the charge button to a full charge; 0 for a gun that does not charge. */
    public int chargeFullTicks() {
        return chargeFullTicks;
    }

    /** How a held charge turns into rounds spent. */
    public ChargeMode chargeMode() {
        return chargeMode;
    }

    /** Rounds a full SCALED charge spends. */
    public int chargeMaxUnits() {
        return chargeMaxUnits;
    }

    /** Default capacity in EU of a full charge before config (see {@link EnergyCapacity}); 0 for a round gun. */
    public long capacityEu() {
        return capacityEu;
    }

    /** The projectile entity this gun fires. */
    public ProjectileKind projectileKind() {
        return projectileKind;
    }

    /**
     * Knockback multiplier of a hit (Techguns {@code knockbackMultiplier}): 1 is vanilla's push, 0 none, 2 twice the
     * push. Unlike Techguns it changes the strength (GUN-38).
     */
    public double knockback() {
        return knockback;
    }

    /** Blast radius in blocks of a rocket or launcher grenade; 0 for a gun whose projectile does not explode. */
    public double blastRadius() {
        return blastRadius;
    }

    /** Blocks per tick squared a rocket or launcher grenade falls; 0 for a straight bullet. */
    public double projectileGravity() {
        return projectileGravity;
    }

    /** Client recoil animation length in ticks. */
    public int recoilTicks() {
        return recoilTicks;
    }

    /** Client muzzle flash length in ticks. */
    public int muzzleFlashTicks() {
        return muzzleFlashTicks;
    }

    /** NPC ranged-AI range in blocks. */
    public double aiRange() {
        return aiRange;
    }

    /** NPC ranged-AI attack time in ticks (variance is a third of this). */
    public int aiAttackTime() {
        return aiAttackTime;
    }

    /** NPC shots per burst; 0 means single shots. */
    public int aiBurstCount() {
        return aiBurstCount;
    }

    /** Ticks between NPC burst shots. */
    public int aiBurstDelay() {
        return aiBurstDelay;
    }

    /** Builder for {@link GunStats}. Defaults follow the catalog's "defaults when no setter is called". */
    public static final class Builder {

        private final String id;
        private double damage = 1;
        private double damageMin = -1;
        private double dropStart;
        private double dropEnd;
        private boolean dropSet;
        private double spread;
        private double zoomFactor = 1.0;
        private double zoomSpreadMultiplier = 1.0;
        private FireMode fireMode = FireMode.SEMI;
        private int fireRateTicks = 4;
        private int burstShots = 1;
        private int burstDelayTicks = 1;
        private int ammoPerShot = 1;
        private int magazineSize = 1;
        private String ammoType;
        private String looseAmmoType;
        private String emptyContainerType;
        private ReloadMode reloadMode = ReloadMode.MAGAZINE;
        private int ammoItemsPerReload = 1;
        private int reloadTicks = 20;
        private double projectileSpeed = 1.5;
        private int lifetimeTicks = 30;
        private int bulletsPerShot = 1;
        private double pelletSpread;
        private boolean pelletsInLine;
        private ShotPattern pattern = ShotPattern.NONE;
        private double penetration;
        private boolean akimbo;
        private boolean doubleShot;
        private boolean silenced;
        private ProjectileKind projectileKind = ProjectileKind.BULLET;
        private double knockback = 1.0;
        private double blastRadius;
        private double projectileGravity;
        private int recoilTicks = 5;
        private int muzzleFlashTicks = 5;
        private double aiRange = 15;
        private int aiAttackTime = 60;
        private int aiBurstCount;
        private int aiBurstDelay;
        private long euPerShot;
        private long capacityEu;
        private int chargeFullTicks;
        private ChargeMode chargeMode = ChargeMode.LEVELS;
        private int chargeMaxUnits = ChargeMath.LEVEL_COUNT;

        private Builder(String id) {
            this.id = id;
        }

        /** Maximum damage; minimum damage defaults to the same value. */
        public Builder damage(double damage) {
            this.damage = damage;
            return this;
        }

        /** Damage drop-off: full to {@code start} blocks, down to {@code min} at {@code end} blocks. */
        public Builder damageDrop(double start, double end, double min) {
            dropStart = start;
            dropEnd = end;
            damageMin = min;
            dropSet = true;
            return this;
        }

        /** Spread value of the main projectile. */
        public Builder spread(double spread) {
            this.spread = spread;
            return this;
        }

        /** Scope: FOV multiplier (below 1) and spread multiplier while zoomed. */
        public Builder zoom(double factor, double spreadMultiplier) {
            zoomFactor = factor;
            zoomSpreadMultiplier = spreadMultiplier;
            return this;
        }

        /** Fire mode and ticks between shots. */
        public Builder fire(FireMode mode, int rateTicks) {
            fireMode = mode;
            fireRateTicks = rateTicks;
            return this;
        }

        /** Makes the gun a burst gun: shots per trigger and ticks between them. */
        public Builder burst(int shots, int delayTicks) {
            fireMode = FireMode.BURST;
            burstShots = shots;
            burstDelayTicks = delayTicks;
            return this;
        }

        /** Rounds used per trigger pull. */
        public Builder ammoPerShot(int rounds) {
            ammoPerShot = rounds;
            return this;
        }

        /** Rounds in a full gun. */
        public Builder magazine(int capacity) {
            magazineSize = capacity;
            return this;
        }

        /** Ammo type id consumed by a reload. */
        public Builder ammoType(String type) {
            ammoType = type;
            return this;
        }

        /** Ammo type id for loose rounds returned on a forced reload (default: the ammo type). */
        public Builder looseAmmoType(String type) {
            looseAmmoType = type;
            return this;
        }

        /** Ammo type id of the empty container returned by a reload. */
        public Builder emptyContainerType(String type) {
            emptyContainerType = type;
            return this;
        }

        /** Magazine swap, or the shell-by-shell top-up with {@code itemsPerReload} items at most. */
        public Builder reloadMode(ReloadMode mode, int itemsPerReload) {
            reloadMode = mode;
            ammoItemsPerReload = itemsPerReload;
            return this;
        }

        /** Reload duration in ticks. */
        public Builder reloadTicks(int ticks) {
            reloadTicks = ticks;
            return this;
        }

        /** Projectile speed (blocks per tick) and lifetime (ticks). */
        public Builder projectile(double speed, int lifetime) {
            projectileSpeed = speed;
            lifetimeTicks = lifetime;
            return this;
        }

        /** Extra pellets per round and their spread; {@code inLine} spaces them along the flight path. */
        public Builder pellets(int extraPellets, double pelletSpread, boolean inLine) {
            bulletsPerShot = 1 + extraPellets;
            this.pelletSpread = pelletSpread;
            pelletsInLine = inLine;
            return this;
        }

        /** Shot pattern (burst line, ring volley or fan); replaces the main shot plus pellets when active. */
        public Builder pattern(ShotPattern pattern) {
            this.pattern = pattern;
            return this;
        }

        /** Armor penetration, 0 to 1. */
        public Builder penetration(double penetration) {
            this.penetration = penetration;
            return this;
        }

        /** Akimbo pair; {@code doubleShot} fires both guns per trigger. */
        public Builder akimbo(boolean doubleShot) {
            akimbo = true;
            this.doubleShot = doubleShot;
            return this;
        }

        /**
         * Makes the gun an energy gun: one pull costs {@code euPerShot} EU and a full default charge holds
         * {@code capacityEu}. The gun has no reload; its magazine size becomes the shots in a full charge.
         */
        public Builder energy(long euPerShot, long capacityEu) {
            this.euPerShot = euPerShot;
            this.capacityEu = capacityEu;
            return this;
        }

        /**
         * Gives the gun a hold-to-charge shot on the right button, released to fire (I10).
         *
         * @param fullTicks ticks held to a full charge
         * @param mode      LEVELS (three levels) or SCALED (up to {@code maxUnits} rounds)
         * @param maxUnits  rounds a full SCALED charge spends (ignored for LEVELS)
         */
        public Builder charge(int fullTicks, ChargeMode mode, int maxUnits) {
            chargeFullTicks = fullTicks;
            chargeMode = mode;
            chargeMaxUnits = mode == ChargeMode.LEVELS ? ChargeMath.LEVEL_COUNT : maxUnits;
            return this;
        }

        /** The projectile entity to fire (default {@link ProjectileKind#BULLET}). */
        public Builder projectileKind(ProjectileKind kind) {
            projectileKind = kind;
            return this;
        }

        /** Knockback multiplier of a hit: 1 vanilla, 0 none, above 1 stronger (GUN-38). */
        public Builder knockback(double multiplier) {
            knockback = multiplier;
            return this;
        }

        /** Marks a silenced gun: quiet fire event, tooltip line, no other effect. */
        public Builder silenced() {
            this.silenced = true;
            return this;
        }

        /**
         * Blast radius (blocks) and gravity (blocks per tick squared) of an exploding projectile (rocket, launcher
         * grenade).
         */
        public Builder blast(double radius, double gravity) {
            blastRadius = radius;
            projectileGravity = gravity;
            return this;
        }

        /** Client animation lengths in ticks. */
        public Builder animation(int recoilTicks, int muzzleFlashTicks) {
            this.recoilTicks = recoilTicks;
            this.muzzleFlashTicks = muzzleFlashTicks;
            return this;
        }

        /** NPC ranged-AI parameters (Techguns {@code setAIStats}). */
        public Builder ai(double range, int attackTime, int burstCount, int burstDelay) {
            aiRange = range;
            aiAttackTime = attackTime;
            aiBurstCount = burstCount;
            aiBurstDelay = burstDelay;
            return this;
        }

        /** Validates and builds. Throws {@link IllegalArgumentException} on inconsistent values. */
        public GunStats build() {
            return new GunStats(this);
        }
    }
}
