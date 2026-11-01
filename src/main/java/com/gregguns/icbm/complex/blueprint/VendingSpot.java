package com.gregguns.icbm.complex.blueprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.RoomKind;

/**
 * The Vending Machine easter egg (D355): one of the pack's Vending Machines per silo complex, standing against a wall
 * of a dining hall and facing into it. The machine is a 2 x 3 x 1 GT multiblock, so it takes two floor cells side by
 * side along one wall, three layers high.
 *
 * <p>
 * {@link #halls} picks the room: the dining halls (mess galley, cafeteria) in an order hashed from the plan seed, then
 * the recreation lounges the same way; the first is the complex's hall, and the furnisher offers the machine to the
 * next candidate it furnishes only when that hall has no spot. {@link #place} picks the spot inside the room, after the
 * blueprint and the reachability pass: two free cells along a wall whose two cells in front are bare floor, with no
 * cell of the machine or of its front beside a counter, and the stamp kept only when every cell a walk from the door
 * reached still is reached and every chest, machine and guard still touches the walk. So the door, the aisle, the
 * kitchen and serving counters and every path stay as the blueprint left them. Pure: no Minecraft types, no random
 * draws.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Vending Machine); D355
public final class VendingSpot {

    /** The character the room maps draw the machine with. */
    public static final char GLYPH = 'U';

    /** How many layers the machine stands, from y 1 up. */
    public static final int HEIGHT = 3;

    /** The least height of a hall's box, floor and ceiling included, that fits the machine under its ceiling. */
    public static final int MIN_BOX_HEIGHT = HEIGHT + 2;

    /** Salts: the hall order and the spot pick. */
    private static final long S_HALL = 0x7E4DA11L;
    private static final int S_SPOT = 0x7E4D;

    private VendingSpot() {}

    /** @return whether a room kind is a dining hall (the machine's first choice) */
    public static boolean dining(RoomKind k) {
        return k == RoomKind.MESS_GALLEY || k == RoomKind.CAFETERIA;
    }

    /**
     * @param r a room
     * @return whether the room may hold the machine: a dining hall or a recreation lounge, tall enough for it
     */
    public static boolean candidate(Room r) {
        return r.kind() != null && (dining(r.kind()) || r.kind() == RoomKind.REC_LOUNGE)
            && r.bounds()
                .sizeY() >= MIN_BOX_HEIGHT;
    }

    /**
     * The rooms that may hold the machine, in the order they are offered: every dining hall, then every recreation
     * lounge, each group in an order hashed from the plan seed and the room id. The first is the complex's hall.
     *
     * @param rooms    the plan's rooms
     * @param planSeed the plan's seed
     * @return the candidates, best first; empty when the complex has no such room
     */
    public static List<Room> halls(List<Room> rooms, final long planSeed) {
        List<Room> dining = new ArrayList<Room>(), lounges = new ArrayList<Room>();
        for (Room r : rooms) {
            if (!candidate(r)) continue;
            (dining(r.kind()) ? dining : lounges).add(r);
        }
        Comparator<Room> byHash = Comparator.comparingLong((Room r) -> mix(planSeed, r.id()))
            .thenComparingInt(Room::id);
        dining.sort(byHash);
        lounges.sort(byHash);
        dining.addAll(lounges);
        return dining;
    }

    /** @return a well-mixed hash of the plan seed and a room id */
    static long mix(long seed, int id) {
        long h = seed ^ S_HALL * 0x9E3779B97F4A7C15L ^ id * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /**
     * One place the machine may stand: its two cells, from the back-left cell of its stamp, and the way it faces.
     */
    public static final class Spot {

        /** The stamp's back-left cell. */
        public final int a, d;
        /** True for a stamp along a side wall (one column, two rows); false along the back or the door wall. */
        public final boolean alongDepth;
        /** The way the machine's front faces: into the room, away from its wall. */
        public final Dir facing;

        Spot(int a, int d, boolean alongDepth, Dir facing) {
            this.a = a;
            this.d = d;
            this.alongDepth = alongDepth;
            this.facing = facing;
        }

        /** @return the machine's two cells, as {a, d} */
        public int[][] cells() {
            return alongDepth ? new int[][] { { a, d }, { a, d + 1 } } : new int[][] { { a, d }, { a + 1, d } };
        }

        /** @return the two cells in front of the machine, where a player stands to use it, as {a, d} */
        public int[][] front() {
            int[][] c = cells();
            return new int[][] { { c[0][0] + facing.da, c[0][1] + facing.dd },
                { c[1][0] + facing.da, c[1][1] + facing.dd } };
        }

        @Override
        public String toString() {
            return a + "," + d + (alongDepth ? " down " : " along ") + facing;
        }
    }

    /** @return the legend entry of the machine facing {@code f}: three layers, both columns one group */
    static Legend.Entry entry(Dir f) {
        return Legend
            .piece(FurniturePiece.VENDING_MACHINE, FurniturePiece.VENDING_MACHINE, FurniturePiece.VENDING_MACHINE)
            .variant(0)
            .grouped()
            .facing(f);
    }

    /**
     * Every stamp along a wall, facing in: the back wall, the door wall, then the left and the right walls.
     *
     * @return the stamps, before any check
     */
    static List<Spot> wallSpots(int L, int D) {
        List<Spot> out = new ArrayList<Spot>();
        if (L < 3 || D < 3) return out;
        for (int a = 0; a + 1 < L; a++) out.add(new Spot(a, 0, false, Dir.TO_DOOR));
        for (int a = 0; a + 1 < L; a++) out.add(new Spot(a, D - 1, false, Dir.TO_BACK));
        for (int d = 0; d + 1 < D; d++) out.add(new Spot(0, d, true, Dir.PLUS_A));
        for (int d = 0; d + 1 < D; d++) out.add(new Spot(L - 1, d, true, Dir.MINUS_A));
        return out;
    }

    /**
     * The spots of a laid-out room where the machine may stand (see the class comment). Tries each wall stamp and
     * takes it back; the room is left as it was.
     *
     * @param k a room's kit after its blueprint and the reachability pass
     * @return the valid spots, in {@link #wallSpots} order
     */
    public static List<Spot> spots(Kit k) {
        final boolean[] before = k.walked();
        List<Spot> ok = new ArrayList<Spot>();
        for (Spot s : wallSpots(k.lateral(), k.depth())) {
            if (!precheck(k, s)) continue;
            final boolean[] fits = { false };
            k.anchorIf(module(k, s), s.a, s.d, kit -> {
                fits[0] = keepsOpen(kit, s, before);
                return false; // a probe: always taken back
            });
            if (fits[0]) ok.add(s);
        }
        return ok;
    }

    /**
     * Stands the machine on one of the room's valid spots, picked by the room's hash.
     *
     * @param k a room's kit after its blueprint and the reachability pass
     * @return the spot taken, or null when the room has none (nothing changed)
     */
    public static Spot place(Kit k) {
        List<Spot> ok = spots(k);
        if (ok.isEmpty()) return null;
        Spot s = ok.get(k.pick(S_SPOT, 0, 0, ok.size()));
        final boolean[] before = k.walked();
        return k.anchorIf(module(k, s), s.a, s.d, kit -> keepsOpen(kit, s, before)) ? s : null;
    }

    /** @return the one- or two-cell module of a spot, its character set on the room's legend */
    private static Module module(Kit k, Spot s) {
        k.legend()
            .set(GLYPH, entry(s.facing));
        String g = String.valueOf(GLYPH);
        return s.alongDepth ? k.module(g, g) : k.module(g + g);
    }

    /** The checks that need no stamp: free cells, bare floor in front, no counter beside any of them. */
    private static boolean precheck(Kit k, Spot s) {
        for (int[] c : s.cells()) if (!k.free(c[0], c[1])) return false;
        for (int[] c : s.front()) {
            if (!k.in(c[0], c[1]) || k.at(c[0], c[1]) != null || k.base(c[0], c[1]) == Kit.HOLE) return false;
        }
        for (int[][] cells : new int[][][] { s.cells(), s.front() }) {
            for (int[] c : cells) {
                for (Dir x : Dir.REAL) {
                    if (k.pieceAt(c[0] + x.da, c[1] + x.dd) == FurniturePiece.COUNTER) return false;
                }
            }
        }
        return true;
    }

    /**
     * @return whether the room with the machine stamped on {@code s} keeps every walked cell walked (the machine's own
     *         two aside), has both front cells walked, and leaves every chest, machine and guard that stood beside a
     *         walked cell beside one still
     */
    static boolean keepsOpen(Kit k, Spot s, boolean[] before) {
        int L = k.lateral(), D = k.depth();
        boolean[] now = k.walked();
        int[][] mine = s.cells();
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                int i = a * D + d;
                boolean own = a == mine[0][0] && d == mine[0][1] || a == mine[1][0] && d == mine[1][1];
                if (before[i] && !now[i] && !own) return false;
            }
        }
        for (int[] c : s.front()) if (!now[c[0] * D + c[1]]) return false;
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                Legend.Entry e = k.at(a, d);
                if (e == null || e.kind != Legend.Entry.Kind.MARK) continue;
                if (touches(k, before, a, d) && !touches(k, now, a, d)) return false;
            }
        }
        return true;
    }

    /** @return whether a cell has a walked neighbor */
    private static boolean touches(Kit k, boolean[] walked, int a, int d) {
        for (Dir x : Dir.REAL) {
            int na = a + x.da, nd = d + x.dd;
            if (k.in(na, nd) && walked[na * k.depth() + nd]) return true;
        }
        return false;
    }
}
