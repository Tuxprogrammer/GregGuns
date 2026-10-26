package com.gregguns.worldgen;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.WorldEvent;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Makes structures reach Ross128b and Ross128ba although Galacticraft skips Forge world generators there.
 *
 * <p>
 * Galacticraft's {@code ChunkProviderServerMixin} wraps the {@code GameRegistry.generateWorld} call of
 * {@code ChunkProviderServer.populate} and drops it on every {@code IGalacticraftWorldProvider} world, except for a
 * short whitelist of other mods' generators (GT ore, GalacticGreg, CoFH, Dense Ores, AE2 meteorites, Thaumcraft
 * nodes), unless {@code enableOtherModsFeatures} is on. Both Ross128 providers are Galacticraft worlds, so the
 * {@link StructureGenerator} registered with Forge never runs there. This hook wraps the dimension's chunk provider
 * once, when the world loads, and runs the generator right after the provider's own {@code populate}, inside the same
 * call and on the same chunk, so the populate window and the no-cascade rules (GGS-11-08, 09) hold unchanged. If
 * Galacticraft is configured to allow other mods' generators, Forge calls the generator too; the call that comes
 * second for the same chunk is skipped ({@link StructureGenerator#markRanViaProvider}).
 *
 * <p>
 * Deviation from the M8 design, recorded in structure-placement.md: the standard Forge populate path is not enough on
 * these two dimensions, so they get this one extra hook. Server side.
 */
// Source:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/mixins/early/minecraft/ChunkProviderServerMixin.java
// Source:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/util/WorldUtil.java#L1457-L1470
// Spec: docs/design/0019-structures-on-ross128.md; docs/gregguns-spec/11-worldgen/structure-placement.md
public final class Ross128ProviderHook {

    private static final java.util.concurrent.atomic.AtomicLong POPULATES = new java.util.concurrent.atomic.AtomicLong();

    private final StructureGenerator generator = new StructureGenerator();

    /** @return populate calls that went through a wrapped provider, since boot */
    public static long populateCount() {
        return POPULATES.get();
    }

    /** Wraps the chunk provider of a Ross128 world as it loads. */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.world;
        if (world.isRemote || !WorldgenSettings.generatorWanted()
            || !WorldgenSettings.isRoss128(world.provider.dimensionId)) {
            return;
        }
        if (wrap(world)) {
            GregGuns.LOG.info("Structures: hooked chunk population of dimension {}", world.provider.dimensionId);
        }
    }

    /**
     * @param world a server world
     * @return true if its chunk provider was wrapped now; false if it already was or has no server chunk provider
     */
    boolean wrap(World world) {
        if (!(world.getChunkProvider() instanceof ChunkProviderServer)) {
            return false;
        }
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        if (cps.currentChunkProvider == null || cps.currentChunkProvider instanceof Wrapped) {
            return false;
        }
        cps.currentChunkProvider = new Wrapped(cps.currentChunkProvider, world, generator);
        return true;
    }

    /** The dimension's own provider plus one call to the structure generator after its populate. */
    static final class Wrapped implements IChunkProvider {

        private final IChunkProvider inner;
        private final World world;
        private final StructureGenerator generator;

        Wrapped(IChunkProvider inner, World world, StructureGenerator generator) {
            this.inner = inner;
            this.world = world;
            this.generator = generator;
        }

        @Override
        public void populate(IChunkProvider provider, int cx, int cz) {
            POPULATES.incrementAndGet();
            inner.populate(provider, cx, cz);
            StructureGenerator.markRanViaProvider(world.provider.dimensionId, cx, cz);
            generator.generate(new Random(), cx, cz, world, inner, provider);
        }

        @Override
        public boolean chunkExists(int x, int z) {
            return inner.chunkExists(x, z);
        }

        @Override
        public Chunk provideChunk(int x, int z) {
            return inner.provideChunk(x, z);
        }

        @Override
        public Chunk loadChunk(int x, int z) {
            return inner.loadChunk(x, z);
        }

        @Override
        public boolean saveChunks(boolean all, IProgressUpdate progress) {
            return inner.saveChunks(all, progress);
        }

        @Override
        public boolean unloadQueuedChunks() {
            return inner.unloadQueuedChunks();
        }

        @Override
        public boolean canSave() {
            return inner.canSave();
        }

        @Override
        public String makeString() {
            return inner.makeString();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
            return inner.getPossibleCreatures(type, x, y, z);
        }

        @Override
        public ChunkPosition func_147416_a(World w, String name, int x, int y, int z) {
            return inner.func_147416_a(w, name, x, y, z);
        }

        @Override
        public int getLoadedChunkCount() {
            return inner.getLoadedChunkCount();
        }

        @Override
        public void recreateStructures(int x, int z) {
            inner.recreateStructures(x, z);
        }

        @Override
        public void saveExtraData() {
            inner.saveExtraData();
        }
    }
}
