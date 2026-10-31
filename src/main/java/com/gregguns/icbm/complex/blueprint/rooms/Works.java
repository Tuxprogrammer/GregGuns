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
     * then in front of the back row, every other cell. A spot is taken only when the walk from the door still reaches
     * every open cell and every chest, machine and guard with the spot closed (the kit's reachability pass cannot open
     * a way through chests). A room too small or too full to take them stops short.
     */
    static void topUp(Kit k, int min) {
        int L = k.lateral(), D = k.depth();
        int have = chests(k);
        for (int pass = 0; pass < 2 && have < min; pass++) {
            for (int a = pass; a < L && have < min; a += 2) if (chestIfClear(k, a, 0)) have++;
            for (int d = 2 + pass; d < D - 3 && have < min; d += 2) {
                if (chestIfClear(k, 0, d)) have++;
                if (have < min && chestIfClear(k, L - 1, d)) have++;
            }
            for (int a = pass; a < L && have < min; a += 2) if (chestIfClear(k, a, 1)) have++;
        }
    }

    /** Puts a chest on (a, d) when closing that cell cuts nothing off from the door. */
    static boolean chestIfClear(Kit k, int a, int d) {
        if (!k.free(a, d)) return false;
        boolean[] before = flood(k, -1, -1), after = flood(k, a, d);
        int L = k.lateral(), D = k.depth();
        for (int ca = 0; ca < L; ca++) {
            for (int cd = 0; cd < D; cd++) {
                int i = ca * D + cd;
                if ((ca != a || cd != d) && before[i] && !after[i]) return false;
                Legend.Entry e = k.at(ca, cd);
                boolean mark = e != null && e.kind == Legend.Entry.Kind.MARK || ca == a && cd == d;
                if (mark && touches(k, before, ca, cd) && !touches(k, after, ca, cd)) return false;
            }
        }
        return k.put(a, d, 'C');
    }

    /** @return whether a player walks over the cell as it stands */
    static boolean walkable(Kit k, int a, int d) {
        Legend.Entry e = k.at(a, d);
        if (e == null) return true;
        if (e.kind == Legend.Entry.Kind.KEEP || e.kind == Legend.Entry.Kind.KEEP_STRICT) return true;
        if (e.kind != Legend.Entry.Kind.PIECE) return false;
        FurniturePiece b = e.bottom();
        if (b == null) return e.floorPiece() == null || e.floorPiece()
            .walkable();
        return b.walkable();
    }

    /** @return the cells a walk from the door row's doorway cells reaches, with (sa, sd) closed (-1 for none) */
    static boolean[] flood(Kit k, int sa, int sd) {
        int L = k.lateral(), D = k.depth();
        boolean[] seen = new boolean[L * D];
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<int[]>();
        for (int a = k.spineLo(); a <= k.spineHi(); a++) {
            if (!walkable(k, a, D - 1) || a == sa && D - 1 == sd) continue;
            seen[a * D + D - 1] = true;
            q.add(new int[] { a, D - 1 });
        }
        int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] n : nb) {
                int a = c[0] + n[0], d = c[1] + n[1];
                if (a < 0 || d < 0 || a >= L || d >= D || seen[a * D + d] || a == sa && d == sd || !walkable(k, a, d))
                    continue;
                seen[a * D + d] = true;
                q.add(new int[] { a, d });
            }
        }
        return seen;
    }

    private static boolean touches(Kit k, boolean[] seen, int a, int d) {
        int L = k.lateral(), D = k.depth();
        int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] n : nb) {
            int ca = a + n[0], cd = d + n[1];
            if (ca >= 0 && cd >= 0 && ca < L && cd < D && seen[ca * D + cd]) return true;
        }
        return false;
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
