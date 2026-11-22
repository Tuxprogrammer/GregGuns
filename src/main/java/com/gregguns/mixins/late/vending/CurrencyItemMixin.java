package com.gregguns.mixins.late.vending;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.gregguns.compat.vending.VendingCoin;

/**
 * Teaches the Vending Machine the GregGuns Coin's value. The machine reads a coin's worth from its registry name (one
 * item per denomination, suffixes I to IV); the GregGuns Coin is one item with the denomination in its damage, so the
 * machine's own reading would count every GregGuns coin as 1. These two methods are the only places the machine turns
 * items into a sum and back: the coin slot, held-coin deposit, ME Uplink Hatch and coin sound all call
 * {@code fromItemStack}, and wallet and uplink payouts call {@code itemize}. The NEI and quest-book price icon
 * ({@code getItemRepresentation}) and the uplink's {@code isMatchingType} already work through the item prefix.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Wallet)
// Source (CurrencyItem, unchanged from 0.4.101 through 0.5.1):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/CurrencyItem.java#L34-L73
@Mixin(value = CurrencyItem.class, remap = false)
public abstract class CurrencyItemMixin {

    @Shadow
    public CurrencyType type;

    @Shadow
    public int value;

    @Inject(method = "fromItemStack", at = @At("HEAD"), cancellable = true)
    private static void gregguns$readCoin(ItemStack stack, CallbackInfoReturnable<CurrencyItem> cir) {
        if (stack == null || !VendingCoin.ITEM_PREFIX
            .equals(net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()))) {
            return;
        }
        long worth = VendingCoin.worth(stack);
        CurrencyType kind = CurrencyType.getTypeFromId(VendingCoin.CURRENCY_ID);
        cir.setReturnValue(
            kind == null || worth <= 0 || worth > Integer.MAX_VALUE ? null : new CurrencyItem(kind, (int) worth));
    }

    @Inject(method = "itemize", at = @At("HEAD"), cancellable = true)
    private void gregguns$payOut(CallbackInfoReturnable<List<ItemStack>> cir) {
        if (type != null && VendingCoin.CURRENCY_ID.equals(type.id)) {
            List<ItemStack> out = VendingCoin.itemize(value);
            // The machine's own itemize spends the value as it goes; leave nothing behind, as it does.
            value = 0;
            cir.setReturnValue(out);
        }
    }
}
