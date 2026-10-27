package com.gregguns.icbm.emp;

/**
 * ICBM's EMP kill-chance table for a missile in flight (D16), as a chain of rolls. Each step rolls once: destroy, else
 * fry the guidance, else trigger the warhead, else nothing. With the defaults one pulse destroys 50%, fries the
 * guidance of 40%, triggers 1% and leaves 9% alone. Chances are clamped to 0 to 1. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md (Missiles)
public final class MissileTable {

    /** What one pulse does to one missile. */
    public enum Outcome {
        /** The missile is removed and its fuel goes off in a small blast that breaks nothing; the warhead does not. */
        DESTROYED,
        /** The guidance is gone: no thrust, the missile falls and detonates where it lands. */
        FRIED,
        /** The warhead goes off where the missile is. */
        TRIGGERED,
        /** Nothing happens. */
        UNAFFECTED
    }

    /** The ICBM defaults. */
    public static final MissileTable DEFAULT = new MissileTable(0.5, 0.8, 0.1);

    private final double destroy;
    private final double fry;
    private final double trigger;

    /**
     * @param destroy chance the missile is destroyed outright
     * @param fry     chance, among the survivors, that the guidance is fried
     * @param trigger chance, among those left, that the warhead is triggered
     */
    public MissileTable(double destroy, double fry, double trigger) {
        this.destroy = clamp(destroy);
        this.fry = clamp(fry);
        this.trigger = clamp(trigger);
    }

    private static double clamp(double chance) {
        return Double.isNaN(chance) ? 0.0 : Math.max(0.0, Math.min(1.0, chance));
    }

    /**
     * Rolls the chain.
     *
     * @param first  a uniform random number in [0, 1) for the destroy step
     * @param second the same for the fry step
     * @param third  the same for the trigger step
     * @return the outcome
     */
    public Outcome roll(double first, double second, double third) {
        if (first < destroy) {
            return Outcome.DESTROYED;
        }
        if (second < fry) {
            return Outcome.FRIED;
        }
        return third < trigger ? Outcome.TRIGGERED : Outcome.UNAFFECTED;
    }

    /** @return the chance of {@link Outcome#DESTROYED} per pulse */
    public double destroyedChance() {
        return destroy;
    }

    /** @return the chance of {@link Outcome#FRIED} per pulse */
    public double friedChance() {
        return (1.0 - destroy) * fry;
    }

    /** @return the chance of {@link Outcome#TRIGGERED} per pulse */
    public double triggeredChance() {
        return (1.0 - destroy) * (1.0 - fry) * trigger;
    }

    /** @return the chance of {@link Outcome#UNAFFECTED} per pulse */
    public double unaffectedChance() {
        return (1.0 - destroy) * (1.0 - fry) * (1.0 - trigger);
    }
}
