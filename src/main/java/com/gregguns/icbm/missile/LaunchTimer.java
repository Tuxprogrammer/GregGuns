package com.gregguns.icbm.missile;

/**
 * The timing math of a launch sequence (D88): a missile in {@link FlightPhase#WARMUP} first waits out a lead (the
 * silo doors opening; 0 on a pad), then counts down, then ignites. Plain integers, no Minecraft types, both sides; the
 * entity, the Launch Control and the smoke use these so they agree by construction.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md (The sequence)
public final class LaunchTimer {

    /** The engines show flame and the smoke thickens during this many ticks before ignition. */
    public static final int FLAME_TICKS = 20;

    private LaunchTimer() {}

    /**
     * @param tick  ticks into the warm-up
     * @param total lead plus countdown
     * @return ticks until ignition, never below 0
     */
    public static int untilIgnition(int tick, int total) {
        return Math.max(0, total - Math.max(0, tick));
    }

    /**
     * @param tick  ticks into the warm-up
     * @param lead  ticks spent waiting for the doors
     * @param total lead plus countdown
     * @return ticks left on the visible countdown: the full countdown while the doors are still opening
     */
    public static int countdownLeft(int tick, int lead, int total) {
        return Math.min(untilIgnition(tick, total), Math.max(0, total - lead));
    }

    /**
     * @param tick ticks into the warm-up
     * @param lead ticks spent waiting for the doors
     * @return true while the doors are still opening and the countdown has not started
     */
    public static boolean waitingForDoors(int tick, int lead) {
        return tick < lead;
    }

    /**
     * @param tick  ticks into the warm-up
     * @param total lead plus countdown
     * @return true while the launch can still be aborted: before the countdown ends (D88)
     */
    public static boolean abortable(int tick, int total) {
        return tick < total;
    }

    /**
     * @param tick  ticks into the warm-up
     * @param lead  ticks spent waiting for the doors
     * @param total lead plus countdown
     * @return 0 when the smoke has not begun (doors opening), rising to 1 at ignition
     */
    public static double smokeIntensity(int tick, int lead, int total) {
        int count = total - lead;
        if (count <= 0 || tick < lead) {
            return 0.0;
        }
        return Math.min(1.0, (tick - lead + 1) / (double) count);
    }

    /**
     * @param intensity {@link #smokeIntensity}
     * @return smoke puffs to spawn this tick: 1 early, growing to 6 in the last seconds
     */
    public static int puffsPerTick(double intensity) {
        if (intensity <= 0.0) {
            return 0;
        }
        return 1 + (int) Math.floor(intensity * intensity * 5.0);
    }

    /**
     * @param intensity {@link #smokeIntensity}
     * @return how far a puff may drift along the floor, blocks per tick: 0.05 early, 0.2 at ignition
     */
    public static double spreadSpeed(double intensity) {
        return 0.05 + 0.15 * Math.max(0.0, Math.min(1.0, intensity));
    }

    /**
     * @param ticks a countdown in ticks
     * @return whole seconds, rounded up, for the display
     */
    public static int seconds(int ticks) {
        return (Math.max(0, ticks) + 19) / 20;
    }
}
