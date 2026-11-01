package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;

/**
 * Site preparation (D351): a level column ends solid from the ground Y down to natural ground and clear above it, the
 * bank is cut one to one, liquid and built blocks in the bank stay, and nothing outside the window is read or written.
 */
class SitePrepTest {

    private static final Block SOIL = new Block(Material.ground) {};
    private static final Block DEEP = new Block(Material.rock) {};
    private static final Block AIR = new Block(Material.air) {};
    /** Window of chunk (0, 0): x and z 8..23. */
    private static final PopulateWindow WINDOW = new PopulateWindow(0, 0);
    private static final int REF = 66;

    /** Records the last block written at each position. */
    private static final class Writes implements WindowedSink.Target {

        final Map<String, Block> at = new HashMap<>();

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            at.put(x + "," + y + "," + z, block);
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {}

        @Override
        public void spawn(Entity entity, double x, double y, double z) {}

        Block get(int x, int y, int z) {
            return at.get(x + "," + y + "," + z);
        }
    }

    private static final class Run {

        final GridColumns cols;
        final Writes writes = new Writes();
        final ClippingSink sink = new ClippingSink(WINDOW, writes);
        int n;

        Run(GridColumns cols) {
            this.cols = cols;
        }

        Run apply(int minX, int minZ, int maxX, int maxZ, SitePrep.Columns extra) {
            n = SitePrep.apply(sink, cols, minX, minZ, maxX, maxZ, REF, extra, SOIL, DEEP, AIR);
            return this;
        }

        /** The block kind after the run: a write wins over the original column. */
        BlockKind after(int x, int y, int z) {
            Block b = writes.get(x, y, z);
            if (b == null) return cols.kindAt(x, y, z);
            return b == AIR ? BlockKind.AIR : BlockKind.GROUND;
        }

        int topSolid(int x, int z) {
            for (int y = 140; y >= 0; y--) {
                if (after(x, y, z) != BlockKind.AIR) return y;
            }
            return -1;
        }
    }

    /** A slope falling 1 block per 2 east, from 74 at x 8 to 67 at x 23: the box spans both sides of REF. */
    private static GridColumns slope() {
        return new GridColumns((x, z) -> 78 - x / 2, (x, z) -> -1);
    }

    @Test
    void slopedFootprintEndsLevelAtTheGroundY() {
        // Falls 1 block per column east: 70 at x 10 (4 over REF) to 60 at x 20 (6 under).
        GridColumns cols = new GridColumns((x, z) -> 80 - x, (x, z) -> -1);
        Run r = new Run(cols).apply(10, 10, 20, 20, SitePrep.NONE);
        for (int x = 10; x <= 20; x++) {
            for (int z = 10; z <= 20; z++) {
                int natural = 80 - x;
                assertEquals(REF, r.topSolid(x, z), "top at " + x + "," + z);
                for (int y = Math.min(natural, REF); y <= REF; y++) {
                    assertTrue(r.after(x, y, z) != BlockKind.AIR, "hole under the site at " + x + "," + y + "," + z);
                }
            }
        }
        // The low east side is filled soil first, then deep, down to the natural ground.
        assertEquals(SOIL, r.writes.get(20, REF, 15));
        assertEquals(SOIL, r.writes.get(20, REF - SitePrep.SOIL_DEPTH + 1, 15));
        assertEquals(DEEP, r.writes.get(20, REF - SitePrep.SOIL_DEPTH, 15));
        assertEquals(DEEP, r.writes.get(20, 61, 15));
        assertEquals(null, r.writes.get(20, 60, 15), "natural ground is not rewritten");
        // The high west side is cut, and the bank beyond it climbs one block per column.
        assertEquals(REF + 1, r.topSolid(9, 15));
        assertEquals(REF + 2, r.topSolid(8, 15));
    }

    @Test
    void buriedFloorGetsAFoundationOverACave() {
        // Flat ground at REF with a cave from 50 to 55 under one column; the structure's lowest layer is at 57.
        GridColumns cols = GridColumns.flat(REF);
        for (int y = 50; y <= 55; y++) cols.put(14, y, 14, BlockKind.AIR);
        cols.put(15, 56, 15, BlockKind.AIR); // a pocket right under the floor of another column
        int n = SitePrep.apply(
            new ClippingSink(WINDOW, new Writes()),
            cols,
            12,
            12,
            18,
            18,
            REF,
            57,
            SitePrep.NONE,
            new SitePrep.Own(),
            SOIL,
            DEEP,
            AIR);
        assertEquals(1, n, "only the pocket touching the floor is filled; rock under the floor stops the fill");
        Writes w = new Writes();
        GridColumns open = GridColumns.flat(40);
        SitePrep.apply(
            new ClippingSink(WINDOW, w),
            open,
            12,
            12,
            18,
            18,
            REF,
            57,
            SitePrep.NONE,
            new SitePrep.Own(),
            SOIL,
            DEEP,
            AIR);
        assertEquals(DEEP, w.get(14, 56, 14), "a void under the floor is filled with the deep block");
        assertEquals(DEEP, w.get(14, 41, 14), "down to the natural ground at 40");
        assertEquals(null, w.get(14, 40, 14));
        assertEquals(null, w.get(19, 56, 14), "the bank gets no buried foundation");
    }

    /** The structure's own cells are left to it, so a pass over a finished structure clears and fills nothing. */
    @Test
    void theStructuresOwnCellsAreLeftToIt() {
        GridColumns cols = new GridColumns((x, z) -> 60, (x, z) -> -1);
        SitePrep.Own own = new SitePrep.Own();
        // A wall the structure writes from the ground Y up, and a cell of its floor under the ground Y.
        for (int y = REF; y <= REF + 4; y++) {
            own.setBlock(14, y, 14, SOIL, 0);
            cols.put(14, y, 14, BlockKind.SOLID);
        }
        own.setBlock(15, REF, 15, SOIL, 0);
        Writes w = new Writes();
        SitePrep
            .apply(new ClippingSink(WINDOW, w), cols, 12, 12, 18, 18, REF, REF, SitePrep.NONE, own, SOIL, DEEP, AIR);
        for (int y = REF; y <= REF + 4; y++) {
            assertEquals(null, w.get(14, y, 14), "the wall is not cleared at " + y);
        }
        assertEquals(null, w.get(15, REF, 15), "the structure's own floor cell is not filled");
        assertEquals(SOIL, w.get(15, REF - 1, 15), "the fill goes on under it");
        assertEquals(SOIL, w.get(16, REF, 16), "other cells are filled");
    }

    @Test
    void flatGroundAtTheGroundYWritesNothing() {
        Run r = new Run(GridColumns.flat(REF)).apply(10, 10, 20, 20, SitePrep.NONE);
        assertEquals(0, r.n);
        assertTrue(r.writes.at.isEmpty());
    }

    @Test
    void foundationStopsAtTheCap() {
        Run r = new Run(GridColumns.flat(20)).apply(10, 10, 20, 20, SitePrep.NONE);
        assertEquals(DEEP, r.writes.get(12, REF - SitePrep.FILL_CAP + 1, 12));
        assertEquals(null, r.writes.get(12, REF - SitePrep.FILL_CAP, 12), "a ravine deeper than the cap stays open");
    }

    @Test
    void bankIsCutOneToOneAndStopsAtItsWidth() {
        Run r = new Run(GridColumns.flat(90)).apply(12, 12, 18, 18, SitePrep.NONE);
        assertEquals(REF, r.topSolid(15, 15), "the footprint is cut to the ground Y");
        for (int d = 1; d <= SitePrep.BANK; d++) {
            assertEquals(REF + d, r.topSolid(18 + d, 15), "east bank at distance " + d);
            assertEquals(REF + d, r.topSolid(15, 12 - d), "north bank at distance " + d);
            assertEquals(REF + d, r.topSolid(18 + d, 12 - d), "corner bank at distance " + d);
        }
        assertEquals(90, r.topSolid(18 + SitePrep.BANK + 1, 15), "past the bank the hill is untouched");
        // A bank column is never filled: low ground beside the box stays low.
        Run low = new Run(GridColumns.flat(50)).apply(12, 12, 18, 18, SitePrep.NONE);
        assertEquals(50, low.topSolid(19, 15));
        assertEquals(REF, low.topSolid(18, 15));
    }

    @Test
    void bankKeepsLiquidAndBuiltBlocksButTheFootprintClearsEverything() {
        // Ground 60, water up to 75 everywhere: a pond the terrain check did not sample.
        GridColumns cols = new GridColumns((x, z) -> 60, (x, z) -> 75);
        cols.put(20, 80, 15, BlockKind.SOLID); // a built block on top of the water, in the bank
        cols.put(15, 70, 15, BlockKind.VILLAGE); // planks in the footprint
        Run r = new Run(cols).apply(12, 12, 18, 18, SitePrep.NONE);
        for (int y = REF + 1; y <= 80; y++) {
            assertEquals(BlockKind.AIR, r.after(15, y, 15), "footprint clear at " + y);
        }
        for (int y = 61; y <= REF; y++) {
            assertEquals(BlockKind.GROUND, r.after(15, y, 15), "pond filled at " + y);
        }
        assertEquals(BlockKind.SOLID, r.after(20, 80, 15), "the built block in the bank stays");
        assertEquals(BlockKind.LIQUID, r.after(19, 75, 15), "bank water stays");
        assertEquals(BlockKind.LIQUID, r.after(19, REF + 2, 15));
    }

    @Test
    void trunksAndPlantsUnderTheGroundYAreFilledOver() {
        GridColumns cols = GridColumns.flat(62);
        cols.put(14, 63, 14, BlockKind.WOOD)
            .put(14, 64, 14, BlockKind.WOOD)
            .put(14, 65, 14, BlockKind.FOLIAGE)
            .put(14, 66, 14, BlockKind.ICE)
            .put(14, 67, 14, BlockKind.WOOD);
        Run r = new Run(cols).apply(12, 12, 18, 18, SitePrep.NONE);
        for (int y = 63; y <= REF; y++) {
            assertEquals(SOIL, r.writes.get(14, y, 14), "filled at " + y);
        }
        assertEquals(AIR, r.writes.get(14, 67, 14), "the trunk above the ground Y is cleared");
    }

    @Test
    void extraColumnsAreLevelLikeTheFootprint() {
        // A road tail east of the box, over a slope above the ground Y, and one through a hill's bank.
        Run r = new Run(slope()).apply(10, 10, 16, 16, (x, z) -> x > 16 && z == 13);
        assertEquals(REF, r.topSolid(18, 13), "the tail is level");
        assertTrue(r.after(18, REF, 13) != BlockKind.AIR);
        Run hill = new Run(GridColumns.flat(80)).apply(10, 10, 16, 16, (x, z) -> x > 16 && z == 13);
        assertEquals(REF, hill.topSolid(19, 13), "the tail is cut through the bank");
        assertEquals(REF + 3, hill.topSolid(19, 14), "beside it the bank stands");
    }

    @Test
    void neverReadsOrWritesOutsideTheWindow() {
        GridColumns cols = slope();
        Run r = new Run(cols).apply(-20, -20, 60, 60, (x, z) -> true);
        assertTrue(r.n > 0);
        assertEquals(0, cols.readsOutside(WINDOW));
        assertEquals(0, r.sink.rejectedWrites());
        for (String k : r.writes.at.keySet()) {
            String[] p = k.split(",");
            assertTrue(WINDOW.contains(Integer.parseInt(p[0]), Integer.parseInt(p[2])), "write at " + k);
        }
    }
}
