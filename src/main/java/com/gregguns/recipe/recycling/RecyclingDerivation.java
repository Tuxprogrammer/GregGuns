package com.gregguns.recipe.recycling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.gregguns.GregGuns;

import gregtech.api.GregTechAPI;
import gregtech.api.enchants.EnchantmentRadioactivity;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SubTag;
import gregtech.api.enums.TierEU;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/**
 * Gives every GregGuns item, block and machine that has no GT recycling data yet the data GT would give it: the
 * content of the cheapest recipe that makes it ({@link RecyclingSolver}), attached with
 * {@code GTOreDictUnificator.addItemData}, which makes GT generate the Macerator, Arc Furnace and other recycling
 * recipes itself. Items that already carry data (the hand-written armor, gun, ammo and ICBM entries) keep it, because
 * {@code addItemData} never overwrites an explicit composition and this pass only looks at items with none. Items on
 * {@link RecyclingExclusions} are left alone. Common side.
 *
 * <p>
 * Timing: GT's after-postload hook. Every GregGuns recipe is queued on the before-postload hook, and GT has flushed its
 * buffered crafting recipes (ours included) into the crafting manager just before this hook runs, so every recipe that
 * makes a GregGuns item exists here. GT's load has started, so each {@code addItemData} registers its recycling recipes
 * at once.
 */
// Spec: docs/gregguns-spec/06-items/recycling.md (GGS-06-49 to GGS-06-55)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L576-L591
// (postload flushes the buffered crafting recipes, then runs sAfterGTPostload)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L353-L360
// (addItemData keeps an explicit composition)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L406-L445
// (processRecycling registers the recycling recipes at once after load start)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeRegistrator.java#L184-L210
// (registerMaterialRecycling: Macerator, smelting, Fluid Extractor and Arc Furnace from one ItemData)
public final class RecyclingDerivation {

    /** What happened to one catalog entry. */
    public enum Status {
        /** It carried GT item data before this pass (hand-written, or a GT reversible crafting recipe). */
        EXPLICIT,
        /** This pass derived and attached its data. */
        DERIVED,
        /** On the exclusion list; no data on purpose. */
        EXCLUDED,
        /** Another branch owns its data (see {@link RecyclingExclusions#delegatedReason}). */
        DELEGATED,
        /** No recipe makes it, and it is not excluded. */
        NO_RECIPE,
        /** Its cheapest recipe holds no material GT can return. */
        NOTHING_RECOVERED,
        /** A recycling recipe for it existed already, so no data was attached. */
        CLASH
    }

    /** One report line. */
    public static final class Entry {

        private final Status status;
        private final String detail;

        Entry(Status status, String detail) {
            this.status = status;
            this.detail = detail;
        }

        /** @return what happened */
        public Status status() {
            return status;
        }

        /** @return the recipe, the exclusion reason or the composition, for the log and the scenario */
        public String detail() {
            return detail;
        }
    }

    private static final Map<String, Entry> REPORT = new TreeMap<>();

    private RecyclingDerivation() {}

    /** Queues the pass on GT's after-postload hook. Call once, from init. */
    public static void register() {
        GregTechAPI.sAfterGTPostload.add(RecyclingDerivation::run);
    }

    /** @return the report of the pass, by catalog key; empty before it ran */
    public static Map<String, Entry> report() {
        return Collections.unmodifiableMap(REPORT);
    }

    static void run() {
        REPORT.clear();
        Map<String, ItemStack> catalog = RecyclingCatalog.stacks();
        Map<String, ItemStack> pending = new TreeMap<>();
        for (Map.Entry<String, ItemStack> entry : catalog.entrySet()) {
            String key = entry.getKey();
            ItemStack stack = entry.getValue();
            String excluded = RecyclingExclusions.reason(key);
            String delegated = RecyclingExclusions.delegatedReason(key);
            if (excluded != null) {
                REPORT.put(key, new Entry(Status.EXCLUDED, excluded));
            } else if (delegated != null) {
                REPORT.put(key, new Entry(Status.DELEGATED, delegated));
            } else if (GTOreDictUnificator.getItemData(stack) != null) {
                REPORT.put(key, new Entry(Status.EXPLICIT, describe(GTOreDictUnificator.getItemData(stack))));
            } else {
                pending.put(key, stack);
            }
        }

        Map<String, Materials> materials = new HashMap<>();
        Map<String, List<RecyclingSolver.Recipe>> recipes = new TreeMap<>();
        for (String key : pending.keySet()) {
            recipes.put(key, new ArrayList<>());
        }
        collectCrafting(pending, recipes, materials);
        collectMachines(pending, recipes, materials);
        collectAssemblyLine(pending, recipes, materials);

        Map<String, RecyclingSolver.Derived> derived = RecyclingSolver
            .solve(recipes, name -> keep(materials.get(name)));
        int attached = 0;
        for (Map.Entry<String, ItemStack> entry : pending.entrySet()) {
            String key = entry.getKey();
            RecyclingSolver.Derived result = derived.get(key);
            if (result == null) {
                REPORT.put(key, new Entry(Status.NO_RECIPE, "no recipe makes it"));
                continue;
            }
            if (result.composition()
                .isEmpty()) {
                REPORT.put(key, new Entry(Status.NOTHING_RECOVERED, result.source()));
                continue;
            }
            ItemStack stack = entry.getValue();
            if (hasRecyclingRecipe(stack)) {
                REPORT.put(key, new Entry(Status.CLASH, "a Macerator or Arc Furnace recipe already takes it"));
                continue;
            }
            ItemData data = itemData(result.composition(), materials);
            data = withRadiationMarker(key, data);
            GTOreDictUnificator.addItemData(stack, data);
            REPORT.put(key, new Entry(Status.DERIVED, result.source() + ": " + describe(data)));
            attached++;
        }
        logSummary(attached);
    }

    // ---- recipe collection ----

    private static void collectCrafting(Map<String, ItemStack> pending, Map<String, List<RecyclingSolver.Recipe>> out,
        Map<String, Materials> materials) {
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            IRecipe recipe = (IRecipe) entry;
            ItemStack output = recipe.getRecipeOutput();
            String key = output == null ? null : keyOf(output, pending);
            if (key == null) {
                continue;
            }
            List<Object> inputs = craftingInputs(recipe);
            if (inputs == null) {
                continue; // a special recipe class with no readable inputs (dyeing, NBT copies)
            }
            List<RecyclingSolver.Input> parsed = new ArrayList<>();
            for (Object input : inputs) {
                if (input instanceof ItemStack) {
                    parsed.add(input((ItemStack) input, 1, pending, materials));
                } else if (input instanceof List && !((List<?>) input).isEmpty()
                    && ((List<?>) input).get(0) instanceof ItemStack) {
                        parsed.add(oreInput((List<?>) input, pending, materials));
                    }
            }
            out.get(key)
                .add(new RecyclingSolver.Recipe("crafting", parsed, Math.max(1, output.stackSize)));
        }
    }

    /** @return the inputs (ItemStack, ore list or null) of a readable recipe class, or null for any other class */
    private static List<Object> craftingInputs(IRecipe recipe) {
        List<Object> inputs = new ArrayList<>();
        if (recipe instanceof ShapedOreRecipe) {
            Collections.addAll(inputs, ((ShapedOreRecipe) recipe).getInput());
        } else if (recipe instanceof ShapelessOreRecipe) {
            inputs.addAll(((ShapelessOreRecipe) recipe).getInput());
        } else if (recipe instanceof ShapedRecipes) {
            Collections.addAll(inputs, (Object[]) ((ShapedRecipes) recipe).recipeItems);
        } else if (recipe instanceof ShapelessRecipes) {
            inputs.addAll(((ShapelessRecipes) recipe).recipeItems);
        } else {
            return null;
        }
        return inputs;
    }

    private static void collectMachines(Map<String, ItemStack> pending, Map<String, List<RecyclingSolver.Recipe>> out,
        Map<String, Materials> materials) {
        for (Map.Entry<String, RecipeMap<?>> map : new TreeMap<>(RecipeMap.ALL_RECIPE_MAPS).entrySet()) {
            for (GTRecipe recipe : map.getValue()
                .getAllRecipes()) {
                if (recipe.mFakeRecipe || recipe.mOutputs == null || recipe.mInputs == null) {
                    continue;
                }
                String key = soleOutput(recipe, pending);
                if (key == null) {
                    continue;
                }
                int count = 0;
                for (ItemStack output : recipe.mOutputs) {
                    count += output == null ? 0 : output.stackSize;
                }
                List<RecyclingSolver.Input> parsed = new ArrayList<>();
                for (ItemStack input : recipe.mInputs) {
                    // Stack size 0 marks an input the machine keeps (a mold, a lens, a programmed circuit).
                    if (input != null && input.stackSize > 0) {
                        parsed.add(input(input, input.stackSize, pending, materials));
                    }
                }
                out.get(key)
                    .add(new RecyclingSolver.Recipe(shortName(map.getKey()), parsed, Math.max(1, count)));
            }
        }
    }

    /** Assembly Line recipes live in their own list; the visual map's copies are marked fake. */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipe.java#L1079-L1110
    // (RecipeAssemblyLine: mInputs, with mOreDictAlt holding the alternatives of an ore-dictionary slot, and mOutput)
    private static void collectAssemblyLine(Map<String, ItemStack> pending,
        Map<String, List<RecyclingSolver.Recipe>> out, Map<String, Materials> materials) {
        for (GTRecipe.RecipeAssemblyLine recipe : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            String key = recipe.mOutput == null ? null : keyOf(recipe.mOutput, pending);
            if (key == null || recipe.mInputs == null) {
                continue;
            }
            List<RecyclingSolver.Input> parsed = new ArrayList<>();
            for (int i = 0; i < recipe.mInputs.length; i++) {
                ItemStack input = recipe.mInputs[i];
                if (input == null && recipe.mOreDictAlt != null
                    && i < recipe.mOreDictAlt.length
                    && recipe.mOreDictAlt[i] != null
                    && recipe.mOreDictAlt[i].length > 0) {
                    input = recipe.mOreDictAlt[i][0];
                }
                if (input != null && input.stackSize > 0) {
                    parsed.add(input(input, input.stackSize, pending, materials));
                }
            }
            out.get(key)
                .add(new RecyclingSolver.Recipe("assemblyline", parsed, Math.max(1, recipe.mOutput.stackSize)));
        }
    }

    /**
     * @return the key of the one pending item a machine recipe makes, or null when it makes something else too (the
     *         inputs would be split between products) or makes it only by chance
     */
    private static String soleOutput(GTRecipe recipe, Map<String, ItemStack> pending) {
        String key = null;
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            ItemStack output = recipe.mOutputs[i];
            if (output == null) {
                continue;
            }
            String outputKey = keyOf(output, pending);
            if (outputKey == null || recipe.getOutputChance(i) < 10000 || (key != null && !key.equals(outputKey))) {
                return null;
            }
            key = outputKey;
        }
        return key;
    }

    private static String shortName(String mapName) {
        return mapName.startsWith("gt.recipe.") ? mapName.substring("gt.recipe.".length()) : mapName;
    }

    // ---- inputs ----

    private static RecyclingSolver.Input input(ItemStack stack, int count, Map<String, ItemStack> pending,
        Map<String, Materials> materials) {
        ItemStack normal = stack.copy();
        if (normal.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            normal.setItemDamage(0);
        }
        String key = keyOf(normal, pending);
        if (key != null) {
            return RecyclingSolver.Input.target(key, count);
        }
        Item item = normal.getItem();
        // A tool or a filled container stays in the grid or returns its container: it is not consumed.
        if (item instanceof MetaGeneratedTool || item.hasContainerItem(normal)) {
            return RecyclingSolver.Input.known(Collections.<String, Long>emptyMap(), count);
        }
        return RecyclingSolver.Input.known(composition(GTOreDictUnificator.getItemData(normal), materials), count);
    }

    /**
     * An ore-dictionary slot of a crafting recipe. GT's reversible crafting recipes give a few ore names a fixed
     * content, because their members carry no item data (planks, stones, chests, books, pistons, furnaces, anvils);
     * they are applied here the same way. Any other ore name counts as its first member.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L1114-L1146
    private static RecyclingSolver.Input oreInput(List<?> ores, Map<String, ItemStack> pending,
        Map<String, Materials> materials) {
        ItemStack first = (ItemStack) ores.get(0);
        String name = oreName(ores, first);
        if (name != null) {
            Map<String, Long> fixed = new LinkedHashMap<>();
            long m = GTValues.M;
            switch (name) {
                case "plankWood":
                    put(fixed, Materials.Wood, m, materials);
                    break;
                case "stoneNetherrack":
                    put(fixed, Materials.Netherrack, m, materials);
                    break;
                case "stoneObsidian":
                    put(fixed, Materials.Obsidian, m, materials);
                    break;
                case "stoneEndstone":
                    put(fixed, Materials.Endstone, m, materials);
                    break;
                case "craftingChest":
                    put(fixed, Materials.Wood, 8 * m, materials);
                    break;
                case "craftingBook":
                    put(fixed, Materials.Paper, 3 * m, materials);
                    break;
                case "craftingPiston":
                    put(fixed, Materials.Stone, 4 * m, materials);
                    put(fixed, Materials.Wood, 3 * m, materials);
                    break;
                case "craftingFurnace":
                    put(fixed, Materials.Stone, 8 * m, materials);
                    break;
                case "craftingIndustrialDiamond":
                    put(fixed, Materials.Diamond, m, materials);
                    break;
                case "craftingAnvil":
                    put(fixed, Materials.Iron, 10 * m, materials);
                    break;
                default:
                    fixed = null;
            }
            if (fixed != null) {
                return RecyclingSolver.Input.known(fixed, 1);
            }
        }
        return input(first, 1, pending, materials);
    }

    /** @return the ore name whose live list is {@code ores} (Forge recipes keep that very list), or null */
    private static String oreName(List<?> ores, ItemStack first) {
        for (int id : OreDictionary.getOreIDs(first)) {
            String name = OreDictionary.getOreName(id);
            if (OreDictionary.getOres(name) == ores) {
                return name;
            }
        }
        return null;
    }

    private static void put(Map<String, Long> out, Materials material, long amount, Map<String, Materials> materials) {
        materials.put(material.mName, material);
        out.merge(material.mName, amount, Long::sum);
    }

    /** @return material name to amount of an item's data; empty for no data. Circuits have negative amounts. */
    private static Map<String, Long> composition(ItemData data, Map<String, Materials> materials) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (data == null) {
            return out;
        }
        add(out, data.mMaterial, materials);
        for (MaterialStack byProduct : data.mByProducts) {
            add(out, byProduct, materials);
        }
        return out;
    }

    private static void add(Map<String, Long> out, MaterialStack stack, Map<String, Materials> materials) {
        if (stack == null || stack.mMaterial == null || stack.mAmount <= 0) {
            return;
        }
        materials.put(stack.mMaterial.mName, stack.mMaterial);
        out.merge(stack.mMaterial.mName, stack.mAmount, Long::sum);
    }

    /**
     * GregGuns' own filter: no radioactive material comes back (GGS-06-46, D84), no explosive (the powder burns), and a
     * material GT can return in no form would only take one of the Macerator's four output slots.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1971-L1980
    // (GT reads radioactivity from the material's radioactivity enchantment)
    static boolean keep(Materials material) {
        if (material == null || material == Materials._NULL || material == Materials.Empty) {
            return false;
        }
        if (material.mArmorEnchantment instanceof EnchantmentRadioactivity
            || material.mToolEnchantment instanceof EnchantmentRadioactivity) {
            return false;
        }
        // Gunpowder, TNT and the like burn away, as in the hand-written ammo data (GGS-06-21); GT's own Arc Furnace
        // turns them to a pinch of ash.
        if (material.contains(SubTag.EXPLOSIVE)) {
            return false;
        }
        return GTOreDictUnificator.getDust(material.mMacerateInto, GTValues.M) != null
            || GTOreDictUnificator.getIngot(material.mSmeltInto.mArcSmeltInto, GTValues.M) != null;
    }

    // ---- output ----

    private static ItemData itemData(Map<String, Long> composition, Map<String, Materials> materials) {
        List<MaterialStack> stacks = new ArrayList<>();
        for (Map.Entry<String, Long> entry : composition.entrySet()) {
            stacks.add(new MaterialStack(materials.get(entry.getKey()), entry.getValue()));
        }
        return new ItemData(
            stacks.get(0),
            stacks.subList(1, stacks.size())
                .toArray(new MaterialStack[0]));
    }

    /**
     * For a {@link RadiationMarker} stack, the same data with a Uranium-235 entry of {@link RadiationMarker#amount} as
     * the main material, so GT irradiates the holder (GGS-06-23); the composition becomes the by-products, unchanged,
     * and still comes back. Any other stack's data is returned as it is. Used by this pass and by the component
     * recycling data ({@code ComponentRecipes}), which attaches the nuclear components' data before this pass runs.
     *
     * @param key  the stack's {@link RecyclingCatalog} key (a bare registry name for a component)
     * @param data the recycling data
     * @return the data to attach
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/objects/ItemData.java#L33-L53
    // (the explicit constructor keeps the given main material first; it does not sort)
    public static ItemData withRadiationMarker(String key, ItemData data) {
        if (!RadiationMarker.marks(key)) {
            return data;
        }
        MaterialStack[] byProducts = new MaterialStack[data.mByProducts.length + 1];
        byProducts[0] = data.mMaterial;
        System.arraycopy(data.mByProducts, 0, byProducts, 1, data.mByProducts.length);
        return new ItemData(new MaterialStack(Materials.Uranium235, RadiationMarker.amount(GTValues.M)), byProducts);
    }

    private static boolean hasRecyclingRecipe(ItemStack stack) {
        return RecipeMaps.maceratorRecipes.findRecipeQuery()
            .items(stack)
            .voltage(TierEU.RECIPE_UV)
            .find() != null
            || RecipeMaps.arcFurnaceRecipes.findRecipeQuery()
                .items(stack, GTUtility.getIntegratedCircuit(1))
                .voltage(TierEU.RECIPE_UV)
                .find() != null;
    }

    /** @return the catalog key of a stack when it is pending, else null */
    private static String keyOf(ItemStack stack, Map<String, ItemStack> pending) {
        String key = RecyclingCatalog.keyOf(stack);
        return key != null && pending.containsKey(key) ? key : null;
    }

    /** @return the composition of item data as {@code Steel 3.0, Rubber 0.5} in units of one ingot */
    public static String describe(ItemData data) {
        if (data == null || !data.hasValidMaterialData()) {
            return "(no material)";
        }
        StringBuilder out = new StringBuilder();
        append(out, data.mMaterial);
        for (MaterialStack byProduct : data.mByProducts) {
            out.append(", ");
            append(out, byProduct);
        }
        return out.toString();
    }

    private static void append(StringBuilder out, MaterialStack stack) {
        out.append(stack.mMaterial.mName)
            .append(' ')
            .append(String.format(java.util.Locale.ROOT, "%.3f", stack.mAmount / (double) GTValues.M));
    }

    private static void logSummary(int attached) {
        Map<Status, Integer> counts = new java.util.EnumMap<>(Status.class);
        for (Entry entry : REPORT.values()) {
            counts.merge(entry.status, 1, Integer::sum);
        }
        GregGuns.LOG.info("Recycling data: {} catalog entries, {} derived now; {}", REPORT.size(), attached, counts);
        for (Map.Entry<String, Entry> entry : REPORT.entrySet()) {
            Status status = entry.getValue().status;
            boolean problem = status == Status.NO_RECIPE || status == Status.NOTHING_RECOVERED
                || status == Status.CLASH;
            String line = "Recycling " + status + " " + entry.getKey() + " - " + entry.getValue().detail;
            // With a module off its items have no recipe; a player's log gets only the summary line.
            if (problem && com.gregguns.core.DevEnvironment.isActive()) {
                GregGuns.LOG.warn(line);
            } else {
                GregGuns.LOG.debug(line);
            }
        }
    }
}
