package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-12, GGS-02-13, GGS-02-14)
class EnergyGunLogicTest {

    /** A battery with a plain counter. */
    private static final class Battery implements ChargeSource {

        long eu;

        Battery(long eu) {
            this.eu = eu;
        }

        @Override
        public long stored() {
            return eu;
        }

        @Override
        public long drain(long want) {
            long taken = Math.min(eu, Math.max(0, want));
            eu -= taken;
            return taken;
        }
    }

    /** Ammo side of a shooter whose gun holds the given battery. */
    private static AmmoInventory shooter(ChargeSource charge) {
        return new AmmoInventory() {

            @Override
            public int count(String ammoType) {
                return 0;
            }

            @Override
            public int withdraw(String ammoType, int maxItems) {
                return 0;
            }

            @Override
            public void depositItems(String ammoType, int items) {}

            @Override
            public void depositRounds(String ammoType, int rounds) {}

            @Override
            public ChargeSource charge() {
                return charge;
            }
        };
    }

    private static final GunStats LASER = GunStats.builder("laser_test")
        .energy(1000, 45000)
        .fire(FireMode.AUTO, 5)
        .build();
    private static final GunStats AKIMBO = GunStats.builder("akimbo_test")
        .energy(1000, 10000)
        .akimbo(false)
        .fire(FireMode.SEMI, 4)
        .build();

    private static GunTickResult tick(GunController c, GunStats s, AmmoInventory a, boolean infinite) {
        return c.tick(s, GunState.full(s), a, true, infinite);
    }

    @Test
    void energyStatsDeriveShotsAndAmmoMarker() {
        assertTrue(LASER.isEnergy());
        assertEquals(1000, LASER.euPerShot());
        assertEquals(45000, LASER.capacityEu());
        assertEquals(45, LASER.magazineSize());
        assertEquals(GunStats.ENERGY_AMMO_TYPE, LASER.ammoType());
        assertFalse(GunCatalog.PISTOL.isEnergy());
        assertEquals(0, GunCatalog.PISTOL.euPerShot());
    }

    @Test
    void capacityMustCoverOneShot() {
        assertThrows(
            IllegalArgumentException.class,
            () -> GunStats.builder("bad")
                .energy(500, 100)
                .build());
    }

    @Test
    void aShotSpendsEuAndStopsWhenTheChargeCannotPayForOne() {
        GunController c = new GunController(new FireControl(0, 0));
        Battery b = new Battery(2500);
        AmmoInventory a = shooter(b);
        int fired = 0;
        for (int i = 0; i < 40; i++) {
            c.triggerPressed();
            if (tick(c, LASER, a, false).shot()
                .wasFired()) {
                fired++;
            }
        }
        assertEquals(2, fired);
        assertEquals(500, b.eu);
    }

    @Test
    void anEmptyBatteryClicksOnceAndNeverReloads() {
        GunController c = new GunController();
        AmmoInventory a = shooter(new Battery(999));
        c.triggerPressed();
        GunTickResult first = tick(c, LASER, a, false);
        assertEquals(
            ShotResult.Outcome.EMPTY,
            first.shot()
                .outcome());
        assertTrue(first.outOfAmmo());
        assertEquals(ReloadStateMachine.Event.NONE, first.reload());
        for (int i = 0; i < 20; i++) {
            GunTickResult r = tick(c, LASER, a, false);
            assertEquals(ReloadStateMachine.Event.NONE, r.reload());
            assertFalse(
                c.reload()
                    .isReloading());
        }
    }

    @Test
    void theReloadKeyDoesNothingOnAnEnergyGun() {
        GunController c = new GunController();
        AmmoInventory a = shooter(new Battery(5));
        assertEquals(ReloadStateMachine.Event.NONE, c.forceReload(LASER, GunState.full(LASER), a));
        assertFalse(
            c.reload()
                .isReloading());
    }

    @Test
    void creativeAndNpcShootersSpendNothing() {
        GunController c = new GunController(new FireControl(0, 0));
        Battery b = new Battery(1000);
        c.triggerPressed();
        assertTrue(
            tick(c, LASER, shooter(b), true).shot()
                .wasFired());
        assertEquals(1000, b.eu);

        GunController npc = new GunController(new FireControl(0, 0));
        npc.triggerPressed();
        assertTrue(
            tick(npc, LASER, AmmoInventory.INFINITE, false).shot()
                .wasFired());
        assertEquals(
            Long.MAX_VALUE,
            AmmoInventory.INFINITE.charge()
                .stored());
    }

    @Test
    void aPredictedShotRequestSpendsFromTheChargeToo() {
        GunController c = new GunController();
        Battery b = new Battery(1500);
        AmmoInventory a = shooter(b);
        GunState unused = GunState.full(LASER);
        assertTrue(
            c.requestShot(LASER, unused, a, false)
                .shot()
                .wasFired());
        assertEquals(500, b.eu);
        for (int i = 0; i < 10; i++) {
            c.fire()
                .tick();
        }
        GunTickResult dry = c.requestShot(LASER, unused, a, false);
        assertEquals(
            ShotResult.Outcome.EMPTY,
            dry.shot()
                .outcome());
        assertEquals(500, b.eu);
    }

    @Test
    void akimboHandsAlternateWithTheShotsLeft() {
        GunController c = new GunController(new FireControl(0, 0));
        AmmoInventory a = shooter(new Battery(10000));
        int previous = -1;
        for (int i = 0; i < 4; i++) {
            GunTickResult r;
            int guard = 0;
            do {
                c.triggerPressed();
                r = tick(c, AKIMBO, a, false);
            } while (!r.shot()
                .wasFired() && ++guard < 10);
            assertTrue(
                r.shot()
                    .wasFired());
            assertNotEquals(
                previous,
                r.shot()
                    .attackType());
            previous = r.shot()
                .attackType();
            c.triggerReleased();
        }
    }

    @Test
    void energyMathBasics() {
        assertEquals(2, EnergyMath.shotsLeft(2999, 1000));
        assertEquals(0, EnergyMath.shotsLeft(-5, 1000));
        assertEquals(0, EnergyMath.shotsLeft(10, 0));
        assertTrue(EnergyMath.canFire(1000, 1000));
        assertFalse(EnergyMath.canFire(999, 1000));
        assertEquals(0.25, EnergyMath.chargeFraction(25, 100), 1e-12);
        assertEquals(1.0, EnergyMath.chargeFraction(500, 100), 1e-12);
        assertEquals(0.0, EnergyMath.chargeFraction(1, 0), 1e-12);
        assertEquals(0.75, EnergyMath.barFraction(25, 100), 1e-12);
        assertEquals(33, EnergyMath.percent(1, 3));
        assertEquals(100, EnergyMath.percent(3, 3));
        assertEquals(142_222, EnergyMath.euPerShotFor(6_400_000, 45));
        assertEquals(1, EnergyMath.euPerShotFor(5, 100));
    }

    @Test
    void defaultCapacitiesFollowTheGtBatteryTable() {
        assertEquals(100_000L, EnergyMath.defaultCapacityEu(1));
        assertEquals(6_400_000L, EnergyMath.defaultCapacityEu(4));
        assertEquals(10_000_000_000L, EnergyMath.defaultCapacityEu(7));
        assertEquals(0L, EnergyMath.defaultCapacityEu(0));
        assertEquals(0L, EnergyMath.defaultCapacityEu(8));
        assertEquals(0L, EnergyMath.defaultCapacityEu(-1));
    }

    @Test
    void capacityResolvesFromDefaultScaleAndOverrides() {
        assertEquals(6_400_000L, EnergyCapacity.resolve("laser", 6_400_000L, 1000, 100, new String[0]));
        assertEquals(3_200_000L, EnergyCapacity.resolve("laser", 6_400_000L, 1000, 50, null));
        String[] lines = { "other=5", "junk", "laser = 8000000", "laser=notanumber" };
        assertEquals(8_000_000L, EnergyCapacity.resolve("laser", 6_400_000L, 1000, 50, lines));
        assertEquals(6_400_000L, EnergyCapacity.resolve("sonic", 6_400_000L, 1000, 100, lines));
        // An override below one shot is ignored; the default stands.
        assertEquals(6_400_000L, EnergyCapacity.resolve("laser", 6_400_000L, 1000, 100, new String[] { "laser=10" }));
        // A default below one shot is raised to one shot.
        assertEquals(1000L, EnergyCapacity.resolve("tiny", 10, 1000, 100, null));
    }

    @Test
    void chargeSourceConstants() {
        assertEquals(0, ChargeSource.NONE.stored());
        assertEquals(0, ChargeSource.NONE.drain(10));
        assertEquals(10, ChargeSource.INFINITE.drain(10));
        assertEquals(0, ChargeSource.INFINITE.drain(-3));
    }
}
