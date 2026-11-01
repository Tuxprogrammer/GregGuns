package com.gregguns.icbm.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.Door;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.gen.MetalProps;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * How a room's furniture looks at its {@link RoomState} (D309, D323): what is gone, what is rubble, what is lit,
 * grown or low on water. A staffed room keeps everything, with lit furnaces, full cauldrons and crops sown in turns,
 * half of them ripe. A room in service has lost a few pieces and its crops are at mixed growth. A mothballed room has
 * lost more, its furnaces are cold, its cauldrons two-thirds full and its fields bare dirt with mushrooms (a crop needs
 * light 8, and the room is dark). A gutted room has lost most, with rubble where pieces stood, scavenged metal,
 * stripped containers, cauldrons a third full, dead fields and fresh rubble piles.
 *
 * <p>
 * Pieces wear by group ({@link FurniturePiece#groupOf}): a bed's two halves, a container prism and a piece's stacked
 * layers go or stay together. Every roll is a hash of the plan seed and the group or cell, so each populate window
 * agrees. The rubble piles a gutted room gains never cut any open cell, chest, machine or door off from the room's
 * doors: a pile cell is kept only where the open cells around it stay joined without it. Pure.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Room looks); D309
public final class FurnitureWear {

    private static final long S_WEAR = 0xF0A2, S_RUBBLE = 0xF0A3, S_LIVING = 0xF0A4, S_PILE = 0xF0A5, S_STRIP = 0xF0A6,
        S_PROP = 0xF0A7, S_CAP = 0xF0A8;

    /** Share of {@code NORMAL} pieces gone by state: staffed, in service, mothballed, gutted. */
    static final double[] NORMAL_LOST = { 0, 0.05, 0.30, 0.35 };
    /** Share of {@code SCAVENGED} pieces (metal props, coal) gone by state. */
    static final double[] SCAVENGED_LOST = { 0, 0, 0.50, 0.70 };
    /** Share of a gutted room's lost pieces that leave a rubble pile where they stood. */
    static final double LOST_RUBBLE = 0.40;
    /** Share of container prisms that lose their top layer in a gutted room. */
    static final double STRIPPED = 0.25;
    /** A gutted room gains one rubble pile per this many floor cells. */
    static final int PILE_CELLS = 40;
    /** Share of glass broken in a gutted room. */
    static final double GLASS_BROKEN = 0.50;

    private FurnitureWear() {}

    /** @return the share of plain furniture gone in a room of this state */
    public static double lostShare(RoomState state) {
        return NORMAL_LOST[state.ordinal()];
    }

    /** @return whether a room of this state burns its lamps, so crops grow and furnaces are lit */
    public static boolean lit(RoomState state) {
        return state == RoomState.STAFFED || state == RoomState.IN_SERVICE;
    }

    /**
     * The look of one section's furniture.
     *
     * @param plan      the plan (for the section's doors)
     * @param section   the section id
     * @param kind      the room's kind, or null for the hub and the halls
     * @param marks     the section's marks
     * @param bounds    its box
     * @param state     its state
     * @param collapsed whether it caved in: every piece is crushed to rubble
     * @param seed      the plan seed
     * @param ceiling   the complex's ceiling (metal props)
     * @param out       where the placements go
     */
    public static void room(ComplexPlan plan, int section, RoomKind kind, List<Mark> marks, Box bounds, RoomState state,
        boolean collapsed, long seed, int ceiling, List<Placement> out) {
        Map<Long, Integer> top = new HashMap<Long, Integer>();
        for (Mark m : marks) {
            if (m.kind() != MarkKind.FURNITURE) continue;
            top.merge(FurniturePiece.groupOf(m), m.y(), Math::max);
        }
        List<Placement> mine = new ArrayList<Placement>();
        for (Mark m : marks) {
            if (m.kind() != MarkKind.FURNITURE) continue;
            piece(m, kind, state, collapsed, seed, ceiling, top, mine);
        }
        // The dining hall's Vending Machine (D355): its controller by the room's state.
        VendingEgg.room(marks, state, collapsed, mine);
        out.addAll(mine);
        if (state == RoomState.GUTTED && !collapsed && kind != null) {
            piles(plan, section, marks, bounds, seed, mine, out);
        }
    }

    private static void piece(Mark m, RoomKind kind, RoomState state, boolean collapsed, long seed, int ceiling,
        Map<Long, Integer> top, List<Placement> out) {
        FurniturePiece piece = FurniturePiece.of(m.key());
        if ("BASIN".equals(m.key()) || piece == FurniturePiece.PARTITION) return; // part of the room, not furniture
        FurniturePiece.Wear wear = piece == null ? FurniturePiece.Wear.NORMAL : piece.wear();
        long group = FurniturePiece.groupOf(m);
        double u = Mix.unit(Mix.of(seed, S_WEAR, group, 0));
        if (collapsed) {
            // A cave-in crushes the room: every standing piece is rubble; floor inlays lie under it.
            if (piece == null || !piece.inlay()) {
                out.add(at(Kind.RUBBLE, m));
            }
            return;
        }
        switch (wear) {
            case KEEP:
                if (piece == FurniturePiece.WATER_INLAY && state == RoomState.GUTTED
                    && kind == RoomKind.UNDERGROUND_FARM) {
                    out.add(block(m, m.y(), "minecraft:gravel", 0)); // the farm's channels ran dry
                }
                return;
            case SCAVENGED:
                if (u < SCAVENGED_LOST[state.ordinal()]) {
                    out.add(at(Kind.AIR, m));
                } else if (piece == FurniturePiece.METAL_PROP) {
                    MetalProps.Prop p = MetalProps.pick(ceiling, Mix.of(seed, S_PROP, group, m.y()));
                    out.add(
                        Placement.at(Kind.BLOCK, m.x(), m.y(), m.z())
                            .block(p.block, p.meta, p.fallback())
                            .build());
                }
                return;
            case HEAVY:
                if (state == RoomState.GUTTED && Mix.unit(Mix.of(seed, S_STRIP, group, 0)) < STRIPPED) {
                    int t = top.getOrDefault(group, m.y());
                    if (t > 1 && m.y() == t) out.add(at(Kind.AIR, m));
                }
                return;
            case LIVING:
                living(m, state, seed, out);
                return;
            case STATEFUL:
                stateful(m, piece, state, seed, out);
                return;
            default:
                normal(m, piece, kind, state, seed, u, group, top, out);
        }
    }

    private static void normal(Mark m, FurniturePiece piece, RoomKind kind, RoomState state, long seed, double u,
        long group, Map<Long, Integer> top, List<Placement> out) {
        double lost = NORMAL_LOST[state.ordinal()];
        if (piece == FurniturePiece.GLASS && state == RoomState.GUTTED) lost = GLASS_BROKEN;
        if (u < lost) {
            boolean pile = state == RoomState.GUTTED && m.y() == 1
                && Mix.unit(Mix.of(seed, S_RUBBLE, group, 0)) < LOST_RUBBLE;
            out.add(at(pile ? Kind.RUBBLE : Kind.AIR, m));
            return;
        }
        // An overgrown cellar: a huge mushroom cap on a third of the stems.
        if (piece == FurniturePiece.MUSHROOM_STEM && state == RoomState.GUTTED
            && m.y() == top.getOrDefault(group, m.y())
            && Mix.unit(Mix.of(seed, S_CAP, group, 0)) < 0.35) {
            out.add(block(m, m.y() + 1, "minecraft:red_mushroom_block", 14));
        }
    }

    /** Crops grow only in a lit room; a dark room's fields are bare, with mushrooms, which live in the dark. */
    private static void living(Mark m, RoomState state, long seed, List<Placement> out) {
        long cell = m.x() * 4096L + m.z();
        double u = Mix.unit(Mix.of(seed, S_LIVING, cell, 0));
        double v = Mix.unit(Mix.of(seed, S_LIVING, cell, 1));
        int crop = Math.max(0, FurniturePiece.variantOf(m.key())) % CROPS.length;
        switch (state) {
            case STAFFED:
                // A worked farm is sown in turns: half the rows ripe, the rest at every stage (owner, alpha 8).
                out.add(block(m, m.y() + 1, CROPS[crop], v < 0.5 ? 7 : 1 + (int) (u * 7)));
                return;
            case IN_SERVICE:
                if (u >= 0.10) out.add(block(m, m.y() + 1, CROPS[crop], 2 + (int) (v * 6)));
                return;
            case MOTHBALLED:
                if (u < 0.35) {
                    out.add(block(m, m.y(), "minecraft:dirt", 1));
                    if (v < 0.30) out.add(block(m, m.y() + 1, "minecraft:brown_mushroom", 0));
                }
                return;
            default:
                out.add(block(m, m.y(), "minecraft:dirt", u < 0.6 ? 1 : 0));
                if (v < 0.15) out.add(block(m, m.y() + 1, "minecraft:brown_mushroom", 0));
                else if (v < 0.20) out.add(block(m, m.y() + 1, "minecraft:web", 0));
        }
    }

    /** The crops of a farm strip, by the piece's variant. */
    static final String[] CROPS = { "minecraft:wheat", "minecraft:carrots", "minecraft:potatoes" };

    private static void stateful(Mark m, FurniturePiece piece, RoomState state, long seed, List<Placement> out) {
        if (piece == FurniturePiece.FURNACE) {
            if (lit(state)) out.add(block(m, m.y(), "minecraft:lit_furnace", furnaceMeta(m.facing())));
        } else if (piece == FurniturePiece.CAULDRON) {
            // Every cauldron holds water (owner, alpha 8): full in a lit room, lower the longer the room stood shut.
            out.add(block(m, m.y(), "minecraft:cauldron", cauldronLevel(state)));
        } else if (piece == FurniturePiece.ANVIL) {
            int more = state == RoomState.MOTHBALLED ? 1 : state == RoomState.GUTTED ? 2 : 0;
            if (more > 0) {
                int dmg = Math.min(2, Math.max(0, FurniturePiece.variantOf(m.key())) + more);
                out.add(block(m, m.y(), "minecraft:anvil", anvilMeta(m.facing()) | dmg << 2));
            }
        } else if (piece == FurniturePiece.IRON_DOOR) {
            // A shut room's doors stand open: half of them when mothballed, all when gutted.
            boolean open = state == RoomState.GUTTED
                || state == RoomState.MOTHBALLED && Mix.unit(Mix.of(seed, S_WEAR, m.x() * 4096L + m.z(), 1)) < 0.5;
            if (open) out.add(block(m, m.y(), "minecraft:iron_door", doorMeta(m.facing()) | 4));
        }
    }

    /** @return a cauldron's water level (1 to 3) in a room of this state: 3 lit, 2 mothballed, 1 gutted (D323) */
    public static int cauldronLevel(RoomState state) {
        return lit(state) ? 3 : state == RoomState.MOTHBALLED ? 2 : 1;
    }

    /** @return a furnace's meta (2 north, 3 south, 4 west, 5 east) for a hanging direction (0 S, 1 W, 2 N, 3 E) */
    static int furnaceMeta(int facing) {
        switch (facing & 3) {
            case 0:
                return 3;
            case 1:
                return 4;
            case 2:
                return 2;
            default:
                return 5;
        }
    }

    /** @return an anvil's direction bits for a facing: its long side across the facing */
    static int anvilMeta(int facing) {
        return (facing + 1) & 3;
    }

    /**
     * @return the lower half's direction bits of a vanilla door whose front faces {@code facing} (0 S, 1 W, 2 N, 3 E):
     *         the vanilla door rule, where 0 is a door facing east, 1 south, 2 west, 3 north
     */
    static int doorMeta(int facing) {
        switch (facing & 3) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                return 0;
        }
    }

    // --- rubble piles ------------------------------------------------------------------------------------------------

    /**
     * One rubble pile, 1 to 3 cells and 1 or 2 high, per {@link #PILE_CELLS} floor cells of a gutted room, on hashed
     * free floor cells away from doors, lamps and floor damage. Each pile cell stays only if it cannot cut any cell,
     * chest, machine or door off ({@link RoomGrid#tryBlock}).
     */
    private static void piles(ComplexPlan plan, int section, List<Mark> marks, Box b, long seed, List<Placement> worn,
        List<Placement> out) {
        RoomGrid g = RoomGrid.of(plan, section, marks, b, worn);
        if (g == null) return;
        int floor = g.openCells();
        int want = floor / PILE_CELLS;
        if (want == 0) return;
        List<int[]> cand = g.pileCandidates();
        // A hash order, so the piles are the same in every window and do not bunch.
        final long s = seed;
        cand.sort(
            (p, q) -> Long.compare(
                Mix.of(s, S_PILE, p[0] * 4096L + p[1], 0) >>> 1,
                Mix.of(s, S_PILE, q[0] * 4096L + q[1], 0) >>> 1));
        int made = 0;
        for (int[] c : cand) {
            if (made >= want) break;
            if (!g.free(c[0], c[1])) continue;
            long h = Mix.of(seed, S_PILE, c[0] * 4096L + c[1], 1);
            int cells = 1 + (int) Long.remainderUnsigned(h, 3);
            int high = 1 + (int) (Long.remainderUnsigned(h >>> 8, 2));
            int laid = 0;
            int x = c[0], z = c[1];
            for (int i = 0; i < cells; i++) {
                if (g.free(x, z) && g.tryBlock(x, z)) {
                    for (int y = 1; y <= high; y++) {
                        out.add(
                            Placement.at(Kind.RUBBLE, x, y, z)
                                .build());
                    }
                    laid++;
                }
                // The next cell of the pile: a hashed neighbor.
                int d = (int) Long.remainderUnsigned(h >>> (16 + 2 * i), 4);
                x += d == 0 ? 1 : d == 1 ? -1 : 0;
                z += d == 2 ? 1 : d == 3 ? -1 : 0;
            }
            if (laid > 0) made++;
        }
    }

    private static Placement at(Kind k, Mark m) {
        return Placement.at(k, m.x(), m.y(), m.z())
            .build();
    }

    private static Placement block(Mark m, int y, String block, int meta) {
        return Placement.at(Kind.BLOCK, m.x(), y, m.z())
            .block(block, meta, null)
            .build();
    }

    /** A section's floor as walkable or blocked cells, for the rubble piles' reach check. */
    static final class RoomGrid {

        final int x0, z0, w, d;
        final boolean[] blocked;
        final boolean[] keepClear;
        final List<int[]> starts = new ArrayList<int[]>();
        final List<int[]> targets = new ArrayList<int[]>();

        private RoomGrid(Box b) {
            x0 = b.minX + 1;
            z0 = b.minZ + 1;
            w = b.sizeX() - 2;
            d = b.sizeZ() - 2;
            blocked = new boolean[w * d];
            keepClear = new boolean[w * d];
        }

        static RoomGrid of(ComplexPlan plan, int section, List<Mark> marks, Box b, List<Placement> worn) {
            if (b.sizeX() < 5 || b.sizeZ() < 5) return null;
            RoomGrid g = new RoomGrid(b);
            Map<Long, Boolean> gone = new HashMap<Long, Boolean>();
            for (Placement p : worn) {
                if (p.kind() == Kind.AIR && p.y() == 1) gone.put(p.x() * 4096L + p.z(), true);
            }
            for (Mark m : marks) {
                if (!g.in(m.x(), m.z())) continue;
                switch (m.kind()) {
                    case LOOT_CHEST:
                    case MACHINE:
                    case SPAWNER:
                    case GUN_DISPLAY:
                        g.set(m.x(), m.z());
                        g.targets.add(new int[] { m.x(), m.z() });
                        break;
                    case FURNITURE: {
                        FurniturePiece piece = FurniturePiece.of(m.key());
                        boolean walk = piece != null && (piece.walkable() || piece.inlay());
                        if (!walk && m.y() >= 1 && !gone.containsKey(m.x() * 4096L + m.z())) g.set(m.x(), m.z());
                        // A sunk pool (D321) is no floor: no one walks it and no pile falls in it.
                        if (piece == FurniturePiece.POOL_WATER && m.y() == 0) g.set(m.x(), m.z());
                        if (piece != null && piece.inlay() && !piece.walkable()) g.clear(m.x(), m.z());
                        break;
                    }
                    case LIGHT:
                    case DAMAGE_CELL:
                    case RUBBLE:
                    case FLOOD_CELL:
                        if (m.y() <= 1) g.clear(m.x(), m.z());
                        break;
                    default:
                        break;
                }
            }
            for (Door door : plan.doors()) {
                if (!door.joins(section) || door.kind == Door.Kind.WINDOW) continue;
                for (int x = door.x0 - 3; x <= door.x1 + 3; x++) {
                    for (int z = door.z0 - 3; z <= door.z1 + 3; z++) {
                        if (!g.in(x, z)) continue;
                        g.clear(x, z);
                        boolean touches = x >= door.x0 - 1 && x <= door.x1 + 1
                            && z >= door.z0 - 1
                            && z <= door.z1 + 1
                            && (x >= door.x0 && x <= door.x1 || z >= door.z0 && z <= door.z1);
                        if (touches && !g.blocked[g.i(x, z)]) g.starts.add(new int[] { x, z });
                    }
                }
            }
            return g.starts.isEmpty() ? null : g;
        }

        /**
         * Applies the rest of a room's look to the grid: rubble at foot height blocks its cell, and a piece removed
         * at foot height frees it.
         */
        void after(List<Placement> ps) {
            for (Placement p : ps) {
                if (p.y() != 1 || !in(p.x(), p.z())) continue;
                if (p.kind() == Kind.RUBBLE) blocked[i(p.x(), p.z())] = true;
            }
        }

        /** @return the targets (chests, machines, spawners) with no reached cell beside them */
        List<int[]> cutOff(boolean[] seen) {
            List<int[]> out = new ArrayList<int[]>();
            for (int[] t : targets) if (!touches(seen, t)) out.add(t);
            return out;
        }

        /** @return the share of the floor that is blocked (pieces, marks, rubble) */
        double fill() {
            int n = 0;
            for (boolean b : blocked) if (b) n++;
            return blocked.length == 0 ? 0 : n / (double) blocked.length;
        }

        boolean in(int x, int z) {
            return x >= x0 && z >= z0 && x < x0 + w && z < z0 + d;
        }

        int i(int x, int z) {
            return (x - x0) * d + z - z0;
        }

        void set(int x, int z) {
            blocked[i(x, z)] = true;
        }

        void clear(int x, int z) {
            keepClear[i(x, z)] = true;
        }

        boolean free(int x, int z) {
            return in(x, z) && !blocked[i(x, z)] && !keepClear[i(x, z)];
        }

        int openCells() {
            int n = 0;
            for (boolean b : blocked) if (!b) n++;
            return n;
        }

        List<int[]> pileCandidates() {
            List<int[]> out = new ArrayList<int[]>();
            for (int x = x0; x < x0 + w; x++)
                for (int z = z0; z < z0 + d; z++) if (free(x, z)) out.add(new int[] { x, z });
            return out;
        }

        /** @return the open cells a walk from the doors reaches */
        boolean[] reach() {
            boolean[] seen = new boolean[w * d];
            ArrayDeque<int[]> q = new ArrayDeque<int[]>();
            for (int[] s : starts) {
                if (blocked[i(s[0], s[1])] || seen[i(s[0], s[1])]) continue;
                seen[i(s[0], s[1])] = true;
                q.add(s);
            }
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int k = 0; k < 4; k++) {
                    int x = c[0] + DX[k], z = c[1] + DZ[k];
                    if (!in(x, z) || blocked[i(x, z)] || seen[i(x, z)]) continue;
                    seen[i(x, z)] = true;
                    q.add(new int[] { x, z });
                }
            }
            return seen;
        }

        /** The ring of eight cells around a cell, in walking order (each one step from the next). */
        private static final int[] RX = { 0, 1, 1, 1, 0, -1, -1, -1 }, RZ = { -1, -1, 0, 1, 1, 1, 0, -1 };

        private boolean open(int x, int z) {
            return in(x, z) && !blocked[i(x, z)];
        }

        /**
         * Blocks a cell when that cannot cut anything off, judged on its 3 x 3 neighborhood alone: the open cells
         * beside it must stay joined around it (so every walk through it has a way round), and every chest, machine or
         * guard beside it must keep another open neighbor. Conservative: a cell that might matter is left open.
         */
        boolean tryBlock(int x, int z) {
            // Runs of open cells around the ring that hold a cell straight beside this one.
            int runs = 0;
            for (int k = 0; k < 8; k++) {
                boolean here = open(x + RX[k], z + RZ[k]);
                boolean prev = open(x + RX[(k + 7) & 7], z + RZ[(k + 7) & 7]);
                if (here && !prev) {
                    // A run starts at k; does it hold an orthogonal neighbor?
                    boolean side = false;
                    for (int j = k; open(x + RX[j & 7], z + RZ[j & 7]) && j < k + 8; j++) side |= (j & 1) == 0;
                    if (side) runs++;
                }
            }
            boolean allOpen = true;
            for (int k = 0; k < 8; k++) allOpen &= open(x + RX[k], z + RZ[k]);
            if (runs > 1 || runs == 0 && !allOpen && hasOpenSide(x, z)) return false;
            boolean besideTarget = false;
            for (int[] t : targets) besideTarget |= Math.abs(t[0] - x) + Math.abs(t[1] - z) == 1;
            if (besideTarget) {
                // Beside a chest, machine or guard: check the walk itself, so its last way in is never closed.
                boolean[] before = reach();
                blocked[i(x, z)] = true;
                boolean[] now = reach();
                for (int[] t : targets) {
                    if (touches(before, t) && !touches(now, t)) {
                        blocked[i(x, z)] = false;
                        return false;
                    }
                }
                return true;
            }
            blocked[i(x, z)] = true;
            return true;
        }

        private boolean hasOpenSide(int x, int z) {
            for (int k = 0; k < 4; k++) if (open(x + DX[k], z + DZ[k])) return true;
            return false;
        }

        private boolean touches(boolean[] seen, int[] c) {
            for (int k = 0; k < 4; k++) {
                int x = c[0] + DX[k], z = c[1] + DZ[k];
                if (in(x, z) && seen[i(x, z)]) return true;
            }
            return false;
        }

        private static final int[] DX = { 1, -1, 0, 0 };
        private static final int[] DZ = { 0, 0, 1, -1 };
    }
}
