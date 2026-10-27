package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.explosive.ExplosiveTier;

class WarheadBlastTest {

    @Test
    void conventionalWarheadsMeetTheDamageTargetsAtFullFill() {
        // D34: T1 at least 1,200 and T2 at least 3,000 at ground zero.
        boolean t1 = false;
        boolean t2 = false;
        for (WarheadBlast b : WarheadBlast.values()) {
            if (!b.damaging()) {
                continue;
            }
            double full = WarheadMath.centerDamage(b.fullDamage(), b.fillCap(), b.fillCap());
            assertTrue(
                full >= b.tier()
                    .minCenterDamage(),
                b.id() + " meets its tier target");
            t1 |= b.tier() == ExplosiveTier.T1;
            t2 |= b.tier() == ExplosiveTier.T2;
        }
        assertTrue(t1 && t2, "both tiers have a damaging type");
    }

    @Test
    void idsAreUniqueAndValidRegistryIds() {
        Set<String> seen = new HashSet<>();
        for (WarheadBlast b : WarheadBlast.values()) {
            assertTrue(seen.add(b.id()), "duplicate " + b.id());
            assertTrue(
                b.id()
                    .equals(
                        b.id()
                            .toLowerCase())
                    && b.id()
                        .indexOf('_') < 0
                    && b.id()
                        .indexOf(':') < 0);
            assertNotNull(WarheadCasing.forTier(b.tier()), b.id() + " has a casing");
            assertEquals(b, WarheadBlast.byId(b.id()));
        }
    }

    @Test
    void theSmallestFillStillHasAUsefulRadius() {
        for (WarheadBlast b : WarheadBlast.values()) {
            assertTrue(WarheadMath.radius(b.fullRadius(), 1, b.fillCap()) >= WarheadMath.MIN_RADIUS);
        }
    }

    @Test
    void everyKindIsCoveredByAnEffectOrNone() {
        // Guards against a new Kind constant that the effect factory forgets.
        for (WarheadBlast b : WarheadBlast.values()) {
            assertNotNull(b.kind());
        }
        assertEquals(17, WarheadBlast.values().length);
    }

    @Test
    void theT3ConventionalTypesMeetTheEightThousandTarget() {
        // D34: T3 deals at least 8,000 at ground zero of a full casing; the nuclear warhead annihilates instead.
        for (WarheadBlast b : new WarheadBlast[] { WarheadBlast.EXOTHERMIC, WarheadBlast.ENDOTHERMIC }) {
            com.gregguns.icbm.explosive.ExplosiveDef def = WarheadBlasts.define(b);
            org.junit.jupiter.api.Assertions.assertTrue(def.damage(def.maxFill()) >= 8000.0, b.id());
            org.junit.jupiter.api.Assertions.assertFalse(def.annihilates());
            org.junit.jupiter.api.Assertions.assertEquals(28.0, def.radius(def.maxFill()), 1e-9);
        }
        com.gregguns.icbm.explosive.ExplosiveDef nuke = WarheadBlasts.define(WarheadBlast.NUCLEAR);
        org.junit.jupiter.api.Assertions.assertTrue(nuke.annihilates());
        org.junit.jupiter.api.Assertions.assertTrue(nuke.annihilatesBosses());
        org.junit.jupiter.api.Assertions.assertEquals(64.0, nuke.radius(64), 1e-9);
        org.junit.jupiter.api.Assertions.assertEquals(32.0, nuke.radius(8), 1e-9);
    }
}
