package com.gregguns.explosion.nuclear;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

/** The item of {@link BlockIrradiatedEarth}: the variant is the damage value and picks the name. */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class ItemBlockIrradiated extends ItemBlockWithMetadata {

    /** @param block the irradiated block */
    public ItemBlockIrradiated(Block block) {
        super(block, block);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + (stack.getItemDamage() % FalloutBlocks.VARIANTS);
    }
}
