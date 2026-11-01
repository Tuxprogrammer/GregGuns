package com.gregguns.icbm.complex.gen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.GregGuns;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.PlanetTable;
import com.gregguns.icbm.complex.SiloSlot;
import com.gregguns.icbm.complex.SurfaceVariant;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.icbm.worldgen.ComplexApplier;
import com.gregguns.icbm.worldgen.ComplexContents;
import com.gregguns.icbm.worldgen.ComplexFixtures;
import com.gregguns.icbm.worldgen.FixtureWriter;
import com.gregguns.icbm.worldgen.Placement;
import com.gregguns.icbm.worldgen.SiloCeiling;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.HeightProbe;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.ResolvedBlock;

/**
 * Writes the part of a silo complex that lies in one populate window (D128). The plan and its raster are pure
 * functions of the placement plan, so any window can run first or last; each cell is written by the one window that
 * contains its column, through the clipped sink. The level lies {@link #DEPTH} blocks under the recorded ground; the
 * silo shafts and the escape ladder rise from it to the ground height read in the shaft's own window.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md
public final class ComplexPlacer implements ComplexWriter {

    /** Level floor is this far under the recorded ground height. */
    public static final int DEPTH = 24;
    private static final int BUILD_HEIGHT = 255;

    /** Per-dimension and tuning inputs, so tests need no config. */
    public interface Settings {

        boolean dimensionEnabled(String dimensionName);

        int chancePercent();

        Set<Type> overworldBiomes();

        /** @return whether the world type (for example "default" or "flat") may host a complex (GGS-11-12) */
        default boolean worldTypeAllowed(String worldType) {
            return true;
        }

        /** @return the nine weights, EV first, of the Ross128 ceiling roll */
        default int[] ross128Weights() {
            return SiloCeiling.DEFAULT_ROSS_WEIGHTS;
        }
    }

    private final BlockResolver blocks;
    private final MachineHook machines;
    private final MarkFiller filler;
    private final Settings settings;
    private java.util.function.Consumer<String> skipListener = r -> {};
    /** The world of the populate call in progress (set by the gate check that precedes every call). */
    private World world;
    private final Map<String, ComplexRaster> cache = new LinkedHashMap<String, ComplexRaster>();

    public ComplexPlacer(BlockResolver blocks, MachineHook machines, MarkFiller filler, Settings settings) {
        this.blocks = blocks;
        this.machines = machines;
        this.filler = filler;
        this.settings = settings;
    }

    private Styles.Factory styles = Styles.Factory.NONE;
    private SiloMouthKeeper mouths;
    private ComplexKeeper keeper;

    /**
     * @param keeper where each window records what it wrote, so a later generator's changes are set back (D348):
     *               {@link ComplexKeeper#game()} in the game, an in-memory one in a scenario, none (null) in unit tests
     * @return this
     */
    public ComplexPlacer withKeeper(ComplexKeeper keeper) {
        this.keeper = keeper;
        return this;
    }

    /**
     * @param keeper where each silo's mouth is recorded for its finish (D319): {@link SiloMouthKeeper#game()} in the
     *               game, an in-memory one in a scenario, none (null) in unit tests
     * @return this
     */
    public ComplexPlacer withMouths(SiloMouthKeeper keeper) {
        this.mouths = keeper;
        return this;
    }

    /**
     * @param factory where each section's look comes from: {@link PaletteStyles#FACTORY} in the game, none in unit
     *                tests (every role then falls to the block resolver)
     * @return this
     */
    public ComplexPlacer withStyles(Styles.Factory factory) {
        this.styles = factory;
        stylesCache.clear();
        return this;
    }

    private final Map<String, Styles> stylesCache = new LinkedHashMap<String, Styles>();

    /** The styles of a plan's sections, cached like the raster; they depend on the dimension and the level too. */
    public Styles stylesOf(StructurePlan plan) {
        ComplexRaster raster = rasterOf(plan);
        String key = cacheKey(plan) + ":" + (forcedLevel == null ? "" : forcedLevel.name());
        Styles st = stylesCache.remove(key);
        if (st == null) {
            String dim = world == null ? "Overworld" : DimensionKey.of(world);
            st = styles.of(raster, dim, levelOf(plan));
            if (stylesCache.size() >= CACHED) stylesCache.remove(
                stylesCache.keySet()
                    .iterator()
                    .next());
        }
        stylesCache.put(key, st);
        return st;
    }

    /** @param listener told the reason of each skipped plan (scenarios); the log line is written regardless */
    public ComplexPlacer withSkipListener(java.util.function.Consumer<String> listener) {
        this.skipListener = listener;
        return this;
    }

    @Override
    public boolean enabledIn(World world) {
        this.world = world;
        return settings.dimensionEnabled(DimensionKey.of(world)) && settings.worldTypeAllowed(
            world.getWorldInfo()
                .getTerrainType()
                .getWorldTypeName());
    }

    /**
     * Drops what the placer holds per world: the cached rasters and contents and the world itself. Called when the
     * server stops, so a second save in the same JVM never sees the first one's layout or world.
     */
    public void releaseWorld() {
        world = null;
        cache.clear();
        contentsCache.clear();
        fixturesCache.clear();
        stylesCache.clear();
        summaries.clear();
        decisions.clear();
    }

    @Override
    public int chancePercent() {
        return settings.chancePercent();
    }

    @Override
    public boolean biomeOk(int dimension, Set<Type> types) {
        Set<Type> white = settings.overworldBiomes();
        if (dimension != 0 || white.isEmpty()) return true;
        for (Type t : types) if (white.contains(t)) return true;
        return false;
    }

    @Override
    public int minGroundY() {
        return DEPTH + 6;
    }

    @Override
    public void skipped(StructurePlan plan, String reason) {
        skipped(plan, reason, "");
    }

    @Override
    public void skipped(StructurePlan plan, String reason, String detail) {
        skipListener.accept(reason);
        GregGuns.LOG.info(
            "Silo complex skipped at dimension {} cell {},{} ({}): {}{}",
            plan.dimension(),
            plan.cellX(),
            plan.cellZ(),
            plan.anchorX() + "," + plan.anchorZ(),
            reason,
            detail == null || detail.isEmpty() ? "" : ", " + detail);
    }

    @Override
    public void sunk(StructurePlan plan, int sink, String detail) {
        GregGuns.LOG.info(
            "Silo complex at dimension {} cell {},{} ({}) sinks {} blocks deeper: {}",
            plan.dimension(),
            plan.cellX(),
            plan.cellZ(),
            plan.anchorX() + "," + plan.anchorZ(),
            sink,
            detail);
    }

    /** Lowest level floor: clear of bedrock. */
    public static final int MIN_FLOOR = 6;
    /** The least and most a complex sinks under a village or temple (D302, owner: "10 or 20 blocks"). */
    public static final int SINK_MIN = 10, SINK_MAX = 20;
    private static final long SINK_SALT = 0x5149_4E4BL;

    /**
     * How deep a complex under a village or temple goes (D302): its own roll from {@link #SINK_MIN} to
     * {@link #SINK_MAX}, cut to what keeps the level floor at or above {@link #MIN_FLOOR}; -1 when not even
     * {@link #SINK_MIN} fits, and the plan is skipped.
     */
    @Override
    public int sinkFor(StructurePlan plan, int groundY) {
        return sinkFor(plan.chestSeedBase(), floorY(world, groundY));
    }

    /**
     * @param seedBase the plan's seed base
     * @param floorY   the level floor before sinking
     * @return the sink, or -1 when {@link #SINK_MIN} would put the floor under {@link #MIN_FLOOR}
     */
    public static int sinkFor(long seedBase, int floorY) {
        int room = floorY - MIN_FLOOR;
        if (room < SINK_MIN) return -1;
        int roll = SINK_MIN + (int) Math.floorMod(StructureHash.mix(seedBase ^ SINK_SALT), SINK_MAX - SINK_MIN + 1L);
        return Math.min(roll, room);
    }

    /** The activity level, rolled once with the weights in force when the plan is decided (D304). */
    @Override
    public int decisionVariant(StructurePlan plan) {
        return rollLevel(plan.chestSeedBase()).ordinal();
    }

    /**
     * The level floor: {@link #DEPTH} under the recorded ground, and no higher than {@link #DEPTH} under the
     * dimension's average ground level, so a plan recorded on a hilltop still lies under the lower land around it.
     * A pure function of the recorded height and the provider, so every window agrees.
     *
     * @param world the world (null: the recorded ground alone)
     */
    public static int floorY(World world, int groundY) {
        int avg = world == null ? Integer.MAX_VALUE : world.provider.getAverageGroundLevel();
        return Math.max(MIN_FLOOR, Math.min(groundY, avg) - DEPTH);
    }

    /**
     * The level floor of a decided complex: the rule above, then {@code sink} blocks deeper when a village or
     * temple stands over the box (D302). The sink was cut at decision time to keep the floor clear of bedrock.
     *
     * @param sink {@link com.gregguns.worldgen.Decision#sink()}
     */
    public static int floorY(World world, int groundY, int sink) {
        return Math.max(MIN_FLOOR, floorY(world, groundY) - Math.max(0, sink));
    }

    /** The seed base folds in the world seed, the dimension and the cell, so no two worlds share a key. */
    private static String cacheKey(StructurePlan plan) {
        return plan.dimension() + ":" + plan.chestSeedBase();
    }

    /**
     * Complexes whose raster, contents and styles stay cached. A complex spans up to 225 windows, and several can
     * populate interleaved (players in several dimensions), so a short cache would rebuild them window after window. A
     * raster is about 0.8 MB.
     */
    static final int CACHED = 6;

    /** The raster of a plan, cached for the windows one complex spans. */
    public ComplexRaster rasterOf(StructurePlan plan) {
        String key = cacheKey(plan);
        ComplexRaster r = cache.remove(key);
        if (r == null) {
            r = new ComplexRaster(ComplexLayout.fromSeed(plan.chestSeedBase(), plan.cellX(), plan.cellZ()));
            if (cache.size() >= CACHED) {
                cache.remove(
                    cache.keySet()
                        .iterator()
                        .next());
            }
        }
        cache.put(key, r);
        return r;
    }

    private final Map<String, java.util.List<Placement>> contentsCache = new LinkedHashMap<String, java.util.List<Placement>>();
    private boolean contents = true;

    /**
     * @param on whether the activity, guard and loot pass runs after the shell (scenarios of the shell alone turn it
     *           off)
     */
    public ComplexPlacer withContents(boolean on) {
        this.contents = on;
        return this;
    }

    /** The ceiling of a complex: the dimension's table row, rolled per complex on Ross128b and Ross128ba. */
    public int ceilingOf(StructurePlan plan) {
        if (forcedCeiling > 0) {
            return forcedCeiling;
        }
        String name = world == null ? "Overworld" : DimensionKey.of(world);
        return ceilingFor(name, plan.chestSeedBase(), settings.ross128Weights());
    }

    /**
     * The ceiling the table gives a dimension: its row, or EV for a dimension the table does not list (only a
     * scenario asks for one: a listed dimension outside the table generates nothing, {@link ConfigSettings}). Ross128b
     * and ba roll one per complex from their own nine weights.
     *
     * @param dimensionKey a {@link DimensionKey}
     * @param seedBase     the plan's seed base
     * @param rossWeights  nine weights, EV first
     * @return a GT voltage index, EV to UMV
     */
    public static int ceilingFor(String dimensionKey, long seedBase, int[] rossWeights) {
        Integer tier = PlanetTable.tierOf(dimensionKey);
        if (tier != null && tier == PlanetTable.ROLLED) {
            return SiloCeiling.roll(seedBase, rossWeights);
        }
        return Math.max(SiloCeiling.EV, PlanetTable.ceilingVoltage(dimensionKey));
    }

    private ActivityLevel forcedLevel;
    private int forcedCeiling;

    /** @param ceiling the ceiling every complex gets instead of its dimension's (scenarios), or 0 for the table */
    public ComplexPlacer forceCeiling(int ceiling) {
        this.forcedCeiling = ceiling;
        contentsCache.clear();
        fixturesCache.clear();
        return this;
    }

    /** @param level the level every complex gets instead of its rolled one (scenarios), or null to roll */
    public ComplexPlacer forceLevel(ActivityLevel level) {
        this.forcedLevel = level;
        contentsCache.clear();
        fixturesCache.clear();
        stylesCache.clear();
        return this;
    }

    /** Decisions the writes in progress carry, by plan: the recorded level wins over a fresh roll (D304). */
    private final Map<String, Decision> decisions = new LinkedHashMap<String, Decision>();

    /**
     * The activity level of a complex: the forced one (scenarios), else the one recorded with its decision (by the
     * write in progress, or in the world's ledger), else rolled from its seed with the weights in force. A level is
     * recorded the first time the plan is decided, so changing the weights later never splits a complex (D304).
     */
    public ActivityLevel levelOf(StructurePlan plan) {
        if (forcedLevel != null) {
            return forcedLevel;
        }
        Decision d = decisions.get(cacheKey(plan));
        if (d == null && world != null) {
            d = StructureState.forWorld(world)
                .get(PlanKey.of(plan));
        }
        return levelFor(d, plan.chestSeedBase());
    }

    /**
     * @param d        the plan's decision, or null when undecided
     * @param seedBase the plan's seed base
     * @return the recorded level when {@code d} holds one, else the roll with the configured weights
     */
    public static ActivityLevel levelFor(Decision d, long seedBase) {
        int v = d == null ? Decision.NO_VARIANT : d.variant();
        if (v >= 0 && v < ActivityLevel.values().length) return ActivityLevel.values()[v];
        return rollLevel(seedBase);
    }

    private static ActivityLevel rollLevel(long seedBase) {
        return ActivityLevel
            .roll(seedBase, ActivityLevel.weightsFrom(com.gregguns.core.config.IcbmConfig.siloComplexActivityWeights));
    }

    private java.util.List<Placement> contentsOf(StructurePlan plan, ComplexPlan cp) {
        String key = cacheKey(plan);
        java.util.List<Placement> l = contentsCache.remove(key);
        if (l == null) {
            // The comms data stick points at the nearest other complex the seed rolls (block x, z of its hall).
            int[] other = world == null ? null
                : NeighborComplex
                    .nearest(world.getSeed(), plan.dimension(), plan, settings.chancePercent(), NeighborComplex.RADIUS);
            l = ComplexContents.build(cp, levelOf(plan), ceilingOf(plan), other);
            if (contentsCache.size() >= CACHED) contentsCache.remove(
                contentsCache.keySet()
                    .iterator()
                    .next());
        }
        contentsCache.put(key, l);
        return l;
    }

    private final Map<String, ComplexFixtures> fixturesCache = new LinkedHashMap<String, ComplexFixtures>();
    private Boolean forcedTierCasings;

    /**
     * @param on whether the blast-door frames are the tier's machine casings instead of the dimension's own rule
     *           (scenarios), or null for the rule: casings on the rolled Ross128 dimensions only (D337)
     * @return this
     */
    public ComplexPlacer forceTierCasings(Boolean on) {
        this.forcedTierCasings = on;
        fixturesCache.clear();
        return this;
    }

    /**
     * @return whether a complex shows its tier in its blast-door frames: on Ross128b and Ross128ba, whose ceiling is
     *         rolled per complex (D38, D337)
     */
    public boolean tierCasingsOf(StructurePlan plan) {
        if (forcedTierCasings != null) return forcedTierCasings;
        Integer tier = world == null ? null : com.gregguns.icbm.complex.PlanetTable.tierOf(DimensionKey.of(world));
        return tier != null && tier == com.gregguns.icbm.complex.PlanetTable.ROLLED;
    }

    /** The blast doors and service runs of a complex (D335, D336), cached like its contents. */
    public ComplexFixtures fixturesOf(StructurePlan plan, ComplexRaster raster) {
        String key = cacheKey(plan);
        ComplexFixtures f = fixturesCache.remove(key);
        if (f == null) {
            long t0 = System.nanoTime();
            f = ComplexFixtures.build(raster, levelOf(plan), tierCasingsOf(plan));
            FixtureWriter.addSpent(System.nanoTime() - t0);
            if (fixturesCache.size() >= CACHED) fixturesCache.remove(
                fixturesCache.keySet()
                    .iterator()
                    .next());
        }
        fixturesCache.put(key, f);
        return f;
    }

    @Override
    public int write(StructurePlan plan, Decision decision, PopulateWindow window, WindowedSink sink,
        HeightProbe probe) {
        // The GT blocks the window places (silo concrete, hatches) start no machine-update search (D205).
        boolean before = machines.suspendUpdates();
        String key = cacheKey(plan);
        Decision was = decisions.put(key, decision);
        if (was != null && !was.equals(decision)) {
            // Another decision for the same plan (a scenario's fresh ledger): the cached contents used the old one.
            contentsCache.remove(key);
            fixturesCache.remove(key);
            stylesCache.keySet()
                .removeIf(k -> k.startsWith(key + ":"));
        }
        if (decisions.size() > CACHED * 4) decisions.remove(
            decisions.keySet()
                .iterator()
                .next());
        try {
            return writeWindow(plan, decision.groundY(), decision.sink(), window, sink, probe);
        } finally {
            machines.resumeUpdates(before);
        }
    }

    /** Plans no layout grew from (never seen in 40,000 seeds), so each is logged once, not once per window. */
    private final Set<String> unbuildable = new java.util.LinkedHashSet<String>();

    private int writeWindow(StructurePlan plan, int groundY, int sunk, PopulateWindow window, WindowedSink sink,
        HeightProbe probe) {
        ComplexRaster raster;
        try {
            raster = unbuildable.contains(cacheKey(plan)) ? null : rasterOf(plan);
        } catch (IllegalStateException e) {
            // Every regrowth failed: skip the complex rather than throw out of chunk populate.
            if (unbuildable.size() >= 16) unbuildable.remove(
                unbuildable.iterator()
                    .next());
            unbuildable.add(cacheKey(plan));
            skipped(plan, "layout");
            raster = null;
        }
        if (raster == null) return 0;
        ComplexPlan cp = raster.plan();
        int floorY = floorY(world, groundY, sunk);
        int ax = plan.anchorX(), az = plan.anchorZ();
        int x0 = Math.max(ax, window.minX()), x1 = Math.min(ax + ComplexLayout.SIZE - 1, window.maxX());
        int z0 = Math.max(az, window.minZ()), z1 = Math.min(az + ComplexLayout.SIZE - 1, window.maxZ());
        // The skin (D320) reaches one column past the box, behind a section wall on the box's edge, wherever that
        // column lies in a window that runs the complex.
        int sx0 = Math.max(ax - 1, window.minX()), sx1 = Math.min(ax + ComplexLayout.SIZE, window.maxX());
        int sz0 = Math.max(az - 1, window.minZ()), sz1 = Math.min(az + ComplexLayout.SIZE, window.maxZ());
        int n = 0;
        Block air = blocks.air();
        // The recorded ground comes from one window; this window's own columns may lie lower (a valley, a lake bed).
        // A column whose ground is within a few blocks of the level's top is cut off, so nothing breaks the surface.
        boolean[] mask = new boolean[PopulateWindow.SIZE * PopulateWindow.SIZE];
        boolean masked = false;
        for (int x = sx0; x <= sx1; x++) {
            for (int z = sz0; z <= sz1; z++) {
                if (cutOff(probe, floorY, x, z)) {
                    mask[ColumnMaskSink.index(window, x, z)] = true;
                    masked = true;
                }
            }
        }
        int[] seal = sealFloors(raster, probe, window, floorY, mask, x0, x1, z0, z1, ax, az);
        // The keep (D348): every pass below writes through the recorder, and what the window leaves is read back at
        // the end, so the generators that run after it (GregTech's ore veins last of all) cannot keep what they change.
        ComplexKeeper.Recorder recorder = keeper != null && world != null ? keeper.recorder(sink) : null;
        if (recorder != null) sink = recorder;
        WindowedSink under = masked ? new ColumnMaskSink(sink, mask) : sink;
        Styles st = stylesOf(plan);
        // The role audit (D267): tag every write with the pass that made it, then read the complex back once its
        // last window is written. Off in normal play.
        RoleAudit.TaggingSink tagUnder = null, tagSink = null;
        String auditKey = null;
        if (RoleAudit.enabled()) {
            auditKey = System.identityHashCode(this) + ":" + cacheKey(plan);
            RoleAudit.Tracker t = RoleAudit
                .tracker(auditKey, world, raster, st, auditWhere(plan, floorY), levelOf(plan).name(), ax, floorY, az);
            under = tagUnder = new RoleAudit.TaggingSink(under, t);
            sink = tagSink = new RoleAudit.TaggingSink(sink, t);
        }
        int maskedColumns = 0;
        for (boolean m : mask) if (m) maskedColumns++;
        // The silo number signs are written after the shell, into the walkway cell they hang in; the shell leaves that
        // air cell to them, so no cell is written twice.
        Set<Long> signCells = new java.util.HashSet<>();
        for (SiloSlot s : cp.silos()) {
            int[] at = ComplexSigns.siloSignAt(s);
            signCells.add(((long) at[0] << 40) ^ ((long) at[1] << 32) ^ (at[2] & 0xFFFFFFFFL));
        }
        // The escape ladder's column and the one behind it belong to the ladder pass from the top of its section up.
        int ex = cp.escapeX(), ez = cp.escapeZ(), em = cp.escapeMeta();
        int bx = ex + (em == 4 ? 1 : em == 5 ? -1 : 0), bz = ez + (em == 2 ? 1 : em == 3 ? -1 : 0);
        for (int x = sx0; x <= sx1; x++) {
            for (int z = sz0; z <= sz1; z++) {
                int clx = x - ax, clz = z - az;
                boolean escape = clx == ex && clz == ez || clx == bx && clz == bz;
                n += skin(raster, clx, clz, x, z, floorY, escape ? cp.escapeTop() : Integer.MAX_VALUE, under);
                if (x < x0 || x > x1 || z < z0 || z > z1) continue; // past the box: the skin only
                for (int y = 0; y < ComplexRaster.LEVELS; y++) {
                    int lx = x - ax, lz = z - az;
                    byte c = raster.at(lx, y, lz);
                    if (c == Cell.NONE) continue;
                    int wy = floorY + y;
                    // Open cells beside a cut-off column whose ground lies under them face its water or air: wall.
                    if (y >= 1 && Cell.open(c) && wy > seal[ColumnMaskSink.index(window, x, z)]) c = Cell.WALL;
                    if (c == Cell.AIR && signCells.contains(((long) lx << 40) ^ ((long) y << 32) ^ (lz & 0xFFFFFFFFL)))
                        continue;
                    if (Cell.siloMachine(c)) {
                        if (under.setBlock(x, wy, z, air, 0) && world != null) {
                            machines.place(world, c, x, wy, z, siloFacing(cp, lx, lz));
                        }
                        n++;
                        continue;
                    }
                    ResolvedBlock b = c == Cell.LADDER ? blocks.ladder(cp.escapeMeta(), x, wy, z) : null;
                    if (b == null && styled(c)) b = st.block(c, lx, y, lz);
                    if (b == null) b = blocks.resolve(c, x, wy, z);
                    if (b != null && under.setBlock(x, wy, z, b.block, b.meta)) n++;
                }
            }
        }
        if (tagSink != null) tagSink.as(RoleAudit.SHAFT);
        n += shafts(cp, plan, floorY, window, sink, probe);
        n += ladder(cp, plan, floorY, window, sink, probe);
        n += radars(cp, plan, groundY, window, sink, probe);
        if (tagUnder != null) tagUnder.as(RoleAudit.MARK);
        for (Mark m : cp.allMarks()) {
            int wx = ax + m.x(), wz = az + m.z();
            if (window.contains(wx, wz)) filler.fill(world, plan, m, wx, floorY + m.y(), wz, under);
        }
        if (tagUnder != null) tagUnder.as(RoleAudit.CONTENTS);
        if (tagSink != null) tagSink.as(RoleAudit.SIGNS);
        // The signs go through the cut-off mask too (D207): no sign hangs in rock, water or valley air where no room
        // was built. The silo numbers belong to the silo, so the shell pass writes them (shell-only runs read them
        // back too).
        WindowedSink signs = new QuietSink(masked ? new ColumnMaskSink(sink, mask) : sink);
        ComplexSigns.writeSilos(cp, ax, floorY, az, signs);
        if (contents) {
            final boolean[] cut = mask;
            ComplexApplier applier = new ComplexApplier(
                new QuietSink(under),
                world,
                ax,
                floorY,
                az,
                plan.chestSeedBase(),
                plan.cellX(),
                plan.cellZ(),
                ceilingOf(plan)).withLamps(st::litLamp)
                    .withColumns(masked ? (x, z) -> !cut[ColumnMaskSink.index(window, x, z)] : null);
            java.util.List<Placement> placements = contentsOf(plan, cp);
            for (Placement p : placements) {
                if (tagUnder != null)
                    tagUnder.as(p.kind() == Placement.Kind.LIGHT ? RoleAudit.LIGHT : RoleAudit.CONTENTS);
                applier.apply(p);
            }
            // A chest turns itself and the chest beside it as it is placed: their facings go back on once all are in.
            applier.settleChests(placements);
            if (tagUnder != null) tagUnder.as(RoleAudit.CONTENTS);
            // The blast doors and service runs (D335 to D337), clipped to the window.
            new FixtureWriter(new QuietSink(under), ax, floorY, az, ceilingOf(plan), st::doorPlate)
                .write(fixturesOf(plan, raster));
            ComplexSigns.write(cp, raster, ax, floorY, az, ceilingOf(plan), levelOf(plan), signs);
        }
        if (recorder != null) keeper.capture(world, recorder);
        if (summarize(plan, floorY, window, n, maskedColumns, sink) && auditKey != null) RoleAudit.complete(auditKey);
        return n;
    }

    /**
     * A column whose own ground (the sea floor under water) lies less than 14 blocks above the level floor is cut off:
     * nothing of the level, its marks or its contents is written there (D207). Scenarios use the same rule to know
     * which marks to expect.
     *
     * @return whether the column is cut off
     */
    public static boolean cutOff(HeightProbe probe, int floorY, int x, int z) {
        return cutOffAt(probe.seaFloorY(x, z), floorY);
    }

    private static boolean cutOffAt(int g, int floorY) {
        return g != HeightProbe.NONE && g < floorY + ComplexLayout.SILO_HEIGHT + 2;
    }

    /**
     * Where a written column borders a cut-off one. The cut-off column keeps its natural terrain, and where that
     * ground lies lower than the level's open cells beside it, a hall would open sideways onto the lake or valley
     * over it. Per window column: the lowest such neighbor's ground, so the writer walls up every open cell above it
     * ({@link Integer#MAX_VALUE} when no neighbor is cut off). A neighbor outside the window is read through
     * {@link HeightProbe#seaFloorYBeside}: one column past the window still lies in a loaded chunk.
     */
    private static int[] sealFloors(ComplexRaster raster, HeightProbe probe, PopulateWindow window, int floorY,
        boolean[] mask, int x0, int x1, int z0, int z1, int ax, int az) {
        int[] seal = new int[PopulateWindow.SIZE * PopulateWindow.SIZE];
        java.util.Arrays.fill(seal, Integer.MAX_VALUE);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int i = ColumnMaskSink.index(window, x, z);
                if (mask[i]) continue;
                // A silo's inside is its own shaft, open to the bore above: never walled up (S3-2).
                if (siloColumn(raster, x - ax, z - az)) continue;
                for (int d = 0; d < 4; d++) {
                    int nx = x + (d == 1 ? 1 : d == 3 ? -1 : 0), nz = z + (d == 2 ? 1 : d == 0 ? -1 : 0);
                    // Outside the complex nothing is written next to an open cell: every section ends in its wall.
                    if (nx < ax || nz < az || nx >= ax + ComplexLayout.SIZE || nz >= az + ComplexLayout.SIZE) continue;
                    // A bore column is the complex's own air from the pad up once its window has run, so reading it
                    // from the next window would depend on which ran first (D319): it is never a cut-off neighbor.
                    if (boreColumn(raster, nx - ax, nz - az)) continue;
                    int g;
                    if (window.contains(nx, nz)) {
                        if (!mask[ColumnMaskSink.index(window, nx, nz)]) continue;
                        g = probe.seaFloorY(nx, nz);
                    } else {
                        g = probe.seaFloorYBeside(nx, nz);
                        if (!cutOffAt(g, floorY)) continue;
                    }
                    seal[i] = Math.min(seal[i], g);
                }
            }
        }
        return seal;
    }

    /** @return whether a complex-local column belongs to a silo's 5 x 5 footprint */
    static boolean siloColumn(ComplexRaster raster, int lx, int lz) {
        return raster.ownerAt(lx, lz) < 0 && raster.at(lx, 0, lz) != Cell.NONE;
    }

    /** @return whether a complex-local column is one of a silo's nine bore columns (open over the pad) */
    static boolean boreColumn(ComplexRaster raster, int lx, int lz) {
        return siloColumn(raster, lx, lz) && raster.at(lx, 1, lz) == Cell.AIR;
    }

    /**
     * The complex's own skin around its level (D320): a foundation under every column the level uses, and solid fill
     * in every rock cell beside a section's or silo's cell and over a section's ceiling, so a cave or loose terrain
     * never opens onto a floor, a wall or a ceiling, nor shows through a floor or wall that wear breaks open. Pure
     * raster arithmetic, so each window writes its own columns the same whatever ran before. Written through the
     * shell's (cut-off) sink before the marks pass, so marks that reach below the floor (a pool) overwrite it.
     *
     * @param ownedFrom the layer from which another pass writes the column (the escape ladder and its backing)
     * @return blocks written
     */
    private int skin(ComplexRaster r, int lx, int lz, int x, int z, int floorY, int ownedFrom, WindowedSink sink) {
        int top = -1;
        for (int y = ComplexRaster.LEVELS - 1; y >= 0; y--) {
            if (r.at(lx, y, lz) != Cell.NONE) {
                top = y;
                break;
            }
        }
        int n = 0;
        if (top >= 0 && floorY - 1 >= 1) n += solid(x, floorY - 1, z, sink);
        for (int y = 0; y < Math.min(ComplexRaster.LEVELS, ownedFrom); y++) {
            if (r.at(lx, y, lz) != Cell.NONE) continue;
            boolean beside = r.at(lx + 1, y, lz) != Cell.NONE || r.at(lx - 1, y, lz) != Cell.NONE
                || r.at(lx, y, lz + 1) != Cell.NONE
                || r.at(lx, y, lz - 1) != Cell.NONE;
            boolean over = y > 0 && r.at(lx, y - 1, lz) != Cell.NONE;
            if (beside || over) n += solid(x, floorY + y, z, sink);
        }
        // Over the tallest sections; a silo's column above the level is the shaft pass's ring and bore.
        if (top == ComplexRaster.LEVELS - 1 && ComplexRaster.LEVELS < ownedFrom && !siloColumn(r, lx, lz)) {
            n += solid(x, floorY + ComplexRaster.LEVELS, z, sink);
        }
        return n;
    }

    private int solid(int x, int y, int z, WindowedSink sink) {
        ResolvedBlock b = blocks.named("foundation", x, y, z);
        return b != null && sink.setBlock(x, y, z, b.block, b.meta) ? 1 : 0;
    }

    /** How far above the ground a surface opening is cleared of leaves, logs and snow. */
    static final int CLEAR_ABOVE = 32;

    /**
     * Clears the natural cover over a column's ground, so a shaft mouth or the escape trapdoor is never left under
     * leaves or a trunk: foliage (plants, vines, snow layers, cactus), logs and leaves ({@code isWood} or
     * {@code isLeaves}) and ice, up to {@link #CLEAR_ABOVE} over the ground. Above that only ice goes on, while it
     * lasts, so an ice spike over the opening is cut whole rather than left floating. Anything else stays: a chest,
     * a fence, another mod's structure, and every block that carries a tile entity. Reads only this window's columns
     * (the populate world; nothing without one, as in unit tests).
     */
    private int clearAbove(WindowedSink sink, int x, int z, int groundY) {
        if (world == null || !sink.window()
            .contains(x, z)) {
            return 0;
        }
        int n = 0;
        int cap = Math.min(BUILD_HEIGHT, groundY + CLEAR_ABOVE);
        for (int y = groundY + 1; y <= BUILD_HEIGHT; y++) {
            Block b = world.getBlock(x, y, z);
            Material m = b.getMaterial();
            boolean ice = m == Material.ice || m == Material.packedIce;
            if (y > cap && !ice) break;
            if (m == Material.air || !clearable(b, m, ice, x, y, z)) continue;
            if (sink.setBlock(x, y, z, blocks.air(), 0)) n++;
        }
        return n;
    }

    /** @return whether a block over a surface opening is natural cover the clear may remove */
    private boolean clearable(Block b, Material m, boolean ice, int x, int y, int z) {
        if (b.hasTileEntity(world.getBlockMetadata(x, y, z))) return false;
        if (ice) return true;
        if (m == Material.leaves || m == Material.plants
            || m == Material.vine
            || m == Material.snow
            || m == Material.cactus) {
            return true;
        }
        return b.isWood(world, x, y, z) || b.isLeaves(world, x, y, z);
    }

    /** @return whether the role is a building role the section's style decides */
    private static boolean styled(byte c) {
        switch (c) {
            case Cell.WALL:
            case Cell.FLOOR:
            case Cell.CEILING:
            case Cell.LAMP:
            case Cell.DOOR_FRAME:
            case Cell.FURN_TABLE:
            case Cell.FURN_CONSOLE:
            case Cell.FURN_SHELF:
                return true;
            default:
                return false;
        }
    }

    /** @return the facing of the silo whose footprint holds the column (its machines face the gallery) */
    static int siloFacing(ComplexPlan cp, int lx, int lz) {
        for (SiloSlot s : cp.silos()) {
            if (s.bounds()
                .contains(lx, 0, lz)) return s.facing();
        }
        return 0;
    }

    /** Per complex: windows written, blocks written, columns cut off, writes the sink refused. */
    private final Map<String, int[]> summaries = new LinkedHashMap<String, int[]>();

    /**
     * Counts what each window wrote and, once every window over the complex has run, logs one line at debug level, so
     * a complex that writes nothing in a pack shows up in the log with its numbers (D236).
     *
     * @return whether this was the complex's last window
     */
    private boolean summarize(StructurePlan plan, int floorY, PopulateWindow window, int blocksWritten,
        int maskedColumns, WindowedSink sink) {
        String key = cacheKey(plan);
        int[] s = summaries.get(key);
        if (s == null) {
            if (summaries.size() >= 16) summaries.remove(
                summaries.keySet()
                    .iterator()
                    .next());
            s = new int[4];
            summaries.put(key, s);
        }
        s[0]++;
        s[1] += blocksWritten;
        s[2] += maskedColumns;
        s[3] = sink.rejectedWrites();
        if (s[0] == windowsOver(plan.anchorX()) * windowsOver(plan.anchorZ())) {
            GregGuns.LOG.debug(
                "Silo complex at dimension {} cell {},{} anchor {},{} floor y {}: {} windows, {} blocks, {} columns cut"
                    + " off, last window refused {} writes",
                plan.dimension(),
                plan.cellX(),
                plan.cellZ(),
                plan.anchorX(),
                plan.anchorZ(),
                floorY,
                s[0],
                s[1],
                s[2],
                s[3]);
            summaries.remove(key);
            return true;
        }
        return false;
    }

    /** @return where a complex lies, for the role audit's log line */
    private String auditWhere(StructurePlan plan, int floorY) {
        String dim = world == null ? "Overworld" : DimensionKey.of(world);
        return "dimension " + dim
            + " cell "
            + plan.cellX()
            + ","
            + plan.cellZ()
            + " anchor "
            + plan.anchorX()
            + ","
            + plan.anchorZ()
            + " floor y "
            + floorY;
    }

    /** @return how many populate windows along one axis overlap a complex anchored at {@code a} */
    static int windowsOver(int a) {
        int first = Math.floorDiv(a - PopulateWindow.OFFSET, 16);
        int last = Math.floorDiv(a + ComplexLayout.SIZE - 1 - PopulateWindow.OFFSET, 16);
        return last - first + 1;
    }

    /** The 5 x 5 shaft ring and 3 x 3 bore from the top of the silo to the surface, and the mouth variant. */
    private int shafts(ComplexPlan cp, StructurePlan plan, int floorY, PopulateWindow window, WindowedSink sink,
        HeightProbe probe) {
        int n = 0;
        Block air = blocks.air();
        int index = 0;
        for (SiloSlot s : cp.silos()) {
            String id = plan.chestSeedBase() + "#" + index++;
            int sx = plan.anchorX() + s.bounds().minX, sz = plan.anchorZ() + s.bounds().minZ;
            for (int x = Math.max(sx, window.minX()); x <= Math.min(sx + 4, window.maxX()); x++) {
                for (int z = Math.max(sz, window.minZ()); z <= Math.min(sz + 4, window.maxZ()); z++) {
                    int g = probe.landY(x, z);
                    // Under water the shaft rises through it to one block over its surface (D354).
                    if (g == HeightProbe.NONE) g = riserTopAt(probe, floorY, x, z);
                    int top = g == HeightProbe.NONE ? HeightProbe.NONE : Math.min(g, BUILD_HEIGHT - 8);
                    // The finish (D319) puts the mouth back to this once everything around it has populated.
                    if (mouths != null) mouths.record(world, id, sx, sz, floorY, s.variant(), x, z, top);
                    if (g == HeightProbe.NONE) continue;
                    boolean bore = x > sx && x < sx + 4 && z > sz && z < sz + 4;
                    // The mouth writes the top cell itself (cap, or air up to build height), so the bore stops under
                    // it.
                    boolean mouthOwnsTop = bore && (s.variant() == SurfaceVariant.CLOSED
                        || s.variant() == SurfaceVariant.HALF_OPEN && x <= sx + 2);
                    for (int y = floorY + ComplexLayout.SILO_HEIGHT; y <= (mouthOwnsTop ? top - 1 : top); y++) {
                        ResolvedBlock b = bore ? null : blocks.named("ring", x, y, z);
                        if (bore ? sink.setBlock(x, y, z, air, 0)
                            : b != null && sink.setBlock(x, y, z, b.block, b.meta)) n++;
                    }
                    n += mouth(s.variant(), bore, x, z, sx, sz, top, sink);
                    // A closed mouth's open cells are air to build height already; its capped cells lose their cover
                    // like every other cell (snow and trunks on the old ground would stand on the cap, S3-2).
                    boolean hole = bore && s.variant() == SurfaceVariant.CLOSED
                        && !SiloMouth.capped(s.variant(), sx, sz, x, z);
                    if (!hole) n += clearAbove(sink, x, z, Math.max(top, g));
                }
            }
        }
        return n;
    }

    /**
     * Open: bare bore. Half open: the west half of the bore capped. Closed: capped, with air columns to build height.
     */
    private int mouth(SurfaceVariant v, boolean bore, int x, int z, int sx, int sz, int top, WindowedSink sink) {
        if (!bore || v == SurfaceVariant.OPEN) return 0;
        ResolvedBlock cap = blocks.named("cap", x, top, z);
        if (v == SurfaceVariant.HALF_OPEN) {
            return x <= sx + 2 && cap != null && sink.setBlock(x, top, z, cap.block, cap.meta) ? 1 : 0;
        }
        // Closed: nine cells capped, two of the eight non-center cells stay air all the way up (D17).
        int n = 0;
        if (!SiloMouth.capped(v, sx, sz, x, z)) {
            for (int y = top; y <= BUILD_HEIGHT; y++) if (sink.setBlock(x, y, z, blocks.air(), 0)) n++;
        } else if (cap != null && sink.setBlock(x, top, z, cap.block, cap.meta)) {
            n++;
        }
        return n;
    }

    /**
     * The 1 x 1 escape ladder from the level up to a trapdoor at the surface. On land the trapdoor replaces the
     * column's top block and nothing is built around it (D128, D218). Under water (D354) the shaft rises through the
     * water inside a 3 x 3 riser of the shaft-ring block to one block over the water surface, where the trapdoor lies
     * flush with the riser's top, hinged on the wall the ladder clings to.
     * <p>
     * Every column of the riser decides from its own column alone (is it under water, and how high is its surface),
     * so no window reads another window's writes and the result is the same in any window order. Water lies level,
     * so the escape column and its neighbors agree on the top. A neighbor on dry land keeps its terrain.
     */
    private int ladder(ComplexPlan cp, StructurePlan plan, int floorY, PopulateWindow window, WindowedSink sink,
        HeightProbe probe) {
        int x = plan.anchorX() + cp.escapeX(), z = plan.anchorZ() + cp.escapeZ();
        int base = floorY + cp.escapeTop();
        int n = 0;
        // A ladder clings to the block behind it, and a natural cave the shaft crosses has none: the cell behind the
        // whole climb is written solid, from the level up to just under that column's own surface block. The surface
        // block itself stays as the terrain made it, so nothing but the trapdoor's own cell differs from the grass
        // beside it. Its window writes it, whether or not the window holds the ladder.
        int meta = cp.escapeMeta();
        int bx = x + (meta == 4 ? 1 : meta == 5 ? -1 : 0), bz = z + (meta == 2 ? 1 : meta == 3 ? -1 : 0);
        if (window.contains(bx, bz)) {
            int bg = probe.landY(bx, bz);
            if (bg != HeightProbe.NONE) {
                for (int y = base; y < bg; y++) {
                    ResolvedBlock b = blocks.named("backing", bx, y, bz);
                    if (b != null && sink.setBlock(bx, y, bz, b.block, b.meta)) n++;
                }
            }
        }
        // The riser's walls (D354): each neighbor under water is the shaft-ring block from its bed up to one block over
        // its water. Behind the ladder the stone backing runs up to the bed first.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int wx = x + dx, wz = z + dz;
                if (dx == 0 && dz == 0 || !window.contains(wx, wz)) continue;
                int top = riserTopAt(probe, floorY, wx, wz);
                if (top == HeightProbe.NONE) continue;
                int bed = probe.seaFloorY(wx, wz);
                boolean behind = wx == bx && wz == bz;
                // Under the bed the other neighbors are the terrain's (or the level's skin, D320); never lower than
                // the shaft's own start.
                int from = Math.max(bed, behind ? base : floorY + ComplexRaster.LEVELS + 1);
                if (behind) {
                    for (int y = base; y < from; y++) {
                        ResolvedBlock b = blocks.named("backing", wx, y, wz);
                        if (b != null && sink.setBlock(wx, y, wz, b.block, b.meta)) n++;
                    }
                }
                for (int y = from; y <= top; y++) {
                    ResolvedBlock b = blocks.named("ring", wx, y, wz);
                    if (b != null && sink.setBlock(wx, y, wz, b.block, b.meta)) n++;
                }
            }
        }
        if (!window.contains(x, z)) return n;
        int g = probe.landY(x, z);
        if (g == HeightProbe.NONE) {
            // Under water: the ladder climbs through the bed and the water to the riser's top (D354).
            g = riserTopAt(probe, floorY, x, z);
            if (g == HeightProbe.NONE) return n;
        }
        for (int y = base; y < g; y++) {
            ResolvedBlock b = blocks.ladder(meta, x, y, z);
            if (b != null && sink.setBlock(x, y, z, b.block, b.meta)) n++;
        }
        // landY is the top solid block, so the trapdoor replaces it; a top-half trapdoor then lies flush with the
        // surface (its top face at g + 1, level with the terrain around it). On a riser it lies flush with the riser.
        // It hinges on the backed column (D354): that column's top block rests on the backing or is the riser, so sand
        // falling away beside the hatch never takes its support.
        ResolvedBlock t = blocks.named("trapdoor", x, g, z);
        if (t != null && sink.setBlock(x, g, z, t.block, t.meta | trapdoorHinge(meta))) n++;
        n += clearAbove(sink, x, z, g);
        return n;
    }

    /**
     * The top of a riser over a column whose surface is water (D354): one block over the water's top, when the bed
     * under it is not cut off (D207). Ice over the water is passed through like any cover, so on a frozen river the
     * top is the ice's own layer.
     *
     * @return the riser's top y, or {@link HeightProbe#NONE} when the column is dry, has no bed, or is cut off
     */
    public static int riserTop(HeightProbe probe, int floorY, int x, int z) {
        int w = probe.waterSurfaceY(x, z), bed = probe.seaFloorY(x, z);
        if (w == HeightProbe.NONE || bed == HeightProbe.NONE || cutOffAt(bed, floorY)) return HeightProbe.NONE;
        return Math.min(w + 1, BUILD_HEIGHT - 8);
    }

    /**
     * {@link #riserTop} in the populate world: a frozen top layer (ice over the water, as on a cold river) counts as
     * the water's own, so a river that froze in places still gives every column the same top, one block over the ice.
     * Reads only a column of this window.
     */
    private int riserTopAt(HeightProbe probe, int floorY, int x, int z) {
        int top = riserTop(probe, floorY, x, z);
        if (top == HeightProbe.NONE || world == null
            || !probe.window()
                .contains(x, z)
            || top >= BUILD_HEIGHT - 8) return top;
        Material m = world.getBlock(x, top, z)
            .getMaterial();
        return m == Material.ice || m == Material.packedIce ? top + 1 : top;
    }

    /**
     * The direction bits of a trapdoor hinged on the wall a ladder clings to: a trapdoor drops when the block on its
     * hinge side goes, and the backed column is the one neighbor sure to stand (1.7.10 trapdoor metadata: 0 hinged
     * on +z, 1 on -z, 2 on +x, 3 on -x).
     *
     * @param ladderMeta the ladder's metadata, 2 to 5 (2 clings to +z, 3 to -z, 4 to +x, 5 to -x)
     * @return the trapdoor's direction bits, 0 to 3
     */
    public static int trapdoorHinge(int ladderMeta) {
        switch (ladderMeta) {
            case 3:
                return 1;
            case 4:
                return 2;
            case 5:
                return 3;
            default:
                return 0;
        }
    }

    /** Radar hut footprint: a 5 x 5 floor, walls three high, a roof, and the small radar (D125) standing on it. */
    static final int HUT = 5;
    static final int WALL_H = 3;
    static final int MAST_H = 7;

    /**
     * Radar stations (D128, 0 to 2 per complex): a small hut with a doorway on its south side and, on its roof, the
     * small radar (D125, {@link RadarStation}): a Radar Casing base, the pedestal, an HV radar controller and a
     * Maintenance Hatch, with the Energy Hatch left out. Everything is built off the recorded ground height, so a hut
     * that straddles
     * windows stays level; each column's own ground fills the foundation below the floor or clears the bump above
     * it. A column under water stands on a foundation from its bed up through the water (D354).
     */
    private int radars(ComplexPlan cp, StructurePlan plan, int groundY, PopulateWindow window, WindowedSink sink,
        HeightProbe probe) {
        int n = 0;
        for (int[] st : cp.radarStations()) {
            int hx = plan.anchorX() + st[0], hz = plan.anchorZ() + st[1];
            int cx = hx + HUT / 2, cz = hz + HUT / 2;
            for (int x = Math.max(hx, window.minX()); x <= Math.min(hx + HUT - 1, window.maxX()); x++) {
                for (int z = Math.max(hz, window.minZ()); z <= Math.min(hz + HUT - 1, window.maxZ()); z++) {
                    int g = probe.landY(x, z);
                    // Under water the foundation rises from the bed (D354).
                    if (g == HeightProbe.NONE)
                        g = probe.waterSurfaceY(x, z) == HeightProbe.NONE ? HeightProbe.NONE : probe.seaFloorY(x, z);
                    if (g == HeightProbe.NONE) continue;
                    ResolvedBlock wall = blocks.named("hutWall", x, groundY, z);
                    ResolvedBlock roof = blocks.named("hutRoof", x, groundY, z);
                    ResolvedBlock casing = blocks.named("radarCasing", x, groundY, z);
                    boolean edge = x == hx || x == hx + HUT - 1 || z == hz || z == hz + HUT - 1;
                    boolean door = x == cx && z == hz + HUT - 1;
                    int dx = x - cx, dz = z - cz;
                    boolean padColumn = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                    int top = groundY + WALL_H + 1 + MAST_H;
                    // One write per cell: foundation, floor, walls, roof, then air up to the mast top (it clears
                    // trees and terrain bumps in the way).
                    for (int y = Math.min(g, groundY - 1) + 1; y <= top; y++) {
                        ResolvedBlock b = null;
                        int h = y - groundY;
                        if (h <= 0) b = wall;
                        else if (h <= WALL_H) b = edge && !(door && h <= 2) ? wall : null;
                        else if (h == WALL_H + 1) b = roof;
                        else if (padColumn) {
                            int cell = RadarStation.cellAt(dx, dz, h - (WALL_H + 1));
                            if (cell == RadarStation.CASING) b = casing;
                            else if (cell != RadarStation.NOTHING) {
                                // The controller and the Maintenance Hatch are GT machines: the placeholder is air,
                                // then the hook builds the machine, as for the silo.
                                if (sink.setBlock(x, y, z, blocks.air(), 0)) {
                                    n++;
                                    if (world != null) machines.place(world, (byte) cell, x, y, z);
                                }
                                continue;
                            }
                        }
                        if (b != null ? sink.setBlock(x, y, z, b.block, b.meta)
                            : y > groundY && sink.setBlock(x, y, z, blocks.air(), 0)) n++;
                    }
                }
            }
        }
        return n;
    }
}
