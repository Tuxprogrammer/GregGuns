package com.gregguns.icbm.complex.gen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
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

    /** The raster of a plan, cached for the dozens of windows one complex spans (two entries are enough). */
    public ComplexRaster rasterOf(StructurePlan plan) {
        String key = cacheKey(plan);
        ComplexRaster r = cache.remove(key);
        if (r == null) {
            r = new ComplexRaster(ComplexLayout.fromSeed(plan.chestSeedBase(), plan.cellX(), plan.cellZ()));
            if (cache.size() >= 2) {
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
     * The ceiling the table gives a dimension: its row, or EV for a dimension the owner added to the config that the
     * table does not list. Ross128b and ba roll one per complex from their own nine weights.
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
        return this;
    }

    /** The activity level of a complex, rolled from its seed. */
    public ActivityLevel levelOf(ComplexPlan cp) {
        if (forcedLevel != null) {
            return forcedLevel;
        }
        return ActivityLevel.roll(cp.seed(), ActivityLevel.DEFAULT_WEIGHTS);
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
            if (contentsCache.size() >= 2) contentsCache.remove(
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

    private int writeWindow(StructurePlan plan, int groundY, PopulateWindow window, WindowedSink sink,
        HeightProbe probe) {
        ComplexRaster raster = rasterOf(plan);
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
                int g = probe.seaFloorY(x, z);
                if (g != HeightProbe.NONE && g < floorY + ComplexLayout.SILO_HEIGHT + 2) {
                    mask[ColumnMaskSink.index(window, x, z)] = true;
                    masked = true;
                }
            }
        }
        WindowedSink under = masked ? new ColumnMaskSink(sink, mask) : sink;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = 0; y < ComplexRaster.LEVELS; y++) {
                    byte c = raster.at(x - ax, y, z - az);
                    if (c == Cell.NONE) continue;
                    int wy = floorY + y;
                    if (c >= Cell.CONTROLLER && c <= Cell.HATCH_POWER) {
                        if (under.setBlock(x, wy, z, air, 0) && world != null) machines.place(world, c, x, wy, z);
                        n++;
                        continue;
                    }
                    ResolvedBlock b = c == Cell.LADDER ? blocks.ladder(cp.escapeMeta(), x, wy, z)
                        : blocks.resolve(c, x, wy, z);
                    if (b != null && under.setBlock(x, wy, z, b.block, b.meta)) n++;
                }
            }
        }
        n += shafts(cp, plan, floorY, window, sink, probe);
        n += ladder(cp, plan, floorY, window, sink, probe);
        n += radars(cp, plan, groundY, window, sink, probe);
        for (Mark m : cp.allMarks()) {
            int wx = ax + m.x(), wz = az + m.z();
            if (window.contains(wx, wz)) filler.fill(plan, m, wx, floorY + m.y(), wz, under);
        }
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
                ceilingOf(plan));
            for (Placement p : contentsOf(plan, cp)) applier.apply(p);
        }
        return n;
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
        // whole climb is written solid, from the level up to that column's own ground. Its window writes it, whether
        // or not the window holds the ladder.
        int meta = cp.escapeMeta();
        int bx = x + (meta == 4 ? 1 : meta == 5 ? -1 : 0), bz = z + (meta == 2 ? 1 : meta == 3 ? -1 : 0);
        if (window.contains(bx, bz)) {
            int bg = probe.landY(bx, bz);
            if (bg != HeightProbe.NONE) {
                for (int y = floorY + ComplexLayout.HEIGHT; y <= bg; y++) {
                    ResolvedBlock b = blocks.named("backing", bx, y, bz);
                    if (b != null && sink.setBlock(bx, y, bz, b.block, b.meta)) n++;
                }
            }
        }
        if (!window.contains(x, z)) return n;
        int g = probe.landY(x, z);
        if (g == HeightProbe.NONE) return n;
        for (int y = floorY + ComplexLayout.HEIGHT; y < g; y++) {
            ResolvedBlock b = blocks.ladder(meta, x, y, z);
            if (b != null && sink.setBlock(x, y, z, b.block, b.meta)) n++;
        }
        // landY is the top solid block, so the trapdoor replaces it; a top-half trapdoor then lies flush with the
        // surface (its top face at g + 1, level with the terrain around it).
        ResolvedBlock t = blocks.named("trapdoor", x, g, z);
        if (t != null && sink.setBlock(x, g, z, t.block, t.meta)) n++;
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
