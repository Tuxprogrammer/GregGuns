package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.turret.TurretLogic.Result;

class TurretLogicTest {

    /** A bank of EU that records what was paid. */
    private static final class Bank implements TurretLogic.Power {

        long eu;
        long spent;

        Bank(long eu) {
            this.eu = eu;
        }

        @Override
        public boolean pay(long cost) {
            if (cost <= 0) {
                return true;
            }
            if (eu < cost) {
                return false;
            }
            eu -= cost;
            spent += cost;
            return true;
        }
    }

    private static TurretLogic logic() {
        // The defaults of UtilityConfig: 4 EU/t, 120 EU per HP, 10 EU/t for 200 ticks.
        return new TurretLogic(4, 120, 10, 200);
    }

    @Test
    void guardingPaysTheUpkeepEveryTick() {
        TurretLogic logic = logic();
        Bank bank = new Bank(1000);
        for (int i = 0; i < 10; i++) {
            logic.tick(bank, true, true, false);
        }
        assertEquals(40, bank.spent);
    }

    @Test
    void anUnpoweredTurretSleepsAndWakesWhenPowerReturns() {
        TurretLogic logic = logic();
        Result sleep = logic.tick(new Bank(0), true, true, false);
        assertTrue(sleep.deactivate);
        assertFalse(logic.isActive());
        Result wake = logic.tick(new Bank(100), true, true, false);
        assertTrue(wake.activate);
        assertTrue(logic.isActive());
    }

    @Test
    void healingCostsEuPerHpAndWaitsSixtyTicks() {
        TurretLogic logic = logic();
        Bank bank = new Bank(100000);
        int heals = 0;
        for (int i = 0; i < 180; i++) {
            if (logic.tick(bank, true, true, true).heal) {
                heals++;
            }
        }
        // The cooldown starts at 0: heals at ticks 0, 61 and 122 (a heal, then a cooldown of 60 ticks).
        assertEquals(3, heals);
        assertEquals(180 * 4 + 3 * 120, bank.spent);
    }

    @Test
    void aFullyHealthyTurretNeverHeals() {
        TurretLogic logic = logic();
        Bank bank = new Bank(100000);
        for (int i = 0; i < 200; i++) {
            assertFalse(logic.tick(bank, true, true, false).heal);
        }
    }

    @Test
    void healingNeedsTheWholeCostInTheBuffer() {
        TurretLogic logic = logic();
        // Enough for the upkeep but not for 120 EU of healing.
        Result r = logic.tick(new Bank(50), true, true, true);
        assertFalse(r.heal);
    }

    @Test
    void deathStartsATwoHundredTickRepairThatRespawnsTheTurret() {
        TurretLogic logic = logic();
        logic.onTurretDeath();
        assertTrue(logic.isDead());
        Bank bank = new Bank(100000);
        int ticks = 0;
        boolean respawned = false;
        while (!respawned && ticks < 1000) {
            respawned = logic.tick(bank, true, false, false).respawn;
            ticks++;
        }
        assertEquals(200, ticks);
        assertEquals(200 * 10, bank.spent);
        assertFalse(logic.isDead());
        assertEquals(10.0F, TurretLogic.RESPAWN_HEALTH);
    }

    @Test
    void theRepairStallsWithoutPowerAndWhileRedstoneForbidsIt() {
        TurretLogic logic = logic();
        logic.onTurretDeath();
        for (int i = 0; i < 50; i++) {
            logic.tick(new Bank(0), true, false, false);
            logic.tick(new Bank(1000), false, false, false);
        }
        assertEquals(200, logic.repairLeft());
    }

    @Test
    void aDisabledTurretDeactivatesOnce() {
        TurretLogic logic = logic();
        assertTrue(logic.tick(new Bank(1000), false, true, false).deactivate);
        assertFalse(logic.tick(new Bank(1000), false, true, false).deactivate);
    }

    @Test
    void withNoTurretTheBaseDoesNothingAndPaysNothing() {
        TurretLogic logic = logic();
        Bank bank = new Bank(1000);
        Result r = logic.tick(bank, true, false, true);
        assertFalse(r.heal || r.respawn || r.activate || r.deactivate);
        assertEquals(0, bank.spent);
    }

    @Test
    void restoreRoundTripsTheSavedState() {
        TurretLogic logic = logic();
        logic.restore(true, 77, 12, false);
        assertTrue(logic.isDead());
        assertEquals(77, logic.repairLeft());
        assertEquals(12, logic.healCooldown());
        assertFalse(logic.isActive());
    }

    @Test
    void aTurretFiresGunsUpToItsOwnTierOnly() {
        // GGS-08-50: an MV turret (index 2) takes an LV gun (1) and an MV gun (2), not an HV gun (3).
        assertTrue(TurretLogic.gunAllowed(1, 2));
        assertTrue(TurretLogic.gunAllowed(2, 2));
        assertFalse(TurretLogic.gunAllowed(3, 2));
    }
}
