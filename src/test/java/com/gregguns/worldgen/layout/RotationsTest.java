package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Rotation tables: footprint mapping is a bijection with an exact inverse, and facing metas turn clockwise. */
class RotationsTest {

    @Test
    void everyRotationMapsTheLayoutOntoItsFootprintExactlyOnce() {
        int sx = 5;
        int sz = 3;
        for (int r = 0; r < 4; r++) {
            int w = Rotations.width(r, sx, sz);
            int d = Rotations.depth(r, sx, sz);
            boolean[][] hit = new boolean[w][d];
            for (int lx = 0; lx < sx; lx++) {
                for (int lz = 0; lz < sz; lz++) {
                    int dx = Rotations.offsetX(r, lx, lz, sx, sz);
                    int dz = Rotations.offsetZ(r, lx, lz, sx, sz);
                    assertTrue(dx >= 0 && dx < w && dz >= 0 && dz < d, "rotation " + r + " leaves the footprint");
                    assertTrue(!hit[dx][dz], "rotation " + r + " maps two cells to one column");
                    hit[dx][dz] = true;
                    assertEquals(lx, Rotations.layoutX(r, dx, dz, sx, sz));
                    assertEquals(lz, Rotations.layoutZ(r, dx, dz, sx, sz));
                }
            }
        }
    }

    @Test
    void clockwiseQuarterTurnSendsEastToSouth() {
        // layout +x (east) must point to world +z (south) after one clockwise turn
        int sx = 4;
        int sz = 2;
        assertEquals(Rotations.offsetX(1, 0, 0, sx, sz), Rotations.offsetX(1, 1, 0, sx, sz));
        assertEquals(1, Rotations.offsetZ(1, 1, 0, sx, sz) - Rotations.offsetZ(1, 0, 0, sx, sz));
        // layout +z (south) points to world -x (west)
        assertEquals(-1, Rotations.offsetX(1, 0, 1, sx, sz) - Rotations.offsetX(1, 0, 0, sx, sz));
    }

    @Test
    void halfTurnReversesBothAxes() {
        assertEquals(3, Rotations.offsetX(2, 0, 0, 4, 2));
        assertEquals(1, Rotations.offsetZ(2, 0, 0, 4, 2));
    }

    @Test
    void facingKindsCycleClockwiseAndWrap() {
        // stairs: east -> south -> west -> north
        assertEquals(2, RotationKind.STAIRS.rotate(0, 1));
        assertEquals(1, RotationKind.STAIRS.rotate(0, 2));
        assertEquals(3, RotationKind.STAIRS.rotate(0, 3));
        // upside-down bit survives
        assertEquals(6, RotationKind.STAIRS.rotate(4, 1));
        // chest facing: north -> east -> south -> west
        assertEquals(5, RotationKind.FACING.rotate(2, 1));
        assertEquals(4, RotationKind.FACING.rotate(2, 3));
        // torch: east -> south
        assertEquals(3, RotationKind.TORCH.rotate(1, 1));
        // door: low bits turn, open (4) and top-half (8) bits stay
        assertEquals(1 | 4, RotationKind.DOOR.rotate(0 | 4, 1));
        assertEquals(0 | 8, RotationKind.DOOR.rotate(3 | 8, 1));
        // trapdoor: north -> east, open bit stays
        assertEquals(3 | 4, RotationKind.TRAPDOOR.rotate(0 | 4, 1));
    }

    @Test
    void everyKindIsIdentityAfterFourTurnsAndComposes() {
        for (RotationKind k : RotationKind.values()) {
            for (int meta = 0; meta < 16; meta++) {
                assertEquals(meta, k.rotate(meta, 4), k + " meta " + meta);
                assertEquals(meta, k.rotate(meta, 0));
                assertEquals(k.rotate(meta, 3), k.rotate(k.rotate(meta, 1), 2), k + " meta " + meta);
            }
        }
    }

    @Test
    void axisSwapsOnOddTurnsOnly() {
        assertEquals(8, RotationKind.AXIS.rotate(4, 1));
        assertEquals(4, RotationKind.AXIS.rotate(8, 3));
        assertEquals(4, RotationKind.AXIS.rotate(4, 2));
        assertEquals(0, RotationKind.AXIS.rotate(0, 1)); // vertical log
        assertEquals(12, RotationKind.AXIS.rotate(12, 1)); // bark
        assertEquals(1 | 8, RotationKind.AXIS.rotate(1 | 4, 1)); // wood type kept
    }

    @Test
    void noneAndUnknownValuesAreUntouched() {
        assertEquals(9, RotationKind.NONE.rotate(9, 1));
        assertEquals(0, RotationKind.FACING.rotate(0, 1)); // piston facing down is not horizontal
        assertNull(RotationKind.byToken("sideways"));
        assertEquals(RotationKind.DOOR, RotationKind.byToken("door"));
    }
}
