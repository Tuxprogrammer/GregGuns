package com.gregguns.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;

import com.gregguns.GregGuns;
import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.api.Tier;
import com.gregguns.compat.vending.QuestGate;
import com.gregguns.compat.vending.VendingCatalog;
import com.gregguns.compat.vending.VendingTradeNbt;
import com.gregguns.core.config.CompatConfig;
import com.gregguns.core.registry.Naming;
import com.gregguns.item.coin.CoinItems;
import com.gregguns.item.coin.ItemCoin;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Puts the GregGuns trades ({@link VendingCatalog}) into the pack's Vending Machine, priced in GregGuns Coins. The
 * machine has no registration API: it reads {@code config/vendingmachine/tradeDatabase.json} in its own
 * server-starting handler. So once the server has started, GregGuns merges its trade groups into the machine's live
 * database through the machine's own public loader ({@code TradeDatabase.readFromNBT}, merge mode), by reflection, so
 * GregGuns has no compile or class-loading link to the mod. The pack's file is never written by GregGuns.
 *
 * <p>
 * Players get the merged database from the machine's own login sync; single player shares the server's copy. Before
 * merging, any GregGuns group already in the database (an admin's {@code /vending add} saves the whole database,
 * GregGuns groups included) is taken out, so the jar's table always wins and {@code compat.vendingTrades=false} leaves
 * none behind. {@code /vending reload database} drops the merged groups; GregGuns merges them again a second later and
 * resends the database. Server side; does nothing without the Vending Machine.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Mechanism)
// Source (the machine loads its file in FMLServerStartingEvent; readFromNBT(nbt, merge, isFileLoad) is public, merge
// keeps the existing groups, isFileLoad=false never marks the file dirty):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/CommonProxy.java#L41-L53
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/TradeDatabase.java#L74-L120
// Source (clients receive the whole database at login and after /vending reload):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/network/handlers/NetBulkSync.java#L38-L58
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/command/vending/SubCmdReload.java#L28-L33
public final class VendingMachineCompat {

    /** The Vending Machine's mod id. */
    public static final String MOD_ID = "vendingmachine";

    private static final String DATABASE_CLASS = "com.cubefury.vendingmachine.trade.TradeDatabase";
    private static final String SYNC_CLASS = "com.cubefury.vendingmachine.network.handlers.NetTradeDbSync";
    private static final String BQ_MOD_ID = "betterquesting";

    /** Ticks between a {@code /vending reload} and the re-merge, so the machine's own resend has gone out first. */
    static final int RELOAD_DELAY_TICKS = 20;

    private static final VendingMachineCompat HANDLER = new VendingMachineCompat();
    private static boolean registered;
    private int reloadIn = -1;

    private VendingMachineCompat() {}

    /** Registers the reload hook. Call once at init; does nothing without the Vending Machine. */
    public static void init() {
        if (registered || !Loader.isModLoaded(MOD_ID)) {
            return;
        }
        registered = true;
        MinecraftForge.EVENT_BUS.register(HANDLER);
        FMLCommonHandler.instance()
            .bus()
            .register(HANDLER);
    }

    /** Merges the trades into the live database. Call from GregGuns' server-started handler. */
    public static void onServerStarted() {
        if (!Loader.isModLoaded(MOD_ID)) {
            return;
        }
        HANDLER.reloadIn = -1;
        apply(false);
    }

    /**
     * Takes any GregGuns groups out of the live database and, when {@code compat.vendingTrades} is on, merges the
     * current table in.
     *
     * @param resend true to send the database to every player afterwards (after a reload)
     * @return how many GregGuns trades the database holds now, or -1 if the machine could not be reached
     */
    public static int apply(boolean resend) {
        try {
            Class<?> dbClass = Class.forName(DATABASE_CLASS);
            Object db = dbClass.getField("INSTANCE")
                .get(null);
            Method write = dbClass.getMethod("writeToNBT", NBTTagCompound.class);
            Method read = dbClass.getMethod("readFromNBT", NBTTagCompound.class, boolean.class, boolean.class);

            NBTTagCompound live = (NBTTagCompound) write.invoke(db, new NBTTagCompound());
            int version = live.getInteger(VendingTradeNbt.VERSION);
            int removed = VendingTradeNbt.removeOurs(live);
            if (removed > 0) {
                // Reload the rest in place (not a file load, so nothing is written).
                read.invoke(db, live, false, false);
            }
            int added = 0;
            ItemCoin coin = CoinItems.coin();
            if (CompatConfig.vendingTrades && coin != null) {
                List<VendingCatalog.Offer> offers = registeredOffers();
                Map<String, UUID> gates = gates();
                NBTTagCompound ours = VendingTradeNbt
                    .database(offers, version, Item.itemRegistry.getNameForObject(coin), gates);
                read.invoke(db, ours, true, false);
                added = offers.size();
                GregGuns.LOG.info(
                    "Added {} GregGuns trades to the Vending Machine ({} behind GregGuns quests{})",
                    added,
                    countGated(offers, gates),
                    removed > 0 ? ", replacing " + removed + " saved copies" : "");
            } else if (removed > 0) {
                GregGuns.LOG
                    .info("Removed {} GregGuns trades from the Vending Machine (compat.vendingTrades)", removed);
            }
            if (resend) {
                Class.forName(SYNC_CLASS)
                    .getMethod("sendDatabase", net.minecraft.entity.player.EntityPlayerMP.class, boolean.class)
                    .invoke(null, null, false);
            }
            return added;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            GregGuns.LOG.error("Could not add the GregGuns trades to the Vending Machine; its trades are unchanged", e);
            return -1;
        }
    }

    /** @return the catalog offers whose item is registered on this server and not a hidden technical item; spawn eggs of kinds that cannot spawn are left out */
    static List<VendingCatalog.Offer> registeredOffers() {
        List<VendingCatalog.Offer> out = new ArrayList<>();
        for (VendingCatalog.Offer offer : VendingCatalog.build()) {
            Object item = Item.itemRegistry.getObject(Naming.qualifiedName(offer.item()));
            if (item instanceof Item && !(item instanceof HiddenFromCreativeTab) && VendingCatalog.spawnable(offer)) {
                out.add(offer);
            }
        }
        return out;
    }

    /**
     * @return gate quest key to quest ID, for the gate quests BetterQuesting's database holds; empty without it
     */
    private static Map<String, UUID> gates() {
        Map<String, UUID> wanted = new HashMap<>();
        for (Tier tier : Tier.values()) {
            wanted.put(QuestGate.introKey(tier), QuestGate.questId(QuestGate.introKey(tier)));
        }
        if (!Loader.isModLoaded(BQ_MOD_ID)) {
            return new HashMap<>();
        }
        return BetterQuestingQuests.present(wanted);
    }

    private static int countGated(List<VendingCatalog.Offer> offers, Map<String, UUID> gates) {
        int n = 0;
        for (VendingCatalog.Offer offer : offers) {
            if (gates.containsKey(offer.questKey())) {
                n++;
            }
        }
        return n;
    }

    /** @param event any server command; {@code /vending reload ...} schedules a re-merge */
    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (event.command != null && "vending".equals(event.command.getCommandName())
            && event.parameters != null
            && event.parameters.length > 0
            && "reload".equalsIgnoreCase(event.parameters[0])) {
            reloadIn = RELOAD_DELAY_TICKS;
        }
    }

    /** @param event the server tick; runs a scheduled re-merge */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || reloadIn < 0) {
            return;
        }
        if (reloadIn-- == 0 && MinecraftServer.getServer() != null) {
            apply(true);
        }
    }
}
