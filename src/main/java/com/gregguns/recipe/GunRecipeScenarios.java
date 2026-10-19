package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.item.part.PartItems;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

/**
 * Recipe scenarios for the guns, as factories each gun's own scenario class registers: a gun's final assembly row is in
 * GT's assembler map ({@code RecipeMaps.assemblerRecipes}, which GT's NEI handler lists), matches its sheet inputs at
 * its tier, and is refused below it. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LV table, 9mm Pistol)
public final class GunRecipeScenarios {

    private GunRecipeScenarios() {}

    /**
     * The scenario every gun with an assembly row registers: the row is in GT's assembler map at its tier and
     * duration, findable at its voltage and not below it.
     *
     * @param name        the scenario name, for example {@code gun.revolver.recipe}
     * @param description what it checks, for the docs
     * @param row         the gun's assembly row
     * @return the scenario
     */
    public static Scenario assembly(String name, String description, ProductionTable.Row row) {
        return new GunAssemblerRecipe(name, description, row);
    }

    /** @return {@code gun.sawedoff.shells.recipe}: the shotgun shell's press recipe (RC-29) */
    public static Scenario shellPress() {
        return new ShellPressRecipe();
    }

    /** @return {@code gun.revolver.parts}: the Revolver's hand-made parts and machine-made barrel */
    public static Scenario revolverParts() {
        return new RevolverPartRecipes();
    }

    /** @return {@code gun.handcannon.recipe}: the Hand Cannon's crafting recipe and the stone bullet chain */
    public static Scenario handCannon() {
        return new HandCannonRecipeScenario();
    }

    private static ItemStack modStack(String name, int count) {
        return ProductionRecipes.modStack(name, count);
    }

    private static ItemStack gt(String prefix, String material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.getPrefix(prefix), Materials.get(material), count);
    }

    /** A gun row is in GT's assembler map at its tier and duration, findable at its voltage and not below it. */
    private static final class GunAssemblerRecipe implements Scenario {

        private final String name;
        private final String description;
        private final ProductionTable.Row row;

        GunAssemblerRecipe(String name, String description, ProductionTable.Row row) {
            this.name = name;
            this.description = description;
            this.row = row;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            List<ItemStack> inputList = new ArrayList<>();
            for (ProductionTable.Input input : row.inputs()) {
                inputList.add(
                    input.isGregGuns() ? modStack(input.itemName(), input.count())
                        : gt(input.prefix(), input.material(), input.count()));
            }
            inputList.add(GTUtility.getIntegratedCircuit(row.circuit()));
            for (ItemStack input : inputList) {
                out.check(input != null, "every sheet input should resolve");
            }
            ItemStack[] inputs = inputList.toArray(new ItemStack[0]);
            ItemStack gun = modStack(row.output(), 1);

            boolean listed = false;
            for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (recipe.mOutputs != null && recipe.mOutputs.length > 0
                    && recipe.mOutputs[0] != null
                    && gun != null
                    && recipe.mOutputs[0].getItem() == gun.getItem()) {
                    listed = true;
                    out.checkEquals("EU/t", (int) ProductionRecipes.eutFor(row.tier()), recipe.mEUt);
                    out.checkEquals("duration (ticks)", row.seconds() * GTRecipeBuilder.SECONDS, recipe.mDuration);
                }
            }
            out.check(listed, "RecipeMaps.assemblerRecipes should hold the recipe (NEI lists that map)");

            GTRecipe found = RecipeMaps.assemblerRecipes.findRecipeQuery()
                .items(inputs)
                .voltage(
                    GTValues.V[row.tier()
                        .voltageIndex()])
                .find();
            out.check(
                found != null,
                "an assembler of the row's tier with the sheet inputs and circuit should find the recipe");
            GTRecipe tooLow = RecipeMaps.assemblerRecipes.findRecipeQuery()
                .items(inputs)
                .voltage(
                    GTValues.V[row.tier()
                        .voltageIndex() - 1] / 2)
                .find();
            out.check(
                tooLow == null,
                "a machine below the row's tier should not be offered the recipe, found " + (tooLow == null ? ""
                    : java.util.Arrays.toString(tooLow.mOutputs) + " / "
                        + java.util.Arrays.toString(tooLow.mInputs)
                        + " / "
                        + java.util.Arrays.toString(tooLow.mFluidOutputs)
                        + " at "
                        + tooLow.mEUt
                        + " EU/t"));
            return out.build();
        }
    }

    /** The Revolver's hand-made parts have crafting-table recipes, and its machine-made barrel an assembler row. */
    private static final class RevolverPartRecipes implements Scenario {

        @Override
        public String name() {
            return "gun.revolver.parts";
        }

        @Override
        public String description() {
            return "The Wooden Stock and Mechanical Parts have crafting-table recipes and the Iron Barrel an LV assembler recipe, so the revolver can be built from raw GT parts.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(craftable(PartItems.STOCK_WOODEN), "no crafting recipe makes the Wooden Stock");
            out.check(craftable(PartItems.MECHANICAL_1), "no crafting recipe makes Mechanical Parts");
            ItemStack barrel = modStack(PartItems.BARREL_IRON, 1);
            boolean listed = false;
            for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (recipe.mOutputs != null && recipe.mOutputs.length > 0
                    && recipe.mOutputs[0] != null
                    && barrel != null
                    && recipe.mOutputs[0].getItem() == barrel.getItem()) {
                    listed = true;
                    out.checkEquals("iron barrel EU/t", (int) TierEU.RECIPE_LV, recipe.mEUt);
                }
            }
            out.check(listed, "the assembler map should hold the Iron Barrel recipe");
            return out.build();
        }

        private static boolean craftable(String registryName) {
            ItemStack wanted = modStack(registryName, 1);
            if (wanted == null) {
                return false;
            }
            for (Object recipe : CraftingManager.getInstance()
                .getRecipeList()) {
                ItemStack result = ((IRecipe) recipe).getRecipeOutput();
                if (result != null && result.getItem() == wanted.getItem()) {
                    return true;
                }
            }
            return false;
        }
    }

    /** RC-29: the LV Ammo Press makes 8 shotgun shells from brass foil, lead and gunpowder, and not below LV. */
    private static final class ShellPressRecipe implements Scenario {

        @Override
        public String name() {
            return "gun.sawedoff.shells.recipe";
        }

        @Override
        public String description() {
            return "The ammo map finds RC-29 (8 foil Brass, 24 round Lead, 4 Gunpowder) at RECIPE_LV for 6 s making 8 shotgun shells, and not below LV.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemStack[] inputs = { gt("foil", "Brass", 8), gt("round", "Lead", 24), gt("dust", "Gunpowder", 4) };
            GTRecipe found = GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                .items(inputs)
                .voltage(GTValues.V[1])
                .find();
            out.check(found != null, "RC-29 should be found with the sheet inputs at LV");
            if (found != null) {
                out.checkEquals("RC-29 EU/t", (int) TierEU.RECIPE_LV, found.mEUt);
                out.checkEquals("RC-29 duration (ticks)", 6 * GTRecipeBuilder.SECONDS, found.mDuration);
                out.checkEquals("RC-29 output count", 8, found.mOutputs[0].stackSize);
                ItemStack shell = modStack(ProductionTable.SHELL, 1);
                out.check(
                    shell != null && found.mOutputs[0].getItem() == shell.getItem(),
                    "RC-29 should make the shotgun shell");
            }
            GTRecipe below = GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                .items(inputs)
                .voltage(GTValues.V[0])
                .find();
            out.check(below == null, "RC-29 should not run below LV");
            return out.build();
        }
    }
}
