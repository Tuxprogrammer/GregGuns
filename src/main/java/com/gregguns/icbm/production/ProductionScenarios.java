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

import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import micdoodle8.mods.galacticraft.api.recipe.ISchematicPage;
import micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry;

/**
 * Headless scenarios of how a missile body is made: the chip, schematic and body recipes are in GT's maps (the body in
 * the Assembler below LuV and on the Assembly Line from LuV up, D281),
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
            return "Each missile chip is in the Circuit Assembler, each schematic in the Laser Engraver (lens not consumed), each tier's one guidance-0 body recipe in the Assembler below LuV (found by GT's lookup from its own inputs, no Assembly Line recipe) or on the Assembly Line from LuV (reachable by GT's scanner from its own research item), each guidance fit in the Assembler, and every body and computer in the workbench's NEI map, at its tier's voltage; no GregGuns Assembly Line recipe runs below LuV.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (MissileBuildSpec spec : MissileBuildSpec.values()) {
                long volts = com.gregguns.recipe.ProductionRecipes.eutFor(spec.tier());
                GTRecipe chip = findByOutput(RecipeMaps.circuitAssemblerRecipes, ProductionItems.chipItem(spec), 0);
                out.check(chip != null, spec + " chip recipe in the Circuit Assembler");
                if (chip != null) {
                    out.checkEquals(spec + " chip voltage", volts, (long) chip.mEUt);
                }
                GTRecipe sch = findByOutput(RecipeMaps.laserEngraverRecipes, ProductionItems.schematicItem(spec), 0);
                out.check(sch != null, spec + " schematic recipe in the Laser Engraver");
                if (sch != null) {
                    out.checkEquals(spec + " engraver voltage", volts, (long) sch.mEUt);
                    out.check(
                        sch.mInputs.length == 2 && sch.mInputs[0].getItem() == ProductionItems.chipItem(spec),
                        spec + " schematic takes the chip first");
                }
                MissileTier tier = MissileTier.byNumber(spec.number());
                // The guidance-0 body: one Assembly Line recipe from LuV up (GT's scanner reaches exactly it), none
                // below LuV (D281).
                int lines = 0;
                for (GTRecipe r : RecipeMaps.assemblylineVisualRecipes.getAllRecipes()) {
                    ItemStack o = r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
                    if (o != null && MissileBodies.tierOf(o) == tier) {
                        lines++;
                        out.checkEquals(spec + " line body guidance", 0, MissileBodies.guidanceOf(o));
                        out.checkEquals(spec + " line voltage", volts, (long) r.mEUt);
                    }
                }
                out.checkEquals(spec + " Assembly Line recipes", spec.onAssemblyLine() ? 1 : 0, lines);
                if (spec.onAssemblyLine()) {
                    ItemStack research = MissileBodies.body(tier, 0);
                    gregtech.api.util.GTScannerResult scan = gregtech.loaders.postload.ScannerHandlerLoader
                        .doAssemblyLineResearch(null, research, ItemList.Tool_DataStick.get(1), null);
                    boolean reaches = scan instanceof gregtech.api.util.GTScannerResult.ALScannerResult
                        && ((gregtech.api.util.GTScannerResult.ALScannerResult) scan).alRecipe != null
                        && MissileBodies.tierOf(
                            ((gregtech.api.util.GTScannerResult.ALScannerResult) scan).alRecipe.mOutput) == tier;
                    out.check(reaches, spec + " research item reaches its own recipe in GT's scanner");
                } else {
                    assemblerBody(out, spec, tier, volts);
                }
                for (IcbmPart computer : spec.computers()) {
                    int g = spec.guidanceOf(computer);
                    boolean fit = false;
                    for (GTRecipe r : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                        ItemStack o = r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
                        if (o != null && MissileBodies.tierOf(o) == tier && MissileBodies.guidanceOf(o) == g && g > 0) {
                            fit = true;
                            out.checkEquals(spec + " fit voltage", volts, (long) r.mEUt);
                        }
                    }
                    out.check(g == 0 || fit, spec + " Assembler fit of " + computer + " (guidance " + g + ")");
                    boolean bench = false;
                    for (GTRecipe r : IcbmRecipeMaps.MISSILE_WORKBENCH.getAllRecipes()) {
                        ItemStack o = r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
                        if (o != null && MissileBodies.tierOf(o) == tier && MissileBodies.guidanceOf(o) == g) {
                            bench = true;
                        }
                    }
                    out.check(bench, spec + " body with " + computer + " in the workbench's NEI map");
                }
            }
            noLineRecipeBelowLuv(out);
            return out.build();
        }

        /**
         * One Assembler recipe makes the guidance-0 body of a tier below LuV, at the tier's voltage, and GT's lookup
         * on exactly its inputs returns it (no other Assembler recipe takes a subset of them first).
         */
        private static void assemblerBody(ScenarioResult.Builder out, MissileBuildSpec spec, MissileTier tier,
            long volts) {
            GTRecipe body = null;
            int found = 0;
            for (GTRecipe r : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                ItemStack o = r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
                if (o != null && MissileBodies.tierOf(o) == tier && MissileBodies.guidanceOf(o) == 0) {
                    found++;
                    body = r;
                }
            }
            out.checkEquals(spec + " Assembler body recipes", 1, found);
            if (body == null) {
                return;
            }
            out.checkEquals(spec + " Assembler body voltage", volts, (long) body.mEUt);
            out.checkEquals(spec + " Assembler body ticks", spec.bodyTicks(), body.mDuration);
            boolean lube = body.mFluidInputs != null && body.mFluidInputs.length == 1
                && body.mFluidInputs[0] != null
                && body.mFluidInputs[0].amount == MissileBuildSpec.LUBRICANT_LITERS;
            out.check(lube, spec + " Assembler body takes the lubricant");
            if (!lube) {
                return;
            }
            ItemStack[] in = new ItemStack[] { IcbmIngredients.stack(spec.plates()), IcbmIngredients.stack(spec.fins()),
                IcbmIngredients.stack(spec.engine()), IcbmIngredients.stack(spec.bodyCircuits()) };
            GTRecipe looked = RecipeMaps.assemblerRecipes.findRecipeQuery()
                .items(in)
                .fluids(body.mFluidInputs[0].copy())
                .voltage(volts)
                .find();
            out.check(looked == body, spec + " GT's Assembler lookup on the body inputs finds the body recipe");
        }

        /** Every Assembly Line recipe with a GregGuns output runs at LuV or above (D281). */
        private static void noLineRecipeBelowLuv(ScenarioResult.Builder out) {
            long luv = com.gregguns.recipe.ProductionRecipes.eutFor(MissileBuildSpec.ASSEMBLY_LINE_TIER);
            int ours = 0;
            for (GTRecipe r : RecipeMaps.assemblylineVisualRecipes.getAllRecipes()) {
                ItemStack o = r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
                if (o == null || o.getItem() == null) {
                    continue;
                }
                cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier id = cpw.mods.fml.common.registry.GameRegistry
                    .findUniqueIdentifierFor(o.getItem());
                if (id != null && com.gregguns.GregGuns.MODID.equals(id.modId)) {
                    ours++;
                    out.check(r.mEUt >= luv, o.getUnlocalizedName() + " Assembly Line recipe runs at LuV or above");
                }
            }
            out.check(ours > 0, "at least one GregGuns Assembly Line recipe is listed (the check is not vacuous)");
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
                .putStack(one(IcbmIngredients.stack(spec.engine())));
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
                    .isItemValid(IcbmIngredients.stack(spec.engine())),
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
