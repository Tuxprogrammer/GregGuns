package com.gregguns.icbm.control;

/**
 * A launch target: a block column, or nothing. An unset target is its own state, never (0, 0), so a block that was
 * never aimed cannot fire at the world origin (ICBM-04-06). Immutable. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Targeting)
public final class LaunchTarget {

    /** Largest coordinate a target may hold; the world border of 1.7.10 is 30,000,000. */
    public static final int LIMIT = 30_000_000;

    /** No target set. */
    public static final LaunchTarget NONE = new LaunchTarget(false, 0, 0);

    private final boolean set;
    private final int x;
    private final int z;

    private LaunchTarget(boolean set, int x, int z) {
        this.set = set;
        this.x = x;
        this.z = z;
    }

    /**
     * @param x block X
     * @param z block Z
     * @return the target, or {@link #NONE} when a coordinate is outside plus or minus {@link #LIMIT}
     */
    public static LaunchTarget of(long x, long z) {
        if (Math.abs(x) > LIMIT || Math.abs(z) > LIMIT) {
            return NONE;
        }
        return new LaunchTarget(true, (int) x, (int) z);
    }

    /** The value {@link #parseCoordinate} answers for text that is not a legal coordinate. */
    public static final int INVALID = Integer.MIN_VALUE;

    /**
     * @param text a typed coordinate
     * @return the value, or {@link #INVALID} when the text is empty, not a plain integer, or beyond {@link #LIMIT}
     */
    public static int parseCoordinate(String text) {
        if (text == null) {
            return INVALID;
        }
        String t = text.trim();
        if (t.isEmpty() || t.length() > 9) {
            return INVALID;
        }
        try {
            long v = Long.parseLong(t);
            return Math.abs(v) > LIMIT ? INVALID : (int) v;
        } catch (NumberFormatException e) {
            return INVALID;
        }
    }

    /** @return true when a target is set */
    public boolean isSet() {
        return set;
    }

    /** @return block X; meaningless when unset */
    public int x() {
        return x;
    }

    /** @return block Z; meaningless when unset */
    public int z() {
        return z;
    }

    /** @return the aim point of the block's center, X */
    public double aimX() {
        return x + 0.5;
    }

    /** @return the aim point of the block's center, Z */
    public double aimZ() {
        return z + 0.5;
    }

    /**
     * @param fromX the pad's center X
     * @param fromZ the pad's center Z
     * @return the horizontal distance from the pad's center to the aim point, in blocks (a circle, not a square:
     *         ICBM-04-05); 0 for an unset target
     */
    public double distanceFrom(double fromX, double fromZ) {
        if (!set) {
            return 0.0;
        }
        double dx = aimX() - fromX;
        double dz = aimZ() - fromZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LaunchTarget)) {
            return false;
        }
        LaunchTarget t = (LaunchTarget) o;
        return set == t.set && x == t.x && z == t.z;
    }

    @Override
    public int hashCode() {
        return set ? 31 * x + z : -1;
    }

    @Override
    public String toString() {
        return set ? x + ", " + z : "no target";
    }
}
