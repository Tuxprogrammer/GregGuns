package com.gregguns.explosion.nuclear;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Fallout goo (D21): rare, near ground zero. While any goo stands in a chunk, that chunk's irradiated earth does not
 * decay, so cleaning the goo up is how a player lets the land recover. Metadata 1 is hot goo from a ZPM or better
 * blast (trace fermium after processing). The block never reverts by itself; the world's goo ledger
 * ({@link FalloutSavedData}) learns of it when fallout places it and when it is broken.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
public final class BlockFalloutGoo extends Block implements com.gregguns.api.Tiered {

    /** Builds the block. */
    public BlockFalloutGoo() {
        super(Material.clay);
        setHardness(0.8F);
        setResistance(2.0F);
        setStepSound(soundTypeGravel);
        setHarvestLevel("shovel", 0);
    }

    @Override
    public com.gregguns.api.Tier getTier() {
        return com.gregguns.api.Tier.ZPM;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return (meta & 1) == 1 ? ItemIrradiatedRubble.HOT_TINT : 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return getRenderColor(world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return false;
    }

    @Override
    public int damageDropped(int meta) {
        return meta & 1;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(FalloutBlocks.gooItem(), 1, meta & 1));
        return drops;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (!world.isRemote) {
            FalloutEngine.gooRemoved(world, x, y, z);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }
}
