package com.gregguns.core.selfcheck;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.gregguns.GregGuns;
import com.gregguns.api.GregGunsComponent;
import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.config.ModuleFlags;
import com.gregguns.item.part.PartCatalog;
import com.gregguns.recipe.ComponentTable;

import cpw.mods.fml.relauncher.ReflectionHelper;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * The thin Minecraft glue of the dev self-check: it reads the live registries, the crafting manager and GT's recipe
 * maps
 * into plain descriptors, hands them to {@link SelfCheck}, and stops the dev server on any violation.
 *
 * <p>
 * Timing: run this from {@code FMLLoadCompleteEvent}, not from postInit. GT buffers its crafting-table recipes and adds
 * the second pass in its own {@code FMLLoadCompleteEvent} handler
 * (<a href=
 * "https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L624-L644">GTMod.onLoadComplete</a>),
 * and other mods add recipes in their postInit. Load-complete is the first point where every recipe exists, and
 * GregGuns' hard dependency on gregtech means GT's handler has already run when ours does.
 *
 * <p>
 * Dev environment only (see {@link com.gregguns.core.DevEnvironment}), so field reads use the deobfuscated names. Both
 * sides; nothing here touches client-only members. Spec: CONTRIBUTING.md (Testing, dev self-check).
 */
public final class SelfCheckRunner {

    private static final String NAMESPACE_PREFIX = GregGuns.MODID + ":";

    private SelfCheckRunner() {}

    /**
     * Runs the self-check over everything registered so far.
     *
     * @throws SelfCheckFailedException listing every violation; this stops the dev server
     */
    public static void run() {
        List<String> glueProblems = new ArrayList<>();
        List<ContentDescriptor> contents = scanContent(glueProblems);
        List<RecipeDescriptor> recipes = new ArrayList<>();
        scanCraftingRecipes(recipes);
        scanGtRecipes(recipes);

        SelfCheck.Report report = SelfCheck
            .run(contents, recipes, new ClasspathResources(), glueProblems, scanComponents());
        if (report.violations()
            .isEmpty()) {
            GregGuns.LOG.info(report.summary());
            return;
        }
        GregGuns.LOG.error(report.failureMessage());
        report.throwIfFailed();
    }

    /**
     * Reads the unification data (ore names and GT's association of each GregGuns item), the registered components,
     * and the allowlist and module-deferred sets into plain data.
     */
    private static ComponentInput scanComponents() {
        List<UnificationDescriptor> unification = new ArrayList<>();
        List<String> components = new ArrayList<>();
        for (Object key : Item.itemRegistry.getKeys()) {
            String name = (String) key;
            if (!name.startsWith(NAMESPACE_PREFIX)) {
                continue;
            }
            Item item = (Item) Item.itemRegistry.getObject(name);
            if (item instanceof GregGunsComponent) {
                components.add(name);
            }
            ItemStack stack = new ItemStack(item, 1, 0);
            List<String> oreNames = new ArrayList<>();
            for (int id : OreDictionary.getOreIDs(stack)) {
                oreNames.add(OreDictionary.getOreName(id));
            }
            // Source: GTOreDictUnificator.getAssociation returns the ItemData GT registered for the stack; mPrefix is
            // null for plain composition data, set only when the stack is a unified prefix + material part.
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java
            ItemData association = GTOreDictUnificator.getAssociation(stack);
            String prefix = association == null || association.mPrefix == null ? null : association.mPrefix.name();
            String material = association == null || association.mMaterial == null
                || association.mMaterial.mMaterial == null ? null : association.mMaterial.mMaterial.mName;
            unification.add(new UnificationDescriptor(name, oreNames, prefix, material));
        }
        List<String> prefixes = new ArrayList<>();
        // The prefixes are public static constants of OrePrefixes, so they are listed by reflection on the fields.
        for (java.lang.reflect.Field field : OrePrefixes.class.getFields()) {
            if (field.getType() == OrePrefixes.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                prefixes.add(field.getName());
            }
        }
        Map<String, String> allowlist = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : PartCatalog.noRecipeAllowlist()
            .entrySet()) {
            allowlist.put(NAMESPACE_PREFIX + entry.getKey(), entry.getValue());
        }
        List<String> deferred = new ArrayList<>();
        for (String output : ComponentTable.outputsDeferredBy(ModuleFlags::isEnabled)) {
            deferred.add(NAMESPACE_PREFIX + output);
        }
        return new ComponentInput(unification, prefixes, components, allowlist, deferred);
    }

    private static List<ContentDescriptor> scanContent(List<String> glueProblems) {
        List<ContentDescriptor> contents = new ArrayList<>();
        for (Object key : Item.itemRegistry.getKeys()) {
            String name = (String) key;
            if (!name.startsWith(NAMESPACE_PREFIX)) {
                continue;
            }
            Item item = (Item) Item.itemRegistry.getObject(name);
            Block block = item instanceof ItemBlock ? Block.getBlockFromItem(item) : null;
            contents.add(describe(name, item, block, glueProblems));
        }
        for (Object key : Block.blockRegistry.getKeys()) {
            String name = (String) key;
            if (!name.startsWith(NAMESPACE_PREFIX)) {
                continue;
            }
            Block block = (Block) Block.blockRegistry.getObject(name);
            if (Item.getItemFromBlock(block) == null) {
                contents.add(describe(name, null, block, glueProblems));
            }
        }
        return contents;
    }

    private static ContentDescriptor describe(String name, Item item, Block block, List<String> glueProblems) {
        Object tiered = item instanceof Tiered ? item : block;
        Tier tier = tiered instanceof Tiered ? ((Tiered) tiered).getTier() : null;
        boolean hidden = item instanceof HiddenFromCreativeTab || block instanceof HiddenFromCreativeTab;

        // Item.getIconString, Item.getCreativeTab, Block.getTextureName and Block.getCreativeTabToDisplayOn are all
        // @SideOnly(CLIENT) in 1.7.10 and are stripped on a dedicated server, so the dev-only check reads the fields.
        String iconName;
        boolean hasTab;
        String langKey;
        if (item != null) {
            iconName = block == null ? readField(Item.class, item, "iconString", name, glueProblems)
                : readField(Block.class, block, "textureName", name, glueProblems);
            Object itemTab = readFieldObject(Item.class, item, "tabToDisplayOn", name, glueProblems);
            Object blockTab = block == null ? null
                : readFieldObject(Block.class, block, "displayOnCreativeTab", name, glueProblems);
            hasTab = itemTab != null || blockTab != null;
            langKey = item.getUnlocalizedName() + ".name";
        } else {
            iconName = readField(Block.class, block, "textureName", name, glueProblems);
            // A block with no item cannot appear in a creative tab, so there is nothing to require.
            hasTab = true;
            langKey = block.getUnlocalizedName() + ".name";
        }
        return new ContentDescriptor(name, item == null || block != null, langKey, iconName, tier, hidden, hasTab);
    }

    private static <E> String readField(Class<? super E> owner, E instance, String field, String content,
        List<String> glueProblems) {
        Object value = readFieldObject(owner, instance, field, content, glueProblems);
        return value == null ? null : value.toString();
    }

    private static <E> Object readFieldObject(Class<? super E> owner, E instance, String field, String content,
        List<String> glueProblems) {
        try {
            return ReflectionHelper.getPrivateValue(owner, instance, field);
        } catch (ReflectionHelper.UnableToAccessFieldException e) {
            glueProblems.add("self-check cannot read " + owner.getSimpleName() + "." + field + " of " + content);
            GregGuns.LOG.error("Self-check cannot read {}.{} of {}", owner.getSimpleName(), field, content, e);
            return null;
        }
    }

    private static void scanCraftingRecipes(List<RecipeDescriptor> out) {
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            IRecipe recipe = (IRecipe) entry;
            ItemStack output = recipe.getRecipeOutput();
            List<Object> inputs = new ArrayList<>();
            boolean shaped = collectInputs(recipe, inputs);
            if (!involvesGregGuns(output, inputs)) {
                continue;
            }
            List<StackDescriptor> inputDescriptors = new ArrayList<>();
            for (Object input : inputs) {
                inputDescriptors.add(describeInput(input));
            }
            List<StackDescriptor> outputs = new ArrayList<>();
            outputs.add(describeStack(output));
            out.add(new RecipeDescriptor(craftingId(recipe, output), 0, shaped, inputDescriptors, outputs));
        }
    }

    /**
     * Fills {@code inputs} with ItemStack, List of ItemStack (ore dict) or null; returns whether nulls are grid gaps.
     */
    private static boolean collectInputs(IRecipe recipe, List<Object> inputs) {
        if (recipe instanceof ShapedOreRecipe) {
            for (Object input : ((ShapedOreRecipe) recipe).getInput()) {
                inputs.add(input);
            }
            return true;
        }
        if (recipe instanceof ShapelessOreRecipe) {
            inputs.addAll(((ShapelessOreRecipe) recipe).getInput());
            return false;
        }
        if (recipe instanceof ShapedRecipes) {
            for (ItemStack input : ((ShapedRecipes) recipe).recipeItems) {
                inputs.add(input);
            }
            return true;
        }
        if (recipe instanceof ShapelessRecipes) {
            inputs.addAll(((ShapelessRecipes) recipe).recipeItems);
            return false;
        }
        // Special recipe classes (dyeing, repair, GT's own) expose no input list; only the output is checked.
        return true;
    }

    private static boolean involvesGregGuns(ItemStack output, List<Object> inputs) {
        if (isGregGuns(output)) {
            return true;
        }
        for (Object input : inputs) {
            if (input instanceof ItemStack && isGregGuns((ItemStack) input)) {
                return true;
            }
        }
        return false;
    }

    private static StackDescriptor describeInput(Object input) {
        if (input == null) {
            return StackDescriptor.emptySlot();
        }
        if (input instanceof ItemStack) {
            return describeStack((ItemStack) input);
        }
        if (input instanceof List) {
            return StackDescriptor.oreDict("list of " + ((List<?>) input).size(), !((List<?>) input).isEmpty());
        }
        return StackDescriptor.item(String.valueOf(input), false, false, null);
    }

    private static StackDescriptor describeStack(ItemStack stack) {
        return describeStack(stack, false);
    }

    /**
     * @param gtInput {@code true} for an input of a GT machine recipe, where a stack size of 0 means "required but not
     *                consumed" (programmed circuits, molds) and is valid
     */
    private static StackDescriptor describeStack(ItemStack stack, boolean gtInput) {
        if (stack == null || stack.getItem() == null) {
            return StackDescriptor.item("null", false, false, null);
        }
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        boolean gregGuns = name != null && name.startsWith(NAMESPACE_PREFIX);
        Tier tier = gregGuns && stack.getItem() instanceof Tiered ? ((Tiered) stack.getItem()).getTier() : null;
        if (gregGuns && tier == null && stack.getItem() instanceof ItemBlock) {
            Block block = Block.getBlockFromItem(stack.getItem());
            tier = block instanceof Tiered ? ((Tiered) block).getTier() : null;
        }
        // Source (size 0 = not consumed):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipe.java#L1299-L1316
        // (parallel maths skips an input whose consumption is 0; GT's builder gives a programmed circuit size 0).
        return StackDescriptor
            .item(String.valueOf(name), StackPresence.isPresent(true, stack.stackSize, gtInput), gregGuns, tier);
    }

    private static boolean isGregGuns(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        return name != null && name.startsWith(NAMESPACE_PREFIX);
    }

    private static String craftingId(IRecipe recipe, ItemStack output) {
        return "crafting " + recipe.getClass()
            .getSimpleName() + " -> " + (output == null ? "null" : String.valueOf(describeStack(output).label()));
    }

    private static void scanGtRecipes(List<RecipeDescriptor> out) {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMap.java#L48
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (!involvesGregGuns(recipe.mOutputs) && !involvesGregGuns(recipe.mInputs)) {
                    continue;
                }
                List<StackDescriptor> inputs = new ArrayList<>();
                addStacks(recipe.mInputs, inputs, true);
                addFluids(recipe.mFluidInputs, inputs);
                List<StackDescriptor> outputs = new ArrayList<>();
                addStacks(recipe.mOutputs, outputs, false);
                addFluids(recipe.mFluidOutputs, outputs);
                String id = "gt:" + map.unlocalizedName + " -> " + firstLabel(outputs);
                out.add(new RecipeDescriptor(id, recipe.mEUt, false, inputs, outputs));
            }
        }
    }

    private static boolean involvesGregGuns(ItemStack[] stacks) {
        if (stacks == null) {
            return false;
        }
        for (ItemStack stack : stacks) {
            if (isGregGuns(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void addStacks(ItemStack[] stacks, List<StackDescriptor> into, boolean gtInput) {
        if (stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            into.add(describeStack(stack, gtInput));
        }
    }

    private static void addFluids(FluidStack[] fluids, List<StackDescriptor> into) {
        if (fluids == null) {
            return;
        }
        for (FluidStack fluid : fluids) {
            into.add(
                StackDescriptor.fluid(
                    fluid == null ? "null" : String.valueOf(fluid.getFluid()),
                    fluid != null && fluid.getFluid() != null));
        }
    }

    private static String firstLabel(List<StackDescriptor> descriptors) {
        return descriptors.isEmpty() ? "nothing"
            : descriptors.get(0)
                .label();
    }

    /** Reads the mod's resources from the classpath, which works the same on both sides. */
    private static final class ClasspathResources implements ResourceSource {

        @Override
        public boolean exists(String path) {
            return GregGuns.class.getClassLoader()
                .getResource(path) != null;
        }

        @Override
        public String readText(String path) throws IOException {
            try (InputStream in = GregGuns.class.getClassLoader()
                .getResourceAsStream(path)) {
                if (in == null) {
                    throw new IOException("resource not found on the classpath: " + path);
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    bytes.write(buffer, 0, read);
                }
                return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }
}
