package com.gregguns.utility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.storage.TileGregChest;

/**
 * Visual walkthrough of the two chests for the dev autotest (see {@code docs/testing/README.md}): both chests side by
 * side, shot near and far, closed and with the lid open, two frames a few ticks apart each, so depth fighting between
 * coplanar faces shows up as a change between the two frames. Needs a connected client, so it is not part of
 * {@code test all}. Dev environment only, server side.
 */
public final class ChestLookScenario implements Scenario {

    private static final String NAME = "mp.look.chest";
    private static final int GAP = 6;
    private static final int SETTLE = 5;

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Shoots the Weapon and Reinforced Chests near and far, closed and open, two frames each. Needs 1 client.";
    }

    @SuppressWarnings("unchecked")
    private static EntityPlayerMP firstPlayer() {
        List<EntityPlayerMP> list = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        list.sort(Comparator.comparing(p -> p.getCommandSenderName()));
        return list.isEmpty() ? null : list.get(0);
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        EntityPlayerMP a = firstPlayer();
        if (a == null) {
            return ScenarioResult.fail(NAME, "needs a connected player");
        }
        ctx.clearArea(14, 3);
        ctx.setBlock(5, 0, -1, WorldBlockRegistration.weaponChest(), 3);
        ctx.setBlock(5, 0, 1, WorldBlockRegistration.reinforcedChest(), 3);
        a.setGameType(WorldSettings.GameType.CREATIVE);
        double ox = ctx.originX() + 0.5;
        double oy = ctx.originY();
        double oz = ctx.originZ() + 0.5;
        // Eye positions relative to the origin: near is 3 blocks from the chest, far 11 blocks at a glancing angle.
        double[][] spots = { { 2.5, 0.5 }, { -6.5, -5.5 } };
        String[] spotName = { "near", "far" };
        double[][] chests = { { 5.5, -0.5 }, { 5.5, 1.5 } };
        String[] chestName = { "weapon", "reinforced" };
        Map<Integer, Runnable> steps = new HashMap<>();
        int t = 10;
        steps.put(t, () -> signal(a, "view", "0"));
        for (int open = 0; open < 2; open++) {
            final int openNow = open;
            t += 2;
            steps.put(t, () -> {
                for (int dz : new int[] { -1, 1 }) {
                    TileEntity te = ctx.world()
                        .getTileEntity(ctx.originX() + 5, ctx.originY(), ctx.originZ() + dz);
                    if (te instanceof TileGregChest) {
                        if (openNow == 1) {
                            ((TileGregChest) te).openInventory();
                        }
                    }
                }
            });
            t += 40; // the lid takes about 10 client ticks to open fully
            for (int c = 0; c < chests.length; c++) {
                for (int s = 0; s < spots.length; s++) {
                    double px = ox + spots[s][0];
                    double pz = oz + spots[s][1];
                    double dx = ox + chests[c][0] - px;
                    double dz = oz + chests[c][1] - pz;
                    double dy = oy + 0.6 - (oy + 1.62);
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
                    String base = chestName[c] + "-" + spotName[s] + "-" + (openNow == 1 ? "open" : "closed");
                    int at = t;
                    steps.put(at, () -> a.playerNetServerHandler.setPlayerLocation(px, oy, pz, yaw, pitch));
                    steps.put(at + SETTLE, () -> signal(a, "shot", base + "-a"));
                    steps.put(at + SETTLE + GAP, () -> signal(a, "shot", base + "-b"));
                    t += SETTLE + GAP + GAP;
                }
            }
        }
        final int end = t + 20;
        return ScenarioResult.pending(NAME, end + 100, tick -> {
            Runnable r = steps.get(tick);
            if (r != null) {
                r.run();
            }
            if (tick < end) {
                return null;
            }
            return ScenarioResult.builder(NAME)
                .note("signals sent; check the client screenshots chest-*")
                .build();
        });
    }
}
