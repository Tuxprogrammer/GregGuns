package com.gregguns.icbm.complex;

/**
 * The room set (D128, D229). Sizes are a room's nominal footprint, walls included: {@code length} along its door wall,
 * {@code depth} from the door wall to the back. The generator varies them and grows rooms into the rock around them,
 * so a room ends up at about four to six times the I6 room's floor area.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Rooms)
public enum RoomKind {

    /** The hub. Its size is {@link ComplexLayout#HUB_LONG} x {@link ComplexLayout#HUB_SHORT}, not these numbers. */
    ASSEMBLY_BAY(51, 40, true),
    LAUNCH_CONTROL(30, 22, true),
    RADAR_TRACKING(26, 22, false),
    COMMS(24, 20, true),
    COMMANDER_OFFICE(22, 20, true),
    WARHEAD_MAGAZINE(30, 24, true),
    FUEL_DEPOT(28, 24, false),
    GENERATOR_HALL(32, 26, true),
    AIR_HANDLING(24, 20, false),
    PUMP_ROOM(24, 20, false),
    WORKSHOP(28, 24, false),
    BUNKS(28, 22, false),
    STOCKPILE(28, 24, false),
    MESS_GALLEY(26, 22, false),
    INFIRMARY(24, 20, false),
    ARMORY(24, 20, true),
    DECON_AIRLOCK(12, 12, false),
    GUARD_POST(10, 10, false),
    /** A garage for rovers and carts, off a major hall (wide doorway). */
    VEHICLE_BAY(36, 30, false),
    /** Rows of racks and a comms machine. */
    SERVER_ROOM(26, 22, false),
    /** The missile assembly floor beside the silo gallery: the NASA Workbench, workbenches and casing stock. */
    MISSILE_ASSEMBLY(36, 30, true),
    /** A big mess hall: rows of tables and a counter. */
    CAFETERIA(32, 26, false),
    /** Basins of standing water and pumps. */
    WATER_TREATMENT(30, 24, false);

    public final int length;
    public final int depth;
    /** Always present in every complex. */
    public final boolean mandatory;

    RoomKind(int length, int depth, boolean mandatory) {
        this.length = length;
        this.depth = depth;
        this.mandatory = mandatory;
    }

    /** @return true for the small annex rooms (guard post, airlock), which keep the minor hallways' height */
    public boolean small() {
        return this == GUARD_POST || this == DECON_AIRLOCK;
    }
}
