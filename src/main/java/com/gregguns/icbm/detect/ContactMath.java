package com.gregguns.icbm.detect;

/**
 * The geometry of a radar contact: whether a point is in a radar's range and whether a missile's predicted impact
 * point falls inside a protected circle (D14). Flat (X and Z) distances only, because a missile's danger is where it
 * lands. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Incoming test)
public final class ContactMath {

    private ContactMath() {}

    /**
     * @param ax first X
     * @param az first Z
     * @param bx second X
     * @param bz second Z
     * @return the flat distance between two points
     */
    public static double flat(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * The incoming test (D14): the predicted impact point lies inside the protected circle. The edge counts as inside.
     * Unlike ICBM's distance-trend test (ICBM-05-04), a missile that passes by does not count.
     *
     * @param impactX the predicted impact X
     * @param impactZ the predicted impact Z
     * @param centerX the protected center X
     * @param centerZ the protected center Z
     * @param radius  the protected radius, blocks; a negative radius protects nothing
     * @return true when the missile is incoming for that circle
     */
    public static boolean incoming(double impactX, double impactZ, double centerX, double centerZ, double radius) {
        return radius >= 0.0 && flat(impactX, impactZ, centerX, centerZ) <= radius;
    }

    /**
     * A true circle in the flat plane, with no height limit. ICBM's chunk-aligned square reached 16 to 32
     * blocks past the stated range (ICBM-05-01); this does not.
     *
     * @param x       the contact X
     * @param z       the contact Z
     * @param centerX the radar X
     * @param centerZ the radar Z
     * @param range   the radar range, blocks
     * @return true when the contact is within range
     */
    public static boolean inRange(double x, double z, double centerX, double centerZ, double range) {
        return flat(x, z, centerX, centerZ) <= range;
    }
}
