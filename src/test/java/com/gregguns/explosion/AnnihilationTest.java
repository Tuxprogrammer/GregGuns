package com.gregguns.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The shared annihilation rule and its bosses flag (D30), and the Nuke Launcher keeping its 50% boss rule. */
// Spec: docs/gregguns-icbm-spec/02-explosives/annihilation.md
class AnnihilationTest {

    @Test
    void withTheBossesFlagOnABossDiesLikeAnythingElse() {
        double lethal = Annihilation.lethal(true, true, 300.0, 20.0, 300.0);
        assertEquals(321.0, lethal, 1e-9);
        assertTrue(lethal > 300.0 + 20.0, "more than health plus absorption");
    }

    @Test
    void withTheFlagOffABossLosesHalfItsMaximumHealth() {
        assertEquals(150.0, Annihilation.lethal(false, true, 300.0, 20.0, 300.0), 1e-9);
    }

    @Test
    void aNonBossAlwaysDiesWhateverTheFlag() {
        assertEquals(
            Annihilation.lethal(true, false, 2000.0, 4.0, 2000.0),
            Annihilation.lethal(false, false, 2000.0, 4.0, 2000.0),
            0.0);
        assertEquals(2005.0, Annihilation.lethal(true, false, 2000.0, 4.0, 2000.0), 1e-9);
    }

    @Test
    void theKillZoneIsSixtyPercentOfTheRadiusAndNaNIsOutside() {
        assertEquals(30.0, Annihilation.killRadius(50.0), 1e-9);
        assertTrue(Annihilation.inKillZone(30.0, 50.0));
        assertFalse(Annihilation.inKillZone(30.1, 50.0));
        assertFalse(Annihilation.inKillZone(Double.NaN, 50.0));
    }

    @Test
    void outsideTheZoneTheRampApplies() {
        assertEquals(77.0, Annihilation.amount(40.0, 50.0, 77.0, true, true, 300.0, 0.0, 300.0), 0.0);
        assertEquals(301.0, Annihilation.amount(10.0, 50.0, 77.0, true, true, 300.0, 0.0, 300.0), 1e-9);
    }

    @Test
    void theNukeLauncherKeepsItsHalfBossRuleThroughTheSharedRule() {
        assertEquals(150.0, NukeAnnihilation.lethal(true, 300.0, 0.0, 300.0), 1e-9);
        assertEquals(
            NukeAnnihilation.lethal(false, 40.0, 4.0, 40.0),
            Annihilation.lethal(false, false, 40.0, 4.0, 40.0),
            0.0);
        assertEquals(NukeAnnihilation.KILL_ZONE_SHARE, Annihilation.KILL_ZONE_SHARE, 0.0);
    }
}
