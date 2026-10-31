package com.gregguns.icbm.complex.blueprint.rooms;

import java.util.ArrayList;
import java.util.List;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Dir;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Opening;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The brig (D305, D322): a row of cells along the back wall, each 3 x 4 inside, with a bunk (a bed, no foot chest), a
 * cauldron and a closed iron door; a second row facing the first across a 3-wide corridor in a deep room. A cell is
 * walled in the room's own wall block (partitions that never wear) on its sides, and on its back where the room wall is
 * not behind it; its front is iron bars that never wear, a wall jamb and the door, so the door stands between two solid
 * blocks and no gap is left beside it or the bars. A cell is built whole or not at all, so none is ever left open: a
 * light tile or a floor hole under its wall, bars or door is built over, and a cell that would cross a doorway, the
 * aisle or another door's way in is left out. Guard desks with consoles face the cells, evidence lockers and chests
 * line the
 * side walls, an interrogation table and crates fill the front. The aisle runs from the door to the corridor, through
 * the gap it leaves in the second row.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Brig); D305; D322
public final class Brig implements RoomBlueprint {

    /** A cell's pitch along the wall: 3 cells inside and the wall between. */
    static final int PITCH = 4;

    /** The corridor between the two rows of a deep brig: its first row. */
    static final int CORRIDOR = 5;

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.STOCKPILE); // the chest budget picks the table by room kind
        k.legend()
            .set('x', Legend.piece(FurniturePiece.CELL_BARS, FurniturePiece.CELL_BARS))
            .set('#', Legend.piece(FurniturePiece.PARTITION, FurniturePiece.PARTITION));
        int front = 6;
        if (D >= 24) {
            // The aisle runs on to the corridor first, so the second row leaves it a way through.
            k.spine(CORRIDOR);
            cells(k, 0, false);
            cells(k, 8, true);
            front = 14;
        } else {
            k.spine(front);
            cells(k, 0, false);
        }
        int lo = k.spineLo(), hi = k.spineHi();
        k.anchor(lo - 5, front + 3, "cdd", "hhh");
        k.anchor(hi + 3, front + 3, "ddc", "hhh");
        k.column(0, front + 2, D - 4, "L.C.");
        k.column(L - 1, front + 2, D - 4, "LC.");
        k.anchor(3, D - 6, ".h.", "hTh", ".C.");
        k.anchor(L - 8, D - 6, "kk.r", "kC..");
        k.anchor(0, D - 3, "k.", "Ck");
        k.anchor(L - 2, D - 3, ".k", "kC");
        k.guards(LegacyRoom.steps(k), D - 3);
    }

    /**
     * @return whether the cell at a0 (rows d0 to d0 + 5) would stand across another door's way in: a door in the back
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
     * A row of cells from d0: facing the room's back (the first row, its back the room's back wall: the bars on row
     * d0 + 4) or its front (the second, mirrored in depth: the bars on row d0, a wall on row d0 + 5). Each cell's
     * front, from its low side: bars, a wall jamb, the door, and the cell's side wall.
     */
    private static void cells(Kit k, int d0, boolean flipped) {
        int L = k.lateral();
        int bed = flipped ? d0 + 4 : d0, sink = flipped ? d0 + 1 : d0 + 3, bars = flipped ? d0 : d0 + 4;
        int top = d0, bottom = flipped ? d0 + 5 : d0 + 4; // the rows the side walls run
        Legend.Entry door = Legend.piece(FurniturePiece.IRON_DOOR, FurniturePiece.IRON_DOOR_TOP)
            .facing(flipped ? Dir.TO_BACK : Dir.TO_DOOR);
        for (int a0 = 0; a0 + PITCH - 1 < L; a0 += PITCH) {
            if (blocksAnOpening(k, a0, d0, flipped)) continue; // the way in from another door stays open
            List<int[]> walls = new ArrayList<int[]>();
            for (int d = top; d <= bottom; d++) {
                if (a0 > 0) walls.add(new int[] { a0 - 1, d });
                walls.add(new int[] { a0 + PITCH - 1, d });
            }
            if (flipped) for (int a = a0; a < a0 + PITCH - 1; a++) walls.add(new int[] { a, bottom });
            walls.add(new int[] { a0 + 1, bars }); // the jamb on the door's low side
            if (!buildable(k, walls, a0, bars, top, bottom)) continue; // never half a cell
            for (int[] w : walls) k.build(w[0], w[1], '#');
            k.build(a0, bars, 'x');
            k.legend()
                .set('D', door);
            k.build(a0 + 2, bars, 'D');
            k.keepClear(a0 + 2, flipped ? bars - 1 : bars + 1); // the way to the door stays open
            if (!k.anchor(a0, bed, "Bb")) k.anchor(a0 + 1, bed, "Bb");
            if (!k.put(a0, sink, 'u')) k.put(a0 + 1, sink, 'u');
        }
    }

    /**
     * @return whether a whole cell can stand at a0: every wall cell free (a light tile or a floor hole is built over)
     *         or already the neighbor's wall, and the bars and door cells and the cell's inside free of doorways,
     *         aisles and pieces
     */
    private static boolean buildable(Kit k, List<int[]> walls, int a0, int bars, int top, int bottom) {
        for (int[] w : walls) {
            if (k.pieceAt(w[0], w[1]) == FurniturePiece.PARTITION) continue;
            if (!open(k, w[0], w[1])) return false;
        }
        for (int a = a0; a < a0 + PITCH - 1; a++) {
            for (int d = top; d <= bottom; d++) {
                boolean wall = false;
                for (int[] w : walls) wall |= w[0] == a && w[1] == d;
                if (wall) continue;
                // The bars and the door must stand; the inside may hold a floor hole but no doorway or aisle.
                if (!open(k, a, d)) return false;
            }
        }
        return true;
    }

    /**
     * @return whether a cell is in the room with nothing on it or reserved and no doorway under it (a light tile or a
     *         floor hole is built over)
     */
    private static boolean open(Kit k, int a, int d) {
        return k.in(a, d) && k.at(a, d) == null && !k.reserved(a, d) && !k.kept(a, d) && !k.doorway(a, d);
    }
}
