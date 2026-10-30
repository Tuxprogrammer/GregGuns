package com.gregguns.worldgen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;

import cpw.mods.fml.common.Loader;

import com.gregguns.GregGuns;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.worldgen.camp.CampFootprint;
import com.gregguns.worldgen.camp.CampPlacer;
import com.gregguns.worldgen.camp.CampPlan;
import com.gregguns.worldgen.camp.PartLayouts;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.ResolvedBlock;
import com.gregguns.worldgen.layout.Stamper;

/**
 * Palette review tools (dev and test hooks only, never part of {@code test all}): {@code worldgen.palette_showcase}
 * builds every surface structure, rotation 0, on its own flat lot far from spawn through the real populator and the
 * pack's resolved palette keys, and leaves it standing; {@code worldgen.palette_showcase_cam} moves players and sends
 * client signals from a file, so a script can frame the same shots before and after a palette change.
 *
 * <p>
 * Each placed structure logs one {@code SHOWCASE id=... box=x0,y0,z0,x1,y1,z1} line (world box, y0 the anchor layer)
 * that the script turns into cameras. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/chisel-palette.md (Palette review)
public final class ShowcaseScenarios {

    /** West edge of the first lot; far from spawn and from the placement scenarios' areas. */
    static final int LOT_X = 60000;
    static final int LOT_Z = 60000;
    /** Lot pitch along X: wide enough that a neighbor never shares a frame. */
    static final int LOT_PITCH = 256;
    /** Flat ground prepared around each footprint. */
    static final int PAD = 40;
    /** The fixed shape seed of the showcased Military Base. */
    static final long CAMP_SEED = 0x5EEDCA4FL;

    private ShowcaseScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Showcase());
        registry.register(new Cam());
        registry.register(new Roles());
    }

    /** One lot: a structure, its water mode and, for the base, the camo biome. */
    private static final class Lot {

        final String label;
        final StructureId id;
        final boolean water;
        final boolean drain;
        final Type biome;

        Lot(String label, StructureId id, boolean water, boolean drain, Type biome) {
            this.label = label;
            this.id = id;
            this.water = water;
            this.drain = drain;
            this.biome = biome;
        }
    }

    static List<Lot> lots() {
        List<Lot> l = new ArrayList<>();
        l.add(new Lot("factory_small", StructureId.FACTORY_SMALL, false, false, Type.PLAINS));
        l.add(new Lot("train_station", StructureId.TRAIN_STATION, false, false, Type.PLAINS));
        l.add(new Lot("gas_station", StructureId.GAS_STATION, false, false, Type.PLAINS));
        l.add(new Lot("house_medium", StructureId.HOUSE_MEDIUM, false, false, Type.PLAINS));
        l.add(new Lot("big_bunker", StructureId.BIG_BUNKER, false, false, Type.PLAINS));
        l.add(new Lot("aircraft_carrier", StructureId.AIRCRAFT_CARRIER, true, false, Type.OCEAN));
        // The hull lies on the sea floor, out of sight from the surface: the basin is drained after it is placed.
        l.add(new Lot("submarine", StructureId.SUBMARINE, true, true, Type.OCEAN));
        l.add(new Lot("military_base", StructureId.MILITARY_BASE, false, false, Type.PLAINS));
        l.add(new Lot("military_base_desert", StructureId.MILITARY_BASE, false, false, Type.SANDY));
        l.add(new Lot("military_base_snow", StructureId.MILITARY_BASE, false, false, Type.SNOWY));
        return l;
    }

    private static final class Showcase implements Scenario {

        @Override
        public String name() {
            return "worldgen.palette_showcase";
        }

        @Override
        public String description() {
            return "Palette review (needs no client, not part of test all): builds every surface structure and three"
                + " Military Bases (woodland, desert, snow camo) on flat lots from x "
                + LOT_X
                + " z "
                + LOT_Z
                + " through the real populator and pack palette, leaves them standing and logs each box as a"
                + " SHOWCASE line.";
        }

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
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
            Block flag = WorldBlockRegistration.campFlag();
            StructurePopulator populator = new StructurePopulator(
                new StructureGrid(16, 32, 64, new EnumMap<>(SizeClass.class)),
                layouts,
                new Stamper(keys),
                Blocks.dirt,
                Blocks.air,
                StructurePopulator.CONFIG_RULES,
                flag == null ? null : new CampPlacer(keys, new PartLayouts.FromRegistry(layouts)),
                flag,
                UtilityConfig.campFlagCapFactor);
            List<Lot> lots = lots();
            for (int i = 0; i < lots.size(); i++) {
                Lot lot = lots.get(i);
                if (lot.id == StructureId.MILITARY_BASE && flag == null) {
                    out.note(lot.label + ": skipped, the utility module is off");
                    continue;
                }
                StructurePlan plan = plan(lot, i, layouts);
                if (plan == null) {
                    out.check(false, lot.label + ": no layout");
                    continue;
                }
                prepare(world, plan, lot.water);
                StructureState state = new StructureState("gregguns_showcase_" + lot.label);
                Set<Type> biome = Collections.singleton(lot.biome);
                for (int cx = Math.floorDiv(plan.anchorX() - 24, 16); cx <= Math.floorDiv(plan.maxX() + 8, 16); cx++) {
                    for (int cz = Math.floorDiv(plan.anchorZ() - 24, 16); cz
                        <= Math.floorDiv(plan.maxZ() + 8, 16); cz++) {
                        // As the real grid: only windows the plan's box (and apron) touches visit it.
                        if (!plan.overlapsWindow(cx, cz)) continue;
                        PopulateWindow window = new PopulateWindow(cx, cz);
                        ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                        populator.populatePlans(
                            Collections.singletonList(plan),
                            window,
                            sink,
                            new WorldColumnReader(world),
                            (x, z) -> biome,
                            state,
                            new StructureGenerator.WorldMarks(world, sink));
                    }
                }
                Decision d = state.get(PlanKey.of(plan));
                boolean placed = d != null && d.isPlaced();
                out.check(placed, lot.label + " placed: " + d);
                if (!placed) continue;
                int y0 = d.groundY() + 1;
                if (lot.drain) drain(world, plan);
                int h = plan.footprint()
                    .sizeY();
                GregGuns.LOG.info(
                    "SHOWCASE id={} box={},{},{},{},{},{} rot={}",
                    lot.label,
                    plan.anchorX(),
                    y0,
                    plan.anchorZ(),
                    plan.maxX(),
                    y0 + h,
                    plan.maxZ(),
                    plan.rotation());
            }
            return out.build();
        }
    }

    /** The forced plan of a lot: rotation 0, a fixed variant and seeds, anchored inside the lot. */
    private static StructurePlan plan(Lot lot, int index, LayoutRegistry layouts) {
        int ax = LOT_X + index * LOT_PITCH + PAD;
        int az = LOT_Z + PAD;
        if (lot.id == StructureId.MILITARY_BASE) {
            return new StructurePlan(
                lot.id.id(),
                StructureCatalog.sizeClass(lot.id),
                0,
                3000 + index,
                3000,
                ax,
                az,
                0,
                3,
                CampFootprint.of(CAMP_SEED),
                0x5A0CA5EL + index,
                CAMP_SEED,
                CampPlan.ROAD_TAIL);
        }
        Layout l = layouts.get(lot.id.id());
        if (l == null) return null;
        return new StructurePlan(
            lot.id.id(),
            StructureCatalog.sizeClass(lot.id),
            0,
            3000 + index,
            3000,
            ax,
            az,
            0,
            3,
            new Footprint(l.sizeX(), l.sizeY(), l.sizeZ()),
            0x5A0CA5EL + index);
    }

    /** Flat grass at {@link PlacementHarness#LAND_Y}, or a flooded basin, over the footprint plus {@link #PAD}. */
    private static void prepare(World world, StructurePlan p, boolean water) {
        int x0 = p.anchorX() - PAD, x1 = p.maxX() + PAD, z0 = p.anchorZ() - PAD, z1 = p.maxZ() + PAD;
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int cx = Math.floorDiv(x0, 16) - 1; cx <= Math.floorDiv(x1, 16) + 1; cx++) {
            for (int cz = Math.floorDiv(z0, 16) - 1; cz <= Math.floorDiv(z1, 16) + 1; cz++) {
                cps.loadChunk(cx, cz);
            }
        }
        int basin = 12;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int top = Math.min(
                    255,
                    world.getChunkFromBlockCoords(x, z)
                        .getTopFilledSegment() + 15);
                boolean wet = water && x >= p.anchorX() - basin
                    && x <= p.maxX() + basin
                    && z >= p.anchorZ() - basin
                    && z <= p.maxZ() + basin;
                int surface = wet ? PlacementHarness.WATER_SURFACE_Y : PlacementHarness.LAND_Y;
                for (int y = top; y > surface; y--) put(world, x, y, z, Blocks.air);
                if (wet) {
                    for (int y = PlacementHarness.WATER_SURFACE_Y; y > PlacementHarness.WATER_FLOOR_Y; y--) {
                        put(world, x, y, z, Blocks.water);
                    }
                    put(world, x, PlacementHarness.WATER_FLOOR_Y, z, Blocks.sand);
                    for (int y = PlacementHarness.WATER_FLOOR_Y - 1; y > PlacementHarness.WATER_FLOOR_Y - 4; y--) {
                        put(world, x, y, z, Blocks.stone);
                    }
                } else {
                    put(world, x, PlacementHarness.LAND_Y, z, Blocks.grass);
                    for (int y = PlacementHarness.LAND_Y - 1; y > PlacementHarness.LAND_Y - 4; y--) {
                        put(world, x, y, z, Blocks.dirt);
                    }
                }
            }
        }
    }

    /** Replaces the basin's water with air, so the hull on the floor can be seen. */
    private static void drain(World world, StructurePlan p) {
        int basin = 12;
        for (int x = p.anchorX() - basin; x <= p.maxX() + basin; x++) {
            for (int z = p.anchorZ() - basin; z <= p.maxZ() + basin; z++) {
                for (int y = PlacementHarness.WATER_SURFACE_Y; y > PlacementHarness.WATER_FLOOR_Y; y--) {
                    Block b = world.getBlock(x, y, z);
                    if (b == Blocks.water || b == Blocks.flowing_water) world.setBlock(x, y, z, Blocks.air, 0, 2);
                }
            }
        }
    }

    private static void put(World world, int x, int y, int z, Block b) {
        if (world.getBlock(x, y, z) != b || world.getBlockMetadata(x, y, z) != 0) world.setBlock(x, y, z, b, 0, 2);
    }

    /**
     * Runs the lines of {@code showcase-cam.txt} in the server directory, then deletes it: {@code cam <player> ex ey ez
     * tx ty tz} puts the player's eye at e looking at t (flying), {@code sig <player> <command> [args]} sends a client
     * autotest signal ({@code shot <label>}, {@code hud off}).
     */
    private static final class Cam implements Scenario {

        private static final File OPS = new File("showcase-cam.txt");
        private static final double EYE = 1.62;

        @Override
        public String name() {
            return "worldgen.palette_showcase_cam";
        }

        @Override
        public String description() {
            return "Palette review (needs a connected client, not part of test all): runs the cam and sig lines of"
                + " showcase-cam.txt in the server directory.";
        }

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<String> lines;
            try {
                if (!OPS.exists()) return ScenarioResult.fail(name(), "no showcase-cam.txt");
                lines = Files.readAllLines(OPS.toPath(), StandardCharsets.UTF_8);
                Files.delete(OPS.toPath());
            } catch (IOException e) {
                return ScenarioResult.fail(name(), e.toString());
            }
            for (String line : lines) {
                String[] t = line.trim()
                    .split("\\s+");
                if (t.length < 3) continue;
                EntityPlayerMP p = MinecraftServer.getServer()
                    .getConfigurationManager()
                    .func_152612_a(t[1]);
                if (p == null) return ScenarioResult.fail(name(), "no player " + t[1]);
                if (t[0].equals("cam") && t.length == 8) {
                    double[] v = new double[6];
                    for (int i = 0; i < 6; i++) v[i] = Double.parseDouble(t[2 + i]);
                    double dx = v[3] - v[0], dz = v[5] - v[2];
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float pitch = (float) -Math.toDegrees(Math.atan2(v[4] - v[1], Math.hypot(dx, dz)));
                    p.capabilities.allowFlying = true;
                    p.capabilities.isFlying = true;
                    p.sendPlayerAbilities();
                    p.playerNetServerHandler.setPlayerLocation(v[0], v[1] - EYE, v[2], yaw, pitch);
                } else if (t[0].equals("sig")) {
                    String[] rest = new String[t.length - 3];
                    System.arraycopy(t, 3, rest, 0, rest.length);
                    p.addChatMessage(new ChatComponentText(AutotestSignal.format(t[2], rest)));
                }
            }
            return ScenarioResult.builder(name())
                .build();
        }
    }

    /**
     * The roles resolve in this install: no warning, every role present, and with Chisel loaded every role whose first
     * candidate is a Chisel block got that block (so the pack shows the designed look, not a stand-in).
     */
    private static final class Roles implements Scenario {

        @Override
        public String name() {
            return "worldgen.palette_roles";
        }

        @Override
        public String description() {
            return "Resolves the structure palette roles as postInit does: no warning, every role present, and with"
                + " Chisel loaded every role whose first candidate is Chisel resolved to it (design record 0025).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            List<String> warnings = new ArrayList<>();
            PaletteKeys keys = PaletteKeys.resolveForPack(warnings::add);
            out.checkEquals("resolve warnings " + warnings, 0, warnings.size());
            boolean chisel = Loader.isModLoaded("chisel");
            int chiselRoles = 0;
            for (Map.Entry<String, List<String>> e : PaletteKeys.table()
                .entrySet()) {
                ResolvedBlock r = keys.get(e.getKey());
                out.check(r != null, "role " + e.getKey() + " resolved");
                if (r == null || !chisel) continue;
                String first = e.getValue()
                    .get(0);
                if (first.startsWith("chisel:")) {
                    chiselRoles++;
                    out.check(!r.fallback, "role " + e.getKey() + " uses " + first + " in this pack");
                }
            }
            out.note("Chisel loaded: " + chisel + ", Chisel roles checked: " + chiselRoles);
            return out.build();
        }
    }
}
