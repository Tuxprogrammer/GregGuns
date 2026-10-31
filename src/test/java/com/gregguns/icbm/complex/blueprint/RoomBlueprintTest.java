package com.gregguns.icbm.complex.blueprint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.MarkKind;

/**
 * D305, D306: every C1 blueprint at its small, drawn and large size, on the furnisher's kind of floor: stamps never
 * cover a doorway, a hole or (unless thin) a light tile; every chest, machine, guard and open cell is reached from the
 * door; chairs face something to sit at; beds are whole; no chest touches two chests; and each room holds at least
 * the chest spots the loot budget needs (F-loot-tables §1, C-blueprints' tally). Prints each room's map and fill.
 */
class RoomBlueprintTest {

    /** A size to test: lateral and depth interior cells, and the lamp gap the furnisher uses (6, 7 or 4). */
    private static int[] size(int L, int D) {
        return new int[] { L, D };
    }

    /** Small, drawn and large interiors per kind (C-rooms §3), and the chest spots asked at the drawn size. */
    private static final Map<String, int[][]> SIZES = new LinkedHashMap<String, int[][]>();
    private static final Map<String, Integer> MIN_CHESTS = new HashMap<String, Integer>();

    static {
        add("BUNKS", 30, size(18, 15), size(28, 20), size(40, 31));
        add("INFIRMARY", 20, size(16, 14), size(28, 20), size(34, 28));
        add("MESS_GALLEY", 13, size(18, 15), size(28, 20), size(37, 31));
        add("CAFETERIA", 16, size(23, 18), size(36, 28), size(46, 37));
        add("REC_LOUNGE", 11, size(18, 15), size(28, 20), size(37, 31));
        add("SHOWERS_LAUNDRY", 4, size(10, 8), size(12, 10), size(16, 14));
        add("GUARD_POST", 3, size(6, 6), size(8, 8), size(11, 11));
        add("DECON_AIRLOCK", 4, size(8, 8), size(10, 10), size(14, 14));
        add("BRIG", 6, size(18, 13), size(28, 20), size(37, 28));
        add("SHOOTING_RANGE", 15, size(10, 20), size(16, 34), size(25, 44));
        add("LAUNCH_CONTROL", 12, size(22, 15), size(28, 20), size(43, 31));
        add("RADAR_TRACKING", 12, size(18, 15), size(28, 20), size(37, 31));
        add("COMMS", 15, size(16, 13), size(28, 20), size(34, 28));
        add("COMMANDER_OFFICE", 6, size(15, 13), size(20, 18), size(31, 28));
        add("SERVER_ROOM", 10, size(18, 15), size(28, 20), size(37, 31));
        add("ASSEMBLY_BAY", 9, size(49, 38), size(49, 38), size(49, 38));
    }

    private static void add(String kind, int chests, int[]... sizes) {
        SIZES.put(kind, sizes);
        MIN_CHESTS.put(kind, chests);
    }

    /** The lamp gap of each kind, as the furnisher sets it. */
    private static int gap(String kind) {
        return RoomBlueprints.all()
            .get(kind)
            .lampGap();
    }

    /** True for the low annexes, lit from the ceiling only (no floor tiles). */
    private static boolean low(String kind) {
        return "GUARD_POST".equals(kind) || "DECON_AIRLOCK".equals(kind);
    }

    /** A furnisher-like floor: the doorway in the middle of the door wall (2 wide for a low annex) and the tiles. */
    static Kit floor(String kind, int L, int D) {
        byte[] base = new byte[L * D];
        int w = low(kind) ? 2 : 3;
        int lo = (L - w) / 2, hi = lo + w - 1;
        if (!low(kind)) {
            for (int a : KitTest.grid(L, gap(kind)))
                for (int d : KitTest.grid(D, gap(kind))) base[a * D + d] = Kit.TILE;
        }
        List<Opening> o = new ArrayList<Opening>();
        if ("ASSEMBLY_BAY".equals(kind)) {
            // The hub: a portal of a major hall on each side.
            o.add(new Opening(Opening.Wall.DOOR, L / 2 - 8, L / 2 + 8, false));
            o.add(new Opening(Opening.Wall.LEFT, D / 2 - 8, D / 2 + 8, false));
            o.add(new Opening(Opening.Wall.RIGHT, D / 2 - 8, D / 2 + 8, false));
            o.add(new Opening(Opening.Wall.BACK, 10, 12, false));
            for (Opening op : o) zone(base, L, D, op);
        } else {
            Opening door = new Opening(Opening.Wall.DOOR, lo, hi, true);
            o.add(door);
            zone(base, L, D, door);
            if (D >= 14) {
                Opening side = new Opening(Opening.Wall.RIGHT, D / 2 - 1, D / 2 + 1, false);
                o.add(side);
                zone(base, L, D, side);
            }
        }
        return new Kit(L, D, base, o, 0x5EEDL + L * 31 + D, 1);
    }

    /** The doorway zone: the opening's cells and one either side, two deep. */
    private static void zone(byte[] base, int L, int D, Opening o) {
        for (int t = o.lo - 1; t <= o.hi + 1; t++) {
            for (int k = 0; k < 2; k++) {
                int a, d;
                switch (o.wall) {
                    case DOOR:
                        a = t;
                        d = D - 1 - k;
                        break;
                    case BACK:
                        a = t;
                        d = k;
                        break;
                    case LEFT:
                        a = k;
                        d = t;
                        break;
                    default:
                        a = L - 1 - k;
                        d = t;
                        break;
                }
                if (a >= 0 && d >= 0 && a < L && d < D) base[a * D + d] = Kit.DOORWAY;
            }
        }
    }

    /** Lays a kind's blueprint the way the furnisher does. */
    static Kit laid(String kind, int L, int D) {
        Kit k = floor(kind, L, D);
        RoomBlueprints.all()
            .get(kind)
            .lay(k);
        k.reachable();
        k.shadowFill();
        return k;
    }

    @Test
    void everyC1KindHasItsBlueprint() {
        for (String kind : SIZES.keySet()) assertNotNull(
            RoomBlueprints.all()
                .get(kind),
            kind);
    }

    @Test
    void stampsKeepEveryRule() {
        for (Map.Entry<String, int[][]> e : SIZES.entrySet()) {
            for (int[] s : e.getValue()) check(e.getKey(), s[0], s[1]);
        }
    }

    private static void check(String kind, int L, int D) {
        String where = kind + " " + L + " x " + D;
        Kit floor = floor(kind, L, D);
        Kit k = laid(kind, L, D);
        List<Kit.Placed> out = k.commit();
        boolean[] walk = new boolean[L * D];
        for (int a = 0; a < L; a++) for (int d = 0; d < D; d++) {
            Legend.Entry e = k.at(a, d);
            walk[a * D + d] = e == null || e.walkable();
        }
        boolean[] seen = flood(k, walk, L, D);
        int beds = 0;
        for (Kit.Placed p : out) {
            String at = where + " at " + p.a + "," + p.d + " " + p.entry;
            assertNotEquals(Dir.AUTO, p.facing, at);
            // Never a doorway or a hole; a light tile only under a thin piece.
            assertNotEquals(Kit.DOORWAY, floor.base(p.a, p.d), "on a doorway: " + at);
            assertNotEquals(Kit.HOLE, floor.base(p.a, p.d), "over a hole: " + at);
            boolean wasTile = floor.tile(p.a, p.d);
            if (wasTile) assertTrue(p.entry.overTile(), "a solid piece on a light tile: " + at);
            if (p.entry.kind == Legend.Entry.Kind.MARK) {
                assertTrue(touches(seen, p.a, p.d, L, D), "unreached: " + at);
                if (p.entry.chest()) assertTrue(chestsAround(k, p.a, p.d) <= 1, "a chest touching two: " + at);
            }
            FurniturePiece b = p.entry.bottom();
            if (b == FurniturePiece.CHAIR) {
                FurniturePiece front = k.pieceAt(p.a + p.facing.da, p.d + p.facing.dd);
                assertTrue(front != null && front.seatsAt(), "a chair facing nothing: " + at);
            }
            if (b == FurniturePiece.BED_FOOT) {
                beds++;
                int ha = p.a + p.facing.da, hd = p.d + p.facing.dd;
                assertEquals(FurniturePiece.BED_HEAD, k.pieceAt(ha, hd), "a bed without its head: " + at);
                boolean found = false;
                for (Kit.Placed q : out) {
                    if (q.a == ha && q.d == hd) {
                        assertEquals(p.facing, q.facing, "the halves' metas differ: " + at);
                        assertEquals(p.group, q.group, at);
                        found = true;
                    }
                }
                assertTrue(found, at);
            }
        }
        for (int a = 0; a < L; a++) for (int d = 0; d < D; d++) {
            if (walk[a * D + d]) assertTrue(seen[a * D + d], "a walled-in floor cell: " + where + " at " + a + "," + d);
        }
        // Every walked cell reads light 8 from the floor tiles (rooms lit from the floor).
        if (!low(kind)) {
            int[] light = k.light();
            for (int i = 0; i < L * D; i++) {
                if (k.lit(i))
                    assertTrue(light[i] >= Kit.MIN_LIGHT, "dark cell " + where + " at " + i / D + "," + i % D);
            }
        }
        if ("BUNKS".equals(kind) || "INFIRMARY".equals(kind)) assertTrue(beds >= 4, where + ": beds " + beds);
    }

    private static boolean[] flood(Kit k, boolean[] walk, int L, int D) {
        boolean[] seen = new boolean[L * D];
        ArrayDeque<int[]> q = new ArrayDeque<int[]>();
        Opening first = null;
        for (Opening o : k.openings()) if (o.primary) first = o;
        if (first == null) first = k.openings()
            .get(0);
        for (int[] c : first.inside(L, D)) {
            if (walk[c[0] * D + c[1]]) {
                seen[c[0] * D + c[1]] = true;
                q.add(c);
            }
        }
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (Dir x : Dir.REAL) {
                int a = c[0] + x.da, d = c[1] + x.dd;
                if (a < 0 || d < 0 || a >= L || d >= D || seen[a * D + d] || !walk[a * D + d]) continue;
                seen[a * D + d] = true;
                q.add(new int[] { a, d });
            }
        }
        return seen;
    }

    private static boolean touches(boolean[] seen, int a, int d, int L, int D) {
        for (Dir x : Dir.REAL) {
            int ca = a + x.da, cd = d + x.dd;
            if (ca >= 0 && cd >= 0 && ca < L && cd < D && seen[ca * D + cd]) return true;
        }
        return false;
    }

    private static int chestsAround(Kit k, int a, int d) {
        int n = 0;
        for (Dir x : Dir.REAL) {
            Legend.Entry e = k.at(a + x.da, d + x.dd);
            if (e != null && e.chest()) n++;
        }
        return n;
    }

    @Test
    void eachRoomHoldsTheChestSpotsTheBudgetNeeds() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, int[][]> e : SIZES.entrySet()) {
            String kind = e.getKey();
            for (int i = 0; i < e.getValue().length; i++) {
                int[] s = e.getValue()[i];
                Kit k = laid(kind, s[0], s[1]);
                Kit.Fill f = k.fill();
                sb.append(
                    String.format(
                        "%-17s %2d x %2d  fill %3.0f%%  center %3.0f%%  chests %3d  machines %2d%n",
                        kind,
                        s[0],
                        s[1],
                        100 * f.fill,
                        100 * f.center,
                        f.chests,
                        f.machines));
                if (i == 1) {
                    assertTrue(
                        f.chests >= MIN_CHESTS.get(kind),
                        kind + " at " + s[0] + " x " + s[1] + ": " + f.chests + " chest spots");
                    // C-rooms §6: the middle is never the empty part, and no room is a bare bay.
                    if (s[0] >= 16 && s[1] >= 12 && !"ASSEMBLY_BAY".equals(kind)) { // the hub keeps its lanes
                        assertTrue(f.fill >= 0.15 && f.fill <= 0.65, kind + " fill " + f.fill);
                        assertTrue(f.center >= 0.12, kind + " center " + f.center);
                    }
                }
            }
        }
        System.out.print(sb);
    }

    @Test
    void chestsHaveAFrontAndTheSafeComesFirst() {
        Kit k = laid("COMMANDER_OFFICE", 20, 18);
        List<Kit.Placed> out = k.commit();
        Kit.Placed firstChest = null;
        for (Kit.Placed p : out) {
            if (p.entry.chest() && firstChest == null) firstChest = p;
            if (p.entry.chest()) {
                int fa = p.a + p.facing.da, fd = p.d + p.facing.dd;
                assertTrue(k.in(fa, fd) && k.at(fa, fd) == null, "a chest opening on a piece at " + p.a + "," + p.d);
            }
        }
        assertNotNull(firstChest);
        assertTrue(firstChest.entry.primary, "the safe is the first slot");
        assertEquals("safe", firstChest.key);
    }

    /** Prints every C1 room at its drawn size, for the review. */
    @Test
    void printTheMaps() {
        for (Map.Entry<String, int[][]> e : SIZES.entrySet()) {
            int[] s = e.getValue()[1];
            System.out.println("== " + e.getKey() + " " + s[0] + " x " + s[1]);
            System.out.print(laid(e.getKey(), s[0], s[1]).render());
        }
        assertNotNull(MarkKind.LOOT_CHEST);
    }
}
