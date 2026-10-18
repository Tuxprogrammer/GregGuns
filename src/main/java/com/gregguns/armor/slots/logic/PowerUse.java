package com.gregguns.armor.slots.logic;

/**
 * Charge accounting for powered equipment that draws a fixed amount per tick while it is switched on. All-or-nothing:
 * a tick either pays the whole cost or the device is dead for that tick. Plain Java, both sides.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (goggles drawn from EU); docs/design/0003-energy-model.md
public final class PowerUse {

    private PowerUse() {}

    /**
     * @param enabled whether the player switched the device on
     * @param charge  stored EU
     * @param cost    EU per tick; 0 or less means free
     * @return true when the device works this tick
     */
    public static boolean works(boolean enabled, double charge, double cost) {
        return enabled && charge >= Math.max(0.0, cost) && charge > 0.0;
    }

    /**
     * @param enabled whether the device is switched on
     * @param charge  stored EU
     * @param cost    EU per tick
     * @return the EU to draw this tick: {@code cost} when the device works, else 0
     */
    public static double drain(boolean enabled, double charge, double cost) {
        return works(enabled, charge, cost) ? Math.max(0.0, cost) : 0.0;
    }

    /**
     * @param charge stored EU
     * @param max    capacity in EU
     * @return the charge as a 0..1 fraction, clamped; 0 for a zero capacity
     */
    public static double fraction(double charge, double max) {
        if (max <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, charge / max));
    }

    /**
     * @param stored  the stored switch value, meaningful only when {@code present}
     * @param present whether a value was stored at all
     * @return the effective switch: a stack that never stored one is on
     */
    public static boolean switchState(boolean stored, boolean present) {
        return !present || stored;
    }
}
