package com.gregguns.recipe.recycling;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SubTag;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/**
 * The recycling scenarios. {@code recycling.coverage}: every GregGuns stack carries GT item data or is on
 * {@link RecyclingExclusions} (or owned by another branch), excluded stacks carry none, and no exclusion is stale.
 * {@code recycling.spot}: for guns, a plate-built part, an armor piece, machines, an ICBM item, a casing and a turret
 * plate, the Macerator and the Arc Furnace hold a recycling recipe with the outputs GT derives from the item data, and
 * two derived compositions match hand-worked figures. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/06-items/recycling.md (GGS-06-49 to GGS-06-55)
public final class RecyclingScenarios {

    private RecyclingScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Coverage());
        registry.register(new Spot());
    }

    /** {@code recycling.coverage}. */
    static final class Coverage implements Scenario {

        @Override
        public String name() {
            return "recycling.coverage";
        }

        @Override
        public String description() {
            return "Every GregGuns stack carries GT recycling data or is on the justified exclusion list.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Map<String, RecyclingDerivation.Entry> report = RecyclingDerivation.report();
            Map<String, ItemStack> catalog = RecyclingCatalog.stacks();
            out.check(!report.isEmpty(), "the recycling pass should have run at postload");
            out.checkEquals("catalog entries in the report", catalog.size(), report.size());

            Map<RecyclingDerivation.Status, Integer> counts = new EnumMap<>(RecyclingDerivation.Status.class);
            for (Map.Entry<String, ItemStack> entry : catalog.entrySet()) {
                String key = entry.getKey();
                RecyclingDerivation.Entry line = report.get(key);
                if (line == null) {
                    out.check(false, key + ": missing from the recycling report");
                    continue;
                }
                counts.merge(line.status(), 1, Integer::sum);
                GregGuns.LOG.info("recycling.coverage {} {} - {}", line.status(), key, line.detail());
                ItemData data = GTOreDictUnificator.getItemData(entry.getValue());
                switch (line.status()) {
                    case EXPLICIT:
                    case DERIVED:
                        out.check(
                            data != null && data.hasValidMaterialData() && data.mMaterial.mAmount > 0,
                            key + ": should carry GT item data with a material");
                        break;
                    case EXCLUDED:
                        out.check(
                            data == null || !data.hasValidMaterialData(),
                            key + ": is excluded ("
                                + line.detail()
                                + ") but carries "
                                + RecyclingDerivation.describe(data));
                        break;
                    case DELEGATED:
                        break;
                    default:
                        out.check(
                            false,
                            key + ": "
                                + line.status()
                                + " ("
                                + line.detail()
                                + "); give it data or a justified exclusion");
                }
            }
            for (String key : RecyclingExclusions.excluded()
                .keySet()) {
                boolean present = catalog.containsKey(key);
                for (String catalogKey : catalog.keySet()) {
                    present |= catalogKey.startsWith(key + "@");
                }
                out.check(present, "exclusion " + key + " names no GregGuns stack; remove it");
            }
            for (String prefix : RecyclingExclusions.excludedPrefixes()
                .keySet()) {
                boolean present = false;
                for (String catalogKey : catalog.keySet()) {
                    present |= catalogKey.startsWith(prefix);
                }
                out.check(present, "excluded family " + prefix + "* names no GregGuns stack; remove it");
            }
            for (Object name : Item.itemRegistry.getKeys()) {
                if (!((String) name).startsWith(GregGuns.MODID + ":")) {
                    continue;
                }
                Item item = (Item) Item.itemRegistry.getObject(name);
                String content = ((String) name).substring(GregGuns.MODID.length() + 1);
                boolean listed = RecyclingCatalog.listedSubtypes()
                    .contains(item.getClass())
                    || RecyclingExclusions.excluded()
                        .containsKey(content);
                out.check(
                    !item.getHasSubtypes() || listed,
                    name + " has subtypes the recycling catalog does not list; add its variants or exclude it whole");
            }
            out.note("recycling coverage: " + counts);
            return out.build();
        }
    }

    /** {@code recycling.spot}. */
    static final class Spot implements Scenario {

        /**
         * Catalog keys checked: guns (derived and hand-written), a plate-built part, an armor piece, machines (a GT
         * reversible crafting recipe's data and a derived one), an ICBM item, a casing and a turret plate.
         */
        static final String[] KEYS = { "ak47", "pistol_9mm", "part_barrel_iron", "t1_combat_helmet", "mte:20109",
            "mte:20121", "sam", "casings@8", "turret_armor_steel" };

        @Override
        public String name() {
            return "recycling.spot";
        }

        @Override
        public String description() {
            return "Guns, a part, armor, machines, an ICBM item and a casing have Macerator and Arc Furnace recycling.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Map<String, ItemStack> catalog = RecyclingCatalog.stacks();
            for (String key : KEYS) {
                ItemStack stack = catalog.get(key);
                out.check(stack != null, key + ": not a GregGuns stack in this run");
                if (stack == null) {
                    continue;
                }
                ItemData data = GTOreDictUnificator.getItemData(stack);
                out.check(data != null && data.hasValidMaterialData(), key + ": should carry GT item data");
                if (data == null || !data.hasValidMaterialData()) {
                    continue;
                }
                out.note(key + ": " + RecyclingDerivation.describe(data));
                checkMacerator(out, key, stack, data);
                checkArcFurnace(out, key, stack, data);
            }
            // Two derived items worked out by hand from their recipes, so the derivation itself is checked.
            long m = GTValues.M;
            // Steel Turret Armor: five steel plates in the Assembler, one plate out.
            checkComposition(out, catalog.get("turret_armor_steel"), Materials.Steel, 5 * m);
            // Steel Bastion Plating: one Steel Turret Armor and two concrete (no data) make four: 5/4 plate each.
            checkComposition(out, catalog.get("casings@8"), Materials.Steel, 5 * m / 4);
            return out.build();
        }

        private static void checkComposition(ScenarioResult.Builder out, ItemStack stack, Materials material,
            long amount) {
            ItemData data = stack == null ? null : GTOreDictUnificator.getItemData(stack);
            String key = RecyclingCatalog.keyOf(stack);
            out.check(data != null && data.hasValidMaterialData(), key + ": should carry derived data");
            if (data == null || !data.hasValidMaterialData()) {
                return;
            }
            out.checkEquals(key + " main material", material, data.mMaterial.mMaterial);
            out.checkEquals(key + " amount", amount, data.mMaterial.mAmount);
            out.checkEquals(key + " byproducts", 0, data.mByProducts.length);
        }

        /**
         * GT's Macerator maps each material to its macerate-into form, merges and sorts them, and returns the main one
         * and up to three byproducts as dust. The expectation is built the same way from the item data.
         */
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeRegistrator.java#L459-L497
        private static void checkMacerator(ScenarioResult.Builder out, String key, ItemStack stack, ItemData data) {
            GTRecipe recipe = find(RecipeMaps.maceratorRecipes, stack);
            out.check(recipe != null, key + ": the Macerator should have a recycling recipe");
            if (recipe == null) {
                return;
            }
            out.note(key + " Macerator: " + outputs(recipe));
            List<MaterialStack> mapped = new ArrayList<>();
            for (MaterialStack material : materials(data)) {
                MaterialStack copy = material.clone();
                copy.mMaterial = copy.mMaterial.mMacerateInto;
                mapped.add(copy);
            }
            ItemData merged = new ItemData(
                new ItemData(
                    mapped.get(0),
                    mapped.subList(1, mapped.size())
                        .toArray(new MaterialStack[0])));
            List<MaterialStack> expected = materials(merged);
            int checked = 0;
            for (int i = 0; i < Math.min(4, expected.size()); i++) {
                ItemStack dust = GTOreDictUnificator.getDust(expected.get(i));
                if (dust != null) {
                    checked++;
                    out.check(
                        contains(recipe, dust),
                        key + ": the Macerator should return " + dust.stackSize + " x " + dust.getDisplayName());
                }
            }
            out.check(checked > 0, key + ": no dust was expected from the Macerator");
        }

        /** GT's Arc Furnace recycling recipe returns the main metal as an ingot of its arc-smelt form. */
        private static void checkArcFurnace(ScenarioResult.Builder out, String key, ItemStack stack, ItemData data) {
            GTRecipe recipe = arcRecycling(stack);
            out.check(recipe != null, key + ": the Arc Furnace should have a recycling recipe");
            if (recipe != null) {
                out.note(key + " Arc Furnace: " + outputs(recipe));
            }
            if (recipe == null || !data.mMaterial.mMaterial.contains(SubTag.METAL)) {
                return;
            }
            MaterialStack main = data.mMaterial;
            ItemStack ingot = GTOreDictUnificator
                .getIngotOrDust(new MaterialStack(main.mMaterial.mSmeltInto.mArcSmeltInto, main.mAmount));
            out.check(
                ingot == null || contains(recipe, ingot),
                key + ": the Arc Furnace should return "
                    + (ingot == null ? "?" : ingot.stackSize + " x " + ingot.getDisplayName()));
        }

        private static GTRecipe find(RecipeMap<?> map, ItemStack stack) {
            return map.findRecipeQuery()
                .items(stack.copy())
                .voltage(TierEU.RECIPE_UV)
                .find();
        }

        /** The recycling variant of GT's universal Arc Furnace recipe: no gas, programmed circuit 1. */
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L344-L380
        static GTRecipe arcRecycling(ItemStack stack) {
            return RecipeMaps.arcFurnaceRecipes.findRecipeQuery()
                .items(stack.copy(), GTUtility.getIntegratedCircuit(1))
                .voltage(TierEU.RECIPE_UV)
                .find();
        }

        /** @return the recipe's outputs as {@code 4 x Bronze Dust, 3 x Steel Dust} */
        private static String outputs(GTRecipe recipe) {
            StringBuilder text = new StringBuilder();
            for (ItemStack output : recipe.mOutputs) {
                if (output != null) {
                    text.append(text.length() == 0 ? "" : ", ")
                        .append(output.stackSize)
                        .append(" x ")
                        .append(output.getDisplayName());
                }
            }
            return text.toString();
        }

        private static boolean contains(GTRecipe recipe, ItemStack want) {
            for (ItemStack output : recipe.mOutputs) {
                if (output != null && output.getItem() == want.getItem()
                    && output.getItemDamage() == want.getItemDamage()
                    && output.stackSize == want.stackSize) {
                    return true;
                }
            }
            return false;
        }

        private static List<MaterialStack> materials(ItemData data) {
            List<MaterialStack> all = new ArrayList<>();
            all.add(data.mMaterial);
            for (MaterialStack by : data.mByProducts) {
                all.add(by);
            }
            return all;
        }
    }
}
