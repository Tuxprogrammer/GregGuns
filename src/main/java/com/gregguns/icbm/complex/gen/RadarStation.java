package com.gregguns.icbm.complex.gen;

import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.defense.RadarShape;

/**
 * The small radar (D125) of a radar station, cell by cell, read from {@link RadarShape}, the very piece the radar's
 * StructureLib definition builds, so the two cannot drift. Pure. The base is three aisles of 3 x 3 over two layers,
 * with
 * the pedestal on the top layer of the middle aisle; the controller sits in the front aisle's upper layer and faces
 * north. The Energy Hatch position (under the controller) holds a Radar Casing: the station is inert until a player
 * puts an Energy Hatch there, the way a silo is missing one part (D210).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Radar stations); radar.md (The small radar)
public final class RadarStation {

    /** A cell that holds nothing. */
    public static final int NOTHING = 0;
    /** A Radar Casing. */
    public static final int CASING = 1;

    private RadarStation() {}

    /**
     * @param dx    -1 to 1 along X, from the station's center
     * @param dz    -1 to 1 along Z; the front aisle (the controller's) is -1
     * @param layer 1 and 2 for the base, 3 for the pedestal level
     * @return {@link #NOTHING}, {@link #CASING}, {@link Cell#RADAR_CONTROLLER} or {@link Cell#HATCH_MAINTENANCE}
     */
    public static int cellAt(int dx, int dz, int layer) {
        if (layer < 1 || layer > 3) return NOTHING;
        char ch = RadarShape.PIECES[0][dz + 1][3 - layer].charAt(dx + 1);
        switch (ch) {
            case '~':
                return Cell.RADAR_CONTROLLER;
            case 'C':
                return CASING;
            case 'H':
                // The Maintenance Hatch beside the Energy Hatch's place, in the front aisle's lower layer.
                return dz == -1 && dx == 1 && layer == 1 ? Cell.HATCH_MAINTENANCE : CASING;
            default:
                return NOTHING;
        }
    }

    /** @return x, y, z offsets from the station's center column at the hut roof level, of the controller */
    public static int[] controllerOffset() {
        return new int[] { 0, 2, -1 };
    }

    /** @return the offsets of the Energy Hatch's place (a casing until a player swaps in a hatch) */
    public static int[] energyHatchOffset() {
        return new int[] { 0, 1, -1 };
    }
}
