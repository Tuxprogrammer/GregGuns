package com.gregguns.item.coin;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;

/**
 * The coin exchange, both ways. Change: one coin breaks into ten of the next smaller denomination in a crafting grid,
 * so
 * a player can always pay a Vending Machine price exactly. Exchange: ten coins and programmed circuit 10 make one of
 * the
 * next larger in the GT Assembler, the pack's own shape for its coins, at {@value #EXCHANGE_EUT} EU/t so any Assembler
 * runs it. Common side, init.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md
// Source (pack coin exchange: 10 coins + circuit 10 in the Assembler, 5 s):
// https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/gthandler/recipes/AssemblerRecipes.java#L7794-L7811
public final class CoinRecipes {

    /** The programmed circuit of the exchange recipes, the pack's coin circuit. */
    public static final int CIRCUIT = 10;

    /** EU/t of the exchange: ULV, so the recipe fits the coin's Steam tier in the D1 tier check. */
    public static final int EXCHANGE_EUT = 7;

    private CoinRecipes() {}

    /** Adds the change and exchange recipes; does nothing when the coin is not registered. */
    public static void register() {
        ItemCoin coin = CoinItems.coin();
        if (coin == null) {
            return;
        }
        try {
            for (CoinDenomination d : CoinDenomination.values()) {
                CoinDenomination smaller = d.previous();
                if (smaller != null) {
                    GameRegistry.addShapelessRecipe(coin.stack(smaller, CoinDenomination.STEP), coin.stack(d, 1));
                }
                CoinDenomination larger = d.next();
                if (larger != null) {
                    GTValues.RA.stdBuilder()
                        .itemInputs(coin.stack(d, CoinDenomination.STEP))
                        .circuit(CIRCUIT)
                        .itemOutputs(coin.stack(larger, 1))
                        .duration(5 * SECONDS)
                        .eut(EXCHANGE_EUT)
                        .addTo(RecipeMaps.assemblerRecipes);
                }
            }
        } catch (RuntimeException e) {
            GregGuns.LOG.error("GregGuns Coin recipes failed", e);
        }
    }
}
