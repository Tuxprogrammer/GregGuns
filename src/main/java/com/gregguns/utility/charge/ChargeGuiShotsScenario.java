package com.gregguns.utility.charge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;

/**
 * {@code utility.charge_gui_shots}: places the charges the settings screen has to be checked on, then tells one
 * connected client (Alice by preference) to open each screen with a real right-click and photograph it. Cases: the
 * Advanced charge unarmed on a floor, armed, and a wall charge owned by someone else (read only); the TNT charge
 * unarmed and armed. It is a visual aid for the dev autotest hooks (see docs/testing/README.md), so it is not part of
 * {@code test all}; the evidence is the {@code charge-gui-*} screenshots. Dev environment only, server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-40 to GGS-09-44)
final class ChargeGuiShotsScenario implements Scenario {

    static final String NAME = "utility.charge_gui_shots";
    private static final String PLAYER = "Alice";
    private static final int STEP = 60;

    /** One screen to photograph. */
    private static final class Case {

        final String label;
        final ChargeType type;
        final int dx;
        final int orientation;
        final boolean mine;
        final boolean armed;

        Case(String label, ChargeType type, int dx, int orientation, boolean mine, boolean armed) {
            this.label = label;
            this.type = type;
            this.dx = dx;
            this.orientation = orientation;
            this.mine = mine;
            this.armed = armed;
        }
    }

    private static final Case[] CASES = { new Case("advanced-unarmed", ChargeType.ADVANCED, 0, 1, true, false),
        new Case("advanced-armed", ChargeType.ADVANCED, 8, 1, true, true),
        new Case("advanced-not-owner", ChargeType.ADVANCED, 16, 4, false, false),
        new Case("tnt-unarmed", ChargeType.TNT, 24, 1, true, false),
        new Case("tnt-armed", ChargeType.TNT, 32, 1, true, true), };

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Opens each charge settings screen (armed, unarmed, not owner) on one client and signals screenshots. Needs 1 client with the autotest hooks.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    public int areaRadius() {
        return 40;
    }

    @SuppressWarnings("unchecked")
    private static EntityPlayerMP pick() {
        List<EntityPlayerMP> all = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        all.sort(Comparator.comparing(p -> p.getCommandSenderName()));
        for (EntityPlayerMP p : all) {
            if (p.getCommandSenderName()
                .equalsIgnoreCase(PLAYER)) {
                return p;
            }
        }
        return all.isEmpty() ? null : all.get(0);
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        if (BlockExplosiveCharge.instance == null) {
            return ScenarioResult.fail(NAME, "needs the utility module");
        }
        EntityPlayerMP a = pick();
        if (a == null) {
            return ScenarioResult.fail(NAME, "needs a connected player (" + PLAYER + " by preference)");
        }
        ctx.clearArea(40, 4);
        a.setGameType(WorldSettings.GameType.CREATIVE);
        a.inventory.clearInventory(null, -1);
        UUID stranger = UUID.nameUUIDFromBytes("gg-charge-shots-stranger".getBytes());
        final int ox = ctx.originX();
        final int oy = ctx.originY();
        final int oz = ctx.originZ();
        TileExplosiveCharge[] tiles = new TileExplosiveCharge[CASES.length];
        for (int i = 0; i < CASES.length; i++) {
            Case c = CASES[i];
            if (c.orientation == 4) {
                ctx.setBlock(c.dx + 1, 0, 0, Blocks.stone, 0);
            }
            ctx.setBlock(c.dx, 0, 0, BlockExplosiveCharge.instance, c.type.meta());
            TileExplosiveCharge tile = (TileExplosiveCharge) ctx.world()
                .getTileEntity(ox + c.dx, oy, oz);
            tile.init(c.type, c.orientation, null);
            if (c.mine) {
                tile.setOwner(a.getUniqueID(), a.getCommandSenderName());
            } else {
                tile.setOwner(stranger, "Brenda");
            }
            tiles[i] = tile;
        }
        final int total = CASES.length * STEP;
        a.playerNetServerHandler.setPlayerLocation(ox + 0.5, oy, oz + 3.5, 0.0f, 0.0f);
        signal(a, "hud", "off");
        return ScenarioResult.pending(NAME, total + 200, t -> {
            int i = t / STEP;
            int step = t % STEP;
            if (i < CASES.length) {
                Case c = CASES[i];
                switch (step) {
                    case 1 -> {
                        a.playerNetServerHandler.setPlayerLocation(ox + c.dx + 0.5, oy, oz + 3.5, 0.0f, 0.0f);
                        if (c.armed) {
                            for (int k = 0; k < 5; k++) {
                                tiles[i].press(ChargeParams.BTN_FUSE_UP);
                            }
                            tiles[i].press(ChargeParams.BTN_ARM);
                        }
                    }
                    case 8 -> signal(
                        a,
                        "gui",
                        "press",
                        String.valueOf(ox + c.dx),
                        String.valueOf(oy),
                        String.valueOf(oz));
                    case 35 -> signal(a, "shot", "charge-gui-" + c.label);
                    case 45 -> signal(a, "close");
                    case 52 -> ctx.world()
                        .setBlockToAir(ox + c.dx, oy, oz);
                    default -> {}
                }
            }
            if (t < total) {
                return null;
            }
            signal(a, "hud", "on");
            return ScenarioResult.builder(NAME)
                .note("signals sent to " + a.getCommandSenderName() + "; read the charge-gui-* screenshots")
                .build();
        });
    }
}
