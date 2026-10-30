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
    }

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
}
