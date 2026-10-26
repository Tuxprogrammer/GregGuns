package com.gregguns.icbm.control;

/**
 * The pre-check the launch-control block runs before it asks the pad to launch, and the same one its GUI shows (the
 * button is locked and says why). The order is fixed so the first problem is the one the player sees: target, pad,
 * power, cooldown, missile, arming, fuel, range. The pad has the final say; its own result is mapped by
 * {@link #fromResult}. The power is checked here but paid only after the pad accepted the launch, so a refused shot
 * costs nothing (ICBM-04-07). No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md (Pre-check)
public final class LaunchGate {

    private LaunchGate() {}

    /**
     * @param target      the stored target
     * @param padFound    whether a pad is within reach
     * @param energyOk    whether the block holds a full shot of EU
     * @param coolingDown whether the block is in its cooldown
     * @param present     whether a missile entity stands on the pad
     * @param armed       whether it is armed
     * @param fueled      whether its tank is full
     * @param distance    horizontal distance to the target, blocks
     * @param minRange    shortest legal distance, blocks
     * @param maxRange    longest legal distance, blocks; 0 when unknown (no missile)
     * @return the first problem, or {@link ControlStatus#READY}
     */
    public static ControlStatus check(LaunchTarget target, boolean padFound, boolean energyOk, boolean coolingDown,
        boolean present, boolean armed, boolean fueled, double distance, double minRange, double maxRange) {
        if (!target.isSet()) {
            return ControlStatus.NO_TARGET;
        }
        if (!padFound) {
            return ControlStatus.NO_PAD;
        }
        if (!energyOk) {
            return ControlStatus.NO_POWER;
        }
        if (coolingDown) {
            return ControlStatus.COOLDOWN;
        }
        if (!present) {
            return ControlStatus.NO_MISSILE;
        }
        if (!armed) {
            return ControlStatus.NOT_ARMED;
        }
        if (!fueled) {
            return ControlStatus.NOT_FUELED;
        }
        if (distance < minRange) {
            return ControlStatus.TOO_CLOSE;
        }
        if (distance > maxRange) {
            return ControlStatus.OUT_OF_RANGE;
        }
        return ControlStatus.READY;
    }

    /**
     * Maps the pad's answer to a status. Names match one to one, plus {@code OK} to {@link ControlStatus#FIRED}.
     *
     * @param resultName the name of a {@code LaunchResult}
     * @return the status; {@link ControlStatus#BAD_TARGET} for a name this block does not know
     */
    public static ControlStatus fromResult(String resultName) {
        if ("OK".equals(resultName)) {
            return ControlStatus.FIRED;
        }
        try {
            return ControlStatus.valueOf(resultName);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ControlStatus.BAD_TARGET;
        }
    }
}
