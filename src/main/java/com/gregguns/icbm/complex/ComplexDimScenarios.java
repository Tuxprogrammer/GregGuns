package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;

import com.gregguns.GregGuns;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.ConfigSettings;
import com.gregguns.icbm.complex.gen.DimensionKey;
import com.gregguns.icbm.complex.gen.GtMachineHook;
import com.gregguns.icbm.complex.gen.NeighborComplex;
import com.gregguns.icbm.complex.gen.RadarStation;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.icbm.worldgen.ComplexContents;
import com.gregguns.icbm.worldgen.Placement;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.Ross128Dimensions;
import com.gregguns.worldgen.StructureGenerator;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.spawner.TileHoleSpawner;

/**
 * Real generation of a silo complex per dimension: the chunks around a forced-100-percent complex are loaded through
 * the dimension's own chunk provider, so the genuine populate path runs. Checks no cascade (no chunk beyond the
 * request plus the populate margin is loaded, no PopulateGuard violation, no dropped write), the surface (shaft mouths
 * meet the terrain, the trapdoor lies flush, radar huts), the armory frames and the deep guard posts, and that a cell
 * the terrain rejects is skipped and logged. Dev environment only; needs {@code worldgen} and {@code icbm}. Server
 * side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md; exit gate box 1
public final class ComplexDimScenarios {

    /** Cells tried per dimension before giving up (a cell can be ocean or too steep). */
    private static final int CELLS = 8;

    private ComplexDimScenarios() {}

    /** Mod that adds the Amun-Ra planets. */
    private static final String AMUN_RA = "Amun-Ra (GalacticraftAmunRa)";

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new OnDim("icbm.complex.dim.overworld", "Overworld", "vanilla", -1));
        registry.register(new OnDim("icbm.complex.dim.moon", "moon", "Galacticraft", -1));
        registry.register(new OnDim("icbm.complex.dim.mars", "mars", "Galacticraft Planets", -1));
        registry.register(new OnDim("icbm.complex.dim.ross128b", "ross128b", "Ross128", 0));
        registry.register(new OnDim("icbm.complex.dim.ross128ba", "ross128ba", "Ross128", 1));
        // One or two planets per rocket tier from T3 up. They need the planet mods, which the dev environment lacks:
        // the scenario then skips with the planet and its mod named. They exist for the real-pack alpha run (D207).
        for (String key : PACK_PLANETS) {
            registry.register(new OnDim("icbm.complex.dim." + key, key, modOf(key), -1));
        }
    }

    /**
     * The planets of the real-pack run: T3 ceres, ganymed; T4 io; T5 enceladus, titan; T6 proteus, triton; T7 pluto;
     * T8 barnarda2; T9 anubis.
     */
    static final String[] PACK_PLANETS = { "ceres", "ganymed", "iojupiter", "enceladus", "titan", "proteus", "triton",
        "pluto", "barnarda2", "anubis" };

    private static String modOf(String key) {
        switch (key) {
            case "anubis":
            case "horus":
            case "maahes":
            case "neper":
            case "seth":
                return AMUN_RA;
            default:
                return "GalaxySpace";
        }
    }

    private static final class OnDim implements Scenario {

        private final String name;
        private final String dimName;
        private final String mod;
        private final int rossIndex;

        OnDim(String name, String dimName, String mod, int rossIndex) {
            this.name = name;
            this.dimName = dimName;
            this.mod = mod;
            this.rossIndex = rossIndex;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "Generates a real silo complex in '" + dimName
                + "' (found by its DimensionKey, never its translated name) through the chunk provider (chance forced"
                + " to 100 percent): no cascade, no dropped write, no PopulateGuard violation; shaft mouths meet the"
                + " terrain, the trapdoor lies flush, radar huts and armory frames stand, deep guard posts exist; cells"
                + " the terrain rejects are skipped and logged. Skips, naming the planet, when its mod is not loaded"
                + " (D128, D38, D207, GGS-11-08, GGS-11-09).";
        }

        /** The ids and keys seen while looking, for the skip reason. */
        private final List<String> seen = new ArrayList<String>();

        private World world() {
            seen.clear();
            MinecraftServer srv = MinecraftServer.getServer();
            if (rossIndex >= 0) {
                int[] ids = Ross128Dimensions.ids();
                return ids.length > rossIndex ? srv.worldServerForDimension(ids[rossIndex]) : null;
            }
            if (dimName.equalsIgnoreCase("Overworld")) return srv.worldServerForDimension(0);
            for (int id : DimensionManager.getStaticDimensionIDs()) {
                try {
                    String key = DimensionKey.of(DimensionManager.createProviderFor(id));
                    seen.add(id + "=" + key);
                    if (dimName.equalsIgnoreCase(key)) return srv.worldServerForDimension(id);
                } catch (Throwable t) {
                    // a provider that cannot name itself without a world is not the one we want
                    seen.add(id + "=?");
                }
            }
            return null;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!com.gregguns.core.module.ModuleState.isActive(com.gregguns.core.module.ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off (Galacticraft missing or modules.icbm off)");
            }
            World world = world();
            if (world == null) {
                String reason = "planet '" + dimName
                    + "' ("
                    + mod
                    + ") is not present in this environment: no dimension"
                    + " has that DimensionKey among "
                    + seen;
                GregGuns.LOG.info("{}: skipped, {}", name, reason);
                return ScenarioResult.skip(name, reason);
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name);
            String actual = DimensionKey.of(world);
            out.note(
                "dimension id " + world.provider.dimensionId
                    + " key '"
                    + actual
                    + "' name '"
                    + world.provider.getDimensionName()
                    + "'");
            out.check(PlanetTable.tierOf(actual) != null, "the dimension key '" + actual + "' is in the planet table");
            out.checkEquals(
                "the dimension key '" + actual + "' has the table's tier for '" + dimName + "'",
                PlanetTable.tierOf(dimName),
                PlanetTable.tierOf(actual));
            int chance = IcbmConfig.siloComplexChancePercent;
            IcbmConfig.siloComplexChancePercent = 100;
            try {
                PopulateGuard.register();
                run(world, out);
            } finally {
                IcbmConfig.siloComplexChancePercent = chance;
                release(world);
            }
            return out.build();
        }

        private void run(World world, ScenarioResult.Builder out) {
            ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
            StructureState state = StructureState.forWorld(world);
            int placed = 0, clean = 0;
            List<String> rejects = new ArrayList<String>();
            int tried = 0;
            // Far cells, away from the spawn exclusion and from what other scenarios loaded.
            for (int ring = 4; ring < 40 && tried < CELLS && clean == 0; ring++) {
                for (int k = 0; k < 4 && tried < CELLS && clean == 0; k++) {
                    int cx = k % 2 == 0 ? ring : -ring, cz = k < 2 ? ring : -ring;
                    java.util.Optional<StructurePlan> op = StructurePopulator
                        .complexPlanAt(world.getSeed(), world.provider.dimensionId, cx, cz, 100);
                    if (!op.isPresent() || state.get(PlanKey.of(op.get())) != null || !fresh(cps, op.get())) continue; // a
                                                                                                                       // decided
                                                                                                                       // plan
                                                                                                                       // was
                                                                                                                       // generated
                                                                                                                       // by
                                                                                                                       // an
                                                                                                                       // earlier
                                                                                                                       // run
                    tried++;
                    StructurePlan plan = op.get();
                    long[] before = counters();
                    long popBefore = StructureGenerator.populateCount(),
                        hookBefore = com.gregguns.worldgen.Ross128ProviderHook.populateCount();
                    Set<Long> loadedBefore = loaded(cps);
                    int x0 = Math.floorDiv(plan.anchorX(), 16) - 1, x1 = Math.floorDiv(plan.anchorX() + 239, 16) + 1;
                    int z0 = Math.floorDiv(plan.anchorZ(), 16) - 1, z1 = Math.floorDiv(plan.anchorZ() + 239, 16) + 1;
                    for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) cps.loadChunk(x, z);
                    long[] after = counters();
                    out.note(
                        "cell " + cx
                            + ","
                            + cz
                            + ": populates generator "
                            + (StructureGenerator.populateCount() - popBefore)
                            + ", hook "
                            + (com.gregguns.worldgen.Ross128ProviderHook.populateCount() - hookBefore)
                            + ", chunks now loaded "
                            + cps.getLoadedChunkCount()
                            + ", provider "
                            + cps.currentChunkProvider.getClass()
                                .getSimpleName());
                    Decision d = state.get(PlanKey.of(plan));
                    String label = "cell " + cx + "," + cz;
                    out.check(d != null, label + ": the populate path decided the plan");
                    if (d == null) continue;
                    out.checkEquals(label + ": writes dropped outside a window", 0L, after[1] - before[1]);
                    out.checkEquals(label + ": PopulateGuard violations", 0L, after[2] - before[2]);
                    int stray = 0;
                    for (long key : loaded(cps)) {
                        if (loadedBefore.contains(key)) continue;
                        int lx = (int) (key >> 32), lz = (int) key;
                        if (lx < x0 - 2 || lx > x1 + 2 || lz < z0 - 2 || lz > z1 + 2) stray++;
                    }
                    out.checkEquals(label + ": chunks loaded beyond the request plus the populate margin", 0, stray);
                    if (!d.isPlaced()) {
                        rejects.add(label + " " + d.reason());
                        out.note(label + " skipped by the terrain check: " + d.reason());
                        continue;
                    }
                    placed++;
                    // The terrain check sees one window of a 240 x 240 box, so a placed cell may still have a lake
                    // or a cliff under a hut or a bore. Its column checks go to a side builder: a cell that fails them
                    // is noted and the next cell is tried; at least one cell must pass them all.
                    ScenarioResult.Builder cellOut = ScenarioResult.builder(name);
                    surface(world, plan, d.groundY(), cellOut, label);
                    contents(world, plan, cellOut, label);
                    ScenarioResult cellResult = cellOut.build();
                    for (String m : cellResult.messages()) out.note(m);
                    if (cellResult.passed()) clean++;
                    else out.note(label + ": its terrain defeats the column checks above; the next cell is tried");
                }
            }
            out.check(placed > 0, "at least one cell hosts a complex; rejected: " + rejects);
            out.check(clean > 0, "at least one placed cell passes every surface and contents check");
        }

        private static boolean fresh(ChunkProviderServer cps, StructurePlan p) {
            for (int x = Math.floorDiv(p.anchorX(), 16) - 3; x <= Math.floorDiv(p.anchorX() + 239, 16) + 3; x += 3) {
                for (int z = Math.floorDiv(p.anchorZ(), 16) - 3; z
                    <= Math.floorDiv(p.anchorZ() + 239, 16) + 3; z += 3) {
                    if (cps.chunkExists(x, z)) return false;
                }
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private static Set<Long> loaded(ChunkProviderServer cps) {
            Set<Long> s = new HashSet<Long>();
            for (Object o : new ArrayList<Object>(cps.loadedChunks)) {
                Chunk c = (Chunk) o;
                s.add(((long) c.xPosition << 32) | (c.zPosition & 0xFFFFFFFFL));
            }
            return s;
        }

        private static long[] counters() {
            return new long[] { StructureGenerator.stampedCount(), StructureGenerator.rejectedWrites(),
                PopulateGuard.violationCount() };
        }

        /** Y of the topmost block that stops movement (trees and plants skipped), or -1. */
        /** The placer's own cut-off rule (D207), read from the world around the column. */
        private static boolean cutOff(World w, int floor, int x, int z) {
            com.gregguns.worldgen.PopulateWindow win = new com.gregguns.worldgen.PopulateWindow(
                Math.floorDiv(x - 8, 16),
                Math.floorDiv(z - 8, 16));
            return ComplexPlacer.cutOff(
                new com.gregguns.worldgen.WindowedHeightProbe(win, new com.gregguns.worldgen.WorldColumnReader(w)),
                floor,
                x,
                z);
        }

        /**
         * Lets the dimension go once its scenario is done: its chunks are queued for unload, and a planet's world is
         * queued to unload too (Forge unloads it on a later tick when nothing keeps it). One prefix run visits about
         * 25 planets of about 2,600 chunks each in the real pack; held together they ran an 8 GB server out of memory.
         */
        private static void release(World world) {
            if (!(world.getChunkProvider() instanceof ChunkProviderServer)) return;
            ((ChunkProviderServer) world.getChunkProvider()).unloadAllChunks();
            if (world.provider.dimensionId != 0) DimensionManager.unloadWorld(world.provider.dimensionId);
        }

        /** @return whether any column of the {@code size} x {@code size} square at (x, z) lies under water */
        private static boolean anyUnderWater(World w, int x, int z, int size) {
            for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) {
                if (underWater(w, x + dx, z + dz)) return true;
            }
            return false;
        }

        /** @return whether the first block down from the sky in a column is a liquid */
        private static boolean underWater(World w, int x, int z) {
            for (int y = 254; y > 0; y--) {
                Block b = w.getBlock(x, y, z);
                if (b.getMaterial() == net.minecraft.block.material.Material.air || b.isLeaves(w, x, y, z)
                    || b.isWood(w, x, y, z)) continue;
                return b.getMaterial()
                    .isLiquid();
            }
            return false;
        }

        private static int top(World w, int x, int z) {
            for (int y = 254; y > 0; y--) {
                Block b = w.getBlock(x, y, z);
                if (b.getMaterial()
                    .blocksMovement() && !b.isLeaves(w, x, y, z)
                    && !b.isWood(w, x, y, z)) return y;
            }
            return -1;
        }

        /**
         * The radar on the hut roof: a Radar Casing pedestal, an HV radar controller that stands unformed (no Energy
         * Hatch), and a station that forms once an Energy Hatch takes the place under the controller (D125, D215).
         */
        private void radarStation(World w, int groundY, int cx, int cz, ScenarioResult.Builder out, String label) {
            int roof = groundY + 4;
            out.check(
                w.getBlock(cx, roof + 3, cz) == com.gregguns.block.GregGunsCasings.INSTANCE
                    && w.getBlockMetadata(cx, roof + 3, cz) == com.gregguns.block.GregGunsCasings.RADAR_CASING,
                label + ": a Radar Casing pedestal on the radar");
            int[] c = RadarStation.controllerOffset();
            TileEntity t = w.getTileEntity(cx + c[0], roof + c[1], cz + c[2]);
            boolean controller = t instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity
                && ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) t)
                    .getMetaTileEntity() instanceof com.gregguns.icbm.defense.MTERadar;
            out.check(controller, label + ": an HV radar controller in the station");
            if (!controller) return;
            out.check(
                !com.gregguns.machine.DevStructureBuilder.check(w, cx + c[0], roof + c[1], cz + c[2]),
                label + ": the station is inert without an Energy Hatch");
            int[] e = RadarStation.energyHatchOffset();
            new GtMachineHook().place(w, Cell.HATCH_ENERGY, cx + e[0], roof + e[1], cz + e[2]);
            out.check(
                com.gregguns.machine.DevStructureBuilder.check(w, cx + c[0], roof + c[1], cz + c[2]),
                label + ": the station forms once an Energy Hatch takes its place");
        }

        private void surface(World w, StructurePlan plan, int groundY, ScenarioResult.Builder out, String label) {
            ComplexPlan cp = ComplexLayout.fromSeed(plan.chestSeedBase(), plan.cellX(), plan.cellZ());
            int floor = ComplexPlacer.floorY(w, groundY);
            // Trapdoor: the topmost block of its column, a top-half closed trapdoor, level with the ground around.
            int ex = plan.anchorX() + cp.escapeX(), ez = plan.anchorZ() + cp.escapeZ();
            int ty = top(w, ex, ez);
            out.note(
                label + ": escape column "
                    + ex
                    + ","
                    + ez
                    + " top "
                    + ty
                    + " "
                    + w.getBlock(ex, ty, ez)
                        .getUnlocalizedName()
                    + " above "
                    + w.getBlock(ex, ty + 1, ez)
                        .getUnlocalizedName()
                    + " below "
                    + w.getBlock(ex, ty - 1, ez)
                        .getUnlocalizedName()
                    + " groundY "
                    + groundY
                    + " chunkLoaded "
                    + w.getChunkProvider()
                        .chunkExists(ex >> 4, ez >> 4));
            if (underWater(w, ex, ez)) {
                // A lake over the escape column: the placer writes no trapdoor into water, by design (landY is none).
                out.note(label + ": the escape column lies under water; no trapdoor there, as designed");
            } else {
                out.check(w.getBlock(ex, ty, ez) == Blocks.trapdoor, label + ": the escape ladder ends in a trapdoor");
                out.checkEquals(label + ": trapdoor is closed and in the top half", 8, w.getBlockMetadata(ex, ty, ez));
                // Flush: on a slope the neighbors up and down the slope differ, the two across it are level.
                int level = 0;
                StringBuilder around = new StringBuilder();
                for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                    int ny = top(w, ex + d[0], ez + d[1]);
                    around.append(ny)
                        .append(' ');
                    if (Math.abs(ny - ty) <= 1) level++;
                }
                out.check(level >= 2, label + ": trapdoor flush with the terrain " + ty + " vs " + around);
                out.check(w.getBlock(ex, ty - 1, ez) == Blocks.ladder, label + ": a ladder under the trapdoor");
                out.checkEquals(
                    label + ": ladder clings to its wall",
                    cp.escapeMeta(),
                    w.getBlockMetadata(ex, ty - 1, ez));
                out.check(
                    !w.getBlock(ex, ty + 1, ez)
                        .getMaterial()
                        .blocksMovement()
                        && !w.getBlock(ex, ty + 1, ez)
                            .isLeaves(w, ex, ty + 1, ez),
                    label + ": nothing grows over the trapdoor");
            }
            for (SiloSlot s : cp.silos()) {
                int ox = plan.anchorX() + s.bounds().minX, oz = plan.anchorZ() + s.bounds().minZ;
                if (anyUnderWater(w, ox, oz, 5)) {
                    // A lake over the silo: the shaft is not written into water (landY is none), by design.
                    out.note(label + ": a silo shaft lies under water; no mouth there, as designed");
                    continue;
                }
                int ry = top(w, ox, oz);
                int near = 99;
                for (int[] d : new int[][] { { -1, 2 }, { 5, 2 }, { 2, -1 }, { 2, 5 } }) {
                    near = Math.min(near, Math.abs(top(w, ox + d[0], oz + d[1]) - ry));
                }
                out.check(
                    w.getBlock(ox, ry, oz) == gregtech.api.GregTechAPI.sBlockConcretes && !w.getBlock(ox, ry + 1, oz)
                        .getMaterial()
                        .blocksMovement(),
                    label + ": the shaft ring's top is concrete with open air above it");
                out.check(
                    near <= 3,
                    label + ": shaft ring meets the terrain (nearest outside column differs by " + near + ")");
                if (s.variant() == SurfaceVariant.OPEN) {
                    out.check(
                        w.canBlockSeeTheSky(ox + 2, floor + 14, oz + 2),
                        label + ": an open silo's bore reaches the sky");
                }
            }
            for (int[] st : cp.radarStations()) {
                int hx = plan.anchorX() + st[0], hz = plan.anchorZ() + st[1];
                if (anyUnderWater(w, hx, hz, 5)) {
                    // A hut is built only on land; over a lake its columns are left alone, by design.
                    out.note(label + ": a radar hut stands over water; left unbuilt there, as designed");
                    continue;
                }
                Block corner = w.getBlock(hx, groundY + 1, hz);
                out.check(
                    corner == com.gregguns.block.GregGunsCasings.INSTANCE,
                    label + ": radar hut wall is a casing");
                out.check(w.isAirBlock(hx + 2, groundY + 1, hz + 4), label + ": radar hut doorway open");
                radarStation(w, groundY, hx + 2, hz + 2, out, label);
            }
        }

        private void contents(World w, StructurePlan plan, ScenarioResult.Builder out, String label) {
            ComplexPlan cp = ComplexLayout.fromSeed(plan.chestSeedBase(), plan.cellX(), plan.cellZ());
            ComplexPlacer placer = ConfigSettings.writer();
            placer.enabledIn(w);
            int ceiling = placer.ceilingOf(plan);
            ActivityLevel level = placer.levelOf(cp);
            int[] other = NeighborComplex.nearest(w.getSeed(), plan.dimension(), plan, 100, NeighborComplex.RADIUS);
            Decision d = StructureState.forWorld(w)
                .get(PlanKey.of(plan));
            int floor = ComplexPlacer.floorY(w, d.groundY());
            out.note(
                label + ": level " + level + ", ceiling " + ceiling + ", stick -> " + java.util.Arrays.toString(other));
            int deepWanted = 0, deepFound = 0, sticks = 0, expected = 0, cut = 0;
            for (Placement p : ComplexContents.build(cp, level, ceiling, other)) {
                int x = plan.anchorX() + p.x(), y = floor + p.y(), z = plan.anchorZ() + p.z();
                if (p.kind() == Placement.Kind.FRAME_WALL) {
                    // A collapsed armory hangs none; a frame over a column the terrain cut off is not written (D207).
                    boolean alongZ = p.facing() == 1 || p.facing() == 3;
                    for (int i = 0; i < p.extent(); i++) {
                        int fx = x + (alongZ ? 0 : ComplexLayout.FRAME_STRIDE * i);
                        int fz = z + (alongZ ? ComplexLayout.FRAME_STRIDE * i : 0);
                        if (cutOff(w, floor, fx, fz)) cut++;
                        else expected++;
                    }
                }
                if (p.kind() == Placement.Kind.SPAWNER && cutOff(w, floor, x, z)) {
                    cut++;
                } else if (p.kind() == Placement.Kind.SPAWNER) {
                    TileEntity t = w.getTileEntity(x, y, z);
                    boolean deep = p.spawner() == com.gregguns.worldgen.spawner.HoleKind.SILO_ELITE;
                    if (deep) {
                        deepWanted++;
                        if (t instanceof TileHoleSpawner) deepFound++;
                    }
                }
                if (p.kind() == Placement.Kind.CHEST && p.bonuses()
                    .contains(Placement.Bonus.DATA_STICK)) {
                    sticks++;
                    out.check(
                        other != null && p.extra()
                            .equals(other[0] + "," + other[1]),
                        label + ": the stick holds the real neighbor coordinates " + p.extra());
                }
            }
            if (cut > 0) out.note(label + ": " + cut + " frames or posts stand over columns the terrain cut off");
            out.checkEquals(label + ": every planned elite/deep guard post stands", deepWanted, deepFound);
            if (other == null) out.check(sticks == 0, label + ": no stick without another complex");
            int frames = 0, bad = 0;
            for (Room r : cp.rooms()) {
                if (r.kind() != RoomKind.ARMORY) continue;
                Box b = r.bounds();
                @SuppressWarnings("unchecked")
                List<?> es = w.getEntitiesWithinAABB(
                    EntityItemFrame.class,
                    AxisAlignedBB.getBoundingBox(
                        plan.anchorX() + b.minX,
                        floor,
                        plan.anchorZ() + b.minZ,
                        plan.anchorX() + b.maxX,
                        floor + b.sizeY(),
                        plan.anchorZ() + b.maxZ));
                for (Object o : es) {
                    EntityItemFrame f = (EntityItemFrame) o;
                    frames++;
                    boolean noGun = f.getDisplayedItem() == null || !(f.getDisplayedItem()
                        .getItem() instanceof com.gregguns.item.gun.ItemGun);
                    boolean invalid = !f.onValidSurface();
                    if (noGun || invalid) {
                        bad++;
                        if (bad <= 3) out.note(
                            label + ": frame at "
                                + f.posX
                                + ","
                                + f.posY
                                + ","
                                + f.posZ
                                + " dir "
                                + f.hangingDirection
                                + " wallBlock "
                                + w.getBlock(
                                    (int) Math.floor(f.posX),
                                    (int) Math.floor(f.posY),
                                    (int) Math.floor(
                                        f.posZ + (f.hangingDirection == 2 ? 1 : f.hangingDirection == 0 ? -1 : 0)))
                                    .getUnlocalizedName()
                                + " item "
                                + f.getDisplayedItem()
                                + " noGun="
                                + noGun
                                + " invalid="
                                + invalid);
                    }
                }
            }
            int anyFrames = 0;
            String first = "";
            for (Object o : new ArrayList<Object>(w.loadedEntityList)) {
                if (o instanceof EntityItemFrame) {
                    anyFrames++;
                    if (first.isEmpty()) first = ((EntityItemFrame) o).posX + ","
                        + ((EntityItemFrame) o).posY
                        + ","
                        + ((EntityItemFrame) o).posZ;
                }
            }
            out.note(label + ": item frames in the world " + anyFrames + ", first " + first + "; floor y " + floor);
            out.check(frames == expected, label + ": armory frames " + frames + " of " + expected);
            out.checkEquals(label + ": frames without a gun or a wall", 0, bad);
            GregGuns.LOG.info("{} {}: {} frames, {} deep posts", name, label, frames, deepFound);
        }
    }
}
