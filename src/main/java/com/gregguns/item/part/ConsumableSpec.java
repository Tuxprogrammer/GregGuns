package com.gregguns.item.part;

/**
 * The numbers of a healing consumable, as plain data: how long it takes to use, what it heals at once, and the
 * Regeneration it adds. Pure, both sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-61, RC-62), docs/techguns-spec/06-items/special-items.md (Medkit)
public final class ConsumableSpec {

    /** Bandages: 60 ticks to use, heal 2 HP, Regeneration I for 300 ticks. */
    public static final ConsumableSpec BANDAGES = new ConsumableSpec(60, 2.0F, 0, 300);
    /** Medkit: 50 ticks to use, heal 6 HP, Regeneration II for 400 ticks. */
    public static final ConsumableSpec MEDKIT = new ConsumableSpec(50, 6.0F, 1, 400);

    /** Ticks between the use sound (and the arm swing) while the item is in use. */
    public static final int FEEDBACK_PERIOD = 7;
    /** The vanilla sound played every {@link #FEEDBACK_PERIOD} ticks of use. */
    public static final String USE_SOUND = "random.eat";
    /** Ticks a player waits between two full-health notices while the button stays held. */
    public static final int NOTICE_COOLDOWN = 40;

    private final int useTicks;
    private final float healHp;
    private final int regenAmplifier;
    private final int regenTicks;

    private ConsumableSpec(int useTicks, float healHp, int regenAmplifier, int regenTicks) {
        this.useTicks = useTicks;
        this.healHp = healHp;
        this.regenAmplifier = regenAmplifier;
        this.regenTicks = regenTicks;
    }

    /** @return ticks the player holds the item to use it */
    public int useTicks() {
        return useTicks;
    }

    /** @return the instant heal in half-hearts of health points (HP) */
    public float healHp() {
        return healHp;
    }

    /** @return the Regeneration amplifier (0 is level I) */
    public int regenAmplifier() {
        return regenAmplifier;
    }

    /** @return the Regeneration duration in ticks */
    public int regenTicks() {
        return regenTicks;
    }

    /**
     * @param health    the user's current health
     * @param maxHealth the user's maximum health
     * @return whether using the item is worthwhile: it only starts below full health
     */
    public static boolean canUse(float health, float maxHealth) {
        return health < maxHealth;
    }

    /**
     * @param ticksLeft the use ticks still to go, as {@code onUsingTick} gets them
     * @return whether this tick plays the use sound and swings the arm
     */
    public static boolean isFeedbackTick(int ticksLeft) {
        return ticksLeft > 0 && ticksLeft % FEEDBACK_PERIOD == 0;
    }

    /**
     * @param lastTick the world tick of the player's last full-health notice
     * @param now      the world tick now
     * @return whether another notice may go out (also after the world clock went backwards)
     */
    public static boolean noticeDue(long lastTick, long now) {
        return now < lastTick || now - lastTick >= NOTICE_COOLDOWN;
    }
}
