package com.gregguns.icbm.defense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DishSpinTest {

    @Test
    void aStillDishHoldsItsAngle() {
        DishSpin s = new DishSpin();
        assertEquals(0.0F, s.angle(1000L), 1e-4);
        assertFalse(s.spinning());
    }

    @Test
    void aSpinningDishTurnsAtAFixedRateFromTheTickItStarted() {
        DishSpin s = new DishSpin();
        assertTrue(s.set(true, 100L));
        assertEquals(0.0F, s.angle(100L), 1e-4);
        assertEquals(DishSpin.DEGREES_PER_TICK * 10, s.angle(110L), 1e-3);
    }

    @Test
    void theAngleWrapsAtAFullTurn() {
        DishSpin s = new DishSpin();
        s.set(true, 0L);
        long oneTurn = (long) (360.0F / DishSpin.DEGREES_PER_TICK);
        assertEquals(0.0F, s.angle(oneTurn), 1e-3);
        assertEquals(DishSpin.DEGREES_PER_TICK, s.angle(oneTurn + 1), 1e-3);
    }

    @Test
    void stoppingFreezesTheAngleWhereItWasAndStartingAgainContinuesFromThere() {
        DishSpin s = new DishSpin();
        s.set(true, 0L);
        assertTrue(s.set(false, 20L));
        float frozen = s.angle(20L);
        assertEquals(frozen, s.angle(500L), 1e-4, "still means still");
        assertTrue(s.set(true, 600L));
        assertEquals(frozen, s.angle(600L), 1e-3, "no jump when it starts again");
        assertEquals((frozen + DishSpin.DEGREES_PER_TICK * 5) % 360.0F, s.angle(605L), 1e-3);
    }

    @Test
    void settingTheStateItAlreadyHasChangesNothing() {
        DishSpin s = new DishSpin();
        s.set(true, 10L);
        assertFalse(s.set(true, 50L));
        assertEquals(10L, s.since());
    }

    @Test
    void aClientThatRestoresTheServersNumbersDrawsTheSameAngle() {
        DishSpin server = new DishSpin();
        server.set(true, 40L);
        server.set(false, 90L);
        server.set(true, 150L);
        DishSpin client = new DishSpin();
        client.restore(server.spinning(), server.baseAngle(), server.since());
        for (long t : new long[] { 150L, 151L, 400L, 100_000L }) {
            assertEquals(server.angle(t), client.angle(t), 1e-3);
        }
    }

    @Test
    void anAngleBeforeTheChangeStillComesOutInRange() {
        DishSpin s = new DishSpin();
        s.set(true, 100L);
        float a = s.angle(50L);
        assertTrue(a >= 0.0F && a < 360.0F);
    }
}
