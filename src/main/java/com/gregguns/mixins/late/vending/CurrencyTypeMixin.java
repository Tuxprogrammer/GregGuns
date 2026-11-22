package com.gregguns.mixins.late.vending;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.gregguns.GregGuns;
import com.gregguns.compat.vending.VendingCoin;

/**
 * Adds the GregGuns Coin to the Vending Machine's coin kinds ({@link VendingCoin}). At the end of the enum's static
 * initializer, before anything can read {@code values()}, it builds one more constant and appends it to the values
 * array. Nothing in the machine reads an ordinal or switches on the enum, wallets save the id, and its coin display has
 * room for 16 kinds (14 used). The constructor registers the id with {@code CurrencyItem}.
 *
 * <p>
 * The machine loads every coin icon from the pack core mod's textures; for ours, {@link VendingCoin#iconPath} points
 * at the GregGuns Coin's own item textures (16 x 16, like the pack's).
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Wallet)
// Source (the enum, createCoinUITexture and the 4 x 4 coin display; unchanged from 0.4.101 through 0.5.1):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/CurrencyType.java#L12-L78
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/blocks/gui/MTEVendingMachineGui.java#L148-L149
@Mixin(value = CurrencyType.class, remap = false)
public abstract class CurrencyTypeMixin {

    @Shadow
    @Final
    @Mutable
    private static CurrencyType[] $VALUES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void gregguns$addCoin(CallbackInfo ci) {
        CurrencyType[] old = $VALUES;
        CurrencyType[] grown = Arrays.copyOf(old, old.length + 1);
        grown[old.length] = CurrencyTypeInvoker.gregguns$create(
            VendingCoin.ENUM_NAME,
            old.length,
            VendingCoin.CURRENCY_ID,
            VendingCoin.ITEM_PREFIX,
            VendingCoin.TEXTURE_NAME);
        $VALUES = grown;
    }

    @Inject(method = "createCoinUITexture", at = @At("HEAD"), cancellable = true)
    private static void gregguns$coinIcon(String name, CallbackInfoReturnable<UITexture> cir) {
        String path = VendingCoin.iconPath(name);
        if (path != null) {
            cir.setReturnValue(
                UITexture.builder()
                    .location(GregGuns.MODID, path)
                    .imageSize(16, 16)
                    .name("VM_UI_Coin_" + name.replace('/', '_'))
                    .build());
        }
    }
}
