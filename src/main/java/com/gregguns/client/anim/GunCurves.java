package com.gregguns.client.anim;

/**
 * The shared animation curves of the gun rendering framework. Each takes a progress {@code p} (0 to 1 over the
 * window) and returns a dimensionless shape that the per-gun transforms scale into degrees or blocks. Pure maths, no
 * Minecraft types.
 *
 * <p>
 * Spec: docs/techguns-spec/03-gun-visuals-audio/rendering-framework.md section 5 (names match the table there).
 */
public final class GunCurves {

    private GunCurves() {}

    /** Triangle wave: 0 at p=0, 1 at p=0.5, 0 at p=1. */
    public static double tri(double p) {
        return 1.0 - Math.abs(2.0 * p - 1.0);
    }

    /** "Fire dip": 0, then -0.5 at the midpoint, then 0. */
    public static double yF(double p) {
        return -tri(p) / 2.0;
    }

    /** "Reload dip": 0, then -1 at the midpoint, then 0. */
    public static double yR(double p) {
        return -tri(p);
    }

    /** Smooth half sine: 0, 1 at the midpoint, 0. */
    public static double sin(double p) {
        return Math.sin(Math.PI * p);
    }

    /** Sine clipped to a 0.66 plateau and rescaled so the plateau reads about 0.99. */
    public static double p66(double p) {
        return 1.5 * Math.min(0.66, sin(p));
    }

    /** Sine clipped to a 0.75 plateau, rescaled so the plateau reads about 1. */
    public static double p75(double p) {
        return 1.333 * Math.min(0.75, sin(p));
    }

    /** Sine clipped to a 0.5 plateau, rescaled so the plateau reads 1. */
    public static double p50(double p) {
        return 2.0 * Math.min(0.5, sin(p));
    }

    /**
     * "Kick": only in the first half of the window. With q = 2p, rises to full at q = 0.25 and eases to 0 at q = 1.
     * Zero for p >= 0.5.
     */
    public static double kick(double p) {
        if (p >= 0.5) {
            return 0.0;
        }
        double q = 2.0 * p;
        return q < 0.25 ? 4.0 * q : 1.25 * (1.0 - q);
    }

    /** The same kick stretched over the whole window. */
    public static double kickFull(double p) {
        return p < 0.25 ? 4.0 * p : 1.25 * (1.0 - p);
    }

    /** Default third-person recoil: a linear ramp over the first 30% of the window, then a snap back to 0. */
    public static double d30(double p) {
        return p < 0.3 ? p / 0.3 : 0.0;
    }

    /** "Bounce": 0 at p=0, peaks 1 at p=0.25, 0.5 near p=0.56, back to 0 at p=1. */
    public static double bounce(double p) {
        return 0.5 + 0.5 * Math.sin((Math.sqrt(p) + 0.75) * 2.0 * Math.PI);
    }

    /** One full sine wave (vibration). */
    public static double wave(double p) {
        return Math.sin(2.0 * Math.PI * p);
    }

    /** Scope recoil: 0 to 1 over the first 20% of the window, then back to 0 at p = 1. */
    public static double scopeRecoil(double p) {
        return p < 0.2 ? 5.0 * p : 1.0 - 1.25 * (p - 0.2);
    }
}
