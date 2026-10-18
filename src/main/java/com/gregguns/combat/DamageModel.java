package com.gregguns.combat;

import java.util.function.DoubleUnaryOperator;

/**
 * The GregGuns damage formulas. Plain Java with no Minecraft types, so every formula is unit-tested.
 *
 * <p>
 * A hit has a raw damage {@code d} and a penetration {@code p} (0..1). The share {@code d * p} passes through armour
 * untouched; the rest, {@code d * (1 - p)}, goes through the target's armour. Penetration is computed once per hit
 * (GUN-39/ARM-02) and applies to every living target (GUN-34).
 */
// Spec: docs/techguns-spec/02-guns/damage-system.md sections 5.3 and 5.4; docs/design/0004-damage-model.md
public final class DamageModel {

    /** Highest armour value that counts: 24 points leave 4% of the damage (fixes GUN-40, where more inverted it). */
    public static final double MAX_ARMOUR = 24.0;

    private static final double ARMOUR_DIVISOR = 25.0;

    private DamageModel() {}

    /**
     * Computes the penetration that applies to a hit. Called once per hit with the whole worn set's resistance, never
     * once per armour piece.
     *
     * @param base       the source's penetration, 0..1
     * @param scale      a multiplier for the hit (for example a PvP penetration setting); 1.0 for none
     * @param resistance the target's total penetration resistance against this hit, subtracted after scaling
     * @return the effective penetration, clamped to 0..1
     */
    public static double effectivePenetration(double base, double scale, double resistance) {
        double p = clamp01(base) * Math.max(0.0, scale) - Math.max(0.0, resistance);
        return clamp01(p);
    }

    /**
     * The share of damage that gets through vanilla-style armour points: {@code (25 - armour) / 25}, with the armour
     * clamped to 0..{@link #MAX_ARMOUR}.
     *
     * @param armourPoints total armour points (vanilla 0..20)
     * @return the factor, 0.04..1.0
     */
    public static double vanillaArmourFactor(double armourPoints) {
        double a = Double.isNaN(armourPoints) ? 0.0 : Math.max(0.0, Math.min(MAX_ARMOUR, armourPoints));
        return (ARMOUR_DIVISOR - a) / ARMOUR_DIVISOR;
    }

    /**
     * The fraction of damage one armour piece absorbs, as Forge's {@code ArmorProperties.AbsorbRatio} wants it:
     * {@code points / 25}, clamped to 0..1. GregGuns armour (M3) returns this from {@code getProperties}.
     *
     * @param pieceArmourPoints the piece's armour value against the hit's type
     * @return the absorb ratio
     */
    public static double pieceAbsorbRatio(double pieceArmourPoints) {
        return clamp01(pieceArmourPoints / ARMOUR_DIVISOR);
    }

    /**
     * Resolves a hit to the damage that reaches health (before potions and absorption).
     *
     * @param type   the damage type; {@link DamageType#UNRESISTABLE} skips armour and returns the damage
     * @param damage raw damage before armour; zero, negative or NaN yields zero
     * @param pen    effective penetration 0..1, from {@link #effectivePenetration}
     * @param armour applies the target's armour to the non-penetrating share: receives that share, returns what
     *               survives. It runs at most once, and not at all when the share is zero
     * @return the final damage
     */
    public static double resolve(DamageType type, double damage, double pen, DoubleUnaryOperator armour) {
        if (!(damage > 0.0)) {
            return 0.0;
        }
        if (type.bypassesArmour()) {
            return damage;
        }
        double p = clamp01(pen);
        double share = damage * (1.0 - p);
        double through = damage * p;
        if (share <= 0.0) {
            return through;
        }
        double armoured = armour.applyAsDouble(share);
        return through + Math.max(0.0, Math.min(share, armoured));
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }
}
