package com.gregguns.icbm.production;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.production.gc.MissileSchematicPage;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.IcbmParts;
import com.mojang.authlib.GameProfile;

import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import micdoodle8.mods.galacticraft.api.recipe.ISchematicPage;
import micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry;

/**
 * Headless scenarios of how a missile body is made: the chip, schematic and Assembly Line recipes are in GT's maps,
 * the NASA Workbench page is registered and unlocks with its schematic, and the page's container turns the right
 * slots into the right body (and nothing else). Server side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md
public final class ProductionScenarios {

    private ProductionScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Recipes());
        registry.register(new Bench());
    }

    private abstract static class Base implements Scenario {

        @Override
        public final ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off, so it registers no recipes or pages");
            }
            return go(ctx);
        }

        abstract ScenarioResult go(ScenarioContext ctx);
    }

    private static GTRecipe findByOutput(gregtech.api.recipe.RecipeMap<?> map, Item item, int damage) {
        for (GTRecipe r : map.getAllRecipes()) {
            if (r.mOutputs != null && r.mOutputs.length > 0
                && r.mOutputs[0] != null
                && r.mOutputs[0].getItem() == item
                && r.mOutputs[0].getItemDamage() == damage) {
                return r;
            }
        }
        return null;
    }

    private static final class Recipes extends Base {

        @Override
        public String name() {
            return "icbm.production.recipes";
        }

        @Override
        public String description() {
            return "Each missile chip is in the Circuit Assembler, each schematic in the Laser Engraver (lens not consumed), and each body (every guidance computer it takes) in the Assembly Line's and the workbench's NEI maps, with its guidance on the output, at its tier's voltage.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (MissileBuildSpec spec : MissileBuildSpec.values()) {
                long volts = gregtech.api.enums.TierEU.RECIPE_HV;
                volts = com.gregguns.recipe.ProductionRecipes.eutFor(spec.tier());
                GTRecipe chip = findByOutput(
                    RecipeMaps.circuitAssemblerRecipes,
                    ProductionItems.chipItem(),
                    spec.number() - 1);
                out.check(chip != null, spec + " chip recipe in the Circuit Assembler");
                if (chip != null) {
                    out.checkEquals(spec + " chip voltage", volts, (long) chip.mEUt);
                }
                GTRecipe sch = findByOutput(
                    RecipeMaps.laserEngraverRecipes,
                    ProductionItems.schematicItem(),
                    spec.number() - 1);
                out.check(sch != null, spec + " schematic recipe in the Laser Engraver");
                if (sch != null) {
                    out.checkEquals(spec + " engraver voltage", volts, (long) sch.mEUt);
                    out.check(
                        sch.mInputs.length == 2 && sch.mInputs[0].getItem() == ProductionItems.chipItem(),
                        spec + " schematic takes the chip first");
                }
                MissileTier tier = MissileTier.byNumber(spec.number());
                for (IcbmPart computer : spec.computers()) {
                    boolean found = false;
                    for (GTRecipe r : RecipeMaps.assemblylineVisualRecipes.getAllRecipes()) {
                        ItemStack o = r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
                        if (o != null && MissileBodies.tierOf(o) == tier
                            && MissileBodies.guidanceOf(o) == spec.guidanceOf(computer)) {
                            found = true;
                        }
                    }
                    out.check(found, spec + " body with " + computer + " in the Assembly Line map");
                    boolean bench = false;
                    for (GTRecipe r : IcbmRecipeMaps.MISSILE_WORKBENCH.getAllRecipes()) {
                        ItemStack o = r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
                        if (o != null && MissileBodies.tierOf(o) == tier
                            && MissileBodies.guidanceOf(o) == spec.guidanceOf(computer)) {
                            bench = true;
                        }
                    }
                    out.check(bench, spec + " body with " + computer + " in the workbench's NEI map");
                }
            }
            return out.build();
        }
    }

    private static final class Bench extends Base {

        @Override
        public String name() {
            return "icbm.production.bench";
        }

        @Override
        public String description() {
            return "The NASA Workbench pages are registered and unlock with their schematic; the page container makes the right body with the right guidance from the right slots, and nothing from a wrong one.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            EntityPlayer player = new FakePlayer(
                (WorldServer) ctx.world(),
                new GameProfile(java.util.UUID.nameUUIDFromBytes("gg-bench".getBytes()), "[GGBench]"));
            for (MissileBuildSpec spec : MissileBuildSpec.values()) {
                ISchematicPage page = SchematicRegistry.getMatchingRecipeForItemStack(ProductionItems.schematic(spec));
                out.check(page != null, spec + " page unlocks with its schematic");
                if (page == null) {
                    continue;
                }
                out.checkEquals(
                    spec + " page id",
                    MissileSchematicPage.PAGE_ID_FIRST + spec.number() - 1,
                    page.getPageID());
                out.checkEquals(
                    spec + " gui id",
                    MissileSchematicPage.GUI_ID_FIRST + spec.number() - 1,
                    page.getGuiID());
                // The other tier's schematic does not unlock this page.
                for (MissileBuildSpec other : MissileBuildSpec.values()) {
                    if (other != spec) {
                        out.check(
                            SchematicRegistry.getMatchingRecipeForItemStack(ProductionItems.schematic(other)) != page,
                            spec + " page must not open with the " + other + " schematic");
                    }
                }
                for (IcbmPart computer : spec.computers()) {
                    fill(out, player, spec, computer);
                }
            }
            return out.build();
        }

        private void fill(ScenarioResult.Builder out, EntityPlayer player, MissileBuildSpec spec, IcbmPart computer) {
            ContainerMissileBench c = new ContainerMissileBench(player.inventory, spec);
            int plates = MissileBenchRules.plateSlots(spec);
            ItemStack plate = IcbmIngredients.stack(spec.plates());
            ItemStack fin = IcbmIngredients.stack(spec.fins());
            for (int i = 0; i < plates; i++) {
                c.getSlot(1 + i)
                    .putStack(one(plate));
            }
            for (int i = 0; i < MissileBenchRules.FIN_SLOTS - 1; i++) {
                c.getSlot(1 + plates + i)
                    .putStack(one(fin));
            }
            int engine = 1 + plates + MissileBenchRules.FIN_SLOTS;
            c.getSlot(engine)
                .putStack(new ItemStack(IcbmParts.get(spec.engine())));
            c.getSlot(engine + 1)
                .putStack(new ItemStack(IcbmParts.get(computer)));
            c.onCraftMatrixChanged(null);
            String label = spec + "/" + computer;
            out.check(
                c.getSlot(ContainerMissileBench.RESULT)
                    .getStack() == null,
                label + ": three fins make no body");
            c.getSlot(1 + plates + 3)
                .putStack(one(fin));
            c.onCraftMatrixChanged(null);
            ItemStack result = c.getSlot(ContainerMissileBench.RESULT)
                .getStack();
            out.check(result != null, label + ": the full set makes a body");
            if (result == null) {
                return;
            }
            out.checkEquals(label + " tier", MissileTier.byNumber(spec.number()), MissileBodies.tierOf(result));
            out.checkEquals(label + " guidance", spec.guidanceOf(computer), MissileBodies.guidanceOf(result));
            // A slot only takes its own item.
            out.check(
                !c.getSlot(1)
                    .isItemValid(new ItemStack(IcbmParts.get(spec.engine()))),
                label + ": plate slot takes an engine");
            out.check(
                !c.getSlot(engine)
                    .isItemValid(plate),
                label + ": engine slot takes a plate");
            // Taking the result uses one of everything.
            c.getSlot(ContainerMissileBench.RESULT)
                .onPickupFromSlot(player, result);
            for (int i = 1; i <= engine + 1; i++) {
                out.check(
                    c.getSlot(i)
                        .getStack() == null,
                    label + ": slot " + i + " should be used up");
            }
            out.check(
                c.getSlot(ContainerMissileBench.RESULT)
                    .getStack() == null,
                label + ": result clears");
        }

        private static ItemStack one(ItemStack s) {
            ItemStack c = s.copy();
            c.stackSize = 1;
            return c;
        }
    }
}
