package com.gregguns.projectile;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Reads the world for one {@link Hitscan} trace: the nearest solid block (passable blocks such as grass and open
 * doors are skipped), the entities that may be hit, and no chunk that is not already loaded. Server only.
 *
 * <pre>
 * {@code
 * HitscanTracer.Trace t = HitscanTracer.trace(world, shooter, x, y, z, dx, dy, dz, 37.5);
 * if (t.entity() != null) { ... t.result().distance() ... }
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.3); bugs PFX-23 (nearer wins) and PFX-27 (passable
// blocks are skipped) are fixed here and in Hitscan.
public final class HitscanTracer {

    /** Passable blocks a ray flies through before the trace gives up looking for a wall. */
    private static final int MAX_PASSABLE_BLOCKS = 16;
    /** Step along the ray, blocks, at which chunk loading is checked. */
    private static final double CHUNK_STEP = 8.0;
    private static final double SWEEP_MARGIN = 1.0;

    private HitscanTracer() {}

    /** A trace result and the entity it hit, if any. */
    public static final class Trace {

        private final Hitscan.Result result;
        private final Entity entity;

        Trace(Hitscan.Result result, Entity entity) {
            this.result = result;
            this.entity = entity;
        }

        /** @return the pure result: kind, distance and end point */
        public Hitscan.Result result() {
            return result;
        }

        /** @return the entity hit, or {@code null} for a block or a miss */
        public Entity entity() {
            return entity;
        }
    }

    /**
     * Traces a ray through the world.
     *
     * @param world   the world; a client world is allowed but the result is only authoritative on the server
     * @param shooter the entity that fired, never hit (nor the entity it rides), or {@code null}
     * @param ox      origin X
     * @param oy      origin Y
     * @param oz      origin Z
     * @param dx      direction X, any length above zero
     * @param dy      direction Y
     * @param dz      direction Z
     * @param range   maximum length in blocks
     * @return the trace
     */
    public static Trace trace(World world, EntityLivingBase shooter, double ox, double oy, double oz, double dx,
        double dy, double dz, double range) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-12 || range <= 0) {
            return new Trace(Hitscan.trace(ox, oy, oz, dx, dy, dz, range, HitRules.NO_BLOCK, new double[0], 0), null);
        }
        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;
        double reach = loadedReach(world, ox, oz, ux, uz, range);
        double blockDistance = blockDistance(world, ox, oy, oz, ux, uy, uz, reach);

        double end = Math.min(reach, blockDistance);
        AxisAlignedBB sweep = AxisAlignedBB.getBoundingBox(
            Math.min(ox, ox + ux * end) - SWEEP_MARGIN,
            Math.min(oy, oy + uy * end) - SWEEP_MARGIN,
            Math.min(oz, oz + uz * end) - SWEEP_MARGIN,
            Math.max(ox, ox + ux * end) + SWEEP_MARGIN,
            Math.max(oy, oy + uy * end) + SWEEP_MARGIN,
            Math.max(oz, oz + uz * end) + SWEEP_MARGIN);
        @SuppressWarnings("unchecked")
        List<Entity> found = world.getEntitiesWithinAABBExcludingEntity(shooter, sweep);
        Entity[] candidates = new Entity[found.size()];
        double[] boxes = new double[found.size() * Hitscan.BOX_STRIDE];
        int count = 0;
        for (Entity e : found) {
            if (!canHit(e, shooter) || e.boundingBox == null) {
                continue;
            }
            AxisAlignedBB box = e.boundingBox;
            int b = count * Hitscan.BOX_STRIDE;
            boxes[b] = box.minX;
            boxes[b + 1] = box.minY;
            boxes[b + 2] = box.minZ;
            boxes[b + 3] = box.maxX;
            boxes[b + 4] = box.maxY;
            boxes[b + 5] = box.maxZ;
            candidates[count++] = e;
        }
        // The ray is cut at the loaded reach: past it the result is a miss at that point, never a chunk load.
        Hitscan.Result result = Hitscan.trace(ox, oy, oz, ux, uy, uz, reach, blockDistance, boxes, count);
        Entity hit = result.kind() == Hitscan.Kind.ENTITY ? candidates[result.entityIndex()] : null;
        return new Trace(result, hit);
    }

    /** The distance up to which every chunk under the ray is already loaded, in whole steps of the chunk check. */
    private static double loadedReach(World world, double ox, double oz, double ux, double uz, double range) {
        double from = 0.0;
        while (from < range) {
            double to = Math.min(range, from + CHUNK_STEP);
            boolean loaded = ChunkCoverage.allLoaded(
                ox + ux * from,
                oz + uz * from,
                ox + ux * to,
                oz + uz * to,
                (cx, cz) -> world.getChunkProvider()
                    .chunkExists(cx, cz));
            if (!loaded) {
                return from;
            }
            from = to;
        }
        return range;
    }

    /** Distance to the first block that stops a ray, skipping passable ones, or {@link HitRules#NO_BLOCK}. */
    private static double blockDistance(World world, double ox, double oy, double oz, double ux, double uy, double uz,
        double reach) {
        if (reach <= 0) {
            return HitRules.NO_BLOCK;
        }
        Vec3 to = Vec3.createVectorHelper(ox + ux * reach, oy + uy * reach, oz + uz * reach);
        Vec3 from = Vec3.createVectorHelper(ox, oy, oz);
        for (int i = 0; i < MAX_PASSABLE_BLOCKS; i++) {
            // Flags: no liquids, skip blocks without a collision box, no "last uncollidable block".
            MovingObjectPosition mop = world.func_147447_a(from, to, false, true, false);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return HitRules.NO_BLOCK;
            }
            Block block = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            if (HitRules.blockStopsBullet(block.getBlocksMovement(world, mop.blockX, mop.blockY, mop.blockZ))) {
                Vec3 v = mop.hitVec;
                return Ballistics.distance(ox, oy, oz, v.xCoord, v.yCoord, v.zCoord);
            }
            // A passable block: carry on from just past it.
            from = Vec3.createVectorHelper(
                mop.hitVec.xCoord + ux * 0.01,
                mop.hitVec.yCoord + uy * 0.01,
                mop.hitVec.zCoord + uz * 0.01);
        }
        return HitRules.NO_BLOCK;
    }

    private static boolean canHit(Entity e, EntityLivingBase shooter) {
        if (!e.canBeCollidedWith() || !e.isEntityAlive()
            || e instanceof EntityItem
            || e instanceof EntityXPOrb
            || e instanceof GenericProjectile) {
            return false;
        }
        return shooter == null || e != shooter && e != shooter.ridingEntity;
    }
}
