package com.gregguns.icbm.complex.blueprint;

/**
 * How one room kind is furnished (D305): its lamp gap, and what it stamps on the room's {@link Kit}. Every room kind
 * has one in {@link RoomBlueprints}; the hub has its own.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D305
public interface RoomBlueprint {

    /** The usual gap between floor light tiles (D231). */
    int GAP = 6;

    /**
     * @return the room's gap between floor light tiles: 6, 7 for the head-to-head bed bands (an odd gap of 7 keeps
     *         every floor cell within 3 + 3 cells of a tile), 4 for rooms of dense racks or display walls
     */
    default int lampGap() {
        return GAP;
    }

    /**
     * Stamps the room. The kit's floor already holds the doorway zones, the light tiles and the floor holes; the
     * furnisher runs the reachability pass, the shadow fill and the commit afterwards.
     *
     * @param k the room's kit
     */
    void lay(Kit k);
}
