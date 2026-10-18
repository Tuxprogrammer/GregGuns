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
        /** Loose rifle bullets (the Bolt Action Rifle's ammo; one item fills the gun). */
        public static final String RIFLE = "ammo_rifle_bullet";
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
     * The Bolt Action Rifle, the first scoped gun.
     *
     * <p>
     * Spec: sheets-rifles-shotguns.md, "Bolt Action Rifle", section 3: damage 16 falling to 10 between 20 and 30
     * blocks, lifetime 40 ticks at 2.25 blocks per tick (the base 1.5 times 1.5), spread 0.05, armor penetration 0.15,
     * semi-auto every 25 ticks (the rechamber), 6 rounds loaded straight from one rifle bullet item (no magazine, no
     * empty container), 50 tick reload, scope FOV x0.35 and spread x0.125 while zoomed, AI range 36 / attack time 60 /
     * no burst, recoil 15 ticks and muzzle flash 5 ticks.
     */
    // Deviation: Techguns' AI attack time has a variance of 20 ticks; GunStats has no variance field, so the NPC uses
    // the plain 60.
    public static final GunStats BOLT_ACTION = GunStats.builder("bolt_action_rifle")
        .damage(16.0)
        .damageDrop(20, 30, 10.0)
        .spread(0.05)
        .zoom(0.35, 0.125)
        .fire(FireMode.SEMI, 25)
        .magazine(6)
        .ammoType(AmmoIds.RIFLE)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(50)
        .projectile(2.25, 40)
        .penetration(0.15)
        .animation(15, 5)
        .ai(36, 60, 0, 0)
        .build();

    private GunCatalog() {}
}
