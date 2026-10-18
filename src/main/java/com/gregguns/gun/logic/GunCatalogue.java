package com.gregguns.gun.logic;

/**
 * Stat definitions for the guns built so far. Later guns are added here as their milestones land.
 */
public final class GunCatalogue {

    /**
     * Ammo type ids used by the catalogue. The ammo module's {@code AmmoType} ids must equal these strings. Ids are
     * lower-case, start with the calibre and use underscores.
     */
    public static final class AmmoIds {

        /** Loose 9mm bullets (refund item for forced reloads). */
        public static final String NINE_MM = "9mm";
        /** Loaded 9mm pistol magazine (18 rounds). */
        public static final String NINE_MM_PISTOL_MAGAZINE = "9mm_pistol_magazine";
        /** Empty 9mm pistol magazine. */
        public static final String NINE_MM_PISTOL_MAGAZINE_EMPTY = "9mm_pistol_magazine_empty";

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

    private GunCatalogue() {}
}
