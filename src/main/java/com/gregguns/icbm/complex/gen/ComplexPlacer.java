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
import com.gregguns.icbm.worldgen.Placement;
import com.gregguns.icbm.worldgen.SiloCeiling;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.HeightProbe;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StructurePlan;
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
            st = styles.of(raster, dim, levelOf(raster.plan()));
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
        stylesCache.clear();
        summaries.clear();
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
        skipListener.accept(reason);
        GregGuns.LOG.info(
            "Silo complex skipped at dimension {} cell {},{} ({}): {}",
            plan.dimension(),
            plan.cellX(),
            plan.cellZ(),
            plan.anchorX() + "," + plan.anchorZ(),
            reason);
    }

    /** Lowest level floor: clear of bedrock. */
    private static final int MIN_FLOOR = 6;

    /**
     * The level floor: {@link #DEPTH} under the recorded ground, and no higher than {@link #DEPTH} under the
     * dimension's average ground level, so a plan recorded on a hilltop still lies under the lower land around it.
     * A pure function of the recorded height and the provider, so every window agrees.
     */
    int floorYOf(int groundY) {
        return floorY(world, groundY);
    }

    /** @see #floorYOf the same rule for a given world (null: the recorded ground alone) */
    public static int floorY(World world, int groundY) {
        int avg = world == null ? Integer.MAX_VALUE : world.provider.getAverageGroundLevel();
        return Math.max(MIN_FLOOR, Math.min(groundY, avg) - DEPTH);
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
        return this;
    }

    /** @param level the level every complex gets instead of its rolled one (scenarios), or null to roll */
    public ComplexPlacer forceLevel(ActivityLevel level) {
        this.forcedLevel = level;
        contentsCache.clear();
        stylesCache.clear();
        return this;
    }

    /** The activity level of a complex, rolled from its seed. */
    public ActivityLevel levelOf(ComplexPlan cp) {
        if (forcedLevel != null) {
            return forcedLevel;
        }
        return ActivityLevel
            .roll(cp.seed(), ActivityLevel.weightsFrom(com.gregguns.core.config.IcbmConfig.siloComplexActivityWeights));
    }

    private java.util.List<Placement> contentsOf(StructurePlan plan, ComplexPlan cp) {
        String key = cacheKey(plan);
        java.util.List<Placement> l = contentsCache.remove(key);
        if (l == null) {
            // The comms data stick points at the nearest other complex the seed rolls (block x, z of its hall).
            int[] other = world == null ? null
                : NeighborComplex
                    .nearest(world.getSeed(), plan.dimension(), plan, settings.chancePercent(), NeighborComplex.RADIUS);
            l = ComplexContents.build(cp, levelOf(cp), ceilingOf(plan), other);
            if (contentsCache.size() >= CACHED) contentsCache.remove(
                contentsCache.keySet()
                    .iterator()
                    .next());
        }
        contentsCache.put(key, l);
        return l;
    }

    @Override
    public int write(StructurePlan plan, int groundY, PopulateWindow window, WindowedSink sink, HeightProbe probe) {
        // The GT blocks the window places (silo concrete, hatches) start no machine-update search (D205).
        boolean before = machines.suspendUpdates();
        try {
            return writeWindow(plan, groundY, window, sink, probe);
        } finally {
            machines.resumeUpdates(before);
        }
    }

    /** Plans no layout grew from (never seen in 40,000 seeds), so each is logged once, not once per window. */
    private final Set<String> unbuildable = new java.util.LinkedHashSet<String>();

    private int writeWindow(StructurePlan plan, int groundY, PopulateWindow window, WindowedSink sink,
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
        int floorY = floorYOf(groundY);
        int ax = plan.anchorX(), az = plan.anchorZ();
        int x0 = Math.max(ax, window.minX()), x1 = Math.min(ax + ComplexLayout.SIZE - 1, window.maxX());
        int z0 = Math.max(az, window.minZ()), z1 = Math.min(az + ComplexLayout.SIZE - 1, window.maxZ());
        int n = 0;
        Block air = blocks.air();
        // The recorded ground comes from one window; this window's own columns may lie lower (a valley, a lake bed).
        // A column whose ground is within a few blocks of the level's top is cut off, so nothing breaks the surface.
        boolean[] mask = new boolean[PopulateWindow.SIZE * PopulateWindow.SIZE];
        boolean masked = false;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (cutOff(probe, floorY, x, z)) {
                    mask[ColumnMaskSink.index(window, x, z)] = true;
                    masked = true;
                }
            }
        }
        int[] seal = sealFloors(probe, window, floorY, mask, x0, x1, z0, z1, ax, az);
        WindowedSink under = masked ? new ColumnMaskSink(sink, mask) : sink;
        Styles st = stylesOf(plan);
        // The role audit (D267): tag every write with the pass that made it, then read the complex back once its
        // last window is written. Off in normal play.
        RoleAudit.TaggingSink tagUnder = null, tagSink = null;
        String auditKey = null;
        if (RoleAudit.enabled()) {
            auditKey = System.identityHashCode(this) + ":" + cacheKey(plan);
            RoleAudit.Tracker t = RoleAudit
                .tracker(auditKey, world, raster, st, auditWhere(plan, floorY), levelOf(cp).name(), ax, floorY, az);
            under = tagUnder = new RoleAudit.TaggingSink(under, t);
            sink = tagSink = new RoleAudit.TaggingSink(sink, t);
        }
        int maskedColumns = 0;
        for (boolean m : mask) if (m) maskedColumns++;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = 0; y < ComplexRaster.LEVELS; y++) {
                    int lx = x - ax, lz = z - az;
                    byte c = raster.at(lx, y, lz);
                    if (c == Cell.NONE) continue;
                    int wy = floorY + y;
                    // Open cells beside a cut-off column whose ground lies under them face its water or air: wall.
                    if (y >= 1 && Cell.open(c) && wy > seal[ColumnMaskSink.index(window, x, z)]) c = Cell.WALL;
                    if (c >= Cell.CONTROLLER && c <= Cell.HATCH_POWER) {
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
        if (contents) {
            ComplexApplier applier = new ComplexApplier(
                new QuietSink(under),
                world,
                ax,
                floorY,
                az,
                plan.chestSeedBase(),
                plan.cellX(),
                plan.cellZ(),
                ceilingOf(plan)).withLamps(st::litLamp);
            for (Placement p : contentsOf(plan, cp)) applier.apply(p);
            ComplexSigns.write(cp, raster, ax, floorY, az, ceilingOf(plan), new QuietSink(sink));
        }
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
    private static int[] sealFloors(HeightProbe probe, PopulateWindow window, int floorY, boolean[] mask, int x0,
        int x1, int z0, int z1, int ax, int az) {
        int[] seal = new int[PopulateWindow.SIZE * PopulateWindow.SIZE];
        java.util.Arrays.fill(seal, Integer.MAX_VALUE);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int i = ColumnMaskSink.index(window, x, z);
                if (mask[i]) continue;
                for (int d = 0; d < 4; d++) {
                    int nx = x + (d == 1 ? 1 : d == 3 ? -1 : 0), nz = z + (d == 2 ? 1 : d == 0 ? -1 : 0);
                    // Outside the complex nothing is written next to an open cell: every section ends in its wall.
                    if (nx < ax || nz < az || nx >= ax + ComplexLayout.SIZE || nz >= az + ComplexLayout.SIZE) continue;
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
            case Cell.FURN_BUNK:
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
        for (SiloSlot s : cp.silos()) {
            int sx = plan.anchorX() + s.bounds().minX, sz = plan.anchorZ() + s.bounds().minZ;
            for (int x = Math.max(sx, window.minX()); x <= Math.min(sx + 4, window.maxX()); x++) {
                for (int z = Math.max(sz, window.minZ()); z <= Math.min(sz + 4, window.maxZ()); z++) {
                    int g = probe.landY(x, z);
                    if (g == HeightProbe.NONE) continue;
                    boolean bore = x > sx && x < sx + 4 && z > sz && z < sz + 4;
                    int top = Math.min(g, BUILD_HEIGHT - 8);
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
                    if (!(bore && s.variant() == SurfaceVariant.CLOSED)) n += clearAbove(sink, x, z, Math.max(top, g));
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
        int cell = (x - sx - 1) * 3 + (z - sz - 1);
        long h = StructureHash.mix(sx * 31L + sz);
        int hole1 = (int) Math.floorMod(h, 8), hole2 = (int) Math.floorMod(h >>> 8, 8);
        int idx = cell > 4 ? cell - 1 : cell; // index among the eight non-center cells
        boolean center = cell == 4;
        if (!center && (idx == hole1 || idx == hole2)) {
            for (int y = top; y <= BUILD_HEIGHT; y++) if (sink.setBlock(x, y, z, blocks.air(), 0)) n++;
        } else if (cap != null && sink.setBlock(x, top, z, cap.block, cap.meta)) {
            n++;
        }
        return n;
    }

    /** The 1 x 1 escape ladder from the level up to a trapdoor at the surface. */
    private int ladder(ComplexPlan cp, StructurePlan plan, int floorY, PopulateWindow window, WindowedSink sink,
        HeightProbe probe) {
        int x = plan.anchorX() + cp.escapeX(), z = plan.anchorZ() + cp.escapeZ();
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
                for (int y = floorY + cp.escapeTop(); y < bg; y++) {
                    ResolvedBlock b = blocks.named("backing", bx, y, bz);
                    if (b != null && sink.setBlock(bx, y, bz, b.block, b.meta)) n++;
                }
            }
        }
        if (!window.contains(x, z)) return n;
        int g = probe.landY(x, z);
        if (g == HeightProbe.NONE) return n;
        for (int y = floorY + cp.escapeTop(); y < g; y++) {
            ResolvedBlock b = blocks.ladder(meta, x, y, z);
            if (b != null && sink.setBlock(x, y, z, b.block, b.meta)) n++;
        }
        // landY is the top solid block, so the trapdoor replaces it; a top-half trapdoor then lies flush with the
        // surface (its top face at g + 1, level with the terrain around it).
        ResolvedBlock t = blocks.named("trapdoor", x, g, z);
        if (t != null && sink.setBlock(x, g, z, t.block, t.meta)) n++;
        n += clearAbove(sink, x, z, g);
        return n;
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
     * it. A column with no land (water) is left alone.
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
