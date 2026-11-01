package com.gregguns.icbm.complex;

/**
 * The loot-table keys of the silo complex (D316). The room a chest stands in picks its table ({@code RoomLoot}); a
 * chest mark's key matters only for the commander's {@link #SAFE} and the armory's {@link #ARMORY_AMMO} display
 * chests, and a key that names no table falls back to the room's own. Each key becomes one Forge chest category per
 * ceiling, {@code gregguns:silo_<key>_<ceiling>}.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Room tables); D316
public final class LootKeys {

    /** The hub's salvage: bulk stock, hardware, crated rations, now and then a salvaged missile body. */
    public static final String SALVAGE = "salvage";
    /** Warehouse racks: bulk ingots, plates, wool, logs, torches. */
    public static final String STOCKPILE = "stockpile";
    /** The commander's safe: the complex's best metals and a few arms. */
    public static final String SAFE = "safe";
    public static final String COMMS = "comms";
    /** The armory's back-wall racks: guns, ammo, grenades, gun parts, combat armor. */
    public static final String ARMORY = "armory";
    /** The ammo chest under an armory display gun; it holds the ammo of that gun, not a table draw. */
    public static final String ARMORY_AMMO = "armory_ammo";
    public static final String WORKSHOP = "workshop";
    public static final String MEDICAL = "medical";
    /** The mess kitchen: ingredients, meals, cookware. */
    public static final String GALLEY = "galley";
    /** Barracks foot chests: armor heavy, with personal effects. */
    public static final String BUNKS = "bunks";
    public static final String FUEL = "fuel";
    /** Launch Control: circuits, data sticks, guidance computers, maps. */
    public static final String CONTROL = "control";
    /** The warhead magazine: warhead components and explosives (never a live warhead). */
    public static final String MAGAZINE = "magazine";
    public static final String RADAR = "radar";
    public static final String SERVER = "server";
    public static final String OFFICE = "office";
    /** The missile assembly floor: missile body parts and warhead components. */
    public static final String ASSEMBLY = "assembly";
    public static final String POWER = "power";
    public static final String HVAC = "hvac";
    public static final String PUMPS = "pumps";
    public static final String MOTORPOOL = "motorpool";
    public static final String CHEMLAB = "chemlab";
    public static final String INCINERATOR = "incinerator";
    /** Cold storage: raw meat, canned and preserved food, produce. */
    public static final String PANTRY = "pantry";
    /** The cafeteria: meals, snacks, drinks. */
    public static final String CANTEEN = "canteen";
    public static final String FARM = "farm";
    public static final String CELLAR = "cellar";
    public static final String LAUNDRY = "laundry";
    public static final String LOUNGE = "lounge";
    public static final String DECON = "decon";
    public static final String GUARD = "guard";
    public static final String BRIG = "brig";
    /** The shooting range: guns and ammo heavy. */
    public static final String RANGE = "range";

    private LootKeys() {}
}
