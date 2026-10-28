package com.gregguns.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Dev-only scenario for {@code /gregguns build} ({@link DevStructureBuilder}): the Ammunition Works, the Armory Gantry
 * and the Rampart each stand as a bare controller, get built, and must report a formed structure.
 */
public final class BuildScenarios {

    private static final int RADIUS = 24;
    private static final int HEIGHT = 12;
    /** Height of the controller above the origin; the floor is two lower. */
    private static final int CY = 3;
    /** Ticks to let GT initialize freshly placed tile entities before the structure check. */
    private static final int WARMUP_TICKS = 45;
    /** Further ticks in which a not-yet-formed machine is rechecked before the scenario fails. */
    private static final int RETRY_TICKS = 40;
    private static final int PHASE_TICKS = WARMUP_TICKS + RETRY_TICKS + 5;

    private static final int[] IDS = { GregGunsMTEIDs.AMMO_WORKS, GregGunsMTEIDs.ARMORY_GANTRY,
        GregGunsMTEIDs.RAMPART };
    private static final String[] NAMES = { "Ammunition Works", "Armory Gantry", "Rampart" };

    private BuildScenarios() {}

    /**
     * @param registry where to add the scenario
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new BuildMultiblocksScenario());
        registry.register(new InteriorShotsScenario());
        CtmCasingsScenario.register(registry);
    }

    /** The three multiblocks form after {@code build}. */
    private static final class BuildMultiblocksScenario implements Scenario {

        @Override
        public String name() {
            return "testing.build_multiblocks";
        }

        @Override
        public String description() {
            return "The dev build command (casings from the controller's hologram routine, then the required hatches) forms the Ammunition Works, the Armory Gantry and the Rampart, each from a bare controller; a stone block is not a machine.";
        }

        @Override
        public int areaRadius() {
            return RADIUS;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            for (int id : IDS) {
                if (id >= GregTechAPI.METATILEENTITIES.length || GregTechAPI.METATILEENTITIES[id] == null) {
                    return ScenarioResult
                        .fail(name(), "MTE " + id + " is not registered (needs GG_MODULES=guns,machines,utility)");
                }
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            DevStructureBuilder.Outcome none = DevStructureBuilder
                .build(ctx.world(), ctx.originX(), ctx.originY() - 1, ctx.originZ(), 0);
            out.check(!none.formed, "a floor block is not a machine: " + none);
            int[] phase = { 0 };
            int[] startTick = { -1 };
            return ScenarioResult.pending(name(), IDS.length * PHASE_TICKS + 100, ticks -> {
                int cx = ctx.originX();
                int cy = ctx.originY() + CY;
                int cz = ctx.originZ();
                if (startTick[0] < 0) {
                    ctx.clearArea(RADIUS, HEIGHT);
                    IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[IDS[phase[0]]];
                    ctx.setBlock(0, CY, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
                    TileEntity tile = ctx.world()
                        .getTileEntity(cx, cy, cz);
                    if (!(tile instanceof IGregTechTileEntity)) {
                        return ScenarioResult.fail(name(), "no GT tile entity for " + NAMES[phase[0]]);
                    }
                    ((IGregTechTileEntity) tile).setInitialValuesAsNBT(null, (short) IDS[phase[0]]);
                    DevStructureBuilder.Outcome built = DevStructureBuilder.build(ctx.world(), cx, cy, cz, 0);
                    startTick[0] = ticks;
                    out.note(NAMES[phase[0]] + " first attempt: " + built);
                    return null;
                }
                if (ticks - startTick[0] < WARMUP_TICKS) {
                    return null;
                }
                boolean formed = DevStructureBuilder.check(ctx.world(), cx, cy, cz);
                if (!formed && ticks - startTick[0] < WARMUP_TICKS + RETRY_TICKS) {
                    return null;
                }
                out.check(formed, "the " + NAMES[phase[0]] + " should be formed after build (mMachine)");
                phase[0]++;
                startTick[0] = -1;
                if (phase[0] < IDS.length) {
                    return null;
                }
                ctx.clearArea(RADIUS, HEIGHT);
                return out.build();
            });
        }
    }

    /**
     * Client shots of the lit interiors: builds a hall and a Gantry with {@code /gregguns build}'s routine, then moves
     * the first player outside a window and signals screenshots of the view through the glass. Dev only, by name.
     */
    private static final class InteriorShotsScenario implements Scenario {

        @Override
        public boolean includedInAll() {
            return false; // needs a connected client (client_shot.py)
        }

        @Override
        public String name() {
            return "testing.interior_shots";
        }

        @Override
        public String description() {
            return "Builds an Ammunition Works and an Armory Gantry with the dev build routine, then has the first connected client screenshot their lit interiors through the glass. Needs 1 client.";
        }

        @Override
        public int areaRadius() {
            return RADIUS;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<EntityPlayerMP> players = new ArrayList<>(
                MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList);
            if (players.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            EntityPlayerMP a = players.get(0);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int cx = ctx.originX();
            int cy = ctx.originY() + CY;
            int cz = ctx.originZ();
            return ScenarioResult.pending(name(), 700, t -> {
                switch (t) {
                    case 2 -> {
                        ctx.clearArea(RADIUS, HEIGHT);
                        signal(a, "hud", "off");
                        build(ctx, out, GregGunsMTEIDs.AMMO_WORKS, cx, cy, cz);
                    }
                    case 70 -> out.check(
                        DevStructureBuilder.build(ctx.world(), cx, cy, cz, 0).formed
                            || DevStructureBuilder.check(ctx.world(), cx, cy, cz),
                        "the Ammunition Works should be formed");
                    case 80 -> {
                        // Stand on a bottom slab (top at cy - 0.5) five blocks off the left wall: eyes at window
                        // height.
                        ctx.setBlock(-5, CY - 1, 4, Blocks.stone_slab, 0);
                        ctx.setBlock(-5, CY - 1, 1, Blocks.stone_slab, 0);
                        a.playerNetServerHandler.setPlayerLocation(cx - 4.5, cy - 0.5, cz + 4.5, -90.0f, -14.0f);
                    }
                    case 110 -> signal(a, "shot", "ammoworks-bay-through-glass");
                    case 112 -> a.playerNetServerHandler
                        .setPlayerLocation(cx - 4.5, cy - 0.5, cz + 1.5, -70.0f, -10.0f);
                    case 140 -> signal(a, "shot", "ammoworks-feeder-through-glass");
                    case 150 -> {
                        a.playerNetServerHandler.setPlayerLocation(cx + 0.5, cy + 3.0, cz - 3.5, 0.0f, 0.0f);
                        ctx.clearArea(RADIUS, HEIGHT);
                        build(ctx, out, GregGunsMTEIDs.ARMORY_GANTRY, cx, cy, cz);
                    }
                    case 220 -> out.check(
                        DevStructureBuilder.build(ctx.world(), cx, cy, cz, 0).formed
                            || DevStructureBuilder.check(ctx.world(), cx, cy, cz),
                        "the Armory Gantry should be formed");
                    case 230 -> {
                        ctx.setBlock(0, CY, -4, Blocks.stone, 0);
                        ctx.setBlock(-1, CY, -4, Blocks.stone, 0);
                        a.playerNetServerHandler.setPlayerLocation(cx + 0.5, cy + 1.0, cz - 3.5, 0.0f, 0.0f);
                    }
                    case 260 -> signal(a, "shot", "gantry-booth-through-glass");
                    case 262 -> a.playerNetServerHandler.setPlayerLocation(cx - 0.5, cy + 1.0, cz - 3.5, -15.0f, 0.0f);
                    case 290 -> signal(a, "shot", "gantry-angle-through-glass");
                    case 300 -> {
                        return out.build();
                    }
                    default -> {}
                }
                return null;
            });
        }

        private static void build(ScenarioContext ctx, ScenarioResult.Builder out, int id, int cx, int cy, int cz) {
            IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
            ctx.setBlock(0, CY, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
            TileEntity tile = ctx.world()
                .getTileEntity(cx, cy, cz);
            if (tile instanceof IGregTechTileEntity) {
                ((IGregTechTileEntity) tile).setInitialValuesAsNBT(null, (short) id);
                out.note("build: " + DevStructureBuilder.build(ctx.world(), cx, cy, cz, 0));
            }
        }

        private static void signal(EntityPlayerMP player, String command, String... args) {
            player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
        }
    }
}
