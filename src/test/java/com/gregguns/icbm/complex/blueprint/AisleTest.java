package com.gregguns.icbm.complex.blueprint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.RoomKind;

/**
 * D321 (owner play session 3, S3-8): a room's aisle is as wide as its doorway with the jamb either side, in every
 * blueprint. The pump room's walkway was 4 wide under a 5-wide opening. For every room kind, at a narrow, a drawn and a
 * wide size, with the doorway centered and pushed to either side: the kit's aisle is the opening's width plus two, and
 * wherever the door's own spine is clear the whole aisle is clear too, so no row of the way in narrows to the bare
 * doorway.
 */
class AisleTest {

    /** Interior sizes: narrow, drawn, wide, shallow. */
    private static final int[][] SIZES = { { 13, 18 }, { 28, 20 }, { 40, 31 }, { 23, 11 } };

    /** The doorway width the grower cuts for a kind: 2 for the low annexes, 3 else (5 for a bay off a hall). */
    private static int doorWidth(String kind) {
        RoomKind k = RoomKind.valueOf(kind);
        return k.small() ? 2 : 3;
    }

    /** A furnisher-like floor with the doorway at lateral {@code lo}, its 2-deep zone, and the lamp grid. */
    private static Kit floor(String kind, int L, int D, int lo, int w) {
        RoomBlueprint bp = RoomBlueprints.all()
            .get(kind);
        byte[] base = new byte[L * D];
        int gap = bp.lampGap();
        for (int a : KitTest.grid(L, gap)) for (int d : KitTest.grid(D, gap)) base[a * D + d] = Kit.TILE;
        for (int a = lo - 1; a <= lo + w; a++) {
            for (int d = D - 2; d < D; d++) if (a >= 0 && a < L) base[a * D + d] = Kit.DOORWAY;
        }
        List<Opening> o = new ArrayList<Opening>();
        o.add(new Opening(Opening.Wall.DOOR, lo, lo + w - 1, true));
        return new Kit(L, D, base, o, 0xA15EL + L * 131 + D * 7 + lo, 2);
    }

    @Test
    void everyRoomsAisleIsItsDoorwayAndBothJambs() {
        int checked = 0;
        for (Map.Entry<String, RoomBlueprint> e : RoomBlueprints.all()
            .entrySet()) {
            String kind = e.getKey();
            if ("ASSEMBLY_BAY".equals(kind)) continue; // the hub has portals and lanes, no doorway
            int w = doorWidth(kind);
            for (int[] s : SIZES) {
                int L = s[0], D = s[1];
                if (RoomKind.valueOf(kind)
                    .small() && (L > 14 || D > 14)) {
                    L = Math.min(L, 11);
                    D = Math.min(D, 11);
                }
                for (int lo : new int[] { (L - w) / 2, 2, L - w - 2 }) {
                    Kit k = floor(kind, L, D, lo, w);
                    e.getValue()
                        .lay(k);
                    k.reachable();
                    k.shadowFill();
                    String where = kind + " " + L + " x " + D + " door at " + lo;
                    assertEquals(lo - 1, k.aisleLo(), where);
                    assertEquals(lo + w, k.aisleHi(), where);
                    assertEquals(
                        w + 2,
                        k.aisleHi() - k.aisleLo() + 1,
                        "the aisle is the opening and its jambs: " + where);
                    checked += clearWhereTheSpineIs(k, where);
                }
            }
        }
        assertTrue(checked > 1000, "rows checked: " + checked);
    }

    /**
     * From the door wall in, row by row while the spine's cells are empty: every aisle cell is walkable too.
     *
     * @return the rows checked
     */
    private static int clearWhereTheSpineIs(Kit k, String where) {
        int rows = 0;
        for (int d = k.depth() - 1; d >= 0; d--) {
            boolean spine = true;
            // The spine's own cells reserved and empty: the spine's stop (a range's firing line, an assembly's jig)
            // or a piece laid before the spine (a kitchen line, the NASA Workbench's ring at the back of a narrow
            // workshop) ends the way in.
            for (int a = k.spineLo(); a <= k.spineHi(); a++) {
                spine &= k.reserved(a, d) && k.at(a, d) == null && !k.kept(a, d);
            }
            if (!spine) break;
            for (int a = k.aisleLo(); a <= k.aisleHi(); a++) {
                assertTrue(walk(k, a, d), "the aisle narrows at " + a + "," + d + " in " + where + "\n" + k.render());
            }
            rows++;
        }
        return rows;
    }

    private static boolean walk(Kit k, int a, int d) {
        Legend.Entry e = k.at(a, d);
        return e == null || e.walkable();
    }

    @Test
    void anOpeningInTheBackWallGetsAPathAsWide() {
        int L = 28, D = 20;
        byte[] base = new byte[L * D];
        for (int a : KitTest.grid(L, 6)) for (int d : KitTest.grid(D, 6)) base[a * D + d] = Kit.TILE;
        List<Opening> o = new ArrayList<Opening>();
        o.add(new Opening(Opening.Wall.DOOR, 12, 14, true));
        o.add(new Opening(Opening.Wall.BACK, 4, 6, false));
        Kit k = new Kit(L, D, base, o, 7L, 1);
        k.spine();
        for (int a = 3; a <= 7; a++) assertTrue(k.reserved(a, 0), "the back door's path covers its jambs at " + a);
        assertFalse(k.reserved(2, 0));
        assertFalse(k.reserved(8, 0));
    }
}
