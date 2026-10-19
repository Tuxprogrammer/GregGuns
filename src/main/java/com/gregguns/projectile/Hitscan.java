package com.gregguns.projectile;

/**
 * The instant ray of the Laser Rifle and the later beams: given where the nearest solid block is and the boxes of the
 * candidate entities, finds what the ray ends on. Plain Java, no Minecraft types; {@link HitscanTracer} gathers the
 * world's numbers and feeds them in.
 *
 * <p>
 * The nearer of block or entity wins, and a tie goes to the block. A ray that hits nothing ends at
 * {@code origin + direction * range}, an absolute point.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.3 LaserProjectile, 3.15 LaserBeam2);
// docs/gregguns-spec/04-projectiles-fx/index.md
// Bug PFX-23: the Techguns hitscan helper let an entity beat a nearer block; here the nearer one wins (HitRules).
// Bug PFX-03: Techguns set a missed beam's end to the direction vector itself, near world origin; here the miss end is
// a point on the ray at full range.
public final class Hitscan {

    /** Values per entity box in the flat array: minX, minY, minZ, maxX, maxY, maxZ. */
    public static final int BOX_STRIDE = 6;
    /** Blocks added to every side of an entity box, as for a bullet. */
    public static final double ENTITY_MARGIN = 0.3;

    private Hitscan() {}

    /** What the ray ended on. */
    public enum Kind {
        /** Nothing within range. */
        MISS,
        /** A solid block. */
        BLOCK,
        /** An entity; see {@link Result#entityIndex()}. */
        ENTITY
    }

    /** The outcome of one trace. Immutable. */
    public static final class Result {

        private final Kind kind;
        private final int entityIndex;
        private final double distance;
        private final double endX;
        private final double endY;
        private final double endZ;

        Result(Kind kind, int entityIndex, double distance, double endX, double endY, double endZ) {
            this.kind = kind;
            this.entityIndex = entityIndex;
            this.distance = distance;
            this.endX = endX;
            this.endY = endY;
            this.endZ = endZ;
        }

        /** @return what the ray ended on */
        public Kind kind() {
            return kind;
        }

        /** @return the index of the entity hit in the box array, or -1 */
        public int entityIndex() {
            return entityIndex;
        }

        /** @return blocks from the origin to the end of the ray (the range on a miss) */
        public double distance() {
            return distance;
        }

        /** @return end point X: the hit point, or the point at full range on a miss */
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
    }

    /**
     * Traces one ray.
     *
     * @param ox            origin X
     * @param oy            origin Y
     * @param oz            origin Z
     * @param dx            direction X; any length above zero, it is normalized here
     * @param dy            direction Y
     * @param dz            direction Z
     * @param range         maximum length in blocks
     * @param blockDistance distance along the ray to the first solid block, or {@link HitRules#NO_BLOCK}; a block
     *                      beyond the range is ignored
     * @param boxes         entity boxes, {@link #BOX_STRIDE} doubles each: min XYZ then max XYZ
     * @param boxCount      how many boxes of the array are in use
     * @return the result; a zero direction or non-positive range is a miss at the origin
     */
    public static Result trace(double ox, double oy, double oz, double dx, double dy, double dz, double range,
        double blockDistance, double[] boxes, int boxCount) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-12 || range <= 0) {
            return new Result(Kind.MISS, -1, 0, ox, oy, oz);
        }
        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;
        int best = -1;
        double bestDistance = blockDistance <= range ? blockDistance : range;
        for (int i = 0; i < boxCount; i++) {
            int b = i * BOX_STRIDE;
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
                ENTITY_MARGIN);
            if (t < 0) {
                continue;
            }
            double distance = t * range;
            // The nearer wins; a tie with the block goes to the block (HitRules.entityBeforeBlock).
            if (distance < bestDistance && HitRules.entityBeforeBlock(distance, blockDistance)) {
                best = i;
                bestDistance = distance;
            }
        }
        if (best >= 0) {
            return new Result(
                Kind.ENTITY,
                best,
                bestDistance,
                ox + ux * bestDistance,
                oy + uy * bestDistance,
                oz + uz * bestDistance);
        }
        if (blockDistance <= range) {
            return new Result(
                Kind.BLOCK,
                -1,
                blockDistance,
                ox + ux * blockDistance,
                oy + uy * blockDistance,
                oz + uz * blockDistance);
        }
        return new Result(Kind.MISS, -1, range, ox + ux * range, oy + uy * range, oz + uz * range);
    }
}
