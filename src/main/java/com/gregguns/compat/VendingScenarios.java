package com.gregguns.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.Constants;

import com.gregguns.api.Tier;
import com.gregguns.compat.vending.QuestGate;
import com.gregguns.compat.vending.VendingTradeNbt;
import com.gregguns.core.config.CompatConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.core.testing.SkipUnless;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.item.coin.CoinDenomination;
import com.gregguns.item.coin.CoinItems;

import cpw.mods.fml.common.Loader;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

/**
 * Scenarios of the Vending Machine trades ({@link VendingMachineCompat}). They read the machine's live database and its
 * parsed trade groups by reflection, so they also prove the machine accepted the trees GregGuns wrote. Skipped where
 * the
 * Vending Machine is not loaded. Dev environment or test hooks only. Server side.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md
public final class VendingScenarios {

    private static final String DB = "com.cubefury.vendingmachine.trade.TradeDatabase";
    private static final String NEEDS = "needs the Vending Machine mod (vendingmachine), which this run does not load";

    private VendingScenarios() {}

    /**
     * @param registry the scenario registry
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(SkipUnless.of(new TradesScenario(), VendingScenarios::loaded, NEEDS));
        registry.register(SkipUnless.of(new ReloadScenario(), VendingScenarios::loaded, NEEDS));
        registry.register(SkipUnless.of(new PurchaseScenario(), VendingScenarios::loaded, NEEDS));
    }

    private static final String VM = "com.cubefury.vendingmachine.";

    private static boolean loaded() {
        return Loader.isModLoaded(VendingMachineCompat.MOD_ID);
    }

    /** The live database as a tree (the machine's own {@code writeToNBT}). */
    static NBTTagCompound live() throws ReflectiveOperationException {
        Class<?> c = Class.forName(DB);
        Object db = c.getField("INSTANCE")
            .get(null);
        return (NBTTagCompound) c.getMethod("writeToNBT", NBTTagCompound.class)
            .invoke(db, new NBTTagCompound());
    }

    /** @return how many GregGuns groups the live database holds */
    static int ours() throws ReflectiveOperationException {
        NBTTagList groups = live().getTagList(VendingTradeNbt.GROUPS, Constants.NBT.TAG_COMPOUND);
        int n = 0;
        for (int i = 0; i < groups.tagCount(); i++) {
            if (VendingTradeNbt.isOurs(groups.getCompoundTagAt(i))) {
                n++;
            }
        }
        return n;
    }

    /** The machine's parsed trade group for an offer key, or null. */
    static Object parsedGroup(String offerKey) throws ReflectiveOperationException {
        Class<?> c = Class.forName(DB);
        Object db = c.getField("INSTANCE")
            .get(null);
        Method get = c.getMethod("getTradeGroupFromId", UUID.class);
        return get.invoke(db, VendingTradeNbt.groupId(offerKey));
    }

    /** The base stack of the first entry of a parsed trade's list field ({@code fromItems} or {@code toItems}). */
    static ItemStack firstStack(Object group, String listField) throws ReflectiveOperationException {
        List<?> trades = (List<?>) group.getClass()
            .getMethod("getTrades")
            .invoke(group);
        Object trade = trades.get(0);
        Field f = trade.getClass()
            .getField(listField);
        Object big = ((List<?>) f.get(trade)).get(0);
        ItemStack base = (ItemStack) big.getClass()
            .getMethod("getBaseStack")
            .invoke(big);
        ItemStack copy = base.copy();
        copy.stackSize = big.getClass()
            .getField("stackSize")
            .getInt(big);
        return copy;
    }

    /** The trades are in, parsed by the machine, priced in coins, idempotent, and gone with the switch off. */
    private static final class TradesScenario implements Scenario {

        @Override
        public String name() {
            return "compat.vending.trades";
        }

        @Override
        public String description() {
            return "The Vending Machine holds one GregGuns group per registered catalog offer; the machine parsed the"
                + " 9mm Pistol (6 x Coin 10 -> 1 pistol) and the Cyberdemon egg (2 x Coin 1000); merging again adds no"
                + " duplicates and keeps the database version; compat.vendingTrades=false removes them all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            boolean prior = CompatConfig.vendingTrades;
            try {
                int expected = VendingMachineCompat.registeredOffers()
                    .size();
                out.check(expected > 150, "the catalog should offer well over 150 trades here: " + expected);
                out.checkEquals("GregGuns groups after server start", expected, ours());

                Object pistol = parsedGroup("item:pistol_9mm");
                out.check(pistol != null, "the machine should have parsed the 9mm Pistol group");
                if (pistol != null) {
                    ItemStack price = firstStack(pistol, "fromItems");
                    ItemStack goods = firstStack(pistol, "toItems");
                    out.check(price.getItem() == CoinItems.coin(), "the pistol is paid in GregGuns Coins: " + price);
                    out.checkEquals("pistol price denomination", CoinDenomination.TEN.meta(), price.getItemDamage());
                    out.checkEquals("pistol price coins", 6, price.stackSize);
                    out.checkEquals("pistols per trade", 1, goods.stackSize);
                }
                Object demon = parsedGroup("egg:" + NpcMob.CYBER_DEMON.registryName());
                out.check(demon != null, "the machine should have parsed the Cyberdemon egg group");
                if (demon != null) {
                    ItemStack price = firstStack(demon, "fromItems");
                    ItemStack egg = firstStack(demon, "toItems");
                    out.checkEquals("Cyberdemon egg damage", NpcMob.CYBER_DEMON.entityId(), egg.getItemDamage());
                    out.checkEquals(
                        "Cyberdemon price denomination",
                        CoinDenomination.THOUSAND.meta(),
                        price.getItemDamage());
                    out.checkEquals("Cyberdemon price coins", 2, price.stackSize);
                }

                int version = live().getInteger(VendingTradeNbt.VERSION);
                out.checkEquals("trades after a second merge", expected, VendingMachineCompat.apply(false));
                out.checkEquals("GregGuns groups after a second merge (no duplicates)", expected, ours());
                out.checkEquals("database version after merges", version, live().getInteger(VendingTradeNbt.VERSION));

                CompatConfig.vendingTrades = false;
                out.checkEquals("trades added with the switch off", 0, VendingMachineCompat.apply(false));
                out.checkEquals("GregGuns groups with the switch off", 0, ours());
                out.check(
                    parsedGroup("item:pistol_9mm") == null,
                    "the pistol group should be gone with the switch off");
            } catch (ReflectiveOperationException | RuntimeException e) {
                out.check(false, "reading the Vending Machine failed: " + e);
            } finally {
                CompatConfig.vendingTrades = prior;
                VendingMachineCompat.apply(false);
            }
            return out.build();
        }
    }

    /** {@code /vending reload database} drops the merged trades; GregGuns merges them again a second later. */
    private static final class ReloadScenario implements Scenario {

        private static final int TIMEOUT_TICKS = 200;

        @Override
        public String name() {
            return "compat.vending.reload";
        }

        @Override
        public String description() {
            return "Running /vending reload database leaves the machine without GregGuns groups for that tick; within"
                + " "
                + TIMEOUT_TICKS
                + " ticks GregGuns has merged them back, the same count as before.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final int expected;
            final int afterCommand;
            try {
                expected = ours();
                MinecraftServer server = MinecraftServer.getServer();
                server.getCommandManager()
                    .executeCommand(server, "vending reload database");
                afterCommand = ours();
            } catch (ReflectiveOperationException | RuntimeException e) {
                return ScenarioResult.builder(name())
                    .check(false, "reloading the Vending Machine failed: " + e)
                    .build();
            }
            return ScenarioResult.pending(name(), TIMEOUT_TICKS, ticks -> {
                int now;
                try {
                    now = ours();
                } catch (ReflectiveOperationException | RuntimeException e) {
                    return ScenarioResult.builder(name())
                        .check(false, "reading the Vending Machine failed: " + e)
                        .build();
                }
                if (now < expected && ticks < TIMEOUT_TICKS - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(expected > 0, "there should be GregGuns trades before the reload");
                out.checkEquals("GregGuns groups right after the reload command", 0, afterCommand);
                out.checkEquals("GregGuns groups after the re-merge", expected, now);
                return out.build();
            });
        }
    }

    /**
     * A GregGuns trade really sells. A real Vending Machine controller (a fresh copy of the machine's registered
     * prototype, as GT makes one for a placed block) takes GregGuns Coins from its input slots for the 9mm Pistol
     * trade: too few coins or the wrong value fail, the exact price passes, and only the price is taken. The trade's
     * gate is the pack's LV quest: with BetterQuesting loaded, the trade opens once the machine hears the quest is
     * done; without it (dev runs) the trade is open.
     */
    // Source (checkTrade takes item prices from the input slots, matched by item and damage; simulate leaves them):
    // https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/blocks/MTEVendingMachine.java#L620-L670
    // Source (a placed controller is newMetaEntity of the prototype registered at CONTROLLER_MTE_ID):
    // https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/blocks/MTEVendingMachine.java#L160-L162
    // https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/VendingMachine.java#L66
    // Source (canExecuteTrade needs the group among the player's available ones; BetterQuesting unlocks a group through
    // BqAdapter.setQuestFinished, and setQuestUnfinished takes it back):
    // https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/TradeManager.java#L154-L178
    // https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/integration/betterquesting/BqAdapter.java#L62-L87
    private static final class PurchaseScenario implements Scenario {

        /** A player no real login has; the machine keeps no state for it beyond this run. */
        private static final UUID PLAYER = UUID.nameUUIDFromBytes(
            "gregguns:scenario:compat.vending.purchase".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        @Override
        public String name() {
            return "compat.vending.purchase";
        }

        @Override
        public String description() {
            return "A Vending Machine controller sells the 9mm Pistol for 6 x GregGuns Coin 10 from its input slots: 5"
                + " coins or 60 x Coin 1 are refused, 10 x Coin 10 pays and leaves 4; the trade waits for the pack's LV"
                + " quest when BetterQuesting is loaded and is open otherwise.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Object bq = null;
            UUID gate = QuestGate.questId(Tier.LV);
            try {
                Object group = parsedGroup("item:pistol_9mm");
                out.check(group != null, "the machine should have parsed the 9mm Pistol group");
                if (group == null) {
                    return out.build();
                }
                Class<?> managerClass = Class.forName(VM + "trade.TradeManager");
                Object manager = managerClass.getField("INSTANCE")
                    .get(null);
                Method canExecute = managerClass.getMethod("canExecuteTrade", UUID.class, group.getClass());
                java.util.Set<?> requirements = (java.util.Set<?>) group.getClass()
                    .getMethod("getRequirements")
                    .invoke(group);
                if (requirements.isEmpty()) {
                    out.check(
                        !Loader.isModLoaded("betterquesting"),
                        "the pistol trade is open, so BetterQuesting should not be loaded here");
                } else {
                    out.checkEquals("pistol trade conditions", 1, requirements.size());
                    Object condition = requirements.iterator()
                        .next();
                    out.checkEquals(
                        "pistol trade gate quest (the pack's LV quest)",
                        gate,
                        condition.getClass()
                            .getMethod("getQuestId")
                            .invoke(condition));
                    out.check(
                        !(Boolean) canExecute.invoke(manager, PLAYER, group),
                        "the pistol trade should wait for the pack's LV quest");
                    Class<?> adapter = Class.forName(VM + "integration.betterquesting.BqAdapter");
                    bq = adapter.getField("INSTANCE")
                        .get(null);
                    // What BetterQuesting tells the machine for every completed quest, every 10 seconds.
                    adapter.getMethod("setQuestFinished", UUID.class, UUID.class)
                        .invoke(bq, PLAYER, gate);
                }
                out.check(
                    (Boolean) canExecute.invoke(manager, PLAYER, group),
                    "the pistol trade should be open to the player now");

                Object trade = ((List<?>) group.getClass()
                    .getMethod("getTrades")
                    .invoke(group)).get(0);
                Object machine = newMachine();
                out.check(machine != null, "the Vending Machine controller should be registered with GT");
                if (machine == null) {
                    return out.build();
                }
                Object inputs = machine.getClass()
                    .getField("inputItems")
                    .get(machine);
                Method setSlot = inputs.getClass()
                    .getMethod("setStackInSlot", int.class, ItemStack.class);
                Method getSlot = inputs.getClass()
                    .getMethod("getStackInSlot", int.class);
                Class<?> walletMode = Class.forName(VM + "blocks.gui.WalletMode");
                Object personal = walletMode.getField("PERSONAL")
                    .get(null);
                Method check = machine.getClass()
                    .getMethod("checkTrade", trade.getClass(), UUID.class, walletMode, boolean.class);

                setSlot.invoke(inputs, 0, CoinItems.coin()
                    .stack(CoinDenomination.TEN, 5));
                out.check(
                    !(Boolean) check.invoke(machine, trade, PLAYER, personal, true),
                    "5 x Coin 10 should not pay a 6 x Coin 10 price");

                setSlot.invoke(inputs, 0, CoinItems.coin()
                    .stack(CoinDenomination.ONE, 60));
                out.check(
                    !(Boolean) check.invoke(machine, trade, PLAYER, personal, true),
                    "the machine matches the coin value exactly, so 60 x Coin 1 should not pay 6 x Coin 10");

                setSlot.invoke(inputs, 0, CoinItems.coin()
                    .stack(CoinDenomination.TEN, 10));
                out.check(
                    (Boolean) check.invoke(machine, trade, PLAYER, personal, true),
                    "10 x Coin 10 should pay the pistol (simulated)");
                out.checkEquals(
                    "coins left after a simulated trade",
                    10,
                    ((ItemStack) getSlot.invoke(inputs, 0)).stackSize);
                out.check(
                    (Boolean) check.invoke(machine, trade, PLAYER, personal, false),
                    "10 x Coin 10 should pay the pistol");
                ItemStack left = (ItemStack) getSlot.invoke(inputs, 0);
                out.check(
                    left != null && left.getItem() == CoinItems.coin()
                        && left.getItemDamage() == CoinDenomination.TEN.meta(),
                    "the change should stay in the slot as Coin 10: " + left);
                out.checkEquals("coins left after the trade", 4, left == null ? 0 : left.stackSize);
            } catch (ReflectiveOperationException | RuntimeException e) {
                out.check(false, "trading at the Vending Machine failed: " + e);
            } finally {
                if (bq != null) {
                    try {
                        bq.getClass()
                            .getMethod("setQuestUnfinished", UUID.class, UUID.class)
                            .invoke(bq, PLAYER, gate);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        out.check(false, "resetting the scenario player's quest failed: " + e);
                    }
                }
            }
            return out.build();
        }

        /** @return a fresh Vending Machine controller, unplaced, or null if GT has none registered */
        private static Object newMachine() throws ReflectiveOperationException {
            int id = Class.forName(VM + "VendingMachine")
                .getField("CONTROLLER_MTE_ID")
                .getInt(null);
            IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
            return prototype == null ? null : prototype.newMetaEntity(null);
        }
    }
}
