package com.gregguns.armor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fluids.IFluidHandler;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.flight.FlightItems;
import com.gregguns.armor.slots.AmmoPouch;
import com.gregguns.armor.slots.BaublesCompat;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.armor.slots.EquipmentItems;
import com.gregguns.armor.slots.ItemAmmoPouch;
import com.gregguns.armor.slots.PouchInventory;
import com.gregguns.armor.slots.SlotSyncProbe;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.armor.slots.logic.SlotPayload;
import com.gregguns.core.registry.Naming;
import com.gregguns.core.testing.AutotestReport;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioPoller;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.server.GunPlayerData;
import com.gregguns.gun.server.ServerGunHandler;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The special-slot sync, persistence and automation scenarios (M3 exit gate, box 3). Dev environment only; server side.
 *
 * <ul>
 * <li>{@code mp.slots} (two clients, Alice and Bob, not part of {@code test all}): puts Night Vision Goggles (on), an
 * Ammo Pouch with a known stack and a Glider into Alice's Baubles slots, reloads her pistol from the pouch, then
 * signals both clients to report what they hold for Alice ({@code slots Alice}, answered by
 * {@link AutotestReport} chat lines) and compares every slot with the server's own view. Bob checks what other players
 * see (GGS-05-04). The expected state is also written to Alice's persistent entity data.</li>
 * <li>{@code mp.slots.after_relog}: after the harness hard-kills and restarts Alice's client, checks that the slots,
 * the pouch contents and the goggles' switch are still there (saved with the player) and that the new client received
 * them again.</li>
 * <li>{@code armor.slots.automation} (headless): a hopper next to a fake player wearing a loaded pouch neither pulls
 * from nor pushes into it.</li>
 * </ul>
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03, GGS-05-04); implementation-notes.md (M3 exit gate)
final class SlotSyncScenarios {

    private static final String ALICE = "Alice";
    private static final String BOB = "Bob";
    private static final String EXPECT_TAG = "gregguns_slots_expect_";
    private static final String BULLET = "ammo_9mm_bullet";
    private static final String MAG = "mag_9mm_pistol";
    private static final int BULLETS = 7;
    private static final int MAGS_AT_START = 2;
    private static final int SYNC_ROUND_TICKS = 40;
    private static final int SYNC_ROUNDS = 5;

    private SlotSyncScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        Reports reports = new Reports();
        MinecraftForge.EVENT_BUS.register(reports);
        registry.register(new Slots(reports));
        registry.register(new AfterRelog(reports));
        registry.register(new Automation());
    }

    /** Files the clients' {@link AutotestReport} chat lines and keeps them out of the chat. Server side. */
    static final class Reports {

        private final Map<String, String> byKey = new ConcurrentHashMap<>();

        @SubscribeEvent
        public void onChat(ServerChatEvent event) {
            AutotestReport report = AutotestReport.parse(event.message);
            if (report != null) {
                byKey.put(report.key(), report.payload());
                event.setCanceled(true);
            }
        }

        void clear() {
            byKey.clear();
        }

        String get(String viewer, String owner, BaubleSlot slot) {
            return byKey.get(AutotestReport.key(viewer, owner, slot.typeName()));
        }
    }

    // ----- shared helpers ------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static EntityPlayerMP online(String name) {
        for (EntityPlayerMP p : (List<EntityPlayerMP>) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            if (p.getCommandSenderName()
                .equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String onlineNames() {
        List<String> names = new ArrayList<>();
        for (EntityPlayerMP p : (List<EntityPlayerMP>) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            names.add(p.getCommandSenderName());
        }
        return names.toString();
    }

    private static ScenarioResult needPlayers(String scenario, boolean bobToo) {
        return ScenarioResult.fail(
            scenario,
            "needs player " + ALICE
                + (bobToo ? " and " + BOB : "")
                + " online (online: "
                + onlineNames()
                + "); run it by name through mp_check with GG_MP_SCENARIOS="
                + scenario);
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    private static void clearBaubles(EntityPlayerMP player) {
        for (BaubleSlot s : BaubleSlot.values()) {
            BaublesCompat.clear(player, s);
        }
    }

    /** What the server itself says is in the slot: the truth the clients are compared with. */
    private static String truth(EntityPlayerMP owner, BaubleSlot slot) {
        return SlotSyncProbe.describe(owner, slot);
    }

    /**
     * Compares what each viewer's client reported for Alice with the expected payloads.
     *
     * @return the differences, empty when every report matches
     */
    private static List<String> compare(Reports reports, EntityPlayerMP alice, String[] viewers, String[] expected) {
        List<String> diffs = new ArrayList<>();
        for (String viewer : viewers) {
            for (BaubleSlot slot : BaubleSlot.values()) {
                String got = reports.get(viewer, alice.getCommandSenderName(), slot);
                String want = expected[slot.ordinal()];
                if (got == null) {
                    diffs.add(viewer + "'s client sent no report for " + slot.typeName());
                } else if (!got.equals(want)) {
                    diffs.add(viewer + "'s client sees " + slot.typeName() + " = " + got + ", expected " + want);
                }
            }
        }
        return diffs;
    }

    private static String[] truths(EntityPlayerMP alice) {
        String[] out = new String[BaubleSlot.values().length];
        for (BaubleSlot slot : BaubleSlot.values()) {
            out[slot.ordinal()] = truth(alice, slot);
        }
        return out;
    }

    /** Asks each viewer's client to report Alice's slots; sent again every round because a report can be early. */
    private static void askClients(Reports reports, EntityPlayerMP alice, EntityPlayerMP bob) {
        reports.clear();
        signal(alice, "slots", alice.getCommandSenderName());
        if (bob != null) {
            signal(bob, "slots", alice.getCommandSenderName());
        }
    }

    // ----- mp.slots -----------------------------------------------------------------------------------------

    private static final class Slots implements Scenario {

        private final Reports reports;

        Slots(Reports reports) {
            this.reports = reports;
        }

        @Override
        public boolean includedInAll() {
            return false; // needs two connected clients: run by name from mp_check
        }

        @Override
        public String name() {
            return "mp.slots";
        }

        @Override
        public String description() {
            return "Alice wears goggles (on), a loaded Ammo Pouch and a Glider; both clients must receive them. Needs Alice and Bob.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EntityPlayerMP alice = online(ALICE);
            EntityPlayerMP bob = online(BOB);
            if (alice == null || bob == null) {
                return needPlayers(name(), true);
            }
            Item bullet = GameRegistry.findItem(Naming.NAMESPACE, BULLET);
            Item mag = GameRegistry.findItem(Naming.NAMESPACE, MAG);
            if (bullet == null || mag == null
                || GunItems.pistol() == null
                || EquipmentItems.nightVisionGoggles() == null
                || EquipmentItems.ammoPouch() == null
                || FlightItems.glider() == null) {
                return ScenarioResult
                    .fail(name(), "needs the guns and armor modules (run with GG_MODULES=guns,armor,machines)");
            }
            if (!BaublesCompat.isLoaded()) {
                return ScenarioResult.fail(name(), "Baubles Expanded must be loaded");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            double ox = ctx.originX() + 0.5;
            double oy = ctx.originY();
            double oz = ctx.originZ() + 0.5;
            ctx.clearArea(6, 3);
            for (EntityPlayerMP p : new EntityPlayerMP[] { alice, bob }) {
                p.inventory.clearInventory(null, -1);
                clearBaubles(p);
                p.setGameType(WorldSettings.GameType.SURVIVAL);
                p.setHealth(p.getMaxHealth());
            }
            // Three blocks apart and facing each other, so Bob's client tracks Alice and receives her Baubles data.
            alice.playerNetServerHandler.setPlayerLocation(ox, oy, oz, -90.0f, 0.0f);
            bob.playerNetServerHandler.setPlayerLocation(ox + 3, oy, oz, 90.0f, 0.0f);

            ItemStack goggles = new ItemStack(EquipmentItems.nightVisionGoggles());
            ElectricGear.charge(goggles, 10000, Tier.MV);
            ElectricGear.setNightVision(goggles, true);
            out.check(BaublesCompat.equip(alice, BaubleSlot.HEAD, goggles), "goggles must fit the head slot");
            out.check(
                BaublesCompat.equip(alice, BaubleSlot.QUIVER, new ItemStack(EquipmentItems.ammoPouch())),
                "pouch must fit the quiver slot");
            out.check(
                BaublesCompat.equip(alice, BaubleSlot.WINGS, new ItemStack(FlightItems.glider())),
                "glider must fit the wings slot");
            PouchInventory pouch = AmmoPouch.wornAt(alice, AmmoPouch.wornIndex(alice));
            if (pouch == null) {
                return out.check(false, "the worn pouch was not found")
                    .build();
            }
            pouch.setInventorySlotContents(0, new ItemStack(bullet, BULLETS));
            pouch.setInventorySlotContents(1, new ItemStack(mag, MAGS_AT_START));
            out.checkEquals(
                "pouch before the reload",
                MAGS_AT_START,
                SlotPayload.countOf(truth(alice, BaubleSlot.QUIVER), MAG));

            // An empty pistol in the hand and no magazine anywhere but the pouch: a reload can only draw from it.
            ItemStack pistol = new ItemStack(GunItems.pistol());
            ItemGun.ensureTag(
                pistol,
                GunItems.pistol()
                    .stats());
            pistol.getTagCompound()
                .setShort(GunNbt.KEY_AMMO, (short) 0);
            alice.inventory.mainInventory[0] = pistol;
            alice.inventory.currentItem = 0;
            alice.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(0));
            alice.inventoryContainer.detectAndSendChanges();
            if (!out.build()
                .passed()) {
                return out.build();
            }
            return ScenarioResult.pending(name(), 600, new Poller(reports, alice, bob));
        }
    }

    private static final class Poller implements ScenarioPoller {

        private final Reports reports;
        private final EntityPlayerMP alice;
        private final EntityPlayerMP bob;
        private int reloadStarted = -1;
        private int nextAsk = -1;
        private String[] expected;
        private List<String> diffs = new ArrayList<>();
        private int rounds;

        Poller(Reports reports, EntityPlayerMP alice, EntityPlayerMP bob) {
            this.reports = reports;
            this.alice = alice;
            this.bob = bob;
        }

        private int loaded() {
            ItemStack held = alice.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemGun)) {
                return -1;
            }
            GunStats stats = ((ItemGun) held.getItem()).stats();
            return GunNbt.read(held.getTagCompound(), stats)
                .loaded();
        }

        @Override
        public ScenarioResult poll(int t) {
            if (nextAsk < 0) {
                if (loaded() > 0) {
                    startSync(t);
                    return null;
                }
                // The fire cooldown can refuse a reload for a few ticks, so keep pressing until it starts.
                if (t >= 5 && reloadStarted < 0) {
                    ServerGunHandler.onReloadKey(alice);
                    if (GunPlayerData.get(alice)
                        .controller()
                        .reload()
                        .isReloading()) {
                        reloadStarted = t;
                    }
                }
                if (t > 200) {
                    return ScenarioResult.fail(
                        "mp.slots",
                        "the reload never finished from the pouch (reload started at " + reloadStarted
                            + ", rounds "
                            + loaded()
                            + ", pouch "
                            + truth(alice, BaubleSlot.QUIVER)
                            + ")");
                }
                return null;
            }
            if (t < nextAsk) {
                return null;
            }
            if (rounds > 0) {
                diffs = compare(reports, alice, new String[] { ALICE, BOB }, expected);
                if (diffs.isEmpty()) {
                    return finish(t, true);
                }
                if (rounds >= SYNC_ROUNDS) {
                    return finish(t, false);
                }
            }
            rounds++;
            askClients(reports, alice, bob);
            nextAsk = t + SYNC_ROUND_TICKS;
            return null;
        }

        private void startSync(int t) {
            nextAsk = t + 1;
            expected = truths(alice);
            // Persisted with the player so mp.slots.after_relog compares with this exact state after the rejoin.
            NBTTagCompound data = alice.getEntityData();
            for (BaubleSlot slot : BaubleSlot.values()) {
                data.setString(EXPECT_TAG + slot.typeName(), expected[slot.ordinal()]);
            }
        }

        private ScenarioResult finish(int t, boolean synced) {
            ScenarioResult.Builder out = ScenarioResult.builder("mp.slots");
            String head = expected[BaubleSlot.HEAD.ordinal()];
            String quiver = expected[BaubleSlot.QUIVER.ordinal()];
            String wings = expected[BaubleSlot.WINGS.ordinal()];
            out.check(
                SlotPayload.hasField(head, "nv=on") && head.startsWith("night_vision_goggles;"),
                "server: goggles on in head, got " + head);
            out.check(wings.startsWith("glider;"), "server: glider in wings, got " + wings);
            out.checkEquals("server: bullets in the pouch", BULLETS, SlotPayload.countOf(quiver, BULLET));
            out.checkEquals(
                "server: the reload took exactly one magazine from the pouch",
                MAGS_AT_START - 1,
                SlotPayload.countOf(quiver, MAG));
            out.check(loaded() > 0, "the reload loaded the pistol");
            out.check(synced, "client sync after " + rounds + " rounds: " + String.join("; ", diffs));
            out.note(
                "head=" + head
                    + " quiver="
                    + quiver
                    + " wings="
                    + wings
                    + "; reload started at tick "
                    + reloadStarted
                    + ", synced after "
                    + rounds
                    + " round(s), finished at tick "
                    + t);
            return out.build();
        }
    }

    // ----- mp.slots.after_relog -----------------------------------------------------------------------------

    private static final class AfterRelog implements Scenario {

        private final Reports reports;

        AfterRelog(Reports reports) {
            this.reports = reports;
        }

        @Override
        public boolean includedInAll() {
            return false; // runs only after mp.slots and a client relog
        }

        @Override
        public String name() {
            return "mp.slots.after_relog";
        }

        @Override
        public String description() {
            return "After Alice's client was killed and rejoined: her slots, pouch contents and goggles switch persisted and re-synced.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EntityPlayerMP alice = online(ALICE);
            if (alice == null) {
                return needPlayers(name(), false);
            }
            EntityPlayerMP bob = online(BOB);
            String[] expected = new String[BaubleSlot.values().length];
            NBTTagCompound data = alice.getEntityData();
            for (BaubleSlot slot : BaubleSlot.values()) {
                String key = EXPECT_TAG + slot.typeName();
                if (!data.hasKey(key)) {
                    return ScenarioResult.fail(
                        name(),
                        "no recorded state for " + slot.typeName()
                            + ": run mp.slots first, and check Alice's entity data was saved across the relog");
                }
                expected[slot.ordinal()] = data.getString(key);
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            // Persisted: the server's reloaded copy equals what mp.slots recorded, and has the literal contents.
            String[] now = truths(alice);
            for (BaubleSlot slot : BaubleSlot.values()) {
                out.checkEquals("persisted " + slot.typeName(), expected[slot.ordinal()], now[slot.ordinal()]);
            }
            String head = now[BaubleSlot.HEAD.ordinal()];
            String quiver = now[BaubleSlot.QUIVER.ordinal()];
            out.check(
                head.startsWith("night_vision_goggles;") && SlotPayload.hasField(head, "nv=on"),
                "goggles still on after the relog, got " + head);
            out.checkEquals("persisted bullets in the pouch", BULLETS, SlotPayload.countOf(quiver, BULLET));
            out.checkEquals("persisted magazines in the pouch", MAGS_AT_START - 1, SlotPayload.countOf(quiver, MAG));
            out.check(
                now[BaubleSlot.WINGS.ordinal()].startsWith("glider;"),
                "glider still in wings, got " + now[BaubleSlot.WINGS.ordinal()]);
            if (!out.build()
                .passed()) {
                return out.build();
            }
            String[] viewers = bob == null ? new String[] { ALICE } : new String[] { ALICE, BOB };
            return ScenarioResult.pending(name(), 400, new ScenarioPoller() {

                private int rounds;
                private List<String> diffs = new ArrayList<>();

                @Override
                public ScenarioResult poll(int t) {
                    int phase = (t - 1) % SYNC_ROUND_TICKS;
                    if (phase == 0) {
                        if (rounds > 0) {
                            diffs = compare(reports, alice, viewers, expected);
                            if (diffs.isEmpty()) {
                                return ScenarioResult.builder(name())
                                    .note(
                                        "rejoined Alice's client received " + String.join(" | ", expected)
                                            + " after "
                                            + rounds
                                            + " round(s)")
                                    .build();
                            }
                            if (rounds >= SYNC_ROUNDS) {
                                return ScenarioResult.fail(name(), "after the relog: " + String.join("; ", diffs));
                            }
                        }
                        rounds++;
                        askClients(reports, alice, bob);
                    }
                    return null;
                }
            });
        }
    }

    // ----- armor.slots.automation ---------------------------------------------------------------------------

    private static final class Automation implements Scenario {

        @Override
        public String name() {
            return "armor.slots.automation";
        }

        @Override
        public String description() {
            return "A worn Ammo Pouch is not an inventory automation can reach: a hopper beside its wearer neither pulls from nor pushes into it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Item bullet = GameRegistry.findItem(Naming.NAMESPACE, BULLET);
            if (bullet == null || EquipmentItems.ammoPouch() == null || !BaublesCompat.isLoaded()) {
                return ScenarioResult.fail(name(), "needs the guns and armor modules and Baubles Expanded");
            }
            // Structural: nothing a hopper, pipe or GT machine can ask for an inventory, fluid or energy handle.
            for (Class<?> c : new Class<?>[] { ItemAmmoPouch.class, EntityPlayerMP.class }) {
                out.check(!IInventory.class.isAssignableFrom(c), c.getSimpleName() + " must not be an IInventory");
                out.check(
                    !ISidedInventory.class.isAssignableFrom(c),
                    c.getSimpleName() + " must not be an ISidedInventory");
                out.check(
                    !IFluidHandler.class.isAssignableFrom(c),
                    c.getSimpleName() + " must not be an IFluidHandler");
            }
            FakePlayer wearer = new FakePlayer(
                (WorldServer) ctx.world(),
                new GameProfile(java.util.UUID.nameUUIDFromBytes("[GGSlotAuto]".getBytes()), "[GGSlotAuto]"));
            try {
                out.check(
                    BaublesCompat.equip(wearer, BaubleSlot.QUIVER, new ItemStack(EquipmentItems.ammoPouch())),
                    "pouch must fit the quiver slot");
                PouchInventory pouch = AmmoPouch.wornAt(wearer, AmmoPouch.wornIndex(wearer));
                if (pouch == null) {
                    return out.check(false, "the worn pouch was not found")
                        .build();
                }
                pouch.setInventorySlotContents(0, new ItemStack(bullet, 3));
                String before = SlotSyncProbe.describe(wearer, BaubleSlot.QUIVER);

                // Physical: a pushing hopper (loaded with 5 bullets) and a pulling hopper, one block either side of
                // the wearer's cell. The wearer is not placed in the world, so this proves the hoppers find no
                // inventory in that cell and leave the pouch and their own stacks alone.
                ctx.clearArea(3, 3);
                ctx.setBlock(1, 0, 0, Blocks.hopper, 4); // faces -X, toward the wearer's cell at the origin
                ctx.setBlock(-1, 0, 0, Blocks.hopper, 5); // faces +X, toward the wearer's cell
                TileEntityHopper pusher = (TileEntityHopper) ctx.world()
                    .getTileEntity(ctx.originX() + 1, ctx.originY(), ctx.originZ());
                TileEntityHopper puller = (TileEntityHopper) ctx.world()
                    .getTileEntity(ctx.originX() - 1, ctx.originY(), ctx.originZ());
                if (pusher == null || puller == null) {
                    return out.check(false, "the hoppers were not placed")
                        .build();
                }
                pusher.setInventorySlotContents(0, new ItemStack(bullet, 5));
                return ScenarioResult.pending(name(), 100, t -> {
                    if (t < 60) {
                        return null;
                    }
                    ScenarioResult.Builder end = out;
                    end.checkEquals("the pushing hopper kept its bullets", 5, stackSize(pusher.getStackInSlot(0)));
                    end.checkEquals("the pulling hopper stayed empty", 0, stackSize(puller.getStackInSlot(0)));
                    end.checkEquals(
                        "the worn pouch is unchanged",
                        before,
                        SlotSyncProbe.describe(wearer, BaubleSlot.QUIVER));
                    cleanup(wearer);
                    return end.build();
                });
            } catch (RuntimeException e) {
                cleanup(wearer);
                GregGuns.LOG.error("[gregguns] armor.slots.automation failed", e);
                return ScenarioResult.fail(name(), e.toString());
            }
        }

        private static int stackSize(ItemStack stack) {
            return stack == null ? 0 : stack.stackSize;
        }

        private static void cleanup(FakePlayer wearer) {
            BaublesCompat.clear(wearer, BaubleSlot.QUIVER);
        }
    }
}
