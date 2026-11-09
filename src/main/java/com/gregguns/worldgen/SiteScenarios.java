package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.GregGuns;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.camp.CampSpawnPort;
import com.gregguns.utility.camp.CampSpawnPorts;
import com.gregguns.worldgen.camp.CampFootprint;
import com.gregguns.worldgen.camp.CampPlacer;
import com.gregguns.worldgen.camp.CampPlan;
import com.gregguns.worldgen.camp.PartLayouts;
import com.gregguns.worldgen.camp.PlacedCamp;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LootPending;

/**
 * The carve-and-found rule in the world (D351): every structure whose site rule is
 * {@link StructureCatalog.SiteRule#CARVE} is placed on built hilly ground (a slope falling east across the footprint, a
 * step down, and a wooded hill rising over its south edge) through the real populator, window by window. Every
 * footprint column (and the camp's road tails) stands on solid ground from the ground Y down to the natural ground, no
 * natural block the structure did not place is left above the ground Y, and the bank climbs no faster than one block
 * per column. One scenario per structure; each rebuilds its lot flat afterwards. {@code worldgen.site_slope_show}
 * builds every lot and leaves it standing for a client shot.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (Site preparation); D351
public final class SiteScenarios {

    /** Far from spawn and from every other worldgen scenario lot. */
    static final int AREA_X = -60000;
    static final int AREA_Z = 60000;
    /** Distance between the lots of the structures, on X. */
    static final int LOT_PITCH = 200;
    /** The recorded ground Y of every lot: the level its site is carved and filled to. */
    static final int REF = 76;
    /** Built terrain around the footprint, past the bank, so the bank and the road tails have ground to cut. */
    static final int PAD = 12;
    /** Bottom of the built terrain. */
    static final int BOTTOM = 40;
    /** The shape seed of the base (the showcase's). */
    static final long CAMP_SEED = ShowcaseScenarios.CAMP_SEED;

    private SiteScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        for (StructureId s : carved()) {
            registry.register(new SlopeSite(s));
        }
        registry.register(new SlopeSite(null));
    }

    /** Every structure the general rule carves, in catalog order. */
    static List<StructureId> carved() {
        List<StructureId> out = new ArrayList<>();
        for (StructureId s : StructureId.values()) {
            if (StructureCatalog.siteRule(s) == StructureCatalog.SiteRule.CARVE) out.add(s);
        }
        return out;
    }

    /** The forced plan of a lot, turned a quarter more per lot, with the apron the catalog gives it. */
    static StructurePlan plan(StructureId id, int lot, LayoutRegistry layouts) {
        int ax = AREA_X + lot * LOT_PITCH;
        int rot = lot & 3;
        if (id == StructureId.MILITARY_BASE) {
            return new StructurePlan(
                id.id(),
                StructureCatalog.sizeClass(id),
                0,
                4000 + lot,
                4000,
                ax,
                AREA_Z,
                rot,
                3,
                CampFootprint.of(CAMP_SEED),
                0x517E5EEDL + lot,
                CAMP_SEED,
                PlacedCamp.REACH);
        }
        Layout l = layouts.get(id.id());
        if (l == null) return null;
        return new StructurePlan(
            id.id(),
            StructureCatalog.sizeClass(id),
            0,
            4000 + lot,
            4000,
            ax,
            AREA_Z,
            rot,
            3,
            new Footprint(l.sizeX(), l.sizeY(), l.sizeZ()),
            0x517E5EEDL + lot,
            0L,
            SitePrep.BANK);
    }

    /**
     * The built ground height: falls 10 blocks west to east across the footprint (from 4 over the ground Y to 5
     * under), steps down 3 more two thirds of the way across, and a hill up to 8 high stands over the south edge.
     */
    static int natural(StructurePlan p, int x, int z) {
        int h = REF + 4 - Math.floorDiv((x - p.anchorX()) * 10, p.sizeX());
        if (x >= p.anchorX() + p.sizeX() * 2 / 3) h -= 3;
        double hx = p.anchorX() + p.sizeX() / 3.0;
        double hz = p.maxZ() - 2;
        double radius = Math.max(6, Math.min(p.sizeX(), p.sizeZ()) / 2.0);
        double r = Math.sqrt((x - hx) * (x - hx) + (z - hz) * (z - hz));
        h += (int) Math.max(0, 8 * (1 - r / radius));
        return h;
    }

    /** Tree trunks: two on the hill inside the footprint, one in the bank south of it. */
    static int[][] trees(StructurePlan p) {
        int hx = p.anchorX() + p.sizeX() / 3;
        int hz = p.maxZ() - 2;
        return new int[][] { { hx, hz - 1 }, { hx + 2, hz - 3 }, { hx, p.maxZ() + 2 } };
    }

    /** The blocks the built terrain is made of. */
    static boolean isNatural(Block b) {
        return b == Blocks.grass || b == Blocks.dirt || b == Blocks.stone || b == Blocks.leaves || b == Blocks.log;
    }

    /** Forwards to the world and remembers the last block written at each position. */
    private static final class Tracking implements WindowedSink.Target {

        private final WorldTarget real;
        final Map<Long, Block> last = new HashMap<>();

        Tracking(World world) {
            this.real = new WorldTarget(world);
        }

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            last.put(PlacementHarness.key(x, y, z), block);
            real.setBlock(x, y, z, block, meta);
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {
            real.setTile(x, y, z, tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            real.spawn(entity, x, y, z);
        }

        Block at(int x, int y, int z) {
            return last.get(PlacementHarness.key(x, y, z));
        }
    }

    private static final class SlopeSite implements Scenario {

        /** The structure, or null for the show scenario (every lot, left standing). */
        private final StructureId id;

        SlopeSite(StructureId id) {
            this.id = id;
        }

        @Override
        public String name() {
            return id == null ? "worldgen.site_slope_show" : "worldgen.site_slope_" + id.id();
        }

        @Override
        public String description() {
            return id == null
                ? "Visual check (not part of test all): builds the hilly lot of every carved structure from x " + AREA_X
                    + " z "
                    + AREA_Z
                    + " and leaves them standing; logs a SHOWCASE line per lot."
                : "The " + id.id()
                    + " on built hilly ground (slope, step down, wooded hill) through the real populator: solid ground"
                    + " under every footprint column down to the natural ground, no natural block over the ground Y"
                    + " but the structure's own, a one-to-one bank (D351). The lot is rebuilt flat after.";
        }

        @Override
        public boolean includedInAll() {
            return id != null;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            Block flag = WorldBlockRegistration.campFlag();
            PaletteKeys keys = PaletteKeys.resolveForPack(GregGuns.LOG::warn);
            List<String> ids = new ArrayList<>(StructureCatalog.idsToLoad(s -> true));
            ids.addAll(PartLayouts.FromRegistry.allIds());
            List<String> errors = new ArrayList<>();
            LayoutRegistry layouts = LayoutRegistry.load(
                new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
                ids,
                WorldgenModule.optionalLayoutIds(),
                keys,
                errors::add,
                s -> {});
            out.check(errors.isEmpty(), "layouts load: " + errors);
            StructurePopulator populator = new StructurePopulator(
                new StructureGrid(16, 32, 64, new EnumMap<>(SizeClass.class)),
                layouts,
                new Stamper(keys),
                Blocks.dirt,
                Blocks.air,
                StructurePopulator.CONFIG_RULES,
                flag == null ? null : new CampPlacer(keys, new PartLayouts.FromRegistry(layouts)),
                flag,
                UtilityConfig.campFlagCapFactor).withDeepFill(Blocks.stone);
            List<StructureId> lots = carved();
            // No camp flag may call for a helicopter while a lot is rebuilt.
            CampSpawnPort old = CampSpawnPorts.install(CampSpawnPort.NONE);
            try {
                for (int lot = 0; lot < lots.size(); lot++) {
                    StructureId s = lots.get(lot);
                    if (id != null && s != id) continue;
                    if (s == StructureId.MILITARY_BASE && flag == null) {
                        if (id != null) return ScenarioResult.fail(name(), "needs the utility module (no Camp Flag)");
                        continue;
                    }
                    StructurePlan plan = plan(s, lot, layouts);
                    if (plan == null) {
                        out.check(false, s.id() + ": no layout");
                        continue;
                    }
                    build(world, plan);
                    Tracking target = new Tracking(world);
                    long t0 = System.nanoTime();
                    int rejected = place(world, populator, plan, target);
                    long micros = (System.nanoTime() - t0) / 1000;
                    out.checkEquals(s.id() + ": writes outside their window", 0, rejected);
                    if (id == null) {
                        GregGuns.LOG.info(
                            "SHOWCASE id=site_{} box={},{},{},{},{},{} rot={}",
                            s.id(),
                            plan.anchorX(),
                            REF + 1,
                            plan.anchorZ(),
                            plan.maxX(),
                            REF + 1
                                + plan.footprint()
                                    .sizeY(),
                            plan.maxZ(),
                            plan.rotation());
                        continue;
                    }
                    try {
                        check(out, world, plan, target, layouts);
                        out.note(s.id() + " placed in " + micros + " us");
                    } finally {
                        flatten(world, plan);
                    }
                }
            } finally {
                CampSpawnPorts.install(old);
            }
            return out.build();
        }
    }

    /** Runs the plan through every window that visits it, the decision recorded up front at {@link #REF}. */
    private static int place(World world, StructurePopulator populator, StructurePlan plan, Tracking target) {
        StructureState state = new StructureState("gregguns_site_scenario_" + plan.id());
        // Recorded first, so the scenario tests the site, not which window the terrain check lands in.
        state.decideOnce(PlanKey.of(plan), () -> Decision.placed(REF));
        Set<Type> plains = Collections.singleton(Type.PLAINS);
        int rejected = 0;
        for (int cx = Math.floorDiv(plan.anchorX() - 24, 16); cx <= Math.floorDiv(plan.maxX() + 8, 16); cx++) {
            for (int cz = Math.floorDiv(plan.anchorZ() - 24, 16); cz <= Math.floorDiv(plan.maxZ() + 8, 16); cz++) {
                // As the real grid: only windows the plan's box and apron touch visit it.
                if (!plan.overlapsWindow(cx, cz)) continue;
                PopulateWindow window = new PopulateWindow(cx, cz);
                ClippingSink sink = new ClippingSink(window, target);
                populator.populatePlans(
                    Collections.singletonList(plan),
                    window,
                    sink,
                    new WorldColumnReader(world),
                    (x, z) -> plains,
                    state,
                    NO_MARKS);
                rejected += sink.rejectedWrites();
            }
        }
        return rejected;
    }

    /** The assertions, over every footprint, road and bank column. */
    private static void check(ScenarioResult.Builder out, World world, StructurePlan plan, Tracking target,
        LayoutRegistry layouts) {
        String id = plan.id();
        PlacedCamp camp = null;
        if (StructureId.MILITARY_BASE.id()
            .equals(id)) {
            CampPlan cp = CampPlan.generate(
                CAMP_SEED,
                plan.footprint()
                    .sizeX(),
                plan.footprint()
                    .sizeZ());
            camp = cp.place(plan.anchorX(), REF + 1, plan.anchorZ(), plan.rotation());
        }
        int holes = 0;
        int leftovers = 0;
        int steep = 0;
        int filled = 0;
        int cut = 0;
        String firstHole = "";
        String firstLeftover = "";
        String firstSteep = "";
        int b = SitePrep.BANK;
        for (int x = plan.anchorX() - b; x <= plan.maxX() + b; x++) {
            for (int z = plan.anchorZ() - b; z <= plan.maxZ() + b; z++) {
                int d = Math.max(
                    Math.max(plan.anchorX() - x, x - plan.maxX()),
                    Math.max(Math.max(plan.anchorZ() - z, z - plan.maxZ()), 0));
                int nat = natural(plan, x, z);
                if (d == 0 || (camp != null && camp.isRoad(x, z))) {
                    if (nat < REF) filled++;
                    if (nat > REF) cut++;
                    // Under the ground Y every cell is solid, unless the structure itself wrote air there (a room
                    // of a sunk bunker).
                    for (int y = Math.min(nat + 1, REF); y <= REF; y++) {
                        Material m = world.getBlock(x, y, z)
                            .getMaterial();
                        boolean open = (m == Material.air || m.isLiquid()) && target.at(x, y, z) != Blocks.air;
                        if (open && holes++ == 0) firstHole = x + "," + y + "," + z;
                    }
                    // Over it no natural block is left that the structure did not place itself.
                    for (int y = REF + 1; y <= REF + 40; y++) {
                        if (isNatural(world.getBlock(x, y, z)) && target.at(x, y, z) == null) {
                            if (leftovers++ == 0) firstLeftover = x + "," + y + "," + z;
                        }
                    }
                } else {
                    for (int y = REF + d + 1; y <= REF + 40; y++) {
                        if (isNatural(world.getBlock(x, y, z))) {
                            if (steep++ == 0) firstSteep = x + "," + y + "," + z + " at distance " + d;
                            break;
                        }
                    }
                }
            }
        }
        int area = plan.sizeX() * plan.sizeZ();
        out.check(filled >= area / 8, id + ": the slope puts footprint columns under the ground Y: " + filled);
        out.check(cut >= area / 8, id + ": the hill puts footprint columns over the ground Y: " + cut);
        out.checkEquals(id + ": air under the site, first " + firstHole, 0, holes);
        out.checkEquals(id + ": natural blocks over the site, first " + firstLeftover, 0, leftovers);
        out.checkEquals(id + ": bank columns over ground Y + distance, first " + firstSteep, 0, steep);
        if (camp != null) {
            FlagPos f = camp.flagPos();
            out.check(world.getBlock(f.x, f.y, f.z) == WorldBlockRegistration.campFlag(), "the flag stands at " + f);
            out.check(
                world.getBlock(f.x, f.y - 1, f.z)
                    .getMaterial()
                    .isSolid(),
                "the flag pad is solid");
        }
        Layout l = layouts.get(id);
        if (l != null && l.ground() > 1) {
            // A buried layout stands on its own floor: the cell under its lowest layer is solid in every column.
            int floor = REF + 1 - l.ground();
            int open = 0;
            for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                    if (!world.getBlock(x, floor - 1, z)
                        .getMaterial()
                        .isSolid()) open++;
                }
            }
            out.checkEquals(id + ": open cells under the buried floor", 0, open);
        }
        out.note(
            id + " "
                + plan.sizeX()
                + "x"
                + plan.sizeZ()
                + " rot "
                + plan.rotation()
                + ": "
                + filled
                + " columns filled, "
                + cut
                + " cut");
    }

    private static final StructurePopulator.Marks NO_MARKS = new StructurePopulator.Marks() {

        @Override
        public void chest(StructurePlan plan, String kind, int x, int y, int z, LootPending order) {}

        @Override
        public void spawner(StructurePlan plan, String tag, int x, int y, int z) {}

        @Override
        public void flag(StructurePlan plan, String planKey, int x, int y, int z) {}
    };

    // ---- terrain ------------------------------------------------------------------------------------------------

    /** Loads the lot and builds its hilly ground: grass, 3 dirt, stone down to {@link #BOTTOM}; trees on the hill. */
    static void build(World world, StructurePlan p) {
        load(world, p);
        for (int x = p.anchorX() - PAD; x <= p.maxX() + PAD; x++) {
            for (int z = p.anchorZ() - PAD; z <= p.maxZ() + PAD; z++) {
                column(world, x, z, natural(p, x, z));
            }
        }
        for (int[] t : trees(p)) {
            int h = natural(p, t[0], t[1]);
            for (int y = h + 1; y <= h + 5; y++) put(world, t[0], y, t[1], Blocks.log);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = h + 4; y <= h + 6; y++) {
                        if (dx != 0 || dz != 0 || y > h + 5) put(world, t[0] + dx, y, t[1] + dz, Blocks.leaves);
                    }
                }
            }
        }
    }

    /** Rebuilds the lot flat at the ground Y and sweeps item entities: a scenario's cleanup. */
    static void flatten(World world, StructurePlan p) {
        for (int x = p.anchorX() - PAD; x <= p.maxX() + PAD; x++) {
            for (int z = p.anchorZ() - PAD; z <= p.maxZ() + PAD; z++) {
                column(world, x, z, REF);
            }
        }
        AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(
            p.anchorX() - PAD - 8,
            0,
            p.anchorZ() - PAD - 8,
            p.maxX() + PAD + 9,
            256,
            p.maxZ() + PAD + 9);
        for (Object o : world.getEntitiesWithinAABB(EntityItem.class, bb)) {
            ((Entity) o).setDead();
        }
    }

    private static void load(World world, StructurePlan p) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int cx = Math.floorDiv(p.anchorX() - PAD, 16) - 1; cx <= Math.floorDiv(p.maxX() + PAD, 16) + 1; cx++) {
            for (int cz = Math.floorDiv(p.anchorZ() - PAD, 16) - 1; cz <= Math.floorDiv(p.maxZ() + PAD, 16) + 1; cz++) {
                // Dev scenario only: the test lot is loaded first (worldgen itself never does).
                cps.loadChunk(cx, cz);
            }
        }
    }

    private static void column(World world, int x, int z, int h) {
        int top = Math.min(
            255,
            world.getChunkFromBlockCoords(x, z)
                .getTopFilledSegment() + 15);
        for (int y = Math.max(top, h + 20); y > h; y--) put(world, x, y, z, Blocks.air);
        put(world, x, h, z, Blocks.grass);
        for (int y = h - 1; y >= h - 3; y--) put(world, x, y, z, Blocks.dirt);
        for (int y = h - 4; y >= BOTTOM; y--) put(world, x, y, z, Blocks.stone);
    }

    private static void put(World world, int x, int y, int z, Block b) {
        if (world.getBlock(x, y, z) != b || world.getBlockMetadata(x, y, z) != 0) world.setBlock(x, y, z, b, 0, 2);
    }
}
