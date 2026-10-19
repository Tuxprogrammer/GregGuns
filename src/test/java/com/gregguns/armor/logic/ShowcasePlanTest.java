package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.armor.model.ArmorModels;

class ShowcasePlanTest {

    private static List<ShowcasePlan.SetSpec> everySet() {
        List<ShowcasePlan.SetSpec> specs = new ArrayList<>();
        for (SetDef set : ArmorCatalog.ALL) {
            specs.add(new ShowcasePlan.SetSpec(set.id(), set.camoCount(), ArmorModels.hasModel(set.id())));
        }
        return specs;
    }

    @Test
    void ticksNeverGoBackwards() {
        int last = -1;
        for (ShowcasePlan.Step s : ShowcasePlan.build(everySet())) {
            assertTrue(s.tick() >= last, "tick " + s.tick() + " after " + last);
            last = s.tick();
        }
    }

    @Test
    void everySetAndEveryCamoGetsAFrontShotWithAnEquipBeforeIt() {
        List<ShowcasePlan.Step> steps = ShowcasePlan.build(everySet());
        for (SetDef set : ArmorCatalog.ALL) {
            for (int camo = 0; camo < set.camoCount(); camo++) {
                String label = ShowcasePlan.label(set.id(), camo, "front");
                int equipTick = -1;
                int shotTick = -1;
                for (ShowcasePlan.Step s : steps) {
                    if (s.setId()
                        .equals(set.id()) && s.camo() == camo) {
                        if (s.kind() == ShowcasePlan.Kind.EQUIP) {
                            equipTick = s.tick();
                        }
                        if (s.kind() == ShowcasePlan.Kind.SHOT && s.argument()
                            .equals(label)) {
                            shotTick = s.tick();
                        }
                    }
                }
                assertTrue(equipTick >= 0 && shotTick >= 0, label);
                assertTrue(
                    shotTick - equipTick >= ShowcasePlan.FRONT_DELAY,
                    label + " is shot too soon after equipping");
            }
        }
    }

    @Test
    void onlyCustomModelSetsGetABackShot() {
        for (ShowcasePlan.Step s : ShowcasePlan.build(everySet())) {
            boolean back = s.kind() == ShowcasePlan.Kind.SHOT && s.argument()
                .endsWith("-back");
            if (back) {
                assertTrue(ArmorModels.hasModel(s.setId()), s.argument());
            }
        }
        long backs = ShowcasePlan.build(everySet())
            .stream()
            .filter(
                s -> s.kind() == ShowcasePlan.Kind.SHOT && s.argument()
                    .endsWith("-back"))
            .count();
        long expected = 0;
        for (SetDef set : ArmorCatalog.ALL) {
            if (ArmorModels.hasModel(set.id())) {
                expected += set.camoCount();
            }
        }
        assertEquals(expected, backs);
    }

    @Test
    void labelsAreUniqueAndShotsAreSpacedForTheClientsFrames() {
        Set<String> labels = new HashSet<>();
        int lastShot = -100;
        for (ShowcasePlan.Step s : ShowcasePlan.build(everySet())) {
            if (s.kind() != ShowcasePlan.Kind.SHOT) {
                continue;
            }
            assertTrue(labels.add(s.argument()), "duplicate label " + s.argument());
            assertTrue(s.tick() - lastShot >= 8, "shots too close at " + s.tick());
            lastShot = s.tick();
        }
    }

    @Test
    void theBackViewIsSelectedBeforeItsShot() {
        List<ShowcasePlan.Step> steps = ShowcasePlan
            .build(java.util.Collections.singletonList(new ShowcasePlan.SetSpec("steam", 1, true)));
        assertEquals(5, steps.size());
        assertEquals(
            ShowcasePlan.Kind.VIEW,
            steps.get(3)
                .kind());
        assertEquals(
            "1",
            steps.get(3)
                .argument());
        assertTrue(
            ShowcasePlan.endTick(steps) > steps.get(4)
                .tick());
    }

    @Test
    void aSetNeedsACamo() {
        assertThrows(IllegalArgumentException.class, () -> new ShowcasePlan.SetSpec("x", 0, false));
    }
}
