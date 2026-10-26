package com.gregguns.icbm.explosive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The shared registry and the definition math. */
// Spec: docs/gregguns-icbm-spec/02-explosives/explosive-registry.md
class ExplosivesTest {

    @AfterEach
    void reset() {
        Explosives.clearForTests();
    }

    @Test
    void idsAreNamesAndDuplicatesAreRefused() {
        ExplosiveDef a = ExplosiveDef.builder("condensed", ExplosiveTier.T1)
            .build();
        Explosives.register(a);
        assertEquals(a, Explosives.get("condensed"));
        assertNull(Explosives.get("missing"));
        assertThrows(
            IllegalStateException.class,
            () -> Explosives.register(
                ExplosiveDef.builder("condensed", ExplosiveTier.T2)
                    .build()));
    }

    @Test
    void badIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ExplosiveDef.builder("red_matter", ExplosiveTier.T4));
        assertThrows(IllegalArgumentException.class, () -> ExplosiveDef.builder("Nuclear", ExplosiveTier.T3));
        assertThrows(IllegalArgumentException.class, () -> ExplosiveDef.builder("a:b", ExplosiveTier.T3));
    }

    @Test
    void hiddenDefsAreDetonableButNotListed() {
        Explosives.register(
            ExplosiveDef.builder("scenario", ExplosiveTier.T1)
                .hidden()
                .build());
        assertTrue(
            Explosives.all()
                .isEmpty());
        assertEquals(
            1,
            Explosives.allIncludingHidden()
                .size());
        assertFalse(Explosives.get("scenario") == null);
    }

    @Test
    void radiusGrowsSubLinearlyWithFillAndClamps() {
        ExplosiveDef d = ExplosiveDef.builder("x", ExplosiveTier.T1)
            .baseRadius(6)
            .radiusExponent(1.0 / 3.0)
            .maxFill(8)
            .build();
        assertEquals(6.0, d.radius(1), 1e-9);
        assertEquals(12.0, d.radius(8), 1e-9);
        assertEquals(12.0, d.radius(500), 1e-9, "fill clamps to the max");
        assertEquals(6.0, d.radius(-5), 1e-9);
    }

    @Test
    void centerDamageDefaultsToTheTierTargetAndScalesWithFill() {
        for (ExplosiveTier t : new ExplosiveTier[] { ExplosiveTier.T1, ExplosiveTier.T2, ExplosiveTier.T3 }) {
            ExplosiveDef d = ExplosiveDef.builder("t" + t.number(), t)
                .maxFill(16)
                .build();
            assertEquals(t.minCenterDamage(), d.damage(16), 1e-9, "a full casing meets the D34 target");
            assertEquals(t.minCenterDamage() / 2.0, d.damage(4), 1e-9, "sqrt(4/16) = half");
        }
        assertEquals(1200.0, ExplosiveTier.T1.minCenterDamage(), 0.0);
        assertEquals(3000.0, ExplosiveTier.T2.minCenterDamage(), 0.0);
        assertEquals(8000.0, ExplosiveTier.T3.minCenterDamage(), 0.0);
    }

    @Test
    void tiersFollowTheGtLadder() {
        assertEquals(3, ExplosiveTier.T1.minVoltageTier());
        assertEquals(4, ExplosiveTier.T2.minVoltageTier());
        assertEquals(6, ExplosiveTier.T3.minVoltageTier());
        assertEquals(8, ExplosiveTier.T4.minVoltageTier());
    }
}
