package com.gregguns.explosion.nuclear;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The item of {@link BlockTrinitite}: an ordinary block item with the D41 tooltip. */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
public final class ItemBlockFallout extends ItemBlock {

    /** @param block the trinitite block */
    public ItemBlockFallout(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        FalloutTooltips.add(tooltip, "trinitite");
    }
}
