package com.gregguns.combat;

/**
 * A status effect a hit adds on top of its damage. Plain data; {@link SecondaryEffectApplier} turns it into potions or
 * fire.
 */
public final class SecondaryEffect {

    /** What the effect does. */
    public enum Kind {
        /** Sets the target on fire. */
        IGNITE,
        /** Slowness. */
        SLOWNESS,
        /** Poison. */
        POISON,
        /** The radiation set: Weakness, Hunger and Nausea. */
        RADIATION_SICKNESS,
        /** Electric shock: Mining Fatigue. */
        SHOCK
    }

    private final Kind kind;
    private final int durationTicks;
    private final int amplifier;

    /**
     * @param kind          the effect
     * @param durationTicks how long, in ticks
     * @param amplifier     potion amplifier (0 = level I)
     */
    public SecondaryEffect(Kind kind, int durationTicks, int amplifier) {
        this.kind = kind;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    /** @return the effect */
    public Kind kind() {
        return kind;
    }

    /** @return the duration in ticks */
    public int durationTicks() {
        return durationTicks;
    }

    /** @return the potion amplifier */
    public int amplifier() {
        return amplifier;
    }
}
