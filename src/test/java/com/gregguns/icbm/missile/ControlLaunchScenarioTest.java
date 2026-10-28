package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Guards the geometry of {@code icbm.control.launch_to_impact}: its platforms must cover the missile's scatter. */
class ControlLaunchScenarioTest {

    @Test
    void theTargetPlatformCoversTheUnguidedT1ScatterAtTheScenarioRange() {
        MissileTier t1 = MissileTier.T1;
        double half = Scatter.halfWidth(t1.baseScatter(), 0, 70, t1.baseMaxRange());
        // The aim point is anywhere within +-half of the target on each axis; a platform narrower than that is
        // missed, and a blast over bare ground 25 blocks lower breaks none of it (the I3 and I5 flake).
        assertTrue(
            ControlLaunchScenario.PLATFORM_RADIUS >= Math.ceil(half),
            "platform radius " + ControlLaunchScenario.PLATFORM_RADIUS + " must cover the scatter " + half);
    }
}
