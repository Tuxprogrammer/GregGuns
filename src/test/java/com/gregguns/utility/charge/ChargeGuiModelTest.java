package com.gregguns.utility.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The pure rules behind the charge screen: labels, the fuse bar, which buttons look live, and the direction name. */
class ChargeGuiModelTest {

    private static ChargeGuiModel.State state(ChargeType type, boolean armed, boolean canEdit) {
        ChargeParams p = new ChargeParams(type);
        if (armed) {
            p.press(ChargeParams.BTN_ARM);
        }
        ChargeGuiModel.State s = new ChargeGuiModel.State();
        s.load(p, canEdit);
        return s;
    }

    @Test
    void secondsRoundUpToOneDecimal() {
        assertEquals("5.0", ChargeGuiModel.seconds(100));
        assertEquals("0.1", ChargeGuiModel.seconds(1));
        assertEquals("0.0", ChargeGuiModel.seconds(0));
        assertEquals("0.0", ChargeGuiModel.seconds(-5));
        assertEquals("3.0", ChargeGuiModel.seconds(60));
        assertEquals("4.5", ChargeGuiModel.seconds(89));
    }

    @Test
    void fuseFractionIsClamped() {
        assertEquals(1.0F, ChargeGuiModel.fuseFraction(100, 100));
        assertEquals(0.5F, ChargeGuiModel.fuseFraction(50, 100));
        assertEquals(0.0F, ChargeGuiModel.fuseFraction(-3, 100));
        assertEquals(1.0F, ChargeGuiModel.fuseFraction(500, 100));
        assertEquals(0.0F, ChargeGuiModel.fuseFraction(10, 0));
    }

    @Test
    void rangeFractionHandlesASingleValueRange() {
        assertEquals(1.0F, ChargeGuiModel.rangeFraction(3, 3, 3));
        assertEquals(0.0F, ChargeGuiModel.rangeFraction(60, 60, 200));
        assertEquals(1.0F, ChargeGuiModel.rangeFraction(200, 60, 200));
        assertEquals(0.0F, ChargeGuiModel.rangeFraction(1, 60, 200));
    }

    @Test
    void directionNamesFollowTheBlastVector() {
        // Orientation 0 is a charge on a ceiling: it bores up. 1 is a floor: down.
        assertEquals("up", ChargeGuiModel.directionName(0));
        assertEquals("down", ChargeGuiModel.directionName(1));
        assertEquals("south", ChargeGuiModel.directionName(2));
        assertEquals("north", ChargeGuiModel.directionName(3));
        assertEquals("east", ChargeGuiModel.directionName(4));
        assertEquals("west", ChargeGuiModel.directionName(5));
        assertEquals("down", ChargeGuiModel.directionName(99), "a corrupt orientation reads as a floor charge");
        for (int o = 0; o < 6; o++) {
            int[] d = ChargeGeometry.direction(o);
            String n = ChargeGuiModel.directionName(o);
            boolean ok = d[1] == 1 && n.equals("up") || d[1] == -1 && n.equals("down")
                || d[2] == 1 && n.equals("south")
                || d[2] == -1 && n.equals("north")
                || d[0] == 1 && n.equals("east")
                || d[0] == -1 && n.equals("west");
            assertTrue(ok, "orientation " + o);
        }
    }

    @Test
    void aNonOwnerSeesNoLiveButton() {
        for (ChargeType type : ChargeType.values()) {
            for (int armedFlag = 0; armedFlag < 2; armedFlag++) {
                ChargeGuiModel.State s = state(type, armedFlag == 1, false);
                for (int b = 0; b < ChargeParams.BUTTON_COUNT; b++) {
                    assertFalse(ChargeGuiModel.enabled(s, type, b), type + " button " + b);
                }
                assertEquals(ChargeGuiModel.Access.VIEW_ONLY, ChargeGuiModel.access(false, armedFlag == 1));
            }
        }
    }

    @Test
    void anArmedChargeOnlyOffersDisarmToItsOwner() {
        ChargeGuiModel.State s = state(ChargeType.ADVANCED, true, true);
        for (int b = 0; b < ChargeParams.BUTTON_COUNT; b++) {
            assertEquals(
                b == ChargeParams.BTN_DISARM,
                ChargeGuiModel.enabled(s, ChargeType.ADVANCED, b),
                "button " + b);
        }
        assertEquals(ChargeGuiModel.Access.FROZEN_ARMED, ChargeGuiModel.access(true, true));
    }

    @Test
    void anUnarmedChargeGreysTheButtonsAtTheirLimits() {
        ChargeGuiModel.State s = state(ChargeType.ADVANCED, false, true);
        assertTrue(ChargeGuiModel.enabled(s, ChargeType.ADVANCED, ChargeParams.BTN_ARM));
        assertFalse(ChargeGuiModel.enabled(s, ChargeType.ADVANCED, ChargeParams.BTN_DISARM));
        s.length = ChargeType.ADVANCED.lengthMax();
        assertFalse(ChargeGuiModel.enabled(s, ChargeType.ADVANCED, ChargeParams.BTN_LENGTH_UP));
        assertTrue(ChargeGuiModel.enabled(s, ChargeType.ADVANCED, ChargeParams.BTN_LENGTH_DOWN));
        s.fuse = ChargeType.FUSE_MIN;
        assertFalse(ChargeGuiModel.enabled(s, ChargeType.ADVANCED, ChargeParams.BTN_FUSE_DOWN));
        assertTrue(ChargeGuiModel.enabled(s, ChargeType.ADVANCED, ChargeParams.BTN_FUSE_UP));
    }

    @Test
    void theTntRadiusButtonsAreNeverLive() {
        ChargeGuiModel.State s = state(ChargeType.TNT, false, true);
        assertFalse(ChargeGuiModel.enabled(s, ChargeType.TNT, ChargeParams.BTN_RADIUS_UP));
        assertFalse(ChargeGuiModel.enabled(s, ChargeType.TNT, ChargeParams.BTN_RADIUS_DOWN));
        assertEquals(ChargeGuiModel.Access.EDITABLE, ChargeGuiModel.access(true, false));
    }
}
