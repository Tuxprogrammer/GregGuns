package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;

/**
 * Shared steps of the works, stores and food blueprints (D305): the least chest spots a room keeps for the loot budget
 * (D315), the machine characters, and the corner caches.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D305; D315
final class Works {

    private Works() {}

    /** @return the chest spots stamped so far */
    static int chests(Kit k) {
        return k.fill().chests;
    }

    /**
     * Adds chest spots until the room holds at least {@code min}: first along the back wall, then down both side walls,
     * then in front of the back row, every other cell, so no chest touches two (the kit's rule holds anyway). A room
     * too
     * small or too full to take them stops short.
     */
    static void topUp(Kit k, int min) {
        int L = k.lateral(), D = k.depth();
        int have = chests(k);
        for (int pass = 0; pass < 2 && have < min; pass++) {
            for (int a = pass; a < L && have < min; a += 2) if (k.put(a, 0, 'C')) have++;
            for (int d = 2 + pass; d < D - 3 && have < min; d += 2) {
                if (k.put(0, d, 'C')) have++;
                if (have < min && k.put(L - 1, d, 'C')) have++;
            }
            for (int a = pass; a < L && have < min; a += 2) if (k.put(a, 1, 'C')) have++;
        }
    }

    /**
     * Container prisms (2 x 3 footprint, two high) two deep along the back wall, every four cells outward from the
     * spine; each prism is stamped whole, so it is one group that wears together (D308).
     */
    static void backContainers(Kit k) {
        k.tile(k.module("KKK", "KKK"), 4, 0, 1, k.lateral() - 2, 1);
    }

    /** The corner caches of every room of 16 x 14 interior or more. */
    static void corners(Kit k) {
        if (k.lateral() >= 16 && k.depth() >= 14) k.corners();
    }

    /** Sets a legend character to a machine of a role. */
    static void machine(Kit k, char c, MachineRole role) {
        k.legend()
            .set(c, Legend.machine(role));
    }

    /** Sets a legend character to a stack of pieces. */
    static void piece(Kit k, char c, FurniturePiece... layers) {
        k.legend()
            .set(c, Legend.piece(layers));
    }

    /** @return the room's area step count (D234: one per 450 interior cells, at least 1) */
    static int steps(Kit k) {
        return LegacyRoom.steps(k);
    }
}
