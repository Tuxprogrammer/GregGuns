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

    private Result run(StructurePlan plan, boolean reverse) {
        ComplexPlacer placer = new ComplexPlacer(
            resolver,
            MachineHook.NONE,
            (p, m, x, y, z, s) -> marks(m, x, y, z),
            settings).withContents(false);
        Result r = new Result();
        currentMarks = r.marks;
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
            if (placer.write(plan, GROUND, w, sink, probe) > 0) r.windowsWithWrites++;
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

    private int seaFloorAt(int x, int z) {
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
        int lx0 = p.anchorX() + hub.minX + 12, lz0 = p.anchorZ() + hub.minZ + 12;
        // 20 x 14 spans window edges both ways (windows are 16 wide).
        lake = new int[] { lx0, lz0, lx0 + 20, lz0 + 14, floor + 8 };
        Result r;
        try {
            r = run(p, false);
        } finally {
            lake = null;
        }
        String[] open = { "c" + Cell.AIR, "c" + Cell.RAIL_NS, "c" + Cell.RAIL_EW, "c" + Cell.LADDER };
        String wall = Integer.toString(System.identityHashCode(named.get("c" + Cell.WALL)));
        int faces = 0, walled = 0;
        for (int x = lx0 - 1; x <= lx0 + 20; x++) {
            for (int z = lz0 - 1; z <= lz0 + 14; z++) {
                boolean in = x >= lx0 && z >= lz0 && x < lx0 + 20 && z < lz0 + 14;
                if (in) {
                    for (int y = floor; y < floor + ComplexRaster.LEVELS; y++) {
                        assertNull(r.cells.get(x + "," + y + "," + z), "the cut-off column keeps its terrain");
                    }
                    continue;
                }
                // A corner column only touches the lake diagonally, where water never flows.
                boolean corner = (x == lx0 - 1 || x == lx0 + 20) && (z == lz0 - 1 || z == lz0 + 14);
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
        assertTrue(faces > 100 && walled == faces, "every cell around the lake above its bed is wall: " + walled + "/" + faces);
        // The plain hub away from the lake is still open.
        String far = r.cells.get((p.anchorX() + hub.minX + 3) + "," + (floor + 2) + "," + (p.anchorZ() + hub.minZ + 3));
        assertTrue(far != null && !far.startsWith(wall + ":"), "the rest of the hub is open");
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
}
