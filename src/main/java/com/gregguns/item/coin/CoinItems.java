package com.gregguns.item.coin;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the GregGuns Coin and holds the registered item. The guns module registers it right after the gun parts
 * ({@code GunsItemRegistry}), matching its place in {@link com.gregguns.item.NeiOrder}. Common side, preInit.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md
public final class CoinItems {

    private static ItemCoin coin;

    private CoinItems() {}

    /**
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        coin = registrar.registerItem(ItemCoin.NAME, new ItemCoin());
    }

    /** @return the coin item, or {@code null} before registration or with the guns module off */
    public static ItemCoin coin() {
        return coin;
    }
}
