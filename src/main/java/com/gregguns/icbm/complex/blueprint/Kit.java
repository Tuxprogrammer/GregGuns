package com.gregguns.icbm.complex.blueprint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.MarkKind;

/**
 * The room blueprint kit (D305): one room's floor in its own frame (lateral {@code a} along the door wall, depth
 * {@code d} from the back wall at 0 to the door wall row at {@code depth - 1}), and the operations a
 * {@link RoomBlueprint} stamps it with. Pure: no Minecraft types, no random draws (variety comes from
 * {@link #hash(int, int, int)} of the plan seed and the cell), so a blueprint never changes the rest of the plan.
 *
 * <p>
 * Stamping rules:
 * <ol>
 * <li>A stamp is all or nothing: a module that would cover a doorway zone, a floor light tile, a floor hole, a
 * reserved cell or another piece is dropped whole, so no bed loses its head and no rack its end.</li>
 * <li>Transparent, thin pieces (bars, panes, rugs, a kept ring) may stand on a light tile.</li>
 * <li>No chest touches two chests, and a chest never touches a chest that already touches one (no broken triple).</li>
 * <li>Lamp rows are aisles: {@link #bands} are the runs of rows between them, and modules fill the bands.</li>
 * <li>Modules tile outward from the door's spine on both sides, mirrored on the low-a side.</li>
 * </ol>
 * After the blueprint, the furnisher calls {@link #reachable()}, {@link #shadowFill()} and {@link #commit()}.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D305
public final class Kit {

    /** Floor states the furnisher hands in (the base under any piece). */
    public static final byte FREE = 0, DOORWAY = 1, TILE = 2, HOLE = 3;

    /** The light a floor tile gives the cell over it, and the least a walked cell needs (D231). */
    static final int TILE_LIGHT = 14, MIN_LIGHT = 8;

    private final int L, D;
    private final byte[] base;
    private final boolean[] reserved, kept, core;
    private final Legend.Entry[] entry;
    private final char[] glyph;
    private final Dir[] facing;
    private final int[] group;
    private final List<Opening> openings;
    private final Opening primary;
    private final long seed;
    private final int wood;
    private Legend legend = Legend.defaults();
    private String defaultLoot = "salvage";
    private int nextGroup = 1;
    private final List<int[]> addedTiles = new ArrayList<int[]>();

    /**
     * @param lateral  interior cells along the door wall
     * @param depth    interior cells from the back wall to the door wall
     * @param base     the floor state of each cell, index {@code a * depth + d} ({@link #FREE}, {@link #DOORWAY},
     *                 {@link #TILE}, {@link #HOLE}); copied
     * @param openings the room's walk-through openings, the primary (its doorway to its parent) among them; the hub
     *                 has none primary
     * @param seed     a seed for the room's hashes (the plan seed mixed with the room)
     * @param wood     the room's wood (0 to 5)
     */
    public Kit(int lateral, int depth, byte[] base, List<Opening> openings, long seed, int wood) {
        if (lateral < 1 || depth < 1 || base.length != lateral * depth) throw new IllegalArgumentException("bad floor");
        this.L = lateral;
        this.D = depth;
        this.base = base.clone();
        this.reserved = new boolean[L * D];
        this.kept = new boolean[L * D];
        this.core = new boolean[L * D];
        this.entry = new Legend.Entry[L * D];
        this.glyph = new char[L * D];
        this.facing = new Dir[L * D];
        this.group = new int[L * D];
        this.openings = Collections.unmodifiableList(new ArrayList<Opening>(openings));
        Opening p = null;
        for (Opening o : openings) if (o.primary) p = o;
        this.primary = p;
        this.seed = seed;
        this.wood = wood;
    }

    // --- the room ---------------------------------------------------------------------------------------------------

    /** @return interior cells along the door wall */
    public int lateral() {
        return L;
    }

    /** @return interior cells from the back wall to the door wall */
    public int depth() {
        return D;
    }

    /** @return the first lateral cell of the spine: the primary doorway's, or the middle when there is none */
    public int spineLo() {
        return primary != null && primary.wall == Opening.Wall.DOOR ? primary.lo : (L - 1) / 2;
    }

    /** @return the last lateral cell of the spine */
    public int spineHi() {
        return primary != null && primary.wall == Opening.Wall.DOOR ? primary.hi : L / 2;
    }

    /**
     * @return the first lateral cell of the aisle: the spine and one cell beyond it, under the doorway's jamb, so the
     *         clear way in is as wide as the doorway with its frame (D321)
     */
    public int aisleLo() {
        return Math.max(0, spineLo() - 1);
    }

    /** @return the last lateral cell of the aisle (see {@link #aisleLo()}) */
    public int aisleHi() {
        return Math.min(L - 1, spineHi() + 1);
    }

    /** @return the room's openings */
    public List<Opening> openings() {
        return openings;
    }

    /** @return the room's wood (0 to 5) */
    public int wood() {
        return wood;
    }

    /** @return the room's legend; a blueprint sets its own characters on it */
    public Legend legend() {
        return legend;
    }

    /** Sets the loot key of chests whose entry names none. */
    public Kit defaultLoot(String key) {
        this.defaultLoot = key;
        return this;
    }

    /** @return a module of the room's legend */
    public Module module(String... rows) {
        return Module.of(legend, rows);
    }

    /** @return true for a cell inside the room */
    public boolean in(int a, int d) {
        return a >= 0 && d >= 0 && a < L && d < D;
    }

    private int i(int a, int d) {
        return a * D + d;
    }

    /** @return true for a cell with nothing on it and nothing reserved: free floor, not a doorway, tile or hole */
    public boolean free(int a, int d) {
        if (!in(a, d)) return false;
        int i = i(a, d);
        return base[i] == FREE && entry[i] == null && !reserved[i] && !kept[i];
    }

    /** @return true for a strictly kept cell (the NASA Workbench's core, D252), which no walk crosses */
    public boolean core(int a, int d) {
        return in(a, d) && core[i(a, d)];
    }

    /** @return true for a kept cell (a ring or core the blueprint keeps free, such as the NASA Workbench's) */
    public boolean kept(int a, int d) {
        return in(a, d) && kept[i(a, d)];
    }

    /** @return true for a cell in a doorway zone (a doorway and the cells just inside it) */
    public boolean doorway(int a, int d) {
        return in(a, d) && base[i(a, d)] == DOORWAY;
    }

    /** @return the floor state under a cell ({@link #FREE}, {@link #DOORWAY}, {@link #TILE}, {@link #HOLE}) */
    byte base(int a, int d) {
        return base[i(a, d)];
    }

    /** @return true for a floor light tile (the furnisher's or the shadow fill's) */
    public boolean tile(int a, int d) {
        return in(a, d) && base[i(a, d)] == TILE;
    }

    /** @return the bottom piece on a cell, or null */
    public FurniturePiece pieceAt(int a, int d) {
        if (!in(a, d)) return null;
        Legend.Entry e = entry[i(a, d)];
        return e == null ? null : e.bottom();
    }

    /** @return the entry on a cell, or null */
    public Legend.Entry at(int a, int d) {
        return in(a, d) ? entry[i(a, d)] : null;
    }

    /** @return a hash of the room seed, a salt and a cell: the kit's only source of variety */
    public long hash(int salt, int a, int d) {
        long h = seed ^ (salt * 0x9E3779B97F4A7C15L) ^ ((long) a << 20) ^ ((long) d << 40);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /** @return {@link #hash} as an index in [0, n) */
    public int pick(int salt, int a, int d, int n) {
        return (int) Long.remainderUnsigned(hash(salt, a, d), n);
    }

    // --- reserving ------------------------------------------------------------------------------------------------

    /** Reserves a block of cells (clipped to the room): free floor nothing may stand on. */
    public void reserve(int a0, int a1, int d0, int d1) {
        for (int a = Math.max(0, Math.min(a0, a1)); a <= Math.min(L - 1, Math.max(a0, a1)); a++) {
            for (int d = Math.max(0, Math.min(d0, d1)); d <= Math.min(D - 1, Math.max(d0, d1)); d++) {
                reserved[i(a, d)] = true;
            }
        }
    }

    /** @return whether a cell is reserved */
    public boolean reserved(int a, int d) {
        return in(a, d) && reserved[i(a, d)];
    }

    /** The width of the hub's lanes: a portal's middle cells, and the cross they meet. */
    static final int LANE = 5;

    /** The door's clear path from the door wall to the back wall, and a path in from every other opening. */
    public void spine() {
        spine(0);
    }

    /**
     * The door's clear path, its aisle ({@link #aisleLo()} to {@link #aisleHi()}: the doorway and its jambs), from
     * the door wall up to depth {@code stop} (the firing line of a range, the corridor of a brig), then a path as wide
     * from every other opening in the back or door wall (a child's or a loop's door, D278)
     * to it: straight in to the nearest lamp row, along it to the spine, and the spine on down. A door in a side
     * wall keeps its doorway zone, and {@link #reachable()} opens the cheapest way to it. The hub, which has no
     * primary door, gets a cross through its middle and a lane from each portal to it.
     */
    public void spine(int stop) {
        if (primary == null) {
            // The hub: a cross through the middle, and a lane from each portal's middle to it.
            int ca = (L - 1) / 2, cd = (D - 1) / 2, w = LANE / 2;
            reserve(ca - w, ca + w, 0, D - 1);
            reserve(0, L - 1, cd - w, cd + w);
            for (Opening o : openings) {
                int mid = (o.lo + o.hi) / 2, lo = Math.max(o.lo, mid - w), hi = Math.min(o.hi, mid + w);
                switch (o.wall) {
                    case LEFT:
                        reserve(0, ca, lo, hi);
                        reserve(ca - w, ca + w, Math.min(lo, cd), Math.max(hi, cd));
                        break;
                    case RIGHT:
                        reserve(ca, L - 1, lo, hi);
                        reserve(ca - w, ca + w, Math.min(lo, cd), Math.max(hi, cd));
                        break;
                    case BACK:
                        reserve(lo, hi, 0, cd);
                        reserve(Math.min(lo, ca), Math.max(hi, ca), cd - w, cd + w);
                        break;
                    default:
                        reserve(lo, hi, cd, D - 1);
                        reserve(Math.min(lo, ca), Math.max(hi, ca), cd - w, cd + w);
                        break;
                }
            }
            return;
        }
        // The aisle is the doorway and its jambs (D321): a 3-wide door under a 5-wide frame keeps 5 cells clear.
        int lo = aisleLo(), hi = aisleHi();
        int top = Math.max(0, Math.min(stop, D - 1));
        for (Opening o : openings) {
            if (o.primary) continue;
            switch (o.wall) {
                case LEFT:
                case RIGHT:
                    // A side door keeps its doorway zone; the reachability pass opens the cheapest way to it, so a
                    // whole band of beds is not given up for one door.
                    break;
                case BACK: {
                    int row = aisleBelow(0);
                    reserve(o.lo - 1, o.hi + 1, 0, row);
                    reserve(Math.min(o.lo - 1, lo), Math.max(o.hi + 1, hi), row, row);
                    top = Math.min(top, row);
                    break;
                }
                default: {
                    int row = aisleAbove(D - 1);
                    reserve(o.lo - 1, o.hi + 1, row, D - 1);
                    reserve(Math.min(o.lo - 1, lo), Math.max(o.hi + 1, hi), row, row);
                    break;
                }
            }
        }
        reserve(lo, hi, top, D - 1);
    }

    /** @return the first lamp row at or below d + 1 (toward the door), or d + 2 when there is none */
    private int aisleBelow(int d) {
        for (int r = d + 1; r < D; r++) if (lampRow(r)) return r;
        return Math.min(D - 1, d + 2);
    }

    /** @return the first lamp row at or above d - 2 (toward the back), or d - 2 when there is none */
    private int aisleAbove(int d) {
        for (int r = d - 2; r >= 0; r--) if (lampRow(r)) return r;
        return Math.max(0, d - 2);
    }

    // --- lamp rows and bands --------------------------------------------------------------------------------------

    /** @return whether row d holds a floor light tile */
    public boolean lampRow(int d) {
        if (d < 0 || d >= D) return false;
        for (int a = 0; a < L; a++) if (base[i(a, d)] == TILE) return true;
        return false;
    }

    /** @return the lamp rows, back first */
    public List<Integer> lampRows() {
        List<Integer> out = new ArrayList<Integer>();
        for (int d = 0; d < D; d++) if (lampRow(d)) out.add(d);
        return out;
    }

    /** @return the runs of rows from d0 to d1 with no light tile, as {first, last} pairs, back first */
    public List<int[]> bands(int d0, int d1) {
        List<int[]> out = new ArrayList<int[]>();
        int start = -1;
        for (int d = Math.max(0, d0); d <= Math.min(D - 1, d1); d++) {
            if (lampRow(d)) {
                if (start >= 0) out.add(new int[] { start, d - 1 });
                start = -1;
            } else if (start < 0) {
                start = d;
            }
        }
        if (start >= 0) out.add(new int[] { start, Math.min(D - 1, d1) });
        return out;
    }

    // --- stamping -------------------------------------------------------------------------------------------------

    /** @return whether entry {@code e} may stand on (a, d) right now, the chest rule aside */
    boolean fits(int a, int d, Legend.Entry e) {
        if (!in(a, d)) return false;
        int i = i(a, d);
        if (entry[i] != null || reserved[i] || kept[i]) return false;
        byte b = base[i];
        if (b == DOORWAY || b == HOLE) return false;
        if (e.kind == Legend.Entry.Kind.KEEP_STRICT) return b == FREE;
        return b != TILE || e.overTile();
    }

    private int chestsAround(int a, int d) {
        int n = 0;
        for (Dir x : Dir.REAL) {
            Legend.Entry e = at(a + x.da, d + x.dd);
            if (e != null && e.chest()) n++;
        }
        return n;
    }

    /** @return whether the chest at (a, d) keeps the chest rule: it and any chest it touches touch at most one */
    private boolean chestOk(int a, int d) {
        if (chestsAround(a, d) > 1) return false;
        for (Dir x : Dir.REAL) {
            Legend.Entry e = at(a + x.da, d + x.dd);
            if (e != null && e.chest() && chestsAround(a + x.da, d + x.dd) > 1) return false;
        }
        return true;
    }

    private void set(int a, int d, Legend.Entry e, char c, Dir f, int g) {
        int i = i(a, d);
        if (e.kind == Legend.Entry.Kind.KEEP || e.kind == Legend.Entry.Kind.KEEP_STRICT) {
            kept[i] = true;
            core[i] = e.kind == Legend.Entry.Kind.KEEP_STRICT;
            glyph[i] = c;
            return;
        }
        entry[i] = e;
        glyph[i] = c;
        facing[i] = f;
        group[i] = g;
    }

    private void unset(int a, int d) {
        int i = i(a, d);
        entry[i] = null;
        kept[i] = false;
        core[i] = false;
        glyph[i] = 0;
        facing[i] = null;
        group[i] = 0;
    }

    /**
     * Stamps a module with its back-left cell at (a, d), all or nothing.
     *
     * @return whether it was placed
     */
    public boolean anchor(Module m, int a, int d) {
        for (Module.Cell c : m.cells) {
            int ca = a + c.da, cd = d + c.dd;
            if (c.entry == null) {
                if (!in(ca, cd)) return false;
            } else if (!fits(ca, cd, c.entry)) {
                return false;
            }
        }
        java.util.Map<Integer, Integer> groups = new java.util.HashMap<Integer, Integer>();
        String[] rows = m.rows();
        for (Module.Cell c : m.cells) {
            if (c.entry == null) continue;
            int g = 0;
            if (c.group != 0) {
                Integer have = groups.get(c.group);
                if (have == null) {
                    have = nextGroup++;
                    groups.put(c.group, have);
                }
                g = have;
            }
            set(a + c.da, d + c.dd, c.entry, rows[c.dd].charAt(c.da), c.facing, g);
        }
        for (Module.Cell c : m.cells) {
            if (c.entry != null && c.entry.chest() && !chestOk(a + c.da, d + c.dd)) {
                for (Module.Cell u : m.cells) if (u.entry != null) unset(a + u.da, d + u.dd);
                return false;
            }
        }
        return true;
    }

    /**
     * Stamps a module as {@link #anchor(Module, int, int)} does, then keeps it only when {@code keep} still holds for
     * the room with it in place (a blueprint's own rule, such as "nothing new is walled off"); otherwise the stamp is
     * taken back whole.
     *
     * @return whether it was placed and kept
     */
    public boolean anchorIf(Module m, int a, int d, java.util.function.Predicate<Kit> keep) {
        if (!anchor(m, a, d)) return false;
        if (keep.test(this)) return true;
        for (Module.Cell c : m.cells) if (c.entry != null) unset(a + c.da, d + c.dd);
        return false;
    }

    /** Stamps rows of the room's legend at (a, d), all or nothing. */
    public boolean anchor(int a, int d, String... rows) {
        return anchor(module(rows), a, d);
    }

    /**
     * Stamps a module at the fitting place nearest (a, d): Manhattan distance, then depth, then lateral (D252's NASA
     * search, generalized).
     *
     * @return whether it was placed anywhere
     */
    public boolean anchorNear(Module m, int a, int d) {
        List<int[]> c = new ArrayList<int[]>();
        for (int ca = 0; ca + m.width <= L; ca++)
            for (int cd = 0; cd + m.height <= D; cd++) c.add(new int[] { ca, cd });
        Collections.sort(c, new Comparator<int[]>() {

            @Override
            public int compare(int[] p, int[] q) {
                int k = (Math.abs(p[0] - a) + Math.abs(p[1] - d)) - (Math.abs(q[0] - a) + Math.abs(q[1] - d));
                if (k != 0) return k;
                k = p[1] - q[1];
                return k != 0 ? k : p[0] - q[0];
            }
        });
        for (int[] p : c) if (anchor(m, p[0], p[1])) return true;
        return false;
    }

    // --- dropping light tiles and sinking pools (D321) --------------------------------------------------------------

    /** How deep a pool's water runs: the floor layer and the layers under it; its lining lies one layer lower. */
    public static final int POOL_DEPTH = 3;

    private final List<int[]> droppedTiles = new ArrayList<int[]>(), filledHoles = new ArrayList<int[]>();

    /**
     * Turns a floor light tile into free floor, so a solid piece or a pool may take the cell; the furnisher then drops
     * the tile's light mark, and the shadow fill relights what goes dark.
     */
    private void dropTile(int a, int d) {
        int i = i(a, d);
        if (base[i] != TILE) return;
        base[i] = FREE;
        droppedTiles.add(new int[] { a, d });
    }

    /**
     * Turns a floor hole back into whole floor, so a wall or a pool may take the cell; the furnisher then drops the
     * cell's floor damage mark, so no hole ever opens under a cell wall or beside a pool's water.
     */
    private void fillHole(int a, int d) {
        int i = i(a, d);
        if (base[i] != HOLE) return;
        base[i] = FREE;
        filledHoles.add(new int[] { a, d });
    }

    /** @return the light tiles a blueprint built over, as {a, d} */
    public List<int[]> droppedTiles() {
        return Collections.unmodifiableList(new ArrayList<int[]>(droppedTiles));
    }

    /** @return the floor holes a blueprint built over, as {a, d} */
    public List<int[]> filledHoles() {
        return Collections.unmodifiableList(new ArrayList<int[]>(filledHoles));
    }

    /**
     * Puts one character on (a, d) as {@link #put(int, int, char)} does, and where only a floor light tile or a floor
     * hole stands in the way, builds over it: the tile is dropped or the hole filled (a cell wall or a door that must
     * not leave a gap, D322).
     */
    public boolean build(int a, int d, char c) {
        Legend.Entry e = legend.get(c);
        if (e == null) throw new IllegalArgumentException("no legend entry for '" + c + "'");
        if (!in(a, d) || base(a, d) != TILE && base(a, d) != HOLE || fits(a, d, e)) return put(a, d, e, c);
        int i = i(a, d);
        if (entry[i] != null || reserved[i] || kept[i]) return false;
        byte was = base[i];
        base[i] = FREE;
        if (put(a, d, e, c)) {
            (was == TILE ? droppedTiles : filledHoles).add(new int[] { a, d });
            return true;
        }
        base[i] = was;
        return false;
    }

    /** The pool's entry: water from the floor layer down; light passes it, no one walks over it. */
    private static final Legend.Entry POOL = Legend.inlay(FurniturePiece.POOL_WATER);

    /** @return true for a pool cell */
    public boolean pool(int a, int d) {
        Legend.Entry e = at(a, d);
        return e != null && e.floorPiece() == FurniturePiece.POOL_WATER;
    }

    /**
     * Sinks a pool into the floor over rows d0..d1 and lateral cells a0..a1, shrunk until it fits: every pool cell is
     * free floor, a light tile (which it drops) or a floor hole (which it fills), at least one cell from every wall,
     * and no cell around it is a doorway. The holes in its rim are filled too, so no damage opens the pool's side.
     * Each step trims the side that keeps the larger pool.
     *
     * @return the pool as {a0, a1, d0, d1}, or null when nothing of at least {@code min} x {@code min} is left
     */
    public int[] sinkPool(int a0, int a1, int d0, int d1, int min) {
        int[] r = { Math.max(1, Math.min(a0, a1)), Math.min(L - 2, Math.max(a0, a1)), Math.max(1, Math.min(d0, d1)),
            Math.min(D - 2, Math.max(d0, d1)) };
        while (r[1] - r[0] + 1 >= min && r[3] - r[2] + 1 >= min) {
            int[] bad = poolBlocker(r);
            if (bad == null) {
                for (int a = r[0]; a <= r[1]; a++) {
                    for (int d = r[2]; d <= r[3]; d++) {
                        dropTile(a, d);
                        fillHole(a, d);
                        set(a, d, POOL, '%', Dir.TO_DOOR, 0);
                    }
                }
                // The rim stays whole: no hole opens beside the water.
                for (int a = r[0] - 1; a <= r[1] + 1; a++) for (int d = r[2] - 1; d <= r[3] + 1; d++) fillHole(a, d);
                return r;
            }
            // Cut the pool short of the blocker on the side that leaves the most.
            int ba = bad[0], bd = bad[1];
            int[][] cuts = { { r[0], ba - 1, r[2], r[3] }, { ba + 1, r[1], r[2], r[3] }, { r[0], r[1], r[2], bd - 1 },
                { r[0], r[1], bd + 1, r[3] } };
            int[] best = null;
            long bestArea = -1;
            for (int[] c : cuts) {
                long area = (long) Math.max(0, c[1] - c[0] + 1) * Math.max(0, c[3] - c[2] + 1);
                if (area > bestArea && (c[0] != r[0] || c[1] != r[1] || c[2] != r[2] || c[3] != r[3])) {
                    bestArea = area;
                    best = c;
                }
            }
            if (best == null || bestArea <= 0) return null;
            r = best;
        }
        return null;
    }

    /**
     * @return the pool cell that must go for the pool {a0, a1, d0, d1} to sink, as {a, d}: a pool cell that is taken,
     *         or the pool cell nearest a doorway in the ring around it; null when the pool fits
     */
    private int[] poolBlocker(int[] r) {
        for (int a = r[0] - 1; a <= r[1] + 1; a++) {
            for (int d = r[2] - 1; d <= r[3] + 1; d++) {
                // The pool keeps a cell off every wall, so its ring lies inside the room.
                boolean ring = a < r[0] || a > r[1] || d < r[2] || d > r[3];
                int i = i(a, d);
                if (ring) {
                    if (base[i] == DOORWAY) return inward(r, a, d);
                    continue;
                }
                if (entry[i] != null || reserved[i] || kept[i] || base[i] == DOORWAY) {
                    return new int[] { a, d };
                }
            }
        }
        return null;
    }

    /** @return the pool cell nearest a ring cell, which the trim takes out with every pool cell in line with it */
    private static int[] inward(int[] r, int a, int d) {
        return new int[] { Math.max(r[0], Math.min(r[1], a)), Math.max(r[2], Math.min(r[3], d)) };
    }

    /** Puts one character of the room's legend on (a, d). */
    public boolean put(int a, int d, char c) {
        Legend.Entry e = legend.get(c);
        if (e == null) throw new IllegalArgumentException("no legend entry for '" + c + "'");
        return put(a, d, e, c);
    }

    /** Puts an entry on (a, d); the character is what the room maps show for it. */
    public boolean put(int a, int d, Legend.Entry e, char c) {
        if (e.bottom() == FurniturePiece.BED_FOOT || e.bottom() == FurniturePiece.BED_HEAD) {
            throw new IllegalArgumentException("beds go in modules, which pair the halves");
        }
        if (!fits(a, d, e)) return false;
        set(a, d, e, c, e.facing, e.grouped ? nextGroup++ : 0);
        if (e.chest() && !chestOk(a, d)) {
            unset(a, d);
            return false;
        }
        return true;
    }

    /** A pattern along row d from a0 to a1, repeating; dots and spaces skip a cell. Cell by cell, not atomic. */
    public int strip(int d, int a0, int a1, String pattern) {
        int n = 0;
        for (int k = 0, a = a0; a <= a1; a++, k++) {
            char c = pattern.charAt(k % pattern.length());
            if (c != '.' && c != ' ' && put(a, d, c)) n++;
        }
        return n;
    }

    /** A pattern down column a from d0 to d1, repeating; dots and spaces skip a cell. */
    public int column(int a, int d0, int d1, String pattern) {
        int n = 0;
        for (int k = 0, d = d0; d <= d1; d++, k++) {
            char c = pattern.charAt(k % pattern.length());
            if (c != '.' && c != ' ' && put(a, d, c)) n++;
        }
        return n;
    }

    /**
     * A pattern along row d outward from the spine on both sides (from {@code spineHi + 1 + gap} up to aMax, and
     * from {@code spineLo - 1 - gap} down to aMin), so a wall strip reads symmetric.
     */
    public int mirrorStrip(int d, int aMin, int aMax, int gap, String pattern) {
        int n = 0;
        for (int k = 0, a = spineHi() + 1 + gap; a <= aMax; a++, k++) {
            char c = pattern.charAt(k % pattern.length());
            if (c != '.' && c != ' ' && put(a, d, c)) n++;
        }
        for (int k = 0, a = spineLo() - 1 - gap; a >= aMin; a--, k++) {
            char c = pattern.charAt(k % pattern.length());
            if (c != '.' && c != ' ' && put(a, d, c)) n++;
        }
        return n;
    }

    /**
     * Repeats a module outward from the spine on both sides, every {@code pitch} cells, its first copy {@code gap}
     * cells clear of the spine; the low-a side takes the mirrored module.
     *
     * @return how many copies were placed
     */
    public int tile(Module m, int pitch, int d0, int aMin, int aMax, int gap) {
        Module mm = m.mirror();
        int n = 0;
        for (int a = spineLo() - 1 - gap - m.width + 1; a >= aMin; a -= pitch) if (anchor(mm, a, d0)) n++;
        for (int a = spineHi() + 1 + gap; a + m.width - 1 <= aMax; a += pitch) if (anchor(m, a, d0)) n++;
        return n;
    }

    /**
     * {@link #tile(Module, int, int, int, int, int)} across the room, one cell clear of the spine and the side walls.
     */
    public int tile(Module m, int pitch, int d0) {
        return tile(m, pitch, d0, 1, L - 2, 1);
    }

    /**
     * Fills every band between d0 and d1 with the first of {@code options} (tallest first) whose height fits it,
     * centered in the band and tiled outward from the spine.
     *
     * @return how many copies were placed
     */
    public int bandTile(int pitch, int d0, int d1, int aMin, int aMax, int gap, Module... options) {
        int n = 0;
        for (int[] b : bands(d0, d1)) {
            int h = b[1] - b[0] + 1;
            for (Module m : options) {
                if (m.height > h) continue;
                n += tile(m, pitch, b[0] + (h - m.height) / 2, aMin, aMax, gap);
                break;
            }
        }
        return n;
    }

    /**
     * Fills one band with the first of {@code options} that fits it and places at least one copy, so a narrow or
     * shallow room falls back to smaller modules instead of staying empty.
     *
     * @param pitches the pitch of each option, in order
     * @return how many copies were placed
     */
    public int fillBand(int b0, int b1, int aMin, int aMax, int gap, Module[] options, int[] pitches) {
        int h = b1 - b0 + 1;
        for (int i = 0; i < options.length; i++) {
            Module m = options[i];
            if (m.height > h) continue;
            int n = tile(m, pitches[i], b0 + (h - m.height) / 2, aMin, aMax, gap);
            if (n > 0) return n;
        }
        return 0;
    }

    /**
     * The chest-and-crate caches in the free corners (every room of 16 x 14 interior or more): a crate and a chest
     * against the back wall's corners, and a crate pair with a chest in each front corner, clear of the door.
     */
    public void corners() {
        anchor(0, 2, "kC");
        anchor(L - 2, 2, "Ck");
        anchor(0, D - 3, "k.", "Ck");
        anchor(L - 2, D - 3, ".k", "kC");
    }

    /** Offsets tried around a wanted cell, nearest first, a step in depth costing two steps along the row. */
    private static final int[][] NEAR = near();

    private static int[][] near() {
        List<int[]> l = new ArrayList<int[]>();
        for (int dd = -3; dd <= 3; dd++) for (int da = -12; da <= 12; da++) l.add(new int[] { da, dd });
        Collections.sort(l, new Comparator<int[]>() {

            @Override
            public int compare(int[] p, int[] q) {
                int c = (Math.abs(p[0]) + 2 * Math.abs(p[1])) - (Math.abs(q[0]) + 2 * Math.abs(q[1]));
                if (c != 0) return c;
                c = Math.abs(p[1]) - Math.abs(q[1]);
                if (c != 0) return c;
                c = p[1] - q[1];
                return c != 0 ? c : p[0] - q[0];
            }
        });
        return l.toArray(new int[0][]);
    }

    /** Puts a character at (a, d) or the nearest cell it fits, same row first. */
    public boolean putNear(int a, int d, char c) {
        for (int[] o : NEAR) if (put(a + o[0], d + o[1], c)) return true;
        return false;
    }

    /** A machine of {@code role} at (a, d) or the nearest cell it fits. */
    public boolean machineNear(int a, int d, MachineRole role) {
        Legend.Entry e = Legend.machine(role);
        for (int[] o : NEAR) if (put(a + o[0], d + o[1], e, 'M')) return true;
        return false;
    }

    /** A mark of any kind (a flood cell, rubble) at (a, d) or the nearest cell it fits. */
    public boolean markNear(int a, int d, Legend.Entry e, char c) {
        for (int[] o : NEAR) if (put(a + o[0], d + o[1], e, c)) return true;
        return false;
    }

    /**
     * Where a spawner may stand (D326): up to {@code n} bare floor cells that a walk from the doorway reaches, far
     * apart.
     * Call it after the commit, when nothing else will stand in the room. The first cell is a hash of the room, each
     * next one the cell farthest from those chosen. A cell holding a floor mark is not offered.
     *
     * @param n the most cells wanted
     * @return {a, d} cells, fewer than {@code n} (or none) when the room has no such floor
     */
    public List<int[]> spawnerSpots(int n) {
        boolean[] seen = flood();
        boolean[] bare = flood(true);
        final List<int[]> pool = new ArrayList<int[]>();
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                if (!free(a, d) || !bare[i(a, d)]) continue;
                boolean marked = false;
                for (Placed p : floorMarks) marked |= p.a == a && p.d == d;
                if (!marked) pool.add(new int[] { a, d });
            }
        }
        List<int[]> out = new ArrayList<int[]>();
        Legend.Entry spot = Legend.mark(MarkKind.SPAWNER, "spot");
        int[] light = light();
        boolean tiled = false;
        for (byte b : base) tiled |= b == TILE;
        try {
            while (out.size() < n && !pool.isEmpty()) {
                int[] pick = null;
                long bestKey = Long.MAX_VALUE;
                int bestFar = -1;
                for (int[] c : pool) {
                    int far = Integer.MAX_VALUE;
                    for (int[] o : out) far = Math.min(far, Math.abs(c[0] - o[0]) + Math.abs(c[1] - o[1]));
                    if (out.isEmpty()) far = 0;
                    long h = hash(0x59A, c[0], c[1]);
                    if (far > bestFar || far == bestFar && h < bestKey) {
                        if (!keepsRoomOpen(c, out, spot, seen, light, tiled)) continue;
                        pick = c;
                        bestFar = far;
                        bestKey = h;
                    }
                }
                if (pick == null) break;
                entry[i(pick[0], pick[1])] = spot;
                out.add(pick);
                pool.remove(pick);
                seen = flood();
                light = light();
            }
        } finally {
            for (int[] c : out) entry[i(c[0], c[1])] = null;
        }
        return out;
    }

    /**
     * @return whether a spawner on cell {@code c} leaves every walked cell reachable from the door and every lit cell
     *         as bright as it was
     */
    private boolean keepsRoomOpen(int[] c, List<int[]> chosen, Legend.Entry spot, boolean[] seen, int[] light,
        boolean tiled) {
        int ci = i(c[0], c[1]);
        entry[ci] = spot;
        try {
            boolean[] now = flood();
            boolean[] bare = flood(true);
            if (!touches(bare, c[0], c[1])) return false;
            for (int[] o : chosen) if (!touches(bare, o[0], o[1])) return false;
            for (int i = 0; i < L * D; i++) {
                if (i != ci && seen[i] && !now[i]) return false;
            }
            for (int a = 0; a < L; a++) {
                for (int d = 0; d < D; d++) {
                    Legend.Entry e = entry[i(a, d)];
                    if (e != null && e.kind == Legend.Entry.Kind.MARK && !touches(now, a, d)) return false;
                }
            }
            if (tiled) {
                int[] after = light();
                for (int i = 0; i < L * D; i++) {
                    if (i != ci && lit(i) && light[i] >= MIN_LIGHT && after[i] < MIN_LIGHT) return false;
                }
            }
            return true;
        } finally {
            entry[ci] = null;
        }
    }

    private final List<Placed> floorMarks = new ArrayList<Placed>();

    /**
     * A mark that takes no cell (a flood cell, a fallout rubble cell): kept only on a cell that is free now, and
     * nothing is stopped from standing there later.
     */
    public boolean floorMark(int a, int d, MarkKind kind, String key) {
        if (!free(a, d)) return false;
        floorMarks.add(new Placed(a, d, Legend.mark(kind, key), Dir.TO_DOOR, 0, -1, key));
        return true;
    }

    /** @return the marks that take no cell, whose cell nothing else took */
    public List<Placed> floorMarks() {
        List<Placed> out = new ArrayList<Placed>();
        for (Placed p : floorMarks) if (entry[i(p.a, p.d)] == null) out.add(p);
        return out;
    }

    /** Lays nothing on (a, d) and keeps it walkable and empty (a sign's cell, a niche's mouth). */
    public void keepClear(int a, int d) {
        reserve(a, a, d, d);
    }

    // --- after the blueprint --------------------------------------------------------------------------------------

    private boolean walk(int i) {
        Legend.Entry e = entry[i];
        return e == null || e.walkable();
    }

    private boolean[] flood() {
        return flood(false);
    }

    /** @param bare walk only cells with nothing on them (a door piece or a rug does not count) */
    private boolean[] flood(boolean bare) {
        boolean[] seen = new boolean[L * D];
        ArrayDeque<int[]> q = new ArrayDeque<int[]>();
        for (Opening o : openings) {
            // From the doorway; in the hub, from its first portal (every other must then be reached).
            if (primary != null ? !o.primary : o != openings.get(0)) continue;
            for (int[] c : o.inside(L, D)) {
                if (!in(c[0], c[1]) || !(bare ? entry[i(c[0], c[1])] == null : walk(i(c[0], c[1])))
                    || seen[i(c[0], c[1])]) continue;
                seen[i(c[0], c[1])] = true;
                q.add(c);
            }
        }
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (Dir x : Dir.REAL) {
                int a = c[0] + x.da, d = c[1] + x.dd;
                if (!in(a, d) || seen[i(a, d)] || !(bare ? entry[i(a, d)] == null : walk(i(a, d)))) continue;
                seen[i(a, d)] = true;
                q.add(new int[] { a, d });
            }
        }
        return seen;
    }

    private boolean touches(boolean[] seen, int a, int d) {
        for (Dir x : Dir.REAL) {
            int ca = a + x.da, cd = d + x.dd;
            if (in(ca, cd) && seen[i(ca, cd)]) return true;
        }
        return false;
    }

    /**
     * Clears pieces that wall a chest, machine or guard off from the door, pieces that cut another opening off (D278)
     * and pieces that wall in a pocket of open floor: from each, the shortest way through pieces to a walked cell is
     * cleared, a grouped piece as a whole.
     */
    public void reachable() {
        boolean[] seen = flood();
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                Legend.Entry e = entry[i(a, d)];
                if (e == null || e.kind != Legend.Entry.Kind.MARK || touches(seen, a, d)) continue;
                clearPath(seen, a, d, false);
                seen = flood();
                // Walled in by other chests: the nearest of them gives way (a last resort; stamps rarely do it).
                if (!touches(seen, a, d)) {
                    clearPath(seen, a, d, true);
                    seen = flood();
                }
            }
        }
        for (Opening o : openings) {
            for (int[] c : o.inside(L, D)) {
                if (!in(c[0], c[1]) || seen[i(c[0], c[1])]) continue;
                clearPath(seen, c[0], c[1], false);
                seen = flood();
            }
        }
        // Every open cell too: no pocket of floor walled in by pieces.
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                if (seen[i(a, d)] || !walk(i(a, d))) continue;
                clearPath(seen, a, d, false);
                seen = flood();
                // Walled in by chests or machines alone: a crate fills the cell instead.
                if (!seen[i(a, d)] && entry[i(a, d)] == null && base[i(a, d)] == FREE && !reserved[i(a, d)]) {
                    set(a, d, legend.get('k'), 'k', Dir.AUTO, 0);
                }
            }
        }
    }

    /**
     * Clears the shortest way from (a0, d0) through pieces to a walked cell; with {@code throughChests} a chest on the
     * way gives way too (never a machine, a guard or a partition, D322).
     */
    private void clearPath(boolean[] seen, int a0, int d0, boolean throughChests) {
        int[] prev = new int[L * D];
        java.util.Arrays.fill(prev, -2);
        ArrayDeque<int[]> q = new ArrayDeque<int[]>();
        prev[i(a0, d0)] = -1;
        q.add(new int[] { a0, d0 });
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (Dir x : Dir.REAL) {
                int a = c[0] + x.da, d = c[1] + x.dd;
                if (!in(a, d) || prev[i(a, d)] != -2) continue;
                Legend.Entry e = entry[i(a, d)];
                if (e != null && e.kind == Legend.Entry.Kind.MARK && !(throughChests && e.chest())) continue;
                // A partition is part of the room (an armory's display wall, a brig cell's wall): never cleared.
                if (e != null && e.bottom() == FurniturePiece.PARTITION) continue;
                prev[i(a, d)] = i(c[0], c[1]);
                if (seen[i(a, d)]) {
                    for (int at = prev[i(a, d)]; at >= 0 && at != i(a0, d0); at = prev[at]) remove(at / D, at % D);
                    return;
                }
                q.add(new int[] { a, d });
            }
        }
    }

    /** Removes the piece (or a chest) on a cell, and every cell of its group. */
    private void remove(int a, int d) {
        Legend.Entry e = entry[i(a, d)];
        if (e == null || e.kind != Legend.Entry.Kind.PIECE && !e.chest()) return;
        int g = group[i(a, d)];
        if (g == 0) {
            unset(a, d);
            return;
        }
        for (int i = 0; i < L * D; i++) if (group[i] == g) unset(i / D, i % D);
    }

    /**
     * @return whether a cell must read light 8: open floor, a walked piece, or a piece the raster leaves open (a
     *         chair, a stool, an armor stand), which a player stands beside or in
     */
    boolean lit(int i) {
        Legend.Entry e = entry[i];
        if (e == null || e.walkable()) return true;
        FurniturePiece p = e.bottom();
        return e.kind == Legend.Entry.Kind.PIECE && p != null && p.role() == com.gregguns.icbm.complex.Cell.AIR;
    }

    /** @return the light at foot height over each cell from the floor tiles, through cells light passes */
    int[] light() {
        int[] light = new int[L * D];
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        for (int i = 0; i < L * D; i++) {
            if (base[i] == TILE) {
                light[i] = TILE_LIGHT;
                q.add(i);
            }
        }
        while (!q.isEmpty()) {
            int c = q.poll();
            int a = c / D, d = c % D;
            for (Dir x : Dir.REAL) {
                int na = a + x.da, nd = d + x.dd;
                if (!in(na, nd)) continue;
                int n = i(na, nd);
                Legend.Entry e = entry[n];
                if (e != null && !e.clear()) continue;
                if (light[c] - 1 > light[n]) {
                    light[n] = light[c] - 1;
                    q.add(n);
                }
            }
        }
        return light;
    }

    /**
     * The shadow fill (D305): in a room lit by floor tiles, adds a floor light tile on (or nearest to) the darkest
     * open cell the light from the tiles leaves under 8, until none is left, so tall props never leave an aisle or a
     * chair dark. Rooms with no floor tiles (the low annexes, lit from the ceiling) are left alone.
     *
     * @return the cells turned into light tiles, as {a, d}
     */
    public List<int[]> shadowFill() {
        boolean any = false;
        for (byte b : base) any |= b == TILE;
        if (!any) return Collections.emptyList();
        for (int round = 0; round < 64; round++) {
            int[] light = light();
            int dark = -1;
            for (int i = 0; i < L * D; i++) {
                if (!lit(i) || light[i] >= MIN_LIGHT) continue;
                if (dark < 0 || light[i] < light[dark]) dark = i;
            }
            if (dark < 0) break;
            // The tile goes on the dark cell, or on the nearest empty floor to it (a dark chair keeps its place).
            int best = -1, bestDist = Integer.MAX_VALUE;
            for (int i = 0; i < L * D; i++) {
                if (entry[i] != null || base[i] != FREE && base[i] != DOORWAY) continue;
                int dist = Math.abs(i / D - dark / D) + Math.abs(i % D - dark % D);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = i;
                }
            }
            if (best < 0) break;
            base[best] = TILE;
            addedTiles.add(new int[] { best / D, best % D });
        }
        return Collections.unmodifiableList(new ArrayList<int[]>(addedTiles));
    }

    /** One placed cell of the committed room. */
    public static final class Placed {

        public final int a, d;
        /** What stands there. */
        public final Legend.Entry entry;
        /** The resolved facing (never {@link Dir#AUTO}). */
        public final Dir facing;
        /** The kit group (0 for none): a bed's halves, a prism's cells. */
        public final int group;
        /** The resolved variant of the pieces. */
        public final int variant;
        /** For a mark: its key (the room's default loot key filled in). */
        public final String key;

        Placed(int a, int d, Legend.Entry entry, Dir facing, int group, int variant, String key) {
            this.a = a;
            this.d = d;
            this.entry = entry;
            this.facing = facing;
            this.group = group;
            this.variant = variant;
            this.key = key;
        }
    }

    /**
     * Resolves every facing left to the kit, drops each chair that faces nothing to sit at, and lists what stands in
     * the room: pieces and non-chest marks in cell order, then the chests in slot order (the safe first, then
     * outward from the spine, back to front), which the loot budget reads as the room's priority.
     */
    public List<Placed> commit() {
        List<Placed> out = new ArrayList<Placed>();
        List<Placed> chests = new ArrayList<Placed>();
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                Legend.Entry e = entry[i(a, d)];
                if (e == null) continue;
                Dir f = facing[i(a, d)];
                if (f == null || f == Dir.AUTO) f = auto(a, d, e);
                if (f == null) continue; // a chair with nothing to sit at
                if (e.bottom() == FurniturePiece.CHAIR && !seatsAt(a + f.da, d + f.dd)) continue;
                Placed p = new Placed(a, d, e, f, group[i(a, d)], variantOf(a, d, e), keyOf(e));
                (e.chest() ? chests : out).add(p);
            }
        }
        final int mid2 = spineLo() + spineHi();
        Collections.sort(chests, new Comparator<Placed>() {

            @Override
            public int compare(Placed p, Placed q) {
                if (p.entry.primary != q.entry.primary) return p.entry.primary ? -1 : 1;
                int k = Math.abs(2 * p.a - mid2) - Math.abs(2 * q.a - mid2);
                if (k != 0) return k;
                k = p.d - q.d;
                return k != 0 ? k : p.a - q.a;
            }
        });
        out.addAll(chests);
        return out;
    }

    private String keyOf(Legend.Entry e) {
        if (e.kind != Legend.Entry.Kind.MARK) return null;
        return e.key != null ? e.key : e.chest() ? defaultLoot : "room";
    }

    private boolean seatsAt(int a, int d) {
        FurniturePiece p = pieceAt(a, d);
        return p != null && p.seatsAt();
    }

    private boolean nonWalkPiece(int a, int d) {
        if (!in(a, d)) return false;
        Legend.Entry e = entry[i(a, d)];
        return e != null && !e.walkable();
    }

    private boolean walkCell(int a, int d) {
        return in(a, d) && walk(i(a, d));
    }

    /** The kit's facing for an entry left to it; null drops a chair with nothing to sit at. */
    private Dir auto(int a, int d, Legend.Entry e) {
        FurniturePiece p = e.bottom();
        if (p != null && p.seat()) {
            for (Dir x : Dir.REAL) if (seatsAt(a + x.da, d + x.dd)) return x;
            if (p == FurniturePiece.CHAIR) return null;
        }
        if (p != null && (p == FurniturePiece.DESK || p == FurniturePiece.CONSOLE)) {
            for (Dir x : Dir.REAL) {
                FurniturePiece n = pieceAt(a + x.da, d + x.dd);
                if (n != null && n.seat()) return x;
            }
        }
        // Against a wall: face away from it (the back wall first).
        if (d == 0 && walkCell(a, 1)) return Dir.TO_DOOR;
        if (a == 0 && walkCell(1, d)) return Dir.PLUS_A;
        if (a == L - 1 && walkCell(L - 2, d)) return Dir.MINUS_A;
        if (d == D - 1 && walkCell(a, D - 2)) return Dir.TO_BACK;
        // Backed by a piece: face away from it, into the aisle.
        for (Dir x : Dir.REAL) {
            if (nonWalkPiece(a + x.da, d + x.dd) && walkCell(a - x.da, d - x.dd)) return x.opposite();
        }
        for (Dir x : Dir.REAL) if (walkCell(a + x.da, d + x.dd)) return x;
        if (d == 0) return Dir.TO_DOOR;
        if (a == 0) return Dir.PLUS_A;
        if (a == L - 1) return Dir.MINUS_A;
        return Dir.TO_DOOR;
    }

    /** The salts of the variant picks. */
    private static final int S_CONTAINER = 1, S_METAL = 2, S_CROP = 3, S_MUSHROOM = 4, S_SMALL = 5, S_CRATE = 6;

    /** The planks a wooden crate takes for each room wood: oak, spruce, birch, oak (jungle), oak (acacia), dark oak. */
    private static final int[] CRATE_WOOD = { 0, 1, 2, 0, 0, 5 };

    private int variantOf(int a, int d, Legend.Entry e) {
        if (e.variant >= 0) return e.variant;
        FurniturePiece p = e.bottom() != null ? e.bottom() : e.floor;
        if (p == null) return wood;
        int g = group[i(a, d)];
        switch (p) {
            case CRATE:
                return CRATE_WOOD[wood % CRATE_WOOD.length] + 8 * pick(S_CRATE, a, d, 3);
            case CONTAINER:
                return g != 0 ? (int) Long.remainderUnsigned(hash(S_CONTAINER, g, 0), 2) : 0;
            case METAL_PROP:
                return pick(S_METAL, a, d, 1000);
            case FARMLAND_CROP:
                return pick(S_CROP, 0, d, 3);
            case MYCELIUM_BED:
                return pick(S_MUSHROOM, a, d, 2);
            case ASH_HEAP:
            case LAUNDRY:
                return pick(S_SMALL, a, d, 2);
            case RUG:
                return 12;
            case ANVIL:
            case IRON_DOOR:
            case IRON_DOOR_TOP:
            case FURNACE:
            case CAULDRON:
                return 0;
            default:
                return wood;
        }
    }

    // --- measuring ------------------------------------------------------------------------------------------------

    /** A room's fill (C-rooms §6). */
    public static final class Fill {

        /**
         * Cells holding a piece, a mark or an inlay, over all interior cells; rugs, kept rings and aisles are empty.
         */
        public final double fill;
        /** The same over the cells at least 3 from every wall, the aisle left out (0 when there are none). */
        public final double center;
        public final int chests, machines;

        Fill(double fill, double center, int chests, int machines) {
            this.fill = fill;
            this.center = center;
            this.chests = chests;
            this.machines = machines;
        }
    }

    /** @return the room's fill as it stands now */
    public Fill fill() {
        int filled = 0, cn = 0, cf = 0, chests = 0, machines = 0;
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                Legend.Entry e = entry[i(a, d)];
                boolean full = e != null && e.bottom() != FurniturePiece.RUG;
                if (full) filled++;
                // The middle off the aisle: the aisle is the way in, empty by design (D321).
                boolean aisle = reserved[i(a, d)] && a >= aisleLo() && a <= aisleHi();
                boolean centre = a >= 3 && d >= 3 && a <= L - 4 && d <= D - 4 && !aisle;
                if (centre) {
                    cn++;
                    if (full) cf++;
                }
                if (e != null && e.chest()) chests++;
                if (e != null && e.kind == Legend.Entry.Kind.MARK && e.mark == MarkKind.MACHINE) machines++;
            }
        }
        return new Fill((double) filled / (L * D), cn == 0 ? 0 : (double) cf / cn, chests, machines);
    }

    /**
     * @return the room as C-blueprints draws it: the back wall on top, the doorway as {@code =}, one character per
     *         cell ({@code :} a light tile, {@code _} a floor hole)
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < L + 2; a++) sb.append('#');
        sb.append('\n');
        for (int d = 0; d < D; d++) {
            sb.append('#');
            for (int a = 0; a < L; a++) {
                int i = i(a, d);
                char c = glyph[i];
                if (c == 0) c = base[i] == TILE ? ':' : base[i] == HOLE ? '_' : '.';
                sb.append(c);
            }
            sb.append("#\n");
        }
        sb.append('#');
        for (int a = 0; a < L; a++) {
            boolean door = primary != null && primary.wall == Opening.Wall.DOOR && a >= primary.lo && a <= primary.hi;
            sb.append(door ? '=' : '#');
        }
        sb.append("#\n");
        return sb.toString();
    }
}
