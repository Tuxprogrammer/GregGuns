package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.core.config.ModuleFlags;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.recipe.Scanning;

/**
 * Registers the component recipes of {@link ComponentTable} on GT's Assembler, Chemical Bath and Assembly Line, and
 * the GT recycling data of every component the table makes.
 *
 * <p>
 * Timing: {@link #register()} queues the work on {@code GregTechAPI.sBeforeGTPostload}, as {@link ProductionRecipes}
 * does, so every GregGuns item and GT's own load are ready and GT's postload generators see the item data. A row whose
 * input does not resolve is skipped with an error in the log (the dev self-check then reports the component as having
 * no recipe); a row that needs a module that is off is skipped quietly. Common side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (component rows; GGS-06-34, GGS-06-35, GGS-06-43, GGS-06-46)
public final class ComponentRecipes {

    private ComponentRecipes() {}

    /** Queues the registration on GT's before-postload hook. Call from the guns module's init. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447
        GregTechAPI.sBeforeGTPostload.add(ComponentRecipes::addAll);
    }

    private static void addAll() {
        List<ComponentTable.Spec> specs = new ArrayList<>(ComponentTable.SPECS);
        specs.addAll(AmmoLineTable.SPECS);
        for (ComponentTable.Spec spec : specs) {
            if (spec.requiresModule() != null && !ModuleFlags.isEnabled(spec.requiresModule())) {
                GregGuns.LOG.info(
                    "Skipping recipe {}: module {} is off",
                    spec.row()
                        .id(),
                    spec.requiresModule()
                        .configKey());
                continue;
            }
            addSpec(spec);
        }
        addRecyclingData();
    }

    static void addSpec(ComponentTable.Spec spec) {
        ProductionTable.Row row = spec.row();
        ItemStack output = ProductionRecipes.modStack(row.output(), row.outputCount());
        if (output == null) {
            GregGuns.LOG.error("Skipping recipe {}: output {} is not registered", row.id(), row.output());
            return;
        }
        List<ItemStack> inputs = new ArrayList<>();
        for (ProductionTable.Input input : row.inputs()) {
            ItemStack stack = ProductionRecipes.resolve(input);
            if (stack == null) {
                GregGuns.LOG.error(
                    "Skipping recipe {}: input {} of {} did not resolve",
                    row.id(),
                    input.isGregGuns() ? input.itemName()
                        : input.special() != null ? input.special() : input.prefix() + "." + input.material(),
                    row.output());
                return;
            }
            inputs.add(stack);
        }
        List<FluidStack> fluids = new ArrayList<>();
        for (ComponentTable.FluidInput fluid : spec.fluids()) {
            FluidStack stack = fluidStack(fluid);
            if (stack == null) {
                GregGuns.LOG
                    .error("Skipping recipe {}: GT has no {} fluid for {}", row.id(), fluid.kind(), fluid.material());
                return;
            }
            fluids.add(stack);
        }
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L485-L506
        // (builder usage). Assembly Line: gregtech.loaders.postload.recipes.AssemblyLineRecipes, "LuV motor" block
        // (RESEARCH_ITEM + SCANNING metadata, addTo(AssemblyLine)).
        GTRecipeBuilder builder = GTValues.RA.stdBuilder();
        if (row.circuit() > 0) {
            builder.circuit(row.circuit());
        }
        builder.itemInputs(inputs.toArray(new ItemStack[0]))
            .itemOutputs(output)
            .duration(row.seconds() * GTRecipeBuilder.SECONDS)
            .eut(ProductionRecipes.eutFor(row.tier()));
        if (!fluids.isEmpty()) {
            builder.fluidInputs(fluids.toArray(new FluidStack[0]));
        }
        switch (row.target()) {
            case ASSEMBLER:
                builder.addTo(RecipeMaps.assemblerRecipes);
                break;
            case CHEMICAL_BATH:
                builder.addTo(RecipeMaps.chemicalBathRecipes);
                break;
            case ASSEMBLY_LINE:
                addAssemblyLine(builder, spec);
                break;
            default:
                GregGuns.LOG.error("Recipe {} has a target ComponentRecipes does not register", row.id());
        }
    }

    private static FluidStack fluidStack(ComponentTable.FluidInput fluid) {
        if (fluid.kind() == ComponentTable.FluidKind.REGISTRY) {
            // Source: Forge 1.7.10 net.minecraftforge.fluids.FluidRegistry.getFluidStack(String, int)
            return FluidRegistry.getFluidStack(fluid.material(), fluid.amount());
        }
        Materials material = Materials.get(fluid.material());
        return fluid.kind() == ComponentTable.FluidKind.MOLTEN ? material.getMolten(fluid.amount())
            : material.getFluid(fluid.amount());
    }

    private static void addAssemblyLine(GTRecipeBuilder builder, ComponentTable.Spec spec) {
        ComponentTable.Research research = spec.research();
        ItemStack researchItem = research == null ? null : ProductionRecipes.resolve(research.item());
        if (researchItem == null) {
            GregGuns.LOG.error(
                "Skipping recipe {}: its research item did not resolve",
                spec.row()
                    .id());
            return;
        }
        builder.metadata(GTRecipeConstants.RESEARCH_ITEM, researchItem)
            .metadata(
                GTRecipeConstants.SCANNING,
                new Scanning(
                    research.scanSeconds() * GTRecipeBuilder.SECONDS,
                    ProductionRecipes.eutFor(research.scanTier())))
            .addTo(GTRecipeConstants.AssemblyLine);
    }

    /**
     * Attaches recycling data to every component the table makes, from its recipe row. The five parts the M1 and M2
     * recipes own keep their own data (GT's {@code addItemData} does not overwrite).
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
    private static void addRecyclingData() {
        Map<String, ProductionTable.Row> rows = new LinkedHashMap<>(ProductionTable.byOutput());
        for (Map.Entry<String, ProductionTable.Row> entry : ComponentTable.byOutput()
            .entrySet()) {
            rows.putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (ProductionTable.Row row : ComponentTable.byOutput()
            .values()) {
            Map<String, Long> composition = ComponentComposition.perItem(
                row,
                rows,
                prefix -> OrePrefixes.getPrefix(prefix)
                    .getMaterialAmount());
            ItemStack stack = ProductionRecipes.modStack(row.output(), 1);
            if (composition.isEmpty() || stack == null) {
                continue;
            }
            List<MaterialStack> stacks = new ArrayList<>();
            for (Map.Entry<String, Long> entry : composition.entrySet()) {
                Materials material = Materials.get(entry.getKey());
                if (material != Materials._NULL) {
                    stacks.add(new MaterialStack(material, entry.getValue()));
                }
            }
            if (stacks.isEmpty()) {
                continue;
            }
            MaterialStack main = stacks.get(0);
            GTOreDictUnificator.addItemData(
                stack,
                new ItemData(
                    main,
                    stacks.subList(1, stacks.size())
                        .toArray(new MaterialStack[0])));
        }
    }
}
