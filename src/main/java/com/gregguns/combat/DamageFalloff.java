package com.gregguns.combat;

/**
 * Damage drop-off over distance traveled, for projectiles. Full damage up to the start distance, the minimum damage
 * beyond the end distance, and a straight line between. Plain Java, no Minecraft types.
 *
 * <p>
 * The caller measures the distance from the projectile's first-tick position, in blocks.
 */
// Spec: docs/techguns-spec/02-guns/gun-catalog.md (drop-off formula)
public final class DamageFalloff {

    private DamageFalloff() {}

    /**
     * Computes the damage at a distance.
     *
     * @param base     damage at or before {@code start}
     * @param min      damage beyond {@code end}
     * @param start    distance in blocks up to which damage is {@code base} (inclusive)
     * @param end      distance in blocks beyond which damage is {@code min}; at least {@code start}. Equal to
     *                 {@code start} means a hard step
     * @param distance distance traveled in blocks; negative values count as zero
     * @return the damage, between {@code min} and {@code base}; a {@code min} above {@code base} counts as
     *         {@code base}
     * @throws IllegalArgumentException if any value is NaN or negative (except {@code distance}), or
     *                                  {@code end < start}
     */
    public static double damageAt(double base, double min, double start, double end, double distance) {
        if (Double.isNaN(base) || Double.isNaN(min)
            || Double.isNaN(start)
            || Double.isNaN(end)
            || Double.isNaN(distance)
            || base < 0
            || min < 0
            || start < 0
            || end < start) {
            throw new IllegalArgumentException(
                "Invalid drop-off: base=" + base + " min=" + min + " start=" + start + " end=" + end);
        }
        double low = Math.min(min, base);
        if (distance <= start) {
            return base;
        }
        if (distance > end) {
            return low;
        }
        double t = (distance - start) / (end - start);
        return base + (low - base) * t;
    }
}
