package com.gregguns.worldgen.nest;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** The item of {@link BlockBugNest}: one subtype per meta, so the resin and the eggs have their own names. */
public final class ItemBlockBugNest extends ItemBlock {

    /** @param block the nest block */
    public ItemBlockBugNest(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage & 1;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return stack.getItemDamage() == BlockBugNest.META_EGGS ? "tile.gregguns.bug_nest_eggs"
            : "tile.gregguns.bug_nest";
    }
}
