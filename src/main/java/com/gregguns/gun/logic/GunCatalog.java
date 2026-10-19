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

        /** Loose high caliber rounds (the AS50's refund item, HV). */
        public static final String HIGH_CALIBER_ROUND = "ammo_high_caliber_round";
        /** Loaded AS50 magazine (10 rounds). */
        public static final String AS50_MAGAZINE = "mag_as50";
        /** Empty AS50 magazine. */
        public static final String AS50_MAGAZINE_EMPTY = "mag_as50_empty";
        /** Loaded LMG magazine (100 rounds). */
        public static final String LMG_MAGAZINE = "mag_lmg";
        /** Empty LMG magazine. */
        public static final String LMG_MAGAZINE_EMPTY = "mag_lmg_empty";

        /** Loose advanced bullets (the Advanced Magazine's refund item, EV). */
        public static final String ADVANCED_BULLET = "ammo_advanced_bullet";
        /** Loaded Advanced Magazine (PDW 20 rounds, Pulse Rifle 12). */
        public static final String ADVANCED_MAGAZINE = "mag_advanced";
        /** Empty Advanced Magazine. */
        public static final String ADVANCED_MAGAZINE_EMPTY = "mag_advanced_empty";
        /** Loaded Minigun Ammo Drum (200 rounds). */
        public static final String MINIGUN_DRUM = "mag_minigun_drum";
        /** Empty Minigun Ammo Drum. */
        public static final String MINIGUN_DRUM_EMPTY = "mag_minigun_drum_empty";

        /** Loose Nether Charge (the Nether Blaster's round, LuV; 10 shots, no empty item). */
        public static final String NETHER_CHARGE = "ammo_nether_charge";
        /** Loose Tactical Nuke Rocket (the Nuke Launcher's round, ZPM). */
        public static final String ROCKET_NUKE = "ammo_rocket_nuke";
        /** Nuclear Power Cell (a component, the Nuclear Death Ray's consumable, ZPM). */
        public static final String CELL_NUCLEAR = "cell_nuclear";
        /** Depleted Nuclear Power Cell (returned on use). */
        public static final String CELL_NUCLEAR_DEPLETED = "cell_nuclear_depleted";

        /** Loose rocket (the Rocket Launcher's round, HV). */
        public static final String ROCKET = "ammo_rocket";
        /** Loose 40 mm grenade (the Grenade Launcher's round, MV). */
        public static final String GRENADE_40MM = "ammo_40mm_grenade";

        /**
         * The Fuel Tank (the Flamethrower's ammo, a Forge fluid container, so not an ammo-package item). Only a full
         * tank counts as ammo; the same id as the emptied container a reload hands back. See {@link FluidAmmo}.
         */
        public static final String FUEL_TANK = "tank_fuel";
        /** The Bio Tank (the Bio Gun's ammo, a Forge fluid container holding Biomass). Same rules as the Fuel Tank. */
        public static final String BIO_TANK = "tank_bio";
        /**
         * The Compressed Air Tank (the Power Hammers' ammo, a Forge fluid container registered by the armor module's
         * tank list). Same contract as {@link #FUEL_TANK}.
         */
        public static final String AIR_TANK = "tank_air";

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
