package com.gregguns.armor.flight.logic;

/**
 * Decides whether a back device acts this tick. One decision, used by the server tick and the client prediction, so
 * both agree. It defers to other flight: while the player is already flying through the vanilla ability (creative, or
 * another mod granted it), GregGuns does not touch their motion. GregGuns never writes the player's flight ability at
 * all, so it cannot take away flight another mod granted (GGS-05-17: the "only undo what we granted" rule of
 * {@code NightVisionPolicy}, met by granting nothing). Plain Java, both sides.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (Flight stacking; GGS-05-17, GGS-05-18)
public final class FlightPolicy {

    private FlightPolicy() {}

    /**
     * @param kind          the worn device
     * @param engaged       the glider key is held, or the anti gravity toggle is on
     * @param powered       the device has the energy for this tick
     * @param airborne      not on the ground, in water, on a ladder or riding
     * @param sneaking      the player is sneaking
     * @param foreignFlight the player is flying through the vanilla ability (creative, or another mod granted it)
     * @return what the device does
     */
    public static FlightMode mode(BackKind kind, boolean engaged, boolean powered, boolean airborne, boolean sneaking,
        boolean foreignFlight) {
        if (foreignFlight || !airborne) {
            return FlightMode.NONE;
        }
        switch (kind) {
            case GLIDER:
                // Techguns: sneak while airborne. GregGuns also glides while the back-device key is held.
                return sneaking || engaged ? FlightMode.GLIDE : FlightMode.NONE;
            case ANTI_GRAVITY:
                return engaged && powered ? FlightMode.FLY : FlightMode.NONE;
            default:
                return FlightMode.NONE;
        }
    }
}
