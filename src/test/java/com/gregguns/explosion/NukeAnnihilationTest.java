package com.gregguns.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Nuke Launcher's kill-zone decision (design 0016). */
// Spec: docs/design/0016-damage-balance-pass.md (Nuke Launcher)
class NukeAnnihilationTest {

    private static final double RADIUS = 10.0;

    @Test
    void killZoneIsSixtyPercentOfTheBlastRadius() {
        assertEquals(6.0, NukeAnnihilation.killRadius(RADIUS), 1e-9);
        assertTrue(NukeAnnihilation.inKillZone(6.0, RADIUS));
        assertFalse(NukeAnnihilation.inKillZone(6.01, RADIUS));
    }

    @Test
    void nonBossInsideTheZoneTakesMoreThanItsHealthAndAbsorption() {
        double amount = NukeAnnihilation.amount(2.0, RADIUS, 400.0, false, 2000.0, 20.0, 2000.0);
        assertTrue(amount > 2020.0, "lethal beyond health plus absorption: " + amount);
    }

    @Test
    void bossInsideTheZoneLosesHalfItsMaximumHealth() {
        assertEquals(150.0, NukeAnnihilation.amount(2.0, RADIUS, 400.0, true, 300.0, 0.0, 300.0), 1e-9);
        assertEquals(150.0, NukeAnnihilation.amount(6.0, RADIUS, 400.0, true, 40.0, 0.0, 300.0), 1e-9);
    }

    @Test
    void theSharedDecisionIsWhatBothCallersDeal() {
        // The Golden Gun deals NukeAnnihilation.lethal directly; the kill zone's amount() must agree with it.
        assertEquals(
            NukeAnnihilation.lethal(false, 40.0, 4.0, 40.0),
            NukeAnnihilation.amount(1.0, RADIUS, 1.0, false, 40.0, 4.0, 40.0),
            1e-9);
        assertEquals(45.0, NukeAnnihilation.lethal(false, 40.0, 4.0, 40.0), 1e-9);
        assertEquals(150.0, NukeAnnihilation.lethal(true, 40.0, 4.0, 300.0), 1e-9);
    }

    @Test
    void outsideTheZoneTheRampApplies() {
        double ramp = BlastFalloff.damage(400.0, 5.0, 15.0, 10.0);
        assertEquals(200.0, ramp, 1e-9);
        assertEquals(ramp, NukeAnnihilation.amount(10.0, RADIUS, ramp, false, 2000.0, 0.0, 2000.0), 1e-9);
        assertEquals(ramp, NukeAnnihilation.amount(10.0, RADIUS, ramp, true, 300.0, 0.0, 300.0), 1e-9);
    }

    @Test
    void theRampFallsFromTheFullDamageAtTheKillZoneEdgeToZeroAtTheOuterRadius() {
        double atEdge = BlastFalloff.damage(400.0, 5.0, 15.0, NukeAnnihilation.killRadius(RADIUS));
        assertEquals(360.0, atEdge, 1e-9);
        assertEquals(0.0, BlastFalloff.damage(400.0, 5.0, 15.0, 15.0), 1e-9);
    }
}
