package com.gregguns.compat.carpenters;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Works out the light value of every Carpenter's Blocks piece as its chunk loads (D345), the complexes' blast doors
 * (D335) and every piece a player placed alike.
 *
 * <p>
 * A Carpenter's tile caches its light value, starting unknown. The first time anything asks, it works the value out and
 * then starts a light update at its own cell. If that first question comes from the pack's lighting engine (ArchaicFix)
 * while it drains a full queue, as the millions of block changes of a nuclear or antimatter blast can make it, the
 * update is a nested run. The engine refuses it with an exception, never clears its busy flag, and the server dies on
 * the next tick. The cache is not saved, so every piece read back from disk starts unknown again.
 *
 * <p>
 * Here the value is set straight into the cache when the chunk loads, with the tile's own method for the value and no
 * light update: the light the chunk was saved with already includes the piece. The lighting engine then never asks a
 * piece first. A piece placed later works its value out at placement, outside any drain.
 *
 * <p>
 * Cost: one pass over the loaded chunk's tile entities, and two reflective calls per Carpenter's piece in it.
 * {@link #stats()} reports it.
 */
// Source:
// https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/tileentity/TEBase.java#L68-L71
// https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/tileentity/TEBase.java#L474-L533
// https://github.com/embeddedt/ArchaicFix/blob/0.8.0/src/main/java/org/embeddedt/archaicfix/lighting/world/lighting/LightingEngine.java#L147-L157
// https://github.com/embeddedt/ArchaicFix/blob/0.8.0/src/main/java/org/embeddedt/archaicfix/lighting/world/lighting/LightingEngine.java#L237-L241
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Blast doors); D345
public final class CarpentersLightPrimer {

    /** Carpenter's Blocks' mod id. */
    public static final String MODID = "CarpentersBlocks";
    /** The base class of every Carpenter's tile, by name. */
    static final String TE_BASE = "com.carpentersblocks.tileentity.TEBase";
    /** The cached light value's field and the method that works it out, as TEBase names them. */
    static final String FIELD = "lightValue", METHOD = "getDynamicLightValue";
    /** The cache's "not worked out yet" value. */
    static final int UNKNOWN = -1;

    private static CarpentersLightPrimer installed;

    private final Class<?> base;
    private final Field light;
    private final Method dynamic;
    private long chunks, pieces, nanos;

    /**
     * @param base the tile base class holding {@link #FIELD} and {@link #METHOD}
     * @throws ReflectiveOperationException when either is missing
     */
    CarpentersLightPrimer(Class<?> base) throws ReflectiveOperationException {
        this.base = base;
        this.light = base.getDeclaredField(FIELD);
        this.light.setAccessible(true);
        this.dynamic = base.getDeclaredMethod(METHOD);
        this.dynamic.setAccessible(true);
    }

    /** Hooks chunk loads when Carpenter's Blocks is loaded; logs and stays off when its tile has changed shape. */
    public static void install() {
        if (installed != null || !Loader.isModLoaded(MODID)) return;
        try {
            installed = new CarpentersLightPrimer(Class.forName(TE_BASE));
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.warn("Carpenter's Blocks light primer is off: {}", e.toString());
            return;
        }
        MinecraftForge.EVENT_BUS.register(installed);
    }

    /**
     * @return a primer that is not hooked to chunk loads, for reading a piece's cache (scenarios), or null without
     *         Carpenter's Blocks
     */
    public static CarpentersLightPrimer reader() {
        if (!Loader.isModLoaded(MODID)) return null;
        try {
            return new CarpentersLightPrimer(Class.forName(TE_BASE));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** @return the installed primer, or null */
    public static CarpentersLightPrimer installed() {
        return installed;
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.world;
        if (world == null || world.isRemote) return;
        long t0 = System.nanoTime();
        Chunk chunk = event.getChunk();
        List<Object> found = null;
        for (Object te : chunk.chunkTileEntityMap.values()) {
            if (base.isInstance(te)) {
                if (found == null) found = new ArrayList<Object>();
                found.add(te);
            }
        }
        // Worked out after the pass: a piece's value reads the world, which may touch the chunk's tile map.
        if (found != null) for (Object te : found) if (prime(te)) pieces++;
        chunks++;
        nanos += System.nanoTime() - t0;
    }

    /**
     * Sets a piece's cached light value if it is still unknown.
     *
     * @param te a Carpenter's tile
     * @return whether the value was set here
     */
    boolean prime(Object te) {
        try {
            if (light.getInt(te) != UNKNOWN) return false;
            light.setInt(te, (Integer) dynamic.invoke(te));
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /**
     * @param te a tile entity
     * @return whether it is a Carpenter's piece whose light value is still unknown (scenarios read this)
     */
    public boolean unknown(Object te) {
        try {
            return base.isInstance(te) && light.getInt(te) == UNKNOWN;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** @return chunk loads seen, pieces primed and the average nanoseconds the hook took per chunk load */
    public long[] stats() {
        return new long[] { chunks, pieces, chunks == 0 ? 0 : nanos / chunks };
    }
}
