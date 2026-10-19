package com.gregguns.gun.logic;

import com.gregguns.gun.def.BoltActionDef;
import com.gregguns.gun.def.HandCannonDef;
import com.gregguns.gun.def.PistolDef;
import com.gregguns.gun.def.RevolverDef;
import com.gregguns.gun.def.SawedOffDef;

/**
 * Aliases for the stats of the first five guns, and the ammo ids their stats name. The stats themselves live in each
 * gun's definition ({@code com.gregguns.gun.def}, for example {@code PistolDef}); a new gun adds nothing here, it
 * adds a definition and one line in {@code GunDefinitions.ALL}. The aliases stay so code and tests that name a
 * built-in gun's stats keep working.
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
        /** Loose rifle bullets (the Bolt Action Rifle's ammo; one item fills the gun). */
        public static final String RIFLE = "ammo_rifle_bullet";
        /** Loaded 9mm pistol magazine (18 rounds). */
        public static final String NINE_MM_PISTOL_MAGAZINE = "mag_9mm_pistol";
        /** Empty 9mm pistol magazine. */
        public static final String NINE_MM_PISTOL_MAGAZINE_EMPTY = "mag_9mm_pistol_empty";
        /** Loaded SMG magazine (Thompson 20 rounds, Mac10 32). */
        public static final String SMG_MAGAZINE = "mag_smg";
        /** Empty SMG magazine. */
        public static final String SMG_MAGAZINE_EMPTY = "mag_smg_empty";
        /** Loaded assault rifle magazine (30 rounds). */
        public static final String ASSAULT_RIFLE_MAGAZINE = "mag_assault_rifle";
        /** Empty assault rifle magazine. */
        public static final String ASSAULT_RIFLE_MAGAZINE_EMPTY = "mag_assault_rifle_empty";

        /** Loose rocket (the Rocket Launcher's round, HV). */
        public static final String ROCKET = "ammo_rocket";
        /** Loose 40 mm grenade (the Grenade Launcher's round, MV). */
        public static final String GRENADE_40MM = "ammo_40mm_grenade";

        private AmmoIds() {}
    }

    /** The 9mm Pistol's stats. */
    public static final GunStats PISTOL = PistolDef.STATS;

    /** The Revolver's stats. */
    public static final GunStats REVOLVER = RevolverDef.STATS;

    /** The Sawed Off's stats. */
    public static final GunStats SAWED_OFF = SawedOffDef.STATS;

    /** The Hand Cannon's stats. */
    public static final GunStats HANDCANNON = HandCannonDef.STATS;

    /** The Bolt Action Rifle's stats. */
    public static final GunStats BOLT_ACTION = BoltActionDef.STATS;

    private GunCatalog() {}
}
