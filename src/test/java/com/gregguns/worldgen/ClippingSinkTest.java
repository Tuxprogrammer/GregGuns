package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.Test;

/** GGS-11-08 clipping: out-of-window writes are dropped and counted, in-window writes reach the target. */
class ClippingSinkTest {

    private static final Block STONE_LIKE = new Block(Material.rock) {};

    private final RecordingWorld world = new RecordingWorld(-2, 3);
    private final ClippingSink sink = new ClippingSink(world.window(), world);

    @Test
    void inWindowWritesPassThrough() {
        // window of (-2, 3): x -24..-9, z 56..71
        assertTrue(sink.setBlock(-24, 64, 56, STONE_LIKE, 0));
        assertTrue(sink.setBlock(-9, 70, 71, STONE_LIKE, 2));
        assertTrue(sink.setTile(-10, 64, 60, null));
        assertTrue(sink.spawn(null, -9.5, 64, 60.5));
        assertEquals(4, world.writes.size());
        assertEquals(4, sink.acceptedWrites());
        assertEquals(0, sink.rejectedWrites());
        assertTrue(
            world.writesOutsideWindow()
                .isEmpty());
    }

    @Test
    void outOfWindowWritesAreDroppedAndCounted() {
        assertFalse(sink.setBlock(-25, 64, 60, STONE_LIKE, 0));
        assertFalse(sink.setBlock(-8, 64, 60, STONE_LIKE, 0));
        assertFalse(sink.setBlock(-20, 64, 55, STONE_LIKE, 0));
        assertFalse(sink.setBlock(-20, 64, 72, STONE_LIKE, 0));
        assertFalse(sink.setTile(0, 64, 0, null));
        assertFalse(sink.spawn(null, 100, 64, 100));
        assertEquals(6, sink.rejectedWrites());
        assertEquals(0, sink.acceptedWrites());
        assertTrue(world.writes.isEmpty());
    }

    @Test
    void mixedBatchCountsEachSide() {
        for (int x = -30; x <= -3; x++) {
            sink.setBlock(x, 64, 60, STONE_LIKE, 0);
        }
        // 28 columns, 16 of them (-24..-9) inside
        assertEquals(16, sink.acceptedWrites());
        assertEquals(12, sink.rejectedWrites());
        assertEquals(16, world.writes.size());
        assertTrue(
            world.writesOutsideWindow()
                .isEmpty());
    }

    @Test
    void entityPositionFloorsForNegativeCoordinates() {
        // x = -8.5 lies in block -9 (inside); x = -8.0 lies in block -8 (outside)
        assertTrue(sink.spawn(null, -8.5, 64, 60));
        assertFalse(sink.spawn(null, -8.0, 64, 60));
        // x = -24.5 is block -25 (outside), x = -24.0 is block -24 (inside)
        assertFalse(sink.spawn(null, -24.5, 64, 60));
        assertTrue(sink.spawn(null, -24.0, 64, 60));
    }

    @Test
    void recordingWorldFlagsAnUnclippedForeignWrite() {
        world.setBlock(0, 64, 0, STONE_LIKE, 0);
        assertEquals(
            1,
            world.writesOutsideWindow()
                .size());
    }

    @Test
    void recordingWorldSeparatesOwnAndForeignChunkRequests() {
        world.provideChunk(-2, 3);
        world.getChunkFromChunkCoords(-2, 3);
        assertTrue(
            world.foreignChunkRequests()
                .isEmpty());
        world.loadChunk(-1, 3);
        world.provideChunk(-2, 4);
        assertEquals(
            2,
            world.foreignChunkRequests()
                .size());
        assertEquals(4, world.chunkRequests.size());
        assertEquals("loadChunk", world.chunkRequests.get(2).method);
    }
}
