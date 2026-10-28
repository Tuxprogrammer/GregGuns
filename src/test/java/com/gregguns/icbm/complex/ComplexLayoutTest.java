package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/** The I6b layout (D229, D230 to D234): coverage, counts, sizes, the gallery, doors, furnishing and lamps. */
class ComplexLayoutTest {

    private static final int S = ComplexLayout.SIZE;

    private static ComplexPlan plan(long seed) {
        return ComplexLayout.generate(seed, (int) seed, 3);
    }

    private static String fingerprint(ComplexPlan p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.allMarks())
            .append(p.doors());
        for (SiloSlot s : p.silos()) sb.append(s.bounds())
            .append(s.facing())
            .append(s.missing())
            .append(s.variant());
        for (Room r : p.rooms()) sb.append(r.kind())
            .append(r.bounds());
        sb.append(p.escapeX())
            .append(',')
            .append(p.escapeZ());
        return sb.toString();
    }

    @Test
    void sameSeedAndCellGiveTheSamePlanWhateverWasGeneratedBefore() {
        String a = fingerprint(ComplexLayout.generate(42L, 3, -7));
        // Other plans in between: no static state carries over.
        for (long s = 0; s < 5; s++) plan(s);
        String b = fingerprint(ComplexLayout.generate(42L, 3, -7));
        assertEquals(a, b);
        assertNotEquals(a, fingerprint(ComplexLayout.generate(42L, 4, -7)));
    }

    @Test
    void atLeastThreeQuartersOfTheFootprintIsBuilt() {
        int min = 100, sum = 0, n = 200;
        for (long s = 0; s < n; s++) {
            ComplexRaster r = new ComplexRaster(plan(s));
            int cover = r.coveredColumns() * 100 / (S * S);
            assertTrue(cover >= ComplexLayout.MIN_COVERAGE_PERCENT, "seed " + s + " covers " + cover + "%");
            min = Math.min(min, cover);
            sum += cover;
        }
        System.out.println("ComplexLayoutTest coverage over " + n + " seeds: min " + min + "%, mean " + sum / n + "%");
    }

    @Test
    void roomCountKindsAndSizes() {
        long area = 0;
        int rooms = 0;
        for (long s = 0; s < 200; s++) {
            ComplexPlan p = plan(s);
            int n = p.rooms()
                .size();
            assertTrue(n >= ComplexLayout.MIN_ROOMS && n <= ComplexLayout.MAX_ROOMS, "seed " + s + ": " + n + " rooms");
            Set<RoomKind> kinds = EnumSet.noneOf(RoomKind.class);
            int posts = 0, lc = 0;
            for (Room r : p.rooms()) {
                kinds.add(r.kind());
                if (r.kind() == RoomKind.GUARD_POST) posts++;
                if (r.kind() == RoomKind.LAUNCH_CONTROL) lc++;
                Box b = r.bounds();
                assertEquals(
                    r.kind()
                        .small() ? ComplexLayout.MINOR_HEIGHT : ComplexLayout.ROOM_HEIGHT,
                    b.sizeY(),
                    r.kind() + " height");
                assertNotNull(r.door(), r.kind() + " has a doorway to its parent");
                if (!r.kind()
                    .small()) {
                    area += b.sizeX() * b.sizeZ();
                    rooms++;
                }
            }
            for (RoomKind k : RoomKind.values()) {
                if (k.mandatory && k != RoomKind.ASSEMBLY_BAY)
                    assertTrue(kinds.contains(k), "seed " + s + " lacks " + k);
            }
            assertTrue(kinds.contains(RoomKind.DECON_AIRLOCK), "seed " + s + " has an airlock");
            assertTrue(posts >= 3, "seed " + s + ": guard posts " + posts);
            assertEquals(1, lc, "one shared Launch Control");
            Box hub = p.hall()
                .bounds();
            assertEquals(
                ComplexLayout.HUB_LONG * ComplexLayout.HUB_SHORT,
                hub.sizeX() * hub.sizeZ(),
                "the hub is 51 x 40 (twice the I6 hall)");
            assertEquals(ComplexLayout.HUB_HEIGHT, hub.sizeY());
            for (Tunnel t : p.tunnels()) {
                Box b = t.bounds();
                int across = t.alongX() ? b.sizeZ() : b.sizeX();
                if (t.major()) {
                    assertEquals(ComplexLayout.MAJOR_WIDTH, across, "a major hall is 20 wide");
                    assertEquals(t.gallery() ? ComplexLayout.GALLERY_HEIGHT : ComplexLayout.MAJOR_HEIGHT, b.sizeY());
                } else {
                    assertEquals(ComplexLayout.MINOR_WIDTH, across, "a minor hallway is 5 wide");
                    assertEquals(ComplexLayout.MINOR_HEIGHT, b.sizeY());
                }
            }
        }
        // The I6 room averaged a 14 x 12 footprint (168 cells): rooms now average four times that or more.
        long mean = area / rooms;
        System.out
            .println("ComplexLayoutTest mean room footprint " + mean + " cells (" + mean * 10 / 168 / 10.0 + "x I6)");
        assertTrue(mean >= 4 * 168 && mean <= 8 * 168, "mean room footprint " + mean);
    }

    @Test
    void sectionsAndSilosStayInsideAndNeverOverlap() {
        for (long s = 0; s < 120; s++) {
            ComplexPlan p = plan(s);
            List<Box> boxes = new ArrayList<Box>();
            for (int id = 0; id < p.sectionCount(); id++) boxes.add(p.sectionBox(id));
            for (SiloSlot si : p.silos()) boxes.add(si.bounds());
            for (int i = 0; i < boxes.size(); i++) {
                Box a = boxes.get(i);
                assertTrue(a.minX >= 0 && a.minZ >= 0 && a.maxX <= S && a.maxZ <= S, "seed " + s + " " + a);
                assertTrue(a.maxY <= ComplexRaster.LEVELS);
                for (int j = i + 1; j < boxes.size(); j++) {
                    assertFalse(a.overlapsXZ(boxes.get(j)), "seed " + s + ": " + a + " overlaps " + boxes.get(j));
                }
            }
        }
    }

    @Test
    void siloCountIsFourToTenWeightedLowAlongAtMostTwoGalleryHalls() {
        int[] counts = new int[11];
        int n = 1000;
        for (long s = 0; s < n; s++) {
            ComplexPlan p = plan(s);
            int c = p.silos()
                .size();
            assertTrue(c >= 4 && c <= 10, "seed " + s + ": " + c + " silos");
            counts[c]++;
            List<Tunnel> galleries = new ArrayList<Tunnel>();
            for (Tunnel t : p.tunnels()) if (t.gallery()) galleries.add(t);
            assertTrue(galleries.size() >= 1 && galleries.size() <= 2, "seed " + s + ": galleries " + galleries.size());
            for (SiloSlot si : p.silos()) {
                // The walkway cell in front of the controller is in a gallery hall's wall, the hall beyond it.
                int ix = si.frontX() + (si.frontX() - si.controllerX()),
                    iz = si.frontZ() + (si.frontZ() - si.controllerZ());
                boolean on = false;
                for (Tunnel g : galleries) on |= g.bounds()
                    .contains(si.frontX(), 1, si.frontZ())
                    && g.bounds()
                        .contains(ix, 1, iz);
                assertTrue(on, "seed " + s + ": a silo off the gallery");
            }
            for (Tunnel g : galleries) {
                Set<Integer> sides = new TreeSet<Integer>();
                int here = 0;
                for (SiloSlot si : p.silos()) {
                    if (!g.bounds()
                        .contains(si.frontX(), 1, si.frontZ())) continue;
                    here++;
                    sides.add(si.facing());
                }
                if (here >= 2) assertEquals(2, sides.size(), "seed " + s + ": silos line both sides of the gallery");
            }
        }
        int low = counts[4] + counts[5] + counts[6], mid = counts[7] + counts[8], high = counts[9] + counts[10];
        System.out.println(
            "ComplexLayoutTest silos 4..10 over " + n
                + " seeds: "
                + java.util.Arrays.toString(java.util.Arrays.copyOfRange(counts, 4, 11)));
        assertTrue(low > n * 6 / 10, "mostly 4 to 6: " + low);
        assertTrue(mid > n / 10 && mid < low, "sometimes 7 or 8: " + mid);
        assertTrue(high > 0 && high < mid, "rarely 9 or 10: " + high);
    }

    @Test
    void launchControlOverlooksTheGalleryAndTheAssemblyFloorStandsBesideIt() {
        int beside = 0, n = 200;
        for (long s = 0; s < n; s++) {
            ComplexPlan p = plan(s);
            Room lc = null, asm = null;
            for (Room r : p.rooms()) {
                if (r.kind() == RoomKind.LAUNCH_CONTROL) lc = r;
                if (r.kind() == RoomKind.MISSILE_ASSEMBLY) asm = r;
            }
            Object parent = p.section(lc.parent());
            assertTrue(
                parent instanceof Tunnel && ((Tunnel) parent).gallery(),
                "seed " + s + ": LC opens off the gallery");
            if (asm != null && p.section(asm.parent()) instanceof Tunnel
                && ((Tunnel) p.section(asm.parent())).gallery()) {
                beside++;
            }
            int windows = 0;
            for (Door d : p.doors()) if (d.kind == Door.Kind.WINDOW && d.a == lc.parent()) windows++;
            ComplexRaster r = new ComplexRaster(p);
            int panes = 0;
            for (Door d : p.doors()) {
                if (d.kind != Door.Kind.WINDOW) continue;
                for (int x = d.x0; x <= d.x1; x++) for (int z = d.z0; z <= d.z1; z++) {
                    if (r.at(x, 4, z) == Cell.WINDOW) panes++;
                }
            }
            if (windows > 0) assertTrue(panes > 0, "seed " + s + ": window panes in the wall");
        }
        assertTrue(beside > n * 3 / 4, "the assembly floor opens off the gallery in most complexes: " + beside);
    }

    @Test
    void guardPostsStandAtBranchPointsAndSilosAreGuardedFromDeepIn() {
        for (long s = 0; s < 120; s++) {
            ComplexPlan p = plan(s);
            int deep = 0;
            for (Mark m : p.allMarks()) if (m.kind() == MarkKind.SPAWNER && "deep".equals(m.key())) deep++;
            assertTrue(
                deep >= p.silos()
                    .size() + 1,
                "a post per silo walkway and one at the gallery's end: " + deep);
            int posts = 0;
            for (Tunnel t : p.tunnels()) {
                for (Mark m : t.marks()) {
                    if (m.kind() != MarkKind.SPAWNER || !"post".equals(m.key())) continue;
                    posts++;
                    boolean nearBranch = false;
                    for (Door d : p.doors()) {
                        if (d.kind != Door.Kind.PORTAL || !d.joins(t.id())) continue;
                        nearBranch |= m.x() >= d.x0 - 8 && m.x() <= d.x1 + 8 && m.z() >= d.z0 - 8 && m.z() <= d.z1 + 8;
                    }
                    assertTrue(nearBranch, "seed " + s + ": a hall post stands by a branch point: " + m);
                }
            }
            assertTrue(posts >= 2, "seed " + s + ": posts at branch points " + posts);
        }
    }

    @Test
    void theEscapeLadderClingsToItsWallAndRisesThroughTheCeiling() {
        for (long seed = 0; seed < 120; seed++) {
            ComplexPlan p = plan(seed);
            ComplexRaster raster = new ComplexRaster(p);
            int meta = p.escapeMeta();
            int dx = meta == 4 ? 1 : meta == 5 ? -1 : 0, dz = meta == 2 ? 1 : meta == 3 ? -1 : 0;
            assertEquals(Cell.WALL, raster.at(p.escapeX() + dx, 3, p.escapeZ() + dz), "ladder meta " + meta);
            for (int y = 1; y < p.escapeTop(); y++) assertEquals(Cell.LADDER, raster.at(p.escapeX(), y, p.escapeZ()));
        }
    }

    @Test
    void armoryFramesHangOnASolidWallInsideTheRoom() {
        int walls = 0;
        for (long seed = 0; seed < 120; seed++) {
            ComplexPlan p = plan(seed);
            ComplexRaster raster = new ComplexRaster(p);
            for (Room r : p.rooms()) {
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.ITEM_FRAME_WALL) continue;
                    walls++;
                    assertEquals(RoomKind.ARMORY, r.kind());
                    boolean alongZ = m.facing() == 1 || m.facing() == 3;
                    int bx = m.facing() == 1 ? 1 : m.facing() == 3 ? -1 : 0,
                        bz = m.facing() == 0 ? -1 : m.facing() == 2 ? 1 : 0;
                    for (int i = 0; i < m.extent(); i++) {
                        int x = m.x() + (alongZ ? 0 : ComplexLayout.FRAME_STRIDE * i),
                            z = m.z() + (alongZ ? ComplexLayout.FRAME_STRIDE * i : 0);
                        assertEquals(Cell.AIR, raster.at(x, m.y(), z), "frame cell is air inside the room");
                        assertEquals(Cell.WALL, raster.at(x + bx, m.y(), z + bz), "a solid wall behind it");
                        assertTrue(
                            x > r.bounds().minX && x < r.bounds().maxX - 1
                                && z > r.bounds().minZ
                                && z < r.bounds().maxZ - 1,
                            "inside the room");
                    }
                }
            }
        }
        assertTrue(walls > 200, "armories are mandatory, two rows each: " + walls);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** D208: furnishing follows each room's own door, whichever way the room faces; everything stays reachable. */
    @Test
    void roomsAreFurnishedRelativeToTheirDoor() {
        int checked = 0, stock = 0;
        int[] sides = new int[4];
        for (long seed = 0; seed < 150; seed++) {
            ComplexPlan p = plan(seed);
            ComplexRaster raster = new ComplexRaster(p);
            for (Room r : p.rooms()) {
                ComplexLayout.Frame f = new ComplexLayout.Frame(r);
                sides[f.side]++;
                int D = f.depth, L = f.lateral;
                // The doorway is open in the door wall and just inside it.
                assertEquals(Cell.AIR, raster.at(f.wallX(f.doorLo), 2, f.wallZ(f.doorLo)), r.kind() + " doorway");
                assertEquals(Cell.AIR, raster.at(f.x(f.doorLo, D - 1), 2, f.z(f.doorLo, D - 1)));
                java.util.Set<Long> blocked = new java.util.HashSet<Long>();
                for (Mark m : r.marks()) {
                    if (m.y() != 1) continue;
                    if (m.kind() == MarkKind.LOOT_CHEST || m.kind() == MarkKind.MACHINE
                        || m.kind() == MarkKind.SPAWNER) {
                        blocked.add(key(m.x(), m.z()));
                        if (m.key()
                            .equals(MachineRole.NASA_WORKBENCH.name())) {
                            for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) {
                                assertEquals(Cell.AIR, raster.at(m.x() + ox, 1, m.z() + oz), "NASA 3x3 clear");
                                assertTrue(Cell.open(raster.at(m.x() + 2 * ox, 1, m.z() + 2 * oz)), "free ring");
                                blocked.add(key(m.x() + ox, m.z() + oz));
                            }
                        }
                    }
                }
                java.util.Set<Long> seen = new java.util.HashSet<Long>();
                ArrayDeque<int[]> q = new ArrayDeque<int[]>();
                for (int a = f.doorLo; a <= f.doorHi; a++) {
                    q.add(new int[] { f.x(a, D - 1), f.z(a, D - 1) });
                    seen.add(key(f.x(a, D - 1), f.z(a, D - 1)));
                }
                int[][] nb = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
                while (!q.isEmpty()) {
                    int[] c = q.poll();
                    for (int[] n : nb) {
                        int x = c[0] + n[0], z = c[1] + n[1];
                        if (!Cell.open(raster.at(x, 1, z)) || !r.bounds()
                            .contains(x, 1, z)) continue;
                        if (blocked.contains(key(x, z)) || !seen.add(key(x, z))) continue;
                        q.add(new int[] { x, z });
                    }
                }
                for (Mark m : r.marks()) {
                    if (m.kind() != MarkKind.LOOT_CHEST && m.kind() != MarkKind.MACHINE && m.kind() != MarkKind.SPAWNER)
                        continue;
                    checked++;
                    int a = -1, d = -1;
                    for (int ia = 0; ia < L; ia++) for (int id = 0; id < D; id++) {
                        if (f.x(ia, id) == m.x() && f.z(ia, id) == m.z()) {
                            a = ia;
                            d = id;
                        }
                    }
                    String what = "seed " + seed + " " + r.kind() + " " + m;
                    assertTrue(d >= 0, "inside the room, not on a wall: " + what);
                    assertEquals(Cell.AIR, raster.at(m.x(), 1, m.z()), what);
                    assertFalse(f.blocksDoor(a, d), "doorway stays clear: " + what);
                    if (m.kind() == MarkKind.LOOT_CHEST && r.kind() == RoomKind.STOCKPILE) {
                        assertEquals(0, d, "stockpile chests stand on the far wall: " + what);
                        stock++;
                    }
                    int reach = m.key()
                        .equals(MachineRole.NASA_WORKBENCH.name()) ? 2 : 1;
                    boolean ok = false;
                    for (int[] n : nb) ok |= seen.contains(key(m.x() + n[0] * reach, m.z() + n[1] * reach));
                    assertTrue(ok, "reachable from the door: " + what);
                }
                for (Mark m : r.marks()) {
                    if (m.kind() == MarkKind.SIGN) {
                        boolean onDoorWall = f.side == 0 ? m.z() == f.b.minZ
                            : f.side == 2 ? m.z() == f.b.maxZ - 1
                                : f.side == 1 ? m.x() == f.b.maxX - 1 : m.x() == f.b.minX;
                        assertTrue(onDoorWall, "sign hangs on the door wall: " + m);
                        assertEquals(f.doorWallFacing(), m.facing());
                    }
                    if (m.kind() == MarkKind.DAMAGE_CELL && m.key()
                        .equals("wall")) {
                        Box b = r.bounds();
                        assertTrue(
                            m.x() == b.minX || m.x() == b.maxX - 1 || m.z() == b.minZ || m.z() == b.maxZ - 1,
                            "wall damage sits in a wall: " + m);
                    }
                }
            }
        }
        for (int side = 0; side < 4; side++) assertTrue(sides[side] > 100, "doors on every side: " + sides[side]);
        assertTrue(checked > 10000 && stock > 50, checked + " / " + stock);
    }

    @Test
    void everyRoomAndEverySiloWalkwayIsReachableOnFootFromTheHub() {
        for (long s = 0; s < 80; s++) {
            ComplexPlan p = plan(s);
            ComplexRaster r = new ComplexRaster(p);
            boolean[] block = new boolean[S * S];
            for (Mark m : p.allMarks()) {
                if (m.y() == 1 && (m.kind() == MarkKind.LOOT_CHEST || m.kind() == MarkKind.MACHINE
                    || m.kind() == MarkKind.SPAWNER)) block[m.x() * S + m.z()] = true;
            }
            boolean[] seen = new boolean[S * S];
            ArrayDeque<int[]> q = new ArrayDeque<int[]>();
            Box h = p.hall()
                .bounds();
            int[] start = null;
            for (int x = h.minX + 1; x < h.maxX - 1 && start == null; x++) {
                for (int z = h.minZ + 1; z < h.maxZ - 1; z++) {
                    if (!block[x * S + z] && Cell.open(r.at(x, 1, z))) {
                        start = new int[] { x, z };
                        break;
                    }
                }
            }
            seen[start[0] * S + start[1]] = true;
            q.add(start);
            int[][] d = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] k : d) {
                    int x = c[0] + k[0], z = c[1] + k[1];
                    if (x < 0 || z < 0 || x >= S || z >= S || seen[x * S + z] || block[x * S + z]) continue;
                    if (!Cell.open(r.at(x, 1, z)) || !Cell.open(r.at(x, 2, z))) continue;
                    seen[x * S + z] = true;
                    q.add(new int[] { x, z });
                }
            }
            for (int id = 0; id < p.sectionCount(); id++) {
                Box b = p.sectionBox(id);
                boolean any = false;
                for (int x = b.minX + 1; x < b.maxX - 1 && !any; x++) {
                    for (int z = b.minZ + 1; z < b.maxZ - 1; z++) {
                        if (seen[x * S + z]) {
                            any = true;
                            break;
                        }
                    }
                }
                assertTrue(any, "seed " + s + ": section " + id + " " + p.section(id) + " unreachable");
            }
            for (SiloSlot si : p.silos())
                assertTrue(seen[si.frontX() * S + si.frontZ()], "seed " + s + " silo walkway");
        }
    }

    /**
     * Lamps sit flush in the ceiling layer on a regular grid aligned to their section: the lamps of a section are the
     * full product of a set of x lines and a set of z lines, at most {@code TILE_GAP} apart, centered in the section,
     * and a section over seven high has a light tile in the floor under each.
     */
    @Test
    void lampsSitOnARegularGridAlignedToEachSection() {
        for (long s = 0; s < 60; s++) {
            ComplexPlan p = plan(s);
            for (int id = 0; id < p.sectionCount(); id++) {
                Box b = p.sectionBox(id);
                List<Mark> marks = id == 0 ? p.hall()
                    .marks()
                    : p.section(id) instanceof Room ? ((Room) p.section(id)).marks() : ((Tunnel) p.section(id)).marks();
                TreeSet<Integer> xs = new TreeSet<Integer>(), zs = new TreeSet<Integer>();
                Set<Long> ceiling = new java.util.HashSet<Long>(), floor = new java.util.HashSet<Long>();
                for (Mark m : marks) {
                    if (m.kind() != MarkKind.LIGHT) continue;
                    assertTrue(m.y() == b.sizeY() - 1 || m.y() == 0, "lamps are flush in the ceiling or floor: " + m);
                    assertTrue(
                        m.key()
                            .startsWith("lamp:" + id + ":"),
                        m.key());
                    if (m.y() == 0) {
                        floor.add(key(m.x(), m.z()));
                        continue;
                    }
                    ceiling.add(key(m.x(), m.z()));
                    xs.add(m.x());
                    zs.add(m.z());
                }
                boolean escapeHere = b.contains(p.escapeX(), 1, p.escapeZ());
                assertTrue(
                    ceiling.size() == xs.size() * zs.size()
                        || escapeHere && ceiling.size() == xs.size() * zs.size() - 1,
                    "seed " + s
                        + " section "
                        + id
                        + ": a full grid, "
                        + ceiling.size()
                        + " of "
                        + xs.size()
                        + " x "
                        + zs.size());
                boolean tall = b.sizeY() > ComplexLayout.MINOR_HEIGHT;
                int gap = tall ? ComplexFurnisher.TILE_GAP : ComplexFurnisher.LOW_GAP;
                assertGrid(xs, b.minX + 1, b.maxX - 2, gap, "x of section " + id);
                assertGrid(zs, b.minZ + 1, b.maxZ - 2, gap, "z of section " + id);
                if (tall) assertEquals(ceiling, floor, "a light tile under every ceiling lamp");
                else assertTrue(floor.isEmpty());
            }
        }
    }

    private static void assertGrid(TreeSet<Integer> lines, int lo, int hi, int gap, String what) {
        assertFalse(lines.isEmpty(), what);
        int first = lines.first() - lo, last = hi - lines.last();
        assertTrue(first <= gap / 2 + 1 && last <= gap / 2 + 1, what + " margins " + first + ", " + last);
        assertTrue(Math.abs(first - last) <= 1, what + " is centered: " + first + " vs " + last);
        Integer prev = null;
        Set<Integer> steps = new TreeSet<Integer>();
        for (int v : lines) {
            if (prev != null) {
                assertTrue(v - prev <= gap, what + " step " + (v - prev));
                steps.add(v - prev);
            }
            prev = v;
        }
        assertTrue(steps.size() <= 2, what + " steps are regular: " + steps);
    }

    /**
     * D217 adapted (D230): with every lamp lit, Minecraft's block light (15 at a lamp, one less per step through air,
     * stopped by solid blocks; furniture and machines counted solid) reads 8 or more on every open floor cell.
     */
    @Test
    void everyOpenFloorCellReadsLightEightWhenTheLampsBurn() {
        final int Y = ComplexRaster.LEVELS;
        int checked = 0;
        for (long s = 0; s < 12; s++) {
            ComplexPlan p = plan(s);
            ComplexRaster r = new ComplexRaster(p);
            boolean[] opaque = new boolean[S * Y * S];
            byte[] lv = new byte[S * Y * S];
            for (int x = 0; x < S; x++) for (int y = 0; y < Y; y++) for (int z = 0; z < S; z++) {
                byte c = r.at(x, y, z);
                opaque[(x * Y + y) * S + z] = !(Cell.open(c) || c == Cell.WINDOW);
            }
            boolean[] occupied = new boolean[S * S];
            for (Mark m : p.allMarks()) {
                if (m.kind() == MarkKind.MACHINE || m.kind() == MarkKind.SPAWNER) {
                    occupied[m.x() * S + m.z()] = true;
                    opaque[(m.x() * Y + m.y()) * S + m.z()] = true;
                    if (m.key()
                        .equals(MachineRole.NASA_WORKBENCH.name())) {
                        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                            occupied[(m.x() + dx) * S + m.z() + dz] = true;
                            for (int y = 1; y <= 2; y++) opaque[((m.x() + dx) * Y + y) * S + m.z() + dz] = true;
                        }
                    }
                }
                if (m.kind() == MarkKind.LOOT_CHEST) occupied[m.x() * S + m.z()] = true;
            }
            ArrayDeque<int[]> q = new ArrayDeque<int[]>();
            for (int x = 0; x < S; x++) for (int y = 0; y < Y; y++) for (int z = 0; z < S; z++) {
                if (r.at(x, y, z) == Cell.LAMP) {
                    lv[(x * Y + y) * S + z] = 15;
                    q.add(new int[] { x, y, z });
                }
            }
            int[][] d = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            while (!q.isEmpty()) {
                int[] c = q.poll();
                int l = lv[(c[0] * Y + c[1]) * S + c[2]];
                if (l <= 1) continue;
                for (int[] k : d) {
                    int x = c[0] + k[0], y = c[1] + k[1], z = c[2] + k[2];
                    if (x < 0 || y < 0 || z < 0 || x >= S || y >= Y || z >= S) continue;
                    int i = (x * Y + y) * S + z;
                    if (opaque[i] || lv[i] >= l - 1) continue;
                    lv[i] = (byte) (l - 1);
                    q.add(new int[] { x, y, z });
                }
            }
            for (int id = 0; id < p.sectionCount(); id++) {
                Box b = p.sectionBox(id);
                for (int x = b.minX + 1; x < b.maxX - 1; x++) {
                    for (int z = b.minZ + 1; z < b.maxZ - 1; z++) {
                        if (occupied[x * S + z] || !Cell.open(r.at(x, 1, z))) continue;
                        checked++;
                        int l = lv[(x * Y + 1) * S + z];
                        assertTrue(l >= 8, "seed " + s + " section " + id + " floor " + x + "," + z + " reads " + l);
                    }
                }
            }
        }
        assertTrue(checked > 200_000, "floor cells checked: " + checked);
    }
}
