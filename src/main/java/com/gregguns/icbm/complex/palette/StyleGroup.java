package com.gregguns.icbm.complex.palette;

import java.util.Locale;

import com.gregguns.icbm.complex.RoomKind;

/**
 * What a room is dressed as. Several room kinds share a group, and the halls have their own. Kinds are mapped by name
 * so the I6b room kinds (vehicle bay, server room and the rest) need no change here.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Style sets); D240
public enum StyleGroup {

    HUB(true),
    HALL_MAJOR(true),
    HALL_MINOR(true),
    GALLERY(true),
    COMMAND(false),
    CREW(false),
    CAFETERIA(false),
    MEDICAL(false),
    POWER(true),
    WORKSHOP(true),
    STORAGE(true),
    GUARD(true),
    VEHICLE_BAY(true),
    SERVER_ROOM(false),
    MISSILE_ASSEMBLY(true),
    WATER_TREATMENT(true);

    /** True when a planet's local rock suits this group (heavy, rugged rooms). */
    public final boolean rugged;

    StyleGroup(boolean rugged) {
        this.rugged = rugged;
    }

    /** @return the group of a room kind */
    public static StyleGroup of(RoomKind kind) {
        return ofName(kind.name());
    }

    /**
     * @param name a {@link RoomKind} constant name, or HUB_HALL, MAJOR_HALL, MINOR_HALL, SILO_GALLERY
     * @return the group; GUARD for an unknown name
     */
    public static StyleGroup ofName(String name) {
        switch (name.toUpperCase(Locale.ROOT)) {
            case "ASSEMBLY_BAY":
            case "HUB_HALL":
            case "HUB":
                return HUB;
            case "MAJOR_HALL":
                return HALL_MAJOR;
            case "MINOR_HALL":
                return HALL_MINOR;
            case "SILO_GALLERY":
            case "GALLERY":
                return GALLERY;
            case "LAUNCH_CONTROL":
            case "COMMS":
            case "COMMANDER_OFFICE":
            case "RADAR_TRACKING":
                return COMMAND;
            case "BUNKS":
                return CREW;
            case "MESS_GALLEY":
            case "CAFETERIA":
                return CAFETERIA;
            case "INFIRMARY":
            case "DECON_AIRLOCK":
                return MEDICAL;
            case "GENERATOR_HALL":
            case "AIR_HANDLING":
            case "PUMP_ROOM":
            case "FUEL_DEPOT":
                return POWER;
            case "WORKSHOP":
                return WORKSHOP;
            case "WARHEAD_MAGAZINE":
            case "STOCKPILE":
            case "ARMORY":
                return STORAGE;
            case "VEHICLE_BAY":
                return VEHICLE_BAY;
            case "SERVER_ROOM":
                return SERVER_ROOM;
            case "MISSILE_ASSEMBLY":
                return MISSILE_ASSEMBLY;
            case "WATER_TREATMENT":
                return WATER_TREATMENT;
            default:
                return GUARD;
        }
    }
}
