package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                    return NONE;
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
}
