package com.gregguns.explosion.nuclear;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the irradiated block, its item and the rubble it drops, and the fallout tick handler. Called from the guns
 * module so the Nuke Launcher has it whether or not the ICBM module is on. The rubble gets no GT item data on
 * purpose: GT would generate recycling recipes that turn it into free uranium-235 ({@link ItemIrradiatedRubble}).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class FalloutRegistration {

    private FalloutRegistration() {}

    /** @param registrar the guns module's registrar */
    public static void preInit(ModuleRegistrar registrar) {
        BlockIrradiatedEarth block = registrar
            .registerBlock("irradiated_earth", new BlockIrradiatedEarth(), ItemBlockIrradiated.class);
        ItemIrradiatedRubble rubble = registrar.registerItem("irradiated_rubble", new ItemIrradiatedRubble());
        // Self-check placeholders (the check wants a file under the registered texture name); the block draws the
        // vanilla textures and the item the vanilla flint icon.
        block.setBlockTextureName("gregguns:casings/gantry_casing");
        rubble.setTextureName("gregguns:thirdparty/ntm/irradiated_rubble");
        FalloutBlocks.set(block, rubble);
        BlockTrinitite trinitite = registrar.registerBlock("trinitite", new BlockTrinitite(), ItemBlockFallout.class);
        trinitite.setBlockTextureName("gregguns:thirdparty/ntm/trinitite");
        BlockFalloutGoo goo = registrar.registerBlock("fallout_goo", new BlockFalloutGoo(), ItemBlockIrradiated.class);
        goo.setBlockTextureName("gregguns:thirdparty/ntm/fallout_goo");
        ItemFalloutProduct shard = registrar
            .registerItem("trinitite_shard", new ItemFalloutProduct(false, "gregguns:thirdparty/ntm/trinitite_shard"));
        ItemFalloutProduct gooItem = registrar
            .registerItem("fallout_goo_item", new ItemFalloutProduct(true, "gregguns:thirdparty/ntm/fallout_goo"));
        // Self-check placeholders: the products draw their own icon (see ItemFalloutProduct.registerIcons).
        shard.setTextureName("gregguns:thirdparty/ntm/trinitite_shard");
        gooItem.setTextureName("gregguns:thirdparty/ntm/fallout_goo");
        FalloutBlocks.setProducts(trinitite, goo, shard, gooItem);
        FalloutEngine.register();
    }

    /** Queues the GT recipes of the byproduct chain for GT's before-postload hook. */
    public static void init() {
        FalloutRecipes.init();
    }
}
