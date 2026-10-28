package com.gregguns.icbm.complex;

/** The D128 room set. Sizes are the footprint (along the wing, depth off it) of a room slot. */
public enum RoomKind {

    ASSEMBLY_BAY(36, 28, true),
    LAUNCH_CONTROL(14, 12, true),
    RADAR_TRACKING(14, 12, false),
    COMMS(12, 10, true),
    COMMANDER_OFFICE(12, 10, true),
    WARHEAD_MAGAZINE(16, 12, true),
    FUEL_DEPOT(14, 12, false),
    GENERATOR_HALL(16, 12, true),
    AIR_HANDLING(12, 10, false),
    PUMP_ROOM(12, 10, false),
    WORKSHOP(14, 12, false),
    BUNKS(14, 10, false),
    STOCKPILE(14, 12, false),
    MESS_GALLEY(14, 12, false),
    INFIRMARY(12, 10, false),
    ARMORY(12, 10, true),
    DECON_AIRLOCK(8, 8, false),
    GUARD_POST(6, 6, false);

    public final int length;
    public final int depth;
    /** Always present in every complex. */
    public final boolean mandatory;

    RoomKind(int length, int depth, boolean mandatory) {
        this.length = length;
        this.depth = depth;
        this.mandatory = mandatory;
    }
}
