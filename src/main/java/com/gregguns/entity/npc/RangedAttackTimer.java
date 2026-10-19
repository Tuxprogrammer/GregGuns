package com.gregguns.entity.npc;

/**
 * The timing of an NPC's gun attack: when to shoot, when to hold position. A burst-capable cousin of vanilla's bow
 * AI, with no Minecraft types so the glue task ({@code EntityAIGunAttack}) only feeds it distances and sight and
 * acts on the answers. Server only, one instance per NPC.
 *
 * <p>
 * Each tick the glue calls {@link #tick}. When it returns true, fire one shot with {@link #lastFactor()} as the
 * distance factor. After a shot the timer is the profile's burst delay while the burst has shots left, else it is
 * {@code floor(f * (attackTime - variance) + variance)}: about {@code attackTime / 3} ticks point-blank and
 * {@code attackTime} at the edge of range.
 *
 * <p>
 * Deviations: Techguns keeps running on a stale cached target after the target is cleared (NPC-31). Here
 * {@link #shouldContinue} depends on having a target only (a leftover path does not count) and {@link #reset}
 * drops all per-target state. The timer's distance factor is clamped to at most 1, so a target far outside range
 * does not stretch the wait beyond {@code attackTime}. A partially fired burst still carries over if sight breaks,
 * as in Techguns.
 *
 * <p>
 * Spec: docs/techguns-spec/10-entities-npcs/ai.md section 3.
 */
public final class RangedAttackTimer {

    /** Consecutive ticks of sight needed before the NPC stops to shoot. */
    public static final int SIGHT_TICKS_TO_HOLD = 20;

    private static final double MIN_FACTOR = 0.1;

    private final NpcGunProfile profile;
    private final int maxBurst;
    private int burstLeft;
    private int timer = -1;
    private int ticksSeen;
    private double lastFactor = 1.0;

    public RangedAttackTimer(NpcGunProfile profile) {
        this.profile = profile;
        this.maxBurst = Math.max(profile.burstCount(), 0);
        this.burstLeft = maxBurst;
    }

    /** True if the NPC should start the task: it has a target. */
    public static boolean shouldStart(boolean hasTarget) {
        return hasTarget;
    }

    /** True while the task should keep running: only if there is a target. A stale path does not count (NPC-31). */
    public static boolean shouldContinue(boolean hasTarget) {
        return hasTarget;
    }

    /**
     * Advances one tick.
     *
     * @param distance distance to the target's feet in blocks
     * @param seen     whether the NPC can see the target
     * @return true if the NPC fires one shot this tick, with {@link #lastFactor()} as the distance factor
     */
    public boolean tick(double distance, boolean seen) {
        ticksSeen = seen ? ticksSeen + 1 : 0;
        double range = profile.range();
        double raw = Math.max(0.0, distance / range);
        timer--;
        if (timer == 0) {
            if (distance > range || !seen) {
                return false;
            }
            lastFactor = Math.max(MIN_FACTOR, Math.min(1.0, raw));
            if (maxBurst > 0) {
                burstLeft--;
                if (burstLeft > 0) {
                    timer = profile.burstDelay();
                    return true;
                }
                burstLeft = maxBurst;
            }
            timer = betweenBursts(raw);
            return true;
        }
        if (timer < 0) {
            timer = betweenBursts(raw);
        }
        return false;
    }

    /**
     * True if the NPC should stand still and shoot: the target is in range and has been seen for
     * {@link #SIGHT_TICKS_TO_HOLD} consecutive ticks. Call after {@link #tick}. Otherwise the NPC paths toward the
     * target.
     */
    public boolean holdPosition(double distance) {
        return distance <= profile.range() && ticksSeen >= SIGHT_TICKS_TO_HOLD;
    }

    /** The distance factor of the shot {@link #tick} last fired: distance over range, clamped to [0.1, 1]. */
    public double lastFactor() {
        return lastFactor;
    }

    /** Ticks until the next shot or recompute; -1 after a reset. */
    public int timer() {
        return timer;
    }

    /** Shots left in the current burst. */
    public int burstLeft() {
        return burstLeft;
    }

    /** Consecutive ticks the target has been seen. */
    public int ticksSeen() {
        return ticksSeen;
    }

    /**
     * Drops the per-target state: the timer goes to -1 and the sight count to 0. Call when the target is cleared or
     * the task stops (NPC-31). A partly spent burst is kept.
     */
    public void reset() {
        timer = -1;
        ticksSeen = 0;
    }

    private int betweenBursts(double raw) {
        double f = Math.min(1.0, raw);
        int variance = profile.variance();
        return (int) Math.floor(f * (profile.attackTime() - variance) + variance);
    }
}
