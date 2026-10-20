package com.gregguns.item.ammo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;

// Spec: docs/gregguns-spec/06-items/ammo.md (tooltip compatibility notes)
class AmmoCompatibilityTest {

    private static final AmmoCompatibility C = AmmoCompatibility.DEFAULT;

    @Test
    void everyRoundResolvesToAMagazineOrAGun() {
        for (AmmoDefinition def : AmmoDefinitions.ALL) {
            String round = def.type()
                .roundItemName();
            assertFalse(
                C.magazinesFilledWith(round)
                    .isEmpty()
                    && C.gunsTaking(round)
                        .isEmpty(),
                round + " has neither a magazine nor a gun");
        }
    }

    @Test
    void everyMagazineResolvesToAGun() {
        for (MagazineType mag : AmmoTypes.allMagazines()
            .values()) {
            assertFalse(
                C.gunsForMagazine(mag)
                    .isEmpty(),
                mag.id() + " is used by no gun");
        }
    }

    @Test
    void nineMmFillsPistolMagazineAndIsLoadedDirectlyByRevolvers() {
        assertTrue(
            C.magazinesFilledWith("ammo_9mm_bullet")
                .contains("mag_9mm_pistol"));
        assertTrue(
            C.gunsTaking("ammo_9mm_bullet")
                .contains("revolver"));
        assertTrue(
            C.gunsForMagazine(AmmoTypes.NINE_MM_PISTOL)
                .contains("pistol_9mm"));
    }

    @Test
    void shellsAreLoadedDirectlyAndEnergyGunsNeverAppear() {
        assertTrue(
            C.gunsTaking("ammo_shotgun_shell")
                .contains("sawed_off"));
        assertTrue(
            C.gunsTaking("energy")
                .isEmpty());
    }

    @Test
    void unknownItemsResolveToNothing() {
        assertTrue(
            C.gunsTaking("nope")
                .isEmpty());
        assertTrue(
            C.magazinesFilledWith("nope")
                .isEmpty());
    }

    @Test
    void shortListIsShownWhole() {
        List<String> out = AmmoTooltipFormat.lines("L:", Arrays.asList("a", "b", "c", "d"), false, "more");
        assertEquals(Collections.singletonList("L: a, b, c, d"), out);
    }

    @Test
    void longListIsCutWithAHintUnlessShiftHeld() {
        List<String> names = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(Arrays.asList("L: a, b, c, ...", "more"), AmmoTooltipFormat.lines("L:", names, false, "more"));
        assertEquals(Collections.singletonList("L: a, b, c, d, e"), AmmoTooltipFormat.lines("L:", names, true, "more"));
    }

    @Test
    void emptyListAddsNoLines() {
        assertTrue(
            AmmoTooltipFormat.lines("L:", Collections.<String>emptyList(), false, "more")
                .isEmpty());
    }
}
