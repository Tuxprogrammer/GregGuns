package com.gregguns.utility.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Parameter ranges, clamping, the hardness rule, and the fixed TNT radius (MUTL-33). */
class ChargeParamsTest {

    @Test
    void defaultsMatchTheParameterTable() {
        ChargeParams tnt = new ChargeParams(ChargeType.TNT);
        assertEquals(3, tnt.radius());
        assertEquals(3, tnt.length());
        assertEquals(100, tnt.fuse());
        ChargeParams adv = new ChargeParams(ChargeType.ADVANCED);
        assertEquals(5, adv.radius());
        assertEquals(10, adv.length());
        assertEquals(100, adv.fuse());
    }

    @Test
    void valuesClampAtTheirLimits() {
        ChargeParams adv = new ChargeParams(ChargeType.ADVANCED);
        for (int i = 0; i < 20; i++) {
            adv.press(ChargeParams.BTN_RADIUS_UP);
            adv.press(ChargeParams.BTN_LENGTH_UP);
            adv.press(ChargeParams.BTN_FUSE_UP);
        }
        assertEquals(7, adv.radius());
        assertEquals(10, adv.length());
        assertEquals(200, adv.fuse());
        for (int i = 0; i < 20; i++) {
            adv.press(ChargeParams.BTN_RADIUS_DOWN);
            adv.press(ChargeParams.BTN_LENGTH_DOWN);
            adv.press(ChargeParams.BTN_FUSE_DOWN);
        }
        assertEquals(3, adv.radius());
        assertEquals(2, adv.length());
        assertEquals(60, adv.fuse());
    }

    @Test
    void doesNotReproduceMUTL33_tntRadiusIsFixedAndItsButtonsAreHidden() {
        ChargeParams tnt = new ChargeParams(ChargeType.TNT);
        assertFalse(ChargeType.TNT.radiusAdjustable());
        assertTrue(ChargeType.ADVANCED.radiusAdjustable());
        assertFalse(tnt.press(ChargeParams.BTN_RADIUS_UP));
        assertFalse(tnt.press(ChargeParams.BTN_RADIUS_DOWN));
        assertEquals(3, tnt.radius());
    }

    @Test
    void anArmedChargeIgnoresEveryButtonButDisarm() {
        ChargeParams p = new ChargeParams(ChargeType.ADVANCED);
        assertTrue(p.press(ChargeParams.BTN_ARM));
        for (int b = 0; b < ChargeParams.BUTTON_COUNT; b++) {
            if (b != ChargeParams.BTN_DISARM) {
                assertFalse(p.press(b), "button " + b);
            }
        }
        assertEquals(5, p.radius());
        assertEquals(100, p.fuse());
    }

    @Test
    void disarmStopsTheFuseAndRestoresTheSetDelay() {
        ChargeParams p = new ChargeParams(ChargeType.ADVANCED);
        assertTrue(p.press(ChargeParams.BTN_FUSE_UP));
        assertEquals(120, p.fuseSet());
        assertTrue(p.press(ChargeParams.BTN_ARM));
        p.tickFuse();
        p.tickFuse();
        assertEquals(118, p.fuse());
        assertTrue(p.press(ChargeParams.BTN_DISARM));
        assertFalse(p.armed());
        assertEquals(120, p.fuse());
        assertTrue(p.press(ChargeParams.BTN_FUSE_DOWN), "settings work again after a disarm");
    }

    @Test
    void disarmOnAnUnarmedChargeChangesNothing() {
        ChargeParams p = new ChargeParams(ChargeType.TNT);
        assertFalse(p.press(ChargeParams.BTN_DISARM));
        assertFalse(p.armed());
    }

    @Test
    void depthAndFuseStayInTheirRangesAtTheEnds() {
        ChargeParams p = new ChargeParams(ChargeType.ADVANCED);
        for (int i = 0; i < 40; i++) {
            p.press(ChargeParams.BTN_LENGTH_UP);
            p.press(ChargeParams.BTN_FUSE_UP);
        }
        assertEquals(ChargeType.ADVANCED.lengthMax(), p.length());
        assertEquals(ChargeType.FUSE_MAX, p.fuse());
        assertFalse(p.press(ChargeParams.BTN_LENGTH_UP));
        for (int i = 0; i < 40; i++) {
            p.press(ChargeParams.BTN_LENGTH_DOWN);
            p.press(ChargeParams.BTN_FUSE_DOWN);
        }
        assertEquals(ChargeType.ADVANCED.lengthMin(), p.length());
        assertEquals(ChargeType.FUSE_MIN, p.fuse());
        assertFalse(p.press(ChargeParams.BTN_FUSE_DOWN));
    }

    @Test
    void anArmedSaveKeepsItsSetDelay() {
        ChargeParams p = new ChargeParams(ChargeType.TNT);
        p.restore(3, 3, 40, 160, true);
        assertEquals(40, p.fuse());
        assertEquals(160, p.fuseSet());
        assertTrue(p.press(ChargeParams.BTN_DISARM));
        assertEquals(160, p.fuse());
    }

    @Test
    void unknownButtonIdsChangeNothing() {
        ChargeParams p = new ChargeParams(ChargeType.ADVANCED);
        assertFalse(p.press(-1));
        assertFalse(p.press(ChargeParams.BUTTON_COUNT));
        assertFalse(p.press(127));
        assertFalse(p.armed());
    }

    @Test
    void restoreClampsAHandEditedSave() {
        ChargeParams p = new ChargeParams(ChargeType.TNT);
        p.restore(99, 99, 99999, true);
        assertEquals(3, p.radius());
        assertEquals(5, p.length());
        assertEquals(200, p.fuse());
        assertTrue(p.armed());
        p.restore(-4, -4, -4, false);
        assertEquals(2, p.length());
        assertEquals(0, p.fuse());
    }

    @Test
    void hardnessCapsAreStrictForTntAndInclusiveForTheAdvancedCharge() {
        // GGS-09-14: obsidian (50) is out of reach of the TNT charge but not of C4.
        assertTrue(ChargeType.TNT.canBreak(29.9F, 30F, 100F));
        assertFalse(ChargeType.TNT.canBreak(30F, 30F, 100F));
        assertFalse(ChargeType.TNT.canBreak(50F, 30F, 100F));
        assertTrue(ChargeType.ADVANCED.canBreak(50F, 30F, 100F));
        assertTrue(ChargeType.ADVANCED.canBreak(100F, 30F, 100F));
        assertFalse(ChargeType.ADVANCED.canBreak(100.1F, 30F, 100F));
    }

    @Test
    void negativeHardnessIsNeverBrokenWhateverTheCaps() {
        assertFalse(ChargeType.TNT.canBreak(-1F, Float.MAX_VALUE, Float.MAX_VALUE));
        assertFalse(ChargeType.ADVANCED.canBreak(-1F, Float.MAX_VALUE, Float.MAX_VALUE));
        assertFalse(ChargeType.ADVANCED.canBreak(Float.NaN, 30F, 100F));
        assertTrue(ChargeType.TNT.canBreak(0F, 30F, 100F));
    }

    @Test
    void aCorruptMetaReadsAsTnt() {
        assertEquals(ChargeType.TNT, ChargeType.fromMeta(7));
        assertEquals(ChargeType.ADVANCED, ChargeType.fromMeta(1));
    }
}
