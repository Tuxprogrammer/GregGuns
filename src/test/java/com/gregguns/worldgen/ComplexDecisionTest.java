package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary.Type;

import org.junit.jupiter.api.Test;

/**
 * The complex decision with structure starts (D302) and the recorded level (D304): a village start over the box sinks
 * the complex whichever window decides, the complex is skipped only when it cannot sink, and the writer's variant is
 * recorded once.
 */
class ComplexDecisionTest {

    private static final long SEED = 0xC0FFEEL;
    private static final Block FILL = new Block(Material.ground) {};
    private static final Block AIR = new Block(Material.air) {};
    private static final StructurePopulator.BiomeSource PLAINS = (x, z) -> Collections.<Type>singleton(Type.PLAINS);

    /** A writer that records what the pipeline asked of it. */
    private static final class FakeWriter implements ComplexWriter {

        int sink = 15;
        int variant = 2;
        int variantCalls;
        final List<String> skips = new ArrayList<>();
        final List<Decision> written = new ArrayList<>();

        @Override
        public boolean enabledIn(World world) {
            return true;
        }

        @Override
        public int chancePercent() {
            return 100;
        }

        @Override
        public boolean biomeOk(int dimension, Set<Type> types) {
            return true;
        }

        @Override
        public int minGroundY() {
            return 30;
        }

        @Override
        public void skipped(StructurePlan plan, String reason) {
            skips.add(reason);
        }

        @Override
        public int decisionVariant(StructurePlan plan) {
            variantCalls++;
            return variant;
        }

        @Override
        public int sinkFor(StructurePlan plan, int groundY) {
            return sink;
        }

        @Override
        public int write(StructurePlan plan, Decision decision, PopulateWindow window, WindowedSink sink,
            HeightProbe probe) {
            written.add(decision);
            return 1;
        }
    }

    private final StructurePopulator populator = new StructurePopulator(null, null, null, FILL, AIR, null);

    private static StructurePlan plan() {
        for (int c = 3; c < 40; c++) {
            java.util.Optional<StructurePlan> p = StructurePopulator.complexPlanAt(SEED, 0, c, -c, 100);
            if (p.isPresent()) return p.get();
        }
        throw new AssertionError("no complex plan");
    }

    /** Every window that overlaps the box, as chunk coordinates of its populate call. */
    private static List<int[]> windowsOver(StructurePlan p) {
        List<int[]> out = new ArrayList<>();
        int x0 = Math.floorDiv(p.anchorX() - PopulateWindow.OFFSET, 16);
        int x1 = Math.floorDiv(p.anchorX() + ComplexWriter.SIZE - 1 - PopulateWindow.OFFSET, 16);
        int z0 = Math.floorDiv(p.anchorZ() - PopulateWindow.OFFSET, 16);
        int z1 = Math.floorDiv(p.anchorZ() + ComplexWriter.SIZE - 1 - PopulateWindow.OFFSET, 16);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) out.add(new int[] { x, z });
        return out;
    }

    private Decision decideFrom(int[] chunk, StructurePlan p, FakeWriter w, StructureStarts starts, ColumnReader reader,
        StructureState state) {
        RecordingWorld world = new RecordingWorld(chunk[0], chunk[1]);
        ClippingSink sink = new ClippingSink(world.window(), world);
        try (PopulateGuard.Scope ignored = PopulateGuard.enter(chunk[0], chunk[1])) {
            populator.populateComplex(p, w, world.window(), sink, reader, PLAINS, state, starts);
        }
        return state.get(PlanKey.of(p));
    }

    private static StructureStarts villageOver(StructurePlan p) {
        return (minX, minZ, maxX, maxZ) -> minX <= p.anchorX() && maxX >= p.anchorX() + ComplexWriter.SIZE - 1
            ? new StructureStarts.Blocker("village", "start chunk 1,2")
            : null;
    }

    @Test
    void aVillageStartSinksTheComplexWhicheverWindowDecides() {
        StructurePlan p = plan();
        List<int[]> windows = windowsOver(p);
        assertTrue(windows.size() >= 15 * 15, "a 240 box spans 15 or 16 windows a side");
        for (int[] chunk : windows) {
            FakeWriter w = new FakeWriter();
            Decision d = decideFrom(chunk, p, w, villageOver(p), GridColumns.flat(70), new StructureState("t"));
            assertTrue(d.isPlaced(), "the complex is kept, deeper");
            assertEquals(15, d.sink());
            assertEquals(2, d.variant());
            assertEquals(70, d.groundY());
            assertTrue(w.skips.isEmpty());
            assertEquals(1, w.written.size());
            assertEquals(d, w.written.get(0), "the writer gets the decision with its sink");
        }
    }

    @Test
    void aComplexThatCannotSinkIsSkippedForTheVillage() {
        StructurePlan p = plan();
        FakeWriter w = new FakeWriter();
        w.sink = -1;
        Decision d = decideFrom(
            windowsOver(p).get(0),
            p,
            w,
            villageOver(p),
            GridColumns.flat(70),
            new StructureState("t"));
        assertFalse(d.isPlaced());
        assertEquals("village", d.reason());
        assertEquals(Collections.singletonList("village"), w.skips);
        assertTrue(w.written.isEmpty(), "nothing is written");
    }

    @Test
    void withNoStartTheComplexStaysAtItsDepthAndRecordsItsVariantOnce() {
        StructurePlan p = plan();
        FakeWriter w = new FakeWriter();
        StructureState state = new StructureState("t");
        List<int[]> windows = windowsOver(p);
        for (int i = 0; i < 5; i++) decideFrom(windows.get(i), p, w, StructureStarts.NONE, GridColumns.flat(70), state);
        Decision d = state.get(PlanKey.of(p));
        assertEquals(0, d.sink());
        assertEquals(2, d.variant());
        assertEquals(1, w.variantCalls, "the variant is rolled at the decision only");
        w.variant = 0;
        decideFrom(windows.get(6), p, w, StructureStarts.NONE, GridColumns.flat(70), state);
        assertEquals(
            2,
            w.written.get(w.written.size() - 1)
                .variant(),
            "a later window keeps the recorded variant");
    }

    @Test
    void villageBlocksInTheDecidingWindowNoLongerRejectAComplex() {
        StructurePlan p = plan();
        int[] chunk = windowsOver(p).get(20);
        PopulateWindow win = new PopulateWindow(chunk[0], chunk[1]);
        GridColumns reader = GridColumns.flat(70);
        for (int x = win.minX(); x <= win.maxX(); x++) {
            for (int z = win.minZ(); z <= win.maxZ(); z++) reader.put(x, 71, z, BlockKind.VILLAGE);
        }
        Decision d = decideFrom(chunk, p, new FakeWriter(), StructureStarts.NONE, reader, new StructureState("t"));
        assertTrue(d.isPlaced(), "the start rule, not a window's blocks, speaks for villages");
    }

    @Test
    void aStartsSourceThatMissesTheBoxChangesNothing() {
        StructurePlan p = plan();
        StructureStarts far = (minX, minZ, maxX, maxZ) -> maxX < -1_000_000 ? new StructureStarts.Blocker("village", "")
            : null;
        Decision d = decideFrom(
            windowsOver(p).get(0),
            p,
            new FakeWriter(),
            far,
            GridColumns.flat(70),
            new StructureState("t"));
        assertTrue(d.isPlaced());
        assertEquals(0, d.sink());
    }
}
