package com.gregguns.compat.vending;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.compat.VendingMachineCompat;
import com.gregguns.item.coin.CoinDenomination;
import com.gregguns.item.coin.CoinItems;
import com.gregguns.item.coin.ItemCoin;

import cpw.mods.fml.common.Loader;

/**
 * The GregGuns Coin as a Vending Machine coin kind. The machine's wallet holds only the coin kinds of its
 * {@code CurrencyType} enum, so the late mixins in {@code com.gregguns.mixins.late.vending} add one more kind,
 * {@value #CURRENCY_ID}, and teach the machine's {@code CurrencyItem} the coin's value: the item damage names the
 * denomination, where the pack's coins use one item per denomination. The mixins only call the hooks here, so the logic
 * stays in GregGuns and unit tests.
 *
 * <p>
 * {@link #check()} runs at postInit on both sides and decides {@link #inWallet()} with a self-test of the patched
 * machine ({@link VendingCurrency}). When it passes, GregGuns trades are priced in the coin kind and paid from the
 * wallet (or from ME through the Uplink Hatch); when the mixins did not apply, as after a Vending Machine update that
 * moved their targets, trades fall back to coin items in the input slots, as before. This class has no link to the
 * Vending Machine's classes.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Wallet)
public final class VendingCoin {

    /** The coin kind's id: the wallet's NBT key, {@code /vending add}'s argument and the trade price's type. */
    public static final String CURRENCY_ID = "gregguns";

    /** The added enum constant's name. */
    public static final String ENUM_NAME = "GREGGUNS";

    /**
     * The coin kind's item prefix: the coin's registry name. The machine's own {@code isMatchingType} (the ME Uplink
     * Hatch's coin pull) matches by this prefix, and no other GregGuns item name starts with it.
     */
    public static final String ITEM_PREFIX = GregGuns.MODID + ":" + ItemCoin.NAME;

    /** The coin kind's texture name: the machine asks for icons {@code icons/gregguns0} to {@code icons/gregguns4}. */
    public static final String TEXTURE_NAME = "gregguns";

    private static final String ICON_PREFIX = "icons/" + TEXTURE_NAME;

    private static volatile boolean inWallet;

    private VendingCoin() {}

    /**
     * @param stack a stack, may be null
     * @return true if it is GregGuns Coin of a known denomination
     */
    public static boolean isCoin(ItemStack stack) {
        ItemCoin coin = CoinItems.coin();
        return coin != null && stack != null
            && stack.getItem() == coin
            && CoinDenomination.byMeta(stack.getItemDamage()) != null;
    }

    /**
     * @param stack a stack, may be null
     * @return the stack's worth in single coins, or -1 if it is not GregGuns Coin of a known denomination
     */
    public static long worth(ItemStack stack) {
        if (!isCoin(stack)) {
            return -1;
        }
        return (long) CoinDenomination.byMeta(stack.getItemDamage())
            .value() * stack.stackSize;
    }

    /**
     * What the machine hands out for a wallet balance (its {@code CurrencyItem.itemize}).
     *
     * @param value a sum in single coins
     * @return coin stacks, largest denomination first; empty for zero or less, or without the coin item
     */
    public static List<ItemStack> itemize(long value) {
        List<ItemStack> out = new ArrayList<>();
        ItemCoin coin = CoinItems.coin();
        if (coin == null) {
            return out;
        }
        for (CoinDenomination.Stack s : CoinDenomination.change(value)) {
            out.add(coin.stack(s.denomination(), s.count()));
        }
        return out;
    }

    /**
     * Where the machine's coin display finds a GregGuns icon. The machine names icons {@code icons/<texture><n>}, n
     * from 0 (worth 1) to 4 (worth 10,000), under the pack core mod's textures; ours are the coin's own item textures.
     *
     * @param name the name the machine builds ({@code icons/gregguns2}, {@code background/small0}, ...)
     * @return the texture path under {@code assets/gregguns/textures/} without {@code .png}, or null for a name that
     *         is not a GregGuns icon
     */
    public static String iconPath(String name) {
        if (name == null || !name.startsWith(ICON_PREFIX) || name.length() != ICON_PREFIX.length() + 1) {
            return null;
        }
        CoinDenomination d = CoinDenomination.byMeta(name.charAt(ICON_PREFIX.length()) - '0');
        return d == null ? null : "items/" + d.iconName();
    }

    /** @return true when the patched machine passed the self-test and GregGuns trades are priced in the wallet */
    public static boolean inWallet() {
        return inWallet;
    }

    /**
     * Runs the self-test once the coin is registered (postInit, both sides) and logs the outcome. Does nothing without
     * the Vending Machine or the coin.
     */
    public static void check() {
        inWallet = false;
        if (!Loader.isModLoaded(VendingMachineCompat.MOD_ID) || CoinItems.coin() == null) {
            return;
        }
        String failure;
        try {
            failure = VendingCurrency.selfTest();
        } catch (RuntimeException | LinkageError e) {
            failure = e.toString();
        }
        inWallet = failure == null;
        if (inWallet) {
            GregGuns.LOG
                .info("The GregGuns Coin is a Vending Machine coin kind: GregGuns trades are paid from the wallet");
        } else {
            GregGuns.LOG.error(
                "The GregGuns Coin could not join the Vending Machine wallet ({}); GregGuns trades take the coins in the"
                    + " machine's input slots instead",
                failure);
        }
    }
}
