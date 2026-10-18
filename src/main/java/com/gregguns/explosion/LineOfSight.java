package com.gregguns.explosion;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Whether light (and a flash, and a fragment) gets from one point to another: opaque blocks stop it, glass and other
 * see-through blocks do not. Server side; reads loaded blocks only.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Flashbang, Frag Grenade)
final class LineOfSight {

    private static final int MAX_PASSES = 8;

    private LineOfSight() {}

    /**
     * @param world the world
     * @param ax    start X
     * @param ay    start Y
     * @param az    start Z
     * @param bx    end X
     * @param by    end Y
     * @param bz    end Z
     * @return true when no opaque block lies on the segment
     */
    static boolean clear(World world, double ax, double ay, double az, double bx, double by, double bz) {
        Vec3 from = Vec3.createVectorHelper(ax, ay, az);
        Vec3 to = Vec3.createVectorHelper(bx, by, bz);
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6) {
            return true;
        }
        for (int i = 0; i < MAX_PASSES; i++) {
            // Flags: no liquids, skip blocks without a collision box, no "last uncollidable block".
            MovingObjectPosition hit = world.func_147447_a(from, to, false, true, false);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return true;
            }
            Block block = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
            if (block.isOpaqueCube()) {
                return false;
            }
            // A see-through block (glass, fence, open door): carry on from just past the hit.
            double nudge = 0.02 / length;
            from = Vec3.createVectorHelper(
                hit.hitVec.xCoord + dx * nudge,
                hit.hitVec.yCoord + dy * nudge,
                hit.hitVec.zCoord + dz * nudge);
        }
        return false;
    }
}
