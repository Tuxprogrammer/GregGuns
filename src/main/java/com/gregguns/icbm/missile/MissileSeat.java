package com.gregguns.icbm.missile;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import com.gregguns.icbm.warhead.TileWarheadFiller;

/**
 * How far a stack is drawn below its block grid. A Galacticraft pad is only 0.2 of a block tall, so a body on it is
 * lowered by 0.8 to stand on the pad's surface, and a warhead seated on that body follows it. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md (Seating on the pad)
public final class MissileSeat {

    /** The drop, blocks: one block minus the pad's height. */
    public static final double PAD_DROP = 1.0 - EntityMissile.PAD_SURFACE;

    private MissileSeat() {}

    /**
     * @param world a world
     * @param x     X of a missile body block
     * @param y     Y
     * @param z     Z
     * @return the drop for the body at that cell: {@link #PAD_DROP} on a pad, else 0
     */
    public static double bodyDrop(IBlockAccess world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileMissileBody && ((TileMissileBody) t).onPad() ? PAD_DROP : 0.0;
    }

    /**
     * @param world a world
     * @param x     X of a warhead block
     * @param y     Y
     * @param z     Z
     * @return how far the warhead at that cell is drawn down: it seats on the body below it when one is there
     */
    public static double warheadDrop(IBlockAccess world, int x, int y, int z) {
        Block below = world.getBlock(x, y - 1, z);
        if (below instanceof BlockMissileBody) {
            return bodyDrop(world, x, y - 1, z);
        }
        if (below instanceof BlockMissileFiller) {
            TileEntity t = world.getTileEntity(x, y - 1, z);
            if (t instanceof TileWarheadFiller) {
                TileWarheadFiller f = (TileWarheadFiller) t;
                return bodyDrop(world, f.mainX(), f.mainY(), f.mainZ());
            }
        }
        return 0.0;
    }
}
