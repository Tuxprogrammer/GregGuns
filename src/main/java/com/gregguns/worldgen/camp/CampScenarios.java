package com.gregguns.worldgen.camp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.core.config.UtilityConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.camp.CampSpawnPort;
import com.gregguns.utility.camp.CampSpawnPorts;
import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.FlagPos;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureBiomes.CampColor;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.WorldTarget;
import com.gregguns.worldgen.layout.PaletteKeys;

/**
 * Scenarios of the camp placer (M8 G10). Dev environment only. Server side. {@code WorldgenScenarios.registerAll}
 * calls {@link #registerAll}.
 */
// Spec: camp_parts.md (Flag); PLAN-M8 Q12; WGEN-10, WGEN-11, WGEN-12
public final class CampScenarios {

    private static final long SEED = 0x6A6D5EL;
    private static final int ROTATION = 1;
    private static final String PLAN_KEY = PlanKey.of("military_base", SizeClass.BIG, 777, -777);

    private CampScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new CampFlagInitScenario());
        BaseScenarios.registerAll(registry);
    }

    /** Counts what a camp does in the world: spawn calls through the sink, and every changed block for undo. */
    private static final class TrackingTarget implements WindowedSink.Target {

        private final World world;
        private final WorldTarget real;
        final Map<Long, Object[]> before = new LinkedHashMap<>();
        final List<int[]> tileAt = new ArrayList<>();
        int spawnCalls;

        TrackingTarget(World world) {
            this.world = world;
            this.real = new WorldTarget(world);
        }

        private static long key(int x, int y, int z) {
            return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        }

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            Long k = key(x, y, z);
            if (!before.containsKey(k)) {
                before.put(k, new Object[] { x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z) });
            }
            real.setBlock(x, y, z, block, meta);
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {
            tileAt.add(new int[] { x, y, z });
            real.setTile(x, y, z, tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            spawnCalls++;
            real.spawn(entity, x, y, z);
        }

        void restore() {
            List<Object[]> all = new ArrayList<>(before.values());
            for (int i = all.size() - 1; i >= 0; i--) {
                Object[] o = all.get(i);
                world.setBlock((Integer) o[0], (Integer) o[1], (Integer) o[2], (Block) o[3], (Integer) o[4], 2);
            }
        }
    }

    /** A port that only counts, so any spawn during placement shows up. */
    private static final class CountingPort implements CampSpawnPort {

        int spawns;
        int counts;

        @Override
        public int countNpcs(World world, List<String> names, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
            counts++;
            return 0;
        }

        @Override
        public boolean spawn(World world, String npcName, double x, double y, double z) {
            spawns++;
            return false;
        }

        @Override
        public void onFlagBroken(World world, int x, int y, int z) {}
    }

    /**
     * Places a fixed-seed camp and checks the flag, its saved cap and names, the state record and the absence of NPCs.
     */
    private static final class CampFlagInitScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.camp_flag_init";
        }

        @Override
        public String description() {
            return "A fixed-seed Military Base (placeholder parts, rotated a quarter turn) is stamped window by window;"
                + " exactly one Camp Flag tile exists, with the cap and NPC names set, recorded in StructureState,"
                + " and no NPC is spawned during placement (Q12). The area is restored afterwards.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            Block flagBlock = WorldBlockRegistration.campFlag();
            if (flagBlock == null) {
                return ScenarioResult.fail(name(), "needs the utility module (the Camp Flag block is not registered)");
            }
            CampPlan plan = CampPlan.generate(SEED);
            int ax = ctx.originX() - plan.sizeX() / 2;
            int az = ctx.originZ() - plan.sizeZ() / 2;
            PlacedCamp camp = plan.place(ax, ctx.originY(), az, ROTATION);
            int c0x = Math.floorDiv(camp.boundsMinX() - 8, 16);
            int c1x = Math.floorDiv(camp.boundsMaxX() - 8, 16);
            int c0z = Math.floorDiv(camp.boundsMinZ() - 8, 16);
            int c1z = Math.floorDiv(camp.boundsMaxZ() - 8, 16);
            for (int cx = c0x; cx <= c1x + 1; cx++) {
                for (int cz = c0z; cz <= c1z + 1; cz++) {
                    // Dev scenario only: load the test area first (worldgen itself never does).
                    world.getChunkProvider()
                        .loadChunk(cx, cz);
                }
            }

            TrackingTarget target = new TrackingTarget(world);
            CountingPort port = new CountingPort();
            CampSpawnPort oldPort = CampSpawnPorts.install(port);
            int entitiesBefore = world.loadedEntityList.size();
            StructureState state = new StructureState("gregguns_structures_camp_scenario");
            state.decideOnce(PLAN_KEY, () -> Decision.placed(ctx.originY()));
            CampPlacer placer = new CampPlacer(PaletteKeys.resolveForPack(s -> {}), CampPlaceholders.all());
            int flagsPlaced = 0;
            int blocks = 0;
            try {
                for (int cx = c0x; cx <= c1x; cx++) {
                    for (int cz = c0z; cz <= c1z; cz++) {
                        PopulateWindow window = new PopulateWindow(cx, cz);
                        WindowedSink sink = new ClippingSink(window, target);
                        blocks += placer.write(sink, camp, window, CampColor.WOODLAND, (t, x, y, z) -> {});
                        if (placer
                            .flagStep(camp, flagBlock, UtilityConfig.campFlagCapFactor, CampFlags.into(state, PLAN_KEY))
                            .run(sink, window)) {
                            flagsPlaced++;
                        }
                    }
                }

                FlagPos flag = camp.flagPos();
                out.check(blocks > 300, "the camp stamped blocks: " + blocks);
                out.checkEquals("flags placed across all windows", 1, flagsPlaced);
                out.check(world.getBlock(flag.x, flag.y, flag.z) == flagBlock, "the flag block stands at " + flag);

                int tiles = 0;
                for (Object o : world.loadedTileEntityList) {
                    if (o instanceof TileCampFlag) {
                        TileCampFlag t = (TileCampFlag) o;
                        if (t.isInvalid()) {
                            continue; // the block made its own tile; setTile replaced and invalidated it
                        }
                        if (t.xCoord >= ax && t.xCoord < ax + camp.footprintX()
                            && t.zCoord >= az
                            && t.zCoord < az + camp.footprintZ()) {
                            tiles++;
                        }
                    }
                }
                out.checkEquals("flag tiles in the footprint", 1, tiles);

                TileEntity te = world.getTileEntity(flag.x, flag.y, flag.z);
                out.check(te instanceof TileCampFlag, "the flag position holds a TileCampFlag");
                if (te instanceof TileCampFlag) {
                    TileCampFlag tile = (TileCampFlag) te;
                    out.check(tile.isInitialized(), "the flag is initialized");
                    out.checkEquals(
                        "cap = floor(((sx + sz) / 2) * factor)",
                        CampCap.cap(camp.footprintX(), camp.footprintZ(), UtilityConfig.campFlagCapFactor),
                        tile.maxNpcs());
                    out.checkEquals("npc names", Arrays.asList("ArmySoldier"), tile.npcNames());
                    out.checkEquals("spawn points", plan.spawnPoints().length / 3, tile.spawnPointCount());
                }

                Decision d = state.get(PLAN_KEY);
                out.check(
                    d != null && d.flags()
                        .contains(flag),
                    "StructureState recorded the flag at " + flag);
                out.checkEquals("sink spawn calls", 0, target.spawnCalls);
                out.checkEquals("port spawns during placement", 0, port.spawns);
                out.checkEquals("loaded entities before and after", entitiesBefore, world.loadedEntityList.size());
                out.check(
                    CampPlan.generate(SEED)
                        .place(ax, ctx.originY(), az, ROTATION)
                        .flagPos()
                        .equals(flag),
                    "the same seed gives the same flag");
            } finally {
                target.restore();
                CampSpawnPorts.install(oldPort);
            }
            out.check(world.getBlock(camp.flagPos().x, camp.flagPos().y, camp.flagPos().z) != flagBlock, "restored");
            out.note("camp " + plan.sizeX() + "x" + plan.sizeZ() + " rotation " + ROTATION + ", " + blocks + " blocks");
            return out.build();
        }
    }
}
