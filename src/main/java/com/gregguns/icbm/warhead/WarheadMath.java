package com.gregguns.icbm.warhead;

/**
 * The radius and damage curves of a filled warhead (D23). With {@code f = fill / cap}, the radius is
 * {@code R * f^(1/3)} (never below {@link #MIN_RADIUS}) and the center damage {@code D * f^(1/2)}. The engine's
 * explosive definitions are built from the same constants, so the tooltip and the blast never disagree. No Minecraft
 * types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The radius curve)
public final class WarheadMath {

    /** The smallest blast radius, blocks (ICBM's floor). */
    public static final double MIN_RADIUS = 3.0;
    /** Exponent of the radius over fill: blast energy spread through a volume. */
    public static final double RADIUS_EXPONENT = 1.0 / 3.0;
    /** Exponent of the center damage over fill. */
    public static final double DAMAGE_EXPONENT = 0.5;

    private WarheadMath() {}

    /**
     * @param fill the charges loaded; clamped to 1..cap
     * @param cap  the fill cap, at least 1
     * @return the fraction {@code fill / cap}
     */
    public static double fraction(int fill, int cap) {
        int c = Math.max(1, cap);
        return Math.max(1, Math.min(fill, c)) / (double) c;
    }

    /**
     * @param fullRadius the radius at a full casing, blocks
     * @param fill       the charges loaded
     * @param cap        the fill cap
     * @return the blast radius, blocks, before any fuel bonus
     */
    public static double radius(double fullRadius, int fill, int cap) {
        return Math.max(MIN_RADIUS, fullRadius * Math.pow(fraction(fill, cap), RADIUS_EXPONENT));
    }

    /**
     * @param fullDamage the center damage at a full casing, half hearts
     * @param fill       the charges loaded
     * @param cap        the fill cap
     * @return the center damage at this fill
     */
    public static double centerDamage(double fullDamage, int fill, int cap) {
        return fullDamage * Math.pow(fraction(fill, cap), DAMAGE_EXPONENT);
    }

    /**
     * @param fullRadius the radius at a full casing
     * @param cap        the fill cap
     * @return the radius at fill 1 divided by {@code 1^exponent}, which is the {@code baseRadius} an engine definition
     *         takes for {@code radius = baseRadius * fill^(1/3)}
     */
    public static double baseRadius(double fullRadius, int cap) {
        return fullRadius / Math.pow(Math.max(1, cap), RADIUS_EXPONENT);
    }
}
