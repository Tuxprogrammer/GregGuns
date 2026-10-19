package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.PistolDef;
import com.gregguns.gun.def.SawedOffDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;

class TurretAmmoTest {

    private static final class Slots implements TurretAmmo.Store {

        final Map<String, Integer> ammo = new HashMap<>();
        final List<String> empties = new ArrayList<>();

        Slots with(String id, int n) {
            ammo.put(id, n);
            return this;
        }

        @Override
        public int count(String ammoId) {
            return ammo.getOrDefault(ammoId, 0);
        }

        @Override
        public int take(String ammoId, int max) {
            int n = Math.min(max, count(ammoId));
            ammo.put(ammoId, count(ammoId) - n);
            return n;
        }

        @Override
        public void putEmpty(String emptyId, int count) {
            for (int i = 0; i < count; i++) {
                empties.add(emptyId);
            }
        }
    }

    private static final GunStats PISTOL = PistolDef.STATS;

    @Test
    void aLoadedGunJustSpendsARoundAndTouchesNoSlot() {
        Slots slots = new Slots().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 3);
        TurretAmmo.Result r = TurretAmmo.consume(PISTOL, 5, slots);
        assertTrue(r.fired());
        assertEquals(4, r.loaded());
        assertEquals(3, slots.count(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
        assertTrue(slots.empties.isEmpty());
    }

    @Test
    void anEmptyGunReloadsInstantlyAndReturnsTheEmptyMagazine() {
        Slots slots = new Slots().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 2);
        TurretAmmo.Result r = TurretAmmo.consume(PISTOL, 0, slots);
        assertTrue(r.fired());
        assertEquals(PISTOL.magazineSize() - PISTOL.ammoPerShot(), r.loaded());
        assertEquals(1, slots.count(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
        assertEquals(1, slots.empties.size());
        assertEquals(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY, slots.empties.get(0));
    }

    @Test
    void noAmmoMeansNoShotAndNoEmpties() {
        Slots slots = new Slots();
        TurretAmmo.Result r = TurretAmmo.consume(PISTOL, 0, slots);
        assertFalse(r.fired());
        assertEquals(0, r.loaded());
        assertTrue(slots.empties.isEmpty());
    }

    @Test
    void doesNotReproduceMUTL24_aZeroOverflowIsNeverDropped() {
        // Techguns spawned a stack-size-0 item entity after every reload, even with room in the output slots.
        assertFalse(TurretAmmo.shouldDrop(0));
        assertFalse(TurretAmmo.shouldDrop(-1));
        assertTrue(TurretAmmo.shouldDrop(1));
    }

    @Test
    void aReloadWithoutAnEmptyContainerReturnsNothing() {
        // The Sawed Off is incremental: shells, no container.
        GunStats sawed = SawedOffDef.STATS;
        Slots slots = new Slots().with(AmmoIds.SHOTGUN_SHELL, 5);
        TurretAmmo.Result r = TurretAmmo.consume(sawed, 0, slots);
        assertTrue(r.fired());
        assertTrue(slots.empties.isEmpty());
    }

    @Test
    void anIncrementalGunTakesUpToItsReloadAndIsNotToppedUpMidMagazine() {
        GunStats sawed = SawedOffDef.STATS;
        Slots one = new Slots().with(AmmoIds.SHOTGUN_SHELL, 1);
        // One shell only: the gun loads one round and fires it.
        TurretAmmo.Result r = TurretAmmo.consume(sawed, 0, one);
        assertTrue(r.fired());
        assertEquals(0, r.loaded());
        assertEquals(0, one.count(AmmoIds.SHOTGUN_SHELL));
        // A gun that still holds a round fires it without touching the slots.
        Slots more = new Slots().with(AmmoIds.SHOTGUN_SHELL, 4);
        TurretAmmo.Result second = TurretAmmo.consume(sawed, 1, more);
        assertEquals(0, second.loaded());
        assertEquals(4, more.count(AmmoIds.SHOTGUN_SHELL));
    }

    @Test
    void aReloadNeverLoadsMoreThanTheMagazineHolds() {
        Slots slots = new Slots().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 64);
        TurretAmmo.Result r = TurretAmmo.consume(PISTOL, 0, slots);
        assertTrue(r.loaded() <= PISTOL.magazineSize());
    }
}
