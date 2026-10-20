package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.Test;

/** HeightProbe: window-only reads (GGS-11-08, 09) and trees are not ground (WGEN-33). */
class HeightProbeTest {

    private static final Block STONE_LIKE = new Block(Material.rock) {};
    private static final Block AIR_LIKE = new Block(Material.air) {};

    private final RecordingWorld world = new RecordingWorld(0, 0); // x, z 8..23

    @Test
    void trunksAndLeavesAreNotGround() {
        GridColumns cols = GridColumns.flat(64);
        for (int y = 65; y <= 70; y++) {
            cols.put(10, y, 10, BlockKind.WOOD);
        }
        cols.put(10, 71, 10, BlockKind.FOLIAGE);
        cols.put(11, 72, 10, BlockKind.FOLIAGE);
        HeightProbe p = new WindowedHeightProbe(world.window(), cols);
        assertEquals(64, p.landY(10, 10));
        assertEquals(64, p.landY(11, 10));
    }

    @Test
    void waterSurfaceIsNotLandAndIceIsSeenThrough() {
        GridColumns cols = new GridColumns((x, z) -> 40, (x, z) -> 62);
        cols.put(9, 63, 9, BlockKind.ICE);
        HeightProbe p = new WindowedHeightProbe(world.window(), cols);
        assertEquals(HeightProbe.NONE, p.landY(10, 10));
        assertEquals(62, p.waterSurfaceY(10, 10));
        assertEquals(40, p.seaFloorY(10, 10));
        assertEquals(62, p.waterSurfaceY(9, 9));
        // Dry land: no water surface, floor is the ground itself.
        HeightProbe dry = new WindowedHeightProbe(world.window(), GridColumns.flat(64));
        assertEquals(HeightProbe.NONE, dry.waterSurfaceY(10, 10));
        assertEquals(64, dry.seaFloorY(10, 10));
    }

    @Test
    void outsideColumnsAnswerNoneWithoutReading() {
        GridColumns cols = GridColumns.flat(64);
        HeightProbe p = new WindowedHeightProbe(world.window(), cols);
        assertEquals(HeightProbe.NONE, p.landY(7, 10));
        assertEquals(HeightProbe.NONE, p.landY(10, 24));
        assertEquals(HeightProbe.NONE, p.waterSurfaceY(24, 10));
        assertEquals(HeightProbe.NONE, p.seaFloorY(-100, -100));
        assertTrue(cols.reads.isEmpty());
        assertEquals(64, p.landY(8, 8));
        assertEquals(64, p.landY(23, 23));
    }

    /**
     * Runs every terrain operation over a footprint that sticks far out of the window on all sides and asserts that
     * no column outside the window was read and no foreign chunk was requested.
     */
    @Test
    void wholeTerrainPipelineNeverReadsOutsideTheWindow() {
        GridColumns cols = new GridColumns((x, z) -> 60 + ((x + z) & 3), (x, z) -> (x & 1) == 0 ? 64 : -1);
        HeightProbe p = new WindowedHeightProbe(world.window(), cols);
        Footprint big = new Footprint(-30, -30, 100, 60, 1);

        TerrainCheck.checkLand(p, big, 10, 4);
        TerrainCheck.checkWater(p, big, 3, 4);
        List<Flatten.ColumnEdit> edits = Flatten.plan(p, big, 2);
        Flatten.planToBand(p, big, 62, 2);
        VillageScan.containsVillageBlocks(world.window(), cols, big, 62, 8);
        for (int x = -40; x < 80; x += 3) {
            TerrainShaper.junkY(world.window(), cols, x, x);
        }
        ClippingSink sink = new ClippingSink(world.window(), world);
        TerrainShaper.apply(sink, edits, STONE_LIKE, 0, AIR_LIKE);

        assertFalse(cols.reads.isEmpty(), "the pipeline must have read something");
        assertEquals(0, cols.readsOutside(world.window()));
        assertTrue(
            world.foreignChunkRequests()
                .isEmpty());
        assertTrue(
            world.writesOutsideWindow()
                .isEmpty());
        assertEquals(0, sink.rejectedWrites());
    }
}
