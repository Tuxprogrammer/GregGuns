package com.gregguns.explosion;

/**
 * The fragments of a Frag Grenade as pure logic: the fixed pattern of directions they fly in, and the damage one
 * fragment does at a distance. No Minecraft types, both sides.
 *
 * <p>
 * Design (the Techguns frag grenade was a plain blast with a different particle effect; GregGuns makes the Steel rounds
 * in its recipe real): {@link #COUNT} fragments leave the blast on a Fibonacci spiral over the sphere, so the spread is
 * even and needs no randomness. Each stops at the first entity along its line and does {@link #damage} to it. A big
 * target or a close one is hit by more of them, which is the whole point of a frag.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     double[] d = FragMath.direction(0, FragMath.COUNT); // unit vector of the first fragment
 *     double dmg = FragMath.damage(5.0); // 2.0 half hearts per fragment at 5 blocks
 * }
 * </pre>
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Frag Grenade); docs/gregguns-spec/02-guns/recipes.md (Steel
// rounds are the fragments)
public final class FragMath {

    /** Fragments per grenade. */
    public static final int COUNT = 64;
    /** Furthest a fragment flies, blocks. */
    public static final double RANGE = 10.0;
    /** Damage of a fragment at the blast, half hearts. */
    public static final double DAMAGE_MAX = 3.0;
    /** Damage of a fragment at the end of its range, half hearts. */
    public static final double DAMAGE_MIN = 1.0;
    /** Share of a fragment's damage that skips armor (a steel round). */
    public static final double PENETRATION = 0.2;

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private FragMath() {}

    /**
     * @param index which fragment, 0 to {@code count - 1}
     * @param count how many fragments there are
     * @return the unit direction {x, y, z} of that fragment
     */
    public static double[] direction(int index, int count) {
        if (count < 1 || index < 0 || index >= count) {
            throw new IllegalArgumentException("index " + index + " of " + count);
        }
        double y = 1.0 - 2.0 * (index + 0.5) / count;
        double ring = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        double angle = GOLDEN_ANGLE * index;
        return new double[] { Math.cos(angle) * ring, y, Math.sin(angle) * ring };
    }

    /**
     * @param distance blocks flown
     * @return the damage of one fragment, falling linearly from {@link #DAMAGE_MAX} to {@link #DAMAGE_MIN} at
     *         {@link #RANGE}; 0 beyond
     */
    public static double damage(double distance) {
        if (Double.isNaN(distance) || distance > RANGE) {
            return 0.0;
        }
        double t = Math.max(0.0, distance) / RANGE;
        return DAMAGE_MAX + (DAMAGE_MIN - DAMAGE_MAX) * t;
    }
}
