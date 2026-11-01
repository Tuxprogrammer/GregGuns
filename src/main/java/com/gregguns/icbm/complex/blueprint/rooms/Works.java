package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Module;

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

    /**
     * Fills every band between d0 and d1 in turn with the next of {@code turns}. Each turn lists its modules tallest
     * and widest first. Each side of the spine is packed outward from it: at each place the first module that fits
     * there (centered in the band) is stamped and the next place is one cell past it, so every module touches an aisle
     * column; where none fits the place moves one cell on. A short or narrow band, or a band with a piece already in
     * it, so takes smaller modules around it instead of staying empty. The low side takes the mirrored modules. A
     * narrow room (under 20 cells across) starts right beside the spine, so its middle is not the empty part. A band
     * that takes nothing keeps the turn for the next band.
     *
     * @return how many modules were placed
     */
    static int fillBands(Kit k, int d0, int d1, Module[]... turns) {
        return fillBands(k, d0, d1, k.lateral() < 20 ? 0 : 1, turns);
    }

    /** {@link #fillBands(Kit, int, int, Module[][])} with the first modules {@code gap} cells clear of the spine. */
    static int fillBands(Kit k, int d0, int d1, int gap, Module[]... turns) {
        int i = 0, n = 0;
        for (int[] b : k.bands(d0, d1)) {
            int got = pack(k, b[0], b[1], gap, turns[i % turns.length]);
            if (got > 0) i++;
            n += got;
        }
        return n;
    }

    /**
     * Packs rows b0 to b1 from the spine out to one cell short of each side wall (see {@link #fillBands}).
     *
     * @return how many modules were placed
     */
    static int pack(Kit k, int b0, int b1, int gap, Module[] options) {
        return pack(k, b0, b1, gap, 1, options);
    }

    /**
     * {@link #pack(Kit, int, int, int, Module[])} keeping {@code margin} columns clear along each side wall (2 where a
     * wall column holds pieces that need an aisle beside them).
     */
    static int pack(Kit k, int b0, int b1, int gap, int margin, Module[] options) {
        int h = b1 - b0 + 1, n = 0, L = k.lateral(), cut = cutOff(k);
        for (int a = k.spineHi() + 1 + gap; a <= L - 1 - margin;) {
            int w = place(k, b0, h, a, margin, cut, options, false);
            n += w > 0 ? 1 : 0;
            a += w > 0 ? w + 1 : 1;
        }
        for (int a = k.spineLo() - 1 - gap; a >= margin;) {
            int w = place(k, b0, h, a, margin, cut, options, true);
            n += w > 0 ? 1 : 0;
            a -= w > 0 ? w + 1 : 1;
        }
        return n;
    }

    /**
     * Stamps the first option that fits with its spine-side column on {@code a} (the high side's left edge, or the low
     * side's right edge, mirrored), and that leaves no more than {@code cut} chests, machines and guards cut off from
     * the door (see {@link #cutOff}).
     *
     * @return the stamped module's width, or 0 for none
     */
    private static int place(Kit k, int b0, int h, int a, int margin, int cut, Module[] options, boolean low) {
        for (Module m : options) {
            if (m.height > h) continue;
            int lo = low ? a - m.width + 1 : a;
            if (lo < margin || lo + m.width - 1 > k.lateral() - 1 - margin) continue;
            if (k.anchorIf(low ? m.mirror() : m, lo, b0 + (h - m.height) / 2, kit -> cutOff(kit) <= cut))
                return m.width;
        }
        return 0;
    }

    /**
     * @return how many chests, machines and guards no walk from the door reaches: a walk over open floor and walked
     *         pieces, never through the NASA Workbench's strict core, though a mark beside the core (the workbench)
     *         counts as reached (D252)
     */
    static int cutOff(Kit k) {
        int L = k.lateral(), D = k.depth();
        boolean[] seen = new boolean[L * D];
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<int[]>();
        for (int a = k.spineLo(); a <= k.spineHi(); a++) {
            if (!strictWalk(k, a, D - 1)) continue;
            seen[a * D + D - 1] = true;
            q.add(new int[] { a, D - 1 });
        }
        int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] n : nb) {
                int a = c[0] + n[0], d = c[1] + n[1];
                if (a < 0 || d < 0 || a >= L || d >= D || seen[a * D + d] || !strictWalk(k, a, d)) continue;
                seen[a * D + d] = true;
                q.add(new int[] { a, d });
            }
        }
        int cut = 0;
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                Legend.Entry e = k.at(a, d);
                if (e == null || e.kind != Legend.Entry.Kind.MARK) continue;
                boolean reached = false;
                for (int[] n : nb) {
                    int ca = a + n[0], cd = d + n[1];
                    if (ca < 0 || cd < 0 || ca >= L || cd >= D) continue;
                    reached |= seen[ca * D + cd] || k.core(ca, cd);
                }
                if (!reached) cut++;
            }
        }
        return cut;
    }

    /** @return whether a player walks over the cell, the NASA Workbench's strict core aside */
    private static boolean strictWalk(Kit k, int a, int d) {
        return !k.core(a, d) && walkable(k, a, d);
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
