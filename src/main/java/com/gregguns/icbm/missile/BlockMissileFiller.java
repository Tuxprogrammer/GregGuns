package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.icbm.warhead.TileWarheadFiller;
import com.gregguns.icbm.warhead.WarheadGeometry;
import com.gregguns.api.HiddenFromCreativeTab;

/**
 * An invisible cell of a missile body taller than one block (D31). It collides like its slice of the model, passes
 * clicks and picks to the base block, and takes the whole body with it when broken. It reuses the warhead filler's
 * tile for the offset to the base. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md (Filler cells)
public final class BlockMissileFiller extends BlockContainer implements HiddenFromCreativeTab, Tiered {

    /** The bare registry name. */
    public static final String NAME = "missile_filler";

    /** Creates the block. */
    public BlockMissileFiller() {
        super(Material.iron);
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
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

    /** @return the base block's tile, or null */
    static TileMissileBody base(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileWarheadFiller)) {
            return null;
        }
        TileWarheadFiller f = (TileWarheadFiller) tile;
        TileEntity m = world.getTileEntity(f.mainX(), f.mainY(), f.mainZ());
        return m instanceof TileMissileBody ? (TileMissileBody) m : null;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        double width = 0.5;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileWarheadFiller) {
            TileWarheadFiller f = (TileWarheadFiller) tile;
            Block m = world.getBlock(f.mainX(), f.mainY(), f.mainZ());
            if (m instanceof BlockMissileBody) {
                width = ((BlockMissileBody) m).columnWidth();
            }
        }
        double[] b = WarheadGeometry.bounds(width);
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileMissileBody b = base(world, x, y, z);
        return b == null ? null
            : ((BlockMissileBody) b.getBlockType()).getPickBlock(target, world, b.xCoord, b.yCoord, b.zCoord);
    }

    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        TileMissileBody b = base(world, x, y, z);
        if (b != null && player.capabilities.isCreativeMode) {
            b.setNoDrop();
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (!world.isRemote) {
            TileMissileBody b = base(world, x, y, z);
            if (b != null && !b.removing()) {
                // Breaking any cell breaks the whole body: the base drops the item and clears the other cells.
                world.setBlockToAir(b.xCoord, b.yCoord, b.zCoord);
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.isRemote && base(world, x, y, z) == null) {
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileMissileBody b = base(world, x, y, z);
        return b != null && b.getBlockType()
            .onBlockActivated(world, b.xCoord, b.yCoord, b.zCoord, player, side, hitX, hitY, hitZ);
    }
}
