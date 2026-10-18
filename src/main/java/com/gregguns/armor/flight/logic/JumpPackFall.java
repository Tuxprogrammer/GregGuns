package com.gregguns.armor.flight.logic;

/**
 * Jump Pack fall absorption. A fall longer than {@link #PAID_FROM} blocks costs one absorption's EU; when paid, the
 * pack removes {@link #FREE_HEIGHT} blocks and {@link #REDUCTION} of the rest (Techguns 4.4: free height 8, reduction
 * 0.2, both only while the pack has charge). A pack that cannot pay changes nothing (GGS-05-19). Plain Java.
 */
// Spec: docs/techguns-spec/05-armor/special-equipment.md 4.4; docs/gregguns-spec/05-armor/powered-flight.md
// (GGS-05-19)
public final class JumpPackFall {

    /** Falls up to this many blocks are harmless and cost nothing. */
    public static final double PAID_FROM = 3.0;
    /** Blocks of fall the pack removes outright. */
    public static final double FREE_HEIGHT = 8.0;
    /** Fraction of the remaining distance the pack removes. */
    public static final double REDUCTION = 0.2;

    /** The result of one fall. */
    public static final class Outcome {

        private final double distance;
        private final boolean paid;

        Outcome(double distance, boolean paid) {
            this.distance = distance;
            this.paid = paid;
        }

        /** @return the fall distance vanilla should use for damage */
        public double distance() {
            return distance;
        }

        /** @return whether the pack spent its absorption cost */
        public boolean paid() {
            return paid;
        }
    }

    private JumpPackFall() {}

    /**
     * @param distance the fall distance in blocks
     * @param charge   the pack's stored EU
     * @param cost     EU one absorption costs; 0 or less means free
     * @return the distance to use and whether the cost applies
     */
    public static Outcome resolve(double distance, double charge, double cost) {
        boolean canPay = charge > 0.0 && charge >= Math.max(0.0, cost);
        if (distance <= PAID_FROM || !canPay) {
            return new Outcome(distance, false);
        }
        double d = Math.max(0.0, distance - FREE_HEIGHT);
        return new Outcome(d * (1.0 - REDUCTION), true);
    }
}
