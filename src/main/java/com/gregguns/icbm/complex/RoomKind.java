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
    WATER_TREATMENT(30, 24, false),
    /** Lit crop fields between water channels; dealt only to a slot of at least 20 x 16 walls. */
    UNDERGROUND_FARM(44, 34, false, 20, 16, 0, 0),
    /** Mycelium beds framed by mushroom stems. */
    MUSHROOM_CELLAR(22, 18, false),
    /** Freezer racks, packed ice and reefer containers. */
    COLD_STORAGE(26, 22, false),
    /** Firing lanes down to wool targets; the door is on a short wall, and it needs at least 12 x 22 walls. */
    SHOOTING_RANGE(18, 36, false, 12, 22, 0, 0),
    /** Barred cells and a guard desk. */
    BRIG(26, 20, false),
    /** Lab benches and working GT chemical reactors. */
    CHEM_LAB(26, 22, false),
    /** Shower stalls and wash tubs; a small room that never grows past 18 x 16 walls. */
    SHOWERS_LAUNDRY(14, 12, false, 0, 0, 18, 16),
    /** Pool tables, a bar and sofas. */
    REC_LOUNGE(26, 22, false),
    /** A furnace bank, GT recyclers and ash heaps. */
    INCINERATOR(22, 20, false);

    public final int length;
    public final int depth;
    /** Always present in every complex. */
    public final boolean mandatory;
    /** The smallest walls-included size (along the door wall, away from it) the kind is dealt to; 0 for no minimum. */
    public final int minLateral, minDepth;
    /** The largest walls-included size the kind is placed at or grows to; 0 for the usual caps. */
    public final int maxLateral, maxDepth;

    RoomKind(int length, int depth, boolean mandatory) {
        this(length, depth, mandatory, 0, 0, 0, 0);
    }

    RoomKind(int length, int depth, boolean mandatory, int minLateral, int minDepth, int maxLateral, int maxDepth) {
        this.length = length;
        this.depth = depth;
        this.mandatory = mandatory;
        this.minLateral = minLateral;
        this.minDepth = minDepth;
        this.maxLateral = maxLateral;
        this.maxDepth = maxDepth;
    }

    /**
     * @param lateral walls-included size along the door wall
     * @param depth   walls-included size away from it
     * @return whether a room of this kind may stand at that size (its minimum and maximum, D307)
     */
    public boolean fits(int lateral, int depth) {
        return lateral >= minLateral && depth >= minDepth
            && (maxLateral == 0 || lateral <= maxLateral)
            && (maxDepth == 0 || depth <= maxDepth);
    }

    /** @return true for the small annex rooms (guard post, airlock), which keep the minor hallways' height */
    public boolean small() {
        return this == GUARD_POST || this == DECON_AIRLOCK;
    }
}
