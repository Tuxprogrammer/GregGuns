package com.gregguns.icbm.missile;

/**
 * Launch rule checks as pure functions: range band, in-flight cap, finite target. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/flight.md (Launch checks), DECISIONS D11, D20
public final class MissileLimits {

    private MissileLimits() {}

    /**
     * @param distance flat distance to the target
     * @param minRange the tier's minimum, blocks
     * @param maxRange the tier's maximum, blocks
     * @return {@code OK}, {@code TOO_CLOSE}, {@code OUT_OF_RANGE} or {@code BAD_TARGET}
     */
    public static LaunchResult range(double distance, double minRange, double maxRange) {
        if (Double.isNaN(distance) || Double.isInfinite(distance)) {
            return LaunchResult.BAD_TARGET;
        }
        if (distance < minRange) {
            return LaunchResult.TOO_CLOSE;
        }
        if (distance > maxRange) {
            return LaunchResult.OUT_OF_RANGE;
        }
        return LaunchResult.OK;
    }

    /**
     * @param flying the missiles in flight now
     * @param cap    the configured cap (0 or less means none may fly)
     * @return true when one more may launch
     */
    public static boolean underCap(int flying, int cap) {
        return flying < cap;
    }
}
