package com.gregguns.armor.flight.logic;

import java.util.Objects;

/**
 * What the server tells the client about the worn back device: the device, whether it is engaged and whether it has
 * the energy for its next action. The client predicts motion from it, because 1.7.10 lets the client own its player's
 * motion; the server stays the only side that spends EU. Immutable. Plain Java, both sides.
 *
 * <p>
 * Meaning of the flags per device: Glider, {@code engaged} is the held back-device key and {@code powered} is always
 * true; Jump Pack, {@code engaged} is false and {@code powered} means the charge covers one boost jump; Anti Gravity
 * Device, {@code engaged} is the toggle and {@code powered} means the charge covers one tick of drive.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-18)
public final class FlightStatus {

    /** No device. */
    public static final FlightStatus NONE = new FlightStatus(BackKind.NONE, false, false);

    private final BackKind kind;
    private final boolean engaged;
    private final boolean powered;

    /**
     * @param kind    the worn device
     * @param engaged see the class comment
     * @param powered see the class comment
     */
    public FlightStatus(BackKind kind, boolean engaged, boolean powered) {
        this.kind = kind;
        this.engaged = engaged;
        this.powered = powered;
    }

    /** @return the worn device */
    public BackKind kind() {
        return kind;
    }

    /** @return whether the device is engaged (key held or toggled on) */
    public boolean engaged() {
        return engaged;
    }

    /** @return whether the device has the energy for its next action */
    public boolean powered() {
        return powered;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FlightStatus)) {
            return false;
        }
        FlightStatus other = (FlightStatus) o;
        return kind == other.kind && engaged == other.engaged && powered == other.powered;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, engaged, powered);
    }

    @Override
    public String toString() {
        return kind + (engaged ? " engaged" : "") + (powered ? " powered" : "");
    }
}
