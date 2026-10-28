package com.gregguns.explosion.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Irradiated earth: one block with five variants (dirt, stone, sand, gravel, cobblestone), made by fallout (D21). Each
 * variant has its own cool and hot texture (HBM's Nuclear Tech Mod art, reworked; see CREDITS.md). Hot earth glows
 * (light level 6), has lime veins in its texture and drips green sparks on the client. It does nothing by
 * itself: no random ticks, no spreading. Players standing on or beside it take GT's radioactivity effect unless they
 * wear hazmat ({@link RadiationExposure}), and decay puts the original block back ({@link FalloutEngine}). Mined, it
 * drops irradiated rubble.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class BlockIrradiatedEarth extends Block implements com.gregguns.api.Tiered {

    /** The lime the rubble item is tinted with (the blocks carry their colors in their textures). */
    public static final int TINT = 0x9CE070;

    private static final String[] VARIANT_NAMES = { "dirt", "stone", "sand", "gravel", "cobble" };

    @SideOnly(Side.CLIENT)
    private net.minecraft.util.IIcon[] cool;
    @SideOnly(Side.CLIENT)
    private net.minecraft.util.IIcon[] hot;

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
        int variant = FalloutBlocks.variantOf(meta);
        return FalloutBlocks.isHot(meta) ? hot[variant] : cool[variant];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        cool = new IIcon[FalloutBlocks.VARIANTS];
        hot = new IIcon[FalloutBlocks.VARIANTS];
        for (int i = 0; i < FalloutBlocks.VARIANTS; i++) {
            cool[i] = register.registerIcon("gregguns:thirdparty/ntm/irradiated_" + VARIANT_NAMES[i]);
            hot[i] = register.registerIcon("gregguns:thirdparty/ntm/irradiated_" + VARIANT_NAMES[i] + "_hot");
        }
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return FalloutBlocks.isHot(world.getBlockMetadata(x, y, z)) ? 6 : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (FalloutBlocks.isHot(world.getBlockMetadata(x, y, z)) && random.nextInt(4) == 0
            && !world.getBlock(x, y + 1, z)
                .isOpaqueCube()) {
            // "reddust" takes its color (r, g, b) in the motion arguments: a lime spark rising off the earth.
            world.spawnParticle(
                "reddust",
                x + random.nextDouble(),
                y + 1.05,
                z + random.nextDouble(),
                0.55,
                1.0,
                0.1);
        }
    }

    /**
     * Never silk-harvestable: the block would otherwise be placeable anywhere, outside every decay record, and never
     * revert. Mining always gives the rubble.
     */
    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return false;
    }

    @Override
    public int damageDropped(int meta) {
        return FalloutBlocks.isHot(meta) ? 1 : 0;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return FalloutBlocks.rubble();
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(FalloutBlocks.rubble(), 1, FalloutBlocks.isHot(meta) ? 1 : 0));
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
