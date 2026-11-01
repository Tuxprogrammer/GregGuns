package com.gregguns.icbm.complex.gen;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import com.gregguns.GregGuns;
import com.gregguns.icbm.complex.SurfaceVariant;
import com.gregguns.worldgen.layout.ResolvedBlock;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The silo mouth finish (D319). The writer builds each silo's bore and cap in the populate window that holds the
 * column, but populates that run later, around and over the mouth, still write there: a neighbor's tree hangs its
 * canopy over a cap, a cave decorator hangs glow lichen in the dark bore, snow settles on what the canopy leaves. So
 * the writer records every mouth ({@link SiloMouth}), and once every chunk whose populate could reach it has populated
 * and is loaded, the keeper puts the mouth back to what the writer meant: the bore air from the pad to the surface, the
 * cap where the activity level's variant has one, and no natural cover over the bore or the ring. Each mouth is
 * finished once, then forgotten, so anything a player builds there later stays. The result does not depend on which
 * window or chunk populated first.
 *
 * <p>
 * Server side. The game's keeper is {@link #game()}, saved per dimension in {@link SiloMouths} and run every second
 * from the world tick; scenarios use {@link #inMemory} and call {@link #finishReady} themselves.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface: the mouth finish)
public final class SiloMouthKeeper {

    /** Ticks between two looks at a world's pending mouths. */
    static final int PERIOD = 20;

    /** Where the pending mouths live. */
    interface Store {

        /** @return the world's pending mouths, or null when it has none and {@code create} is false */
        Map<String, SiloMouth> pending(World world, boolean create);

        /** The world's record changed and must be saved. */
        void changed(World world);
    }

    private static final Store SAVED = new Store() {

        @Override
        public Map<String, SiloMouth> pending(World world, boolean create) {
            SiloMouths s = SiloMouths.forWorld(world, create);
            return s == null ? null : s.pending();
        }

        @Override
        public void changed(World world) {
            SiloMouths s = SiloMouths.forWorld(world, false);
            if (s != null) s.markDirty();
        }
    };

    private static SiloMouthKeeper game;

    /** @return the keeper the game's writer records into, made on first use */
    public static synchronized SiloMouthKeeper game() {
        if (game == null) game = new SiloMouthKeeper(new WorldBlocks(), new GtMachineHook(), SAVED);
        return game;
    }

    /**
     * A keeper that holds its mouths in memory and never runs by itself (scenarios).
     *
     * @param blocks the cap's block
     */
    public static SiloMouthKeeper inMemory(BlockResolver blocks, MachineHook machines) {
        final Map<String, SiloMouth> map = new LinkedHashMap<String, SiloMouth>();
        return new SiloMouthKeeper(blocks, machines, new Store() {

            @Override
            public Map<String, SiloMouth> pending(World world, boolean create) {
                return map;
            }

            @Override
            public void changed(World world) {}
        });
    }

    private final BlockResolver blocks;
    private final MachineHook machines;
    private final Store store;

    SiloMouthKeeper(BlockResolver blocks, MachineHook machines, Store store) {
        this.blocks = blocks;
        this.machines = machines;
        this.store = store;
    }

    /**
     * Records the top the writer used in one column of a silo's shaft footprint.
     *
     * @param id   the silo's id, unique in its dimension
     * @param padY the pad's world y
     * @param top  the column's top, or {@link com.gregguns.worldgen.HeightProbe#NONE} for a column under water
     */
    public void record(World world, String id, int sx, int sz, int padY, SurfaceVariant variant, int x, int z,
        int top) {
        if (world == null) return;
        Map<String, SiloMouth> pending = store.pending(world, true);
        SiloMouth m = pending.get(id);
        if (m == null) {
            m = new SiloMouth(sx, sz, padY, variant);
            pending.put(id, m);
        }
        if (m.record(x, z, top)) store.changed(world);
    }

    /** @return the mouths still waiting in a world (scenarios) */
    public int pendingCount(World world) {
        Map<String, SiloMouth> pending = store.pending(world, false);
        return pending == null ? 0 : pending.size();
    }

    /**
     * Finishes every pending mouth whose surroundings have populated and are loaded.
     *
     * @return how many were finished
     */
    public int finishReady(World world) {
        Map<String, SiloMouth> pending = store.pending(world, false);
        if (pending == null || pending.isEmpty()) return 0;
        int done = 0;
        boolean before = machines.suspendUpdates();
        try {
            for (Iterator<Map.Entry<String, SiloMouth>> it = pending.entrySet()
                .iterator(); it.hasNext();) {
                Map.Entry<String, SiloMouth> e = it.next();
                SiloMouth m = e.getValue();
                if (!ready(world, m)) continue;
                // Forgotten whether or not it works out: a mouth that throws is not retried every second.
                it.remove();
                try {
                    int changed = finish(world, m);
                    GregGuns.LOG.debug(
                        "Silo mouth {} at {},{} finished: {} cells set back{}",
                        e.getKey(),
                        m.sx(),
                        m.sz(),
                        changed,
                        m.complete() ? "" : " (some columns never recorded)");
                } catch (RuntimeException ex) {
                    GregGuns.LOG.error("Silo mouth " + e.getKey() + " at " + m.sx() + "," + m.sz() + " failed", ex);
                }
                done++;
            }
        } finally {
            machines.resumeUpdates(before);
        }
        if (done > 0) store.changed(world);
        return done;
    }

    /** @return whether every chunk that could still write over the mouth has populated and is loaded */
    static boolean ready(World world, SiloMouth m) {
        IChunkProvider p = world.getChunkProvider();
        for (int cx = m.minChunkX(); cx <= m.maxChunkX(); cx++) {
            for (int cz = m.minChunkZ(); cz <= m.maxChunkZ(); cz++) {
                if (!p.chunkExists(cx, cz) || !world.getChunkFromChunkCoords(cx, cz).isTerrainPopulated) return false;
            }
        }
        return true;
    }

    /** Puts every cell of the mouth to what {@link SiloMouth#want} says. @return cells changed */
    int finish(World world, SiloMouth m) {
        int n = 0;
        Block air = blocks.air();
        for (int x = m.sx(); x < m.sx() + SiloMouth.SIDE; x++) {
            for (int z = m.sz(); z < m.sz() + SiloMouth.SIDE; z++) {
                for (int y = m.padY() + 1; y <= SiloMouth.BUILD_HEIGHT; y++) {
                    SiloMouth.Want w = m.want(x, y, z);
                    if (w == SiloMouth.Want.KEEP) continue;
                    Block b = world.getBlock(x, y, z);
                    if (w == SiloMouth.Want.CAP) {
                        ResolvedBlock cap = blocks.named("cap", x, y, z);
                        if (cap != null && (b != cap.block || world.getBlockMetadata(x, y, z) != cap.meta)) {
                            world.setBlock(x, y, z, cap.block, cap.meta, 2);
                            n++;
                        }
                        continue;
                    }
                    Material mat = b.getMaterial();
                    if (mat == Material.air) continue;
                    if (w == SiloMouth.Want.AIR || cover(world, b, mat, x, y, z)) {
                        world.setBlock(x, y, z, air, 0, 2);
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /**
     * Natural cover over a mouth: plants, vines (glow lichen is one, with a tile entity), leaves, snow, cactus,
     * gourds, ice, and logs ({@code isWood}). A block with a tile entity that is not a plant, vine or leaf stays (a
     * chest, another mod's machine), as do stone, dirt and every built block.
     */
    static boolean cover(World world, Block b, Material m, int x, int y, int z) {
        if (m == Material.leaves || m == Material.plants || m == Material.vine) return true;
        if (b.hasTileEntity(world.getBlockMetadata(x, y, z))) return false;
        if (m == Material.snow || m == Material.craftedSnow
            || m == Material.cactus
            || m == Material.gourd
            || m == Material.ice
            || m == Material.packedIce) {
            return true;
        }
        return b.isWood(world, x, y, z) || b.isLeaves(world, x, y, z);
    }

    /** Runs the game keeper once a second per server world. Public for Forge's generated handler. */
    public static final class Ticker {

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
            if (event.world.getTotalWorldTime() % PERIOD != 0) return;
            game().finishReady(event.world);
        }
    }
}
