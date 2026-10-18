package com.gregguns.gun.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.item.gun.ItemGun;

/**
 * A reusable visual walkthrough for one gun, driven by the dev autotest signals (see {@code docs/testing/README.md},
 * "Multiplayer and visual autotest"): first person idle, firing and mid-reload, third person from behind and in front
 * (with the muzzle flash), the dropped item, the inventory slot and NEI's recipe view. It signals the first connected
 * player; the evidence is the client's screenshots, labelled {@code <prefix>-...}. Not part of {@code test all}
 * because it needs a connected client. A new gun adds one line to register its own walkthrough:
 * {@code registry.register(new GunVisualScenario("mp.visual.revolver", "rev", GunItems.revolver(), "ammo_9mm_bullet"))}.
 * Dev environment only, server side.
 */
public final class GunVisualScenario implements Scenario {

    private final String name;
    private final String prefix;
    private final ItemGun gun;
    private final String ammoItem;

    /**
     * @param name     the scenario name, for example {@code mp.visual.revolver}
     * @param prefix   the screenshot label prefix, for example {@code rev}
     * @param gun      the gun to hold
     * @param ammoItem registry name of the ammo item to hand out so a reload can run
     */
    public GunVisualScenario(String name, String prefix, ItemGun gun, String ammoItem) {
        this.name = name;
        this.prefix = prefix;
        this.gun = gun;
        this.ammoItem = ammoItem;
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "Walks the first connected player through screenshots of " + gun.contentName()
            + " (first and third person, dropped, inventory, NEI). Needs 1 client.";
    }

    @SuppressWarnings("unchecked")
    private static List<EntityPlayerMP> players() {
        List<EntityPlayerMP> list = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        list.sort(Comparator.comparing(p -> p.getCommandSenderName()));
        return list;
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        List<EntityPlayerMP> players = players();
        if (players.isEmpty()) {
            return ScenarioResult.fail(name, "needs a connected player");
        }
        EntityPlayerMP a = players.get(0);
        double ox = ctx.originX() + 0.5;
        double oy = ctx.originY();
        double oz = ctx.originZ() + 0.5;
        ctx.clearArea(6, 3);
        a.inventory.clearInventory(null, -1);
        a.setGameType(WorldSettings.GameType.SURVIVAL);
        a.setHealth(a.getMaxHealth());
        ItemStack stack = new ItemStack(gun);
        ItemGun.ensureTag(stack, gun.stats());
        a.inventory.mainInventory[0] = stack;
        a.inventory.mainInventory[1] = new ItemStack((Item) Item.itemRegistry.getObject("gregguns:" + ammoItem), 8);
        a.inventory.currentItem = 0;
        a.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(0));
        a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, -90.0f, 0.0f);
        a.inventoryContainer.detectAndSendChanges();
        String p = prefix;
        // A scoped gun gets a real zoom at the end: the server toggles the zoom state (what a right-click does) and
        // the client narrows the FOV, hides the model and draws the overlay.
        boolean scoped = gun.stats()
            .hasScope();
        int endTick = scoped ? 470 : 380;
        return ScenarioResult.pending(name, endTick + 320, t -> {
            switch (t) {
                case 10 -> signal(a, "view", "0");
                case 25 -> signal(a, "shot", p + "-first-person-idle");
                case 30 -> signal(a, "arm", "shot", p + "-first-person-fire", "1");
                case 32 -> signal(a, "fire", "1");
                // Late enough for the slowest fire interval (the rifle's 25 ticks plus the client's click lag).
                case 80 -> {
                    signal(a, "arm", "reload", p + "-first-person-reload", "22");
                    ServerGunHandler.onReloadKey(a);
                }
                case 130 -> signal(a, "view", "1");
                case 140 -> signal(a, "shot", p + "-third-person-back");
                case 142 -> signal(a, "arm", "shot", p + "-third-person-back-fire", "1");
                case 144 -> signal(a, "fire", "1");
                case 170 -> signal(a, "view", "2");
                case 180 -> signal(a, "shot", p + "-third-person-front");
                case 182 -> signal(a, "arm", "shot", p + "-third-person-front-fire", "1");
                case 184 -> signal(a, "fire", "1");
                case 210 -> signal(a, "view", "0");
                case 215 -> {
                    ItemStack dropped = new ItemStack(gun);
                    ItemGun.ensureTag(dropped, gun.stats());
                    EntityItem drop = new EntityItem(ctx.world(), ox + 1.0, oy + 0.3, oz, dropped);
                    drop.motionX = 0;
                    drop.motionY = 0;
                    drop.motionZ = 0;
                    drop.delayBeforeCanPickup = 32767;
                    ctx.spawn(drop);
                    a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, -90.0f, 55.0f);
                }
                case 235 -> signal(a, "shot", p + "-dropped");
                case 237 -> a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, -90.0f, 0.0f);
                case 245 -> signal(a, "gui", "inventory");
                case 260 -> signal(a, "shot", p + "-inventory");
                case 263 -> signal(a, "close");
                case 270 -> signal(a, "gui", "nei", gun.contentName());
                case 320 -> signal(a, "shot", p + "-nei");
                case 323 -> signal(a, "close");
                case 330 -> signal(a, "scope", "gregguns:textures/fx/testscope.png");
                case 345 -> signal(a, "shot", "scope-testscope");
                case 350 -> signal(a, "scope", "gregguns:textures/fx/techscope.png");
                case 365 -> signal(a, "shot", "scope-techscope");
                case 370 -> signal(a, "scope", "off");
                case 380 -> {
                    if (scoped) {
                        ServerGunHandler.onZoomToggle(a);
                    }
                }
                case 400 -> {
                    if (scoped) {
                        signal(a, "shot", p + "-scoped");
                    }
                }
                case 405 -> {
                    if (scoped) {
                        signal(a, "arm", "shot", p + "-scoped-fire", "1");
                    }
                }
                case 407 -> {
                    if (scoped) {
                        signal(a, "fire", "1");
                    }
                }
                case 440 -> {
                    if (scoped) {
                        ServerGunHandler.onZoomToggle(a);
                    }
                }
                case 455 -> {
                    if (scoped) {
                        signal(a, "shot", p + "-unscoped");
                    }
                }
                default -> {}
            }
            if (t < endTick) {
                return null;
            }
            return ScenarioResult.builder(name)
                .note("signals sent to " + a.getCommandSenderName() + "; check the client log and screenshots")
                .build();
        });
    }
}
