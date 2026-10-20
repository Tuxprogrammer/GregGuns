package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.Test;

/** WGEN-16 (median), WGEN-26 (no water drain), junk removal and the band clamp. */
class FlattenTest {

    private static final Block FILL = new Block(Material.rock) {};
    private static final Block AIR_LIKE = new Block(Material.air) {};

    private final RecordingWorld world = new RecordingWorld(0, 0);

    @Test
    void medianIsTheRealMedian() {
        assertEquals(5.0, Flatten.median(new int[] { 5 }));
        assertEquals(65.0, Flatten.median(new int[] { 60, 70 }));
        assertEquals(64.0, Flatten.median(new int[] { 70, 64, 60 }));
        assertEquals(65.0, Flatten.median(new int[] { 70, 60, 64, 66 }));
    }

    /** WGEN-16: with two columns the Techguns median stayed 0 and the site was dug down to y near 0. */
    @Test
    void twoColumnFlattenStaysNearTheMedian() {
        int[] out = Flatten.flatten(new int[] { 60, 70 }, 2);
        assertArrayEquals(new int[] { 64, 66 }, out);
        int[] one = Flatten.flatten(new int[] { 80 }, 0);
        assertArrayEquals(new int[] { 80 }, one);
    }

    @Test
    void flattenKeepsEveryColumnInsideTheBand() {
        int[] h = { 50, 55, 62, 70, 90, 61, 63 };
        double med = Flatten.median(h);
        for (int v : Flatten.flatten(h, 4)) {
            assertTrue(v >= med - 2.5 && v <= med + 2.5, "value " + v + " median " + med);
        }
    }

    @Test
    void smallSpanIsUnchanged() {
        int[] h = { 64, 65, 66 };
        assertArrayEquals(h, Flatten.flatten(h, 2));
    }

    @Test
    void bandClampUsesTheRecordedReference() {
        assertEquals(63, Flatten.clampToBand(40, 64, 2));
        assertEquals(65, Flatten.clampToBand(90, 64, 2));
        assertEquals(64, Flatten.clampToBand(64, 64, 2));
    }

    /** WGEN-26: a lake column inside the footprint gets no edit and no write; its neighbors still flatten. */
    @Test
    void flattenNeverTouchesWater() {
        // Ground rises with x; columns at x == 12 are under water.
        GridColumns cols = new GridColumns((x, z) -> 50 + x, (x, z) -> x == 12 ? 90 : -1);
        HeightProbe p = new WindowedHeightProbe(world.window(), cols);
        Footprint fp = new Footprint(10, 10, 6, 6, 0);
        List<Flatten.ColumnEdit> edits = Flatten.plan(p, fp, 2);
        assertFalse(edits.isEmpty());
        for (Flatten.ColumnEdit e : edits) {
            assertTrue(e.x != 12, "edit planned on a water column");
        }
        ClippingSink sink = new ClippingSink(world.window(), world);
        TerrainShaper.apply(sink, edits, FILL, 0, AIR_LIKE);
        for (RecordingWorld.Write wr : world.writes) {
            assertTrue(wr.x != 12, "write on a water column");
        }
        assertEquals(0, sink.rejectedWrites());
    }

    @Test
    void applyRaisesAndLowers() {
        List<Flatten.ColumnEdit> edits = Flatten.planToBand(
            new WindowedHeightProbe(world.window(), new GridColumns((x, z) -> x == 10 ? 70 : 58, (x, z) -> -1)),
            new Footprint(10, 10, 2, 1, 0),
            64,
            2);
        // x=10: 70 -> 65 (lower, write y 70..66); x=11: 58 -> 63 (raise, write y 59..63)
        ClippingSink sink = new ClippingSink(world.window(), world);
        TerrainShaper.apply(sink, edits, FILL, 0, AIR_LIKE);
        assertEquals(5 + 5, world.writes.size());
    }

    /** WGEN-26: junk removal clears trees and plants, never water, and leaves a lake column alone. */
    @Test
    void junkIsFoliageAndWoodOnly() {
        GridColumns cols = GridColumns.flat(64);
        cols.put(10, 65, 10, BlockKind.WOOD)
            .put(10, 66, 10, BlockKind.WOOD)
            .put(10, 67, 10, BlockKind.FOLIAGE)
            .put(11, 65, 10, BlockKind.FOLIAGE);
        assertArrayEquals(new int[] { 67, 66, 65 }, TerrainShaper.junkY(world.window(), cols, 10, 10));
        assertArrayEquals(new int[] { 65 }, TerrainShaper.junkY(world.window(), cols, 11, 10));
        assertEquals(0, TerrainShaper.junkY(world.window(), cols, 12, 10).length);

        // A water column with a lily pad on it: nothing removed.
        GridColumns lake = new GridColumns((x, z) -> 50, (x, z) -> 62);
        lake.put(10, 63, 10, BlockKind.FOLIAGE);
        assertEquals(0, TerrainShaper.junkY(world.window(), lake, 10, 10).length);
        // Outside the window: nothing.
        assertEquals(0, TerrainShaper.junkY(world.window(), cols, 100, 100).length);
    }
}
