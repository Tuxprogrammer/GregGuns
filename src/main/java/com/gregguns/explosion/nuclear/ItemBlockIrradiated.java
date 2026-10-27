package com.gregguns.explosion.nuclear;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The item of {@link BlockIrradiatedEarth}: the variant is the damage value and picks the name. */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class ItemBlockIrradiated extends ItemBlockWithMetadata {

    /** @param block the irradiated block */
    public ItemBlockIrradiated(Block block) {
        super(block, block);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + FalloutBlocks.variantOf(stack.getItemDamage());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean hot = (stack.getItemDamage() & FalloutBlocks.HOT_BIT) != 0;
        if (Block.getBlockFromItem(this) instanceof BlockFalloutGoo) {
            FalloutTooltips.add(tooltip, stack.getItemDamage() == 1 ? "hot_goo_block" : "goo_block");
        } else {
            FalloutTooltips.add(tooltip, hot ? "hot_earth" : "earth");
        }
    }
}
