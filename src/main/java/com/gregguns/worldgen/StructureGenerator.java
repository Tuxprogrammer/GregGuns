package com.gregguns.worldgen;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.GregGuns;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.storage.BlockGregChest;
import com.gregguns.utility.storage.TileGregChest;
import com.gregguns.worldgen.camp.CampPlacer;
import com.gregguns.worldgen.camp.CarrierFlag;
import com.gregguns.worldgen.camp.PartLayouts;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LazyChests;
import com.gregguns.worldgen.loot.LootPending;
import com.gregguns.worldgen.nest.NestPlacer;
import com.gregguns.worldgen.spawner.HoleSpawnerHook;

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
    /** The procedural underground complex (ICBM silo complex), set by its module; null when absent. */
    private static volatile ComplexWriter complexWriter;

    /** @param writer the complex writer, or null for none */
    public static void setComplexWriter(ComplexWriter writer) {
        complexWriter = writer;
    }

    /** @return whether the silo complex is on in this non-Overworld world (its chunk provider then needs the hook) */
    static boolean complexWanted(World world) {
        ComplexWriter c = complexWriter;
        return c != null && world.provider.dimensionId != 0 && c.enabledIn(world);
    }

    /** Replaces {@link #real} while a scenario runs (forced spacing, a test layout). Tests only. */
    private static volatile StructurePopulator override;
    /** Replaces the world biome lookup while a scenario runs (a desert square for the nest). Tests only. */
    private static volatile StructurePopulator.BiomeSource biomeOverride;

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
        // The base needs the Camp Flag block (utility module); without it the BIG land pool stays empty.
        Block flagBlock = WorldBlockRegistration.campFlag();
        boolean camps = flagBlock != null;
        StructureGrid grid = new StructureGrid(
            com.gregguns.core.config.WorldgenConfig.spacingSmall,
            com.gregguns.core.config.WorldgenConfig.spacingMedium,
            com.gregguns.core.config.WorldgenConfig.spacingBig,
            StructureCatalog.pools(layouts, WorldgenSettings::enabled, camps, true));
        real = new StructurePopulator(
            grid,
            layouts,
            new Stamper(keys),
            Blocks.dirt,
            Blocks.air,
            StructurePopulator.CONFIG_RULES,
            new CampPlacer(keys, new PartLayouts.FromRegistry(layouts)),
            flagBlock,
            UtilityConfig.campFlagCapFactor).withNests(nestPlacer(keys));
        // Layout structures that carry a camp (the carrier) place their own flag from the layout marks.
        StructureHooks.setFlagHook(new CarrierFlag(layouts, flagBlock));
        // Spawner marks (the Big Bunker's zombie holes, the Bug Nest's bug holes) become hole spawner blocks (G11).
        StructureHooks.setSpawnerHook(new HoleSpawnerHook());
    }

    private static NestPlacer nestPlacer(PaletteKeys keys) {
        try {
            return new NestPlacer(keys, Blocks.air);
        } catch (IllegalStateException e) {
            // A nest key did not resolve: no nest, the rest of worldgen is unaffected.
            GregGuns.LOG.error("Alien Bug Nest disabled: " + e.getMessage());
            return null;
        }
    }

    /** @param populator replaces the real one until cleared with null; scenarios only */
    public static void setOverride(StructurePopulator populator) {
        override = populator;
    }

    /** @param source replaces the biome lookup until cleared with null; scenarios only */
    public static void setBiomeOverride(StructurePopulator.BiomeSource source) {
        biomeOverride = source;
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

    /** The chunk {@link Ross128ProviderHook} just generated, so Forge's own call for it is skipped once. */
    private static long ranViaProvider = Long.MIN_VALUE;
    private static int ranViaProviderDim;

    /** Records that the provider hook generated this chunk; the next Forge call for the same chunk does nothing. */
    static void markRanViaProvider(int dimension, int chunkX, int chunkZ) {
        ranViaProviderDim = dimension;
        ranViaProvider = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
        IChunkProvider chunkProvider) {
        if (world.isRemote) return;
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        if (ranViaProvider == key && ranViaProviderDim == world.provider.dimensionId) {
            // The call that follows the hook's own (Galacticraft allows other mods' generators): already done.
            ranViaProvider = Long.MIN_VALUE;
            return;
        }
        StructurePopulator populator = override != null ? override : real;
        if (populator == null) return;
        ComplexWriter complex = complexWriter;
        boolean general = allowed(world);
        // A complex the ledger already placed keeps generating when the config now keeps new ones out (a dimension
        // removed from the list, the chance lowered, a world type excluded): only new decisions stop.
        if (complex != null && !complex.enabledIn(world)) complex = new LedgerOnly(complex);
        if (!general && complex == null) return;
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
                biomeOverride != null ? biomeOverride
                    : (x, z) -> StructureBiomes.typesAt(world.getWorldChunkManager(), x, z),
                StructureState.forWorld(world),
                new WorldMarks(world, sink),
                complex,
                general);
            STAMPED.addAndGet(n);
            ACCEPTED.addAndGet(sink.acceptedWrites());
            REJECTED.addAndGet(sink.rejectedWrites());
        } catch (RuntimeException e) {
            // A broken structure must not kill chunk generation; the log names the chunk.
            GregGuns.LOG.error("Structure generation failed at chunk " + chunkX + "," + chunkZ, e);
        }
    }

    /** A complex writer that decides nothing new: its chance is 0, so only plans the ledger placed are written. */
    private static final class LedgerOnly implements ComplexWriter {

        private final ComplexWriter inner;

        LedgerOnly(ComplexWriter inner) {
            this.inner = inner;
        }

        @Override
        public boolean enabledIn(World world) {
            return false;
        }

        @Override
        public int chancePercent() {
            return 0;
        }

        @Override
        public boolean biomeOk(int dimension, Set<Type> types) {
            return false;
        }

        @Override
        public int minGroundY() {
            return inner.minGroundY();
        }

        @Override
        public void skipped(StructurePlan plan, String reason) {
            inner.skipped(plan, reason);
        }

        @Override
        public int write(StructurePlan plan, int groundY, PopulateWindow window, WindowedSink sink, HeightProbe probe) {
            return inner.write(plan, groundY, window, sink, probe);
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
    static final class WorldMarks implements StructurePopulator.Marks {

        private final World world;
        private final WindowedSink sink;

        WorldMarks(World world, WindowedSink sink) {
            this.world = world;
            this.sink = sink;
        }

        @Override
        public void chest(StructurePlan plan, String kind, int x, int y, int z, LootPending order) {
            if (kind.equals("vanilla") || kind.equals("barracks")) {
                // The layout placed a vanilla chest here (camp barracks chests are vanilla too); the sink wrote it
                // straight to the world, so its tile exists.
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
