package com.gregguns.icbm.component;

import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.LaunchTarget;
import com.gregguns.icbm.control.TargetStick;
import com.gregguns.icbm.missile.MissileStatus;

/**
 * What the {@code gg_silo} and {@code gg_launch_control} components need from a machine that fires a missile. The
 * Launch Control and the Missile Silo implement it, and unit tests use a fake, so the callbacks are checked without a
 * world. Every call runs on the server thread (GGI-01-03). No OpenComputers types.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_silo, gg_launch_control)
public interface LaunchView {

    /** @return the stored target, or {@link LaunchTarget#NONE} */
    LaunchTarget launchTarget();

    /**
     * @param x block X, already inside plus or minus {@link LaunchTarget#LIMIT}
     * @param z block Z
     */
    void aimAt(int x, int z);

    /** @return the outcome of loading the target from the data stick the block holds; no stick is an outcome too */
    TargetStick.Outcome loadTargetFromStick();

    /** @return the missile on the pad or in the shaft; {@link MissileStatus#NONE} for nothing */
    MissileStatus missileStatus();

    /** @return the registry name of the fuel a tank of this missile takes, or an empty string with no missile */
    String fuelFluid();

    /** @return the pre-check status right now, the same one the GUI shows (power, cooldown, missile, fuel, range) */
    ControlStatus launchStatus();

    /**
     * @return the status that decided; {@link ControlStatus#FIRED} means the missile went (the same gate as the button)
     */
    ControlStatus launchNow();

    /** @return how many missiles it has launched since it loaded */
    int launches();

    /** @return constants of the machine for {@code getInfo()}: EU per shot, cooldown, whether it is formed */
    java.util.Map<String, Object> launchInfo();
}
