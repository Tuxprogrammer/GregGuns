package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Opening;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;
import com.gregguns.icbm.complex.blueprint.RoomBlueprints;
import com.gregguns.icbm.complex.blueprint.VendingSpot;
import com.gregguns.icbm.complex.blueprint.rooms.Hub;

/**
 * Puts the marks into a grown plan: the lamp grid of every section (D230, D231), each room's furniture and contents in
 * the room's own door-relative frame (D208, D234), two spawner spots per room (D326), the Vending Machine (D355), signs
 * and damage cells.
 * Counts of chests and machines scale with a room's floor area. Pure and
 * deterministic; package-private, run by {@link ComplexLayout#fromSeed}.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Lighting, Furnishing)
final class ComplexFurnisher {

    private static final int S = ComplexLayout.SIZE;

    /** Floor cells a room's furnishing can use, in its frame. */
    private static final byte FREE = 0, DOORWAY = 1, LIGHT_TILE = 2, SOLID = 3, FURN = 4, KEEP = 5;

    /** Most spawner spots a room offers, the most any level rolls for it (D326). */
    static final int SPAWNER_SPOTS = 2;

    /** The key of a spawner mark: a bare floor spot the contents pass may fill. */
    static final String SPOT = "spot";

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

    static void furnish(ComplexPlan plan, Random rnd) {
        ComplexFurnisher f = new ComplexFurnisher(plan, rnd);
        f.vendingHalls();
        f.zones();
        f.lamps();
        f.hub();
        for (Room r : plan.rooms()) f.room(r);
        f.halls();
    }

    // --- the Vending Machine (D355) --------------------------------------------------------------------------------

    /** The hall the Vending Machine goes to: the first of {@link VendingSpot#halls}; -1 when there is none. */
    private int vendingHall = -1;
    /** Every room that may take the machine, by id. */
    private final java.util.Set<Integer> vendingCandidates = new java.util.HashSet<Integer>();
    /** The hall had no spot: the next candidate furnished takes the machine. */
    private boolean vendingOpen;
    /** The machine stands. */
    private boolean vendingDone;

    private void vendingHalls() {
        List<Room> halls = VendingSpot.halls(plan.rooms(), plan.seed());
        if (!halls.isEmpty()) vendingHall = halls.get(0)
            .id();
        for (Room r : halls) vendingCandidates.add(r.id());
    }

    /**
     * Stands the complex's one Vending Machine in its hall, after the room's reachability pass (D355); when the hall
     * has no spot, the next candidate room furnished after it takes the machine.
     */
    private void vending(Room r, Kit k) {
        if (vendingDone || r.id() != vendingHall && !(vendingOpen && vendingCandidates.contains(r.id()))) return;
        vendingDone = VendingSpot.place(k) != null;
        if (!vendingDone && r.id() == vendingHall) vendingOpen = true;
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

    /** Chest, machine and guard counts grow by one step per this many interior cells (D234). */
    static final int AREA_STEP = com.gregguns.icbm.complex.blueprint.rooms.LegacyRoom.AREA_STEP;

    /** Longest gap between floor light tiles (each floor cell then lies within 3 + 3 cells of one). */
    static final int TILE_GAP = 6;

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
            // A room's runs follow its door wall; its blueprint sets the gap (D305): rows of furniture cast shadows.
            lampSection(
                r.id(),
                r.bounds(),
                ds == 0 || ds == 2,
                RoomBlueprints.of(r.kind())
                    .lampGap());
        }
        for (Tunnel t : plan.tunnels()) lampSection(t.id(), t.bounds(), t.alongX(), TILE_GAP);
        walkwayLamps();
    }

    /**
     * Run numbers of the walkway lamps start here, clear of any grid run of a section, so each walkway is its own run
     * (an abandoned complex rolls each walkway on its own, as it does each grid run).
     */
    static final int WALKWAY_RUN = 1000;

    /**
     * One lamp over each silo walkway: flush in the gallery wall right above the niche's middle column, so the silo's
     * window and the walkway floor are lit like the rest of the complex. The gallery's own grid lamps sit up to twelve
     * cells up and three or more cells out, which leaves the niche and the window face in the dark. The lamp is an
     * ordinary light mark of the gallery (same palette block, same lit roll per activity level); its key is
     * {@code lamp:<gallery id>:<WALKWAY_RUN + silo index>}.
     */
    private void walkwayLamps() {
        int n = 0;
        for (Door d : plan.doors()) {
            if (d.kind != Door.Kind.WALKWAY) continue;
            int x = (d.x0 + d.x1) / 2, z = (d.z0 + d.z1) / 2;
            add(plan.section(d.a), MarkKind.LIGHT, x, d.y1 + 1, z, "lamp:" + d.a + ":" + (WALKWAY_RUN + n++));
        }
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

    /**
     * The hub (the assembly bay) is furnished by its own blueprint (D306) in a frame turned to its south wall, with a
     * straight lane kept clear across from every portal. Its sign (and the activity sign hung under it) keeps the cell
     * in front of it, and the cells either side, clear.
     */
    private void hub() {
        Room r = plan.hall();
        Box b = r.bounds();
        damage(r, b, r.id());
        ComplexLayout.Frame f = new ComplexLayout.Frame(b, 2);
        Kit k = kit(r, f);
        // The sign: on a wall beside the first portal, facing in.
        for (Door d : plan.doors()) {
            if (d.a != r.id() || d.kind != Door.Kind.PORTAL) continue;
            int[] w = besideOpening(b, d, d.side);
            if (w != null) {
                r.add(MarkKind.SIGN, w[0], 3, w[1], "icbm.complex.sign.assembly_bay", inward(d.side), 1);
                int ix = w[0] + DX[inward(d.side)], iz = w[1] + DZ[inward(d.side)];
                int a = f.a(ix, iz), dd = f.d(ix, iz);
                k.reserve(a - 1, a + 1, dd - 1, dd + 1);
                break;
            }
        }
        lay(r, f, k, new Hub());
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

    /** World offsets of one step in each hanging direction (0 south, 1 west, 2 north, 3 east). */
    static final int[] DX = { 0, -1, 0, 1 }, DZ = { 1, 0, -1, 0 };

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

    private void halls() {
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

    private void room(Room r) {
        Box b = r.bounds();
        damage(r, b, r.id());
        ComplexLayout.Frame f = new ComplexLayout.Frame(r);
        sign(r, f);
        lay(r, f, kit(r, f), RoomBlueprints.of(r.kind()));
        RoomDressing.dress(plan, r); // plates, jars, pots and clipboards on what the blueprint stood (D323)
    }

    /**
     * The kit of one room: its floor in the room's frame (doorway zones, floor light tiles, floor holes) and its
     * openings, the primary one being its doorway to its parent (none for the hub).
     */
    private Kit kit(Room r, ComplexLayout.Frame f) {
        int L = f.lateral, D = f.depth;
        byte[] base = new byte[L * D];
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                int x = f.x(a, d), z = f.z(a, d);
                base[a * D + d] = inZone(x, z) ? Kit.DOORWAY : floorLamp[x * S + z] ? Kit.TILE : Kit.FREE;
            }
        }
        // A floor hole or rubble cell (damage runs first) would leave a piece floating over it: not free, walkable.
        for (Mark m : r.marks()) {
            if (m.kind() != MarkKind.DAMAGE_CELL || m.y() != 0 || !"floor".equals(m.key())) continue;
            int a = f.a(m.x(), m.z()), d = f.d(m.x(), m.z());
            if (a >= 0 && d >= 0 && a < L && d < D && base[a * D + d] == Kit.FREE) base[a * D + d] = Kit.HOLE;
        }
        List<Opening> openings = new ArrayList<Opening>();
        for (Door door : plan.doors()) {
            if (!door.joins(r.id()) || door.kind == Door.Kind.WINDOW || door.kind == Door.Kind.WALKWAY) continue;
            Opening o = opening(r, f, door);
            if (o != null) openings.add(o);
        }
        long seed = plan.seed() * 0x9E3779B97F4A7C15L + r.id() * 0xC2B2AE3D27D4EB4FL;
        return new Kit(L, D, base, openings, seed, woodOf(plan.seed(), r.id()));
    }

    /** @return a door of the room as an opening in its frame, or null when none of its cells lie in the room's wall */
    private static Opening opening(Room r, ComplexLayout.Frame f, Door door) {
        Box b = r.bounds();
        int L = f.lateral, D = f.depth;
        Opening.Wall wall = null;
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int x = door.x0; x <= door.x1; x++) {
            for (int z = door.z0; z <= door.z1; z++) {
                if (!b.contains(x, b.minY, z)) continue;
                int a = f.a(x, z), d = f.d(x, z);
                Opening.Wall w;
                int t;
                if (d < 0) {
                    w = Opening.Wall.BACK;
                    t = a;
                } else if (d >= D) {
                    w = Opening.Wall.DOOR;
                    t = a;
                } else if (a < 0) {
                    w = Opening.Wall.LEFT;
                    t = d;
                } else if (a >= L) {
                    w = Opening.Wall.RIGHT;
                    t = d;
                } else {
                    continue;
                }
                if (t < 0 || t >= ((w == Opening.Wall.LEFT || w == Opening.Wall.RIGHT) ? D : L)) continue;
                wall = w;
                lo = Math.min(lo, t);
                hi = Math.max(hi, t);
            }
        }
        return wall == null ? null : new Opening(wall, lo, hi, door == r.door());
    }

    /**
     * Lays a blueprint on a room's kit, then the reachability pass, the shadow fill and the commit: every piece as
     * FURNITURE marks (one per layer, a floor inlay at y 0), every chest, machine and guard as its mark, the chests
     * last in slot order (D305).
     */
    private void lay(Room r, ComplexLayout.Frame f, Kit k, RoomBlueprint bp) {
        bp.lay(k);
        // D321: a light tile a blueprint built over (a pool, a cell wall) is gone; the shadow fill relights the floor.
        for (int[] t : k.droppedTiles()) {
            int x = f.x(t[0], t[1]), z = f.z(t[0], t[1]);
            r.remove(MarkKind.LIGHT, x, 0, z);
            floorLamp[x * S + z] = false;
        }
        // D322: a floor hole a cell wall or a pool was built over stays whole floor.
        for (int[] t : k.filledHoles()) r.remove(MarkKind.DAMAGE_CELL, f.x(t[0], t[1]), 0, f.z(t[0], t[1]));
        k.reachable();
        vending(r, k);
        List<int[]> tiles = k.shadowFill();
        if (!tiles.isEmpty()) shadowTiles(r, f, tiles);
        java.util.Map<Integer, Long> anchors = new java.util.HashMap<Integer, Long>();
        for (Kit.Placed p : k.commit()) {
            int x = f.x(p.a, p.d), z = f.z(p.a, p.d);
            int hang = p.facing.world(f.side);
            Legend.Entry e = p.entry;
            if (e.kind == Legend.Entry.Kind.MARK) {
                switch (e.markKind()) {
                    case LOOT_CHEST:
                    case GUN_DISPLAY:
                        r.add(e.markKind(), x, 1, z, p.key, hang, 1);
                        break;
                    case MACHINE:
                        // A machine's front faces a horizontal side (0 north, 1 east, 2 south, 3 west).
                        r.add(e.markKind(), x, 1, z, p.key, (hang + 2) & 3, 1);
                        break;
                    default:
                        r.add(e.markKind(), x, 1, z, p.key);
                        break;
                }
                continue;
            }
            long g = 0;
            if (p.group != 0) {
                Long have = anchors.get(p.group);
                if (have == null) {
                    have = x * 4096L + z;
                    anchors.put(p.group, have);
                }
                g = have;
            }
            if (e.floorPiece() != null) r.add(
                MarkKind.FURNITURE,
                x,
                0,
                z,
                e.floorPiece()
                    .key(p.variant, g),
                hang,
                1);
            FurniturePiece[] layers = e.layers();
            for (int y = 1; y <= layers.length; y++) {
                if (layers[y - 1] != null) {
                    r.add(MarkKind.FURNITURE, x, y, z, layers[y - 1].key(p.variant, g), hang, 1);
                }
            }
        }
        for (Kit.Placed p : k.floorMarks()) r.add(p.entry.markKind(), f.x(p.a, p.d), 1, f.z(p.a, p.d), p.key);
        // The spots a spawner may take (D326): two bare, reachable floor cells far apart; the contents pass rolls how
        // many of them hold one at the room's activity level.
        for (int[] c : k.spawnerSpots(SPAWNER_SPOTS)) {
            int x = f.x(c[0], c[1]), z = f.z(c[0], c[1]);
            r.add(MarkKind.SPAWNER, x, 1, z, SPOT);
        }
        pools(r, f, k);
    }

    /**
     * Sinks a room's pools (D321): under each pool cell's floor-layer water, more water down to {@link Kit#POOL_DEPTH}
     * and a lining under it; under every cell around a pool (its rim, diagonals too), a lining from just under the
     * floor down to the same depth, so no cave or loose rock beside the pool ever meets its water.
     */
    private static void pools(Room r, ComplexLayout.Frame f, Kit k) {
        int L = f.lateral, D = f.depth, deep = Kit.POOL_DEPTH;
        String water = FurniturePiece.POOL_WATER.key(0), liner = FurniturePiece.POOL_LINER.key(0);
        boolean[] rim = new boolean[L * D];
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                if (!k.pool(a, d)) continue;
                int x = f.x(a, d), z = f.z(a, d);
                for (int y = 1; y < deep; y++) r.add(MarkKind.FURNITURE, x, -y, z, water, 0, 1);
                r.add(MarkKind.FURNITURE, x, -deep, z, liner, 0, 1);
                for (int da = -1; da <= 1; da++) {
                    for (int dd = -1; dd <= 1; dd++) {
                        int na = a + da, nd = d + dd;
                        if (na >= 0 && nd >= 0 && na < L && nd < D && !k.pool(na, nd)) rim[na * D + nd] = true;
                    }
                }
            }
        }
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                if (!rim[a * D + d]) continue;
                for (int y = 1; y <= deep; y++) r.add(MarkKind.FURNITURE, f.x(a, d), -y, f.z(a, d), liner, 0, 1);
            }
        }
    }

    /**
     * The shadow fill's light tiles: a floor light mark on each, keyed to the room's nearest floor tile's run, so it
     * lights and dies with that run (D231, D305).
     */
    private void shadowTiles(Room r, ComplexLayout.Frame f, List<int[]> tiles) {
        List<Mark> floor = new ArrayList<Mark>();
        for (Mark m : r.marks()) if (m.kind() == MarkKind.LIGHT && m.y() == 0) floor.add(m);
        for (int[] t : tiles) {
            int x = f.x(t[0], t[1]), z = f.z(t[0], t[1]);
            String key = "lamp:" + r.id() + ":0";
            int best = Integer.MAX_VALUE;
            for (Mark m : floor) {
                int dist = Math.abs(m.x() - x) + Math.abs(m.z() - z);
                if (dist < best) {
                    best = dist;
                    key = m.key();
                }
            }
            r.add(MarkKind.LIGHT, x, 0, z, key);
            floorLamp[x * S + z] = true;
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
     * @param planSeed the plan's seed
     * @param roomId   the room's section id
     * @return the room's wood: spruce, dark oak, birch or oak, from the seed (no random draw)
     */
    static int woodOf(long planSeed, int roomId) {
        int[] woods = { 1, 5, 2, 0, 1, 5 };
        long h = planSeed * 0x9E3779B97F4A7C15L + roomId * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        return woods[(int) Math.floorMod(h, (long) woods.length)];
    }
}
