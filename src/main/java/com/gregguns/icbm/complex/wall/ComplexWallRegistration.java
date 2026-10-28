package com.gregguns.icbm.complex.wall;

import com.gregguns.core.registry.ModuleRegistrar;

/** Registers the silo complex wall block. Content always registers, whatever the module flag says. */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md; D205
public final class ComplexWallRegistration {

    private ComplexWallRegistration() {}

    /** @param registrar the ICBM module's registrar */
    public static void preInit(ModuleRegistrar registrar) {
        BlockComplexWall.instance = registrar
            .registerBlock(BlockComplexWall.NAME, new BlockComplexWall(), ItemBlockComplexWall.class);
        // The registrar names the texture after the block; the block draws the Steel Bastion Plating's (no art of its
        // own), so the name is set again after registering.
        BlockComplexWall.instance.setBlockTextureName(BlockComplexWall.TEXTURE);
    }
}
