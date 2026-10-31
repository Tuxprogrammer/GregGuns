package com.gregguns.icbm.soak;

import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;

import com.gregguns.GregGuns;
import com.gregguns.explosion.nuclear.FalloutEngine;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.warhead.EntityFlyingBlock;

/**
 * One sample of what the ICBM soak watches on a running server: tick time, GregGuns chunk tickets and the chunks they
 * force, loaded chunks, entities by kind, the blast engine's queue and fallout, and the heap. The soak scenarios take
 * one before and after each burst, and {@code /gregguns soakstat} prints one for the external driver. Dev environment
 * only. Server thread.
 *
 * <p>
 * Source: Forge 1.7.10-10.13.4.1614 {@code net.minecraftforge.common.ForgeChunkManager}:
 * {@code ticketCountAvailableFor(mod, world)} is the mod's ticket limit minus the tickets it holds in that world, and
 * {@code getPersistentChunksFor(world)} maps every forced chunk to the tickets forcing it. Vanilla 1.7.10
 * {@code MinecraftServer.tickTimeArray} holds the last 100 tick times in nanoseconds.
 */
// Spec: docs/gregguns-icbm-spec/01-core/soak.md
public final class SoakProbe {

    /** Length of {@code MinecraftServer.tickTimeArray}. */
    private static final int TICK_ARRAY = 100;

    /** Mean of the last 100 tick times, ms. */
    public double tickMeanMs;
    /** Longest of the last 100 tick times, ms. */
    public double tickMaxMs;
    /** Chunk tickets GregGuns holds, all worlds. */
    public int tickets;
    /** Chunks GregGuns tickets force, all worlds. */
    public int forcedChunks;
    /** Chunks every mod's tickets force, all worlds. */
    public int allForcedChunks;
    /** Loaded chunks, all worlds. */
    public int loadedChunks;
    /** Loaded worlds. */
    public int worlds;
    /** Loaded entities, all worlds. */
    public int entities;
    /** Missiles off their pads (warm-up, climb, arc). */
    public int missilesFlying;
    /** Every missile entity, pads included. */
    public int missiles;
    /** Anti-gravitational flying blocks. */
    public int flyingBlocks;
    /** Item entities. */
    public int items;
    /** ICBM blasts running. */
    public int blasts;
    /** Blocks still waiting to break across every blast. */
    public int pendingBlocks;
    /** Bytes the blast ray-cast cubes hold. */
    public long gridBytes;
    /** Irradiated blocks waiting to decay, all worlds. */
    public int falloutBlocks;
    /** Heap in use, MB. */
    public long heapUsedMb;

    /** @return a sample of the running server now; all zero when there is none */
    public static SoakProbe sample() {
        SoakProbe p = new SoakProbe();
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null) {
            return p;
        }
        long sum = 0L;
        long max = 0L;
        for (int i = 0; i < TICK_ARRAY; i++) {
            long t = server.tickTimeArray[i];
            sum += t;
            max = Math.max(max, t);
        }
        p.tickMeanMs = sum / (double) TICK_ARRAY / 1.0e6;
        p.tickMaxMs = max / 1.0e6;
        for (WorldServer w : server.worldServers) {
            if (w == null) {
                continue;
            }
            p.worlds++;
            p.loadedChunks += w.theChunkProviderServer.getLoadedChunkCount();
            p.tickets += ticketsHeld(w);
            for (Map.Entry<ChunkCoordIntPair, ForgeChunkManager.Ticket> e : ForgeChunkManager.getPersistentChunksFor(w)
                .entries()) {
                p.allForcedChunks++;
                if (GregGuns.MODID.equals(
                    e.getValue()
                        .getModId())) {
                    p.forcedChunks++;
                }
            }
            p.falloutBlocks += FalloutEngine.instance()
                .irradiatedBlocks(w);
            for (Object o : w.loadedEntityList) {
                p.entities++;
                if (o instanceof EntityMissile) {
                    p.missiles++;
                    if (!((EntityMissile) o).onPad() && !((EntityMissile) o).isDead) {
                        p.missilesFlying++;
                    }
                } else if (o instanceof EntityFlyingBlock) {
                    p.flyingBlocks++;
                } else if (o instanceof EntityItem) {
                    p.items++;
                }
            }
        }
        BlastEngine engine = BlastEngine.instance();
        p.blasts = engine.activeBlasts();
        p.pendingBlocks = engine.pendingBlocks();
        p.gridBytes = com.gregguns.icbm.blast.GridMemory.shared()
            .used();
        Runtime rt = Runtime.getRuntime();
        p.heapUsedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        return p;
    }

    /**
     * @param world a server world
     * @return the chunk tickets GregGuns holds in it (the limit minus what Forge would still hand out)
     */
    public static int ticketsHeld(WorldServer world) {
        try {
            return ForgeChunkManager.getMaxTicketLengthFor(GregGuns.MODID)
                - ForgeChunkManager.ticketCountAvailableFor(GregGuns.instance, world);
        } catch (RuntimeException e) {
            // Forge keeps no ticket table for a world that never asked for one.
            return 0;
        }
    }

    /** @return the one-line form the external driver parses: {@code key=value} pairs, space separated */
    public String line() {
        return String.format(
            Locale.ROOT,
            "tick_mean_ms=%.2f tick_max_ms=%.2f tickets=%d forced=%d forced_all=%d loaded_chunks=%d worlds=%d"
                + " entities=%d missiles=%d flying=%d flying_blocks=%d items=%d blasts=%d pending_blocks=%d"
                + " grid_mb=%.1f fallout_blocks=%d heap_mb=%d",
            tickMeanMs,
            tickMaxMs,
            tickets,
            forcedChunks,
            allForcedChunks,
            loadedChunks,
            worlds,
            entities,
            missiles,
            missilesFlying,
            flyingBlocks,
            items,
            blasts,
            pendingBlocks,
            gridBytes / (1024.0 * 1024.0),
            falloutBlocks,
            heapUsedMb);
    }
}
