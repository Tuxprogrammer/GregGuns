package com.gregguns.fx;

import java.awt.Color;
import java.util.List;

/**
 * Evaluates alpha and color key lists over a particle's normalized lifetime. Pure.
 *
 * <p>
 * Deviation from Techguns (3.5): the segment weights are not swapped, so a key's value is reached at the key's time
 * (Techguns shows the later key's value at the start of each segment). A time outside the keys clamps to the nearest
 * key instead of producing NaN, and the file's alpha is the opacity itself, not {@code 1 - alpha}. Color is
 * interpolated in HSB without hue wrap-around, as in Techguns.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.5); Deviation: weights, NaN, alpha sense
public final class ParticleCurve {

    private ParticleCurve() {}

    /**
     * @param keys     alpha keys (value in {@code a}), ascending by time; may be empty
     * @param progress normalized lifetime, clamped to 0..1
     * @return the opacity, 0 to 1; 1 when there are no keys
     */
    public static float alpha(List<CurveKey> keys, float progress) {
        if (keys.isEmpty()) {
            return 1F;
        }
        int[] seg = segment(keys, progress);
        CurveKey lo = keys.get(seg[0]);
        CurveKey hi = keys.get(seg[1]);
        float p = weight(keys, seg, progress);
        return clamp01(lo.a + (hi.a - lo.a) * p);
    }

    /**
     * @param keys     color keys (red, green, blue 0 to 255 in {@code a}, {@code b}, {@code c}), ascending by time;
     *                 may be empty
     * @param progress normalized lifetime, clamped to 0..1
     * @return packed 0xRRGGBB; white when there are no keys
     */
    public static int rgb(List<CurveKey> keys, float progress) {
        if (keys.isEmpty()) {
            return 0xFFFFFF;
        }
        int[] seg = segment(keys, progress);
        CurveKey lo = keys.get(seg[0]);
        CurveKey hi = keys.get(seg[1]);
        float p = weight(keys, seg, progress);
        float[] h0 = Color.RGBtoHSB(clamp255(lo.a), clamp255(lo.b), clamp255(lo.c), null);
        float[] h1 = Color.RGBtoHSB(clamp255(hi.a), clamp255(hi.b), clamp255(hi.c), null);
        return Color.HSBtoRGB(h0[0] + (h1[0] - h0[0]) * p, h0[1] + (h1[1] - h0[1]) * p, h0[2] + (h1[2] - h0[2]) * p)
            & 0xFFFFFF;
    }

    /** @return {low index, high index}; equal when the time is outside the keys */
    private static int[] segment(List<CurveKey> keys, float progress) {
        float t = clamp01(progress);
        int n = keys.size();
        if (t <= keys.get(0).time) {
            return new int[] { 0, 0 };
        }
        for (int i = 1; i < n; i++) {
            if (t <= keys.get(i).time) {
                return new int[] { i - 1, i };
            }
        }
        return new int[] { n - 1, n - 1 };
    }

    private static float weight(List<CurveKey> keys, int[] seg, float progress) {
        if (seg[0] == seg[1]) {
            return 0F;
        }
        CurveKey prev = keys.get(seg[0]);
        CurveKey next = keys.get(seg[1]);
        float span = next.time - prev.time;
        return span <= 0 ? 1F : (clamp01(progress) - prev.time) / span;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static int clamp255(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }
}
