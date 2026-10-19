package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChargeLogicTest {

    private static GunStats bio() {
        return GunStats.builder("bio_probe")
            .ammoType("bio")
            .magazine(30)
            .charge(30, ChargeMode.LEVELS, 0)
            .build();
    }

    private static GunStats hammer() {
        return GunStats.builder("hammer_probe")
            .ammoType("air")
            .magazine(300)
            .charge(20, ChargeMode.SCALED, 10)
            .build();
    }

    private static ChargeRelease hold(GunStats stats, int ticks, int available) {
        ChargeState state = new ChargeState();
        assertTrue(state.begin(true));
        for (int i = 0; i < ticks; i++) {
            state.tick(stats);
        }
        return state.release(stats, available);
    }

    @Test
    void fractionIsHeldOverFullCappedAtOne() {
        assertEquals(0.0, ChargeMath.fraction(0, 30), 1e-9);
        assertEquals(0.5, ChargeMath.fraction(15, 30), 1e-9);
        assertEquals(1.0, ChargeMath.fraction(99, 30), 1e-9);
        assertEquals(0.0, ChargeMath.fraction(-5, 30), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> ChargeMath.fraction(1, 0));
    }

    @Test
    void bioLevelsSplitAtThe45And95PercentThresholds() {
        assertEquals(1, ChargeMath.level(0.0));
        assertEquals(1, ChargeMath.level(0.45));
        assertEquals(2, ChargeMath.level(0.4501));
        assertEquals(2, ChargeMath.level(0.95));
        assertEquals(3, ChargeMath.level(0.9501));
        assertEquals(3, ChargeMath.level(1.0));
    }

    @Test
    void hammerUnitsAreFloorTenTimesChargeClampedToOneAndTen() {
        assertEquals(1, ChargeMath.scaledUnits(0.0, 10));
        assertEquals(1, ChargeMath.scaledUnits(0.09, 10));
        assertEquals(5, ChargeMath.scaledUnits(0.55, 10));
        assertEquals(9, ChargeMath.scaledUnits(0.99, 10));
        assertEquals(10, ChargeMath.scaledUnits(1.0, 10));
    }

    @Test
    void bioHoldTicksMapToLevelsOneTwoThree() {
        // 13 ticks is 0.433, 14 is 0.467, 28 is 0.933, 29 is 0.967.
        assertEquals(1, hold(bio(), 13, 30).units());
        assertEquals(2, hold(bio(), 14, 30).units());
        assertEquals(2, hold(bio(), 28, 30).units());
        assertEquals(3, hold(bio(), 29, 30).units());
        assertEquals(3, hold(bio(), 500, 30).units());
    }

    @Test
    void doesNotReproduceGUN02_aLowMagazineSpendsWhatIsLoadedAtThatLevel() {
        // Techguns: ammo 1 and level 3 gave level 2; ammo 2 and level 3 gave level 1.
        ChargeRelease one = hold(bio(), 40, 1);
        assertEquals(1, one.units());
        assertEquals(3, one.wantedUnits());
        assertTrue(one.limited());
        ChargeRelease two = hold(bio(), 40, 2);
        assertEquals(2, two.units());
        assertTrue(two.limited());
        assertFalse(hold(bio(), 40, 3).limited());
    }

    @Test
    void hammerSpendsScaledAirClampedToWhatIsLoaded() {
        assertEquals(10, hold(hammer(), 20, 300).units());
        assertEquals(5, hold(hammer(), 10, 300).units());
        assertEquals(1, hold(hammer(), 0, 300).units());
        assertEquals(4, hold(hammer(), 20, 4).units());
    }

    @Test
    void creativeAvailabilityNeverLimits() {
        assertEquals(3, hold(bio(), 40, Integer.MAX_VALUE).units());
    }

    @Test
    void releaseWithNothingChargingOrNothingLoadedFiresNothing() {
        ChargeState idle = new ChargeState();
        assertNull(idle.release(bio(), 30));
        ChargeState state = new ChargeState();
        state.begin(true);
        assertNull(state.release(bio(), 0));
        assertFalse(state.isCharging());
    }

    @Test
    void beginIsRefusedWhenNotAllowedOrAlreadyCharging() {
        ChargeState state = new ChargeState();
        assertFalse(state.begin(false));
        assertTrue(state.begin(true));
        state.tick(bio());
        assertFalse(state.begin(true));
        assertEquals(1, state.heldTicks());
    }

    @Test
    void holdTicksStopAtFullChargeAndCancelResets() {
        ChargeState state = new ChargeState();
        state.begin(true);
        for (int i = 0; i < 100; i++) {
            state.tick(bio());
        }
        assertEquals(30, state.heldTicks());
        assertEquals(1.0, state.fraction(bio()), 1e-9);
        state.cancel();
        assertFalse(state.isCharging());
        assertEquals(0.0, state.fraction(bio()), 1e-9);
    }

    @Test
    void aGunWithoutChargeSaysSo() {
        GunStats plain = GunStats.builder("plain_probe")
            .ammoType("x")
            .build();
        assertFalse(plain.isCharge());
        assertTrue(bio().isCharge());
        assertEquals(10, hammer().chargeMaxUnits());
        assertEquals(ChargeMath.LEVEL_COUNT, bio().chargeMaxUnits());
    }
}
