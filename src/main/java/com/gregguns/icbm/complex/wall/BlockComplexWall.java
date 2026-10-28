package com.gregguns.icbm.complex.wall;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

/**
 * The wall of a silo complex: a plain cube that shows the Steel Bastion Plating texture and joins into one plated
 * mass through the same connected-texture tiles (D205). It is deliberately not a GregTech casing: placing or breaking
 * a casing starts a GregTech machine-update search through every connected casing, and a complex has tens of
 * thousands of wall blocks. It breaks into itself, no recipe uses it and nothing recycles it, so mining a complex
 * yields no Rampart casing.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface); D205
public final class BlockComplexWall extends Block implements Tiered {

    /** The bare content name; the registry id is {@code gregguns:complex_wall}. */
    public static final String NAME = "complex_wall";
    /** The texture it shows (the Steel Bastion Plating's, so no art of its own). */
    public static final String TEXTURE = "gregguns:casings/bastion_plating_steel";

    /** The block, set by {@link ComplexWallRegistration}; null before pre-init. */
    static BlockComplexWall instance;

    BlockComplexWall() {
        super(Material.iron);
        setHardness(5.0F);
        setResistance(30.0F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setBlockTextureName(TEXTURE);
    }

    /** @return the registered block, or null before pre-init */
    public static BlockComplexWall instance() {
        return instance;
    }

    @Override
    public Tier getTier() {
        return Tier.EV;
    }
}
