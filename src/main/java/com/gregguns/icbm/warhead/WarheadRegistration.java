package com.gregguns.icbm.warhead;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;

/**
 * Registration of the warhead content: the two casing blocks and their items, the filler block, the tiles, the T1 and
 * T2 explosives, and (later hooks) recipes and scenarios. {@code IcbmModule} calls the hooks. Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md, warheads-and-fill.md
public final class WarheadRegistration {

    private WarheadRegistration() {}

    /**
     * Config-independent registration: blocks, items, tiles and the explosive definitions.
     *
     * @param registrar the ICBM module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        WarheadBlasts.registerAll();
        BlockWarhead t1 = registrar
            .registerBlock(WarheadCasing.T1.blockName(), new BlockWarhead(WarheadCasing.T1), ItemBlockWarhead.class);
        BlockWarhead t2 = registrar
            .registerBlock(WarheadCasing.T2.blockName(), new BlockWarhead(WarheadCasing.T2), ItemBlockWarhead.class);
        BlockWarheadFiller filler = registrar
            .registerBlock(BlockWarheadFiller.NAME, new BlockWarheadFiller(), net.minecraft.item.ItemBlock.class);
        // Placeholder particle icon: the model texture lives in the atlas under another name (see WarheadSprite).
        t1.setBlockTextureName("gregguns:casings/gantry_casing");
        t2.setBlockTextureName("gregguns:casings/gantry_casing");
        filler.setBlockTextureName("gregguns:casings/gantry_casing");
        WarheadBlocks.set(t1, t2, filler);
        registrar.registerTileEntity("warhead", TileWarhead.class);
        registrar.registerTileEntity("warhead_filler", TileWarheadFiller.class);
    }

    /** Recipes and recycling data, queued for GT's before-postload hook. Runs only for an enabled module. */
    public static void init() {
        WarheadRecipes.init();
    }

    /**
     * Dev-only scenarios.
     *
     * @param registry where to add them
     */
    public static void registerScenarios(ScenarioRegistry registry) {
        WarheadScenarios.registerAll(registry);
    }
}
