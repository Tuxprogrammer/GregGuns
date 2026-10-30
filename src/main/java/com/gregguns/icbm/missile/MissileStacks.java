package com.gregguns.icbm.missile;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.icbm.warhead.BlockWarhead;
import com.gregguns.icbm.warhead.TileWarhead;
import com.gregguns.icbm.warhead.TileWarheadFiller;

/**
 * The stack-on-pad build (D31, 0018 §8): checks that a body stands complete with a fitting warhead on its nose and
 * fuses the stack into the missile entity. Server side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md (Arming)
public final class MissileStacks {

    /** What arming came to. The player sees one lang key per value ({@code gregguns.icbm.arm.<name lowercased>}). */
    public enum ArmResult {
        /** The missile entity now stands where the stack was. */
        OK,
        /** The clicked block is not a missile body. */
        NOT_BODY,
        /** A filler cell of the body is missing or broken. */
        INCOMPLETE,
        /** No warhead sits on the nose. */
        NO_WARHEAD,
        /** The warhead is too heavy for this body tier. */
        WRONG_WARHEAD,
        /** The warhead's fuse is lit. */
        WARHEAD_LIT
    }

    /** The outcome of {@link #armIntoEntity}: the result and, on {@code OK}, the new entity. */
    public static final class Armed {

        /** The outcome. */
        public final ArmResult result;
        /** The missile entity; null unless {@code result} is {@code OK}. */
        public final EntityMissile missile;

        Armed(ArmResult result, EntityMissile missile) {
            this.result = result;
            this.missile = missile;
        }
    }

    private MissileStacks() {}

    /**
     * @param world the world
     * @param x     X of the base cell
     * @param y     Y of the base cell
     * @param z     Z of the base cell
     * @param tier  the body tier
     * @return whether the base cell, the filler cells above it and one cell for the warhead are free
     */
    public static boolean canPlaceBody(World world, int x, int y, int z, MissileTier tier) {
        for (int i = 0; i < tier.heightBlocks() + 1; i++) {
            if (y + i > 255) {
                return false;
            }
            Block b = world.getBlock(x, y + i, z);
            if (!b.isReplaceable(world, x, y + i, z) && !b.isAir(world, x, y + i, z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Stands a body block with its filler cells at a base cell, as the body item does (the player's hand) and the silo
     * does from its Input Bus (D285). The caller checks {@link #canPlaceBody} and sets the owner. Server side.
     *
     * @param world    the server world
     * @param x        X of the base cell
     * @param y        Y of the base cell
     * @param z        Z of the base cell
     * @param tier     the body tier
     * @param guidance guidance tiers above the body's own
     * @param onPad    whether the body stands on a Galacticraft pad (its model is lowered onto the pad's surface)
     * @return the body's tile, or null when the block did not take
     */
    public static TileMissileBody placeBody(World world, int x, int y, int z, MissileTier tier, int guidance,
        boolean onPad) {
        if (!world.setBlock(x, y, z, MissileBlocks.body(tier), 0, 3)) {
            return null;
        }
        TileEntity t = world.getTileEntity(x, y, z);
        if (!(t instanceof TileMissileBody)) {
            return null;
        }
        ((TileMissileBody) t).init(guidance, onPad, null);
        for (int i = 1; i < tier.heightBlocks(); i++) {
            world.setBlock(x, y + i, z, MissileBlocks.filler(), 0, 2);
            TileEntity f = world.getTileEntity(x, y + i, z);
            if (f instanceof TileWarheadFiller) {
                ((TileWarheadFiller) f).setOffset(0, -i, 0);
            }
        }
        return (TileMissileBody) t;
    }

    /**
     * Seats a warhead block on a body's nose from a warhead item, as a player's placement does, without a player: the
     * silo's mating step (D285). Server side.
     *
     * @param world     the server world
     * @param x         X of the nose cell (the cell above the body's top filler)
     * @param y         Y of the nose cell
     * @param z         Z of the nose cell
     * @param stack     the warhead item, read for its explosive and fill; not changed
     * @param ownerId   the owner, may be null
     * @param ownerName the owner's name, may be null
     * @return whether the warhead now sits there; false when a cell it needs is taken
     */
    public static boolean placeWarhead(World world, int x, int y, int z, net.minecraft.item.ItemStack stack,
        java.util.UUID ownerId, String ownerName) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (!(block instanceof BlockWarhead) || y > 255) {
            return false;
        }
        com.gregguns.icbm.warhead.WarheadCasing casing = ((BlockWarhead) block).casing();
        Block here = world.getBlock(x, y, z);
        if (!here.isReplaceable(world, x, y, z) && !here.isAir(world, x, y, z)
            || !com.gregguns.icbm.warhead.WarheadFillers.canPlace(world, x, y, z, casing.heightBlocks())) {
            return false;
        }
        if (!world.setBlock(x, y, z, block, 0, 3) || !(world.getTileEntity(x, y, z) instanceof TileWarhead)) {
            return false;
        }
        TileWarhead tile = (TileWarhead) world.getTileEntity(x, y, z);
        tile.init(casing, stack.getTagCompound(), null);
        if (ownerId != null) {
            tile.setOwner(ownerId, ownerName);
        }
        com.gregguns.icbm.warhead.WarheadFillers.place(world, x, y, z, casing.heightBlocks());
        return true;
    }

    /**
     * Finds the base cell of the body that a cell belongs to.
     *
     * @return {x, y, z} of the base block, or null when the cell is not part of a body
     */
    public static int[] baseOf(World world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        if (b instanceof BlockMissileBody) {
            return new int[] { x, y, z };
        }
        if (b instanceof BlockMissileFiller) {
            TileEntity t = world.getTileEntity(x, y, z);
            if (t instanceof TileWarheadFiller) {
                TileWarheadFiller f = (TileWarheadFiller) t;
                if (world.getBlock(f.mainX(), f.mainY(), f.mainZ()) instanceof BlockMissileBody) {
                    return new int[] { f.mainX(), f.mainY(), f.mainZ() };
                }
            }
        }
        return null;
    }

    /**
     * Arms the stack: replaces the body, its filler cells and the warhead with a missile entity of the same tier, with
     * the warhead's explosive and fill and the body's guidance.
     *
     * @param world  the server world
     * @param x      X of any cell of the body
     * @param y      Y
     * @param z      Z
     * @param player who armed it, may be null
     * @return the outcome
     */
    public static ArmResult arm(World world, int x, int y, int z, EntityPlayer player) {
        return armIntoEntity(world, x, y, z, player).result;
    }

    /**
     * Same as {@link #arm}, and hands back the entity it made.
     *
     * @param world  the server world
     * @param x      X of any cell of the body
     * @param y      Y
     * @param z      Z
     * @param player who armed it, may be null
     * @return the outcome and the entity
     */
    public static Armed armIntoEntity(World world, int x, int y, int z, EntityPlayer player) {
        int[] base = baseOf(world, x, y, z);
        if (base == null) {
            return new Armed(ArmResult.NOT_BODY, null);
        }
        BlockMissileBody block = (BlockMissileBody) world.getBlock(base[0], base[1], base[2]);
        MissileTier tier = block.missileTier();
        TileEntity bodyTile = world.getTileEntity(base[0], base[1], base[2]);
        if (!(bodyTile instanceof TileMissileBody)) {
            return new Armed(ArmResult.INCOMPLETE, null);
        }
        for (int i = 1; i < tier.heightBlocks(); i++) {
            if (!(world.getBlock(base[0], base[1] + i, base[2]) instanceof BlockMissileFiller)) {
                return new Armed(ArmResult.INCOMPLETE, null);
            }
        }
        int topY = base[1] + tier.heightBlocks();
        Block above = world.getBlock(base[0], topY, base[2]);
        TileEntity wt = world.getTileEntity(base[0], topY, base[2]);
        if (!(above instanceof BlockWarhead) || !(wt instanceof TileWarhead)) {
            return new Armed(ArmResult.NO_WARHEAD, null);
        }
        BlockWarhead warheadBlock = (BlockWarhead) above;
        TileWarhead warhead = (TileWarhead) wt;
        if (!tier.carries(
            warheadBlock.casing()
                .tier())) {
            return new Armed(ArmResult.WRONG_WARHEAD, null);
        }
        if (warhead.armed()) {
            return new Armed(ArmResult.WARHEAD_LIT, null);
        }
        TileMissileBody body = (TileMissileBody) bodyTile;
        EntityMissile missile = EntityMissile.create(tier, world);
        double y0 = body.onPad() ? base[1] - MissileSeat.PAD_DROP : base[1];
        missile.setLocationAndAngles(base[0] + 0.5, y0, base[2] + 0.5, 0.0F, 0.0F);
        missile.configure(
            body.guidance(),
            warhead.data()
                .explosive(),
            warhead.data()
                .fill(),
            warheadBlock.casing()
                .number(),
            player);
        if (player == null && body.ownerId() != null) {
            missile.setOwner(body.ownerId(), body.ownerName());
        }
        warhead.setNoDrop();
        body.setNoDrop();
        world.setBlockToAir(base[0], topY, base[2]);
        world.setBlockToAir(base[0], base[1], base[2]);
        world.spawnEntityInWorld(missile);
        return new Armed(ArmResult.OK, missile);
    }
}
