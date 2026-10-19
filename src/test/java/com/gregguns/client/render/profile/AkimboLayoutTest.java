package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void doubleShotAkimboRecoilsBothHandsWhateverTheAttackType() {
        for (int attackType = 0; attackType <= 1; attackType++) {
            assertTrue(AkimboLayout.animates(true, true, 0, attackType, true));
            assertTrue(AkimboLayout.animates(true, true, 1, attackType, true));
        }
        assertFalse(AkimboLayout.animates(true, true, 1, 0, false));
        assertFalse(AkimboLayout.animates(false, true, 1, 0, true));
    }

    @Test
    void theSingleDrawnGunTakesEveryShotInThirdPersonAndForSingleGuns() {
        assertTrue(AkimboLayout.animates(true, false, 0, 1));
        assertTrue(AkimboLayout.animates(true, false, 0, 0));
        assertTrue(AkimboLayout.animates(false, true, 0, 0));
    }

    @Test
    void theMainGunIsNotMovedAndTheSecondIsMirroredAboutTheScreenCentre() {
        double[] p = { 0.3, -0.2, 0.9 };
        assertArrayEquals(p, AkimboLayout.mirrored(p, 0), 1e-12);
        // The same point seen through the first-person camera: the left copy is at (-right, same up, same depth).
        double[] right = InHandFit.cameraOf(
            InHandFit.firstPersonItemFrame()
                .apply(p));
        double[] left = InHandFit.cameraOf(
            InHandFit.firstPersonItemFrame()
                .apply(AkimboLayout.mirrored(p, 1)));
        assertEquals(-right[0], left[0], 1e-9);
        assertEquals(right[1], left[1], 1e-9);
        assertEquals(right[2], left[2], 1e-9);
    }

    @Test
    void theGlCallsMatchTheMirrorFormula() {
        // translate(shift), rotY(-yaw), scale(-1, 1, 1), rotY(yaw): the calls GunItemRenderer issues
        double[] p = { 0.3, -0.2, 0.9 };
        double[] shift = AkimboLayout.mirrorTranslate(1);
        double[] viaOps = InHandFit.Affine.identity()
            .translate(shift[0], shift[1], shift[2])
            .rotY(-AkimboLayout.MIRROR_YAW)
            .scale(AkimboLayout.mirrorScaleX(1), 1.0, 1.0)
            .rotY(AkimboLayout.MIRROR_YAW)
            .apply(p);
        assertArrayEquals(AkimboLayout.mirrored(p, 1), viaOps, 1e-12);
    }

    @Test
    void mirroringTwiceReturnsTheOriginalPoint() {
        double[] p = { 0.77, 0.1, -0.4 };
        assertArrayEquals(p, AkimboLayout.mirrored(AkimboLayout.mirrored(p, 1), 1), 1e-12);
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
