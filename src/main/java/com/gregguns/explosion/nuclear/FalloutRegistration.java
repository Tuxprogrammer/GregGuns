package com.gregguns.explosion.nuclear;

import net.minecraft.item.ItemStack;

import com.gregguns.core.registry.ModuleRegistrar;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Registers the irradiated block, its item and the rubble it drops, and the fallout tick handler. Called from the guns
 * module so the Nuke Launcher has it whether or not the ICBM module is on.
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
        rubble.setTextureName("gregguns:thirdparty/ntm/fragmentation_sleeve_steel");
        FalloutBlocks.set(block, rubble);
        FalloutEngine.register();
    }

    /** GT item data: the rubble is a pile of radioactive dust to GT, so carrying it irradiates. */
    public static void attachItemData() {
        if (FalloutBlocks.rubble() == null) {
            return;
        }
        // getRadioactivityLevel reads only the main material of the stack's ItemData (as for the nuke rocket).
        GTOreDictUnificator.addItemData(
            new ItemStack(FalloutBlocks.rubble()),
            new ItemData(Materials.Uranium235, OrePrefixes.dust.getMaterialAmount()));
    }
}
