package com.gregguns.utility.camp;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.config.UtilityConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Camp Flag: the spawner block of generated military camps. Players never craft it and the creative tab does not
 * list it; right-click does nothing. Breaking it drops one iron ingot (damage 0, MUTL-48) and, when the config allows,
 * tells the {@link CampSpawnPorts active port}, which will send a helicopter once NPCs exist. Drawn by a tile-entity
 * renderer (render type -1). Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 2; fixes MUTL-48
public final class BlockCampFlag extends BlockContainer implements Tiered, HiddenFromCreativeTab {

    private IIcon icon;

    /** Creates the block; the registrar names it. */
    public BlockCampFlag() {
        super(Material.iron);
        setHardness(3.0F);
        setResistance(30.0F);
        setStepSound(soundTypeMetal);
        setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 1.0F, 0.9F);
    }

    @Override
    public Tier getTier() {
        return Tier.LV;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileCampFlag();
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icon = register.registerIcon(getTextureName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icon;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        // Fixes MUTL-48: always the plain iron ingot.
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Items.iron_ingot, 1, 0));
        return drops;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        boolean helicopter = !world.isRemote && UtilityConfig.campFlagBreakHelicopter;
        super.breakBlock(world, x, y, z, block, meta);
        if (helicopter) {
            CampSpawnPorts.get()
                .onFlagBroken(world, x, y, z);
        }
    }
}
