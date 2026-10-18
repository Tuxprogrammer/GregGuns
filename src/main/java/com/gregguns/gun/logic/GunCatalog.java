package com.gregguns.gun.logic;

/**
 * Stat definitions for the guns built so far. Later guns are added here as their milestones land.
 */
public final class GunCatalog {

    /**
     * Ammo type ids used by the catalog: the GregGuns registry names of the ammo items, as declared by
     * {@code com.gregguns.item.ammo.AmmoTypes}.
     */
    public static final class AmmoIds {

        // The ids are the items' GregGuns registry names, so the item glue maps an id to its item directly and the
        // ammo package (com.gregguns.item.ammo.AmmoTypes) has one source of truth. AmmoIdsMatchItemsTest pins this.

        /** Loose 9mm bullets (refund item for forced reloads). */
        public static final String NINE_MM = "ammo_9mm_bullet";
        /** Loose shotgun shells (the Sawed Off's ammo). */
        public static final String SHOTGUN_SHELL = "ammo_shotgun_shell";
        /** Loose stone bullets (the Hand Cannon's round, ULV). */
        public static final String STONE = "ammo_stone_bullet";
        /** Loaded 9mm pistol magazine (18 rounds). */
        public static final String NINE_MM_PISTOL_MAGAZINE = "mag_9mm_pistol";
        /** Empty 9mm pistol magazine. */
        public static final String NINE_MM_PISTOL_MAGAZINE_EMPTY = "mag_9mm_pistol_empty";

        private AmmoIds() {}
    }

    /**
     * The 9mm Pistol.
     *
     * <p>
     * Spec: sheets-handguns-smgs.md, "9mm Pistol", section 3: damage 8 falling to 5 between 18 and 25 blocks (the
     * later builder call wins, GUN-16), lifetime 30 ticks at 1.5 blocks per tick, spread 0.025, no scope, semi-auto
     * every 4 ticks, 18 rounds, 35 tick reload, AI range 24 / attack time 30 / burst 3 / delay 10, recoil 3 ticks
     * and muzzle flash 5 ticks.
     */
    public static final GunStats PISTOL = GunStats.builder("pistol")
        .damage(8.0)
        .damageDrop(18, 25, 5.0)
        .spread(0.025)
        .fire(FireMode.SEMI, 4)
        .magazine(18)
        .ammoType(AmmoIds.NINE_MM_PISTOL_MAGAZINE)
        .looseAmmoType(AmmoIds.NINE_MM)
        .emptyContainerType(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(35)
        .projectile(1.5, 30)
        .penetration(0)
        .animation(3, 5)
        .ai(24, 30, 3, 10)
        .build();

    /**
     * The Revolver.
     *
     * <p>
     * Spec: sheets-handguns-smgs.md, "Revolver", section 3: damage 8 falling to 6 between 12 and 20 blocks, lifetime
     * 25 ticks at 1.5 blocks per tick, spread 0.025, no scope, semi-auto every 6 ticks, 6 rounds loaded straight from
     * one 9mm bullet item (no magazine, no empty container), 45 tick reload, AI range 18 / attack time 90 / burst 6 /
     * delay 20, recoil 6 ticks and muzzle flash 5 ticks.
     */
    public static final GunStats REVOLVER = GunStats.builder("revolver")
        .damage(8.0)
        .damageDrop(12, 20, 6.0)
        .spread(0.025)
        .fire(FireMode.SEMI, 6)
        .magazine(6)
        .ammoType(AmmoIds.NINE_MM)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 25)
        .penetration(0)
        .animation(6, 5)
        .ai(18, 90, 6, 20)
        .build();

    /**
     * The Sawed Off, a double-barrel shotgun.
     *
     * <p>
     * Spec: sheets-rifles-shotguns.md, "Sawed Off", section 3: 4 damage per pellet falling to 1.5 between 1 and 4
     * blocks, lifetime 10 ticks at 1.5 blocks per tick, spread 0.01, 7 extra pellets at spread 0.2 (8 in all, one
     * round per shot), full-auto every 4 ticks, 2 rounds, 28 tick reload, AI range 12 / attack time 60 / burst 2 /
     * delay 20, recoil 5 ticks and muzzle flash 5 ticks. It loads shell by shell: up to 2 shells per reload, one
     * round each, and the rounds already in the gun stay (so a forced reload tops up).
     */
    public static final GunStats SAWED_OFF = GunStats.builder("sawed_off")
        .damage(4.0)
        .damageDrop(1, 4, 1.5)
        .spread(0.01)
        .pellets(7, 0.2, false)
        .fire(FireMode.AUTO, 4)
        .magazine(2)
        .ammoType(AmmoIds.SHOTGUN_SHELL)
        .reloadMode(ReloadMode.INCREMENTAL, 2)
        .reloadTicks(28)
        .projectile(1.5, 10)
        .penetration(0)
        .animation(5, 5)
        .ai(12, 60, 2, 20)
        .build();

    /**
     * The Hand Cannon.
     *
     * <p>
     * Spec: sheets-handguns-smgs.md, "Hand Cannon", section 3: damage 8 falling to 5 between 10 and 25 blocks, speed
     * 1.0 blocks per tick for 25 ticks (gravity 0.015 comes from the SimpleProjectile), spread 0.035, no scope,
     * full-auto every 12 ticks, one round loaded straight from one stone bullet (no magazine, no empty container),
     * 30 tick reload, AI range 12 / attack time 60 / burst 0 / delay 0, recoil 5 ticks and muzzle flash 5 ticks.
     */
    public static final GunStats HANDCANNON = GunStats.builder("handcannon")
        .damage(8.0)
        .damageDrop(10, 25, 5.0)
        .spread(0.035)
        .fire(FireMode.AUTO, 12)
        .magazine(1)
        .ammoType(AmmoIds.STONE)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(30)
        .projectile(1.0, 25)
        .projectileKind(ProjectileKind.GRAVITY_BULLET)
        .penetration(0)
        .animation(5, 5)
        .ai(12, 60, 0, 0)
        .build();

    private GunCatalog() {}
}
