package com.gregguns.worldgen;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.worldgen.camp.CampFlags;
import com.gregguns.worldgen.camp.CampFootprint;
import com.gregguns.worldgen.camp.CampPlan;
import com.gregguns.worldgen.camp.CampWriter;
import com.gregguns.worldgen.camp.PlacedCamp;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.Rotations;
import com.gregguns.worldgen.layout.StampPlan;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LootPending;
import com.gregguns.worldgen.nest.NestPlacer;
import com.gregguns.worldgen.nest.NestPlan;

/**
 * The populate pipeline of one window, with every world touch behind an interface so it runs in unit tests. For each
 * plan that overlaps the window: look up (or make) the placement decision, shape the terrain, stamp, and report marks.
 *
 * <p>
 * Every write goes through the {@link WindowedSink} and every read through a {@link ColumnReader} wrapped in a
 * {@link WindowedHeightProbe}, so nothing here can reach outside the window (GGS-11-08, GGS-11-09). The first window to
 * see a plan decides it from its own columns and records the verdict; later windows obey the record (PLAN-M8 Q3).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-08 to GGS-11-15); fixes WGEN-04, 05, 06, 15, 26
public final class StructurePopulator {

    /** Largest land height spread a footprint may have before it is rejected (also the flatten band). */
    public static final int LAND_MAX_DIFF = 8;
    /** Sample spacing for the terrain checks; the far edge is always sampled (WGEN-15). */
    public static final int SAMPLE_STEP = 4;
    /** Water a ship's hull needs under its keel, in blocks. */
    public static final int SHIP_MIN_DEPTH = 6;

    /** Biome types of a block column, read without loading a chunk. */
    public interface BiomeSource {

        Set<Type> typesAt(int x, int z);
    }

    /** What the generator does with the layout marks that fall in the window. */
    public interface Marks {

        /**
         * @param kind  {@code vanilla}, {@code weapon} or {@code reinforced}
         * @param order the loot order for this chest, seeded per chest (GGS-11-20)
         */
        void chest(StructurePlan plan, String kind, int x, int y, int z, LootPending order);

        void spawner(StructurePlan plan, String tag, int x, int y, int z);

        void flag(StructurePlan plan, String planKey, int x, int y, int z);
    }

    /** Per-structure settings, so tests need no config. */
    public interface Rules {

        boolean enabled(StructureId s);

        Set<Type> biomeBlacklist(StructureId s);

        int lootCeiling(StructureId s);

        int richness(StructureId s);

        /**
         * @param plan a plan
         * @return the loot ceiling the plan rolled (a GT voltage index, MV 2 to UHV 9), or 0 when its dimension does
         *         not roll (everywhere but Ross128b and Ross128ba, design record 0019)
         */
        default int rolledCeiling(StructurePlan plan) {
            return 0;
        }
    }

    /** The settings from {@link WorldgenSettings}. */
    public static final Rules CONFIG_RULES = new Rules() {

        @Override
        public boolean enabled(StructureId s) {
            return WorldgenSettings.enabled(s);
        }

        @Override
        public Set<Type> biomeBlacklist(StructureId s) {
            return StructureBiomes.parseTypes(WorldgenSettings.biomeBlacklist(s));
        }

        @Override
        public int lootCeiling(StructureId s) {
            return WorldgenSettings.lootTierCeiling(s);
        }

        @Override
        public int richness(StructureId s) {
            return WorldgenSettings.lootTier(s);
        }

        @Override
        public int rolledCeiling(StructurePlan plan) {
            return WorldgenSettings.rolledCeiling(plan);
        }
    };

    private final StructureGrid grid;
    private final LayoutRegistry layouts;
    private final Stamper stamper;
    private final Block fill;
    private final Block air;
    private final Rules rules;
    private final CampWriter camps;
    private final Block flagBlock;
    private final float capFactor;
    private NestPlacer nests;

    /**
     * @param grid    the seeded plans
     * @param layouts the loaded layouts
     * @param stamper writes a layout into a window
     * @param fill    the block that raises low columns (dirt)
     * @param air     the block that cuts high columns
     * @param rules   per-structure settings
     */
    public StructurePopulator(StructureGrid grid, LayoutRegistry layouts, Stamper stamper, Block fill, Block air,
        Rules rules) {
        this(grid, layouts, stamper, fill, air, rules, null, null, 0f);
    }

    /**
     * As above, plus the Military Base: plans of that id are written by {@code camps} (no layout file), and the flag
     * step places {@code flagBlock} with the cap from {@code capFactor}.
     */
    public StructurePopulator(StructureGrid grid, LayoutRegistry layouts, Stamper stamper, Block fill, Block air,
        Rules rules, CampWriter camps, Block flagBlock, float capFactor) {
        this.camps = camps;
        this.flagBlock = flagBlock;
        this.capFactor = capFactor;
        this.grid = grid;
        this.layouts = layouts;
        this.stamper = stamper;
        this.fill = fill;
        this.air = air;
        this.rules = rules;
    }

    /**
     * Adds the Alien Bug Nest: plans of that id are written by {@code placer} (procedural, no layout file).
     *
     * @return this
     */
    public StructurePopulator withNests(NestPlacer placer) {
        this.nests = placer;
        return this;
    }

    /** @return the seeded plans, for scenarios that need to know where this populator will build */
    StructureGrid grid() {
        return grid;
    }

    /**
     * Builds the part of every overlapping structure that lies in the window.
     *
     * @return the number of structures that wrote at least one block in this window
     */
    public int populate(long worldSeed, int dimension, PopulateWindow window, WindowedSink sink, ColumnReader reader,
        BiomeSource biomes, StructureState state, Marks marks) {
        return populatePlans(
            grid.plansForWindow(worldSeed, dimension, window.chunkX(), window.chunkZ()),
            window,
            sink,
            reader,
            biomes,
            state,
            marks);
    }

    /**
     * As {@link #populate} for plans the caller already chose: the seam the per-structure placement scenarios use to
     * force a plan at a prepared test area. Same decision, shaping, stamping and marks.
     *
     * @return the number of structures that wrote at least one block in this window
     */
    public int populatePlans(List<StructurePlan> plans, PopulateWindow window, WindowedSink sink, ColumnReader reader,
        BiomeSource biomes, StructureState state, Marks marks) {
        int stamped = 0;
        for (StructurePlan plan : plans) {
            StructureId id = StructureCatalog.byId(plan.id());
            if (camps != null && id == StructureId.MILITARY_BASE) {
                stamped += populateCamp(plan, window, sink, reader, biomes, state, marks);
                continue;
            }
            if (id == StructureId.ALIEN_BUG_NEST) {
                if (nests != null) stamped += populateNest(plan, id, window, sink, reader, biomes, state, marks);
                continue;
            }
            Layout layout = layouts.get(plan.id());
            if (layout == null) continue;
            PlacedFootprint fp = new PlacedFootprint(
                plan.anchorX(),
                plan.anchorZ(),
                layout.sizeX(),
                layout.sizeZ(),
                plan.rotation());
            boolean water = id != null && StructureCatalog.isWater(id);
            HeightProbe probe = new WindowedHeightProbe(window, reader);
            Decision d = state.decideOnce(
                PlanKey.of(plan),
                () -> decide(layout.sizeY() - layout.ground(), id, water, fp, window, reader, probe, biomes));
            if (!d.isPlaced()) continue;
            if (!water) {
                shapeLand(sink, probe, reader, window, fp, d.groundY());
            }
            StampPlan sp = new StampPlan(
                layout,
                plan.anchorX(),
                d.groundY() + 1,
                plan.anchorZ(),
                plan.rotation(),
                plan.variant());
            String key = PlanKey.of(plan);
            int written = stamper.write(
                sink,
                sp,
                window,
                (tag, x, y, z) -> mark(plan, id, key, tag, x, y, z, chestIndex(sp, x, y, z), marks));
            if (written > 0) stamped++;
        }
        return stamped;
    }

    /**
     * The Military Base slice of one window (G10 contract): plan, decide once, shape the land, write every window the
     * camp's own {@code overlaps} accepts (the road tails reach past the box), then run the flag step once in the
     * window that holds the flag. The decision is recorded before the flag step, or {@code addFlag} is ignored.
     *
     * @return 1 when the camp wrote anything in this window, else 0
     */
    private int populateCamp(StructurePlan plan, PopulateWindow window, WindowedSink sink, ColumnReader reader,
        BiomeSource biomes, StructureState state, Marks marks) {
        String key = PlanKey.of(plan);
        // Below the abort size the plan is null: reject (cannot happen with sizeFor, which is 32 or more).
        CampPlan cp = CampPlan.generate(
            plan.shapeSeed(),
            plan.footprint()
                .sizeX(),
            plan.footprint()
                .sizeZ());
        PlacedFootprint fp = new PlacedFootprint(
            plan.anchorX(),
            plan.anchorZ(),
            plan.footprint()
                .sizeX(),
            plan.footprint()
                .sizeZ(),
            plan.rotation());
        // A window that only touches the road tails cannot sample the footprint, so it must not decide: it waits for
        // a record (a tail-only window populated first misses its 3 blocks of road; accepted, see structure-placement).
        if (state.get(key) == null && !window.overlapsBox(fp.minX(), fp.minZ(), fp.maxX(), fp.maxZ())) return 0;
        HeightProbe probe = new WindowedHeightProbe(window, reader);
        Decision d = state.decideOnce(
            key,
            () -> cp == null ? Decision.rejected("size")
                : decide(CampFootprint.HEIGHT, StructureId.MILITARY_BASE, false, fp, window, reader, probe, biomes));
        if (!d.isPlaced() || cp == null) return 0;
        PlacedCamp camp = cp.place(plan.anchorX(), d.groundY() + 1, plan.anchorZ(), plan.rotation());
        if (!camp.overlaps(window)) return 0;
        shapeLand(sink, probe, reader, window, fp, d.groundY());
        // Camo comes from the biome at the base center (WGEN-32), read without loading a chunk.
        StructureBiomes.CampColor color = StructureBiomes
            .campColor(biomes.typesAt((fp.minX() + fp.maxX()) / 2, (fp.minZ() + fp.maxZ()) / 2));
        int written = camps.write(sink, camp, window, color, (tag, x, y, z) -> {
            // The flag is the post-step's; camp parts carry only chest and spawner marks. The chest index is
            // the position hash, so a chest's seed does not depend on which window reports it (GGS-11-20).
            if (tag.equals("flag")) return;
            mark(
                plan,
                StructureId.MILITARY_BASE,
                key,
                tag,
                x,
                y,
                z,
                (x * 73856093) ^ (y * 19349663) ^ (z * 83492791),
                marks);
        });
        boolean flagged = false;
        if (window.contains(camp.flagPos().x, camp.flagPos().z)) {
            flagged = camps.flagStep(camp, flagBlock, capFactor, CampFlags.into(state, key))
                .run(sink, window);
        }
        return written > 0 || flagged ? 1 : 0;
    }

    /**
     * The Alien Bug Nest slice of one window: decide once (biome, terrain, village scan, and ground high enough for the
     * deepest chamber; a failed check skips the nest, WGEN-09), then write the cells of the window. The terrain is not
     * flattened: the nest is underground and only its shaft mouth reaches the surface.
     *
     * @return 1 when the nest wrote anything in this window, else 0
     */
    private int populateNest(StructurePlan plan, StructureId id, PopulateWindow window, WindowedSink sink,
        ColumnReader reader, BiomeSource biomes, StructureState state, Marks marks) {
        String key = PlanKey.of(plan);
        PlacedFootprint fp = new PlacedFootprint(plan.anchorX(), plan.anchorZ(), NestPlan.SIZE, NestPlan.SIZE, 0);
        HeightProbe probe = new WindowedHeightProbe(window, reader);
        Decision d = state.decideOnce(key, () -> {
            Decision r = decide(NestPlan.ABOVE, id, false, fp, window, reader, probe, biomes);
            if (r.isPlaced() && r.groundY() < NestPlan.MIN_GROUND_Y) return Decision.rejected("low");
            return r;
        });
        if (!d.isPlaced()) return 0;
        NestPlan np = NestPlan.generate(plan.chestSeedBase(), plan.anchorX(), plan.anchorZ(), d.groundY());
        int written = nests.write(sink, np, window, (tag, x, y, z) -> mark(plan, id, key, tag, x, y, z, 0, marks));
        return written > 0 ? 1 : 0;
    }

    private Decision decide(int height, StructureId id, boolean water, PlacedFootprint fp, PopulateWindow window,
        ColumnReader reader, HeightProbe probe, BiomeSource biomes) {
        Set<Type> types = biomes.typesAt((fp.minX() + fp.maxX()) / 2, (fp.minZ() + fp.maxZ()) / 2);
        Set<Type> blacklist = id == null ? Collections.<Type>emptySet() : rules.biomeBlacklist(id);
        boolean biomeOk;
        if (id == StructureId.ALIEN_BUG_NEST) {
            biomeOk = StructureBiomes.allowsBugNest(types, StructureBiomes.BUG_NEST_DEFAULT)
                && StructureBiomes.allowsLand(types, blacklist);
        } else if (water) {
            biomeOk = StructureBiomes.allowsWater(types, blacklist);
        } else {
            biomeOk = StructureBiomes.allowsLand(types, blacklist);
        }
        if (!biomeOk) return Decision.rejected("biome");

        TerrainCheck.Result r = water ? TerrainCheck.checkWater(probe, fp, SHIP_MIN_DEPTH, SAMPLE_STEP)
            : TerrainCheck.checkLand(probe, fp, LAND_MAX_DIFF, SAMPLE_STEP);
        if (r.reason == TerrainCheck.Reason.NO_SAMPLES) {
            // The coarse sample grid missed this window's sliver of the footprint; sample every column of it. The
            // plan overlaps the window, so at least one column exists.
            r = water ? TerrainCheck.checkWater(probe, fp, SHIP_MIN_DEPTH, 1)
                : TerrainCheck.checkLand(probe, fp, LAND_MAX_DIFF, 1);
        }
        if (!r.ok) {
            return Decision.rejected(
                r.reason.name()
                    .toLowerCase(Locale.ROOT));
        }
        if (!water && VillageScan.containsVillageBlocks(window, reader, fp, r.y, height)) {
            return Decision.rejected("village");
        }
        return Decision.placed(r.y);
    }

    /** Flattens the footprint columns of this window to the recorded height and clears trees above them. */
    private void shapeLand(WindowedSink sink, HeightProbe probe, ColumnReader reader, PopulateWindow window,
        PlacedFootprint fp, int groundY) {
        TerrainShaper.apply(sink, Flatten.planToBand(probe, fp, groundY, LAND_MAX_DIFF), fill, 0, air);
        for (int x = Math.max(fp.minX(), window.minX()); x <= Math.min(fp.maxX(), window.maxX()); x++) {
            for (int z = Math.max(fp.minZ(), window.minZ()); z <= Math.min(fp.maxZ(), window.maxZ()); z++) {
                for (int y : TerrainShaper.junkY(window, reader, x, z)) {
                    sink.setBlock(x, y, z, air, 0);
                }
            }
        }
    }

    private void mark(StructurePlan plan, StructureId id, String planKey, String tag, int x, int y, int z,
        int chestIndex, Marks marks) {
        if (tag.startsWith("chest.")) {
            String kind = tag.substring("chest.".length());
            int rolled = rules.rolledCeiling(plan);
            int base = Math.max(1, id == null ? 1 : rules.lootCeiling(id));
            // Ross128: the structure's own rolled ceiling, never below the configured one (design record 0019).
            int tier = rolled > 0 ? Math.max(base, rolled) : Math.min(3, base);
            String category = ChestTags.category(kind, id == StructureId.MILITARY_BASE, tier, rolled > 0);
            if (category == null) return;
            int richness = id == null ? 1 : rules.richness(id);
            long seed = plan.chestSeed(chestIndex);
            marks.chest(plan, kind, x, y, z, new LootPending(category, richness, seed));
        } else if (tag.startsWith("spawner")) {
            marks.spawner(plan, tag, x, y, z);
        } else if (tag.equals("flag")) {
            marks.flag(plan, planKey, x, y, z);
        }
    }

    /** The chest's index among the layout's chest marks: the same whichever window reports it (GGS-11-20). */
    static int chestIndex(StampPlan sp, int wx, int wy, int wz) {
        Layout l = sp.layout;
        int n = 0;
        for (Layout.Mark m : l.marks()) {
            if (!m.tag.startsWith("chest.")) continue;
            int mx = sp.anchorX + Rotations.offsetX(sp.rotation, m.x, m.z, l.sizeX(), l.sizeZ());
            int mz = sp.anchorZ + Rotations.offsetZ(sp.rotation, m.x, m.z, l.sizeX(), l.sizeZ());
            int my = sp.anchorY + m.y - l.ground();
            if (mx == wx && my == wy && mz == wz) return n;
            n++;
        }
        return 0;
    }
}
