package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.Test;

/**
 * FacingKeeper: an ordinary block is written once, the window clipping and the counts are the inner sink's, and a meta
 * restore is clipped like a write but not counted. A real chest is checked in the world by
 * {@code worldgen.generator_test_layout} (its second pass changes nothing).
 */
class FacingKeeperTest {

    private static final Block PLAIN = new Block(Material.rock) {};

    @Test
    void anOrdinaryBlockIsWrittenOnceAndClippingStaysTheInnerSinks() {
        RecordingWorld world = new RecordingWorld(0, 0);
        ClippingSink inner = new ClippingSink(world.window(), world);
        FacingKeeper keeper = new FacingKeeper(inner);
        assertFalse(FacingKeeper.turnsWhenPlaced(PLAIN));
        assertTrue(keeper.setBlock(10, 64, 10, PLAIN, 3));
        assertFalse(keeper.setBlock(100, 64, 100, PLAIN, 3), "outside the window");
        assertEquals(1, world.writes.size());
        assertEquals(1, keeper.acceptedWrites());
        assertEquals(1, keeper.rejectedWrites());
        assertEquals(inner.window(), keeper.window());
        // A restore reaches the target inside the window only, and is not a counted write.
        List<String> restored = new ArrayList<>();
        ClippingSink sink = new ClippingSink(world.window(), new WindowedSink.Target() {

            @Override
            public void setBlock(int x, int y, int z, Block block, int meta) {}

            @Override
            public void restoreMeta(int x, int y, int z, Block block, int meta) {
                restored.add(x + "," + y + "," + z + ":" + meta);
            }

            @Override
            public void setTile(int x, int y, int z, net.minecraft.tileentity.TileEntity tile) {}

            @Override
            public void spawn(net.minecraft.entity.Entity entity, double x, double y, double z) {}
        });
        FacingKeeper k2 = new FacingKeeper(sink);
        assertTrue(k2.restoreMeta(9, 64, 9, PLAIN, 5));
        assertFalse(k2.restoreMeta(99, 64, 9, PLAIN, 5));
        assertEquals(java.util.Collections.singletonList("9,64,9:5"), restored);
        assertEquals(0, sink.acceptedWrites());
        assertEquals(0, sink.rejectedWrites());
    }
}
