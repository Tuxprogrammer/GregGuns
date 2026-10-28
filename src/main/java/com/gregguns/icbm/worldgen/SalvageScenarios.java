package com.gregguns.icbm.worldgen;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.missile.ItemBlockMissileBody;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.production.MissileBuildSpec;
import com.gregguns.icbm.recipe.IcbmParts;

import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/**
 * The damaged T1 body found in silo chests is salvage: it recycles for the metal of its plates, as a body does, and
 * it is not a body in any recipe (the I3 rubble lesson: nothing free). Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Salvage), D186
public final class SalvageScenarios {

    private SalvageScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Salvage());
    }

    private static final class Salvage implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.salvage";
        }

        @Override
        public String description() {
            return "A damaged T1 body is flagged salvage, recycles for no more than an intact body (the plates' metal),"
                + " and no Assembler guidance fit takes it as a body, so it never becomes a free guided missile body.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemStack damaged = SiloBonusItems.stack(Placement.Bonus.DAMAGED_BODY, null);
            ItemStack intact = MissileBodies.body(MissileTier.T1);
            out.check(damaged != null && ItemBlockMissileBody.isSalvage(damaged), "the damaged body carries its flag");
            out.check(!ItemBlockMissileBody.isSalvage(intact), "an intact body is not salvage");
            if (damaged == null) {
                return out.build();
            }
            // Recycling data is per item, so the damaged body is worth the plates of a body and no more.
            out.check(
                GTOreDictUnificator.getItemData(damaged) != null,
                "the damaged body has recycling data (it is salvage)");
            out.check(
                String.valueOf(GTOreDictUnificator.getItemData(damaged))
                    .equals(String.valueOf(GTOreDictUnificator.getItemData(intact))),
                "the damaged body recycles as an intact body does, not for more");
            // Not a body in a recipe: every guidance fit, at every voltage.
            MissileBuildSpec spec = MissileBuildSpec.byNumber(1);
            int fits = 0;
            for (com.gregguns.icbm.recipe.IcbmPart computer : spec.computers()) {
                Item c = IcbmParts.get(computer);
                if (c == null) {
                    continue;
                }
                ItemStack comp = new ItemStack(c);
                GTRecipe realFit = RecipeMaps.assemblerRecipes.findRecipeQuery()
                    .items(intact, comp, GTUtility.getIntegratedCircuit(spec.fitCircuit()))
                    .voltage(TierEU.RECIPE_UV)
                    .find();
                out.check(realFit != null, "the intact body has a guidance fit with " + computer);
                GTRecipe fit = RecipeMaps.assemblerRecipes.findRecipeQuery()
                    .items(damaged, comp, GTUtility.getIntegratedCircuit(spec.fitCircuit()))
                    .voltage(TierEU.RECIPE_UV)
                    .find();
                if (fit != null) {
                    out.note("damage " + damaged.getItemDamage() + " matched input " + fit.mInputs[0] + " damage "
                        + fit.mInputs[0].getItemDamage() + " out " + fit.mOutputs[0]);
                }
                out.check(fit == null, "a damaged body must not take the guidance fit with " + computer);
                fits++;
            }
            out.check(fits > 0, "a T1 guidance computer exists to test with");
            return out.build();
        }
    }
}
