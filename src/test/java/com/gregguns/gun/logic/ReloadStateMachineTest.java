package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.CancelReason;
import com.gregguns.gun.logic.ReloadStateMachine.Event;

class ReloadStateMachineTest {

    private static final GunStats PISTOL = GunCatalog.PISTOL;

    /** A revolver-style gun: loads loose bullets, no magazine item, six rounds per item. */
    private static final GunStats REVOLVER = GunStats.builder("revolver")
        .ammoType("9mm")
        .magazine(6)
        .reloadTicks(20)
        .build();

    private static final GunStats SHOTGUN = GunStats.builder("shotgun")
        .ammoType("shell")
        .magazine(8)
        .reloadMode(ReloadMode.INCREMENTAL, 8)
        .reloadTicks(30)
        .build();

    private static Event runToEnd(ReloadStateMachine m, GunStats s, GunState st, AmmoInventory inv, int ticks) {
        Event last = Event.NONE;
        for (int i = 0; i < ticks; i++) {
            last = m.tick(s, st, inv);
        }
        return last;
    }

    @Test
    void emptyPistolReloadsAfterExactlyReloadTicks() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 2);
        assertEquals(Event.STARTED, m.start(PISTOL, st, inv));
        assertTrue(m.isReloading());
        assertEquals(Event.NONE, runToEnd(m, PISTOL, st, inv, 34));
        assertEquals(0, st.loaded());
        assertEquals(Event.COMPLETED, m.tick(PISTOL, st, inv));
        assertEquals(18, st.loaded());
        assertEquals(1, inv.items(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
        assertEquals(1, inv.items(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY));
        assertEquals(0, inv.rounds(AmmoIds.NINE_MM));
        assertFalse(m.isReloading());
    }

    @Test
    void startMovesNothing() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        m.start(PISTOL, st, inv);
        assertEquals(1, inv.items(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
        assertEquals(0, st.loaded());
    }

    @Test
    void rejectsWhenFullWithoutAmmoOrBusy() {
        ReloadStateMachine m = new ReloadStateMachine();
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        assertEquals(Event.REJECTED_FULL, m.start(PISTOL, GunState.full(PISTOL), inv));
        assertEquals(Event.REJECTED_NO_AMMO, m.start(PISTOL, new GunState(3), new MapAmmoInventory()));
        assertEquals(Event.STARTED, m.start(PISTOL, new GunState(3), inv));
        assertEquals(Event.REJECTED_BUSY, m.start(PISTOL, new GunState(3), inv));
    }

    @Test
    void doesNotReproduceGUN13_forcedReloadKeepsTheLoadedRoundsOfAMagazineLessGun() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(3);
        MapAmmoInventory inv = new MapAmmoInventory().with("9mm", 1);
        assertEquals(Event.STARTED, m.start(REVOLVER, st, inv));
        assertTrue(m.isForced());
        runToEnd(m, REVOLVER, st, inv, 20);
        assertEquals(6, st.loaded());
        assertEquals(3, inv.rounds("9mm"), "the three rounds that were loaded come back");
    }

    @Test
    void doesNotReproduceGUN13_forcedPistolReloadReturnsMagazineAndExactRounds() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(7); // not a multiple of any bullets-per-item: no rounding loss
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        m.start(PISTOL, st, inv);
        runToEnd(m, PISTOL, st, inv, 35);
        assertEquals(18, st.loaded());
        assertEquals(7, inv.rounds(AmmoIds.NINE_MM));
        assertEquals(1, inv.items(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY));
    }

    @Test
    void cancelingLeavesGunAndInventoryUntouched() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(4);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        m.start(PISTOL, st, inv);
        runToEnd(m, PISTOL, st, inv, 10);
        assertEquals(Event.CANCELED, m.cancel(CancelReason.WEAPON_SWITCH));
        assertEquals(CancelReason.WEAPON_SWITCH, m.lastCancelReason());
        assertEquals(4, st.loaded());
        assertEquals(1, inv.items(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
        assertEquals(0, inv.rounds(AmmoIds.NINE_MM));
        assertEquals(Event.NONE, m.tick(PISTOL, st, inv));
        assertEquals(Event.NONE, m.cancel(CancelReason.FIRE));
    }

    @Test
    void ammoVanishingBeforeTheEndCancelsWithoutLoss() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(5);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        m.start(PISTOL, st, inv);
        inv.items.put(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 0); // the player dropped it
        assertEquals(Event.CANCELED, runToEnd(m, PISTOL, st, inv, 35));
        assertEquals(CancelReason.NO_AMMO, m.lastCancelReason());
        assertEquals(5, st.loaded());
        assertEquals(0, inv.rounds(AmmoIds.NINE_MM));
    }

    @Test
    void incrementalReloadTopsUpAndKeepsLoadedRounds() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(5);
        MapAmmoInventory inv = new MapAmmoInventory().with("shell", 2);
        m.start(SHOTGUN, st, inv);
        runToEnd(m, SHOTGUN, st, inv, 30);
        assertEquals(7, st.loaded());
        assertEquals(0, inv.items("shell"));
        assertEquals(0, inv.rounds("shell"));
    }

    @Test
    void incrementalReloadTakesOnlyWhatFits() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(6);
        MapAmmoInventory inv = new MapAmmoInventory().with("shell", 20);
        m.start(SHOTGUN, st, inv);
        runToEnd(m, SHOTGUN, st, inv, 30);
        assertEquals(8, st.loaded());
        assertEquals(18, inv.items("shell"));
    }

    @Test
    void infiniteSourceNeverRunsOutAndAcceptsEverything() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(0);
        assertEquals(Event.STARTED, m.start(PISTOL, st, AmmoInventory.INFINITE));
        runToEnd(m, PISTOL, st, AmmoInventory.INFINITE, 35);
        assertEquals(18, st.loaded());
    }

    @Test
    void progressRunsFromZeroToOne() {
        ReloadStateMachine m = new ReloadStateMachine();
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        GunState st = new GunState(0);
        assertEquals(0.0, m.progress());
        m.start(PISTOL, st, inv);
        assertEquals(0.0, m.progress(), 1e-12);
        runToEnd(m, PISTOL, st, inv, 17);
        assertEquals(17.0 / 35.0, m.progress(), 1e-12);
    }
}
