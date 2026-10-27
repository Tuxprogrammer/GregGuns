package com.gregguns.explosion.nuclear;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * The irradiated block's variants and their mapping to the vanilla surface blocks fallout converts (D21): dirt, stone,
 * sand, gravel and cobblestone. Grass counts as dirt. Holds the registered block, set once at pre-init.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class FalloutBlocks {

    /** Metadata of the dirt variant. */
    public static final int DIRT = 0;
    /** Metadata of the stone variant. */
    public static final int STONE = 1;
    /** Metadata of the sand variant. */
    public static final int SAND = 2;
    /** Metadata of the gravel variant. */
    public static final int GRAVEL = 3;
    /** Metadata of the cobblestone variant. */
    public static final int COBBLE = 4;
    /** How many variants there are. */
    public static final int VARIANTS = 5;

    private static BlockIrradiatedEarth irradiated;
    private static ItemIrradiatedRubble rubble;

    private FalloutBlocks() {}

    static void set(BlockIrradiatedEarth block, ItemIrradiatedRubble item) {
        irradiated = block;
        rubble = item;
    }

    /** @return the irradiated block; null before registration */
    public static BlockIrradiatedEarth irradiated() {
        return irradiated;
    }

    /** @return the rubble item it drops; null before registration */
    public static ItemIrradiatedRubble rubble() {
        return rubble;
    }

    /**
     * @param block a block in the world
     * @param meta  its metadata
     * @return the variant fallout turns it into, or -1 when fallout leaves it alone
     */
    public static int variantFor(Block block, int meta) {
        if (block == Blocks.dirt || block == Blocks.grass) {
            return DIRT;
        }
        if (block == Blocks.stone && meta == 0) {
            return STONE;
        }
        if (block == Blocks.sand) {
            return SAND;
        }
        if (block == Blocks.gravel) {
            return GRAVEL;
        }
        if (block == Blocks.cobblestone) {
            return COBBLE;
        }
        return -1;
    }

    /**
     * @param variant a variant
     * @return the vanilla block decay puts back (grass comes back as dirt)
     */
    public static Block original(int variant) {
        switch (variant) {
            case STONE:
                return Blocks.stone;
            case SAND:
                return Blocks.sand;
            case GRAVEL:
                return Blocks.gravel;
            case COBBLE:
                return Blocks.cobblestone;
            default:
                return Blocks.dirt;
        }
    }
}
