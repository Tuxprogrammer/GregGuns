package com.gregguns.fx;

/**
 * One key of an alpha or color curve: a value (alpha, or red, green, blue as 0 to 255) at a normalized lifetime.
 * Immutable.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.5)
public final class CurveKey {

    final float time;
    final float a;
    final float b;
    final float c;

    CurveKey(float time, float a, float b, float c) {
        this.time = time;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /** @return the normalized lifetime, 0 to 1, of this key */
    public float time() {
        return time;
    }
}
