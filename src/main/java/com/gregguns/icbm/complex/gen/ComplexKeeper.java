package com.gregguns.icbm.complex.gen;

import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.IFluidBlock;

import com.gregguns.GregGuns;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.WindowedSink;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The complex keep (D348): a silo complex wins over every world generator that runs after it. GregTech forces its ore
 * generator to the end of every populate and writes 2 to 17 blocks past its chunk's corner, so the populates around a
 * complex's window run their stone layers and ore veins over the complex after it is written, and other mods'
 * populates can too. GregTech's stone layers turn the complex's rubble, its GT stone bricks and the escape ladder's
 * stone backing into raw granite, marble or basalt, and its veins then turn those and every plain stone cell into ore.
 *
 * <p>
 * So the writer wraps each window's sink in a {@link Recorder}, and when the window is done the keeper reads back what
 * every written cell holds, block, meta and tile entity data, into a {@link KeptChunk} per chunk. Once every chunk
 * whose populate could still write into that chunk has populated and is loaded, and GregTech's own queue of deferred
 * ore jobs holds none for them, the keeper compares the world with the record and sets back every cell some later
 * generator changed ({@link KeepRule}): the ore goes, the complex's block comes back. A piece of furniture or a
 * container that still stands keeps its contents untouched; one that was replaced comes back with the contents it was
 * written with. Then the record is dropped, so anything a player does there later stays. Only cells the complex wrote
 * are kept: a column the cut-off mask (D207) left natural has no record. Nothing is loaded or generated: a chunk is
 * kept only while it and its surroundings are loaded.
 *
 * <p>
 * Server side. The game's keeper ({@link #game()}) holds the records of loaded chunks in memory and saves each with
 * its chunk ({@link #NBT_KEY} in the chunk's saved data), so a record whose surroundings populate after a restart is
 * still kept; it runs from the world tick within a time budget. Scenarios use {@link #inMemory} and call
 * {@link #keepReady} themselves.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface: the complex keep)
// Source: GregTech forces its world generator last and queues each chunk's ore job, running at most 5 per populate,
// at 5.09.54.205 (unchanged at 5.09.54.265):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/mixin/java/gregtech/mixin/mixins/early/forge/GameRegistryMixin.java#L28-L44
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTWorldgenerator.java#L176-L213
// Source: an ore vein writes from 2 to 17 blocks past its chunk's corner, into GT stone types only:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/WorldgenGTOreLayer.java#L209-L233
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/ores/OreManager.java#L112-L124
// Source: the stone layers replace stone, cobblestone, stained clay and every GT granite and stone block:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/WorldgenStone.java#L223-L230
public final class ComplexKeeper {

    /** Ticks between two looks at a world's records. */
    static final int PERIOD = 10;
    /** Most time one look spends setting chunks back; the rest wait for the next look. */
    static final long BUDGET_NANOS = 10_000_000L;
    /** The key of a chunk's record in its saved data. */
    public static final String NBT_KEY = "GregGunsComplexKeep";
    /** Chunks past the two whose populate may write a column, on each side, for mods that reach past their window. */
    static final int MARGIN_CHUNKS = SiloMouth.MARGIN_CHUNKS;

    /** Where the records live. */
    interface Store {

        /** @return the world's records by chunk key, or null when it has none and {@code create} is false */
        Map<Long, KeptChunk> pending(World world, boolean create);
    }

    /** The game's records of loaded chunks, per dimension; the rest are in their chunks' saved data. */
    private static final Map<Integer, Map<Long, KeptChunk>> LOADED = new HashMap<Integer, Map<Long, KeptChunk>>();

    private static final Store GAME_STORE = (world, create) -> {
        Map<Long, KeptChunk> m = LOADED.get(world.provider.dimensionId);
        if (m == null && create) {
            m = new LinkedHashMap<Long, KeptChunk>();
            LOADED.put(world.provider.dimensionId, m);
        }
        return m;
    };

    private static ComplexKeeper game;
    /** Records read back from chunks' saved data since boot (a restart between write and keep). */
    private static long loadedFromSave;

    /** @return the keeper the game's writer records into, made on first use */
    public static synchronized ComplexKeeper game() {
        if (game == null) game = new ComplexKeeper(new GtMachineHook(), GAME_STORE);
        return game;
    }

    /** @return a keeper that holds its records in memory and never runs by itself (scenarios) */
    public static ComplexKeeper inMemory(MachineHook machines) {
        final Map<Long, KeptChunk> map = new LinkedHashMap<Long, KeptChunk>();
        return new ComplexKeeper(machines, (world, create) -> map);
    }

    private final MachineHook machines;
    private final Store store;
    /** Totals since boot, for the log and the scenarios: chunks kept, cells checked, cells set back, nanoseconds. */
    private long keptChunks, checkedCells, setBack, spentNanos, recordNanos;
    /** What the cells set back held, by block name (the first 64 names), for the log and the real-pack check. */
    private final Map<String, Long> replaced = new LinkedHashMap<String, Long>();

    ComplexKeeper(MachineHook machines, Store store) {
        this.machines = machines;
        this.store = store;
    }

    static long key(int chunkX, int chunkZ) {
        return (long) chunkX << 32 ^ chunkZ & 0xFFFFFFFFL;
    }

    // --- recording ----------------------------------------------------------------------------------------------

    /** @return a view of a window's sink that notes every cell it writes, for {@link #capture} */
    public Recorder recorder(WindowedSink inner) {
        return new Recorder(inner);
    }

    /** A window sink that notes, per chunk, every cell a write reached. */
    public static final class Recorder implements WindowedSink {

        private final WindowedSink inner;
        private final Map<Long, BitSet> written = new LinkedHashMap<Long, BitSet>();

        Recorder(WindowedSink inner) {
            this.inner = inner;
        }

        private void note(int x, int y, int z) {
            if (y < 0 || y > 255) return;
            long k = key(x >> 4, z >> 4);
            BitSet b = written.get(k);
            if (b == null) {
                b = new BitSet(1 << 16);
                written.put(k, b);
            }
            b.set(KeptChunk.pos(x, y, z));
        }

        @Override
        public PopulateWindow window() {
            return inner.window();
        }

        @Override
        public boolean setBlock(int x, int y, int z, Block block, int meta) {
            if (!inner.setBlock(x, y, z, block, meta)) return false;
            note(x, y, z);
            return true;
        }

        @Override
        public boolean setTile(int x, int y, int z, TileEntity tile) {
            if (!inner.setTile(x, y, z, tile)) return false;
            note(x, y, z);
            return true;
        }

        @Override
        public boolean spawn(Entity entity, double x, double y, double z) {
            return inner.spawn(entity, x, y, z);
        }

        @Override
        public int rejectedWrites() {
            return inner.rejectedWrites();
        }

        @Override
        public int acceptedWrites() {
            return inner.acceptedWrites();
        }

        /** @return how many cells the window wrote */
        public int cells() {
            int n = 0;
            for (BitSet b : written.values()) n += b.cardinality();
            return n;
        }
    }

    /**
     * Reads back what every cell the window wrote now holds, into its chunk's record. Call once the window is written
     * (machines, contents and the chests' facings settled): the record is the complex as its writer left it.
     *
     * @return cells recorded
     */
    public int capture(World world, Recorder rec) {
        if (world == null || rec.written.isEmpty()) return 0;
        long t0 = System.nanoTime();
        Map<Long, KeptChunk> pending = store.pending(world, true);
        int n = 0;
        for (Map.Entry<Long, BitSet> e : rec.written.entrySet()) {
            int cx = (int) (e.getKey() >> 32), cz = (int) (long) e.getKey();
            if (!world.getChunkProvider()
                .chunkExists(cx, cz)) continue;
            Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
            KeptChunk k = pending.get(e.getKey());
            if (k == null) {
                k = new KeptChunk(cx, cz);
                pending.put(e.getKey(), k);
            }
            BitSet b = e.getValue();
            k.forget(b);
            for (int pos = b.nextSetBit(0); pos >= 0; pos = b.nextSetBit(pos + 1)) {
                int lx = pos >> 4 & 15, y = KeptChunk.y(pos), lz = pos & 15;
                Block block = chunk.getBlock(lx, y, lz);
                int meta = chunk.getBlockMetadata(lx, y, lz);
                NBTTagCompound tile = null;
                if (block.hasTileEntity(meta)) {
                    TileEntity te = world.getTileEntity(cx * 16 + lx, y, cz * 16 + lz);
                    if (te != null) {
                        tile = new NBTTagCompound();
                        te.writeToNBT(tile);
                    }
                }
                if (k.put(pos, name(block), meta, tile)) n++;
            }
            chunk.setChunkModified();
        }
        recordNanos += System.nanoTime() - t0;
        return n;
    }

    private static String name(Block b) {
        String n = Block.blockRegistry.getNameForObject(b);
        return n == null ? KeepRule.AIR : n;
    }

    // --- keeping ------------------------------------------------------------------------------------------------

    /** @return the records still waiting in a world */
    public int pendingCount(World world) {
        Map<Long, KeptChunk> pending = store.pending(world, false);
        return pending == null ? 0 : pending.size();
    }

    /** @return what the cells set back since boot held, by block name and count */
    public Map<String, Long> replaced() {
        return java.util.Collections.unmodifiableMap(replaced);
    }

    /** @return a world's records by chunk key (scenarios), never null */
    Map<Long, KeptChunk> records(World world) {
        Map<Long, KeptChunk> pending = store.pending(world, false);
        return pending == null ? java.util.Collections.<Long, KeptChunk>emptyMap() : pending;
    }

    /** @return whether a world holds a record for the chunk */
    public boolean pending(World world, int chunkX, int chunkZ) {
        Map<Long, KeptChunk> pending = store.pending(world, false);
        return pending != null && pending.containsKey(key(chunkX, chunkZ));
    }

    /**
     * @return chunks kept, cells checked, cells set back, nanoseconds spent keeping, nanoseconds spent recording, and
     *         records the game read back from saved chunks
     */
    public long[] stats() {
        return new long[] { keptChunks, checkedCells, setBack, spentNanos, recordNanos, loadedFromSave };
    }

    /**
     * Keeps every recorded chunk whose surroundings have populated and are loaded, within {@link #BUDGET_NANOS} when
     * {@code budgeted}.
     *
     * @return chunks kept
     */
    public int keepReady(World world, boolean budgeted) {
        Map<Long, KeptChunk> pending = store.pending(world, false);
        if (pending == null || pending.isEmpty()) return 0;
        long t0 = System.nanoTime();
        Set<Long> gtQueued = GtOreQueue.queuedChunks(world);
        int done = 0;
        boolean before = machines.suspendUpdates();
        try {
            for (Iterator<KeptChunk> it = pending.values()
                .iterator(); it.hasNext();) {
                if (budgeted && System.nanoTime() - t0 > BUDGET_NANOS) break;
                KeptChunk k = it.next();
                if (!ready(world, k.chunkX(), k.chunkZ(), gtQueued)) continue;
                // Dropped whether or not it works out: a record that throws is not retried every look.
                it.remove();
                try {
                    int changed = keep(world, k);
                    if (changed > 0) {
                        GregGuns.LOG.debug(
                            "Silo complex keep: chunk {},{} set back {} of {} cells",
                            k.chunkX(),
                            k.chunkZ(),
                            changed,
                            k.size());
                    }
                } catch (RuntimeException ex) {
                    GregGuns.LOG.error("Silo complex keep failed at chunk " + k.chunkX() + "," + k.chunkZ(), ex);
                }
                done++;
            }
        } finally {
            machines.resumeUpdates(before);
        }
        spentNanos += System.nanoTime() - t0;
        return done;
    }

    /**
     * A populate for chunk c may write blocks c*16 to c*16+31 (GGS-11-08), and GregTech's ore job for chunk c blocks
     * c*16+2 to c*16+17, so chunk K is written by the populates of K-1 and K on each axis; {@link #MARGIN_CHUNKS} more
     * on each side cover mods that reach past their window (the rule of the mouth finish, D319).
     *
     * @return the first chunk coordinate whose populate must have run before chunk {@code c} is kept
     */
    static int reachMin(int c) {
        return c - 1 - MARGIN_CHUNKS;
    }

    /** @return the last chunk coordinate whose populate must have run before chunk {@code c} is kept */
    static int reachMax(int c) {
        return c + MARGIN_CHUNKS;
    }

    /**
     * @param gtQueued chunk keys of GregTech ore jobs still queued in this world
     * @return whether every chunk that could still write into the chunk has populated and is loaded, and none of them
     *         has an ore job waiting in GregTech's queue
     */
    static boolean ready(World world, int chunkX, int chunkZ, Set<Long> gtQueued) {
        IChunkProvider p = world.getChunkProvider();
        for (int cx = reachMin(chunkX); cx <= reachMax(chunkX); cx++) {
            for (int cz = reachMin(chunkZ); cz <= reachMax(chunkZ); cz++) {
                if (!p.chunkExists(cx, cz) || !world.getChunkFromChunkCoords(cx, cz).isTerrainPopulated) return false;
                if (gtQueued.contains(key(cx, cz))) return false;
            }
        }
        return true;
    }

    /**
     * Sets back every cell of the record some later generator changed.
     *
     * @return cells set back
     */
    int keep(World world, KeptChunk k) {
        Chunk chunk = world.getChunkFromChunkCoords(k.chunkX(), k.chunkZ());
        Map<String, Block> blocks = new HashMap<String, Block>();
        int n = 0;
        for (int i = 0; i < k.size(); i++) {
            int pos = k.posAt(i);
            int lx = pos >> 4 & 15, y = KeptChunk.y(pos), lz = pos & 15;
            String keptName = k.blockAt(i);
            Block kept = blocks.get(keptName);
            if (kept == null) {
                kept = Block.getBlockFromName(keptName);
                if (kept == null) continue; // its mod is gone: nothing to put back
                blocks.put(keptName, kept);
            }
            int keptMeta = k.metaAt(i);
            Block now = chunk.getBlock(lx, y, lz);
            int nowMeta = chunk.getBlockMetadata(lx, y, lz);
            checkedCells++;
            if (now == kept && nowMeta == keptMeta) continue;
            NBTTagCompound tile = k.tile(pos);
            KeepRule.Kind keptKind = tile != null ? KeepRule.Kind.TILE : kind(kept, keptMeta);
            if (KeepRule.intact(keptName, keptMeta, keptKind, name(now), nowMeta, kind(now, nowMeta))) continue;
            int x = k.x(pos), z = k.z(pos);
            String was = name(now);
            if (replaced.containsKey(was) || replaced.size() < 64) {
                Long c = replaced.get(was);
                replaced.put(was, c == null ? 1L : c + 1L);
            }
            world.setBlock(x, y, z, kept, keptMeta, 2);
            if (tile != null) {
                TileEntity te = TileEntity.createAndLoadEntity((NBTTagCompound) tile.copy());
                if (te != null) world.setTileEntity(x, y, z, te);
            }
            n++;
        }
        setBack += n;
        keptChunks++;
        // The chunk is saved again soon, without the record.
        chunk.setChunkModified();
        return n;
    }

    /** @return how a block changes by itself in play */
    static KeepRule.Kind kind(Block b, int meta) {
        if (b.hasTileEntity(meta)) return KeepRule.Kind.TILE;
        if (b instanceof BlockLiquid || b instanceof IFluidBlock) return KeepRule.Kind.LIQUID;
        if (b instanceof BlockFalling) return KeepRule.Kind.FALLING;
        if (b instanceof BlockBush || b instanceof IGrowable
            || b instanceof BlockFarmland
            || b instanceof BlockDoor
            || b instanceof BlockTrapDoor
            || b instanceof BlockFenceGate
            || b instanceof BlockLever
            || b instanceof BlockButton
            || b instanceof BlockBasePressurePlate
            || b instanceof BlockRedstoneWire
            || b instanceof BlockRedstoneDiode
            || b instanceof BlockLeavesBase
            || b instanceof BlockCauldron
            || b instanceof BlockSnow) {
            return KeepRule.Kind.STATEFUL;
        }
        return KeepRule.Kind.PLAIN;
    }

    // --- GregTech's queue ---------------------------------------------------------------------------------------

    /**
     * GregTech's queue of ore jobs not yet run. Each populate queues its chunk's job and runs at most five, so under
     * load a job can run during a later populate, after the chunks around it have populated. Read by reflection,
     * since GregTech offers no API for it; if the field is gone (another GregTech), the keeper goes by populated
     * chunks alone and says so once.
     */
    // Source: PENDING_TASKS and WorldGenContainer's mX, mZ and mWorld at 5.09.54.205 (same at 5.09.54.265):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTWorldgenerator.java#L53
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTWorldgenerator.java#L424-L446
    static final class GtOreQueue {

        private static boolean tried;
        private static List<?> tasks;
        private static Field fx, fz, fw;

        private GtOreQueue() {}

        private static synchronized void init() {
            if (tried) return;
            tried = true;
            try {
                Class<?> gen = Class.forName("gregtech.common.GTWorldgenerator");
                Field f = gen.getDeclaredField("PENDING_TASKS");
                f.setAccessible(true);
                Class<?> task = Class.forName("gregtech.common.GTWorldgenerator$WorldGenContainer");
                fx = task.getField("mX");
                fz = task.getField("mZ");
                fw = task.getField("mWorld");
                tasks = (List<?>) f.get(null);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                tasks = null;
                GregGuns.LOG.info(
                    "Silo complex keep: GregTech's ore job queue is not readable ({}); keeping by populated chunks only",
                    e.toString());
            }
        }

        /** @return chunk keys of this world's queued ore jobs (empty when none, or when the queue is unreadable) */
        static Set<Long> queuedChunks(World world) {
            init();
            if (tasks == null || tasks.isEmpty()) return java.util.Collections.emptySet();
            Set<Long> out = new HashSet<Long>();
            try {
                synchronized (tasks) {
                    for (Object t : tasks) {
                        if (fw.get(t) != world) continue;
                        out.add(key(fx.getInt(t), fz.getInt(t)));
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                return out;
            }
            return out;
        }
    }

    // --- the game's store: records saved with their chunks --------------------------------------------------------

    /**
     * Saves each chunk's record with the chunk and reads it back when the chunk loads, so the records of unloaded
     * chunks take no memory and survive a restart. Public for Forge's generated handler.
     */
    public static final class Events {

        @SubscribeEvent
        public void onChunkSave(ChunkDataEvent.Save event) {
            World world = event.world;
            if (world == null || world.isRemote) return;
            Map<Long, KeptChunk> m = LOADED.get(world.provider.dimensionId);
            if (m == null) return;
            Chunk c = event.getChunk();
            long k = key(c.xPosition, c.zPosition);
            KeptChunk kept = m.get(k);
            if (kept == null) return;
            event.getData()
                .setTag(NBT_KEY, kept.toNbt());
            // An unloading chunk is saved after it is marked unloaded: its record now lives in its data alone.
            if (!c.isChunkLoaded) m.remove(k);
        }

        @SubscribeEvent
        public void onChunkLoad(ChunkDataEvent.Load event) {
            World world = event.world;
            if (world == null || world.isRemote) return;
            NBTTagCompound data = event.getData();
            if (data == null || !data.hasKey(NBT_KEY, 10)) return;
            Chunk c = event.getChunk();
            KeptChunk kept = KeptChunk.fromNbt(c.xPosition, c.zPosition, data.getCompoundTag(NBT_KEY));
            if (kept == null) return;
            GAME_STORE.pending(world, true)
                .put(key(c.xPosition, c.zPosition), kept);
            loadedFromSave++;
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            if (event.world.isRemote) return;
            LOADED.remove(event.world.provider.dimensionId);
        }
    }

    /** Runs the game keeper every {@link #PERIOD} ticks per server world. Public for Forge's generated handler. */
    public static final class Ticker {

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
            if (event.world.getTotalWorldTime() % PERIOD != 0) return;
            game().keepReady(event.world, true);
        }
    }
}
