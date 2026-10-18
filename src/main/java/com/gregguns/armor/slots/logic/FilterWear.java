package com.gregguns.armor.slots.logic;

/**
 * Filter accounting of the Gas Mask: the mask removes Poison and Wither, one filter tick per effect removed, and only
 * while a filter tick is left. The filter is the mask's durability: damage counts used ticks, and like all GregGuns
 * armor the mask never breaks, so one point is always left ("spent" means at most one point left: no cure and no GAS
 * protection). Plain Java, both sides.
 */
// Spec: docs/techguns-spec/05-armor/special-equipment.md section 3.1; docs/gregguns-spec/05-armor/hazard-protection.md
// (GGS-05-13)
public final class FilterWear {

    private FilterWear() {}

    /**
     * @param damage    stack damage (used filter ticks)
     * @param maxDamage filter capacity
     * @return true when no usable filter tick is left
     */
    public static boolean spent(int damage, int maxDamage) {
        return damage >= maxDamage - 1;
    }

    /** The outcome of one tick of the mask. */
    public static final class Result {

        private final int damage;
        private final boolean curePoison;
        private final boolean cureWither;

        Result(int damage, boolean curePoison, boolean cureWither) {
            this.damage = damage;
            this.curePoison = curePoison;
            this.cureWither = cureWither;
        }

        /** @return the new stack damage */
        public int damage() {
            return damage;
        }

        /** @return whether to remove Poison */
        public boolean curePoison() {
            return curePoison;
        }

        /** @return whether to remove Wither */
        public boolean cureWither() {
            return cureWither;
        }
    }

    /**
     * One tick of the mask. Poison is handled first, then Wither, each costing one filter tick while a tick is left.
     *
     * @param damage    stack damage
     * @param maxDamage filter capacity
     * @param poisoned  whether the wearer has Poison
     * @param withered  whether the wearer has Wither
     * @return what to do and the new damage
     */
    public static Result tick(int damage, int maxDamage, boolean poisoned, boolean withered) {
        int d = Math.max(0, damage);
        boolean cureP = false;
        boolean cureW = false;
        if (poisoned && !spent(d, maxDamage)) {
            cureP = true;
            d++;
        }
        if (withered && !spent(d, maxDamage)) {
            cureW = true;
            d++;
        }
        return new Result(d, cureP, cureW);
    }
}
