package com.gregguns.armor.logic;

/**
 * Keeps GregGuns' armor speed modifier from pushing the field of view past what vanilla allows. Vanilla's FOV
 * multiplier is {@code (speedRatio + 1) / 2}, with {@code speedRatio = movementSpeed / walkSpeed}. The cap is the ratio
 * of Speed II plus sprint (1.4 * 1.3 = 1.82). Only the part our modifier adds is clamped: another mod's or potion's
 * speed is left alone. Plain Java.
 */
// Spec: docs/techguns-spec/05-armor/index.md ARM-05
public final class FovClamp {

    /** Speed ratio of vanilla Speed II while sprinting: (1 + 2 * 0.2) * (1 + 0.3). */
    public static final double MAX_RATIO = 1.4 * 1.3;

    private FovClamp() {}

    /**
     * @param totalRatio the player's movement speed over walk speed, including our modifier
     * @param ourRatio   our modifier's own factor (1 + amount); 1.0 when we add none
     * @return the factor to multiply the FOV by; 1.0 when nothing needs clamping
     */
    public static double fovFactor(double totalRatio, double ourRatio) {
        if (ourRatio <= 1.0 || totalRatio <= MAX_RATIO) {
            return 1.0;
        }
        double others = totalRatio / ourRatio;
        double allowed = Math.max(others, MAX_RATIO);
        return (allowed + 1.0) / (totalRatio + 1.0);
    }
}
