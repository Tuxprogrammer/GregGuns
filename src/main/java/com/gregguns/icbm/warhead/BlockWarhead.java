package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A placed warhead (one block per casing tier). Drawn by a static block model (see the client renderer), with a
 * collision box that follows the model. Alone it is a demolition charge: redstone or a lighter starts a fuse. Breaking
 * it, or a filler above it, drops one item that keeps the explosive and fill. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md
public final class BlockWarhead extends BlockContainer implements Tiered {

    private final WarheadCasing casing;
    private final double boxWidth;

    /**
     * @param casing the casing this block is
     */
    public BlockWarhead(WarheadCasing casing) {
        super(Material.iron);
        this.casing = casing;
        this.boxWidth = WarheadGeometry.boxWidth(casing.boxWidth());
        setHardness(1.5F);
        setResistance(6.0F);
        setStepSound(soundTypeMetal);
        double[] b = WarheadGeometry.bounds(boxWidth);
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    /** @return the casing */
    public WarheadCasing casing() {
        return casing;
    }

    /** @return the width of the collision column, blocks */
    public double columnWidth() {
        return boxWidth;
    }

    @Override
    public Tier getTier() {
        return casing == WarheadCasing.T1 ? Tier.HV : Tier.EV;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileWarhead();
    }

    @Override
    public int getRenderType() {
        return WarheadRender.id;
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
    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        // Another explosion sets it off after a short fuse (spec: chain reaction), rather than just deleting it.
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileWarhead) {
            ((TileWarhead) world.getTileEntity(x, y, z)).arm(WarheadFuse.CHAIN_TICKS, null);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (WarheadBlast blast : WarheadBlast.values()) {
            if (blast.tier() == casing.tier()) {
                ExplosiveDef def = Explosives.get(blast.id());
                list.add(WarheadStacks.make(item, casing, blast.id(), WarheadStacks.cap(casing, def)));
            }
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        // The item is dropped in breakBlock, where the tile still exists to read the NBT from.
        return new ArrayList<>();
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileWarhead ? stackOf((TileWarhead) tile) : super.getPickBlock(target, world, x, y, z);
    }

    ItemStack stackOf(TileWarhead tile) {
        return WarheadStacks.make(
            Item.getItemFromBlock(this),
            casing,
            tile.data()
                .explosive(),
            tile.data()
                .fill());
    }

    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileWarhead && player.capabilities.isCreativeMode) {
            ((TileWarhead) tile).setNoDrop();
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!world.isRemote && tile instanceof TileWarhead) {
            TileWarhead warhead = (TileWarhead) tile;
            if (!warhead.removing()) {
                warhead.setRemoving();
                if (!warhead.noDrop()) {
                    EntityItem drop = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stackOf(warhead));
                    drop.delayBeforeCanPickup = 10;
                    world.spawnEntityInWorld(drop);
                }
                WarheadFillers.remove(world, x, y, z, warhead.height());
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, java.util.Random rand) {
        // Only a lit warhead (the tile was synced as armed) smokes and sparks, from the top of the casing.
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileWarhead) || !((TileWarhead) tile).armed()) {
            return;
        }
        double px = x + 0.5 + (rand.nextDouble() - 0.5) * 0.2;
        double pz = z + 0.5 + (rand.nextDouble() - 0.5) * 0.2;
        double top = y + 0.95;
        world.spawnParticle("smoke", px, top, pz, 0.0, 0.05, 0.0);
        if (rand.nextInt(2) == 0) {
            world.spawnParticle("lava", px, top, pz, 0.0, 0.0, 0.0);
        }
        world.spawnParticle("reddust", px, top, pz, 0.0, 0.0, 0.0);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        lightIfPowered(world, x, y, z, true);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        lightIfPowered(world, x, y, z, false);
    }

    private static void lightIfPowered(World world, int x, int y, int z, boolean needInit) {
        if (world.isRemote || !world.isBlockIndirectlyGettingPowered(x, y, z)) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        // On placement the tile still holds the default fill; init lights it again with the real one.
        if (tile instanceof TileWarhead && (!needInit || ((TileWarhead) tile).initialized())) {
            ((TileWarhead) tile).arm(((TileWarhead) tile).litFuseTicks(), null);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (held == null || (held.getItem() != Items.flint_and_steel && held.getItem() != Items.fire_charge)) {
            return false;
        }
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileWarhead) {
            TileWarhead lit = (TileWarhead) world.getTileEntity(x, y, z);
            lit.arm(lit.litFuseTicks(), player);
            if (held.getItem() == Items.flint_and_steel) {
                held.damageItem(1, player);
            } else if (!player.capabilities.isCreativeMode) {
                held.stackSize--;
            }
        }
        return true;
    }
}
