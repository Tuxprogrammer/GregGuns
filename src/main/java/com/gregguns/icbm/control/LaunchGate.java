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

    /** The status line while no target is set and the missile is armed and fueled. */
    public static final String SITE_READY_KEY = "gregguns.icbm.control.gui.status.site_ready";

    private LaunchGate() {}

    /**
     * @param target      the stored target
     * @param padFound    whether a pad is within reach
     * @param energyOk    whether the block (or its silo) holds a full shot of EU
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
        return check(
            target,
            padFound,
            energyOk,
            false,
            coolingDown,
            present,
            armed,
            fueled,
            distance,
            minRange,
            maxRange);
    }

    /**
     * As {@link #check(LaunchTarget, boolean, boolean, boolean, boolean, boolean, boolean, double, double, double)}
     * for a block that may be part of a silo.
     *
     * @param siloPowered whether the shot is paid from the bound silo's Energy Hatches (D300), so too little EU reads
     *                    {@link ControlStatus#SILO_NO_POWER}
     */
    public static ControlStatus check(LaunchTarget target, boolean padFound, boolean energyOk, boolean siloPowered,
        boolean coolingDown, boolean present, boolean armed, boolean fueled, double distance, double minRange,
        double maxRange) {
        if (!target.isSet()) {
            return ControlStatus.NO_TARGET;
        }
        ControlStatus site = checkSite(padFound, energyOk, siloPowered, coolingDown, present, armed, fueled);
        if (site != ControlStatus.READY) {
            return site;
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
     * The part of {@link #check} that does not need a target: pad, power, cooldown, missile, arming and fuel, in the
     * same order. The screen shows it while no target is set, so the status line says something the target line
     * below it does not.
     *
     * @param padFound    whether a pad is within reach
     * @param energyOk    whether the block (or its silo) holds a full shot of EU
     * @param coolingDown whether the block is in its cooldown
     * @param present     whether a missile entity stands on the pad
     * @param armed       whether it is armed
     * @param fueled      whether its tank is full
     * @return the first problem, or {@link ControlStatus#READY} when only a target is missing
     */
    public static ControlStatus checkSite(boolean padFound, boolean energyOk, boolean coolingDown, boolean present,
        boolean armed, boolean fueled) {
        return checkSite(padFound, energyOk, false, coolingDown, present, armed, fueled);
    }

    /**
     * As {@link #checkSite(boolean, boolean, boolean, boolean, boolean, boolean)} for a block that may be part of a
     * silo.
     *
     * @param siloPowered whether the shot is paid from the bound silo's Energy Hatches (D300)
     */
    public static ControlStatus checkSite(boolean padFound, boolean energyOk, boolean siloPowered, boolean coolingDown,
        boolean present, boolean armed, boolean fueled) {
        if (!padFound) {
            return ControlStatus.NO_PAD;
        }
        if (!energyOk) {
            return siloPowered ? ControlStatus.SILO_NO_POWER : ControlStatus.NO_POWER;
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
        return ControlStatus.READY;
    }

    /**
     * The lang key of the screen's status line. With no target set, the target line already says so ("No target: use
     * a Radar Gun"), so the status line reports the silo or pad instead: its first problem, or that the missile is
     * armed and fueled.
     *
     * @param status     the gate's verdict ({@link #check})
     * @param siteStatus the verdict without the target ({@link #checkSite})
     * @return the lang key to show
     */
    public static String statusLineKey(ControlStatus status, ControlStatus siteStatus) {
        if (status != ControlStatus.NO_TARGET) {
            return status.langKey();
        }
        return siteStatus == ControlStatus.READY ? SITE_READY_KEY : siteStatus.langKey();
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
