package com.gregguns.icbm.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LauncherLoadsTest {

    @Test
    void everyWarheadIsListedOnceWithEachLauncherOfItsFamily() {
        int expected = 0;
        for (LauncherWarhead w : LauncherWarhead.values()) {
            expected += w.kind()
                .guns()
                .size();
        }
        assertEquals(
            expected,
            LauncherLoads.all()
                .size());
        Set<String> seen = new HashSet<>();
        for (LauncherLoads.Load l : LauncherLoads.all()) {
            assertTrue(
                l.warhead()
                    .kind()
                    .fits(l.gun()),
                l + " is not a load the recipe accepts");
            assertTrue(seen.add(l.toString()), l + " is listed twice");
        }
    }

    @Test
    void theRocketHeWarheadFitsBothRocketLaunchersAndNothingElse() {
        assertEquals(
            2,
            LauncherLoads.matching(null, LauncherWarhead.HE)
                .size());
        assertEquals(
            0,
            LauncherLoads.matching("grenade_launcher", LauncherWarhead.HE)
                .size());
        assertEquals(
            1,
            LauncherLoads.matching("grim_reaper", LauncherWarhead.HE)
                .size());
    }

    @Test
    void aLauncherListsEveryWarheadOfItsFamily() {
        assertEquals(
            5,
            LauncherLoads.matching("rocket_launcher", null)
                .size());
        assertEquals(
            2,
            LauncherLoads.matching("grenade_launcher", null)
                .size());
        assertEquals(
            1,
            LauncherLoads.matching("nuke_launcher", null)
                .size());
        assertEquals(
            0,
            LauncherLoads.matching("revolver", null)
                .size());
    }
}
