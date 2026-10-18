package com.gregguns.projectile;

/**
 * Segment against axis-aligned box, on plain doubles so the per-tick entity sweep allocates nothing. Plain Java.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2.3 Per-tick update, step 4: box expanded by 0.3)
public final class RayBox {

    private RayBox() {}

    /**
     * Finds where a segment first enters a box (slab method).
     *
     * @param ox     segment start X
     * @param oy     segment start Y
     * @param oz     segment start Z
     * @param dx     segment vector X (end minus start)
     * @param dy     segment vector Y
     * @param dz     segment vector Z
     * @param minX   box minimum X
     * @param minY   box minimum Y
     * @param minZ   box minimum Z
     * @param maxX   box maximum X
     * @param maxY   box maximum Y
     * @param maxZ   box maximum Z
     * @param margin blocks added to every side of the box before testing
     * @return the fraction of the segment, 0 to 1, at which it enters the box (0 if it starts inside), or -1 on a miss
     */
    public static double hit(double ox, double oy, double oz, double dx, double dy, double dz, double minX, double minY,
        double minZ, double maxX, double maxY, double maxZ, double margin) {
        double enter = 0.0;
        double leave = 1.0;
        for (int axis = 0; axis < 3; axis++) {
            double o = axis == 0 ? ox : axis == 1 ? oy : oz;
            double d = axis == 0 ? dx : axis == 1 ? dy : dz;
            double lo = (axis == 0 ? minX : axis == 1 ? minY : minZ) - margin;
            double hi = (axis == 0 ? maxX : axis == 1 ? maxY : maxZ) + margin;
            if (Math.abs(d) < 1.0E-12) {
                if (o < lo || o > hi) {
                    return -1.0;
                }
            } else {
                double t1 = (lo - o) / d;
                double t2 = (hi - o) / d;
                enter = Math.max(enter, Math.min(t1, t2));
                leave = Math.min(leave, Math.max(t1, t2));
                if (enter > leave) {
                    return -1.0;
                }
            }
        }
        return enter;
    }
}
