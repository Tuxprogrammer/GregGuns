package com.gregguns.armor.logic;

/**
 * How a powered set stores the energy that switches its powered bonuses on. The chestplate holds all of it; the other
 * pieces only read whether the chestplate is charged. Plain Java, both sides.
 *
 * <p>
 * Energy is counted in "units", the Techguns {@code power} number: a worn piece costs one unit per second of walking
 * ({@link PowerRules}). A {@link PowerKind#STEAM} chestplate keeps its units in the stack tag and is refilled from a
 * Compressed Air Tank. A {@link PowerKind#ELECTRIC} chestplate is an IC2 electric item whose EU convert at
 * {@code euPerUnit} per unit, so GT chargers fill it (design 0003 and 0008).
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 9; docs/gregguns-spec/05-armor/powered-flight.md
public final class PowerSpec {

    /** What fuels the set. */
    public enum PowerKind {
        /** Not powered: every bonus is always on. */
        NONE,
        /** Compressed air, burned in the chestplate (steam armor). Not EU-powered. */
        STEAM,
        /** An IC2 electric chestplate at the set's tier. */
        ELECTRIC
    }

    /** A set with no power. */
    public static final PowerSpec NONE = new PowerSpec(PowerKind.NONE, 0, 0.0);

    private final PowerKind kind;
    private final int capacityUnits;
    private final double euPerUnit;

    private PowerSpec(PowerKind kind, int capacityUnits, double euPerUnit) {
        this.kind = kind;
        this.capacityUnits = capacityUnits;
        this.euPerUnit = euPerUnit;
    }

    /**
     * @param capacityUnits what a full chestplate holds, in units (Techguns: 3600 for steam armor)
     * @return a steam power spec
     */
    public static PowerSpec steam(int capacityUnits) {
        return new PowerSpec(PowerKind.STEAM, capacityUnits, 0.0);
    }

    /**
     * @param capacityUnits what a full chestplate holds, in units
     * @param euPerUnit     EU per unit; 40 makes one worn piece cost the GT Mech Armor rate of 2 EU/t while walking
     * @return an electric power spec
     */
    public static PowerSpec electric(int capacityUnits, double euPerUnit) {
        return new PowerSpec(PowerKind.ELECTRIC, capacityUnits, euPerUnit);
    }

    /** @return what fuels the set */
    public PowerKind kind() {
        return kind;
    }

    /** @return whether the set has powered bonuses to switch */
    public boolean powered() {
        return kind != PowerKind.NONE;
    }

    /** @return the capacity of a full chestplate, in units */
    public int capacityUnits() {
        return capacityUnits;
    }

    /** @return EU per unit for an electric set, else 0 */
    public double euPerUnit() {
        return euPerUnit;
    }

    /** @return the capacity in EU for an electric set, else 0 */
    public double capacityEu() {
        return capacityUnits * euPerUnit;
    }
}
