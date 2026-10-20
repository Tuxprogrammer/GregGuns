package com.gregguns.worldgen.nest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.RecordingWorld;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.PaletteKeys;

/** The placer writes only inside each window, requests no foreign chunk, and the windows add up to one nest. */
class NestPlacerTest {

    private static final int AX = 1003;
    private static final int AZ = -3001;
    private static final int GROUND = 66;

    private static final class Blocks implements PaletteKeys.BlockLookup {

        final Map<String, Block> byName = new HashMap<>();

        @Override
        public Block byName(String name) {
            return byName.computeIfAbsent(name, n -> new Block(Material.rock) {});
        }
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
        public void setTile(int x, int y, int z, TileEntity tile) {
            world.setTile(x, y, z, tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            world.spawn(entity, x, y, z);
        }
    }

    private final Blocks blocks = new Blocks();
    private final PaletteKeys keys = PaletteKeys.resolve(null, blocks, s -> {});
    private final Block air = new Block(Material.air) {};

    private static final class Result {

        final Map<String, String> cells = new HashMap<>();
        final List<String> marks = new ArrayList<>();
        int outside;
        int rejected;
        int foreign;
        int duplicates;
        int windowsWithWrites;
    }

    private Result run(NestPlan plan, boolean reverse) {
        NestPlacer placer = new NestPlacer(keys, air);
        Result r = new Result();
        List<int[]> order = new ArrayList<>();
        for (int cx = Math.floorDiv(AX - 8, 16) - 1; cx <= Math.floorDiv(AX + NestPlan.SIZE - 8, 16) + 1; cx++) {
            for (int cz = Math.floorDiv(AZ - 8, 16) - 1; cz <= Math.floorDiv(AZ + NestPlan.SIZE - 8, 16) + 1; cz++) {
                order.add(new int[] { cx, cz });
            }
        }
        if (reverse) Collections.reverse(order);
        for (int[] c : order) {
            RecordingWorld world = new RecordingWorld(c[0], c[1]);
            Capture cap = new Capture(world, r.cells);
            ClippingSink sink = new ClippingSink(world.window(), cap);
            int n = placer
                .write(sink, plan, world.window(), (tag, x, y, z) -> r.marks.add(tag + "@" + x + "," + y + "," + z));
            if (n > 0) r.windowsWithWrites++;
            r.outside += world.writesOutsideWindow()
                .size();
            r.rejected += sink.rejectedWrites();
            r.foreign += world.foreignChunkRequests()
                .size();
            r.duplicates += cap.duplicates;
        }
        return r;
    }

    @Test
    void everyWriteIsInsideItsWindowAndNoChunkIsRequested() {
        NestPlan plan = NestPlan.generate(0xBEEFL, AX, AZ, GROUND);
        Result r = run(plan, false);
        assertEquals(0, r.outside, "GGS-11-08");
        assertEquals(0, r.rejected, "the placer never offers a write the sink must drop");
        assertEquals(0, r.foreign, "GGS-11-09");
        assertEquals(0, r.duplicates, "windows tile, so no cell is written twice");
        assertTrue(r.windowsWithWrites > 9, "a 61 wide nest spans many windows: " + r.windowsWithWrites);
    }

    @Test
    void windowOrderDoesNotChangeTheResultAndMarksMatchTheHoles() {
        NestPlan plan = NestPlan.generate(0xBEEFL, AX, AZ, GROUND);
        Result fwd = run(plan, false);
        Result back = run(plan, true);
        assertEquals(fwd.cells, back.cells);
        assertEquals(
            plan.spawners()
                .size(),
            fwd.marks.size(),
            "one Bug Hole mark per chamber, no duplicates");
        for (int[] h : plan.spawners()) {
            assertTrue(fwd.marks.contains(NestPlacer.BUG_HOLE_TAG + "@" + h[0] + "," + h[1] + "," + h[2]));
        }
    }

    @Test
    void theUnionOfWindowsIsExactlyTheNonEmptyCells() {
        NestPlan plan = NestPlan.generate(31L, AX, AZ, GROUND);
        Result r = run(plan, false);
        int expected = 0;
        for (int x = AX; x < AX + NestPlan.SIZE; x++) {
            for (int z = AZ; z < AZ + NestPlan.SIZE; z++) {
                for (int y = plan.minY(); y <= plan.maxY(); y++) {
                    if (plan.cell(x, y, z) != NestPlan.NONE) expected++;
                }
            }
        }
        assertEquals(expected, r.cells.size());
    }
}
