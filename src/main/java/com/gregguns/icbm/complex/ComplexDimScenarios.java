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
import net.minecraft.world.WorldProvider;
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
import com.gregguns.icbm.complex.gen.NeighborComplex;
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

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new OnDim("icbm.complex.dim.overworld", "Overworld", -1));
        registry.register(new OnDim("icbm.complex.dim.moon", "moon", -1));
        registry.register(new OnDim("icbm.complex.dim.mars", "mars", -1));
        registry.register(new OnDim("icbm.complex.dim.ross128b", "ross128b", 0));
        registry.register(new OnDim("icbm.complex.dim.ross128ba", "ross128ba", 1));
    }

    private static final class OnDim implements Scenario {

        private final String name;
        private final String dimName;
        private final int rossIndex;

        OnDim(String name, String dimName, int rossIndex) {
            this.name = name;
            this.dimName = dimName;
            this.rossIndex = rossIndex;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "Generates a real silo complex in '" + dimName
                + "' through the chunk provider (chance forced to 100 percent): no cascade, no dropped write, no"
                + " PopulateGuard violation; shaft mouths meet the terrain, the trapdoor lies flush, radar huts and"
                + " armory frames stand, deep guard posts exist; cells the terrain rejects are skipped and logged"
                + " (D128, D38, GGS-11-08, GGS-11-09).";
        }

        private World world() {
            MinecraftServer srv = MinecraftServer.getServer();
            if (rossIndex >= 0) {
                int[] ids = Ross128Dimensions.ids();
                return ids.length > rossIndex ? srv.worldServerForDimension(ids[rossIndex]) : null;
            }
            if (dimName.equalsIgnoreCase("Overworld")) return srv.worldServerForDimension(0);
            for (int id : DimensionManager.getStaticDimensionIDs()) {
                try {
                    WorldProvider p = DimensionManager.createProviderFor(id);
                    if (p != null && dimName.equalsIgnoreCase(p.getDimensionName()))
                        return srv.worldServerForDimension(id);
                } catch (Throwable t) {
                    // a provider that cannot name itself without a world is not the one we want
                }
            }
            List<String> seen = new ArrayList<String>();
            for (int id : DimensionManager.getStaticDimensionIDs()) {
                try {
                    seen.add(
                        id + "="
                            + DimensionManager.createProviderFor(id)
                                .getDimensionName());
                } catch (Throwable t) {
                    seen.add(id + "=?");
                }
            }
            GregGuns.LOG.info("{}: no provider named '{}' among {}", name, dimName, seen);
            return null;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = world();
            if (world == null) {
                return ScenarioResult.skip(name, "dimension '" + dimName + "' is not present in this environment");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name);
            String actual = world.provider.getDimensionName();
            out.note("dimension id " + world.provider.dimensionId + " name '" + actual + "'");
            out.check(
                dimName.equalsIgnoreCase("Overworld") || PlanetTable.tierOf(actual) != null,
                "the provider's name '" + actual + "' is in the planet table");
            int chance = IcbmConfig.siloComplexChancePercent;
            IcbmConfig.siloComplexChancePercent = 100;
            try {
                PopulateGuard.register();
                run(world, out);
            } finally {
                IcbmConfig.siloComplexChancePercent = chance;
            }
            return out.build();
        }

        private void run(World world, ScenarioResult.Builder out) {
            ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
            StructureState state = StructureState.forWorld(world);
            int placed = 0;
            List<String> rejects = new ArrayList<String>();
            int tried = 0;
            // Far cells, away from the spawn exclusion and from what other scenarios loaded.
            for (int ring = 4; ring < 40 && tried < CELLS && placed == 0; ring++) {
                for (int k = 0; k < 4 && tried < CELLS && placed == 0; k++) {
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
                    Set<Long> loadedBefore = loaded(cps);
                    int x0 = Math.floorDiv(plan.anchorX(), 16) - 1, x1 = Math.floorDiv(plan.anchorX() + 239, 16) + 1;
                    int z0 = Math.floorDiv(plan.anchorZ(), 16) - 1, z1 = Math.floorDiv(plan.anchorZ() + 239, 16) + 1;
                    for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) cps.loadChunk(x, z);
                    long[] after = counters();
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
                    surface(world, plan, d.groundY(), out, label);
                    contents(world, plan, out, label);
                }
            }
            out.check(placed > 0, "at least one cell hosts a complex; rejected: " + rejects);
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
        private static int top(World w, int x, int z) {
            for (int y = 254; y > 0; y--) {
                Block b = w.getBlock(x, y, z);
                if (b.getMaterial()
                    .blocksMovement() && !b.isLeaves(w, x, y, z)
                    && !b.isWood(w, x, y, z)) return y;
            }
            return -1;
        }

        private void surface(World w, StructurePlan plan, int groundY, ScenarioResult.Builder out, String label) {
            ComplexPlan cp = ComplexLayout.fromSeed(plan.chestSeedBase(), plan.cellX(), plan.cellZ());
            int floor = groundY - ComplexPlacer.DEPTH;
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
            out.check(w.getBlock(ex, ty, ez) == Blocks.trapdoor, label + ": the escape ladder ends in a trapdoor");
            out.checkEquals(label + ": trapdoor is closed and in the top half", 8, w.getBlockMetadata(ex, ty, ez));
            for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                int ny = top(w, ex + d[0], ez + d[1]);
                out.check(Math.abs(ny - ty) <= 1, label + ": trapdoor flush with terrain " + ty + " vs " + ny);
            }
            out.check(w.getBlock(ex, ty - 1, ez) == Blocks.ladder, label + ": a ladder under the trapdoor");
            out.checkEquals(label + ": ladder clings to its wall", cp.escapeMeta(), w.getBlockMetadata(ex, ty - 1, ez));
            for (SiloSlot s : cp.silos()) {
                int ox = plan.anchorX() + s.bounds().minX, oz = plan.anchorZ() + s.bounds().minZ;
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
                Block corner = w.getBlock(hx, groundY + 1, hz);
                out.check(
                    corner == com.gregguns.block.GregGunsCasings.INSTANCE,
                    label + ": radar hut wall is a casing");
                out.check(w.isAirBlock(hx + 2, groundY + 1, hz + 4), label + ": radar hut doorway open");
                out.check(
                    w.getBlock(hx + 2, groundY + 12, hz + 2) == Blocks.redstone_block,
                    label + ": PLACEHOLDER_RADAR_DISH on the mast");
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
            int floor = d.groundY() - ComplexPlacer.DEPTH;
            out.note(
                label + ": level " + level + ", ceiling " + ceiling + ", stick -> " + java.util.Arrays.toString(other));
            int deepWanted = 0, deepFound = 0, sticks = 0;
            for (Placement p : ComplexContents.build(cp, level, ceiling, other)) {
                int x = plan.anchorX() + p.x(), y = floor + p.y(), z = plan.anchorZ() + p.z();
                if (p.kind() == Placement.Kind.SPAWNER) {
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
            out.checkEquals(label + ": every planned elite/deep guard post stands", deepWanted, deepFound);
            if (other == null) out.check(sticks == 0, label + ": no stick without another complex");
            int frames = 0, bad = 0, expected = 0;
            for (Room r : cp.rooms()) {
                if (r.kind() != RoomKind.ARMORY) continue;
                for (Mark m : r.marks()) {
                    if (m.kind() == MarkKind.ITEM_FRAME_WALL) expected += m.extent();
                }
                Box b = r.bounds();
                @SuppressWarnings("unchecked")
                List<?> es = w.getEntitiesWithinAABB(
                    EntityItemFrame.class,
                    AxisAlignedBB.getBoundingBox(
                        plan.anchorX() + b.minX,
                        floor,
                        plan.anchorZ() + b.minZ,
                        plan.anchorX() + b.maxX,
                        floor + ComplexLayout.HEIGHT,
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
            out.check(frames >= expected * 8 / 10, label + ": armory frames " + frames + " of " + expected);
            out.checkEquals(label + ": frames without a gun or a wall", 0, bad);
            GregGuns.LOG.info("{} {}: {} frames, {} deep posts", name, label, frames, deepFound);
        }
    }
}
