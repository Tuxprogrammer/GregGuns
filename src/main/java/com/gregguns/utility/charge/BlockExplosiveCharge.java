package com.gregguns.utility.charge;

import java.util.List;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.utility.UtilityGuis;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The explosive charge block: metadata 0 is the Explosive Charge (TNT), 1 the Advanced Explosive Charge. A small plate
 * stuck to the face of another block; it is drawn by a special renderer from {@link ChargeModel}. Right-click opens the
 * settings GUI while unarmed. Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (Placement and shape),
// docs/gregguns-spec/09-machines-utility/explosive-charges.md
// Deviation: the block reports Tier.HV, the highest meta, because the self-check reads one tier per block;
// ChargeType.tier() has the
// per-meta tier and the recipe gate (the MV recipe still passes the rule "EU/t at most the output tier").
public final class BlockExplosiveCharge extends BlockContainer implements Tiered {

    /** The bare registry name. */
    public static final String NAME = "explosive_charge";
    /** Hardness of both charges; harvestable by hand, so breaking one defuses it. */
    private static final float HARDNESS = 0.1F;

    /** The block, set by {@link ChargeRegistration}; null while the utility module is off. */
    public static BlockExplosiveCharge instance;

    /** Creates the block; registration is in {@link ChargeRegistration}. */
    public BlockExplosiveCharge() {
        super(Material.circuits);
        setHardness(HARDNESS);
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileExplosiveCharge(ChargeType.fromMeta(meta));
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
    public int damageDropped(int meta) {
        return ChargeType.fromMeta(meta)
            .meta();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (ChargeType type : ChargeType.values()) {
            list.add(new ItemStack(item, 1, type.meta()));
        }
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        int orientation = tile instanceof TileExplosiveCharge ? ((TileExplosiveCharge) tile).orientation() : 1;
        double[] b = ChargeGeometry.bounds(orientation);
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileExplosiveCharge)) {
            return false;
        }
        // An armed charge ignores right-clicks, and so do wrenches (the baseline).
        if (!world.isRemote && !((TileExplosiveCharge) tile).params()
            .armed()) {
            player.openGui(GregGuns.instance, UtilityGuis.CHARGE, world, x, y, z);
        }
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, net.minecraft.block.Block neighbor) {
        if (world.isRemote) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileExplosiveCharge)) {
            return;
        }
        int[] d = ChargeGeometry.direction(((TileExplosiveCharge) tile).orientation());
        if (world.isAirBlock(x + d[0], y + d[1], z + d[2])) {
            // The block it was stuck to is gone: it falls off as an unarmed item.
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }
}
