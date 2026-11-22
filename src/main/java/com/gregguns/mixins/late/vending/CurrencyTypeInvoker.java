package com.gregguns.mixins.late.vending;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.cubefury.vendingmachine.trade.CurrencyType;

/**
 * Calls the Vending Machine's {@code CurrencyType} enum constructor, so {@link CurrencyTypeMixin} can build one more
 * constant. An enum constructor takes the constant's name and ordinal first, then the declared {@code (id, itemPrefix,
 * textureName)}.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Wallet)
// Source (the constructor, unchanged from 0.4.101 through 0.5.1):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/CurrencyType.java#L46-L69
@Mixin(value = CurrencyType.class, remap = false)
public interface CurrencyTypeInvoker {

    /**
     * @param name        the constant's name
     * @param ordinal     its ordinal: the length of {@code values()} before it is added
     * @param id          the coin kind's id
     * @param itemPrefix  the coin item's registry name
     * @param textureName the icon name stem
     * @return the new constant
     */
    @Invoker("<init>")
    static CurrencyType gregguns$create(String name, int ordinal, String id, String itemPrefix, String textureName) {
        throw new AssertionError("CurrencyTypeInvoker was not applied");
    }
}
