package com.gregguns.icbm.complex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Puts the marks into a grown plan: the lamp grid of every section (D230, D231), each room's furniture and contents in
 * the room's own door-relative frame (D208, D234), guard posts at branch points (D233), the deep posts of the silo
 * gallery, signs and damage cells. Counts of chests, machines and guards scale with a room's floor area. Pure and
 * deterministic; package-private, run by {@link ComplexLayout#fromSeed}.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Lighting, Furnishing)
final class ComplexFurnisher {

    private static final int S = ComplexLayout.SIZE;

    /** Floor cells a room's furnishing can use, in its frame. */
    private static final byte FREE = 0, DOORWAY = 1, LIGHT_TILE = 2, SOLID = 3, FURN = 4, KEEP = 5;

    private final ComplexPlan plan;
    private final Random rnd;
    /** Columns that are a doorway, or one or two cells inside one, in any section (D208). */
    private final boolean[] doorZone = new boolean[S * S];
    /** Columns with a light tile in the floor. */
    private final boolean[] floorLamp = new boolean[S * S];
    /** Columns with a lamp in the ceiling. */
    private final boolean[] ceilLamp = new boolean[S * S];

    private ComplexFurnisher(ComplexPlan plan, Random rnd) {
        this.plan = plan;
        this.rnd = rnd;
    }

    static void furnish(ComplexPlan plan, List<int[]> branchPoints, Random rnd) {
        ComplexFurnisher f = new ComplexFurnisher(plan, rnd);
        f.zones();
        f.lamps();
        f.hub();
        for (Room r : plan.rooms()) f.room(r);
        f.halls(branchPoints);
    }

    // --- door zones ----------------------------------------------------------------------------------------------

    private void zones() {
        for (Door d : plan.doors()) {
            if (d.kind == Door.Kind.WINDOW) continue;
            int reach = 2;
            if (d.inXWall()) {
                for (int x = d.x0 - 1; x <= d.x1 + 1; x++) {
                    for (int z = d.z0 - reach; z <= d.z1 + reach; z++) zone(x, z);
                }
            } else {
                for (int z = d.z0 - 1; z <= d.z1 + 1; z++) {
                    for (int x = d.x0 - reach; x <= d.x1 + reach; x++) zone(x, z);
                }
            }
        }
        // The escape column and the cell in front of it.
        int ex = plan.escapeX(), ez = plan.escapeZ();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) zone(ex + dx, ez + dz);
    }

    private void zone(int x, int z) {
        if (x >= 0 && z >= 0 && x < S && z < S) doorZone[x * S + z] = true;
    }

    private boolean inZone(int x, int z) {
        return x < 0 || z < 0 || x >= S || z >= S || doorZone[x * S + z];
    }

    // --- lamps ---------------------------------------------------------------------------------------------------

    /**
     * Grid positions over {@code n} interior cells, at most {@code gap} apart and at most {@code gap / 2} from either
     * end, spread evenly so the grid sits symmetric in the room.
     */
    static int[] grid(int n, int gap) {
        int c = Math.max(1, (n + gap - 1) / gap);
        int[] out = new int[c];
        for (int i = 0; i < c; i++) out[i] = (2 * i + 1) * n / (2 * c);
        return out;
    }

    /** Longest gap between floor light tiles (each floor cell then lies within 3 + 3 cells of one). */
    static final int TILE_GAP = 6;
    /** Gap of the light tiles in rooms full of furniture rows, whose shadows a light has to reach around. */
    static final int DENSE_GAP = 4;

    /** @return true for the kinds furnished with rows of tables, shelves or racks */
    static boolean rowsOfFurniture(RoomKind k) {
        return k == RoomKind.CAFETERIA || k == RoomKind.MESS_GALLEY
            || k == RoomKind.SERVER_ROOM
            || k == RoomKind.STOCKPILE;
    }

    /** Gap of the ceiling-only lamp rows of low sections (a lamp five cells over the floor reaches two cells). */
    static final int LOW_GAP = 3;

    private void lamps() {
        Box hub = plan.hall()
            .bounds();
        lampSection(
            plan.hall()
                .id(),
            hub,
            hub.sizeX() >= hub.sizeZ(),
            TILE_GAP);
        for (Room r : plan.rooms()) {
            int ds = r.doorSide();
            // A room's runs follow its door wall; rows of furniture cast shadows, so those rooms get a closer grid.
            lampSection(r.id(), r.bounds(), ds == 0 || ds == 2, rowsOfFurniture(r.kind()) ? DENSE_GAP : TILE_GAP);
        }
        for (Tunnel t : plan.tunnels()) lampSection(t.id(), t.bounds(), t.alongX(), TILE_GAP);
    }

    /**
     * The lamp grid of one section: lamps flush in the ceiling layer on grid lines aligned to the section, in runs
     * along {@code runsAlongX}. A section over seven high also gets a light tile in the floor under every lamp, since a
     * ceiling lamp eight or more cells up cannot light the floor to 8 (D231).
     */
    private void lampSection(int id, Box b, boolean runsAlongX, int tallGap) {
        int nx = b.sizeX() - 2, nz = b.sizeZ() - 2, h = b.sizeY();
        boolean tall = h > ComplexLayout.MINOR_HEIGHT;
        int gap = tall ? tallGap : LOW_GAP;
        int[] gx = grid(nx, gap), gz = grid(nz, gap);
        Object owner = plan.section(id);
        for (int i = 0; i < gx.length; i++) {
            for (int j = 0; j < gz.length; j++) {
                int x = b.minX + 1 + gx[i], z = b.minZ + 1 + gz[j];
                if (x == plan.escapeX() && z == plan.escapeZ()) continue;
                int run = runsAlongX ? j : i;
                String key = "lamp:" + id + ":" + run;
                add(owner, MarkKind.LIGHT, x, h - 1, z, key);
                ceilLamp[x * S + z] = true;
                if (tall) {
                    add(owner, MarkKind.LIGHT, x, 0, z, key);
                    floorLamp[x * S + z] = true;
                }
            }
        }
    }

    private static void add(Object owner, MarkKind k, int x, int y, int z, String key) {
        if (owner instanceof Room) ((Room) owner).add(k, x, y, z, key);
        else((Tunnel) owner).add(k, x, y, z, key);
    }

    private static void add(Object owner, MarkKind k, int x, int y, int z, String key, int facing, int extent) {
        if (owner instanceof Room) ((Room) owner).add(k, x, y, z, key, facing, extent);
        else((Tunnel) owner).add(k, x, y, z, key, facing, extent);
    }

    // --- damage --------------------------------------------------------------------------------------------------

    /** Damage cells scale with the area: floor and roof cells off the lamps, wall cells off every opening. */
    private void damage(Object owner, Box b, int id) {
        int area = b.sizeX() * b.sizeZ();
        int n = Math.max(3, area / 40);
        int top = b.sizeY() - 1;
        for (int i = 0; i < n; i++) {
            int pick = rnd.nextInt(3);
            if (pick == 2) {
                wallDamage(owner, b, id);
                continue;
            }
            int x = b.minX + 1 + rnd.nextInt(b.sizeX() - 2), z = b.minZ + 1 + rnd.nextInt(b.sizeZ() - 2);
            if (pick == 0 ? floorLamp[x * S + z] : ceilLamp[x * S + z]) continue;
            add(owner, MarkKind.DAMAGE_CELL, x, pick == 0 ? 0 : top, z, pick == 0 ? "floor" : "roof");
        }
    }

    private void wallDamage(Object owner, Box b, int id) {
        int top = b.sizeY() - 1;
        for (int tries = 0; tries < 8; tries++) {
            int y = 1 + rnd.nextInt(top - 1);
            int x, z;
            if (rnd.nextBoolean()) {
                x = rnd.nextBoolean() ? b.minX : b.maxX - 1;
                z = b.minZ + 1 + rnd.nextInt(b.sizeZ() - 2);
            } else {
                z = rnd.nextBoolean() ? b.minZ : b.maxZ - 1;
                x = b.minX + 1 + rnd.nextInt(b.sizeX() - 2);
            }
            if (nearOpening(x, z, id)) continue;
            add(owner, MarkKind.DAMAGE_CELL, x, y, z, "wall");
            return;
        }
    }

    /** True when a wall cell of a section is within two cells of any opening in that section's walls. */
    private boolean nearOpening(int x, int z, int id) {
        for (Door d : plan.doors()) {
            if (!d.joins(id) && !(d.a == id)) continue;
            if (x >= d.x0 - 2 && x <= d.x1 + 2 && z >= d.z0 - 2 && z <= d.z1 + 2) return true;
        }
        return false;
    }

    // --- the hub -------------------------------------------------------------------------------------------------

    private void hub() {
        Room r = plan.hall();
        Box b = r.bounds();
        damage(r, b, r.id());
        // The sign: on a wall beside the first portal, facing in.
        for (Door d : plan.doors()) {
            if (d.a != r.id() || d.kind != Door.Kind.PORTAL) continue;
            int[] w = besideOpening(b, d, d.side);
            if (w != null) {
                r.add(MarkKind.SIGN, w[0], 3, w[1], "icbm.complex.sign.assembly_bay", inward(d.side), 1);
                break;
            }
        }
        // Salvage chests spread along the walls, a machine in the middle, guards toward the corners.
        List<int[]> ring = ring(b);
        int chests = 4 + b.sizeX() * b.sizeZ() / 700;
        spread(r, ring, chests, MarkKind.LOOT_CHEST, LootKeys.SALVAGE);
        solid(r, b.centerX(), b.centerZ(), MarkKind.MACHINE, MachineRole.WORKBENCH.name());
        int[][] corners = { { b.minX + 4, b.minZ + 4 }, { b.maxX - 5, b.maxZ - 5 }, { b.minX + 4, b.maxZ - 5 },
            { b.maxX - 5, b.minZ + 4 } };
        for (int i = 0; i < 3; i++) solid(r, corners[i][0], corners[i][1], MarkKind.SPAWNER, "room");
    }

    /** The interior cells against the walls of a box, clockwise from the north-west corner. */
    private static List<int[]> ring(Box b) {
        List<int[]> out = new ArrayList<int[]>();
        int x0 = b.minX + 1, x1 = b.maxX - 2, z0 = b.minZ + 1, z1 = b.maxZ - 2;
        for (int x = x0; x < x1; x++) out.add(new int[] { x, z0 });
        for (int z = z0; z < z1; z++) out.add(new int[] { x1, z });
        for (int x = x1; x > x0; x--) out.add(new int[] { x, z1 });
        for (int z = z1; z > z0; z--) out.add(new int[] { x0, z });
        return out;
    }

    private final boolean[] used = new boolean[S * S];

    private boolean usable(int x, int z) {
        return !inZone(x, z) && !floorLamp[x * S + z] && !used[x * S + z];
    }

    /** Puts {@code n} marks evenly along a list of candidate cells, each on the nearest usable cell. */
    private void spread(Object owner, List<int[]> cells, int n, MarkKind k, String key) {
        for (int i = 0; i < n; i++) {
            int at = (int) ((i + 0.5) * cells.size() / n);
            for (int off = 0; off < cells.size(); off++) {
                int[] c = cells.get((at + off) % cells.size());
                if (!usable(c[0], c[1])) continue;
                used[c[0] * S + c[1]] = true;
                add(owner, k, c[0], 1, c[1], key);
                break;
            }
        }
    }

    /** A solid mark on the nearest usable interior cell of its section to (x, z) within four cells. */
    private void solid(Object owner, int x, int z, MarkKind k, String key) {
        Box b = owner instanceof Room ? ((Room) owner).bounds() : ((Tunnel) owner).bounds();
        for (int r = 0; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = x + dx, cz = z + dz;
                    if (cx <= b.minX || cz <= b.minZ || cx >= b.maxX - 1 || cz >= b.maxZ - 1) continue;
                    if (!usable(cx, cz)) continue;
                    used[(x + dx) * S + z + dz] = true;
                    add(owner, k, x + dx, 1, z + dz, key);
                    return;
                }
            }
        }
    }

    /** The wall cell of {@code b} two cells past an opening's edge on the side it is cut in, or null. */
    private static int[] besideOpening(Box b, Door d, int side) {
        switch (side) {
            case 0:
                return d.x1 + 2 <= b.maxX - 2 ? new int[] { d.x1 + 2, b.minZ } : null;
            case 2:
                return d.x1 + 2 <= b.maxX - 2 ? new int[] { d.x1 + 2, b.maxZ - 1 } : null;
            case 1:
                return d.z1 + 2 <= b.maxZ - 2 ? new int[] { b.maxX - 1, d.z1 + 2 } : null;
            default:
                return d.z1 + 2 <= b.maxZ - 2 ? new int[] { b.minX, d.z1 + 2 } : null;
        }
    }

    /** Hanging direction (0 south, 1 west, 2 north, 3 east) of something on a box's {@code side} wall, facing in. */
    static int inward(int side) {
        switch (side) {
            case 0:
                return 0;
            case 2:
                return 2;
            case 1:
                return 1;
            default:
                return 3;
        }
    }

    // --- halls ---------------------------------------------------------------------------------------------------

    private void halls(List<int[]> branchPoints) {
        for (Tunnel t : plan.tunnels()) {
            Box b = t.bounds();
            damage(t, b, t.id());
            if (t.major() && t.parent() == plan.hall()
                .id()) {
                // The mouth: a blast door mark and a sign naming the hall, by its compass direction off the hub.
                Box hub = plan.hall()
                    .bounds();
                int dir = b.maxZ <= hub.minZ ? 0 : b.minX >= hub.maxX ? 1 : b.minZ >= hub.maxZ ? 2 : 3;
                int mx = t.alongX() ? (dir == 1 ? b.minX + 1 : b.maxX - 2) : b.centerX();
                int mz = t.alongX() ? b.centerZ() : (dir == 2 ? b.minZ + 1 : b.maxZ - 2);
                t.add(MarkKind.BLAST_DOOR, mx, 1, mz, "mouth");
                int[] w = signBesideMouth(t, b, dir);
                if (w != null) {
                    t.add(
                        MarkKind.SIGN,
                        w[0],
                        3,
                        w[1],
                        t.gallery() ? "icbm.complex.sign.silo_gallery" : "icbm.complex.sign.wing." + dir,
                        w[2],
                        1);
                }
            }
        }
        for (int[] p : branchPoints) {
            Tunnel t = plan.section(p[2]) instanceof Tunnel ? (Tunnel) plan.section(p[2]) : null;
            if (t != null) solid(t, p[0], p[1], MarkKind.SPAWNER, "post");
            else if (p[2] == plan.hall()
                .id()) solid(plan.hall(), p[0], p[1], MarkKind.SPAWNER, "post");
        }
        // D36: guards thicken with depth; each silo's walkway and the gallery's far end hold a "deep" post.
        for (SiloSlot s : plan.silos()) {
            int dx = s.frontX() - s.controllerX(), dz = s.frontZ() - s.controllerZ();
            Tunnel g = galleryAt(s.frontX() + dx, s.frontZ() + dz);
            if (g == null) continue;
            solidAnyway(g, s.frontX() + 3 * dx, s.frontZ() + 3 * dz, "deep");
        }
        for (Tunnel t : plan.tunnels()) {
            if (!t.gallery()) continue;
            Box b = t.bounds();
            Box hub = plan.hall()
                .bounds();
            // The far end is the end away from the hub.
            boolean farHigh = t.alongX() ? b.centerX() > hub.centerX() : b.centerZ() > hub.centerZ();
            int x = t.alongX() ? (farHigh ? b.maxX - 4 : b.minX + 3) : b.minX + 3;
            int z = t.alongX() ? b.minZ + 3 : (farHigh ? b.maxZ - 4 : b.minZ + 3);
            solidAnyway(t, x, z, "deep");
        }
    }

    /** A deep post on the nearest cell that is not a doorway or a light tile, searching wide. */
    private void solidAnyway(Tunnel t, int x, int z, String key) {
        Box b = t.bounds();
        for (int r = 0; r < 12; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int cx = x + dx, cz = z + dz;
                    if (cx <= b.minX || cz <= b.minZ || cx >= b.maxX - 1 || cz >= b.maxZ - 1) continue;
                    if (!usable(cx, cz)) continue;
                    used[cx * S + cz] = true;
                    t.add(MarkKind.SPAWNER, cx, 1, cz, key);
                    return;
                }
            }
        }
    }

    private Tunnel galleryAt(int x, int z) {
        for (Tunnel t : plan.tunnels()) {
            if (t.gallery() && t.bounds()
                .contains(x, 1, z)) return t;
        }
        return null;
    }

    /** A wall cell beside a hall's mouth, on its side wall two cells in, with its facing; or null. */
    private int[] signBesideMouth(Tunnel t, Box b, int dir) {
        // The side wall nearest the hub end, four cells along from the mouth.
        switch (dir) {
            case 0:
                return new int[] { b.minX, b.maxZ - 5, 3 };
            case 2:
                return new int[] { b.minX, b.minZ + 4, 3 };
            case 1:
                return new int[] { b.minX + 4, b.minZ, 0 };
            default:
                return new int[] { b.maxX - 5, b.minZ, 0 };
        }
    }

    // --- rooms ---------------------------------------------------------------------------------------------------

    /** One room's furnishing, in its frame: cells as (lateral a, depth d), d 0 against the back wall. */
    private final class Fit {

        final Room r;
        final ComplexLayout.Frame f;
        final int L, D;
        final byte[] occ;
        /** Furniture height per cell (0 none). */
        final byte[] furnH;
        final byte[] furnKind;

        Fit(Room r) {
            this.r = r;
            this.f = new ComplexLayout.Frame(r);
            this.L = f.lateral;
            this.D = f.depth;
            occ = new byte[L * D];
            furnH = new byte[L * D];
            furnKind = new byte[L * D];
            for (int a = 0; a < L; a++) {
                for (int d = 0; d < D; d++) {
                    int x = f.x(a, d), z = f.z(a, d);
                    occ[a * D + d] = inZone(x, z) ? DOORWAY : floorLamp[x * S + z] ? LIGHT_TILE : FREE;
                }
            }
        }

        boolean free(int a, int d) {
            return a >= 0 && d >= 0 && a < L && d < D && occ[a * D + d] == FREE;
        }

        /** A solid mark (chest, machine, spawner) at (a, d) or the nearest free cell, same row first. */
        boolean mark(MarkKind k, int a, int d, String key) {
            for (int[] o : NEAR) {
                int ca = a + o[0], cd = d + o[1];
                if (!free(ca, cd)) continue;
                occ[ca * D + cd] = SOLID;
                r.add(k, f.x(ca, cd), 1, f.z(ca, cd), key);
                return true;
            }
            return false;
        }

        /** A mark spread {@code n} times along the back wall. */
        void backRow(MarkKind k, int n, String key) {
            for (int i = 0; i < n; i++) wallMark(k, (int) ((i + 0.5) * L / n), key);
        }

        /** A mark against the back wall: the nearest free cell along it, else the nearest free cell anywhere. */
        boolean wallMark(MarkKind k, int a, String key) {
            for (int off = 0; off < L; off++) {
                for (int ca : new int[] { a + off, a - off }) {
                    if (!free(ca, 0)) continue;
                    occ[ca * D] = SOLID;
                    r.add(k, f.x(ca, 0), 1, f.z(ca, 0), key);
                    return true;
                }
            }
            return mark(k, a, 0, key);
        }

        void furn(byte cell, int a, int d, int height) {
            if (!free(a, d)) return;
            occ[a * D + d] = FURN;
            furnH[a * D + d] = (byte) height;
            furnKind[a * D + d] = cell;
        }

        /** A run of furniture across the room at depth {@code d}, with a gap after every {@code run} cells. */
        void row(byte cell, int d, int a0, int a1, int run, int height) {
            for (int a = a0, k = 0; a <= a1; a++, k++) {
                if (k % (run + 1) == run) continue;
                furn(cell, a, d, height);
            }
        }

        /** A run along the depth at lateral {@code a}. */
        void column(byte cell, int a, int d0, int d1, int run, int height) {
            for (int d = d0, k = 0; d <= d1; d++, k++) {
                if (k % (run + 1) == run) continue;
                furn(cell, a, d, height);
            }
        }

        /**
         * Keeps a 5 x 5 block free around (a, d) for the NASA Workbench and its free ring; false if it does not fit.
         */
        boolean nasa(int a, int d) {
            for (int da = -2; da <= 2; da++) for (int dd = -2; dd <= 2; dd++) if (!free(a + da, d + dd)) return false;
            for (int da = -2; da <= 2; da++) for (int dd = -2; dd <= 2; dd++) occ[(a + da) * D + d + dd] = KEEP;
            occ[a * D + d] = SOLID;
            r.add(MarkKind.MACHINE, f.x(a, d), 1, f.z(a, d), MachineRole.NASA_WORKBENCH.name());
            return true;
        }

        /**
         * Clears furniture that walls a chest, machine or guard off from the door: from every solid mark no walk
         * reaches, the shortest way through furniture to a reached cell is cleared.
         */
        void keepReachable() {
            boolean[] seen = flood();
            for (int a = 0; a < L; a++) {
                for (int d = 0; d < D; d++) {
                    if (occ[a * D + d] != SOLID || touches(seen, a, d)) continue;
                    clearPath(seen, a, d);
                    seen = flood();
                }
            }
        }

        private boolean walk(int a, int d) {
            byte o = occ[a * D + d];
            return o == FREE || o == DOORWAY || o == LIGHT_TILE || o == KEEP;
        }

        private boolean[] flood() {
            boolean[] seen = new boolean[L * D];
            ArrayDeque<int[]> q = new ArrayDeque<int[]>();
            for (int a = Math.max(0, f.doorLo); a <= Math.min(L - 1, f.doorHi); a++) {
                if (walk(a, D - 1)) {
                    seen[a * D + D - 1] = true;
                    q.add(new int[] { a, D - 1 });
                }
            }
            int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] n : nb) {
                    int a = c[0] + n[0], d = c[1] + n[1];
                    if (a < 0 || d < 0 || a >= L || d >= D || seen[a * D + d] || !walk(a, d)) continue;
                    seen[a * D + d] = true;
                    q.add(new int[] { a, d });
                }
            }
            return seen;
        }

        private boolean touches(boolean[] seen, int a, int d) {
            int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] n : nb) {
                int ca = a + n[0], cd = d + n[1];
                if (ca >= 0 && cd >= 0 && ca < L && cd < D && seen[ca * D + cd]) return true;
            }
            return false;
        }

        private void clearPath(boolean[] seen, int a0, int d0) {
            int[] prev = new int[L * D];
            java.util.Arrays.fill(prev, -2);
            ArrayDeque<int[]> q = new ArrayDeque<int[]>();
            prev[a0 * D + d0] = -1;
            q.add(new int[] { a0, d0 });
            int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] n : nb) {
                    int a = c[0] + n[0], d = c[1] + n[1];
                    if (a < 0 || d < 0 || a >= L || d >= D || prev[a * D + d] != -2) continue;
                    byte o = occ[a * D + d];
                    if (o == SOLID) continue;
                    prev[a * D + d] = c[0] * D + c[1];
                    if (seen[a * D + d]) {
                        // Walk back, clearing furniture.
                        int at = prev[a * D + d];
                        while (at >= 0 && at != a0 * D + d0) {
                            if (occ[at] == FURN) {
                                occ[at] = FREE;
                                furnH[at] = 0;
                            }
                            at = prev[at];
                        }
                        return;
                    }
                    q.add(new int[] { a, d });
                }
            }
        }

        /** Writes the furniture that survived as FURNITURE marks, one per layer. */
        void commit() {
            for (int a = 0; a < L; a++) {
                for (int d = 0; d < D; d++) {
                    int i = a * D + d;
                    if (occ[i] != FURN) continue;
                    String key = furnName(furnKind[i]);
                    for (int y = 1; y <= furnH[i]; y++) r.add(MarkKind.FURNITURE, f.x(a, d), y, f.z(a, d), key);
                }
            }
        }
    }

    private static String furnName(byte cell) {
        switch (cell) {
            case Cell.FURN_TABLE:
                return "TABLE";
            case Cell.FURN_CONSOLE:
                return "CONSOLE";
            case Cell.FURN_BUNK:
                return "BUNK";
            default:
                return "SHELF";
        }
    }

    /** Offsets tried around a wanted cell, nearest first, a step in depth costing two steps along the row. */
    private static final int[][] NEAR = near();

    private static int[][] near() {
        List<int[]> l = new ArrayList<int[]>();
        for (int dd = -3; dd <= 3; dd++) for (int da = -12; da <= 12; da++) l.add(new int[] { da, dd });
        java.util.Collections.sort(l, (p, q) -> {
            int c = (Math.abs(p[0]) + 2 * Math.abs(p[1])) - (Math.abs(q[0]) + 2 * Math.abs(q[1]));
            if (c != 0) return c;
            c = Math.abs(p[1]) - Math.abs(q[1]);
            if (c != 0) return c;
            c = p[1] - q[1];
            return c != 0 ? c : p[0] - q[0];
        });
        return l.toArray(new int[0][]);
    }

    /** Chest, machine and guard counts grow by one step per this many interior cells. */
    static final int AREA_STEP = 450;

    private void room(Room r) {
        Box b = r.bounds();
        damage(r, b, r.id());
        Fit fit = new Fit(r);
        sign(r, fit.f);
        int L = fit.L, D = fit.D, ma = L / 2, md = D / 2, last = L - 1;
        int q = Math.max(1, L * D / AREA_STEP);
        int guards = Math.max(1, q / 2);
        byte T = Cell.FURN_TABLE, C = Cell.FURN_CONSOLE, B = Cell.FURN_BUNK, SH = Cell.FURN_SHELF;
        switch (r.kind()) {
            case LAUNCH_CONTROL:
                fit.mark(MarkKind.MACHINE, ma, md, MachineRole.LAUNCH_CONTROL.name());
                if (D >= 9) fit.row(C, D - 4, 2, last - 2, 4, 1);
                fit.mark(MarkKind.LOOT_CHEST, 0, 0, LootKeys.CONTROL);
                if (q > 2) fit.mark(MarkKind.LOOT_CHEST, last, 0, LootKeys.CONTROL);
                spawners(fit, guards, "room");
                break;
            case RADAR_TRACKING:
                fit.mark(MarkKind.MACHINE, ma, md, MachineRole.RADAR.name());
                fit.column(C, 0, 2, D - 4, 4, 1);
                fit.column(C, last, 2, D - 4, 4, 1);
                fit.backRow(MarkKind.LOOT_CHEST, Math.max(1, q / 2), LootKeys.CONTROL);
                break;
            case COMMS:
                fit.mark(MarkKind.MACHINE, ma, md, MachineRole.COMMS.name());
                fit.column(C, 0, 2, D - 4, 4, 1);
                fit.backRow(MarkKind.LOOT_CHEST, Math.max(1, q / 2), LootKeys.COMMS);
                break;
            case COMMANDER_OFFICE:
                fit.mark(MarkKind.LOOT_CHEST, last, 0, LootKeys.SAFE);
                fit.mark(MarkKind.LOOT_CHEST, 0, 0, LootKeys.STOCKPILE);
                fit.row(T, 3, ma - 2, ma + 2, 5, 1);
                fit.column(SH, 0, 2, D - 4, 3, 2);
                break;
            case WARHEAD_MAGAZINE: {
                int n = 0;
                for (int d = 2; d <= D - 4 && n < 4 + 2 * q; d += 3) {
                    for (int a = 2; a <= last - 2 && n < 4 + 2 * q; a += 3) {
                        if (fit.free(a, d)) {
                            fit.mark(MarkKind.MACHINE, a, d, MachineRole.MAGAZINE_CASING.name());
                            n++;
                        }
                    }
                }
                rubble(fit, Math.max(2, q));
                fit.backRow(MarkKind.LOOT_CHEST, Math.max(1, q / 2), LootKeys.MAGAZINE);
                spawners(fit, guards, "room");
                break;
            }
            case FUEL_DEPOT:
                for (int d = 2, n = 0; d <= D - 4 && n < 2 + q; d += 4, n += 2) {
                    fit.mark(MarkKind.MACHINE, 1, d, MachineRole.FUEL_TANK.name());
                    fit.mark(MarkKind.MACHINE, last - 1, d, MachineRole.FUEL_TANK.name());
                }
                fit.backRow(MarkKind.LOOT_CHEST, Math.max(1, q / 2), LootKeys.FUEL);
                break;
            case GENERATOR_HALL:
                for (int a = 2, n = 0; a <= last - 2 && n < 2 + q; a += 4, n++) {
                    fit.mark(MarkKind.MACHINE, a, md, MachineRole.GENERATOR.name());
                }
                fit.mark(MarkKind.MACHINE, last, 0, MachineRole.BATTERY_BUFFER.name());
                spawners(fit, guards, "room");
                break;
            case AIR_HANDLING:
                for (int i = 0, n = Math.max(1, q / 2); i < n; i++) {
                    fit.mark(MarkKind.MACHINE, (int) ((i + 0.5) * L / n), md, MachineRole.AIR_HANDLER.name());
                }
                fit.row(SH, 0, 2, last - 2, 3, 2);
                break;
            case PUMP_ROOM:
                for (int i = 0, n = Math.max(1, q / 2); i < n; i++) {
                    fit.mark(MarkKind.MACHINE, (int) ((i + 0.5) * L / n), md, MachineRole.PUMP.name());
                }
                for (int a = 0; a <= last; a += 2) {
                    if (fit.free(a, md + 1)) {
                        r.add(MarkKind.FLOOD_CELL, fit.f.x(a, md + 1), 1, fit.f.z(a, md + 1), "flood");
                    }
                }
                break;
            case WORKSHOP:
                fit.nasa(2, 2);
                fit.mark(MarkKind.MACHINE, ma + 2, md, MachineRole.WORKBENCH.name());
                if (q > 2) fit.mark(MarkKind.MACHINE, ma + 6, md, MachineRole.WORKBENCH.name());
                fit.mark(MarkKind.LOOT_CHEST, last, 0, LootKeys.WORKSHOP);
                fit.column(T, last, 2, D - 4, 4, 1);
                break;
            case BUNKS:
                fit.column(B, 0, 2, D - 4, 2, 1);
                fit.column(B, last, 2, D - 4, 2, 1);
                fit.mark(MarkKind.LOOT_CHEST, 0, 0, LootKeys.BUNKS);
                fit.mark(MarkKind.LOOT_CHEST, last, 0, LootKeys.BUNKS);
                for (int i = 2; i < 1 + q; i++) fit.mark(MarkKind.LOOT_CHEST, i * L / (2 + q), 0, LootKeys.BUNKS);
                spawners(fit, guards, "room");
                break;
            case STOCKPILE:
                for (int a = 1; a <= last; a += 4) fit.wallMark(MarkKind.LOOT_CHEST, a, LootKeys.STOCKPILE);
                for (int d = 3; d <= D - 4; d += 3) fit.row(SH, d, 2, last - 2, 5, 2);
                break;
            case MESS_GALLEY:
            case CAFETERIA:
                fit.row(C, 1, 2, last - 2, 6, 1);
                for (int d = 4; d <= D - 4; d += 3) fit.row(T, d, 2, last - 2, 5, 1);
                fit.mark(MarkKind.LOOT_CHEST, 0, 0, LootKeys.GALLEY);
                if (q > 2) fit.mark(MarkKind.LOOT_CHEST, last, 0, LootKeys.GALLEY);
                break;
            case INFIRMARY:
                fit.column(B, 0, 2, D - 4, 2, 1);
                fit.column(B, last, 2, D - 4, 2, 1);
                fit.row(SH, 0, 3, last - 3, 3, 2);
                fit.backRow(MarkKind.LOOT_CHEST, Math.max(1, q / 2), LootKeys.MEDICAL);
                break;
            case ARMORY:
                frames(fit);
                fit.backRow(MarkKind.LOOT_CHEST, Math.max(1, q / 2), LootKeys.ARMORY);
                fit.column(T, 0, 3, D - 4, 4, 1);
                spawners(fit, guards, "room");
                break;
            case GUARD_POST:
                fit.mark(MarkKind.SPAWNER, ma, md, "post");
                break;
            case VEHICLE_BAY:
                fit.mark(MarkKind.MACHINE, 0, 0, MachineRole.BATTERY_BUFFER.name());
                fit.mark(MarkKind.MACHINE, last, 0, MachineRole.BATTERY_BUFFER.name());
                fit.mark(MarkKind.MACHINE, ma, 0, MachineRole.WORKBENCH.name());
                fit.mark(MarkKind.LOOT_CHEST, 1, 0, LootKeys.WORKSHOP);
                fit.column(T, 0, 3, D - 4, 4, 1);
                break;
            case SERVER_ROOM:
                for (int a = 2; a <= last - 2; a += 3) {
                    fit.column(SH, a, 2, md - 1, 6, 2);
                    fit.column(SH, a, md + 1, D - 4, 6, 2);
                }
                fit.mark(MarkKind.MACHINE, 0, 0, MachineRole.COMMS.name());
                fit.mark(MarkKind.LOOT_CHEST, last, 0, LootKeys.COMMS);
                break;
            case MISSILE_ASSEMBLY:
                if (!fit.nasa(ma, md)) fit.nasa(3, 3);
                fit.mark(MarkKind.MACHINE, 2, md, MachineRole.WORKBENCH.name());
                fit.mark(MarkKind.MACHINE, last - 2, md, MachineRole.WORKBENCH.name());
                for (int i = 0; i < 2 + q / 2; i++) {
                    fit.mark(MarkKind.MACHINE, 3 + i * 3, 0, MachineRole.MAGAZINE_CASING.name());
                }
                fit.mark(MarkKind.LOOT_CHEST, 0, 0, LootKeys.SALVAGE);
                fit.mark(MarkKind.LOOT_CHEST, last, 0, LootKeys.SALVAGE);
                fit.column(T, last, 3, D - 4, 4, 1);
                spawners(fit, guards, "room");
                break;
            case WATER_TREATMENT:
                for (int a = 2; a + 2 <= last - 2; a += 6) {
                    for (int d = 2; d <= D - 5; d++) {
                        for (int da = 0; da < 3; da++) {
                            if (fit.free(a + da, d)) {
                                fit.occ[(a + da) * D + d] = KEEP;
                                r.add(MarkKind.FURNITURE, fit.f.x(a + da, d), 0, fit.f.z(a + da, d), "BASIN");
                            }
                        }
                    }
                }
                for (int i = 0, n = Math.max(1, q / 2); i < n; i++) {
                    fit.mark(MarkKind.MACHINE, (int) ((i + 0.5) * L / n), 0, MachineRole.PUMP.name());
                }
                break;
            default:
                break;
        }
        fit.keepReachable();
        fit.commit();
    }

    private void spawners(Fit fit, int n, String key) {
        for (int i = 0; i < n; i++) fit.mark(MarkKind.SPAWNER, (int) ((i + 0.5) * fit.L / n), 2, key);
    }

    private void rubble(Fit fit, int n) {
        for (int i = 0; i < n; i++) {
            int a = rnd.nextInt(fit.L), d = 1 + rnd.nextInt(Math.max(1, fit.D - 3));
            if (fit.free(a, d)) {
                fit.r.add(MarkKind.RUBBLE, fit.f.x(a, d), 1, fit.f.z(a, d), "fallout");
            }
        }
    }

    /**
     * The sign by the door: on the doorway's jamb, facing into the room (a jamb is always solid, where the wall beside
     * it may hold a window).
     */
    private static void sign(Room r, ComplexLayout.Frame f) {
        int a = f.doorHi + 1 <= f.lateral - 1 ? f.doorHi + 1 : f.doorLo - 1;
        r.add(
            MarkKind.SIGN,
            f.wallX(a),
            3,
            f.wallZ(a),
            "icbm.complex.sign." + r.kind()
                .name()
                .toLowerCase(java.util.Locale.ROOT),
            f.doorWallFacing(),
            1);
    }

    /**
     * Two rows of item frames on the back wall (y 2 and 3), one frame every {@link ComplexLayout#FRAME_STRIDE} cells,
     * the upper row starting one cell further along so the guns stagger. A doorway in the back wall splits a row.
     */
    private void frames(Fit fit) {
        ComplexLayout.Frame f = fit.f;
        boolean[] open = new boolean[fit.L];
        for (int a = 0; a < fit.L; a++) {
            int wx = f.backWallX(a), wz = f.backWallZ(a);
            for (Door d : plan.doors()) {
                if (d.kind == Door.Kind.WINDOW) continue;
                if (wx >= d.x0 - 1 && wx <= d.x1 + 1 && wz >= d.z0 - 1 && wz <= d.z1 + 1) open[a] = true;
            }
        }
        int facing = f.backFacing();
        for (int y = 2; y <= 3; y++) {
            int a = 1 + (y - 2);
            while (a < fit.L - 1) {
                if (open[a]) {
                    a++;
                    continue;
                }
                int start = a, count = 0;
                while (a < fit.L - 1 && !open[a]) {
                    if ((a - start) % ComplexLayout.FRAME_STRIDE == 0) count++;
                    a++;
                }
                if (count > 0) {
                    fit.r
                        .add(MarkKind.ITEM_FRAME_WALL, f.x(start, 0), y, f.z(start, 0), LootKeys.ARMORY, facing, count);
                }
            }
        }
    }
}
