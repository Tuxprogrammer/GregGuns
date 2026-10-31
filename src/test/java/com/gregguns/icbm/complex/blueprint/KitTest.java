package com.gregguns.icbm.complex.blueprint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.blueprint.rooms.Bunks;

/** D305: the blueprint kit's stamping rules, on synthetic floors. */
class KitTest {

    /**
     * A floor like the furnisher's: a 3-wide doorway in the middle of the door wall with its 2-deep zone, and light
     * tiles on the grid of {@code gap}.
     */
    static Kit room(int L, int D, int gap) {
        byte[] base = new byte[L * D];
        int lo = (L - 3) / 2, hi = lo + 2;
        for (int a : grid(L, gap)) for (int d : grid(D, gap)) base[a * D + d] = Kit.TILE;
        for (int a = lo - 1; a <= hi + 1; a++) for (int d = D - 2; d < D; d++) base[a * D + d] = Kit.DOORWAY;
        List<Opening> o = new ArrayList<Opening>();
        o.add(new Opening(Opening.Wall.DOOR, lo, hi, true));
        return new Kit(L, D, base, o, 12345L, 1);
    }

    static int[] grid(int n, int gap) {
        int c = Math.max(1, (n + gap - 1) / gap);
        int[] out = new int[c];
        for (int i = 0; i < c; i++) out[i] = (2 * i + 1) * n / (2 * c);
        return out;
    }

    @Test
    void aStampIsAllOrNothing() {
        Kit k = room(12, 10, 6);
        k.reserve(5, 5, 4, 4);
        assertFalse(k.anchor(4, 4, "kkk"), "one cell is reserved");
        assertNull(k.pieceAt(4, 4), "nothing of a dropped stamp stays");
        assertTrue(k.anchor(4, 5, "kkk"));
        assertEquals(FurniturePiece.CRATE, k.pieceAt(6, 5));
    }

    @Test
    void lightTilesTakeOnlyThinPieces() {
        Kit k = room(12, 12, 6);
        int a = grid(12, 6)[0], d = grid(12, 6)[0];
        assertTrue(k.tile(a, d));
        assertFalse(k.put(a, d, 'k'), "a crate never covers a light tile");
        assertTrue(k.put(a, d, 'x'), "iron bars may");
    }

    @Test
    void noChestTouchesTwoChests() {
        Kit k = room(12, 10, 6);
        assertTrue(k.put(4, 4, 'C'));
        assertTrue(k.put(5, 4, 'C'), "a pair is a double chest");
        assertFalse(k.put(6, 4, 'C'), "a third in the row would break the pair");
        assertFalse(k.put(4, 5, 'C'), "a chest beside a chest that has a partner");
    }

    @Test
    void bedsPairFootToHead() {
        Kit k = room(14, 12, 7);
        assertTrue(k.anchor(1, 1, "b.", "BL", "BL", "b."));
        List<Kit.Placed> out = k.commit();
        for (Kit.Placed p : out) {
            if (p.entry.bottom() == FurniturePiece.BED_FOOT) {
                FurniturePiece head = k.pieceAt(p.a + p.facing.da, p.d + p.facing.dd);
                assertEquals(FurniturePiece.BED_HEAD, head, "a foot faces its head");
                assertNotEquals(0, p.group);
            }
        }
    }

    @Test
    void aChestWalledInByMarksIsReachedAtTheCostOfAnotherChest() {
        Kit k = room(12, 10, 6);
        k.legend()
            .set('M', Legend.machine(com.gregguns.icbm.complex.MachineRole.WORKBENCH));
        assertTrue(k.put(0, 0, 'C'));
        assertTrue(k.put(1, 0, 'M'));
        assertTrue(k.put(0, 1, 'C'));
        assertTrue(k.put(1, 1, 'M'));
        k.reachable();
        assertNotNull(k.at(0, 0), "the corner chest stays");
        assertNull(k.at(0, 1), "the chest in its way gave way");
    }

    @Test
    void anOpenPocketIsNeverLeftWalledIn() {
        Kit k = room(12, 10, 6);
        assertTrue(k.anchor(0, 0, ".k", "kk"));
        k.reachable();
        assertTrue(k.at(1, 0) == null || k.at(0, 1) == null, "a crate gave way to the corner cell");
    }

    @Test
    void mirroredModulesSwapSides() {
        Legend l = Legend.defaults();
        Module m = Module.of(l, "kC.");
        Module mm = m.mirror();
        assertEquals(".Ck", mm.rows()[0]);
    }

    @Test
    void chestsFaceTheirAisleAndComeInSlotOrder() {
        Kit k = room(28, 20, 7);
        new Bunks().lay(k);
        k.reachable();
        List<Kit.Placed> out = k.commit();
        boolean chests = false;
        for (Kit.Placed p : out) {
            if (p.entry.chest()) chests = true;
            else assertFalse(chests, "chests come last");
            if (!p.entry.chest()) continue;
            int fa = p.a + p.facing.da, fd = p.d + p.facing.dd;
            assertTrue(k.in(fa, fd) && k.at(fa, fd) == null, "a chest opens on an empty cell at " + p.a + "," + p.d);
        }
    }

    @Test
    void bunksMatchTheReviewedMap() {
        Kit k = room(28, 20, 7);
        new Bunks().lay(k);
        k.reachable();
        k.shadowFill();
        String map = k.render();
        System.out.println(map);
        Kit.Fill f = k.fill();
        System.out.println("fill " + f.fill + " center " + f.center + " chests " + f.chests);
        assertTrue(f.chests >= 30, "about 34 chests at 28 x 20: " + f.chests);
        int beds = 0;
        for (Kit.Placed p : k.commit()) if (p.entry.bottom() == FurniturePiece.BED_FOOT) beds++;
        assertTrue(beds >= 20, "beds: " + beds);
        assertEquals(0, k.fill().machines);
    }
}
