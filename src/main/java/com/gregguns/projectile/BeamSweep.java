package com.gregguns.projectile;

import java.util.Arrays;

import com.gregguns.combat.DamageFalloff;

/**
 * One tick of a continuous beam, as pure arithmetic: which entities the ray crosses before the first solid block,
 * how far each is along it, and what each takes. Plain Java, no Minecraft types; {@link BeamTracer} gathers the
 * world's numbers and feeds them in. Unlike {@link Hitscan}, the beam is not stopped by an entity: it burns everything
 * on its line, each entity once per call.
 *
 * <p>
 * Damage falls with the entity's distance along the beam through {@link DamageFalloff}: the full figure up to the
 * start distance, the minimum beyond the end distance.
 */
// Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (Nuclear Death Ray: 6 damage falling to 1 between 20 and 40
// blocks); docs/techguns-spec/04-projectiles-fx/fx-catalog.md (MovingBeam)
// Bug PFX-03: a missed beam ends at origin + direction * range, an absolute point.
// Bug PFX-23: an entity behind a nearer block is not hit (HitRules.entityBeforeBlock).
public final class BeamSweep {

    private BeamSweep() {}

    /** The outcome of one tick. Immutable. */
    public static final class Result {

        private final double length;
        private final double endX;
        private final double endY;
        private final double endZ;
        private final boolean hitBlock;
        private final int[] entityIndex;
        private final double[] distance;
        private final double[] damage;

        Result(double length, double endX, double endY, double endZ, boolean hitBlock, int[] entityIndex,
            double[] distance, double[] damage) {
            this.length = length;
            this.endX = endX;
            this.endY = endY;
            this.endZ = endZ;
            this.hitBlock = hitBlock;
            this.entityIndex = entityIndex;
            this.distance = distance;
            this.damage = damage;
        }

        /** @return the beam's length: the distance to the first block, or the range on a miss */
        public double length() {
            return length;
        }

        /** @return end point X */
        public double endX() {
            return endX;
        }

        /** @return end point Y */
        public double endY() {
            return endY;
        }

        /** @return end point Z */
        public double endZ() {
            return endZ;
        }

        /** @return true if the beam ended on a solid block, false if it ran to full range */
        public boolean hitBlock() {
            return hitBlock;
        }

        /** @return how many entities the beam crosses */
        public int hitCount() {
            return entityIndex.length;
        }

        /**
         * @param n which hit, nearest first, 0 to {@link #hitCount()} - 1
         * @return the index of the entity in the box array
         */
        public int entityIndex(int n) {
            return entityIndex[n];
        }

        /**
         * @param n which hit, nearest first
         * @return blocks from the origin to where the beam enters that entity
         */
        public double distance(int n) {
            return distance[n];
        }

        /**
         * @param n which hit, nearest first
         * @return the damage that entity takes this tick, after fall-off, before armor
         */
        public double damage(int n) {
            return damage[n];
        }
    }

    /**
     * Resolves one beam tick.
     *
     * @param ox            origin X
     * @param oy            origin Y
     * @param oz            origin Z
     * @param dx            direction X; any length above zero, normalized here
     * @param dy            direction Y
     * @param dz            direction Z
     * @param range         maximum length, blocks
     * @param blockDistance distance along the ray to the first solid block, or {@link HitRules#NO_BLOCK}
     * @param boxes         entity boxes, {@link Hitscan#BOX_STRIDE} doubles each: min XYZ then max XYZ
     * @param boxCount      how many boxes are in use
     * @param base          damage at or before {@code dropStart}
     * @param min           damage beyond {@code dropEnd}
     * @param dropStart     blocks up to which damage is {@code base}
     * @param dropEnd       blocks beyond which damage is {@code min}
     * @return the result; a zero direction or non-positive range is a miss at the origin with no hits
     */
    public static Result resolve(double ox, double oy, double oz, double dx, double dy, double dz, double range,
        double blockDistance, double[] boxes, int boxCount, double base, double min, double dropStart, double dropEnd) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-12 || range <= 0) {
            return new Result(0, ox, oy, oz, false, new int[0], new double[0], new double[0]);
        }
        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;
        boolean blocked = blockDistance <= range;
        double length = blocked ? blockDistance : range;
        int[] index = new int[boxCount];
        double[] at = new double[boxCount];
        int n = 0;
        for (int i = 0; i < boxCount; i++) {
            int b = i * Hitscan.BOX_STRIDE;
            double t = RayBox.hit(
                ox,
                oy,
                oz,
                ux * range,
                uy * range,
                uz * range,
                boxes[b],
                boxes[b + 1],
                boxes[b + 2],
                boxes[b + 3],
                boxes[b + 4],
                boxes[b + 5],
                Hitscan.ENTITY_MARGIN);
            if (t < 0) {
                continue;
            }
            double distance = t * range;
            if (distance > length || !HitRules.entityBeforeBlock(distance, blockDistance)) {
                continue;
            }
            // Insertion sort by distance: the list is short.
            int slot = n++;
            while (slot > 0 && at[slot - 1] > distance) {
                at[slot] = at[slot - 1];
                index[slot] = index[slot - 1];
                slot--;
            }
            at[slot] = distance;
            index[slot] = i;
        }
        double[] hitDamage = new double[n];
        for (int i = 0; i < n; i++) {
            hitDamage[i] = DamageFalloff.damageAt(base, min, dropStart, dropEnd, at[i]);
        }
        return new Result(
            length,
            ox + ux * length,
            oy + uy * length,
            oz + uz * length,
            blocked,
            Arrays.copyOf(index, n),
            Arrays.copyOf(at, n),
            hitDamage);
    }
}
