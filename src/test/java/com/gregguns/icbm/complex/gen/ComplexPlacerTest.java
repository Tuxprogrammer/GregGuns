package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.BiomeDictionary.Type;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Footprint;
import com.gregguns.worldgen.HeightProbe;
import com.gregguns.worldgen.PoolEntry;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.RecordingWorld;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.ResolvedBlock;

/** The complex never writes outside a window, never requests a chunk, and any window order gives the same result. */
class ComplexPlacerTest {

    private static final int GROUND = 70;
    private final Map<String, Block> named = new HashMap<>();
    private final Block air = new Block(Material.air) {};

    private final BlockResolver resolver = new BlockResolver() {

        @Override
        public ResolvedBlock resolve(byte cell, int x, int y, int z) {
            return new ResolvedBlock(named.computeIfAbsent("c" + cell, k -> new Block(Material.rock) {}), 0, false);
        }

        @Override
        public ResolvedBlock named(String what, int x, int y, int z) {
            return new ResolvedBlock(named.computeIfAbsent(what, k -> new Block(Material.rock) {}), 0, false);
        }

        @Override
        public Block air() {
            return air;
        }
    };

    private final ComplexPlacer.Settings settings = new ComplexPlacer.Settings() {

        @Override
        public boolean dimensionEnabled(String n) {
            return true;
        }

        @Override
        public int chancePercent() {
            return 100;
        }

        @Override
        public Set<Type> overworldBiomes() {
            return Collections.emptySet();
        }
    };

    private static StructurePlan plan() {
        StructureGrid g = new StructureGrid(
            64,
            64,
            64,
            Collections.singletonMap(
                SizeClass.BIG,
                Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
        return g.planAt(77L, 0, SizeClass.BIG, 3, 4)
            .get();
    }

    private static final class Capture implements WindowedSink.Target {

        final RecordingWorld world;
        final Map<String, String> cells;
        int duplicates;

        Capture(RecordingWorld world, Map<String, String> cells) {
            this.world = world;
            this.cells = cells;
        }

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            world.setBlock(x, y, z, block, meta);
            if (cells.put(x + "," + y + "," + z, System.identityHashCode(block) + ":" + meta) != null) duplicates++;
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {}

        @Override
        public void spawn(Entity entity, double x, double y, double z) {}
    }

    private static final class Result {

        final Map<String, String> cells = new HashMap<>();
        final List<String> marks = new ArrayList<>();
        int outside;
        int rejected;
        int foreign;
        int duplicates;
        int windowsWithWrites;
    }

    /** The decision {@link #run} writes with. */
    private com.gregguns.worldgen.Decision decision = com.gregguns.worldgen.Decision.placed(GROUND);
    /** The placer of the last {@link #run}. */
    private ComplexPlacer lastPlacer;

    private Result run(StructurePlan plan, boolean reverse) {
        ComplexPlacer placer = lastPlacer = new ComplexPlacer(
            resolver,
            MachineHook.NONE,
            (p, m, x, y, z, s) -> marks(m, x, y, z),
            settings).withContents(false);
        Result r = new Result();
        currentMarks = r.marks;
        if (readBack != null) readBack = r.cells;
        List<int[]> order = new ArrayList<>();
        for (int cx = Math.floorDiv(plan.anchorX() - 8, 16) - 1; cx
            <= Math.floorDiv(plan.anchorX() + 240 - 8, 16) + 1; cx++) {
            for (int cz = Math.floorDiv(plan.anchorZ() - 8, 16) - 1; cz
                <= Math.floorDiv(plan.anchorZ() + 240 - 8, 16) + 1; cz++) {
                order.add(new int[] { cx, cz });
            }
        }
        if (reverse) Collections.reverse(order);
        for (int[] c : order) {
            RecordingWorld world = new RecordingWorld(c[0], c[1]);
            Capture cap = new Capture(world, r.cells);
            ClippingSink sink = new ClippingSink(world.window(), cap);
            PopulateWindow w = world.window();
            HeightProbe probe = new HeightProbe() {

                @Override
                public PopulateWindow window() {
                    return w;
                }

                @Override
                public int landY(int x, int z) {
                    return w.contains(x, z) ? GROUND : NONE;
                }

                @Override
                public int waterSurfaceY(int x, int z) {
                    return NONE;
                }

                @Override
                public int seaFloorY(int x, int z) {
                    return w.contains(x, z) ? seaFloorAt(x, z) : NONE;
                }

                @Override
                public int seaFloorYBeside(int x, int z) {
                    boolean near = x >= w.minX() - 1 && x <= w.maxX() + 1 && z >= w.minZ() - 1 && z <= w.maxZ() + 1;
                    return near ? seaFloorAt(x, z) : NONE;
                }
            };
            if (placer.write(plan, decision, w, sink, probe) > 0) r.windowsWithWrites++;
            r.outside += world.writesOutsideWindow()
                .size();
            r.rejected += sink.rejectedWrites();
            r.foreign += world.foreignChunkRequests()
                .size();
            r.duplicates += cap.duplicates;
        }
        return r;
    }

    /** The sea floor the probe reports inside a window (a valley or a lake bed), or NONE for no answer. */
    private int seaFloor = HeightProbe.NONE;
    /** A lake bed over part of the footprint: x0, z0, x1, z1 (exclusive) and its floor y, or null for none. */
    private int[] lake;

    /**
     * When set, the probe reads columns back as the windows before left them (natural ground at {@link #GROUND} under
     * what they wrote), as the real probe does: a window then sees the bores its neighbors already opened.
     */
    private Map<String, String> readBack;

    private int seaFloorAt(int x, int z) {
        if (readBack != null) {
            String airId = Integer.toString(System.identityHashCode(air));
            Block role = named.get("c" + Cell.AIR);
            String airRole = role == null ? airId : Integer.toString(System.identityHashCode(role));
            for (int y = GROUND; y >= 0; y--) {
                String v = readBack.get(x + "," + y + "," + z);
                if (v == null || !v.startsWith(airId + ":") && !v.startsWith(airRole + ":")) return y;
            }
            return HeightProbe.NONE;
        }
        if (lake != null && x >= lake[0] && z >= lake[1] && x < lake[2] && z < lake[3]) return lake[4];
        return seaFloor;
    }

    private List<String> currentMarks;

    private void marks(Mark m, int x, int y, int z) {
        currentMarks.add(m.kind() + "@" + x + "," + y + "," + z);
    }

    @Test
    void everyWriteIsInsideItsWindowAndNoChunkIsRequested() {
        Result r = run(plan(), false);
        assertEquals(0, r.outside, "GGS-11-08");
        assertEquals(0, r.rejected, "the placer never offers a write the sink must drop");
        assertEquals(0, r.foreign, "GGS-11-09");
        assertEquals(0, r.duplicates, "windows tile, so no cell is written twice");
        assertTrue(r.windowsWithWrites > 50, "240 blocks span many windows: " + r.windowsWithWrites);
        assertTrue(r.cells.size() > 20_000);
    }

    @Test
    void windowOrderDoesNotChangeTheResultOrTheMarks() {
        StructurePlan p = plan();
        Result fwd = run(p, false);
        Result back = run(p, true);
        assertEquals(fwd.cells, back.cells);
        Collections.sort(fwd.marks);
        Collections.sort(back.marks);
        assertEquals(fwd.marks, back.marks, "each mark reported once, in whichever window holds it");
        assertTrue(fwd.marks.size() > 200);
    }

    @Test
    void everyRadarStationGetsAHutAndTheSmallRadarOnItsRoof() {
        StructureGrid g = new StructureGrid(
            64,
            64,
            64,
            Collections.singletonMap(
                SizeClass.BIG,
                Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
        for (int cell = 3; cell < 40; cell++) {
            StructurePlan p = g.planAt(77L, 0, SizeClass.BIG, cell, 4)
                .get();
            java.util.List<int[]> stations = new ComplexPlacer(resolver, MachineHook.NONE, MarkFiller.NONE, settings)
                .rasterOf(p)
                .plan()
                .radarStations();
            if (stations.isEmpty()) continue;
            Result r = run(p, false);
            assertEquals(0, r.outside);
            assertEquals(0, r.duplicates);
            String casing = Integer.toString(System.identityHashCode(named.get("radarCasing")));
            String airId = Integer.toString(System.identityHashCode(air));
            for (int[] st : stations) {
                int hx = p.anchorX() + st[0], hz = p.anchorZ() + st[1];
                int cx = hx + 2, cz = hz + 2, roof = GROUND + 4;
                String wall = r.cells.get(hx + "," + (GROUND + 1) + "," + hz);
                assertTrue(wall != null && !wall.startsWith(airId + ":"), "hut corner is a wall");
                assertTrue(
                    r.cells.get(cx + "," + (GROUND + 2) + "," + (hz + 4))
                        .startsWith(airId + ":"),
                    "the doorway is open");
                // The radar is RadarShape's piece: 17 casings, the controller and the Maintenance Hatch as machine
                // placeholders, the pedestal on top, the Energy Hatch's place a casing.
                int casings = 0, machines = 0;
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) for (int layer = 1; layer <= 3; layer++) {
                        String v = r.cells.get((cx + dx) + "," + (roof + layer) + "," + (cz + dz));
                        int want = RadarStation.cellAt(dx, dz, layer);
                        if (want == RadarStation.NOTHING) {
                            assertTrue(
                                v == null || v.startsWith(airId + ":"),
                                "nothing at " + dx + "," + dz + "," + layer);
                        } else if (want == RadarStation.CASING) {
                            assertTrue(
                                v != null && v.startsWith(casing + ":"),
                                "casing at " + dx + "," + dz + "," + layer);
                            casings++;
                        } else {
                            assertTrue(v != null && v.startsWith(airId + ":"), "machine placeholder");
                            machines++;
                        }
                    }
                assertEquals(com.gregguns.icbm.defense.RadarShape.casingPositions() - 1, casings);
                assertEquals(2, machines, "the controller and the Maintenance Hatch");
                String pedestal = r.cells.get(cx + "," + (roof + 3) + "," + cz);
                assertTrue(pedestal != null && pedestal.startsWith(casing + ":"), "pedestal");
                assertEquals(com.gregguns.icbm.complex.Cell.RADAR_CONTROLLER, RadarStation.cellAt(0, -1, 2));
            }
            return;
        }
        throw new AssertionError("no complex with a radar station in the cells tried");
    }

    @Test
    void theEscapeLadderHasStoneBehindItFromTheLevelToJustUnderTheSurfaceBlock() {
        StructurePlan p = plan();
        Result r = run(p, false);
        ComplexPlan cp = new ComplexPlacer(resolver, MachineHook.NONE, MarkFiller.NONE, settings).rasterOf(p)
            .plan();
        int x = p.anchorX() + cp.escapeX(), z = p.anchorZ() + cp.escapeZ(), meta = cp.escapeMeta();
        int bx = x + (meta == 4 ? 1 : meta == 5 ? -1 : 0), bz = z + (meta == 2 ? 1 : meta == 3 ? -1 : 0);
        int floor = GROUND - ComplexPlacer.DEPTH;
        String backing = Integer.toString(System.identityHashCode(named.get("backing")));
        for (int y = floor + cp.escapeTop(); y < GROUND; y++) {
            String ladder = r.cells.get(x + "," + y + "," + z);
            String behind = r.cells.get(bx + "," + y + "," + bz);
            assertTrue(behind != null && behind.startsWith(backing + ":"), "backing at y " + y + ", windows or not");
            assertTrue(ladder != null, "ladder at y " + y);
        }
        // The surface block beside the trapdoor is the terrain's own, so the trapdoor stays camouflaged.
        assertNull(r.cells.get(bx + "," + GROUND + "," + bz), "no backing at the surface cell");
        assertEquals(0, r.duplicates);
    }

    /**
     * A lake bed under the hub's open air is cut off, and its natural terrain stays. The hub's open cells around it,
     * above the bed, become wall, so the hub never opens sideways onto the lake (R1 M2), across window edges too.
     */
    @Test
    void aHallBesideACutOffLakeBedIsWalledUpAboveTheBed() {
        StructurePlan p = plan();
        int floor = GROUND - ComplexPlacer.DEPTH;
        ComplexPlan cp = new ComplexPlacer(resolver, MachineHook.NONE, MarkFiller.NONE, settings).rasterOf(p)
            .plan();
        com.gregguns.icbm.complex.Box hub = cp.hall()
            .bounds();
        // The lake's west and north edges lie on window edges, so those faces are sealed from a column the window
        // beside it reads one past its own (HeightProbe.seaFloorYBeside); the east and south edges fall inside windows.
        int lx0 = windowEdge(p.anchorX() + hub.minX + 8), lz0 = windowEdge(p.anchorZ() + hub.minZ + 8);
        final int lw = 16, ld = 12;
        lake = new int[] { lx0, lz0, lx0 + lw, lz0 + ld, floor + 8 };
        Result r;
        try {
            r = run(p, false);
        } finally {
            lake = null;
        }
        String[] open = { "c" + Cell.AIR, "c" + Cell.RAIL_NS, "c" + Cell.RAIL_EW, "c" + Cell.LADDER };
        String wall = Integer.toString(System.identityHashCode(named.get("c" + Cell.WALL)));
        int faces = 0, walled = 0;
        for (int x = lx0 - 1; x <= lx0 + lw; x++) {
            for (int z = lz0 - 1; z <= lz0 + ld; z++) {
                boolean in = x >= lx0 && z >= lz0 && x < lx0 + lw && z < lz0 + ld;
                if (in) {
                    for (int y = floor; y < floor + ComplexRaster.LEVELS; y++) {
                        assertNull(r.cells.get(x + "," + y + "," + z), "the cut-off column keeps its terrain");
                    }
                    continue;
                }
                // A corner column only touches the lake diagonally, where water never flows.
                boolean corner = (x == lx0 - 1 || x == lx0 + lw) && (z == lz0 - 1 || z == lz0 + ld);
                if (corner) continue;
                for (int y = floor + 1; y < floor + hub.sizeY() - 1; y++) {
                    String v = r.cells.get(x + "," + y + "," + z);
                    if (y <= floor + 8) continue; // the bed's own ground faces these
                    faces++;
                    for (String o : open) {
                        Block b = named.get(o);
                        assertTrue(
                            b == null || v == null || !v.startsWith(System.identityHashCode(b) + ":"),
                            "open cell facing the lake at " + x + "," + y + "," + z);
                    }
                    if (v != null && v.startsWith(wall + ":")) walled++;
                }
            }
        }
        assertTrue(
            faces > 80 && walled == faces,
            "every cell around the lake above its bed is wall: " + walled + "/" + faces);
        // The plain hub away from the lake is still open.
        String far = r.cells.get((p.anchorX() + hub.minX + 3) + "," + (floor + 2) + "," + (p.anchorZ() + hub.minZ + 3));
        assertTrue(far != null && !far.startsWith(wall + ":"), "the rest of the hub is open");
    }

    /** @return the first window's west (or north) edge at or after {@code v} */
    private static int windowEdge(int v) {
        int e = PopulateWindow.chunkOf(v - PopulateWindow.OFFSET) * 16 + PopulateWindow.OFFSET;
        return e < v ? e + PopulateWindow.SIZE : e;
    }

    @Test
    void aColumnWhoseGroundLiesTooCloseAboveTheLevelIsCutOff() {
        StructurePlan p = plan();
        int floor = GROUND - ComplexPlacer.DEPTH;
        seaFloor = floor + 5; // a valley or lake bed: the level's roof would break through
        Result valley = run(p, false);
        seaFloor = HeightProbe.NONE;
        int under = 0;
        ComplexPlan cp = new ComplexPlacer(resolver, MachineHook.NONE, MarkFiller.NONE, settings).rasterOf(p)
            .plan();
        int ex = p.anchorX() + cp.escapeX(), ez = p.anchorZ() + cp.escapeZ();
        for (String key : valley.cells.keySet()) {
            String[] c = key.split(",");
            int y = Integer.parseInt(c[1]), x = Integer.parseInt(c[0]), z = Integer.parseInt(c[2]);
            // The escape shaft rises above its section by design; the level itself is what must not be written.
            if (Math.abs(x - ex) <= 1 && Math.abs(z - ez) <= 1 && y >= floor + cp.escapeTop()) continue;
            if (y < floor + com.gregguns.icbm.complex.ComplexRaster.LEVELS) under++;
        }
        assertEquals(0, under, "nothing of the level is written under such ground");
        assertEquals(0, valley.outside);
        assertTrue(run(p, false).cells.size() > 20_000, "and the same plan writes normally on ordinary ground");
    }

    /**
     * S3-2: a window that reads a neighbor's bore after that window opened it saw a column whose ground lay at the pad,
     * so it took the bore for a cut-off lake bed and walled up the silo's inside beside it. A bore is never a cut-off
     * neighbor and a silo's inside is never sealed: no wall inside any silo, in either window order, and both orders
     * agree.
     */
    @Test
    void aWrittenBoreNeverWallsUpTheSiloBesideIt() {
        StructurePlan p = plan();
        readBack = new HashMap<>();
        Result fwd, back;
        try {
            fwd = run(p, false);
            readBack = new HashMap<>();
            back = run(p, true);
        } finally {
            readBack = null;
        }
        int differ = 0;
        String firstDiffer = null;
        java.util.Set<String> keys = new java.util.HashSet<>(fwd.cells.keySet());
        keys.addAll(back.cells.keySet());
        for (String k : keys) {
            if (!java.util.Objects.equals(fwd.cells.get(k), back.cells.get(k))) {
                differ++;
                if (firstDiffer == null) firstDiffer = k;
            }
        }
        assertEquals(0, differ, "the window order does not change the level; first cell that differs: " + firstDiffer);
        ComplexPlan cp = new ComplexPlacer(resolver, MachineHook.NONE, MarkFiller.NONE, settings).rasterOf(p)
            .plan();
        int floor = GROUND - ComplexPlacer.DEPTH;
        String airId = Integer.toString(System.identityHashCode(air));
        // The level's AIR role goes through the test resolver (its own block); the bore over it is the resolver's air.
        String airRole = Integer.toString(System.identityHashCode(named.get("c" + Cell.AIR)));
        int checked = 0;
        for (Result r : new Result[] { fwd, back }) {
            for (com.gregguns.icbm.complex.SiloSlot s : cp.silos()) {
                int sx = p.anchorX() + s.bounds().minX, sz = p.anchorZ() + s.bounds().minZ;
                for (int x = sx + 1; x <= sx + 3; x++) {
                    for (int z = sz + 1; z <= sz + 3; z++) {
                        for (int y = floor + 1; y < GROUND; y++) {
                            String v = r.cells.get(x + "," + y + "," + z);
                            assertTrue(
                                v != null && (v.startsWith(airId + ":") || v.startsWith(airRole + ":")),
                                "the bore is air from the pad to the surface at " + x + "," + y + "," + z + ": " + v);
                            checked++;
                        }
                    }
                }
            }
        }
        assertTrue(checked > 1000, "silos checked: " + checked);
    }

    /**
     * S3-4: every column the level uses stands on the foundation, and every rock cell beside the level or over a
     * ceiling is the complex's own solid, so no cave opens onto a floor, wall or ceiling.
     */
    @Test
    void theLevelHasAFoundationUnderItAndSolidBesideAndOverIt() {
        StructurePlan p = plan();
        Result r = run(p, false);
        ComplexRaster raster = new ComplexPlacer(resolver, MachineHook.NONE, MarkFiller.NONE, settings).rasterOf(p);
        ComplexPlan cp = raster.plan();
        int floor = GROUND - ComplexPlacer.DEPTH;
        String found = Integer.toString(System.identityHashCode(named.get("foundation")));
        int ex = cp.escapeX(), ez = cp.escapeZ();
        int under = 0, beside = 0, past = 0;
        int size = com.gregguns.icbm.complex.ComplexLayout.SIZE;
        for (int lx = -1; lx <= size; lx++) {
            for (int lz = -1; lz <= size; lz++) {
                int x = p.anchorX() + lx, z = p.anchorZ() + lz;
                // A column past the box carries the skin only where its window also holds part of the box: the
                // window beyond a box edge that falls on a window edge never runs the complex.
                boolean outside = lx < 0 || lz < 0 || lx >= size || lz >= size;
                if (outside && !new PopulateWindow(Math.floorDiv(x - 8, 16), Math.floorDiv(z - 8, 16))
                    .overlapsBox(p.anchorX(), p.anchorZ(), p.anchorX() + size - 1, p.anchorZ() + size - 1)) continue;
                boolean used = false;
                for (int y = 0; y < ComplexRaster.LEVELS; y++) used |= raster.at(lx, y, lz) != Cell.NONE;
                if (used) {
                    String v = r.cells.get(x + "," + (floor - 1) + "," + z);
                    assertTrue(v != null && v.startsWith(found + ":"), "foundation under " + lx + "," + lz);
                    under++;
                }
                // The escape column and the one behind it belong to the ladder from its top up.
                boolean ladder = Math.abs(lx - ex) + Math.abs(lz - ez) <= 1;
                for (int y = 0; y < ComplexRaster.LEVELS; y++) {
                    if (raster.at(lx, y, lz) != Cell.NONE || ladder && y >= cp.escapeTop()) continue;
                    boolean next = raster.at(lx + 1, y, lz) != Cell.NONE || raster.at(lx - 1, y, lz) != Cell.NONE
                        || raster.at(lx, y, lz + 1) != Cell.NONE
                        || raster.at(lx, y, lz - 1) != Cell.NONE
                        || y > 0 && raster.at(lx, y - 1, lz) != Cell.NONE;
                    if (!next) continue;
                    String v = r.cells.get(x + "," + (floor + y) + "," + z);
                    assertTrue(
                        v != null && v.startsWith(found + ":"),
                        "solid beside the level at " + lx + "," + y + "," + lz);
                    beside++;
                    if (outside) past++;
                }
            }
        }
        assertTrue(under > 10_000 && beside > 5_000, "foundation " + under + ", skin " + beside);
        assertTrue(past > 0, "sections on the box's edge get the skin past it: " + past);
        assertEquals(0, r.duplicates, "the skin never takes a cell another pass writes");
    }

    @Test
    void aSunkDecisionWritesTheLevelDeeperAndItsLevelIsTheRecordedOne() {
        StructurePlan plan = plan();
        Result flat = run(plan, false);
        decision = com.gregguns.worldgen.Decision.placed(GROUND, 2, 15);
        Result sunk = run(plan, false);
        assertEquals(minY(flat) - 15, minY(sunk), "the whole level lies 15 blocks deeper");
        assertEquals(0, sunk.outside);
        assertEquals(
            com.gregguns.icbm.worldgen.ActivityLevel.OPERATIONAL,
            lastPlacer.levelOf(plan),
            "the level recorded with the decision");
    }

    private static int minY(Result r) {
        int min = Integer.MAX_VALUE;
        for (String k : r.cells.keySet()) min = Math.min(min, Integer.parseInt(k.split(",")[1]));
        return min;
    }
}
