package com.gregguns.armor.slots.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The pure rules behind the special-slot equipment. */
class SlotLogicTest {

    @Test
    void slotTypesAreTheNamesTheBaublesApiUses() {
        // GGS-05-02: head, wings and quiver, nothing new (design 0009 decision 2).
        assertArrayEquals(new String[] { "head" }, BaubleSlot.HEAD.types());
        assertArrayEquals(new String[] { "wings" }, BaubleSlot.WINGS.types());
        assertArrayEquals(new String[] { "quiver" }, BaubleSlot.QUIVER.types());
        assertEquals(3, BaubleSlot.values().length);
    }

    @Test
    void typesReturnsAFreshArray() {
        String[] a = BaubleSlot.HEAD.types();
        a[0] = "changed";
        assertEquals("head", BaubleSlot.HEAD.types()[0]);
    }

    @Test
    void powerDrainsOnlyWhileOnAndCharged() {
        assertTrue(PowerUse.works(true, 2.0, 2.0));
        assertEquals(2.0, PowerUse.drain(true, 100.0, 2.0));
        assertFalse(PowerUse.works(false, 100.0, 2.0), "switched off");
        assertEquals(0.0, PowerUse.drain(false, 100.0, 2.0));
        assertFalse(PowerUse.works(true, 1.0, 2.0), "not enough for a whole tick");
        assertFalse(PowerUse.works(true, 0.0, 0.0), "no charge at all is dead even when free");
        assertEquals(0.0, PowerUse.drain(true, 1.0, 2.0));
    }

    @Test
    void powerFractionIsClamped() {
        assertEquals(0.5, PowerUse.fraction(50, 100));
        assertEquals(1.0, PowerUse.fraction(500, 100));
        assertEquals(0.0, PowerUse.fraction(-5, 100));
        assertEquals(0.0, PowerUse.fraction(5, 0));
    }

    @Test
    void aStackThatNeverStoredTheSwitchIsOn() {
        assertTrue(PowerUse.switchState(false, false));
        assertTrue(PowerUse.switchState(true, true));
        assertFalse(PowerUse.switchState(false, true));
    }

    @Test
    void gasMaskCuresPoisonThenWitherAndWearsOnePerEffect() {
        FilterWear.Result r = FilterWear.tick(0, 300, true, true);
        assertTrue(r.curePoison());
        assertTrue(r.cureWither());
        assertEquals(2, r.damage());
        FilterWear.Result none = FilterWear.tick(5, 300, false, false);
        assertEquals(5, none.damage());
        assertFalse(none.curePoison() || none.cureWither());
    }

    @Test
    void gasMaskStopsCuringWhenTheFilterIsSpent() {
        // GGS-05-13: no cure without filter charge. One point is always left, so 299 is spent.
        assertTrue(FilterWear.spent(299, 300));
        assertFalse(FilterWear.spent(298, 300));
        FilterWear.Result r = FilterWear.tick(299, 300, true, true);
        assertFalse(r.curePoison());
        assertFalse(r.cureWither());
        assertEquals(299, r.damage(), "a spent filter never wears further");
    }

    @Test
    void lastFilterTickGoesToPoisonWhenBothAreActive() {
        FilterWear.Result r = FilterWear.tick(297, 300, true, true);
        assertTrue(r.curePoison());
        assertTrue(r.cureWither());
        assertEquals(299, r.damage());
        FilterWear.Result r2 = FilterWear.tick(298, 300, true, true);
        assertTrue(r2.curePoison());
        assertFalse(r2.cureWither());
        assertEquals(299, r2.damage());
    }

    @Test
    void fallbackTanksNeedOxygenGearAndLowAirAndCharge() {
        assertTrue(AirTopUp.applies(280, 0, 2000, true));
        assertFalse(AirTopUp.applies(281, 0, 2000, true), "air above the threshold");
        assertFalse(AirTopUp.applies(100, 0, 2000, false), "no oxygen gear: tanks do nothing");
        assertFalse(AirTopUp.applies(100, 2000, 2000, true), "empty tanks");
        assertEquals(120, AirTopUp.topUp(100));
        assertEquals(300, AirTopUp.topUp(290), "never above a full meter");
    }

    @Test
    void refilterNeedsExactlyOneMaskAndOneFilter() {
        // GGS-05-37
        assertTrue(RefilterRule.matches(1, 1, 0));
        assertFalse(RefilterRule.matches(0, 1, 0), "no mask: never makes a mask");
        assertFalse(RefilterRule.matches(1, 0, 0));
        assertFalse(RefilterRule.matches(2, 1, 0));
        assertFalse(RefilterRule.matches(1, 2, 0));
        assertFalse(RefilterRule.matches(1, 1, 1));
    }

    @Test
    void helmetsNeverClaimTanks() {
        // GGS-05-25, GGS-05-26
        assertTrue(OxygenGearRules.handles(OxygenGearRules.Component.HELMET));
        assertTrue(OxygenGearRules.handles(OxygenGearRules.Component.GEAR));
        assertFalse(OxygenGearRules.handles(OxygenGearRules.Component.TANK1));
        assertFalse(OxygenGearRules.handles(OxygenGearRules.Component.TANK2));
    }

    @Test
    void breathingNeedsPowerAndAHandledComponent() {
        assertTrue(OxygenGearRules.canBreathe(OxygenGearRules.Component.HELMET, true));
        assertFalse(OxygenGearRules.canBreathe(OxygenGearRules.Component.HELMET, false));
        assertFalse(OxygenGearRules.canBreathe(OxygenGearRules.Component.TANK1, true));
    }

    @Test
    void componentNamesMatchGalacticraftEnum() {
        assertEquals(OxygenGearRules.Component.GEAR, OxygenGearRules.fromName("GEAR"));
        assertThrows(IllegalArgumentException.class, () -> OxygenGearRules.fromName("VISOR"));
    }
}
