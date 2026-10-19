package com.gregguns.utility.storage;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.utility.UtilityGuis;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Reinforced Chest and the Weapon Chest, one class with a flag. Single chests only; both are public storage as in
 * Techguns. Neither gives redstone power (fixes MUTL-35); both give a comparator signal. Facing is the metadata (2 to
 * 5). Render type -1: a tile-entity renderer draws the vanilla chest model. Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 1; docs/gregguns-spec/07-blocks/storage-
// chests.md; fixes MUTL-35
// Deviation: no double chests, no wrench pickup, no stored-contents item tag (M6 plan decision 13).
public final class BlockGregChest extends BlockContainer implements Tiered {

    private final boolean weapon;
    private IIcon particle;

    /**
     * @param weapon true for the Weapon Chest, false for the Reinforced Chest
     */
    public BlockGregChest(boolean weapon) {
        super(Material.wood);
        this.weapon = weapon;
        setHardness(5.0F);
        setResistance(500.0F);
        setStepSound(soundTypeWood);
        setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
    }

    /** @return true for the Weapon Chest */
    public boolean isWeaponChest() {
        return weapon;
    }

    @Override
    public Tier getTier() {
        return Tier.LV;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileGregChest();
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
        particle = register.registerIcon(getTextureName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return particle;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int meta = facing == 0 ? 2 : facing == 1 ? 5 : facing == 2 ? 3 : 4;
        world.setBlockMetadataWithNotify(x, y, z, meta, 2);
        if (stack != null && stack.hasDisplayName()) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileGregChest) {
                ((TileGregChest) tile).setCustomName(stack.getDisplayName());
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy,
        float hz) {
        if (world.isRemote) {
            return true;
        }
        if (isBlocked(world, x, y, z)) {
            return true;
        }
        player.openGui(GregGuns.instance, UtilityGuis.CHEST, world, x, y, z);
        return true;
    }

    /** A solid block above the chest, or a sitting ocelot on it, keeps the lid shut. */
    private static boolean isBlocked(World world, int x, int y, int z) {
        if (world.getBlock(x, y + 1, z)
            .isNormalCube()) {
            return true;
        }
        for (Object o : world.getEntitiesWithinAABB(
            EntityOcelot.class,
            AxisAlignedBB.getBoundingBox(x, y + 1, z, x + 1, y + 2, z + 1))) {
            if (((EntityOcelot) o).isSitting()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileGregChest && !world.isRemote) {
            Random rand = world.rand;
            for (ItemStack stack : ((TileGregChest) tile).rawItems()) {
                if (stack == null) {
                    continue;
                }
                EntityItem drop = new EntityItem(
                    world,
                    x + rand.nextFloat() * 0.8F + 0.1F,
                    y + rand.nextFloat() * 0.8F + 0.1F,
                    z + rand.nextFloat() * 0.8F + 0.1F,
                    stack.copy());
                drop.motionX = rand.nextGaussian() * 0.05;
                drop.motionY = rand.nextGaussian() * 0.05 + 0.2;
                drop.motionZ = rand.nextGaussian() * 0.05;
                world.spawnEntityInWorld(drop);
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileGregChest ? Container.calcRedstoneFromInventory((TileGregChest) tile) : 0;
    }
}
