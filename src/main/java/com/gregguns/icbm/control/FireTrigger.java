package com.gregguns.icbm.control;

/**
 * Turns a signal into a single shot: it fires on a rising edge only, so a held redstone signal or a cover that stays
 * enabled launches once and then waits for the signal to drop (ICBM-04-20, 04-19). The first sample after a load only
 * seeds the state, so a world that loads with the signal high does not launch. A cooldown, in ticks, spaces out shots
 * further. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md (Triggers)
public final class FireTrigger {

    private boolean seeded;
    private boolean last;
    private int cooldown;

    /**
     * @param level the signal this tick (true for high)
     * @return true on the tick the signal goes from low to high, never on the first sample
     */
    public boolean rising(boolean level) {
        boolean fire = seeded && level && !last;
        seeded = true;
        last = level;
        return fire;
    }

    /** Forgets the signal, so the next sample only seeds again (a chunk load). */
    public void reset() {
        seeded = false;
        last = false;
    }

    /** @param ticks how long to refuse further shots */
    public void startCooldown(int ticks) {
        cooldown = Math.max(0, ticks);
    }

    /** Counts the cooldown down by one tick. */
    public void tick() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /** @return true while the cooldown runs */
    public boolean coolingDown() {
        return cooldown > 0;
    }

    /** @return ticks left on the cooldown */
    public int cooldownLeft() {
        return cooldown;
    }

    /** @param ticks the cooldown to restore from NBT */
    public void restoreCooldown(int ticks) {
        cooldown = Math.max(0, ticks);
    }
}
