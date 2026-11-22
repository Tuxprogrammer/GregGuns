package com.gregguns.compat.vending;

import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.Trade;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.Wallet;
import com.gregguns.item.coin.CoinDenomination;
import com.gregguns.item.coin.CoinItems;

/**
 * The GregGuns coin kind as the patched Vending Machine sees it. This is the one class outside the mixins that links to
 * the machine's classes, so it is only loaded when the Vending Machine is: through {@link VendingCoin#check()} and the
 * scenarios, both behind {@code Loader.isModLoaded}. Its methods take and return only JDK, Minecraft and GregGuns
 * types.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Wallet)
// Source (CurrencyType and CurrencyItem, unchanged from 0.4.101 through 0.5.1):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/CurrencyType.java
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/CurrencyItem.java
// Source (Wallet: an EnumMap, saved by currency id):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/util/Wallet.java
public final class VendingCurrency {

    private VendingCurrency() {}

    /**
     * Proves every path the wallet uses: the coin kind exists, the machine reads a coin stack's value from its
     * denomination, pays a balance back out in our denominations, and a wallet keeps and saves a GregGuns balance.
     *
     * @return null when the patched machine works, else what failed
     */
    public static String selfTest() {
        CurrencyType type = CurrencyType.getTypeFromId(VendingCoin.CURRENCY_ID);
        if (type == null) {
            return "no '" + VendingCoin.CURRENCY_ID + "' coin kind (the CurrencyType mixin did not apply)";
        }
        boolean listed = false;
        for (CurrencyType t : CurrencyType.values()) {
            listed |= t == type;
        }
        if (!listed) {
            return "the coin kind is missing from CurrencyType.values()";
        }
        ItemStack thirty = CoinItems.coin()
            .stack(CoinDenomination.TEN, 3);
        CurrencyItem read = CurrencyItem.fromItemStack(thirty);
        if (read == null || read.type != type || read.value != 30) {
            return "3 x Coin 10 reads as " + describe(read) + ", not 30 (the CurrencyItem mixin did not apply)";
        }
        if (!type.isMatchingType(thirty)) {
            return "the coin kind does not match the coin item";
        }
        List<ItemStack> paid = new CurrencyItem(type, 1234).itemize();
        List<ItemStack> expected = VendingCoin.itemize(1234);
        if (!sameStacks(paid, expected)) {
            return "a balance of 1234 pays out as " + paid + ", not " + expected;
        }
        Wallet wallet = new Wallet();
        wallet.addCount(type, 1234);
        NBTTagCompound saved = new NBTTagCompound();
        wallet.writeToNBT(saved);
        Wallet loaded = new Wallet();
        loaded.readFromNBT(saved, false);
        if (loaded.getCount(type) != 1234) {
            return "a wallet saved with 1234 loads with " + loaded.getCount(type);
        }
        return null;
    }

    /** @return a new, empty wallet (a {@code Wallet}) */
    public static Object newWallet() {
        return new Wallet();
    }

    /**
     * @param player a player
     * @return the player's personal wallet (a {@code Wallet}), or null when the player has no team data yet
     */
    public static Object personalWallet(UUID player) {
        return TradeManager.INSTANCE.getWallet(player, WalletMode.PERSONAL);
    }

    /**
     * @param wallet a {@code Wallet}
     * @return its GregGuns Coin balance
     */
    public static int balance(Object wallet) {
        return ((Wallet) wallet).getCount(type());
    }

    /**
     * @param wallet a {@code Wallet}
     * @param value  the GregGuns Coin balance to set
     */
    public static void setBalance(Object wallet, int value) {
        ((Wallet) wallet).setCount(type(), value);
    }

    /**
     * Puts coins in a wallet the way the machine's coin slot does: the value its {@code fromItemStack} reads.
     *
     * @param wallet a {@code Wallet}
     * @param coins  the stack inserted
     * @return false if the machine did not read the stack as GregGuns Coin
     */
    public static boolean deposit(Object wallet, ItemStack coins) {
        CurrencyItem item = CurrencyItem.fromItemStack(coins);
        if (item == null || item.type != type()) {
            return false;
        }
        ((Wallet) wallet).addCount(item.type, item.value);
        return true;
    }

    /**
     * Pays a trade's currency price from a wallet the way {@code checkTrade} does: the machine's own
     * {@code removeCurrency} charges a copy, and only a whole payment is written back.
     *
     * @param machine  a {@code MTEVendingMachine}
     * @param trade    a {@code Trade}
     * @param wallet   a {@code Wallet}
     * @param simulate true to leave the wallet unchanged
     * @return true if the wallet covered the whole price
     */
    public static boolean pay(Object machine, Object trade, Object wallet, boolean simulate) {
        Wallet before = (Wallet) wallet;
        Wallet after = Wallet.copyOf(before);
        boolean paid = ((MTEVendingMachine) machine).removeCurrency(((Trade) trade).fromCurrency, after)
            .isEmpty();
        if (paid && !simulate) {
            before.resetAllCount();
            before.merge(after);
        }
        return paid;
    }

    /**
     * @param trade a {@code Trade}
     * @return its currency price as {@code "<id>:<value>"} entries joined by commas, empty when it has none
     */
    public static String currencyPrice(Object trade) {
        StringBuilder out = new StringBuilder();
        for (CurrencyItem item : ((Trade) trade).fromCurrency) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(item == null ? "null" : item.type.id + ":" + item.value);
        }
        return out.toString();
    }

    /**
     * The machine's whole trade check with a player's personal wallet: items, wallet, then the ME Uplink Hatch.
     *
     * @param machine  a {@code MTEVendingMachine}
     * @param trade    a {@code Trade}
     * @param player   the player
     * @param simulate true to change nothing
     * @return true if the trade went through (or would)
     */
    public static boolean checkTrade(Object machine, Object trade, UUID player, boolean simulate) {
        return ((MTEVendingMachine) machine).checkTrade((Trade) trade, player, WalletMode.PERSONAL, simulate);
    }

    private static CurrencyType type() {
        return CurrencyType.getTypeFromId(VendingCoin.CURRENCY_ID);
    }

    private static String describe(CurrencyItem item) {
        return item == null ? "nothing" : item.type.id + " " + item.value;
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        if (a == null || a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            ItemStack x = a.get(i);
            ItemStack y = b.get(i);
            if (x == null || x.getItem() != y.getItem()
                || x.getItemDamage() != y.getItemDamage()
                || x.stackSize != y.stackSize) {
                return false;
            }
        }
        return true;
    }
}
