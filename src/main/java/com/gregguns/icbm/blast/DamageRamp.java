package com.gregguns.icbm.blast;

import com.gregguns.explosion.BlastFalloff;

/**
 * The damage a conventional ICBM blast deals at a distance (D34): the full center damage inside a small core, falling
 * linearly to zero at the blast radius. Pure math over {@link BlastFalloff}. No Minecraft types, both sides.
 *
 * <pre>
 * {@code
 * DamageRamp.at(1200.0, 20.0, 0.0);  // 1200.0 at the center
 * DamageRamp.at(1200.0, 20.0, 20.0); // 0.0 at the edge
 * }
 * </pre>
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (damage); DECISIONS.md D34
public final class DamageRamp {

    /** Share of the radius inside which damage is still the full center damage. */
    public static final double CORE_SHARE = 0.1;

    private DamageRamp() {}

    /**
     * @param centerDamage the damage at the center
     * @param radius       the blast radius, blocks
     * @param distance     distance from the center, blocks
     * @return the damage there; zero at and beyond the radius, and for a NaN distance
     */
    public static double at(double centerDamage, double radius, double distance) {
        if (!(distance < radius)) {
            return 0.0;
        }
        return BlastFalloff.damage(centerDamage, radius * CORE_SHARE, radius, distance);
    }
}
