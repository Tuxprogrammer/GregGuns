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

    private MissileStacks() {}

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
        int[] base = baseOf(world, x, y, z);
        if (base == null) {
            return ArmResult.NOT_BODY;
        }
        BlockMissileBody block = (BlockMissileBody) world.getBlock(base[0], base[1], base[2]);
        MissileTier tier = block.missileTier();
        TileEntity bodyTile = world.getTileEntity(base[0], base[1], base[2]);
        if (!(bodyTile instanceof TileMissileBody)) {
            return ArmResult.INCOMPLETE;
        }
        for (int i = 1; i < tier.heightBlocks(); i++) {
            if (!(world.getBlock(base[0], base[1] + i, base[2]) instanceof BlockMissileFiller)) {
                return ArmResult.INCOMPLETE;
            }
        }
        int topY = base[1] + tier.heightBlocks();
        Block above = world.getBlock(base[0], topY, base[2]);
        TileEntity wt = world.getTileEntity(base[0], topY, base[2]);
        if (!(above instanceof BlockWarhead) || !(wt instanceof TileWarhead)) {
            return ArmResult.NO_WARHEAD;
        }
        BlockWarhead warheadBlock = (BlockWarhead) above;
        TileWarhead warhead = (TileWarhead) wt;
        if (!tier.carries(
            warheadBlock.casing()
                .tier())) {
            return ArmResult.WRONG_WARHEAD;
        }
        if (warhead.armed()) {
            return ArmResult.WARHEAD_LIT;
        }
        TileMissileBody body = (TileMissileBody) bodyTile;
        EntityMissile missile = tier == MissileTier.T1 ? new EntityMissileT1(world) : new EntityMissileT2(world);
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
        return ArmResult.OK;
    }
}
