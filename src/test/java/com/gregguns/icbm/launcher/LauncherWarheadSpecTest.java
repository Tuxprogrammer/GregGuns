package com.gregguns.icbm.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.recipe.Ing;
import com.gregguns.icbm.warhead.WarheadBlast;

class LauncherWarheadSpecTest {

    private static WarheadBlast blast(String id) {
        for (WarheadBlast b : WarheadBlast.values()) {
            if (b.id()
                .equals(id)) {
                return b;
            }
        }
        return null;
    }

    @Test
    void everyWarheadNamesARealBlastOfTheLaunchersTier() {
        for (LauncherWarhead w : LauncherWarhead.values()) {
            WarheadBlast b = blast(w.blastId());
            assertNotNull(b, w + " names an unknown blast " + w.blastId());
            assertTrue(
                w.tier()
                    .voltageIndex()
                    >= b.tier()
                        .minVoltageTier(),
                w + " is cheaper than its blast's tier");
        }
    }

    @Test
    void recipesStayAtOrAboveHvAndWithinTheirTier() {
        for (LauncherWarhead w : LauncherWarhead.values()) {
            assertTrue(
                w.tier()
                    .voltageIndex() >= 3,
                w + " is below HV (D69)");
            assertTrue(
                w.inputs()
                    .size() <= 8,
                w + " has too many slots");
            for (Ing in : w.inputs()) {
                assertTrue(
                    in.tier()
                        .voltageIndex()
                        <= w.tier()
                            .voltageIndex(),
                    w + " uses " + in.id() + " above its machine");
            }
        }
    }

    @Test
    void keysCircuitsAndItemNamesAreUnique() {
        Set<String> keys = new HashSet<>();
        Set<Integer> circuits = new HashSet<>();
        for (LauncherWarhead w : LauncherWarhead.values()) {
            assertTrue(keys.add(w.key()), "duplicate key " + w.key());
            assertTrue(
                circuits.add(
                    w.tier()
                        .voltageIndex() * 100 + w.circuit()),
                "duplicate circuit in " + w);
            assertEquals("launcher_warhead_" + w.key(), w.itemName());
            assertEquals(w, LauncherWarhead.byKey(w.key()));
        }
    }

    @Test
    void costGrowsWithTierAndStaysInTheBand() {
        for (LauncherWarhead w : LauncherWarhead.values()) {
            double cost = 0;
            for (Ing in : w.inputs()) {
                cost += in.cost();
            }
            // Four charges: from about a plain rocket's worth up to a mid-T2 warhead's; D63 reading in the sheet.
            assertTrue(cost >= 8.0 && cost <= 60.0, w + " costs " + cost);
        }
    }

    @Test
    void everyFamilyHasAWarheadAndNoUnknownGuns() {
        for (LauncherKind k : LauncherKind.values()) {
            boolean any = false;
            for (LauncherWarhead w : LauncherWarhead.values()) {
                any |= w.kind() == k;
            }
            assertTrue(any, k + " has no warhead");
            assertTrue(
                !k.guns()
                    .isEmpty());
        }
        assertTrue(LauncherKind.ROCKET.fits("grim_reaper"));
        assertTrue(!LauncherKind.ROCKET.fits("grenade_launcher"));
    }
}
