package com.gregguns.gun.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;

/**
 * One generated visual scenario per gun in {@link GunDefinitions#ALL}, named {@code mp.look.<gunId>}, plus the group
 * {@code mp.look.all} that runs them back to back in one session. Each gives the gun to the first connected player
 * and signals screenshots named {@code look-<id>-<pose>}: {@code first-person-idle}, {@code first-person-fire} (one
 * shot, then a screenshot), {@code first-person-aim} (only when the gun has a scope) and {@code third-person-front}.
 * Akimbo guns and guns that still render as flat icons are included automatically. None is part of {@code test all}
 * because they need a connected client. Dev environment only, server side.
 */
public final class GeneratedGunLooks {

    /** Ticks one gun's walkthrough takes, including settling time before the next gun. */
    static final int SEGMENT_TICKS = 140;

    private GeneratedGunLooks() {}

    /**
     * @param def a gun
     * @return the scenario name for the gun, {@code mp.look.<gunId>}
     */
    public static String scenarioName(GunDefinition def) {
        return "mp.look." + def.name();
    }

    /**
     * Registers one scenario per gun in the shared definition list, and {@code mp.look.all}.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        for (GunDefinition def : GunDefinitions.ALL) {
            registry.register(new One(def));
        }
        registry.register(new Group());
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

    /** Clears the player, hands over the gun and a stack of its ammo, and faces the camera spot. */
    private static void setup(ScenarioContext ctx, EntityPlayerMP a, GunDefinition def) {
        ItemGun gun = GunItems.get(def.name());
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
        Item ammo = (Item) Item.itemRegistry.getObject(
            "gregguns:" + gun.stats()
                .ammoType());
        if (ammo != null) {
            a.inventory.mainInventory[1] = new ItemStack(ammo, 8);
        }
        a.inventory.currentItem = 0;
        a.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(0));
        a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, -90.0f, 0.0f);
        a.inventoryContainer.detectAndSendChanges();
    }

    /** One gun's timeline step at local tick {@code t} (0 to {@link #SEGMENT_TICKS}). */
    private static void step(int t, EntityPlayerMP a, GunDefinition def) {
        String p = "look-" + def.name();
        boolean aims = def.stats()
            .hasScope();
        switch (t) {
            case 10 -> signal(a, "view", "0");
            case 25 -> signal(a, "shot", p + "-first-person-idle");
            case 30 -> signal(a, "arm", "shot", p + "-first-person-fire", "1");
            case 32 -> signal(a, "fire", "1");
            case 70 -> {
                if (aims) {
                    ServerGunHandler.onZoomToggle(a);
                }
            }
            case 90 -> {
                if (aims) {
                    signal(a, "shot", p + "-first-person-aim");
                }
            }
            case 95 -> {
                if (aims) {
                    ServerGunHandler.onZoomToggle(a);
                }
            }
            case 105 -> signal(a, "view", "2");
            case 115 -> signal(a, "shot", p + "-third-person-front");
            case 125 -> signal(a, "view", "0");
            default -> {}
        }
    }

    private static final class One implements Scenario {

        private final GunDefinition def;

        One(GunDefinition def) {
            this.def = def;
        }

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public String name() {
            return scenarioName(def);
        }

        @Override
        public String description() {
            return "Screenshots " + def.name()
                + ": first person idle, firing, aiming (if scoped), third person front. Needs 1 client.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<EntityPlayerMP> players = players();
            if (players.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            EntityPlayerMP a = players.get(0);
            setup(ctx, a, def);
            return ScenarioResult.pending(name(), SEGMENT_TICKS + 320, t -> {
                step(t, a, def);
                if (t < SEGMENT_TICKS) {
                    return null;
                }
                return ScenarioResult.builder(name())
                    .note("signals sent to " + a.getCommandSenderName() + "; check the client log and screenshots")
                    .build();
            });
        }
    }

    private static final class Group implements Scenario {

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public String name() {
            return "mp.look.all";
        }

        @Override
        public String description() {
            return "Runs every generated per-gun visual scenario in sequence in one session. Needs 1 client.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<EntityPlayerMP> players = players();
            if (players.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            EntityPlayerMP a = players.get(0);
            List<GunDefinition> defs = GunDefinitions.ALL;
            int total = defs.size() * SEGMENT_TICKS;
            int[] started = { -1 };
            return ScenarioResult.pending(name(), total + 320, t -> {
                if (t >= total) {
                    return ScenarioResult.builder(name())
                        .note(defs.size() + " guns signaled to " + a.getCommandSenderName())
                        .build();
                }
                int index = t / SEGMENT_TICKS;
                if (started[0] != index) {
                    started[0] = index;
                    setup(ctx, a, defs.get(index));
                }
                step(t % SEGMENT_TICKS, a, defs.get(index));
                return null;
            });
        }
    }
}
