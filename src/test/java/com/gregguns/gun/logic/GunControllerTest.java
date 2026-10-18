package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.GunCatalogue.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.CancelReason;
import com.gregguns.gun.logic.ReloadStateMachine.Event;

class GunControllerTest {

    private static final GunStats PISTOL = GunCatalogue.PISTOL;
    private static final GunStats SCOPED = GunStats.builder("scoped")
        .ammoType("a")
        .magazine(10)
        .zoom(0.35, 0.5)
        .build();

    private static GunTickResult tick(GunController c, GunState st, AmmoInventory inv) {
        return c.tick(PISTOL, st, inv, true, false);
    }

    @Test
    void timelineOfTheSpecExample() {
        // Spec timeline for a semi-auto gun: the last round fires, the next click reloads, then it fires again.
        GunController c = new GunController();
        GunState st = new GunState(1);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        c.triggerPressed();
        assertTrue(
            tick(c, st, inv).shot()
                .wasFired());
        assertEquals(0, st.loaded());
        for (int i = 0; i < 3; i++) {
            tick(c, st, inv);
        }
        c.triggerPressed();
        GunTickResult r = tick(c, st, inv);
        assertEquals(Event.STARTED, r.reload());
        assertFalse(r.outOfAmmo());
        Event end = Event.NONE;
        for (int i = 0; i < 35; i++) {
            end = tick(c, st, inv).reload();
        }
        assertEquals(Event.COMPLETED, end);
        assertEquals(18, st.loaded());
    }

    @Test
    void outOfAmmoIsReportedOnceAndStaysQuiet() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        assertTrue(tick(c, st, inv).outOfAmmo());
        for (int i = 0; i < 40; i++) {
            assertFalse(tick(c, st, inv).outOfAmmo());
        }
    }

    @Test
    void switchingWeaponCancelsTheReloadAndZoom() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        c.triggerPressed();
        tick(c, st, inv);
        assertTrue(
            c.reload()
                .isReloading());
        GunTickResult r = c.tick(PISTOL, st, inv, false, false);
        assertEquals(Event.CANCELLED, r.reload());
        assertEquals(
            CancelReason.WEAPON_SWITCH,
            c.reload()
                .lastCancelReason());
        assertEquals(1, inv.items(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
        assertEquals(0, st.loaded());
    }

    @Test
    void firingInterruptsAForcedReloadAndKeepsTheRounds() {
        GunController c = new GunController();
        GunState st = new GunState(5);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        assertEquals(Event.STARTED, c.forceReload(PISTOL, st, inv));
        for (int i = 0; i < 10; i++) {
            tick(c, st, inv);
        }
        c.triggerPressed();
        GunTickResult r = tick(c, st, inv);
        assertEquals(Event.CANCELLED, r.reload());
        assertEquals(
            CancelReason.FIRE,
            c.reload()
                .lastCancelReason());
        assertTrue(
            r.shot()
                .wasFired());
        assertEquals(4, st.loaded());
        assertEquals(0, inv.rounds(AmmoIds.NINE_MM));
    }

    @Test
    void anEmptyGunCannotFireOutOfItsOwnReload() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        c.triggerPressed();
        tick(c, st, inv);
        c.triggerPressed();
        GunTickResult r = tick(c, st, inv);
        assertFalse(
            r.shot()
                .wasFired());
        assertTrue(
            c.reload()
                .isReloading());
    }

    @Test
    void forceReloadRespectsCooldownAndFullness() {
        GunController c = new GunController();
        GunState st = new GunState(10);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        c.triggerPressed();
        tick(c, st, inv);
        assertEquals(Event.REJECTED_BUSY, c.forceReload(PISTOL, st, inv));
        for (int i = 0; i < 4; i++) {
            tick(c, st, inv);
        }
        assertEquals(Event.STARTED, c.forceReload(PISTOL, st, inv));
        GunController other = new GunController();
        assertEquals(Event.REJECTED_FULL, other.forceReload(PISTOL, GunState.full(PISTOL), inv));
    }

    @Test
    void zoomIsServerStateAndResetByReloadAndSwitch() {
        GunController c = new GunController();
        GunState st = new GunState(5);
        MapAmmoInventory inv = new MapAmmoInventory().with("a", 1);
        assertTrue(c.toggleZoom(SCOPED));
        assertTrue(
            c.zoom()
                .isZoomed(SCOPED));
        assertEquals(Event.STARTED, c.forceReload(SCOPED, st, inv));
        assertFalse(
            c.zoom()
                .isZoomed(SCOPED));
        assertFalse(c.toggleZoom(SCOPED), "no zoom while reloading");

        GunController d = new GunController();
        d.toggleZoom(SCOPED);
        d.tick(SCOPED, st, inv, false, false);
        assertFalse(
            d.zoom()
                .isZoomed(SCOPED));
    }

    @Test
    void packetShotsAreServerChecked() {
        GunController c = new GunController();
        GunState st = new GunState(1);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        assertTrue(
            c.requestShot(PISTOL, st, inv, false)
                .shot()
                .wasFired());
        // The gun is now empty on the server whatever the client believes: the next request reloads instead.
        for (int i = 0; i < 4; i++) {
            c.fire()
                .tick();
        }
        GunTickResult r = c.requestShot(PISTOL, st, inv, false);
        assertFalse(
            r.shot()
                .wasFired());
        assertEquals(Event.STARTED, r.reload());
    }

    @Test
    void creativeNeverSpendsAmmo() {
        GunController c = new GunController();
        GunState st = new GunState(18);
        c.triggerPressed();
        assertTrue(
            c.tick(PISTOL, st, AmmoInventory.INFINITE, true, true)
                .shot()
                .wasFired());
        assertEquals(18, st.loaded());
    }

    @Test
    void resetDropsEverythingTransient() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        c.triggerPressed();
        tick(c, st, inv);
        c.reset();
        assertFalse(
            c.reload()
                .isReloading());
        assertEquals(1, inv.items(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
    }
}
