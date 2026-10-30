package com.gregguns.icbm.warhead;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;

/**
 * Registration of the warhead content: the inert warhead item, the casing blocks and their items, the filler block,
 * the tiles, the explosives, and (later hooks) the recipes, the charge-loading crafting recipe and scenarios.
 * {@code IcbmModule} calls the hooks. Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md, warheads-and-fill.md
public final class WarheadRegistration {

    private static final int ENTITY_ID_FLYING_BLOCK = 125;

    private WarheadRegistration() {}

    /**
     * Config-independent registration: blocks, items, tiles and the explosive definitions.
     *
     * @param registrar the ICBM module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        WarheadBlasts.registerAll();
        com.gregguns.icbm.recipe.IcbmParts.register(registrar);
        // D282: the inert warhead, the first step of the chain, sits just before the live warheads.
        ItemInertWarhead inert = registrar.registerItem(InertWarheads.NAME, new ItemInertWarhead());
        inert.setTextureName("gregguns:" + InertWarheads.NAME);
        InertWarheads.set(inert);
        for (WarheadCasing casing : WarheadCasing.values()) {
            BlockWarhead block = registrar
                .registerBlock(casing.blockName(), new BlockWarhead(casing), ItemBlockWarhead.class);
            // Placeholder particle icon: the model texture lives in the atlas under another name (see WarheadSprite).
            block.setBlockTextureName("gregguns:casings/gantry_casing");
            WarheadBlocks.set(casing, block);
        }
        BlockWarheadFiller filler = registrar
            .registerBlock(BlockWarheadFiller.NAME, new BlockWarheadFiller(), net.minecraft.item.ItemBlock.class);
        filler.setBlockTextureName("gregguns:casings/gantry_casing");
        WarheadBlocks.setFiller(filler);
        registrar.registerEntity("flying_block", EntityFlyingBlock.class, ENTITY_ID_FLYING_BLOCK, 128, 2, true);
        registrar.registerTileEntity("warhead", TileWarhead.class);
        registrar.registerTileEntity("warhead_filler", TileWarheadFiller.class);
    }

    /** Recipes and recycling data, queued for GT's before-postload hook. Runs only for an enabled module. */
    public static void init() {
        com.gregguns.icbm.recipe.PartRecipes.init();
        WarheadRecipes.init();
        // Source: Forge 1.7.10 RecipeSorter.register; without it FML logs "Unknown recipe class" at load.
        net.minecraftforge.oredict.RecipeSorter.register(
            "gregguns:warhead_charge",
            WarheadChargeRecipe.class,
            net.minecraftforge.oredict.RecipeSorter.Category.SHAPELESS,
            "");
        cpw.mods.fml.common.registry.GameRegistry.addRecipe(new WarheadChargeRecipe());
    }

    /**
     * Dev-only scenarios.
     *
     * @param registry where to add them
     */
    public static void registerScenarios(ScenarioRegistry registry) {
        WarheadScenarios.registerAll(registry);
        com.gregguns.icbm.recipe.RecipeScenarios.registerAll(registry);
    }
}
