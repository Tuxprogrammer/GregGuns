package com.gregguns.icbm.control;

/** What the block knows about the pad and the missile right now; recomputed every few ticks on the server. */
final class LaunchSnapshot {

    final ControlStatus status;
    final boolean padFound;
    final int minRange;
    final int maxRange;
    final int distance;
    /** Expected blast radius at the target, tenths of a block; 0 when unknown. */
    final int radiusTenths;
    final int tier;
    /** Ticks left on a running countdown (D88), -1 when none. */
    final int countdown;

    LaunchSnapshot(ControlStatus status, boolean padFound, int minRange, int maxRange, int distance, int radiusTenths,
        int tier) {
        this(status, padFound, minRange, maxRange, distance, radiusTenths, tier, -1);
    }

    LaunchSnapshot(ControlStatus status, boolean padFound, int minRange, int maxRange, int distance, int radiusTenths,
        int tier, int countdown) {
        this.countdown = countdown;
        this.status = status;
        this.padFound = padFound;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.distance = distance;
        this.radiusTenths = radiusTenths;
        this.tier = tier;
    }

    /**
     * @param cd the countdown ticks left, -1 once the engines have ignited
     * @return the snapshot of a launch under way: no gate verdict, but the locked target's real numbers
     */
    static LaunchSnapshot inProgress(int cd, int min, int max, double dist, int radiusTenths, int tier) {
        return new LaunchSnapshot(
            cd >= 0 ? ControlStatus.COUNTDOWN : ControlStatus.IGNITED,
            true,
            min,
            max,
            (int) Math.round(dist),
            radiusTenths,
            tier,
            cd);
    }
}
