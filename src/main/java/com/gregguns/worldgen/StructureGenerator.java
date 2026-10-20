package com.gregguns.worldgen;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import com.gregguns.GregGuns;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.storage.BlockGregChest;
import com.gregguns.utility.storage.TileGregChest;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LazyChests;
import com.gregguns.worldgen.loot.LootPending;

import cpw.mods.fml.common.IWorldGenerator;

/**
 * The structure generator (GGS-11-07): one {@link IWorldGenerator}, weight 4, registered once and only when
 * {@link WorldgenSettings#generatorWanted()}. Each populate call gates on dimension and world type, then hands the
 * window to a {@link StructurePopulator}. The whole call runs inside a {@link PopulateGuard} scope, every write goes
 * through a {@link ClippingSink}, and nothing here asks the chunk provider for another chunk (GGS-11-08, 09).
 *
 * <p>
 * Marks: chest marks become a lazy {@link TileGregChest} holding a {@link LootPending}, or a vanilla chest filled now
 * (a vanilla tile cannot hold a pending order, PLAN-M8 Q8). Spawner and flag marks go to {@link StructureHooks}, which
 * G11 and G10 fill in.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-07 to GGS-11-12); structure-loot.md (GGS-11-20)
public final class StructureGenerator implements IWorldGenerator {

    /** The registration weight (GGS-11-07). GT forces its ore pass last whatever its weight. */
    public static final int WEIGHT = 4;

    /** What a generator needs to run: the populator and its source of truth for the grid. */
    private static volatile StructurePopulator real;
    /** Replaces {@link #real} while a scenario runs (forced spacing, a test layout). Tests only. */
    private static volatile StructurePopulator override;

    private static final AtomicLong STAMPED = new AtomicLong();
    private static final AtomicLong ACCEPTED = new AtomicLong();
    private static final AtomicLong REJECTED = new AtomicLong();
    private static final AtomicLong POPULATES = new AtomicLong();

    /**
     * Installs the real populator. Called once from postInit, after the layouts load.
     *
     * @param layouts the loaded layouts
     * @param keys    the resolved palette keys
     */
    public static void configure(LayoutRegistry layouts, PaletteKeys keys) {
        StructureGrid grid = new StructureGrid(
            com.gregguns.core.config.WorldgenConfig.spacingSmall,
            com.gregguns.core.config.WorldgenConfig.spacingMedium,
            com.gregguns.core.config.WorldgenConfig.spacingBig,
            StructureCatalog.pools(layouts, WorldgenSettings::enabled));
        real = new StructurePopulator(
            grid,
            layouts,
            new Stamper(keys),
            Blocks.dirt,
            Blocks.air,
            StructurePopulator.CONFIG_RULES);
    }

    /** @param populator replaces the real one until cleared with null; scenarios only */
    public static void setOverride(StructurePopulator populator) {
        override = populator;
    }

    /** @return structures that wrote at least one block, since boot */
    public static long stampedCount() {
        return STAMPED.get();
    }

    /** @return writes the clipping sinks passed to the world, since boot */
    public static long acceptedWrites() {
        return ACCEPTED.get();
    }

    /** @return writes the clipping sinks dropped, since boot; non-zero means a stamper bug (GGS-11-08) */
    public static long rejectedWrites() {
        return REJECTED.get();
    }

    /** @return populate calls that got past the gate and ran the populator, since boot */
    public static long populateCount() {
        return POPULATES.get();
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
        IChunkProvider chunkProvider) {
        if (world.isRemote) return;
        StructurePopulator populator = override != null ? override : real;
        if (populator == null) return;
        if (!allowed(world)) return;
        try (PopulateGuard.Scope ignored = PopulateGuard.enter(chunkX, chunkZ)) {
            PopulateWindow window = new PopulateWindow(chunkX, chunkZ);
            ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
            POPULATES.incrementAndGet();
            int n = populator.populate(
                world.getSeed(),
                world.provider.dimensionId,
                window,
                sink,
                new WorldColumnReader(world),
                (x, z) -> StructureBiomes.typesAt(world.getWorldChunkManager(), x, z),
                StructureState.forWorld(world),
                new WorldMarks(world, sink));
            STAMPED.addAndGet(n);
            ACCEPTED.addAndGet(sink.acceptedWrites());
            REJECTED.addAndGet(sink.rejectedWrites());
        } catch (RuntimeException e) {
            // A broken structure must not kill chunk generation; the log names the chunk.
            GregGuns.LOG.error("Structure generation failed at chunk " + chunkX + "," + chunkZ, e);
        }
    }

    /** Dimension allow-list and world type gate (GGS-11-12). */
    static boolean allowed(World world) {
        return WorldgenSettings.placementAllowed(
            world.provider.dimensionId,
            world.getWorldInfo()
                .getTerrainType()
                .getWorldTypeName());
    }

    /** The world-touching mark steps. */
    private static final class WorldMarks implements StructurePopulator.Marks {

        private final World world;
        private final WindowedSink sink;

        WorldMarks(World world, WindowedSink sink) {
            this.world = world;
            this.sink = sink;
        }

        @Override
        public void chest(StructurePlan plan, String kind, int x, int y, int z, LootPending order) {
            if (kind.equals("vanilla")) {
                // The layout placed a vanilla chest here; the sink wrote it straight to the world, so its tile exists.
                LazyChests.fillVanillaChest(world, x, y, z, order);
                return;
            }
            // The shipped layouts mark weapon and reinforced chests on vanilla chest blocks (a layout cannot name a
            // block the pack may not have). Put the GregGuns chest block there with the same facing before attaching
            // the order; a GregGuns tile under a vanilla chest block crashes TileEntityChest's neighbor scan.
            BlockGregChest want = kind.equals("reinforced") ? WorldBlockRegistration.reinforcedChest()
                : WorldBlockRegistration.weaponChest();
            if (want == null) {
                // Utility module off: keep the vanilla chest and fill it now, so the structure still has loot.
                LazyChests.fillVanillaChest(world, x, y, z, order);
                return;
            }
            if (world.getBlock(x, y, z) != want) {
                sink.setBlock(x, y, z, want, world.getBlockMetadata(x, y, z));
            }
            // Attaches the unfilled order (lazy fill, Q8).
            TileGregChest tile = new TileGregChest();
            tile.setPending(order);
            sink.setTile(x, y, z, tile);
        }

        @Override
        public void spawner(StructurePlan plan, String tag, int x, int y, int z) {
            StructureHooks.spawner()
                .spawnerMarked(world, sink, plan, tag, x, y, z);
        }

        @Override
        public void flag(StructurePlan plan, String planKey, int x, int y, int z) {
            StructureHooks.flag()
                .flagMarked(world, sink, plan, planKey, x, y, z);
        }
    }
}
