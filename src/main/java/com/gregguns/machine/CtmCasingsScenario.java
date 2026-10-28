package com.gregguns.machine;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Dev-only client shots of the connected casing textures (design record 0023), for a client that has Angelica's bundled
 * MCPatcherForge (see tools/scenarios/README.md, "Connected textures in the dev client"). The server places and
 * rotates;
 * the first connected client screenshots. Layouts, in blocks relative to the origin (floor at dy = -1):
 * <ul>
 * <li><b>wall</b>: Works Casing at z = 7, x -3..3, y 0..4, minus the corners (3,4) and (-3,0), with the Ammunition
 * Works
 * controller at (0,2); seen from z = 0.5, yaw 0.</li>
 * <li><b>plating</b>: z = 9, x -6..5, y 0..2, Bastion Plating Iron, Steel, Aluminum, Carbon (metas 7 to 10) in four
 * columns of three, so Iron touches Steel; seen from z = 0.5, yaw 0.</li>
 * <li><b>ceiling</b>: Works Casing at y = 6, x -3..3, z -2..2, minus (3,-2), (2,-2), (3,-1) and (-1,0); seen from the
 * floor
 * at (0.5, 0.5), straight up (yaw 0, pitch -90).</li>
 * <li><b>hall</b>: a whole Ammunition Works built by the dev build routine (hatches, controller, glass), seen from
 * outside.</li>
 * </ul>
 * tools/asset-gen/infographics/ctm_expect.py renders the same layouts through {@code ctmsel} for comparison. Each scene
 * checks that the blocks read back as placed, so the scenario cannot pass on an empty plot.
 */
public final class CtmCasingsScenario implements Scenario {

    private static final int RADIUS = 24;
    private static final int HEIGHT = 12;
    private static final int CY = 3;
    private static final String OWNER = "gregguns-test";

    private CtmCasingsScenario() {}

    /**
     * @param registry where to add the scenario
     */
    public static void register(ScenarioRegistry registry) {
        registry.register(new CtmCasingsScenario());
    }

    @Override
    public boolean includedInAll() {
        return false; // needs a connected client with Angelica (mp_check.py --angelica)
    }

    @Override
    public String name() {
        return "testing.ctm_casings";
    }

    @Override
    public String description() {
        return "Places a Works Casing wall with a controller, four Bastion Plating metas side by side, a casing ceiling and a built Ammunition Works, and has the first connected client screenshot each (connected textures need a client with Angelica). Needs 1 client.";
    }

    @Override
    public int areaRadius() {
        return RADIUS;
    }

    private static boolean wallCell(int x, int y) {
        return !(x == 3 && y == 4) && !(x == -3 && y == 0);
    }

    private static boolean ceilingCell(int x, int z) {
        return !(x == 3 && z == -2) && !(x == 2 && z == -2) && !(x == 3 && z == -1) && !(x == -1 && z == 0);
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
        if (GregGunsCasings.INSTANCE == null) {
            return ScenarioResult.fail(name(), "the casings block is not registered (needs GG_MODULES with machines)");
        }
        EntityPlayerMP a = players.get(0);
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        int ox = ctx.originX();
        int oy = ctx.originY();
        int oz = ctx.originZ();
        return ScenarioResult.pending(name(), 700, t -> {
            switch (t) {
                case 2 -> {
                    ctx.clearArea(RADIUS, HEIGHT);
                    signal(a, "hud", "off");
                    int placed = 0;
                    for (int x = -3; x <= 3; x++) {
                        for (int y = 0; y <= 4; y++) {
                            if (wallCell(x, y) && !(x == 0 && y == 2)) {
                                ctx.setBlock(x, y, 7, GregGunsCasings.INSTANCE, GregGunsCasings.WORKS_CASING);
                                placed++;
                            }
                        }
                    }
                    ctx.setBlock(0, 2, 7, GregTechAPI.sBlockMachines, controllerBase(GregGunsMTEIDs.AMMO_WORKS));
                    initController(ctx, ox, oy + 2, oz + 7, GregGunsMTEIDs.AMMO_WORKS);
                    out.check(placed == 32, "the wall holds 32 casings, placed " + placed);
                    out.check(
                        ctx.world()
                            .getBlock(ox + 1, oy + 2, oz + 7) == GregGunsCasings.INSTANCE,
                        "the wall casing reads back as a casing");
                    a.playerNetServerHandler.setPlayerLocation(ox + 0.5, oy, oz + 0.5, 0.0f, -4.0f);
                }
                case 90 -> signal(a, "shot", "ctm-wall");
                case 95 -> {
                    ctx.clearArea(RADIUS, HEIGHT);
                    for (int x = -6; x <= 5; x++) {
                        for (int y = 0; y <= 2; y++) {
                            ctx.setBlock(x, y, 9, GregGunsCasings.INSTANCE, GregGunsCasings.BASTION_IRON + (x + 6) / 3);
                        }
                    }
                    out.check(
                        ctx.world()
                            .getBlockMetadata(ox - 4, oy, oz + 9) == GregGunsCasings.BASTION_IRON
                            && ctx.world()
                                .getBlockMetadata(ox - 3, oy, oz + 9) == GregGunsCasings.BASTION_IRON + 1,
                        "Iron plating sits next to Steel plating at x = -4 | -3");
                    a.playerNetServerHandler.setPlayerLocation(ox + 0.5, oy, oz + 0.5, 0.0f, 0.0f);
                }
                case 185 -> signal(a, "shot", "ctm-plating");
                case 190 -> {
                    ctx.clearArea(RADIUS, HEIGHT);
                    int placed = 0;
                    for (int x = -3; x <= 3; x++) {
                        for (int z = -2; z <= 2; z++) {
                            if (ceilingCell(x, z)) {
                                ctx.setBlock(x, 6, z, GregGunsCasings.INSTANCE, GregGunsCasings.WORKS_CASING);
                                placed++;
                            }
                        }
                    }
                    out.check(placed == 31, "the ceiling holds 31 casings, placed " + placed);
                    a.playerNetServerHandler.setPlayerLocation(ox + 0.5, oy, oz + 0.5, 0.0f, -90.0f);
                }
                case 280 -> signal(a, "shot", "ctm-ceiling");
                case 285 -> {
                    ctx.clearArea(RADIUS, HEIGHT);
                    int cx = ox;
                    int cy = oy + CY;
                    int cz = oz;
                    IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.AMMO_WORKS];
                    ctx.setBlock(0, CY, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
                    initController(ctx, cx, cy, cz, GregGunsMTEIDs.AMMO_WORKS);
                    out.note("hall build: " + DevStructureBuilder.build(ctx.world(), cx, cy, cz, 0));
                    a.playerNetServerHandler.setPlayerLocation(cx + 0.5, oy, cz - 14.5, 0.0f, 0.0f);
                }
                case 360 -> out.check(
                    DevStructureBuilder.build(ctx.world(), ox, oy + CY, oz, 0).formed
                        || DevStructureBuilder.check(ctx.world(), ox, oy + CY, oz),
                    "the Ammunition Works should be formed");
                case 400 -> signal(a, "shot", "ctm-hall-front");
                case 405 -> a.playerNetServerHandler.setPlayerLocation(ox - 14.5, oy, oz + 0.5, -90.0f, 0.0f);
                case 450 -> signal(a, "shot", "ctm-hall-side");
                case 455 -> a.playerNetServerHandler.setPlayerLocation(ox + 14.5, oy, oz + 0.5, 90.0f, 0.0f);
                case 500 -> signal(a, "shot", "ctm-hall-other-side");
                case 520 -> {
                    signal(a, "hud", "on");
                    ctx.clearArea(RADIUS, HEIGHT);
                    return out.build();
                }
                default -> {}
            }
            return null;
        });
    }

    private static int controllerBase(int id) {
        return GregTechAPI.METATILEENTITIES[id].getTileEntityBaseType();
    }

    private static void initController(ScenarioContext ctx, int x, int y, int z, int id) {
        TileEntity tile = ctx.world()
            .getTileEntity(x, y, z);
        if (tile instanceof IGregTechTileEntity) {
            IGregTechTileEntity base = (IGregTechTileEntity) tile;
            base.setInitialValuesAsNBT(null, (short) id);
            // GT's break path looks the owner up (an unowned controller NPEs on removal), as DevStructureBuilder sets
            // it.
            base.setOwnerName(OWNER);
            base.setOwnerUuid(UUID.nameUUIDFromBytes(OWNER.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }
}
