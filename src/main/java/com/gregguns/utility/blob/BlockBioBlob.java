package com.gregguns.utility.blob;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.utility.WorldBlockRegistration;

/**
 * The Bio Blob: the slime a Biogun leaves behind. It grows when hit again, bursts past size 3, decays one step per
 * {@code UtilityConfig.bioBlobDecayTicks} and poisons living things that touch it. Metadata is the face it sticks to.
 * The collision box follows the per-state bounds (MUTL-42). Both sides.
 *
 * <p>
 * Usage for the Biogun projectile (M5): {@code BlockBioBlob.onProjectileImpact(world, x, y, z, side, level, shooter)}
 * with the hit block's coordinates and face, on the server.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 3; fixes MUTL-41, MUTL-42, MUTL-43,
// MUTL-44, MUTL-45
public final class BlockBioBlob extends BlockContainer implements Tiered, HiddenFromCreativeTab {

    /** Poison amplifier 2 is Poison III. */
    private static final int POISON_AMPLIFIER = 2;
    private static final int POISON_TICKS = 100;

    /** Creates the block; the registrar names it. */
    public BlockBioBlob() {
        super(Material.rock);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
    }

    @Override
    public Tier getTier() {
        return Tier.LV;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileBioBlob();
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
    public boolean canHarvestBlock(EntityPlayer player, int meta) {
        return false;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        return new ArrayList<>();
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return blockIcon;
    }

    private static double[] boundsAt(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        int size = tile instanceof TileBioBlob ? ((TileBioBlob) tile).size() : BlobState.MIN_SIZE;
        return BlobState.bounds(size, world.getBlockMetadata(x, y, z));
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        double[] b = boundsAt(world, x, y, z);
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        double[] b = boundsAt(world, x, y, z);
        return AxisAlignedBB.getBoundingBox(x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5]);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        return getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 0.25F, 0.75F);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote || !(entity instanceof EntityLivingBase)) {
            return;
        }
        if (entity instanceof EntityPlayer && UtilityConfig.bioBlobSparesPlayers) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        PotionEffect current = living.getActivePotionEffect(Potion.poison);
        // Fixes MUTL-41: any living entity, players included unless the config spares them.
        if (current == null || current.getAmplifier() < POISON_AMPLIFIER || current.getDuration() < POISON_TICKS / 2) {
            living.addPotionEffect(new PotionEffect(Potion.poison.id, POISON_TICKS, POISON_AMPLIFIER));
        }
    }

    /**
     * What a Biogun projectile does where it lands. Server side only.
     *
     * <p>
     * A hit on an existing blob grows it. A hit on another block places a size-1 blob in the cell on the hit face,
     * clinging to the hit block, and grows it by {@code level - 1}.
     *
     * @param world   the world
     * @param x       X of the block that was hit
     * @param y       Y of the block that was hit
     * @param z       Z of the block that was hit
     * @param side    the face of that block that was hit (0 to 5)
     * @param level   the projectile's level, at least 1
     * @param shooter who gets the credit; may be null
     * @return true when a blob was placed, grown or burst
     */
    public static boolean onProjectileImpact(World world, int x, int y, int z, int side, int level,
        EntityLivingBase shooter) {
        if (world.isRemote) {
            return false;
        }
        if (world.getBlock(x, y, z) instanceof BlockBioBlob) {
            return hit(world, x, y, z, level, shooter);
        }
        ForgeDirection d = ForgeDirection.getOrientation(side);
        int nx = x + d.offsetX;
        int ny = y + d.offsetY;
        int nz = z + d.offsetZ;
        if (world.getBlock(nx, ny, nz) instanceof BlockBioBlob) {
            return hit(world, nx, ny, nz, level, shooter);
        }
        Block blob = WorldBlockRegistration.bioBlob();
        if (blob == null || !world.getBlock(nx, ny, nz)
            .isReplaceable(world, nx, ny, nz)) {
            return false;
        }
        world.setBlock(nx, ny, nz, blob, side, 3);
        if (level > 1) {
            hit(world, nx, ny, nz, level - 1, shooter);
        }
        return true;
    }

    /**
     * Grows or bursts the blob at a position. Server side only.
     *
     * @param world   the world
     * @param x       blob X
     * @param y       blob Y
     * @param z       blob Z
     * @param power   the hit's power
     * @param shooter who gets the credit; may be null
     * @return true when there was a blob to hit
     */
    public static boolean hit(World world, int x, int y, int z, int power, EntityLivingBase shooter) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileBioBlob)) {
            return false;
        }
        ((TileBioBlob) tile).hitBlob(power, shooter);
        return true;
    }
}
