package com.gregguns.explosion.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Irradiated earth: one block with five variants (dirt, stone, sand, gravel, cobblestone), made by fallout (D21). It
 * borrows the vanilla textures, tinted sickly green on the client, so it needs no art of its own. It does nothing by
 * itself: no random ticks, no spreading. Players standing on or beside it take GT's radioactivity effect unless they
 * wear hazmat ({@link RadiationExposure}), and decay puts the original block back ({@link FalloutEngine}). Mined, it
 * drops irradiated rubble.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class BlockIrradiatedEarth extends Block implements com.gregguns.api.Tiered {

    /** The tint applied on the client. */
    public static final int TINT = 0x9CE070;

    /** Builds the block (no tick randomly: the default is already false). */
    public BlockIrradiatedEarth() {
        super(Material.ground);
        setHardness(1.0F);
        setResistance(3.0F);
        setStepSound(soundTypeGravel);
        setHarvestLevel("shovel", 0);
    }

    @Override
    public com.gregguns.api.Tier getTier() {
        return com.gregguns.api.Tier.ZPM;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return FalloutBlocks.original(meta % FalloutBlocks.VARIANTS)
            .getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        // The vanilla textures are used as they are.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return TINT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return TINT;
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return FalloutBlocks.rubble();
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(FalloutBlocks.rubble(), 1, 0));
        return drops;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < FalloutBlocks.VARIANTS; i++) {
            list.add(new ItemStack(item, 1, i));
        }
    }
}
