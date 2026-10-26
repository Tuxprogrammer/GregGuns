package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.icbm.warhead.WarheadGeometry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A placed missile body (D31): a full-height model standing on its fins, the base cell here and invisible filler cells
 * above it ({@link BlockMissileFiller}). The warhead goes on its nose; sneaking with an empty hand arms the stack into
 * the missile entity. Breaking any cell drops the one body item. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md
public final class BlockMissileBody extends BlockContainer implements Tiered {

    private final MissileTier tier;
    private final double columnWidth;

    /** @param tier the tier */
    public BlockMissileBody(MissileTier tier) {
        super(Material.iron);
        this.tier = tier;
        this.columnWidth = WarheadGeometry.boxWidth(tier == MissileTier.T1 ? 0.5 : 0.4);
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        double[] b = WarheadGeometry.bounds(columnWidth);
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    /** @return the tier */
    public MissileTier missileTier() {
        return tier;
    }

    /** @return width of the collision column */
    public double columnWidth() {
        return columnWidth;
    }

    @Override
    public Tier getTier() {
        return tier == MissileTier.T1 ? Tier.HV : Tier.EV;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileMissileBody();
    }

    @Override
    public int getRenderType() {
        return MissileRender.id;
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
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(MissileBodies.body(tier));
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        return new ArrayList<>();
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        return MissileBodies.body(tier, t instanceof TileMissileBody ? ((TileMissileBody) t).guidance() : 0);
    }

    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileMissileBody && player.capabilities.isCreativeMode) {
            ((TileMissileBody) tile).setNoDrop();
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!world.isRemote && tile instanceof TileMissileBody) {
            TileMissileBody body = (TileMissileBody) tile;
            if (!body.removing()) {
                body.setRemoving();
                if (!body.noDrop()) {
                    EntityItem drop = new EntityItem(
                        world,
                        x + 0.5,
                        y + 0.5,
                        z + 0.5,
                        MissileBodies.body(tier, body.guidance()));
                    drop.delayBeforeCanPickup = 10;
                    world.spawnEntityInWorld(drop);
                }
                for (int i = 1; i < tier.heightBlocks(); i++) {
                    if (world.getBlock(x, y + i, z) instanceof BlockMissileFiller) {
                        world.setBlockToAir(x, y + i, z);
                    }
                }
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        if (player.isSneaking() && player.getHeldItem() == null) {
            MissileStacks.ArmResult result = MissileStacks.arm(world, x, y, z, player);
            player.addChatMessage(
                new ChatComponentTranslation("gregguns.icbm.arm." + result.name().toLowerCase(java.util.Locale.ROOT)));
        } else {
            player.addChatMessage(new ChatComponentTranslation("gregguns.icbm.body.hint", tier.number()));
        }
        return true;
    }
}
