package com.gregguns.machine;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

/**
 * The interior lamp of the glass multiblocks (Ammunition Works and Armory Gantry): one required light block per bay or
 * section, so the hall is not dark behind its windows. Cosmetic and tier-free: it accepts any block that gives light
 * (glowstone, a lit redstone lamp, any other mod's lamp) and the unlit redstone lamp, which a player can power.
 *
 * <p>
 * The pack ships no GT lamp block, so there is nothing GT specific to chain: GT++'s redstone lamp is a machine tile
 * entity, not a block that shape strings can name. The test is therefore on the block's own light value, which also
 * lets modpack lamps through. The hologram and survival auto-build show and place glowstone.
 */
// Source (the pack has no GT lamp block; GT's own structure utilities only offer glass, casings and frames):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java
public final class InteriorLamp {

    /** The shape character both structures use for the lamp. */
    public static final char SYMBOL = 'S';

    /** The least {@link Block#getLightValue()} that counts as a lamp (glowstone and lit lamps give 15). */
    public static final int MIN_LIGHT = 12;

    private InteriorLamp() {}

    /**
     * @param block a block found at a lamp position
     * @return true when it is accepted: bright enough, or the unlit redstone lamp
     */
    public static boolean accepts(Block block) {
        return block != null && (block.getLightValue() >= MIN_LIGHT || block == Blocks.redstone_lamp);
    }

    /**
     * @param <T> the machine class
     * @return the structure element for {@link #SYMBOL}; hologram and auto-build use glowstone
     */
    public static <T> IStructureElement<T> element() {
        return StructureUtility.<T>ofBlockAdder((t, block, meta) -> accepts(block), Blocks.glowstone, 0);
    }
}
