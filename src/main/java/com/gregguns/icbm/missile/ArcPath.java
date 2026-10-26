package com.gregguns.icbm.missile;

/**
 * The ballistic path of a missile, as a closed-form function of the flight tick. It is built from the baseline's
 * description (constant horizontal speed over a parabola whose apex is well above the chord), not ported, and it fixes
 * three of its bugs on purpose: the target height is used (ICBM-03-18), a flat distance under one block cannot divide
 * by
 * zero or dive (ICBM-03-17), and a missing target is not the world origin (ICBM-03-19: the caller must pass one).
 * Because the position is a pure function of the tick, a reloaded missile resumes exactly where it was and the client
 * can extrapolate the same line. Past the arrival tick the curve keeps descending, so a missile that missed its target
 * height still hits the ground. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/flight.md (The arc)
public final class ArcPath {

    /** Fixed part of the apex height above the chord, blocks. */
    public static final double APEX_BASE = 20.0;
    /** Apex height per block of flat distance. */
    public static final double APEX_PER_BLOCK = 0.3;
    /** Highest apex above the chord, blocks. */
    public static final double APEX_MAX = 300.0;

    private final double sx;
    private final double sy;
    private final double sz;
    private final double dx;
    private final double dy;
    private final double dz;
    private final double apex;
    private final int ticks;

    /**
     * @param sx    start X
     * @param sy    start Y
     * @param sz    start Z
     * @param tx    target X
     * @param ty    target Y
     * @param tz    target Z
     * @param speed horizontal blocks per tick, above 0
     */
    public ArcPath(double sx, double sy, double sz, double tx, double ty, double tz, double speed) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.dx = tx - sx;
        this.dy = ty - sy;
        this.dz = tz - sz;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double s = speed > 0.0 ? speed : 1.0;
        this.ticks = Math.max(1, (int) Math.ceil(flat / s));
        this.apex = Math.min(APEX_MAX, APEX_BASE + APEX_PER_BLOCK * flat);
    }

    /** @return the tick at which the path reaches the target's height and column */
    public int ticks() {
        return ticks;
    }

    /** @return the apex above the chord, blocks */
    public double apex() {
        return apex;
    }

    /**
     * @param n ticks since the arc began; may exceed {@link #ticks()}
     * @return x, y, z at that tick
     */
    public double[] position(int n) {
        double f = (double) n / ticks;
        return new double[] { sx + dx * f, sy + dy * f + 4.0 * apex * f * (1.0 - f), sz + dz * f };
    }

    /**
     * @param n ticks since the arc began
     * @return the displacement from tick n to n+1 (blocks per tick)
     */
    public double[] velocity(int n) {
        double[] a = position(n);
        double[] b = position(n + 1);
        return new double[] { b[0] - a[0], b[1] - a[1], b[2] - a[2] };
    }
}
