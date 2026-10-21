package com.gregguns.gun.logic;

/**
 * The spin-up gate of a rotary gun (the Minigun) as a pure state machine. Spin is a scalar {@code s} in [0, 1]: while
 * the trigger is held it rises in a straight line to 1 over {@code upTicks}; while released it falls in a straight
 * line to 0 over {@code downTicks}. A shot is allowed only at full spin. Pressing again during a spin-down resumes the
 * rise from the current {@code s}, so the time to fire is {@code upTicks * (1 - s)}: a barrel that has barely slowed
 * fires again almost at once, as on a real minigun.
 *
 * <p>
 * Server and client prediction mirror run the same instance type, one per player, advanced once per tick. Switching
 * items, starting a reload and dying call {@link #reset()}. It knows nothing of ammo: holding the trigger on an empty
 * gun still spins (a dry spin).
 *
 * <p>
 * Spec: docs/gregguns-spec/02-guns/index.md (Minigun, recorded deviation). Techguns fires at once.
 */
public final class SpinGate {

    private static final double EPS = 1e-9;

    private final int upTicks;
    private final int downTicks;
    private double spin;

    /**
     * @param upTicks   ticks from rest to full spin with the trigger held; at least 1
     * @param downTicks ticks from full spin to rest with the trigger released; at least 1
     */
    public SpinGate(int upTicks, int downTicks) {
        if (upTicks < 1 || downTicks < 1) {
            throw new IllegalArgumentException("spin ticks must be at least 1");
        }
        this.upTicks = upTicks;
        this.downTicks = downTicks;
    }

    /** @return ticks from rest to full spin */
    public int upTicks() {
        return upTicks;
    }

    /** @return ticks from full spin to rest */
    public int downTicks() {
        return downTicks;
    }

    /**
     * Advances one tick.
     *
     * @param triggerHeld true if the trigger is held this tick
     */
    public void tick(boolean triggerHeld) {
        if (triggerHeld) {
            spin += 1.0 / upTicks;
            if (spin > 1.0 - EPS) {
                spin = 1.0;
            }
        } else {
            spin -= 1.0 / downTicks;
            if (spin < EPS) {
                spin = 0.0;
            }
        }
    }

    /** Stops dead: an item swap, a reload start or a death. */
    public void reset() {
        spin = 0.0;
    }

    /** @return the spin, 0 (at rest) to 1 (full) */
    public double spin() {
        return spin;
    }

    /** @return true at full spin: a shot may leave */
    public boolean isReady() {
        return spin >= 1.0;
    }

    /**
     * The latency-tolerant test for a packet-driven shot request, which the client sent when its own barrel was at
     * speed: accepted from {@code graceTicks} of rise short of full.
     *
     * @param graceTicks ticks of latency to forgive (0 is the strict {@link #isReady()})
     */
    public boolean isReadyWithGrace(int graceTicks) {
        return spin >= 1.0 - (double) Math.max(0, graceTicks) / upTicks - EPS;
    }

    /** @return ticks of holding the trigger until full spin from now (0 when ready) */
    public int ticksToReady() {
        return (int) Math.ceil((1.0 - spin) * upTicks - EPS);
    }
}
