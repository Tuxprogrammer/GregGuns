package com.gregguns.compat.vending;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.gregguns.item.coin.CoinDenomination;

class VendingCoinTest {

    @Test
    void theCoinKindIsNamedForGregGuns() {
        assertEquals("gregguns", VendingCoin.CURRENCY_ID);
        assertEquals("GREGGUNS", VendingCoin.ENUM_NAME);
        assertEquals("gregguns:coin", VendingCoin.ITEM_PREFIX, "the machine's isMatchingType compares this prefix");
    }

    @Test
    void theMachinesIconNamesMapToTheCoinTextures() {
        // The machine asks for icons/<texture>0 (worth 1) to icons/<texture>4 (worth 10,000).
        CoinDenomination[] all = CoinDenomination.values();
        for (int i = 0; i < all.length; i++) {
            assertEquals("items/" + all[i].iconName(), VendingCoin.iconPath("icons/gregguns" + i));
        }
        assertNull(VendingCoin.iconPath("icons/gregguns5"));
        assertNull(VendingCoin.iconPath("icons/gregguns"));
        assertNull(VendingCoin.iconPath("icons/gregguns10"));
        assertNull(VendingCoin.iconPath("icons/technician0"), "the pack's coins keep their own icons");
        assertNull(VendingCoin.iconPath("background/small0"), "the shared backgrounds stay the pack's");
        assertNull(VendingCoin.iconPath(null));
    }

    @Test
    void notInTheWalletUntilTheSelfTestPasses() {
        assertFalse(VendingCoin.inWallet());
    }
}
