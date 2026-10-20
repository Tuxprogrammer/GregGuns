package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.TerrainCheck.Reason;
import com.gregguns.worldgen.TerrainCheck.Result;

/** WGEN-05, 06, 08, 15 and GGS-11-13 with synthetic height maps. Window of chunk (0,0): x, z 8..23. */
class TerrainCheckTest {

    private final PopulateWindow w = new PopulateWindow(0, 0);

    private HeightProbe probe(GridColumns c) {
        return new WindowedHeightProbe(w, c);
    }

    @Test
    void sampleOffsetsAlwaysIncludeTheFarEdge() {
        assertArrayEquals(new int[] { 0, 4, 8, 9 }, TerrainCheck.sampleOffsets(10, 4));
        assertArrayEquals(new int[] { 0, 4, 8 }, TerrainCheck.sampleOffsets(9, 4));
        assertArrayEquals(new int[] { 0 }, TerrainCheck.sampleOffsets(1, 4));
        assertArrayEquals(new int[] { 0, 1 }, TerrainCheck.sampleOffsets(2, 4));
    }

    /** WGEN-15: a drop on the last column only was invisible when the size was not a multiple of the step. */
    @Test
    void farEdgeCliffIsDetected() {
        // size 10, step 4: Techguns sampled 0, 4, 8 and missed column 9.
        GridColumns cols = new GridColumns((x, z) -> x == 17 ? 40 : 64, (x, z) -> -1);
        Result r = TerrainCheck.checkLand(probe(cols), new PlacedFootprint(8, 8, 10, 10, 0), 3, 4);
        assertFalse(r.ok);
        assertEquals(Reason.TOO_STEEP, r.reason);
    }

    @Test
    void flatLandPassesWithMeanHeight() {
        Result r = TerrainCheck.checkLand(probe(GridColumns.flat(70)), new PlacedFootprint(10, 10, 8, 8, 0), 3, 4);
        assertTrue(r.ok);
        assertEquals(70, r.y);
        assertTrue(r.sampled > 0);
    }

    @Test
    void waterColumnFailsLandCheck() {
        GridColumns cols = new GridColumns((x, z) -> 50, (x, z) -> (x == 12 && z == 12) ? 62 : -1);
        Result r = TerrainCheck.checkLand(probe(cols), new PlacedFootprint(10, 10, 8, 8, 0), 30, 1);
        assertFalse(r.ok);
        assertEquals(Reason.LIQUID_OR_NO_GROUND, r.reason);
    }

    /** WGEN-05 / WGEN-06: the cliff lies only inside the quarter-turned box, so the check must use that box. */
    @Test
    void checkUsesTheRotatedFootprint() {
        // 20 x 10 cells. dir 0 covers z 8..17; dir 1 covers x 8..17, z 8..27. Cliff at z >= 20 (inside the window).
        GridColumns cols = new GridColumns((x, z) -> z >= 20 ? 30 : 64, (x, z) -> -1);
        assertTrue(TerrainCheck.checkLand(probe(cols), new PlacedFootprint(8, 8, 20, 10, 0), 3, 2).ok);
        Result turned = TerrainCheck.checkLand(probe(cols), new PlacedFootprint(8, 8, 20, 10, 1), 3, 2);
        assertFalse(turned.ok);
        assertEquals(Reason.TOO_STEEP, turned.reason);
    }

    @Test
    void noSampleColumnInWindowIsReportedNotGuessed() {
        Result r = TerrainCheck.checkLand(probe(GridColumns.flat(64)), new PlacedFootprint(100, 100, 8, 8, 0), 3, 4);
        assertFalse(r.ok);
        assertEquals(Reason.NO_SAMPLES, r.reason);
        assertEquals(
            Reason.NO_SAMPLES,
            TerrainCheck.checkWater(probe(GridColumns.flat(64)), new PlacedFootprint(100, 100, 8, 8, 0), 3, 4).reason);
    }

    @Test
    void shipNeedsWaterAsDeepAsTheHull() {
        // Ocean: floor 40, surface 62 -> 22 liquid blocks.
        GridColumns deep = new GridColumns((x, z) -> 40, (x, z) -> 62);
        Result ok = TerrainCheck.checkWater(probe(deep), new PlacedFootprint(8, 8, 16, 16, 0), 20, 4);
        assertTrue(ok.ok);
        assertEquals(62, ok.y);
        Result tooShallow = TerrainCheck.checkWater(probe(deep), new PlacedFootprint(8, 8, 16, 16, 0), 23, 4);
        assertFalse(tooShallow.ok);
        assertEquals(Reason.TOO_SHALLOW, tooShallow.reason);
    }

    /** WGEN-08: a submarine in an ocean biome over an island or shoal must fail. */
    @Test
    void islandOrShoalInOceanFailsShipCheck() {
        GridColumns island = new GridColumns((x, z) -> (x == 15 && z == 15) ? 66 : 40, (x, z) -> 62);
        Result r = TerrainCheck.checkWater(probe(island), new PlacedFootprint(8, 8, 16, 16, 0), 5, 1);
        assertFalse(r.ok);
        assertEquals(Reason.NOT_WATER, r.reason);

        GridColumns shoal = new GridColumns((x, z) -> (x == 15 && z == 15) ? 60 : 40, (x, z) -> 62);
        Result s = TerrainCheck.checkWater(probe(shoal), new PlacedFootprint(8, 8, 16, 16, 0), 5, 1);
        assertFalse(s.ok);
        assertEquals(Reason.TOO_SHALLOW, s.reason);
    }

    /** WGEN-06: a carrier turned a quarter reaches onto a shore only the rotated check sees. */
    @Test
    void shipChecksTheRotatedBox() {
        // Water only where z < 18. dir 0 (16 x 10) covers z 8..17; dir 1 covers z 8..23.
        GridColumns cols = new GridColumns((x, z) -> z < 18 ? 40 : 70, (x, z) -> z < 18 ? 62 : -1);
        assertTrue(TerrainCheck.checkWater(probe(cols), new PlacedFootprint(8, 8, 16, 10, 0), 5, 2).ok);
        assertFalse(TerrainCheck.checkWater(probe(cols), new PlacedFootprint(8, 8, 16, 10, 1), 5, 2).ok);
    }
}
