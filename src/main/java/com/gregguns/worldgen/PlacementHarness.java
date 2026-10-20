package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.utility.storage.BlockGregChest;
import com.gregguns.utility.storage.TileGregChest;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Rotations;
import com.gregguns.worldgen.layout.StampPlan;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LootPending;

/**
 * Shared setup of the per-structure placement scenarios (M8 S1 to S3): a forced {@link StructurePlan} at a prepared
 * flat (or flooded) test area, run through the real {@link StructurePopulator} one populate window at a time, with
 * counting hooks for the spawner and flag marks and a dry-run recording of what the stamper should have written.
 *
 * <p>
 * The plan is forced, not drawn from the grid, so the test does not depend on where the seeded grid puts a structure;
 * the plan fields (cell, variant, chest seed base) are fixed constants, so every run builds the same thing. The test
 * area is far from spawn, rebuilt from scratch before each run and restored by {@link Placement#cleanup}. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-08 to GGS-11-10, GGS-11-20); PLAN-M8 section 3
public final class PlacementHarness {

    /** The fixed world seed handed to the populator (the populator itself ignores it when given plans). */
    public static final long SEED = 0x5EED6A09E667L;
    /** Top ground block of the flat land area. The anchor is this + 1 (PLAN-M8: decided ground Y + 1). */
    public static final int LAND_Y = 63;
    /** Top water block of the flooded area (sea level minus one). */
    public static final int WATER_SURFACE_Y = 62;
    /** Top seabed block under the water (22 deep, well over the 6 a hull needs). */
    public static final int WATER_FLOOR_Y = 40;
    /** Blocks of prepared terrain around the footprint, so a stray write next to it is seen. */
    public static final int MARGIN = 3;

    private static PaletteKeys keys;
    private static LayoutRegistry layouts;

    private PlacementHarness() {}

    static synchronized PaletteKeys keys() {
        if (keys == null) keys = PaletteKeys.resolveForPack(s -> {});
        return keys;
    }

    /** The real shipped layouts, loaded once. */
    static synchronized LayoutRegistry layouts() {
        if (layouts == null) {
            List<String> errors = new ArrayList<>();
            layouts = LayoutRegistry.load(
                new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
                StructureCatalog.idsToLoad(s -> s != StructureId.ALIEN_BUG_NEST && s != StructureId.MILITARY_BASE),
                keys(),
                errors::add);
            if (!errors.isEmpty()) throw new IllegalStateException("layouts: " + errors);
        }
        return layouts;
    }

    /** A populator whose grid is empty: it places nothing by itself, and stands in for the real one during prep. */
    static StructurePopulator populator() {
        Map<SizeClass, List<PoolEntry>> none = new EnumMap<>(SizeClass.class);
        return new StructurePopulator(
            new StructureGrid(16, 32, 64, none),
            layouts(),
            new Stamper(keys()),
            Blocks.dirt,
            Blocks.air,
            StructurePopulator.CONFIG_RULES);
    }

    /**
     * The forced plan of a structure: a fixed area per structure, an anchor that is not window-aligned (so even the
     * small ones cross a window border), the given rotation, a fixed variant and chest seed base.
     */
    static StructurePlan plan(World world, StructureId id, int rotation) {
        Layout l = layouts().get(id.id());
        int i = id.ordinal();
        return new StructurePlan(
            id.id(),
            StructureCatalog.sizeClass(id),
            world.provider.dimensionId,
            1000 + i,
            1000,
            20000 + i * 512 + 5,
            20000 + 3,
            rotation,
            3,
            new Footprint(l.sizeX(), l.sizeY(), l.sizeZ()),
            0x600DC0FFEEL + i);
    }

    /** Chest and mark tallies of a layout, counted from its marks. */
    static final class MarkCounts {

        int vanilla;
        int weapon;
        int reinforced;
        int spawners;
        int flags;

        int chests() {
            return vanilla + weapon + reinforced;
        }
    }

    static MarkCounts countMarks(Layout l) {
        MarkCounts c = new MarkCounts();
        for (Layout.Mark m : l.marks()) {
            if (m.tag.equals("chest.vanilla")) c.vanilla++;
            else if (m.tag.equals("chest.weapon")) c.weapon++;
            else if (m.tag.equals("chest.reinforced")) c.reinforced++;
            else if (m.tag.startsWith("spawner")) c.spawners++;
            else if (m.tag.equals("flag")) c.flags++;
        }
        return c;
    }

    /** World position of a layout mark under the plan, by the stamper's own rotation tables. */
    static int[] markPos(StructurePlan p, Layout l, Layout.Mark m, int anchorY) {
        return new int[] { p.anchorX() + Rotations.offsetX(p.rotation(), m.x, m.z, l.sizeX(), l.sizeZ()),
            anchorY + m.y - l.ground(), p.anchorZ() + Rotations.offsetZ(p.rotation(), m.x, m.z, l.sizeX(), l.sizeZ()) };
    }

    /** What one run left behind, and everything needed to judge and undo it. */
    static final class Placement {

        final World world;
        final StructurePlan plan;
        final Layout layout;
        final boolean water;
        final int anchorY;
        final Decision decision;
        final int windows;
        final int stamped;
        final int rejectedWrites;
        final int acceptedWrites;
        final int guardViolations;
        final List<String> spawnerTags = new ArrayList<>();
        final List<int[]> spawnerPos = new ArrayList<>();
        final List<int[]> flagPos = new ArrayList<>();
        /** Layout-derived expectation: every non-air block the stamper should write. */
        final Map<Long, Block> expected = new HashMap<>();
        final long millis;

        Placement(World world, StructurePlan plan, Layout layout, boolean water, Decision decision, int windows,
            int stamped, int rejected, int accepted, int violations, long millis) {
            this.world = world;
            this.plan = plan;
            this.layout = layout;
            this.water = water;
            this.decision = decision;
            this.anchorY = decision.isPlaced() ? decision.groundY() + 1 : -1;
            this.windows = windows;
            this.stamped = stamped;
            this.rejectedWrites = rejected;
            this.acceptedWrites = accepted;
            this.guardViolations = violations;
            this.millis = millis;
        }

        int minY() {
            return anchorY - layout.ground();
        }

        int maxY() {
            return minY() + layout.sizeY() - 1;
        }

        /** Non-air blocks of the rotated box at or above the anchor layer (the part above the prepared terrain). */
        int footprintNonAir() {
            int n = 0;
            for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                    for (int y = anchorY; y <= maxY(); y++) {
                        if (world.getBlock(x, y, z) != Blocks.air) n++;
                    }
                }
            }
            return n;
        }

        /** Non-air cells the dry run expects at or above the anchor layer. */
        int expectedNonAir() {
            int n = 0;
            for (Long k : expected.keySet()) {
                if (yOf(k) >= anchorY) n++;
            }
            return n;
        }

        /**
         * Number of expected cells (any layer) whose world block differs; the first one is described in {@code why}.
         */
        int mismatches(StringBuilder why) {
            int bad = 0;
            for (Map.Entry<Long, Block> e : expected.entrySet()) {
                long k = e.getKey();
                Block got = world.getBlock(xOf(k), yOf(k), zOf(k));
                boolean swapped = e.getValue() == Blocks.chest && got instanceof BlockGregChest;
                if (got != e.getValue() && !swapped) {
                    if (bad == 0) {
                        why.append(xOf(k))
                            .append(',')
                            .append(yOf(k))
                            .append(',')
                            .append(zOf(k))
                            .append(" wanted ")
                            .append(Block.blockRegistry.getNameForObject(e.getValue()))
                            .append(" got ")
                            .append(Block.blockRegistry.getNameForObject(got));
                    }
                    bad++;
                }
            }
            return bad;
        }

        /** Non-air blocks in the margin ring around the box, above the terrain: a stray write shows up here. */
        int strayBlocks() {
            int n = 0;
            int top = maxY() + 3;
            for (int x = plan.anchorX() - MARGIN; x <= plan.maxX() + MARGIN; x++) {
                for (int z = plan.anchorZ() - MARGIN; z <= plan.maxZ() + MARGIN; z++) {
                    boolean inBox = x >= plan.anchorX() && x <= plan.maxX() && z >= plan.anchorZ() && z <= plan.maxZ();
                    if (inBox) continue;
                    for (int y = anchorY; y <= top; y++) {
                        if (world.getBlock(x, y, z) != Blocks.air) n++;
                    }
                }
            }
            return n;
        }

        /** Item entities around the box. */
        int itemEntities() {
            AxisAlignedBB bb = AxisAlignedBB
                .getBoundingBox(plan.anchorX() - 8, 0, plan.anchorZ() - 8, plan.maxX() + 9, 256, plan.maxZ() + 9);
            List<?> items = world.getEntitiesWithinAABB(EntityItem.class, bb);
            int live = 0;
            for (Object o : items) {
                if (!((Entity) o).isDead) live++;
            }
            return live;
        }

        /** Chest-class tiles (vanilla or GregGuns) inside the box, from the chunk tile maps (no tile is created). */
        List<TileEntity> chestTiles() {
            List<TileEntity> out = new ArrayList<>();
            for (TileEntity t : tilesInBox()) {
                if (t instanceof TileEntityChest || t instanceof TileGregChest) out.add(t);
            }
            return out;
        }

        List<TileEntity> tilesInBox() {
            List<TileEntity> out = new ArrayList<>();
            for (int cx = Math.floorDiv(plan.anchorX(), 16); cx <= Math.floorDiv(plan.maxX(), 16); cx++) {
                for (int cz = Math.floorDiv(plan.anchorZ(), 16); cz <= Math.floorDiv(plan.maxZ(), 16); cz++) {
                    Chunk c = world.getChunkFromChunkCoords(cx, cz);
                    for (Object o : c.chunkTileEntityMap.values()) {
                        TileEntity t = (TileEntity) o;
                        if (t.xCoord >= plan.anchorX() && t.xCoord <= plan.maxX()
                            && t.zCoord >= plan.anchorZ()
                            && t.zCoord <= plan.maxZ()) {
                            out.add(t);
                        }
                    }
                }
            }
            return out;
        }

        /** Every block id and meta in the rotated box, buried layers included, then the chest contents and orders. */
        Snapshot snapshot() {
            int sy = layout.sizeY();
            int[] cells = new int[plan.sizeX() * plan.sizeZ() * sy];
            int i = 0;
            for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                    for (int y = minY(); y <= maxY(); y++) {
                        cells[i++] = Block.getIdFromBlock(world.getBlock(x, y, z)) << 4
                            | world.getBlockMetadata(x, y, z);
                    }
                }
            }
            List<String> chests = new ArrayList<>();
            for (Layout.Mark m : layout.marks()) {
                if (!m.tag.startsWith("chest.")) continue;
                int[] p = markPos(plan, layout, m, anchorY);
                TileEntity t = world.getTileEntity(p[0], p[1], p[2]);
                chests.add(m.tag + "@" + p[0] + "," + p[1] + "," + p[2] + "=" + describe(t));
            }
            return new Snapshot(cells, chests);
        }

        private static String describe(TileEntity t) {
            if (t instanceof TileGregChest) {
                LootPending lp = ((TileGregChest) t).pending();
                return "pending " + (lp == null ? "none" : lp.category() + "/" + lp.richness() + "/" + lp.seed());
            }
            if (t instanceof TileEntityChest) {
                StringBuilder sb = new StringBuilder("items");
                TileEntityChest c = (TileEntityChest) t;
                for (int s = 0; s < c.getSizeInventory(); s++) {
                    ItemStack st = c.getStackInSlot(s);
                    if (st == null) continue;
                    sb.append(' ')
                        .append(s)
                        .append(':')
                        .append(st.getUnlocalizedName())
                        .append('/')
                        .append(st.getItemDamage())
                        .append('x')
                        .append(st.stackSize);
                }
                return sb.toString();
            }
            return String.valueOf(t);
        }

        /** Rebuilds the prepared terrain over the whole area: placed blocks and chest tiles go, nothing is dropped. */
        void cleanup() {
            // Empty the chests first: breaking a chest block drops its contents as item entities.
            for (TileEntity t : chestTiles()) {
                if (t instanceof TileGregChest) ((TileGregChest) t).setPending(null);
                IInventory inv = (IInventory) t;
                for (int s = 0; s < inv.getSizeInventory(); s++) {
                    inv.setInventorySlotContents(s, null);
                }
            }
            prepare(world, plan, water, layout);
            // Rebuilding the terrain replaces every block, which also removes its tile; any item entity is swept.
            AxisAlignedBB bb = AxisAlignedBB
                .getBoundingBox(plan.anchorX() - 8, 0, plan.anchorZ() - 8, plan.maxX() + 9, 256, plan.maxZ() + 9);
            for (Object o : world.getEntitiesWithinAABB(Entity.class, bb)) {
                Entity e = (Entity) o;
                if (e instanceof EntityItem) e.setDead();
            }
        }
    }

    /** Block cells and chest descriptions of one run, for the same-seed comparison. */
    static final class Snapshot {

        final int[] cells;
        final List<String> chests;

        Snapshot(int[] cells, List<String> chests) {
            this.cells = cells;
            this.chests = chests;
        }

        /**
         * @param ignoreChestFacing a vanilla chest turns toward its neighbors as it is placed, so across a window border its
         *                          facing depends on which window was stamped first; compare only its presence then
         * @return null when equal, else a description of the first difference
         */
        String diff(Snapshot other, boolean ignoreChestFacing) {
            if (cells.length != other.cells.length) return "size " + cells.length + " vs " + other.cells.length;
            for (int i = 0; i < cells.length; i++) {
                int a = cells[i];
                int b = other.cells[i];
                if (ignoreChestFacing && a >> 4 == Block.getIdFromBlock(Blocks.chest)) {
                    a &= ~0xF;
                    b &= ~0xF;
                }
                if (a != b) {
                    return "cell " + i
                        + ": "
                        + Integer.toHexString(cells[i])
                        + " vs "
                        + Integer.toHexString(other.cells[i]);
                }
            }
            if (!chests.equals(other.chests)) {
                for (int i = 0; i < Math.min(chests.size(), other.chests.size()); i++) {
                    if (!chests.get(i)
                        .equals(other.chests.get(i))) {
                        return chests.get(i) + " vs " + other.chests.get(i);
                    }
                }
                return "chest count " + chests.size() + " vs " + other.chests.size();
            }
            return null;
        }
    }

    // ---- terrain ------------------------------------------------------------------------------------------------

    /**
     * Loads the area's chunks (with the empty populator standing in, so nothing natural is stamped), then shapes it:
     * flat stone with a grass top at {@link #LAND_Y}, or a stone seabed under water up to {@link #WATER_SURFACE_Y},
     * and clear air above. Only blocks that differ are written. Safe to call again as the cleanup.
     */
    static void prepare(World world, StructurePlan p, boolean water, Layout l) {
        int x0 = p.anchorX() - MARGIN;
        int x1 = p.maxX() + MARGIN;
        int z0 = p.anchorZ() - MARGIN;
        int z1 = p.maxZ() + MARGIN;
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int cx = Math.floorDiv(x0, 16) - 1; cx <= Math.floorDiv(x1, 16) + 1; cx++) {
            for (int cz = Math.floorDiv(z0, 16) - 1; cz <= Math.floorDiv(z1, 16) + 1; cz++) {
                cps.loadChunk(cx, cz);
            }
        }
        // Leftovers of an earlier aborted run: a GregGuns tile on a vanilla chest block crashes the vanilla break
        // code, and dropped items would be counted as this run's.
        for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x1, 16); cx++) {
            for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z1, 16); cz++) {
                Chunk c = world.getChunkFromChunkCoords(cx, cz);
                for (Object o : new ArrayList<Object>(c.chunkTileEntityMap.values())) {
                    TileEntity t = (TileEntity) o;
                    if (t instanceof TileGregChest && !(world.getBlock(t.xCoord, t.yCoord, t.zCoord) instanceof BlockGregChest)) {
                        world.removeTileEntity(t.xCoord, t.yCoord, t.zCoord);
                    }
                }
            }
        }
        AxisAlignedBB sweep = AxisAlignedBB.getBoundingBox(x0 - 8, 0, z0 - 8, x1 + 9, 256, z1 + 9);
        for (Object o : world.getEntitiesWithinAABB(EntityItem.class, sweep)) {
            ((Entity) o).setDead();
        }
        int depth = Math.max(14, l.ground() + 2);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int top = Math.min(
                    255,
                    world.getChunkFromBlockCoords(x, z)
                        .getTopFilledSegment() + 15);
                int surface = water ? WATER_SURFACE_Y : LAND_Y;
                for (int y = top; y > surface; y--) {
                    put(world, x, y, z, Blocks.air);
                }
                if (water) {
                    for (int y = WATER_SURFACE_Y; y > WATER_FLOOR_Y; y--) {
                        put(world, x, y, z, Blocks.water);
                    }
                    for (int y = WATER_FLOOR_Y; y > WATER_FLOOR_Y - 3; y--) {
                        put(world, x, y, z, Blocks.stone);
                    }
                } else {
                    put(world, x, LAND_Y, z, Blocks.grass);
                    for (int y = LAND_Y - 1; y > LAND_Y - depth; y--) {
                        put(world, x, y, z, Blocks.stone);
                    }
                }
            }
        }
    }

    private static void put(World world, int x, int y, int z, Block b) {
        if (world.getBlock(x, y, z) == Blocks.chest && world.getTileEntity(x, y, z) instanceof TileGregChest) {
            world.removeTileEntity(x, y, z); // left by an earlier aborted run; the vanilla break code cannot take it
        }
        if (world.getBlock(x, y, z) != b || world.getBlockMetadata(x, y, z) != 0) {
            world.setBlock(x, y, z, b, 0, 2);
        }
    }

    // ---- the run ------------------------------------------------------------------------------------------------

    /** Records what the stamper writes, per window, without touching the world. */
    private static final class Recorder implements WindowedSink {

        private final PopulateWindow window;
        private final Map<Long, Block> into;

        Recorder(PopulateWindow window, Map<Long, Block> into) {
            this.window = window;
            this.into = into;
        }

        @Override
        public PopulateWindow window() {
            return window;
        }

        @Override
        public boolean setBlock(int x, int y, int z, Block block, int meta) {
            if (!window.contains(x, z)) return false;
            if (block == Blocks.air) into.remove(key(x, y, z));
            else into.put(key(x, y, z), block);
            return true;
        }

        @Override
        public boolean setTile(int x, int y, int z, TileEntity tile) {
            return window.contains(x, z);
        }

        @Override
        public boolean spawn(Entity entity, double x, double y, double z) {
            return false;
        }

        @Override
        public int rejectedWrites() {
            return 0;
        }

        @Override
        public int acceptedWrites() {
            return 0;
        }
    }

    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    static int xOf(long k) {
        return (int) (k >> 38) << 6 >> 6;
    }

    static int zOf(long k) {
        return (int) (k >> 12 & 0x3FFFFFF) << 6 >> 6;
    }

    static int yOf(long k) {
        return (int) (k & 0xFFF);
    }

    /** The windows (chunk coordinates) that touch the plan's box, in a fixed order or its reverse. */
    static List<int[]> windowsOf(StructurePlan p, boolean reverse) {
        List<int[]> out = new ArrayList<>();
        for (int cx = Math.floorDiv(p.anchorX() - 24, 16); cx <= Math.floorDiv(p.maxX(), 16); cx++) {
            for (int cz = Math.floorDiv(p.anchorZ() - 24, 16); cz <= Math.floorDiv(p.maxZ(), 16); cz++) {
                if (p.overlapsWindow(cx, cz)) out.add(new int[] { cx, cz });
            }
        }
        if (reverse) Collections.reverse(out);
        return out;
    }

    /**
     * Prepares the area and runs the plan through the populator, one window at a time, with fresh plan state (so the
     * decision is made again) and counting spawner and flag hooks installed.
     *
     * @param water   a flooded area for a ship, else flat land
     * @param reverse visit the windows in the opposite order (stamping must not depend on it)
     */
    static Placement place(World world, StructurePlan plan, boolean water, boolean reverse) {
        long t0 = System.nanoTime();
        Layout layout = layouts().get(plan.id());
        StructureGenerator.setOverride(populator());
        try {
            prepare(world, plan, water, layout);
            StructurePopulator populator = populator();
            StructureState state = new StructureState("gregguns_places_test");
            Set<Type> biome = Collections.singleton(water ? Type.OCEAN : Type.PLAINS);
            StructurePopulator.BiomeSource biomes = (x, z) -> biome;
            List<int[]> windows = windowsOf(plan, reverse);

            List<String> spawnerTags = new ArrayList<>();
            List<int[]> spawnerPos = new ArrayList<>();
            List<int[]> flagPos = new ArrayList<>();
            StructureHooks.setSpawnerHook((w, s, pl, tag, x, y, z) -> {
                spawnerTags.add(tag);
                spawnerPos.add(new int[] { x, y, z });
            });
            StructureHooks.setFlagHook((w, s, pl, k, x, y, z) -> flagPos.add(new int[] { x, y, z }));

            int violationsBefore = PopulateGuard.violationCount();
            int stamped = 0;
            int rejected = 0;
            int accepted = 0;
            try {
                for (int[] w : windows) {
                    PopulateWindow window = new PopulateWindow(w[0], w[1]);
                    ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                    try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                        stamped += populator.populatePlans(
                            Collections.singletonList(plan),
                            window,
                            sink,
                            new WorldColumnReader(world),
                            biomes,
                            state,
                            new StructureGenerator.WorldMarks(world, sink));
                    }
                    rejected += sink.rejectedWrites();
                    accepted += sink.acceptedWrites();
                }
            } finally {
                StructureHooks.setSpawnerHook(null);
                StructureHooks.setFlagHook(null);
            }
            Decision d = state.get(PlanKey.of(plan));
            Placement out = new Placement(
                world,
                plan,
                layout,
                water,
                d == null ? Decision.rejected("no decision") : d,
                windows.size(),
                stamped,
                rejected,
                accepted,
                PopulateGuard.violationCount() - violationsBefore,
                (System.nanoTime() - t0) / 1_000_000L);
            out.spawnerTags.addAll(spawnerTags);
            out.spawnerPos.addAll(spawnerPos);
            out.flagPos.addAll(flagPos);
            if (out.decision.isPlaced()) dryRun(out, windows);
            return out;
        } finally {
            StructureGenerator.setOverride(null);
        }
    }

    /** What the stamper alone should write, window by window, recorded without a world. */
    private static void dryRun(Placement out, List<int[]> windows) {
        StampPlan sp = new StampPlan(
            out.layout,
            out.plan.anchorX(),
            out.anchorY,
            out.plan.anchorZ(),
            out.plan.rotation(),
            out.plan.variant());
        Stamper stamper = new Stamper(keys());
        for (int[] w : windows) {
            PopulateWindow window = new PopulateWindow(w[0], w[1]);
            stamper.write(new Recorder(window, out.expected), sp, window);
        }
    }
}
