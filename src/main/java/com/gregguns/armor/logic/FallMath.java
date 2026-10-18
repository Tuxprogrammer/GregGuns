package com.gregguns.armor.logic;

/**
 * Fall-distance adjustment from boots. Plain Java, both sides.
 */
// Spec: docs/techguns-spec/05-armor/set-bonuses.md section 3.2 (LivingFallEvent)
public final class FallMath {

    private FallMath() {}

    /**
     * Subtracts the free height, floors at zero, then scales by one minus the reduction.
     *
     * @param distance   the fall distance in blocks
     * @param reduction  the {@link ArmorBonus#FALL_REDUCTION} total; clamped to 0..1
     * @param freeBlocks the {@link ArmorBonus#FREE_FALL_HEIGHT} total
     * @return the distance vanilla should use for damage
     */
    public static double adjust(double distance, double reduction, double freeBlocks) {
        double d = Math.max(0.0, distance - Math.max(0.0, freeBlocks));
        double r = Math.max(0.0, Math.min(1.0, reduction));
        return d * (1.0 - r);
    }
}
