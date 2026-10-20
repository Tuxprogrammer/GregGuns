package com.gregguns.projectile;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * Reads the world for one {@link BeamSweep}: the nearest solid block, every entity the beam may burn, and no chunk that
 * is not already loaded. Server only. Shares the block and chunk rules of {@link HitscanTracer}.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (MovingBeam, hitscan each tick)
public final class BeamTracer {

    private static final double SWEEP_MARGIN = 1.0;

    private BeamTracer() {}

    /** A sweep result and the entities its hits index into. */
    public static final class Trace {

        private final BeamSweep.Result result;
        private final Entity[] entities;

        Trace(BeamSweep.Result result, Entity[] entities) {
            this.result = result;
            this.entities = entities;
        }

        /** @return the pure result */
        public BeamSweep.Result result() {
            return result;
        }

        /**
         * @param n which hit of the result, nearest first
         * @return the entity of that hit
         */
        public Entity entity(int n) {
            return entities[result.entityIndex(n)];
        }
    }

    /**
     * Traces the beam through the world.
     *
     * @param world     the world
     * @param shooter   never hit, nor the entity it rides; may be {@code null}
     * @param ox        origin X
     * @param oy        origin Y
     * @param oz        origin Z
     * @param dx        direction X, any length above zero
     * @param dy        direction Y
     * @param dz        direction Z
     * @param range     maximum length, blocks
     * @param base      damage at or before {@code dropStart}
     * @param min       damage beyond {@code dropEnd}
     * @param dropStart blocks up to which damage is full
     * @param dropEnd   blocks beyond which damage is the minimum
     * @return the trace
     */
    public static Trace trace(World world, EntityLivingBase shooter, double ox, double oy, double oz, double dx,
        double dy, double dz, double range, double base, double min, double dropStart, double dropEnd) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-12 || range <= 0) {
            return new Trace(
                BeamSweep.resolve(
                    ox,
                    oy,
                    oz,
                    dx,
                    dy,
                    dz,
                    range,
                    HitRules.NO_BLOCK,
                    new double[0],
                    0,
                    base,
                    min,
                    dropStart,
                    dropEnd),
                new Entity[0]);
        }
        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;
        double reach = HitscanTracer.loadedReach(world, ox, oz, ux, uz, range);
        double blockDistance = HitscanTracer.blockDistance(world, ox, oy, oz, ux, uy, uz, reach);
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
            if (!HitscanTracer.canHit(e, shooter) || e.boundingBox == null) {
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
        // Cut at the loaded reach: past it the beam is a miss at that point, never a chunk load.
        BeamSweep.Result result = BeamSweep
            .resolve(ox, oy, oz, ux, uy, uz, reach, blockDistance, boxes, count, base, min, dropStart, dropEnd);
        return new Trace(result, candidates);
    }
}
