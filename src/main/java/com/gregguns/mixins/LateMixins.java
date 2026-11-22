package com.gregguns.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

/**
 * GregGuns' late mixins: patches to other mods' classes, queued once every mod is known. Each group is applied only
 * when its target mod is loaded. The config ({@code mixins.gregguns.late.json}) is not required, so a patch whose
 * target
 * moved in a newer version of that mod is logged and skipped, never a crash; the code that relies on a patch checks
 * that it took ({@link com.gregguns.compat.vending.VendingCoin#check()}). This class sits outside the mixin package
 * so the loader can load it.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Wallet)
@LateMixin
public final class LateMixins implements ILateMixinLoader {

    /** The Vending Machine's mod id (as {@code com.gregguns.compat.VendingMachineCompat.MOD_ID}). */
    static final String VENDING_MACHINE = "vendingmachine";

    @Override
    public String getMixinConfig() {
        return "mixins.gregguns.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        if (loadedMods.contains(VENDING_MACHINE)) {
            // The GregGuns Coin as a wallet coin kind (VendingCoin).
            mixins.add("vending.CurrencyTypeInvoker");
            mixins.add("vending.CurrencyTypeMixin");
            mixins.add("vending.CurrencyItemMixin");
        }
        return mixins;
    }
}
