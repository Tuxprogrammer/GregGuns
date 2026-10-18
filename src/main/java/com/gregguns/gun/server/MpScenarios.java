package com.gregguns.gun.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import com.gregguns.core.testing.ScenarioPoller;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoItems;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.item.gun.PlayerAmmoStore;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.machine.MTEAmmoPress;
import com.gregguns.projectile.GenericProjectile;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Dev-only multiplayer scenarios that need real clients connected to the server (they report a failure on a console
 * with too few players). The server drives the clients by chat-borne {@link AutotestSignal}s, and shots and reloads go
 * through the same {@link ServerGunHandler} entry points the packets reach.
 *
 * <ul>
 * <li>{@code mp.sync}: two players face each other four blocks apart. Player A's client fires twice (the real trigger
 * packet), the server reloads A's pistol through {@link ServerGunHandler#onReloadKey}, and the scenario checks that a
 * bullet was launched, B took damage, and A's rounds and magazines changed. Player B's client logs and screenshots what
 * it sees.</li>
 * <li>{@code mp.visual}: walks player A through the visual checks (first and third person, dropped item, inventory,
 * creative tab, Ammo Press GUI, NEI recipes) by signalling screenshots.</li>
 * <li>{@code mp.give}: gives every connected player every registered GregGuns item (and the LV Ammo Press) one at a
 * time with the real {@code /give} command, selects it, and has the client hold it for a few ticks and build its
 * tooltip; then repeats through the client-side creative slot packet. Exceptions show up in the logs.</li>
 * </ul>
 *
 * <p>
 * Players are ordered by username so a run is repeatable: the first is A. Registered from {@code GunsModule} in the dev
 * environment only; see {@code docs/testing/README.md}. Server side.
 */
public final class MpScenarios {

    private static final String MAG = "mag_9mm_pistol";
    private static final double SEPARATION = 4.0;

    private MpScenarios() {}

    /**
     * Registers the multiplayer scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Sync());
        registry.register(new Visual());
        registry.register(new Give());
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

    /** Survival, full health, a full pistol in slot 0 and three loaded magazines, standing at the given place. */
    private static void prepare(EntityPlayerMP player, double x, double y, double z, float yaw, float pitch) {
        player.inventory.clearInventory(null, -1);
        player.setGameType(WorldSettings.GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        ItemStack pistol = new ItemStack(GunItems.pistol());
        ItemGun.ensureTag(
            pistol,
            GunItems.pistol()
                .stats());
        player.inventory.mainInventory[0] = pistol;
        player.inventory.mainInventory[1] = new ItemStack(AmmoItems.nineMmPistolMagazine(), 3);
        player.inventory.currentItem = 0;
        player.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(0));
        player.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
        player.inventoryContainer.detectAndSendChanges();
    }

    private static int loaded(EntityPlayerMP player) {
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemGun)) {
            return -1;
        }
        GunStats stats = ((ItemGun) held.getItem()).stats();
        return GunNbt.read(held.getTagCompound(), stats)
            .loaded();
    }

    private static int magazines(EntityPlayerMP player) {
        return new PlayerAmmoStore(player).count(MAG);
    }

    // ----- mp.sync ------------------------------------------------------------------------------------------

    private static final class Sync implements Scenario {

        @Override
        public String name() {
            return "mp.sync";
        }

        @Override
        public String description() {
            return "Two connected players face each other; A fires and reloads a pistol, B must see it. Needs 2 clients.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<EntityPlayerMP> players = players();
            if (players.size() < 2) {
                return ScenarioResult.fail(name(), "needs two connected players, found " + players.size());
            }
            EntityPlayerMP a = players.get(0);
            EntityPlayerMP b = players.get(1);
            double ox = ctx.originX() + 0.5;
            double oy = ctx.originY();
            double oz = ctx.originZ() + 0.5;
            ctx.clearArea(6, 3);
            // MC yaw: -90 faces +X, 90 faces -X.
            prepare(a, ox, oy, oz, -90.0f, 0.0f);
            prepare(b, ox + SEPARATION, oy, oz, 90.0f, 0.0f);
            return ScenarioResult.pending(name(), 600, new SyncPoller(ctx, a, b));
        }
    }

    private static final class SyncPoller implements ScenarioPoller {

        private final ScenarioContext ctx;
        private final EntityPlayerMP a;
        private final EntityPlayerMP b;
        private boolean bulletSeen;
        private float lowestBHealth;
        private int roundsStart = -1;
        private int roundsAfterShots = -1;
        private int magsBefore;
        private int reloadAt = -1;

        SyncPoller(ScenarioContext ctx, EntityPlayerMP a, EntityPlayerMP b) {
            this.ctx = ctx;
            this.a = a;
            this.b = b;
            this.lowestBHealth = b.getHealth();
        }

        @Override
        public ScenarioResult poll(int t) {
            for (Object o : ctx.world().loadedEntityList) {
                if (o instanceof GenericProjectile && ((GenericProjectile) o).getShooter() == a) {
                    bulletSeen = true;
                }
            }
            lowestBHealth = Math.min(lowestBHealth, b.getHealth());
            if (t == 40) {
                roundsStart = loaded(a);
                signal(b, "shot", "b-sees-a-idle");
                signal(b, "arm", "shot", "b-sees-a-shot", "1");
                signal(b, "arm", "reload", "b-sees-a-reload", "12");
            }
            if (t == 60) {
                signal(a, "fire", "2");
            }
            if (t > 60 && reloadAt < 0 && (loaded(a) <= roundsStart - 2 || t >= 140)) {
                // The fire cooldown refuses a reload for a few ticks after a shot, so keep pressing until it starts.
                if (roundsAfterShots < 0) {
                    roundsAfterShots = loaded(a);
                    magsBefore = magazines(a);
                }
                ServerGunHandler.onReloadKey(a);
                if (GunPlayerData.get(a)
                    .controller()
                    .reload()
                    .isReloading()) {
                    reloadAt = t;
                }
            }
            if (reloadAt >= 0 && t >= reloadAt + 60) {
                return finish(t);
            }
            if (t > 400) {
                return finish(t);
            }
            return null;
        }

        private ScenarioResult finish(int t) {
            ScenarioResult.Builder out = ScenarioResult.builder("mp.sync");
            out.check(bulletSeen, "no bullet fired by " + a.getCommandSenderName() + " was seen in the world");
            out.check(
                roundsAfterShots <= roundsStart - 2,
                "A's rounds after firing twice: " + roundsStart + " -> " + roundsAfterShots);
            out.check(
                lowestBHealth < b.getMaxHealth(),
                "B took no damage (health " + lowestBHealth + " / " + b.getMaxHealth() + ")");
            out.check(
                loaded(a) > roundsAfterShots,
                "A's reload did not add rounds: " + roundsAfterShots + " -> " + loaded(a));
            out.check(
                magazines(a) < magsBefore,
                "A's reload did not use a magazine: " + magsBefore + " -> " + magazines(a));
            out.note(
                "A=" + a.getCommandSenderName()
                    + " B="
                    + b.getCommandSenderName()
                    + " rounds "
                    + roundsStart
                    + " -> "
                    + roundsAfterShots
                    + " -> "
                    + loaded(a)
                    + ", B lowest health "
                    + lowestBHealth
                    + ", mags "
                    + magsBefore
                    + " -> "
                    + magazines(a)
                    + ", finished at tick "
                    + t);
            return out.build();
        }
    }

    // ----- mp.visual ----------------------------------------------------------------------------------------

    private static final class Visual implements Scenario {

        @Override
        public String name() {
            return "mp.visual";
        }

        @Override
        public String description() {
            return "Walks the first connected player through the visual checks, signalling screenshots. Needs 1 client.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<EntityPlayerMP> players = players();
            if (players.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            EntityPlayerMP a = players.get(0);
            double ox = ctx.originX() + 0.5;
            double oy = ctx.originY();
            double oz = ctx.originZ() + 0.5;
            ctx.clearArea(6, 3);
            prepare(a, ox, oy, oz, -90.0f, 0.0f);
            int pressId = GregGunsMTEIDs.AMMO_PRESS_LV;
            boolean pressPlaced = false;
            Object prototype = GregTechAPI.METATILEENTITIES[pressId];
            if (prototype instanceof MTEAmmoPress) {
                ctx.setBlock(3, 0, 0, GregTechAPI.sBlockMachines, ((MTEAmmoPress) prototype).getTileEntityBaseType());
                Object tile = ctx.world()
                    .getTileEntity(ctx.originX() + 3, ctx.originY(), ctx.originZ());
                if (tile instanceof IGregTechTileEntity) {
                    ((IGregTechTileEntity) tile).setInitialValuesAsNBT(null, (short) pressId);
                    pressPlaced = true;
                }
            }
            if (!pressPlaced) {
                return ScenarioResult.fail(name(), "could not place the LV Ammo Press");
            }
            String px = Integer.toString(ctx.originX() + 3);
            String py = Integer.toString(ctx.originY());
            String pz = Integer.toString(ctx.originZ());
            return ScenarioResult.pending(name(), 600, t -> {
                switch (t) {
                    case 20 -> signal(a, "view", "0");
                    case 30 -> signal(a, "shot", "a-first-person-idle");
                    case 38 -> signal(a, "arm", "shot", "a-first-person-fire", "1");
                    case 40 -> signal(a, "fire", "1");
                    case 60 -> signal(a, "view", "1");
                    case 70 -> signal(a, "shot", "a-third-person-back");
                    case 72 -> signal(a, "view", "2");
                    case 82 -> signal(a, "shot", "a-third-person-front");
                    case 84 -> signal(a, "view", "0");
                    case 90 -> {
                        ItemStack stack = new ItemStack(GunItems.pistol());
                        ItemGun.ensureTag(
                            stack,
                            GunItems.pistol()
                                .stats());
                        EntityItem drop = new EntityItem(ctx.world(), ox + 1.0, oy + 0.3, oz, stack);
                        drop.motionX = 0;
                        drop.motionY = 0;
                        drop.motionZ = 0;
                        drop.delayBeforeCanPickup = 32767;
                        ctx.spawn(drop);
                        a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, -90.0f, 55.0f);
                    }
                    case 110 -> signal(a, "shot", "a-dropped");
                    case 112 -> a.playerNetServerHandler.setPlayerLocation(ox, oy, oz, -90.0f, 0.0f);
                    case 120 -> signal(a, "gui", "inventory");
                    case 135 -> signal(a, "shot", "a-inventory");
                    case 138 -> signal(a, "close");
                    case 140 -> a.setGameType(WorldSettings.GameType.CREATIVE);
                    case 150 -> signal(a, "gui", "creative", "gregguns");
                    case 170 -> signal(a, "shot", "a-creative-tab");
                    case 173 -> signal(a, "close");
                    case 176 -> a.setGameType(WorldSettings.GameType.SURVIVAL);
                    case 180 -> signal(a, "gui", "press", px, py, pz);
                    case 215 -> signal(a, "shot", "a-press-gui");
                    case 218 -> signal(a, "close");
                    case 225 -> signal(a, "gui", "nei", "pistol_9mm");
                    case 275 -> signal(a, "shot", "a-nei-pistol");
                    case 278 -> signal(a, "close");
                    case 285 -> signal(a, "gui", "nei", "ammo_9mm_bullet");
                    case 335 -> signal(a, "shot", "a-nei-bullet");
                    case 338 -> signal(a, "close");
                    default -> {}
                }
                if (t < 345) {
                    return null;
                }
                return ScenarioResult.builder(name())
                    .note("signals sent to " + a.getCommandSenderName() + "; check the client log and screenshots")
                    .build();
            });
        }
    }

    // ----- mp.give ------------------------------------------------------------------------------------------

    private static final String PRESS = "gregtech:gt.blockmachines:" + GregGunsMTEIDs.AMMO_PRESS_LV;
    private static final int GIVE_STEP = 12;

    /** Every item name to hand out: the registered {@code gregguns:} items plus the Ammo Press machine. */
    private static List<String> giveList() {
        Set<String> names = new TreeSet<>();
        for (Object key : Item.itemRegistry.getKeys()) {
            String name = (String) key;
            if (name.startsWith("gregguns:")) {
                names.add(name);
            }
        }
        List<String> list = new ArrayList<>(names);
        list.add(PRESS);
        return list;
    }

    private static final class Give implements Scenario {

        @Override
        public String name() {
            return "mp.give";
        }

        @Override
        public String description() {
            return "Gives every connected player every GregGuns item via /give, then via the creative slot packet.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<EntityPlayerMP> players = players();
            if (players.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            List<String> items = giveList();
            int total = items.size();
            for (EntityPlayerMP p : players) {
                p.setGameType(WorldSettings.GameType.SURVIVAL);
            }
            return ScenarioResult.pending(name(), (2 * total + 4) * GIVE_STEP + 40, t -> {
                int step = (t - 1) / GIVE_STEP;
                int phase = (t - 1) % GIVE_STEP;
                if (step >= 2 * total) {
                    return phase == 0 ? ScenarioResult.builder(name())
                        .note("gave " + total + " items twice to " + players.size() + " player(s)")
                        .build() : null;
                }
                String item = items.get(step % total);
                boolean creative = step >= total;
                for (EntityPlayerMP p : players) {
                    switch (phase) {
                        case 0 -> {
                            p.inventory.clearInventory(null, -1);
                            p.setGameType(creative ? WorldSettings.GameType.CREATIVE : WorldSettings.GameType.SURVIVAL);
                            p.inventory.currentItem = 0;
                            p.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(0));
                            if (!creative) {
                                String arg = item.equals(PRESS)
                                    ? "gregtech:gt.blockmachines 1 " + GregGunsMTEIDs.AMMO_PRESS_LV
                                    : item;
                                MinecraftServer.getServer()
                                    .getCommandManager()
                                    .executeCommand(
                                        MinecraftServer.getServer(),
                                        "give " + p.getCommandSenderName() + " " + arg);
                            }
                        }
                        case 3 -> {
                            if (creative) {
                                signal(p, "creativegive", item);
                            }
                        }
                        case 7 -> signal(p, "hold", item);
                        default -> {}
                    }
                }
                return null;
            });
        }
    }
}
