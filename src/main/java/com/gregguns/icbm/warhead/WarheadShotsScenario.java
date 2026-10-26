package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;

/**
 * {@code testing.icbm_warhead_shots}: a visual walkthrough of the warhead content for one connected client, driven by
 * the dev autotest signals. It photographs a row of every casing from fixed cameras (3/4 and side), the selection
 * outline, a warhead beside the player, the hotbar and inventory icons with and without the Shift tooltip, a lit
 * fuse, a T1 crater in stone and a gas cloud, the last two before and after. Labels are {@code icbm-*}. Not part of
 * {@code test all}: it needs a connected client. Dev environment only, server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Model and rendering)
final class WarheadShotsScenario implements Scenario {

    static final String NAME = "testing.icbm_warhead_shots";
    private static final double EYE = 1.62;
    /** Ticks between camera moves and shots; the software-rendered client is slow. */
    private static final int GAP = 100;

    /** One row slot: block offset, casing, explosive, fill. */
    private static final Object[][] ROW = { { -2, WarheadCasing.T1, "condensed", 5 },
        { 0, WarheadCasing.T2, "thermobaric", 20 }, { 2, WarheadCasing.T1, "chemical", 3 },
        { 4, WarheadCasing.T2, "contagious", 10 } };

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Photographs every registered warhead, its outline, icons, tooltip, a lit fuse, a T1 crater and a gas"
            + " cloud for a connected client. Needs 1 client; not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    private static void signal(EntityPlayerMP p, String command, String... args) {
        p.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    /** Puts the eye at (ex, ey, ez) looking at the target point. */
    private static void cam(EntityPlayerMP p, double ex, double ey, double ez, double tx, double ty, double tz) {
        p.capabilities.allowFlying = true;
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        ey = Math.max(ey, p.worldObj.getHeightValue((int) Math.floor(ex), (int) Math.floor(ez)) + EYE + 0.3);
        double dx = tx - ex;
        double dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty - ey, Math.hypot(dx, dz)));
        p.playerNetServerHandler.setPlayerLocation(ex, ey - EYE, ez, yaw, pitch);
    }

    private static void fillStone(World w, int cx, int cy, int cz, int half, int lowY, int highY) {
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                for (int y = lowY; y <= highY; y++) {
                    w.setBlock(x, y, z, Blocks.stone, 0, 2);
                }
            }
        }
    }

    private static void clearBox(World w, int cx, int cy, int cz, int half, int lowY, int highY) {
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                for (int y = lowY; y <= highY; y++) {
                    w.setBlock(x, y, z, Blocks.air, 0, 2);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ScenarioResult run(ScenarioContext ctx) {
        if (WarheadBlocks.of(WarheadCasing.T1) == null) {
            return ScenarioResult.fail(NAME, "the ICBM content is not registered");
        }
        List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        if (players.isEmpty()) {
            return ScenarioResult.fail(NAME, "needs a connected player");
        }
        EntityPlayerMP p = players.get(0);
        World w = ctx.world();
        int ox = ctx.originX();
        int oy = ctx.originY();
        int oz = ctx.originZ();
        ctx.clearArea(8, 6);
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.capabilities.allowFlying = true;
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        p.inventory.clearInventory(null, -1);
        p.clearActivePotions();
        p.setHealth(p.getMaxHealth());
        // Fixed noon for repeatable light.
        w.setWorldTime(6000L);
        w.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");

        List<TileWarhead> tiles = new ArrayList<>();
        for (Object[] r : ROW) {
            WarheadCasing c = (WarheadCasing) r[1];
            int dx = (Integer) r[0];
            ctx.setBlock(dx, 0, 0, WarheadBlocks.of(c), 0);
            TileEntity t = w.getTileEntity(ox + dx, oy, oz);
            if (t instanceof TileWarhead) {
                ItemStack s = WarheadStacks
                    .make(Item.getItemFromBlock(WarheadBlocks.of(c)), c, (String) r[2], (Integer) r[3]);
                ((TileWarhead) t).init(c, s.getTagCompound(), null);
                tiles.add((TileWarhead) t);
            }
        }
        for (int i = 0; i < ROW.length; i++) {
            WarheadCasing c = (WarheadCasing) ROW[i][1];
            p.inventory.setInventorySlotContents(
                i,
                WarheadStacks
                    .make(Item.getItemFromBlock(WarheadBlocks.of(c)), c, (String) ROW[i][2], (Integer) ROW[i][3]));
        }
        p.inventory.currentItem = 0;
        p.inventoryContainer.detectAndSendChanges();

        // Row centre and the T2 at dx 0 that gets the outline.
        final double cx = ox + 1.5;
        final double cy = oy + 0.5;
        final double cz = oz + 0.5;
        final int craterX = ox;
        final int craterZ = oz - 30;
        final int gasX = ox;
        final int gasZ = oz + 14;
        final int[] fillHalf = { 15 };
        final int[] airAfter = { -1 };

        List<int[]> times = new ArrayList<>();
        List<Runnable> acts = new ArrayList<>();
        int[] clock = { 20 };
        java.util.function.BiConsumer<Integer, Runnable> at = (dt, r) -> {
            clock[0] += dt;
            times.add(new int[] { clock[0] });
            acts.add(r);
        };
        at.accept(0, () -> signal(p, "hud", "off"));
        // 1. row, 3/4 and side
        at.accept(10, () -> cam(p, cx - 3.5, cy + 2.2, cz + 5, cx, cy, cz));
        at.accept(GAP, () -> signal(p, "shot", "icbm-row-34"));
        at.accept(GAP, () -> cam(p, cx, cy + 0.4, cz + 7.5, cx, cy, cz));
        at.accept(GAP, () -> signal(p, "shot", "icbm-row-side"));
        at.accept(GAP, () -> cam(p, cx + 3.5, cy + 1.5, cz - 5, cx, cy, cz));
        at.accept(GAP, () -> signal(p, "shot", "icbm-row-back34"));
        // 2. selection outline on the T2 (dx 0): crosshair on its centre
        at.accept(GAP, () -> {
            signal(p, "hud", "on");
            cam(p, ox - 1.2, cy + 0.9, oz + 2.6, ox + 0.5, cy, cz);
        });
        at.accept(GAP, () -> signal(p, "shot", "icbm-outline-34"));
        at.accept(GAP, () -> cam(p, ox + 0.5, cy + 0.2, oz + 2.6, ox + 0.5, cy, cz));
        at.accept(GAP, () -> signal(p, "shot", "icbm-outline-side"));
        at.accept(5, () -> signal(p, "hud", "off"));
        // player for scale: third person from behind, player beside the T2
        at.accept(GAP, () -> {
            signal(p, "view", "1");
            p.playerNetServerHandler.setPlayerLocation(ox + 1.0, oy, oz + 0.5, 180f, 15f);
        });
        at.accept(GAP, () -> signal(p, "shot", "icbm-scale"));
        at.accept(GAP / 2, () -> signal(p, "view", "0"));
        // 3. hotbar, hand, inventory, tooltips
        at.accept(10, () -> {
            signal(p, "hud", "on");
            cam(p, ox + 0.5, cy + 1.0, oz + 6, ox + 0.5, cy + 0.5, cz);
        });
        at.accept(GAP, () -> signal(p, "shot", "icbm-hotbar-hand"));
        at.accept(GAP / 2, () -> {
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            signal(p, "gui", "inventory");
        });
        at.accept(GAP, () -> signal(p, "shot", "icbm-inventory"));
        at.accept(10, () -> signal(p, "tip", "36"));
        at.accept(GAP, () -> signal(p, "shot", "icbm-tooltip-plain"));
        at.accept(10, () -> signal(p, "shift", "on"));
        at.accept(GAP, () -> signal(p, "shot", "icbm-tooltip-shift"));
        at.accept(10, () -> {
            signal(p, "shift", "off");
            signal(p, "tip", "37");
        });
        at.accept(GAP, () -> signal(p, "shot", "icbm-tooltip-plain-t2"));
        at.accept(10, () -> signal(p, "shift", "on"));
        at.accept(GAP, () -> signal(p, "shot", "icbm-tooltip-shift-t2"));
        at.accept(10, () -> {
            signal(p, "tip", "off");
            signal(p, "shift", "off");
            signal(p, "close");
            signal(p, "hud", "off");
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.capabilities.allowFlying = true;
            p.capabilities.isFlying = true;
            p.sendPlayerAbilities();
        });
        // 4. lit fuse on the last T2 (dx 4) and the T1 (dx -2)
        at.accept(10, () -> {
            for (TileWarhead t : tiles) {
                t.arm(100000, null);
            }
            cam(p, cx - 4, cy + 1.6, cz + 5, cx, cy, cz);
        });
        at.accept(GAP, () -> signal(p, "shot", "icbm-fuse-lit-34"));
        at.accept(GAP, () -> { cam(p, ox + 0.5 + 2.2, cy + 0.6, oz + 2.0, ox + 0.5, cy, cz); });
        at.accept(GAP, () -> signal(p, "shot", "icbm-fuse-lit-close"));
        // Disarm for later: nothing is detonated by the row. noDrop keeps cleanup quiet.
        at.accept(5, () -> {
            for (TileWarhead t : tiles) {
                t.setNoDrop();
            }
        });
        // 5a. crater
        at.accept(10, () -> {
            fillStone(w, craterX, oy, craterZ, fillHalf[0], Math.max(1, oy - 14), oy + 3);
            cam(p, craterX - 15, oy + 14, craterZ + 15, craterX + 0.5, oy + 1, craterZ + 0.5);
        });
        at.accept(GAP + 40, () -> signal(p, "shot", "icbm-crater-before"));
        at.accept(GAP / 2, () -> {
            ExplosiveDef d = Explosives.get("condensed");
            IcbmBlasts.detonate(w, craterX + 0.5, oy + 3.5, craterZ + 0.5, d, 5, null);
        });
        at.accept(GAP * 3, () -> {
            int air = 0;
            for (int x = craterX - 12; x <= craterX + 12; x++) {
                for (int z = craterZ - 12; z <= craterZ + 12; z++) {
                    for (int y = Math.max(1, oy - 14); y <= oy + 3; y++) {
                        if (w.isAirBlock(x, y, z)) {
                            air++;
                        }
                    }
                }
            }
            airAfter[0] = air;
            signal(p, "shot", "icbm-crater-after");
        });
        at.accept(GAP, () -> { cam(p, craterX + 0.5, oy + 24, craterZ + 1.5, craterX + 0.5, oy + 3, craterZ + 0.5); });
        at.accept(GAP, () -> signal(p, "shot", "icbm-crater-after-top"));
        // 5b. gas
        at.accept(10, () -> cam(p, gasX + 6, oy + 3, gasZ + 6, gasX + 0.5, oy + 2, gasZ + 0.5));
        at.accept(GAP + 40, () -> signal(p, "shot", "icbm-gas-before"));
        at.accept(GAP / 2, () -> {
            ExplosiveDef d = Explosives.get("chemical");
            IcbmBlasts.detonate(w, gasX + 0.5, oy + 1.0, gasZ + 0.5, d, 5, null);
        });
        at.accept(GAP, () -> signal(p, "shot", "icbm-gas-after-a"));
        at.accept(GAP, () -> signal(p, "shot", "icbm-gas-after-b"));
        final int end = clock[0] + 40;

        int[] next = { 0 };
        return ScenarioResult.pending(NAME, end + 400, t -> {
            while (next[0] < times.size() && times.get(next[0])[0] <= t) {
                acts.get(next[0]++)
                    .run();
            }
            if (t < end) {
                return null;
            }
            w.getGameRules()
                .setOrCreateGameRule("doDaylightCycle", "true");
            signal(p, "hud", "on");
            signal(p, "view", "0");
            clearBox(w, craterX, oy, craterZ, fillHalf[0] + 2, Math.max(1, oy - 14), oy + 5);
            // Stay in creative: the gas cloud may still hang about and a survival player would be poisoned.
            return ScenarioResult.builder(NAME)
                .note(
                    times.size() + " steps sent to "
                        + p.getCommandSenderName()
                        + "; air blocks in the crater box: "
                        + airAfter[0]
                        + "; read the icbm-*.png shots")
                .build();
        });
    }
}
