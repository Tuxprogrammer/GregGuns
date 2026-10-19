package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.Ak47Def;
import com.gregguns.gun.def.M4Def;
import com.gregguns.gun.def.Mac10Def;
import com.gregguns.gun.def.ThompsonDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.Event;
import com.gregguns.item.gun.GunItems;

/**
 * Unit tests of the MV batch B1 guns' behavior through the pure gun logic: full-auto cadence, the magazine reload
 * with its empty-magazine return, the forced-reload refund (GUN-13, and GUN-14 not repeated), and the M4's scope.
 */
// Spec: docs/techguns-spec/02-guns/sheets-handguns-smgs.md (Thompson, Mac10); sheets-rifles-shotguns.md (M4, AK47)
class MvGunsLogicTest {

    private static final int HELD_TICKS = 13;

    /** Shots fired by holding the trigger for {@link #HELD_TICKS} ticks. */
    private static int shotsHeld(GunStats stats) {
        GunController c = new GunController();
        GunState st = new GunState(stats.magazineSize());
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int shots = 0;
        for (int i = 0; i < HELD_TICKS; i++) {
            if (c.tick(stats, st, inv, true, false)
                .shot()
                .wasFired()) {
                shots++;
            }
        }
        return shots;
    }

    @Test
    void thompsonFiresEveryThreeTicksAndMac10EveryTwo() {
        assertEquals(FireMode.AUTO, ThompsonDef.STATS.fireMode());
        assertEquals(3, ThompsonDef.STATS.fireRateTicks());
        assertEquals(5, shotsHeld(ThompsonDef.STATS)); // ticks 0, 3, 6, 9, 12
        assertEquals(FireMode.AUTO, Mac10Def.STATS.fireMode());
        assertEquals(2, Mac10Def.STATS.fireRateTicks());
        assertEquals(7, shotsHeld(Mac10Def.STATS)); // ticks 0, 2, ... 12
    }

    @Test
    void m4AndAk47FireEveryThreeTicks() {
        assertEquals(5, shotsHeld(M4Def.STATS));
        assertEquals(5, shotsHeld(Ak47Def.STATS));
        assertEquals(FireMode.AUTO, M4Def.STATS.fireMode());
        assertEquals(FireMode.AUTO, Ak47Def.STATS.fireMode());
    }

    @Test
    void theSheetsFiguresAreKept() {
        // sheets-handguns-smgs.md and sheets-rifles-shotguns.md, section 3.
        assertEquals(20, ThompsonDef.STATS.magazineSize());
        assertEquals(32, Mac10Def.STATS.magazineSize());
        assertEquals(30, M4Def.STATS.magazineSize());
        assertEquals(30, Ak47Def.STATS.magazineSize());
        assertEquals(40, ThompsonDef.STATS.reloadTicks());
        assertEquals(40, Mac10Def.STATS.reloadTicks());
        assertEquals(45, M4Def.STATS.reloadTicks());
        assertEquals(45, Ak47Def.STATS.reloadTicks());
        assertEquals(5.0, ThompsonDef.STATS.damage());
        assertEquals(3.0, ThompsonDef.STATS.damageMin());
        assertEquals(8.0, M4Def.STATS.damage());
        assertEquals(6.0, M4Def.STATS.damageMin());
        assertEquals(9.0, Ak47Def.STATS.damage());
        assertEquals(5.0, Ak47Def.STATS.damageMin());
        assertEquals(0.0, ThompsonDef.STATS.penetration());
        assertEquals(0.0, Mac10Def.STATS.penetration());
        assertEquals(0.15, M4Def.STATS.penetration());
        assertEquals(0.15, Ak47Def.STATS.penetration());
        assertEquals(20, Ak47Def.STATS.dropStart());
        assertEquals(30, Ak47Def.STATS.dropEnd());
        assertEquals(25, M4Def.STATS.dropStart());
        assertEquals(35, M4Def.STATS.dropEnd());
        assertEquals(2, Mac10Def.STATS.recoilTicks());
        assertEquals(5, ThompsonDef.STATS.recoilTicks());
    }

    @Test
    void theSmgsShareOneMagazineButNotOneClip() {
        assertEquals(AmmoIds.SMG_MAGAZINE, ThompsonDef.STATS.ammoType());
        assertEquals(AmmoIds.SMG_MAGAZINE, Mac10Def.STATS.ammoType());
        assertEquals(AmmoIds.SMG_MAGAZINE_EMPTY, Mac10Def.STATS.emptyContainerType());
        assertEquals(AmmoIds.NINE_MM, Mac10Def.STATS.looseAmmoType());
        // A magazine is 2 bullets: the Thompson's 20 rounds are 10 per bullet, the Mac10's 32 are 16 per bullet.
        assertEquals(10, GunItems.roundsPerLooseItem(ThompsonDef.STATS));
        assertEquals(16, GunItems.roundsPerLooseItem(Mac10Def.STATS));
        assertEquals(AmmoIds.ASSAULT_RIFLE_MAGAZINE, M4Def.STATS.ammoType());
        assertEquals(AmmoIds.ASSAULT_RIFLE_MAGAZINE, Ak47Def.STATS.ammoType());
        assertEquals(10, GunItems.roundsPerLooseItem(M4Def.STATS));
        assertEquals(10, GunItems.roundsPerLooseItem(Ak47Def.STATS));
    }

    private static void checkMagazineReload(GunStats stats) {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(stats.ammoType(), 1);
        c.triggerPressed();
        assertEquals(
            Event.STARTED,
            c.tick(stats, st, inv, true, false)
                .reload());
        Event end = Event.NONE;
        for (int i = 0; i < stats.reloadTicks(); i++) {
            end = c.tick(stats, st, inv, true, false)
                .reload();
        }
        assertEquals(Event.COMPLETED, end);
        assertEquals(stats.magazineSize(), st.loaded());
        assertEquals(0, inv.items(stats.ammoType()));
        assertEquals(1, inv.items(stats.emptyContainerType()));
    }

    @Test
    void everyB1GunReloadsFromOneMagazineAndReturnsTheEmptyOne() {
        checkMagazineReload(ThompsonDef.STATS);
        checkMagazineReload(Mac10Def.STATS);
        checkMagazineReload(M4Def.STATS);
        checkMagazineReload(Ak47Def.STATS);
    }

    @Test
    void aForcedReloadHandsTheLoadedRoundsBackAsRoundsNotAWholeMagazine() {
        // GUN-13: the rounds still in the gun are refunded. GUN-14: the refund is per loose bullet item, derived from
        // the gun's own clip, so a Mac10 with 20 rounds left is worth 1 bullet (16 per item), not 2.
        GunStats stats = Mac10Def.STATS;
        GunController c = new GunController();
        GunState st = new GunState(20);
        MapAmmoInventory inv = new MapAmmoInventory().with(stats.ammoType(), 1);
        assertEquals(Event.STARTED, c.forceReload(stats, st, inv));
        for (int i = 0; i < stats.reloadTicks(); i++) {
            c.tick(stats, st, inv, true, false);
        }
        assertEquals(32, st.loaded());
        assertEquals(20, inv.rounds(AmmoIds.NINE_MM));
        assertEquals(1, 20 / GunItems.roundsPerLooseItem(stats));
        assertEquals(1, inv.items(AmmoIds.SMG_MAGAZINE_EMPTY));
    }

    @Test
    void onlyTheM4HasAScope() {
        assertTrue(M4Def.STATS.hasScope());
        assertFalse(ThompsonDef.STATS.hasScope());
        assertFalse(Mac10Def.STATS.hasScope());
        assertFalse(Ak47Def.STATS.hasScope());
        GunController c = new GunController();
        assertFalse(c.toggleZoom(Ak47Def.STATS), "right-click does nothing on a gun without a scope");
    }

    @Test
    void theM4ZoomsToThreeQuartersFovAndSpreadAndAReloadCancelsIt() {
        GunStats stats = M4Def.STATS;
        GunController c = new GunController();
        assertEquals(
            1.0,
            c.zoom()
                .fovMultiplier(stats));
        assertTrue(c.toggleZoom(stats));
        assertTrue(
            c.zoom()
                .isZoomed(stats));
        assertEquals(
            0.75,
            c.zoom()
                .fovMultiplier(stats),
            1e-9);
        double unzoomed = SpreadModel.mainSpread(stats, ShotContext.player(false, 0.0));
        double zoomed = SpreadModel.mainSpread(stats, ShotContext.player(true, 0.0));
        assertEquals(0.015, unzoomed, 1e-9);
        assertEquals(0.015 * 0.75, zoomed, 1e-9);
        assertNotEquals(unzoomed, zoomed);

        GunState st = new GunState(10);
        MapAmmoInventory inv = new MapAmmoInventory().with(stats.ammoType(), 1);
        assertEquals(Event.STARTED, c.forceReload(stats, st, inv));
        assertFalse(
            c.zoom()
                .isZoomed(stats),
            "starting a reload cancels zoom");
        assertFalse(c.toggleZoom(stats), "zoom is refused while reloading");
    }
}
