package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gregguns.api.HiddenFromCreativeTab;

/**
 * An invisible filler cell of a warhead taller than one block (D31). It collides like its slice of the model, passes
 * clicks, picks and breaks to the main block, and removes itself when the main block is gone. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Filler blocks)
public final class BlockWarheadFiller extends BlockContainer implements HiddenFromCreativeTab {

    /** The bare registry name. */
    public static final String NAME = "warhead_filler";

    private static final double DEFAULT_WIDTH = 0.5;

    /** Creates the block; registration is in {@link WarheadRegistration}. */
    public BlockWarheadFiller() {
        super(Material.iron);
        setHardness(1.5F);
        setResistance(6.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileWarheadFiller();
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
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        return new ArrayList<>();
    }

    @Override
    public boolean canDropFromExplosion(net.minecraft.world.Explosion explosion) {
        return false;
    }

    private static TileWarhead main(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileWarheadFiller)) {
            return null;
        }
        TileWarheadFiller f = (TileWarheadFiller) tile;
        TileEntity m = world.getTileEntity(f.mainX(), f.mainY(), f.mainZ());
        return m instanceof TileWarhead ? (TileWarhead) m : null;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        double width = DEFAULT_WIDTH;
        if (tile instanceof TileWarheadFiller) {
            TileWarheadFiller f = (TileWarheadFiller) tile;
            Block m = world.getBlock(f.mainX(), f.mainY(), f.mainZ());
            if (m instanceof BlockWarhead) {
                width = ((BlockWarhead) m).columnWidth();
            }
        }
        double[] b = WarheadGeometry.bounds(width);
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    @Override
    public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileWarhead main = main(world, x, y, z);
        return main == null ? null : ((BlockWarhead) main.getBlockType()).stackOf(main);
    }

    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        TileWarhead main = main(world, x, y, z);
        if (main != null && player.capabilities.isCreativeMode) {
            main.setNoDrop();
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (!world.isRemote) {
            TileWarhead main = main(world, x, y, z);
            if (main != null && !main.removing()) {
                // Breaking any cell breaks the whole warhead: the main block drops the item and clears the rest.
                world.setBlockToAir(main.xCoord, main.yCoord, main.zCoord);
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) {
            return;
        }
        TileWarhead main = main(world, x, y, z);
        if (main == null) {
            // The main block is gone: a filler is never left behind.
            world.setBlockToAir(x, y, z);
        } else if (world.isBlockIndirectlyGettingPowered(x, y, z)) {
            main.arm(WarheadFuse.FUSE_TICKS, null);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileWarhead main = main(world, x, y, z);
        if (main == null) {
            return false;
        }
        return main.getBlockType()
            .onBlockActivated(world, main.xCoord, main.yCoord, main.zCoord, player, side, hitX, hitY, hitZ);
    }
}
