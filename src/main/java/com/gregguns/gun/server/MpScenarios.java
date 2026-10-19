package com.gregguns.gun.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;

import com.gregguns.armor.slots.BaublesCompat;
import com.gregguns.armor.slots.logic.BaubleSlot;
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

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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
        public boolean includedInAll() {
            return false; // needs two connected clients: run by name from mp_check
        }

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
            return ScenarioResult.pending(name(), 700, new SyncPoller(ctx, a, b));
        }
    }

    /** Ticks to wait for a pressed shot to show up as a round drop before pressing again, and between steps. */
    private static final int SHOT_RETRY_TICKS = 8;
    /**
     * The same wait in wall-clock time: after a stall (a cold server skips ticks, then bursts) several poll ticks
     * pass within a few real milliseconds, before the client's input can have reached the server.
     */
    private static final long SHOT_RETRY_MS = 1500L;
    /** Most fire presses before giving up (two are needed). */
    private static final int MAX_PRESSES = 6;
    /** Ticks to keep requesting the reload before giving up. */
    private static final int RELOAD_START_TIMEOUT = 60;
    /** Ticks after the reload started by which the rounds must have risen (the pistol reload takes well under it). */
    private static final int RELOAD_TIMEOUT = 120;

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
        private int shotsSeen;
        private int lastStep = -1;
        private int presses;
        private long lastStepMs;

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
            if (t < 60) {
                return null;
            }
            // Server-confirmed sequence: each step starts only when the server saw the previous one happen.
            // Shots come from client A's real trigger packets ("fire 1" presses the fire button once).
            if (shotsSeen < 2) {
                int now = loaded(a);
                if (shotsSeen == 0 && now >= 0 && now <= roundsStart - 1) {
                    shotsSeen = 1;
                    lastStep = t;
                    lastStepMs = System.currentTimeMillis();
                } else if (shotsSeen == 1 && now <= roundsStart - 2) {
                    shotsSeen = 2;
                    roundsAfterShots = now;
                    magsBefore = magazines(a);
                    lastStep = t;
                    lastStepMs = System.currentTimeMillis();
                } else if (lastStep < 0
                    || (t - lastStep >= SHOT_RETRY_TICKS && System.currentTimeMillis() - lastStepMs >= SHOT_RETRY_MS)) {
                        // First press, or the previous press was lost (cooldown, input race): press again. The gap
                        // after a seen drop is longer than the 4-tick fire cooldown.
                        if (presses >= MAX_PRESSES) {
                            return finish(t);
                        }
                        presses++;
                        signal(a, "fire", "1");
                        lastStep = t;
                        lastStepMs = System.currentTimeMillis();
                    }
                return null;
            }
            if (reloadAt < 0) {
                if (t - lastStep < SHOT_RETRY_TICKS || System.currentTimeMillis() - lastStepMs < SHOT_RETRY_MS) {
                    return null;
                }
                // The fire cooldown can still refuse a reload for a few ticks, so keep pressing until it starts.
                ServerGunHandler.onReloadKey(a);
                if (GunPlayerData.get(a)
                    .controller()
                    .reload()
                    .isReloading()) {
                    reloadAt = t;
                } else if (t - lastStep > RELOAD_START_TIMEOUT) {
                    return finish(t);
                }
                return null;
            }
            // Wait for the reload to finish (rounds rise), with margin.
            if (loaded(a) > roundsAfterShots || t >= reloadAt + RELOAD_TIMEOUT) {
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
                    + t
                    + ", reload started at "
                    + reloadAt
                    + ", presses "
                    + presses
                    + ", reloading now "
                    + GunPlayerData.get(a)
                        .controller()
                        .reload()
                        .isReloading()
                    + ", last cancel "
                    + GunPlayerData.get(a)
                        .controller()
                        .reload()
                        .lastCancelReason());
            return out.build();
        }
    }

    // ----- mp.visual ----------------------------------------------------------------------------------------

    private static final class Visual implements Scenario {

        @Override
        public boolean includedInAll() {
            return false; // needs two connected clients: run by name from mp_check
        }

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
                    case 4 -> {
                        a.inventory.currentItem = 8;
                        a.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(8));
                    }
                    case 15 -> signal(a, "shot", "a-baseline-empty-hand");
                    case 18 -> {
                        a.inventory.currentItem = 0;
                        a.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(0));
                    }
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

    /**
     * Per-item wait for every client's confirmation, in wall-clock milliseconds: a stalled server skips ticks (a
     * cold first login can skip 80), so a tick budget would expire without any real time passing.
     */
    private static final long GIVE_CONFIRM_TIMEOUT_MS = 10_000L;

    /** How long a client may take to work off the previous scenario's queued commands (screenshots, GUIs). */
    private static final long SYNC_TIMEOUT_MS = 90_000L;

    /**
     * Chat turns {@code _word_} into formatting in the pack (it ate "_9mm_" from an item name), so item names travel
     * in acks with underscores swapped for dashes.
     */
    private static String wire(String item) {
        return item.replace('_', '-');
    }

    /**
     * Collects the clients' {@code hold} confirmations. A client sends a chat line
     * {@code PREFIX ack hold <item> ok|fail}; this handler stores it under the sender's name and hides it.
     */
    public static final class AckInbox {

        private final Map<String, String> acks = new ConcurrentHashMap<>();

        /**
         * @param event a chat line from a client
         */
        @SubscribeEvent
        public void onChat(ServerChatEvent event) {
            String[] parts = AutotestSignal.parse(event.message);
            if (parts == null || parts.length < 4 || !"ack".equals(parts[0])) {
                return;
            }
            event.setCanceled(true);
            acks.put(event.username, parts[1] + " " + parts[2] + " " + parts[3]);
        }

        void clear(String player) {
            acks.remove(player);
        }

        /** @return {@code "hold <item> ok|fail"}, or null if nothing arrived since {@link #clear} */
        String get(String player) {
            return acks.get(player);
        }
    }

    /**
     * Closed loop, one item at a time: clear the player (inventory, armour, Baubles), put the item in the selected
     * hotbar slot (command or creative packet), tell the client to hold it, and move on only when every client has
     * confirmed that the item arrived in its hand and was drawn for a few ticks. A client that never confirms fails
     * the scenario naming the item.
     */
    private static final class Give implements Scenario {

        private AckInbox inbox;

        @Override
        public boolean includedInAll() {
            return false; // needs two connected clients: run by name from mp_check
        }

        @Override
        public String name() {
            return "mp.give";
        }

        @Override
        public String description() {
            return "Gives every connected player every GregGuns item via /give, then via the creative slot packet.";
        }

        private void stop() {
            if (inbox != null) {
                MinecraftForge.EVENT_BUS.unregister(inbox);
                inbox = null;
            }
        }

        private static void reset(EntityPlayerMP p, boolean creative) {
            p.inventory.clearInventory(null, -1);
            for (BaubleSlot slot : BaubleSlot.values()) {
                BaublesCompat.clear(p, slot);
            }
            p.setGameType(creative ? WorldSettings.GameType.CREATIVE : WorldSettings.GameType.SURVIVAL);
            p.inventory.currentItem = 0;
            p.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(0));
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<EntityPlayerMP> players = players();
            if (players.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            stop();
            inbox = new AckInbox();
            MinecraftForge.EVENT_BUS.register(inbox);
            List<String> items = giveList();
            int total = items.size();
            // state: [0] step, [1] tick the current step was sent (-1: not yet sent)
            long[] state = { -1, -1 };
            return ScenarioResult.pending(name(), 2 * total * 200 + 100, t -> {
                int step = (int) state[0];
                if (step < 0) {
                    // Barrier: the previous scenario (mp.visual) leaves the clients with a backlog of GUI and
                    // screenshot commands; hold signals queue behind it, so start only once every client is idle.
                    if (state[1] < 0) {
                        for (EntityPlayerMP p : players) {
                            inbox.clear(p.getCommandSenderName());
                            signal(p, "sync");
                        }
                        state[1] = System.currentTimeMillis();
                        return null;
                    }
                    boolean idle = true;
                    for (EntityPlayerMP p : players) {
                        idle &= "sync ready ok".equals(inbox.get(p.getCommandSenderName()));
                    }
                    if (idle) {
                        state[0] = 0;
                        state[1] = -1;
                    } else if (System.currentTimeMillis() - state[1] > SYNC_TIMEOUT_MS) {
                        stop();
                        return ScenarioResult.fail(
                            name(),
                            "a client did not drain its command queue within " + SYNC_TIMEOUT_MS / 1000 + " s");
                    }
                    return null;
                }
                if (step >= 2 * total) {
                    stop();
                    return ScenarioResult.builder(name())
                        .note(
                            "gave and held " + total
                                + " items twice (command, then creative packet) for "
                                + players.size()
                                + " player(s)")
                        .build();
                }
                String item = items.get(step % total);
                boolean creative = step >= total;
                String how = creative ? "creative packet" : "/give";
                if (state[1] < 0) {
                    for (EntityPlayerMP p : players) {
                        inbox.clear(p.getCommandSenderName());
                        reset(p, creative);
                        if (creative) {
                            signal(p, "creativegive", item);
                        } else {
                            String arg = item.equals(PRESS)
                                ? "gregtech:gt.blockmachines 1 " + GregGunsMTEIDs.AMMO_PRESS_LV
                                : item;
                            MinecraftServer.getServer()
                                .getCommandManager()
                                .executeCommand(
                                    MinecraftServer.getServer(),
                                    "give " + p.getCommandSenderName() + " " + arg);
                        }
                        signal(p, "hold", item);
                    }
                    state[1] = System.currentTimeMillis();
                    return null;
                }
                boolean all = true;
                for (EntityPlayerMP p : players) {
                    String ack = inbox.get(p.getCommandSenderName());
                    if (ack == null) {
                        all = false;
                    } else if (!ack.equals("hold " + wire(item) + " ok")) {
                        stop();
                        return ScenarioResult.fail(
                            name(),
                            p.getCommandSenderName() + " could not hold "
                                + item
                                + " ("
                                + how
                                + "): client said '"
                                + ack
                                + "'");
                    }
                }
                if (all) {
                    state[0]++;
                    state[1] = -1;
                } else if (System.currentTimeMillis() - state[1] > GIVE_CONFIRM_TIMEOUT_MS) {
                    StringBuilder silent = new StringBuilder();
                    for (EntityPlayerMP p : players) {
                        if (inbox.get(p.getCommandSenderName()) == null) {
                            ItemStack slot0 = p.inventory.mainInventory[0];
                            silent.append(' ')
                                .append(p.getCommandSenderName())
                                .append(" (server side: selected slot ")
                                .append(p.inventory.currentItem)
                                .append(", slot 0 = ")
                                .append(slot0 == null ? "empty" : slot0.getDisplayName())
                                .append(')');
                        }
                    }
                    stop();
                    return ScenarioResult.fail(
                        name(),
                        "no client confirmation from" + silent
                            + " for "
                            + item
                            + " ("
                            + how
                            + ") within "
                            + GIVE_CONFIRM_TIMEOUT_MS / 1000
                            + " s");
                }
                return null;
            });
        }
    }
}
