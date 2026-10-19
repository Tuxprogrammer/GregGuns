package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The akimbo wrapper: hands drawn, which hand animates, the mirror and the round split. */
class AkimboLayoutTest {

    @Test
    void akimboDrawsTwoCopiesInFirstPersonAndOneElsewhere() {
        assertEquals(2, AkimboLayout.handsDrawn(true, true));
        assertEquals(1, AkimboLayout.handsDrawn(true, false));
        assertEquals(1, AkimboLayout.handsDrawn(false, true));
    }

    @Test
    void onlyTheHandThatFiredAnimatesInFirstPerson() {
        assertTrue(AkimboLayout.animates(true, true, 0, 0));
        assertFalse(AkimboLayout.animates(true, true, 1, 0));
        assertFalse(AkimboLayout.animates(true, true, 0, 1));
        assertTrue(AkimboLayout.animates(true, true, 1, 1));
    }

    @Test
    void theSingleDrawnGunTakesEveryShotInThirdPersonAndForSingleGuns() {
        assertTrue(AkimboLayout.animates(true, false, 0, 1));
        assertTrue(AkimboLayout.animates(true, false, 0, 0));
        assertTrue(AkimboLayout.animates(false, true, 0, 0));
    }

    @Test
    void theMainGunIsNotMovedAndTheSecondIsMirroredAboutTheScreenCentre() {
        assertEquals(0.5, AkimboLayout.mirroredX(0.5, 0), 1e-12);
        double right = 0.3; // a point of the main gun, right of the hand origin
        double centre = AkimboLayout.SCREEN_CENTRE_X;
        double left = AkimboLayout.mirroredX(right, 1);
        // equidistant from the centre, on the other side
        assertEquals(right - centre, centre - left, 1e-12);
        assertEquals(-1.0, AkimboLayout.mirrorScaleX(1), 0.0);
    }

    @Test
    void mirroringTwiceReturnsTheOriginalPoint() {
        double x = 0.77;
        double once = AkimboLayout.mirroredX(x, 1);
        double twice = AkimboLayout.mirrorTranslateX(1) + AkimboLayout.mirrorScaleX(1) * once;
        assertEquals(x, twice, 1e-12);
    }

    @Test
    void roundsSplitByParityAndSumToTheTotal() {
        for (int total = 0; total <= 36; total++) {
            assertEquals(total, AkimboLayout.handAmmo(total, 0) + AkimboLayout.handAmmo(total, 1));
        }
        assertEquals(18, AkimboLayout.handAmmo(36, 0));
        assertEquals(18, AkimboLayout.handAmmo(36, 1));
        assertEquals(3, AkimboLayout.handAmmo(5, 0));
        assertEquals(2, AkimboLayout.handAmmo(5, 1));
        assertEquals(0, AkimboLayout.handAmmo(-4, 0));
    }

    @Test
    void anyGunsProfileWrapsIntoAnAkimboDefinitionSharingItsModelAndTextures() {
        for (ClientGunDefinition single : new ClientGunDefinition[] { Pistol9mmProfile.DEFINITION,
            RevolverProfile.DEFINITION, Mac10Profile.DEFINITION }) {
            ClientGunDefinition pair = ClientGunDefinition.akimboOf("akimbo_x", single);
            assertTrue(pair.akimbo());
            assertFalse(single.akimbo());
            assertEquals("akimbo_x", pair.gunName());
            assertEquals(single.boxes().length, pair.boxes().length);
            assertEquals(single.parts(), pair.parts());
            assertEquals(java.util.Arrays.asList(single.textures()), java.util.Arrays.asList(pair.textures()));
            assertEquals(single.transforms(), pair.transforms());
            assertNotSame(single, pair);
        }
    }

    @Test
    void theAkimboGunsHaveClientDefinitionsThatAreAkimbo() {
        assertTrue(
            ClientGunDefinitions.byName("akimbo_pistols")
                .akimbo());
        assertTrue(
            ClientGunDefinitions.byName("akimbo_revolvers")
                .akimbo());
        assertFalse(ClientGunDefinitions.usesFallback("akimbo_pistols"));
        assertFalse(ClientGunDefinitions.usesFallback("akimbo_revolvers"));
        assertNull(ClientGunDefinitions.byName("akimbo_nothing"));
    }
}
