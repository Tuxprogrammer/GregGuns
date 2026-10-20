package com.gregguns.utility.charge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * {@code mp.look.charge}: screenshots of both charges as items (first person, third person front, inventory, dropped)
 * and placed on a wall and on the floor, for judging how the item renders against the placed block. Needs one connected
 * client; not part of {@code test all}. Dev environment only, server side.
 */
final class ChargeLookScenario implements Scenario {

    static final String NAME = "mp.look.charge";
    private static final int TICKS = 460;

    static void register(ScenarioRegistry registry) {
        registry.register(new ChargeLookScenario());
    }

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
        return "Screenshots both charges in hand, third person, inventory, dropped and placed. Needs 1 client.";
    }

    @SuppressWarnings("unchecked")
    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        players.sort(Comparator.comparing(p -> p.getCommandSenderName()));
        if (players.isEmpty() || BlockExplosiveCharge.instance == null) {
            return ScenarioResult.fail(name(), "needs a connected player and the charge block");
        }
        EntityPlayerMP a = players.get(0);
        double ox = ctx.originX() + 0.5;
        double oy = ctx.originY();
        double oz = ctx.originZ() + 0.5;
        ctx.clearArea(8, 4);
        ctx.world()
            .setWorldTime(6000L);
        a.inventory.clearInventory(null, -1);
        a.setGameType(WorldSettings.GameType.SURVIVAL);
        Item item = Item.getItemFromBlock(BlockExplosiveCharge.instance);
        a.inventory.mainInventory[0] = new ItemStack(item, 1, ChargeType.TNT.meta());
        a.inventory.mainInventory[1] = new ItemStack(item, 1, ChargeType.ADVANCED.meta());
        select(a, 0);
        a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, -90.0f, 10.0f);
        a.inventoryContainer.detectAndSendChanges();
        return ScenarioResult.pending(name(), TICKS + 400, t -> {
            step(ctx, a, t, ox, oy, oz);
            if (t < TICKS) {
                return null;
            }
            return ScenarioResult.builder(name())
                .note("signals sent to " + a.getCommandSenderName())
                .build();
        });
    }

    private static void select(EntityPlayerMP a, int slot) {
        a.inventory.currentItem = slot;
        a.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(slot));
    }

    private static void signal(EntityPlayerMP p, String command, String... args) {
        p.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    private static void place(ScenarioContext ctx, ChargeType type, int orientation, int dx, int dy, int dz) {
        ctx.setBlock(dx, dy, dz, BlockExplosiveCharge.instance, type.meta());
        TileExplosiveCharge tile = (TileExplosiveCharge) ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        tile.init(type, orientation, null);
        ctx.world()
            .markBlockForUpdate(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
    }

    private static void drop(ScenarioContext ctx, ChargeType type, double x, double y, double z) {
        EntityItem e = new EntityItem(
            ctx.world(),
            x,
            y,
            z,
            new ItemStack(Item.getItemFromBlock(BlockExplosiveCharge.instance), 1, type.meta()));
        e.motionX = 0;
        e.motionY = 0;
        e.motionZ = 0;
        e.delayBeforeCanPickup = 32767;
        ctx.spawn(e);
    }

    private static void step(ScenarioContext ctx, EntityPlayerMP a, int t, double ox, double oy, double oz) {
        switch (t) {
            case 20 -> signal(a, "view", "0");
            case 60 -> signal(a, "shot", "charge-tnt-hand-first-person");
            case 70 -> select(a, 1);
            case 110 -> signal(a, "shot", "charge-adv-hand-first-person");
            case 120 -> {
                select(a, 0);
                signal(a, "view", "2");
            }
            case 170 -> signal(a, "shot", "charge-tnt-hand-third-person-front");
            case 180 -> select(a, 1);
            case 220 -> signal(a, "shot", "charge-adv-hand-third-person-front");
            case 230 -> {
                signal(a, "view", "0");
                signal(a, "gui", "inventory");
            }
            case 280 -> signal(a, "shot", "charge-inventory");
            case 290 -> signal(a, "close");
            case 300 -> {
                // Placed references: stone wall 4 east, two charges on its west face, two on the floor.
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        ctx.setBlock(4, dy, dz, Blocks.stone, 0);
                    }
                }
                place(ctx, ChargeType.TNT, 4, 3, 1, -1);
                place(ctx, ChargeType.ADVANCED, 4, 3, 1, 1);
                place(ctx, ChargeType.TNT, 1, 2, 0, -1);
                place(ctx, ChargeType.ADVANCED, 1, 2, 0, 1);
                a.playerNetServerHandler.setPlayerLocation(ox - 1.5, oy, oz, -90.0f, 20.0f);
            }
            case 360 -> signal(a, "shot", "charge-placed-wall-and-floor");
            case 380 -> {
                a.playerNetServerHandler.setPlayerLocation(ox - 1.5, oy, oz, 90.0f, 35.0f);
                drop(ctx, ChargeType.TNT, ox - 3.5, oy + 0.3, oz - 0.6);
                drop(ctx, ChargeType.ADVANCED, ox - 3.5, oy + 0.3, oz + 0.6);
            }
            case 450 -> signal(a, "shot", "charge-dropped");
            default -> {}
        }
    }
}
