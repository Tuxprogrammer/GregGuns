package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Dir;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Opening;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The brig (D305): a row of cells along the back wall, each 3 x 4 inside behind iron bars, with a bunk (a bed, no foot
 * chest), a cauldron and a closed iron door; a second row facing the first across a 3-wide corridor in a deep room.
 * Guard desks with consoles face the cells, evidence lockers and chests line the side walls, an interrogation table
 * and crates fill the front. The spine runs only from the door to the corridor, and no cell stands across another
 * door's way in.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Brig); D305
public final class Brig implements RoomBlueprint {

    /** A cell's pitch along the wall: 3 cells inside and the bar wall between. */
    static final int PITCH = 4;

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.STOCKPILE); // the chest budget picks the table by room kind
        k.legend()
            .set('D', Legend.piece(FurniturePiece.IRON_DOOR, FurniturePiece.IRON_DOOR_TOP));
        cells(k, 0, false);
        int front = 6;
        if (D >= 24) {
            cells(k, 8, true);
            front = 14;
        }
        k.spine(front);
        int lo = k.spineLo(), hi = k.spineHi();
        k.anchor(lo - 5, front + 3, "cdd", "hhh");
        k.anchor(hi + 3, front + 3, "ddc", "hhh");
        k.column(0, front + 2, D - 4, "L.C.");
        k.column(L - 1, front + 2, D - 4, "LC.");
        k.anchor(3, D - 6, ".h.", "hTh", ".C.");
        k.anchor(L - 8, D - 6, "kk.r", "kC..");
        k.anchor(0, D - 3, "k.", "Ck");
        k.anchor(L - 2, D - 3, ".k", "kC");
    }

    /**
     * @return whether the cell at a0 (rows d0 to d0 + 4) would stand across another door's way in: a door in the back
     *         wall behind it, or a door in the side wall beside it
     */
    static boolean blocksAnOpening(Kit k, int a0, int d0, boolean flipped) {
        int L = k.lateral();
        for (Opening o : k.openings()) {
            if (o.primary) continue;
            switch (o.wall) {
                case BACK:
                    if (!flipped && o.hi >= a0 - 1 && o.lo <= a0 + PITCH) return true;
                    break;
                case LEFT:
                    if (a0 == 0 && o.hi >= d0 - 1 && o.lo <= d0 + 5) return true;
                    break;
                case RIGHT:
                    if (a0 + PITCH >= L - 1 && o.hi >= d0 - 1 && o.lo <= d0 + 5) return true;
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    /**
     * A row of cells, 5 rows deep from d0: the bunk and the cauldron inside, bars between cells and along the front,
     * the door in the front bars. Facing the room's back (the first row) or its front (the second, mirrored in depth).
     */
    private static void cells(Kit k, int d0, boolean flipped) {
        int L = k.lateral();
        int bed = flipped ? d0 + 4 : d0, sink = flipped ? d0 + 1 : d0 + 3, bars = flipped ? d0 : d0 + 4;
        Legend.Entry door = Legend.piece(FurniturePiece.IRON_DOOR, FurniturePiece.IRON_DOOR_TOP)
            .facing(flipped ? Dir.TO_BACK : Dir.TO_DOOR);
        for (int a0 = 0; a0 + PITCH - 1 < L; a0 += PITCH) {
            int wall = a0 + PITCH - 1;
            if (blocksAnOpening(k, a0, d0, flipped)) continue; // the way in from another door stays open
            for (int d = d0; d <= d0 + 4; d++) k.put(wall, d, 'x');
            for (int a = a0; a < wall; a++) if (a != a0 + 1) k.put(a, bars, 'x');
            // The door, or beside it when a light tile lies under its place.
            if (!k.put(a0 + 1, bars, door, 'D') && k.put(a0 + 2, bars, door, 'D')) k.put(a0 + 1, bars, 'x');
            if (!k.anchor(a0, bed, "Bb")) k.anchor(a0 + 1, bed, "Bb");
            if (!k.put(a0, sink, 'u')) k.put(a0 + 2, sink, 'u');
        }
    }
}
