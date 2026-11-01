package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.ComplexArena;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Footprint;
import com.gregguns.worldgen.PlacementHarnessAccess;
import com.gregguns.worldgen.PoolEntry;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WorldColumnReader;
import com.gregguns.worldgen.WorldTarget;

/**
 * The complex keep (D348) against late writes: a staffed complex is written onto a prepared slab with its contents,
 * then the world is changed the way the generators that run after it change it (GregTech's chain: a stone layer turns
 * a wall to stone and a vein turns that stone to ore through GregTech's own ore call; a stone layer's blob through a
 * GT brick; another generator's blocks in a room's air; a chest replaced), plus a chest a player emptied a slot of.
 * Once the surroundings have populated the keeper sets back exactly what the generators changed: the complex's block
 * comes back in every changed cell, the replaced chest comes back with the contents it was written with, the emptied
 * chest keeps its empty slot, and nothing is kept twice. Dev environment only; server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface: the complex keep)
public final class ComplexKeepScenarios {

    private static final int GROUND = 63;
    private static final long SEED = 0x51105L;

    private ComplexKeepScenarios() {}

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new LateWrites());
    }

    /** One cell the scenario changed, and what the keeper must leave there. */
    private static final class Target {

        final int x, y, z;
        final Block block;
        final int meta;
        final String what;

        Target(int x, int y, int z, Block block, int meta, String what) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
            this.what = what;
        }
    }

    private static final class LateWrites implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.keep_late_writes";
        }

        @Override
        public String description() {
            return "A staffed complex on a slab, then GregTech's late writes into it (stone layer then ore vein through"
                + " GT's own ore call, a blob through a GT brick), another generator's blocks in room air and a"
                + " replaced chest: once the surroundings have populated the keep sets every changed cell back, the"
                + " chest with its written contents, leaves a player-emptied slot alone, loads no chunk and keeps"
                + " each chunk once (D348).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = new StructureGrid(
                64,
                64,
                64,
                Collections.singletonMap(
                    SizeClass.BIG,
                    Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))))
                        .planAt(SEED, world.provider.dimensionId, SizeClass.BIG, 2, 4)
                        .get();
            int base = GROUND - 4;
            GtMachineHook machines = new GtMachineHook();
            ComplexKeeper keeper = ComplexKeeper.inMemory(machines);
            try {
                // Two chunks past the arena's own margin: the keep waits for every chunk that may write into a
                // recorded one, two out on the low side and one on the high side.
                ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
                for (int cx = (plan.anchorX() >> 4) - 4; cx <= ((plan.anchorX() + ComplexLayout.SIZE) >> 4) + 4; cx++) {
                    for (int cz = (plan.anchorZ() >> 4) - 4; cz
                        <= ((plan.anchorZ() + ComplexLayout.SIZE) >> 4) + 4; cz++) {
                        cps.loadChunk(cx, cz);
                    }
                }
                ComplexArena.load(world, plan);
                ComplexArena.slab(world, plan, false, (x, z) -> GROUND, 0, base);
                ComplexPlacer placer = new ComplexPlacer(
                    new WorldBlocks(),
                    machines,
                    new FurnitureFiller(),
                    new Settings()).withStyles(PaletteStyles.FACTORY)
                        .forceLevel(ActivityLevel.OPERATIONAL)
                        .withKeeper(keeper);
                placer.enabledIn(world);
                StructureState state = new StructureState("gregguns_complex_keep_test");
                int before = PopulateGuard.violationCount();
                for (int[] w : PlacementHarnessAccess.windowsOf(plan, false)) {
                    PopulateWindow window = new PopulateWindow(w[0], w[1]);
                    ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                    try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                        PlacementHarnessAccess.populator()
                            .populateComplex(
                                plan,
                                placer,
                                window,
                                sink,
                                new WorldColumnReader(world),
                                (x, z) -> Collections.singleton(Type.PLAINS),
                                state);
                    }
                }
                out.checkEquals("PopulateGuard violations", 0, PopulateGuard.violationCount() - before);
                Map<Long, KeptChunk> records = keeper.records(world);
                int chunks = records.size();
                long cells = 0, tiles = 0;
                for (KeptChunk k : records.values()) {
                    cells += k.size();
                    tiles += k.tileCount();
                }
                long[] st = keeper.stats();
                out.note(
                    chunks + " chunks recorded, "
                        + cells
                        + " cells, "
                        + tiles
                        + " tile entities, recording took "
                        + st[4] / 1_000_000L
                        + " ms");
                out.check(chunks >= 200, "every chunk the complex wrote has a record (" + chunks + ")");
                out.check(tiles > 20, "the staffed complex's tile entities are recorded (" + tiles + ")");

                List<Target> targets = new ArrayList<Target>();
                lateWrites(out, world, records, targets);

                // Before the keep: the cells that differ from the record are exactly the late writes.
                java.util.Set<String> want = new java.util.HashSet<String>();
                for (Target t : targets) want.add(t.x + "," + t.y + "," + t.z);
                int stray = 0;
                StringBuilder strays = new StringBuilder();
                Map<String, Block> cache = new java.util.HashMap<String, Block>();
                for (KeptChunk k : records.values()) {
                    net.minecraft.world.chunk.Chunk c = world.getChunkFromChunkCoords(k.chunkX(), k.chunkZ());
                    for (int i = 0; i < k.size(); i++) {
                        if (ComplexKeeper.changed(c, k, i, cache) == null) continue;
                        int pos = k.posAt(i);
                        String at = k.x(pos) + "," + KeptChunk.y(pos) + "," + k.z(pos);
                        if (want.contains(at)) continue;
                        stray++;
                        if (strays.length() < 600) strays.append(' ')
                            .append(at)
                            .append(' ')
                            .append(k.blockAt(i))
                            .append(':')
                            .append(k.metaAt(i))
                            .append("<-")
                            .append(
                                Block.blockRegistry
                                    .getNameForObject(world.getBlock(k.x(pos), KeptChunk.y(pos), k.z(pos))))
                            .append(':')
                            .append(world.getBlockMetadata(k.x(pos), KeptChunk.y(pos), k.z(pos)));
                    }
                }
                out.check(
                    stray == 0,
                    "only the late writes differ from the record"
                        + (stray == 0 ? "" : " (" + stray + " more:" + strays + ")"));
                int loadedBefore = cps.getLoadedChunkCount();
                int kept = keeper.keepReady(world, false);
                st = keeper.stats();
                out.note(
                    "kept " + kept
                        + " chunks, "
                        + st[1]
                        + " cells checked, "
                        + st[2]
                        + " set back, in "
                        + st[3] / 1_000_000L
                        + " ms");
                out.checkEquals(
                    "every recorded chunk's surroundings are populated and loaded, so all are kept",
                    chunks,
                    kept);
                out.checkEquals("the keep loads no chunk", loadedBefore, cps.getLoadedChunkCount());
                out.checkEquals("no record is left", 0, keeper.pendingCount(world));
                int wrong = 0;
                String firstWrong = null;
                for (Target t : targets) {
                    if (t.block == null) continue;
                    Block b = world.getBlock(t.x, t.y, t.z);
                    int m = world.getBlockMetadata(t.x, t.y, t.z);
                    if (b != t.block || m != t.meta) {
                        wrong++;
                        if (firstWrong == null) firstWrong = t.what + " at "
                            + t.x
                            + ","
                            + t.y
                            + ","
                            + t.z
                            + " holds "
                            + Block.blockRegistry.getNameForObject(b)
                            + ":"
                            + m;
                    }
                }
                out.check(
                    wrong == 0,
                    "every changed cell holds the complex's block again"
                        + (firstWrong == null ? "" : " (" + wrong + " wrong, first: " + firstWrong + ")"));
                out.checkEquals("set back exactly the cells the late writes changed", (long) setCount, st[2]);
                checkChests(out, world);

                // Once only: what is written after the keep stays.
                Target first = targets.get(0);
                world.setBlock(first.x, first.y, first.z, Blocks.stone, 0, 2);
                out.checkEquals("a kept chunk is forgotten", 0, keeper.keepReady(world, false));
                out.check(
                    world.getBlock(first.x, first.y, first.z) == Blocks.stone,
                    "a block placed after the keep stays");
            } finally {
                ComplexArena.slab(world, plan, true, (x, z) -> GROUND, 0, base);
            }
            return out.build();
        }

        /** Cells the late writes changed; the keeper must set back exactly these. */
        private int setCount;
        private int[] replacedChest, emptiedChest;
        private NBTTagCompound replacedData;
        private int emptiedSlot = -1;

        /**
         * Changes the complex the way later generators do, each kind spread over the whole complex: about one plain
         * cell in 400 goes to stone and then, through GregTech's own world-gen ore call, to ore; GT bricks lose their
         * meta to a blob's raw stone; room air takes mossy cobblestone; one chest is replaced by stone; and a player
         * empties a slot of another chest.
         */
        private void lateWrites(ScenarioResult.Builder out, World world, Map<Long, KeptChunk> records,
            List<Target> targets) {
            setCount = 0;
            replacedChest = emptiedChest = null;
            replacedData = null;
            emptiedSlot = -1;
            int ore = 0, oreRefused = 0, blob = 0, air = 0, n = 0;
            for (KeptChunk k : records.values()) {
                for (int i = 0; i < k.size(); i++) {
                    int pos = k.posAt(i);
                    int x = k.x(pos), y = KeptChunk.y(pos), z = k.z(pos);
                    Block kept = Block.getBlockFromName(k.blockAt(i));
                    int meta = k.metaAt(i);
                    if (kept == null) continue;
                    NBTTagCompound tile = k.tile(pos);
                    n++;
                    if (tile != null) {
                        TileEntity te = world.getTileEntity(x, y, z);
                        if (!(te instanceof IInventory) || !holdsItems((IInventory) te)) continue;
                        if (replacedChest == null) {
                            replacedChest = new int[] { x, y, z };
                            replacedData = (NBTTagCompound) tile.copy();
                            world.setBlock(x, y, z, Blocks.stone, 0, 2);
                            targets.add(new Target(x, y, z, kept, meta, "replaced container"));
                            setCount++;
                        } else if (emptiedChest == null) {
                            IInventory inv = (IInventory) te;
                            for (int s = 0; s < inv.getSizeInventory(); s++) {
                                if (inv.getStackInSlot(s) != null) {
                                    inv.setInventorySlotContents(s, null);
                                    emptiedSlot = s;
                                    break;
                                }
                            }
                            emptiedChest = new int[] { x, y, z };
                        }
                        continue;
                    }
                    String name = k.blockAt(i);
                    if (("gregtech:gt.blockgranites".equals(name) || "gregtech:gt.blockstones".equals(name))
                        && meta != 0
                        && meta != 8
                        && blob < 40) {
                        // A stone layer's blob through a GT brick leaves the raw stone of the same block.
                        world.setBlock(x, y, z, kept, meta & 8, 2);
                        targets.add(new Target(x, y, z, kept, meta, "GT brick"));
                        blob++;
                        setCount++;
                        continue;
                    }
                    if (n % 400 != 0) continue;
                    if (kept == Blocks.air) {
                        if (air >= 40) continue;
                        world.setBlock(x, y, z, Blocks.mossy_cobblestone, 0, 2);
                        targets.add(new Target(x, y, z, kept, meta, "room air"));
                        air++;
                        setCount++;
                        continue;
                    }
                    if (ComplexKeeper.kind(kept, meta) != KeepRule.Kind.PLAIN || ore >= 80) continue;
                    // GregTech's chain: a stone layer leaves stone, and the vein's own call turns it to ore.
                    world.setBlock(x, y, z, Blocks.stone, 0, 2);
                    if (!gregtech.common.ores.OreManager
                        .setOreForWorldGen(world, x, y, z, null, gregtech.api.enums.Materials.Iron, false))
                        oreRefused++;
                    targets.add(new Target(x, y, z, kept, meta, "ore"));
                    ore++;
                    setCount++;
                }
            }
            out.note(ore + " cells to ore, " + blob + " GT bricks to raw stone, " + air + " room air cells filled");
            out.check(ore >= 40, "ore went into the complex in many places (" + ore + ")");
            out.checkEquals("GregTech's ore call takes the stone it left", 0, oreRefused);
            out.check(air >= 10, "room air took another generator's blocks (" + air + ")");
            out.check(replacedChest != null && emptiedChest != null, "two stocked containers to work with");
        }

        private void checkChests(ScenarioResult.Builder out, World world) {
            if (replacedChest == null || emptiedChest == null) return;
            TileEntity te = world.getTileEntity(replacedChest[0], replacedChest[1], replacedChest[2]);
            NBTTagCompound now = new NBTTagCompound();
            if (te != null) te.writeToNBT(now);
            out.check(
                te != null && now.getTag("Items") != null
                    && now.getTag("Items")
                        .equals(replacedData.getTag("Items")),
                "the replaced container is back with the contents it was written with");
            TileEntity e = world.getTileEntity(emptiedChest[0], emptiedChest[1], emptiedChest[2]);
            out.check(
                e instanceof IInventory && ((IInventory) e).getStackInSlot(emptiedSlot) == null,
                "a standing container's contents are left alone (the emptied slot stays empty)");
        }

        private static boolean holdsItems(IInventory inv) {
            for (int s = 0; s < inv.getSizeInventory(); s++) if (inv.getStackInSlot(s) != null) return true;
            return false;
        }
    }

    private static final class Settings implements ComplexPlacer.Settings {

        @Override
        public boolean dimensionEnabled(String n) {
            return true;
        }

        @Override
        public int chancePercent() {
            return 100;
        }

        @Override
        public java.util.Set<Type> overworldBiomes() {
            return Collections.emptySet();
        }
    }
}
