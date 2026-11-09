package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.AkimboMac10sDef;
import com.gregguns.gun.def.AkimboPistolsDef;
import com.gregguns.gun.def.AkimboRevolversDef;
import com.gregguns.gun.def.CombatShotgunDef;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.def.LmgDef;
import com.gregguns.gun.def.M4Def;
import com.gregguns.gun.def.RevolverDef;
import com.gregguns.gun.def.SawedOffDef;
import com.gregguns.gun.def.ThompsonDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.CancelReason;
import com.gregguns.gun.logic.ReloadStateMachine.Event;
import com.gregguns.gun.logic.ReloadStateMachine.Load;

/**
 * The beta reload rules of design record 0026: single loose rounds into a magazine-fed gun with no magazine at hand,
 * chamber-by-chamber loading of the revolvers and shotguns with a trigger interrupt, and the idle reload. The ammo
 * economy (no dupe) is {@code com.gregguns.item.gun.ReloadEconomyTest}.
 */
class SingleRoundAndChamberReloadTest {

    private static final GunStats THOMPSON = ThompsonDef.STATS;
    private static final GunStats M4 = M4Def.STATS;
    private static final GunStats LMG = LmgDef.STATS;
    private static final GunStats REVOLVER = RevolverDef.STATS;
    private static final GunStats SHOTGUN = CombatShotgunDef.STATS;

    private static Event run(ReloadStateMachine m, GunStats s, GunState st, AmmoInventory inv, int ticks) {
        Event last = Event.NONE;
        for (int i = 0; i < ticks; i++) {
            last = m.tick(s, st, inv);
        }
        return last;
    }

    // ----- which guns ---------------------------------------------------------------------------------------------

    @Test
    void exactlyTheMagazineFedGunsLoadSingleRounds() {
        List<String> single = new ArrayList<>();
        List<String> chamber = new ArrayList<>();
        for (GunDefinition def : GunDefinitions.ALL) {
            GunStats s = def.stats();
            if (s.loadsSingleRounds()) {
                single.add(def.name());
                assertEquals(ReloadMode.MAGAZINE, s.reloadMode(), def.name());
                assertTrue(
                    s.ammoType()
                        .startsWith("mag_"),
                    def.name() + " loads single rounds but its ammo is not a magazine: " + s.ammoType());
            }
            if (s.reloadMode() == ReloadMode.CHAMBER) {
                chamber.add(def.name());
            }
        }
        List<String> expectedSingle = new ArrayList<>();
        for (String n : new String[] { "pistol_9mm", "thompson_smg", "mac10", "m4", "ak47", "aug", "scar", "vector",
            "infiltrator", "as50", "lmg", "akimbo_mac10s", "akimbo_pistols", "minigun", "pdw", "pulse_rifle" }) {
            expectedSingle.add(n);
        }
        java.util.Collections.sort(single);
        java.util.Collections.sort(expectedSingle);
        assertEquals(expectedSingle, single);
        java.util.Collections.sort(chamber);
        assertEquals(
            java.util.Arrays.asList(
                "akimbo_golden_revolvers",
                "akimbo_revolvers",
                "combat_shotgun",
                "golden_revolver",
                "revolver",
                "sawed_off"),
            chamber);
    }

    // ----- single rounds ------------------------------------------------------------------------------------------

    @Test
    void anEmptyThompsonWithOnlyBulletsLoadsOneRoundAfterTheFullReload() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 5);
        assertEquals(Event.STARTED, m.start(THOMPSON, st, inv));
        assertEquals(Load.SINGLE_ROUND, m.load());
        assertEquals(THOMPSON.reloadTicks(), m.totalTicks());
        assertEquals(Event.NONE, run(m, THOMPSON, st, inv, THOMPSON.reloadTicks() - 1));
        assertEquals(0, st.loaded());
        assertEquals(5, inv.items(AmmoIds.NINE_MM), "nothing moves before the end");
        assertEquals(Event.COMPLETED, m.tick(THOMPSON, st, inv));
        assertEquals(1, st.loaded());
        assertEquals(4, inv.items(AmmoIds.NINE_MM));
        assertEquals(0, inv.items(AmmoIds.SMG_MAGAZINE_EMPTY), "no empty magazine comes from a loose round");
        assertEquals(0, inv.rounds(AmmoIds.NINE_MM));
    }

    @Test
    void aMagazineIsPreferredWhenBothAreAtHand() {
        for (GunStats s : new GunStats[] { THOMPSON, M4, LMG }) {
            ReloadStateMachine m = new ReloadStateMachine();
            GunState st = new GunState(0);
            MapAmmoInventory inv = new MapAmmoInventory().with(s.ammoType(), 1)
                .with(s.looseAmmoType(), 10);
            assertEquals(Event.STARTED, m.start(s, st, inv));
            assertEquals(Load.MAGAZINE, m.load(), s.id());
            run(m, s, st, inv, s.reloadTicks());
            assertEquals(s.magazineSize(), st.loaded(), s.id());
            assertEquals(10, inv.items(s.looseAmmoType()), s.id());
        }
    }

    @Test
    void theReloadKeyOnAPartLoadedGunAddsOneRoundAndRefundsNothing() {
        GunController c = new GunController();
        GunState st = new GunState(17);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.RIFLE, 3);
        assertEquals(Event.STARTED, c.forceReload(M4, st, inv));
        for (int i = 0; i < M4.reloadTicks(); i++) {
            c.tick(M4, st, inv, true, false);
        }
        assertEquals(18, st.loaded());
        assertEquals(2, inv.items(AmmoIds.RIFLE));
        assertEquals(0, inv.rounds(AmmoIds.RIFLE), "a single-round load hands nothing back");
        assertEquals(0, inv.items(AmmoIds.ASSAULT_RIFLE_MAGAZINE_EMPTY));
    }

    @Test
    void singleRoundsStopAtTheMagazineSize() {
        ReloadStateMachine m = new ReloadStateMachine();
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 5);
        assertEquals(Event.REJECTED_FULL, m.start(THOMPSON, GunState.full(THOMPSON), inv));
        GunState st = new GunState(THOMPSON.magazineSize() - 1);
        m.start(THOMPSON, st, inv);
        run(m, THOMPSON, st, inv, THOMPSON.reloadTicks());
        assertEquals(THOMPSON.magazineSize(), st.loaded());
        assertEquals(4, inv.items(AmmoIds.NINE_MM));
    }

    @Test
    void firingAPartLoadedGunCancelsASingleRoundLoadWithNothingSpent() {
        GunController c = new GunController();
        GunState st = new GunState(5);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 2);
        assertEquals(Event.STARTED, c.forceReload(THOMPSON, st, inv));
        c.tick(THOMPSON, st, inv, true, false);
        c.triggerPressed();
        GunTickResult r = c.tick(THOMPSON, st, inv, true, false);
        assertEquals(Event.CANCELED, r.reload());
        assertTrue(
            r.shot()
                .wasFired());
        assertEquals(4, st.loaded());
        assertEquals(2, inv.items(AmmoIds.NINE_MM));
    }

    @Test
    void theDryTriggerStartsASingleRoundLoadAndNoMagazineNoBulletIsDry() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        c.triggerPressed();
        GunTickResult dry = c.tick(THOMPSON, st, new MapAmmoInventory(), true, false);
        assertTrue(dry.outOfAmmo());
        c.triggerReleased();
        c.triggerPressed();
        GunTickResult r = c.tick(THOMPSON, st, new MapAmmoInventory().with(AmmoIds.NINE_MM, 1), true, false);
        assertEquals(Event.STARTED, r.reload());
        assertEquals(
            Load.SINGLE_ROUND,
            c.reload()
                .load());
    }

    @Test
    void anAkimboPairTakesOneShotsWorthWithoutTwoMagazines() {
        // My call (design record 0026): one reload loads one shot's worth for the pair, not one round per gun.
        GunStats pistols = AkimboPistolsDef.STATS;
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(pistols.ammoType(), 1)
            .with(AmmoIds.NINE_MM, 4);
        assertEquals(Event.STARTED, m.start(pistols, st, inv));
        assertEquals(Load.SINGLE_ROUND, m.load(), "one magazine is not a pair's reload");
        run(m, pistols, st, inv, pistols.reloadTicks());
        assertEquals(1, st.loaded());
        assertEquals(3, inv.items(AmmoIds.NINE_MM));
        assertEquals(1, inv.items(pistols.ammoType()), "the lone magazine stays");

        GunStats macs = AkimboMac10sDef.STATS;
        m = new ReloadStateMachine();
        st = new GunState(0);
        inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 3);
        assertEquals(Event.STARTED, m.start(macs, st, inv));
        run(m, macs, st, inv, macs.reloadTicks());
        assertEquals(2, st.loaded(), "a double shot spends two, so a reload loads two");
        assertEquals(1, inv.items(AmmoIds.NINE_MM));
        assertTrue(st.canFire(macs));
        inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 1);
        assertEquals(Event.REJECTED_NO_AMMO, new ReloadStateMachine().start(macs, new GunState(0), inv));
    }

    @Test
    void nonMagazineGunsAndNpcsNeverLoadSingleRounds() {
        GunState empty = new GunState(0);
        assertEquals(
            Load.MAGAZINE,
            ReloadStateMachine.choose(THOMPSON, empty, AmmoInventory.INFINITE),
            "an NPC's infinite source always has the magazine");
        assertEquals(
            Load.NONE,
            ReloadStateMachine.choose(GunCatalog.BOLT_ACTION, empty, new MapAmmoInventory().with("x", 1)));
        assertFalse(GunCatalog.BOLT_ACTION.loadsSingleRounds());
        assertFalse(GunCatalog.HANDCANNON.loadsSingleRounds());
    }

    // ----- chambers -----------------------------------------------------------------------------------------------

    @Test
    void chambersSplitTheReloadEvenlyAndAddUpToIt() {
        for (GunStats s : new GunStats[] { REVOLVER, SHOTGUN, SawedOffDef.STATS, AkimboRevolversDef.STATS }) {
            int sum = 0;
            for (int i = 0; i < s.magazineSize(); i++) {
                int t = s.chamberTicks(i);
                assertTrue(
                    t == s.reloadTicks() / s.magazineSize() || t == s.reloadTicks() / s.magazineSize() + 1,
                    s.id() + " chamber " + i + ": " + t);
                sum += t;
            }
            assertEquals(s.reloadTicks(), sum, s.id());
            assertEquals(s.reloadTicks(), s.chamberTicksBetween(0, s.magazineSize()), s.id());
        }
        assertEquals(7, REVOLVER.chamberTicks(0));
        assertEquals(8, REVOLVER.chamberTicks(1));
    }

    @Test
    void anEmptyRevolverFillsChamberByChamberInTodaysTotalTime() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 1);
        assertEquals(Event.STARTED, m.start(REVOLVER, st, inv));
        assertEquals(Load.CHAMBER, m.load());
        assertEquals(REVOLVER.reloadTicks(), m.totalTicks());
        int[] loadedAt = new int[REVOLVER.reloadTicks() + 1];
        Event last = Event.NONE;
        for (int t = 1; t <= REVOLVER.reloadTicks(); t++) {
            last = m.tick(REVOLVER, st, inv);
            loadedAt[t] = st.loaded();
        }
        assertEquals(Event.COMPLETED, last);
        // Chamber boundaries floor(k * 45 / 6): 7, 15, 22, 30, 37, 45.
        assertEquals(0, loadedAt[6]);
        assertEquals(1, loadedAt[7]);
        assertEquals(1, loadedAt[14]);
        assertEquals(2, loadedAt[15]);
        assertEquals(5, loadedAt[44]);
        assertEquals(6, loadedAt[45]);
        assertEquals(0, inv.items(AmmoIds.NINE_MM));
        assertEquals(0, st.credit());
        assertFalse(m.isReloading());
    }

    @Test
    void theRevolverPaysForItsBulletAtTheFirstChamberAndKeepsTheRestAsCredit() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(5);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 2);
        assertEquals(Event.STARTED, m.start(REVOLVER, st, inv));
        assertEquals(REVOLVER.chamberTicks(5), m.totalTicks(), "one empty chamber, one chamber's time");
        assertEquals(Event.COMPLETED, run(m, REVOLVER, st, inv, m.totalTicks()));
        assertEquals(6, st.loaded());
        assertEquals(1, inv.items(AmmoIds.NINE_MM));
        assertEquals(5, st.credit(), "the bullet's other five rounds wait on the gun");
        assertEquals(0, inv.rounds(AmmoIds.NINE_MM));

        st.use(3);
        assertEquals(Event.STARTED, m.start(REVOLVER, st, new MapAmmoInventory()), "credit alone reloads");
        run(m, REVOLVER, st, new MapAmmoInventory(), m.totalTicks());
        assertEquals(6, st.loaded());
        assertEquals(2, st.credit());
    }

    @Test
    void aShotgunRunCoversOnlyTheShellsAtHand() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.SHOTGUN_SHELL, 3);
        m.start(SHOTGUN, st, inv);
        assertEquals(SHOTGUN.chamberTicksBetween(0, 3), m.totalTicks());
        assertEquals(Event.COMPLETED, run(m, SHOTGUN, st, inv, m.totalTicks()));
        assertEquals(3, st.loaded());
        assertEquals(0, inv.items(AmmoIds.SHOTGUN_SHELL));
        assertEquals(0, st.credit(), "a shell is one round: no credit");
    }

    @Test
    void ammoGoneMidRunKeepsTheChambersLoaded() {
        ReloadStateMachine m = new ReloadStateMachine();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.SHOTGUN_SHELL, 5);
        m.start(SHOTGUN, st, inv);
        run(m, SHOTGUN, st, inv, SHOTGUN.chamberTicksBetween(0, 2));
        assertEquals(2, st.loaded());
        inv.items.put(AmmoIds.SHOTGUN_SHELL, 0);
        assertEquals(Event.CANCELED, run(m, SHOTGUN, st, inv, SHOTGUN.chamberTicks(2)));
        assertEquals(CancelReason.NO_AMMO, m.lastCancelReason());
        assertEquals(2, st.loaded());
    }

    @Test
    void aPullMidRunStopsTheReloadAndFiresAtOnce() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.SHOTGUN_SHELL, 8);
        c.triggerPressed();
        assertEquals(
            Event.STARTED,
            c.tick(SHOTGUN, st, inv, true, false)
                .reload());
        c.triggerReleased();
        int threeChambers = SHOTGUN.chamberTicksBetween(0, 3);
        for (int i = 0; i < threeChambers; i++) {
            c.tick(SHOTGUN, st, inv, true, false);
        }
        assertEquals(3, st.loaded());
        c.triggerPressed();
        GunTickResult r = c.tick(SHOTGUN, st, inv, true, false);
        assertEquals(Event.CANCELED, r.reload());
        assertEquals(
            CancelReason.FIRE,
            c.reload()
                .lastCancelReason());
        assertTrue(
            r.shot()
                .wasFired(),
            "the shot leaves on the same tick");
        assertEquals(2, st.loaded());
        assertEquals(5, inv.items(AmmoIds.SHOTGUN_SHELL));
    }

    @Test
    void anAutomaticTriggerHeldSinceTheDryClickLetsTheChambersFill() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.SHOTGUN_SHELL, 8);
        c.triggerPressed(); // held from here on: the Combat Shotgun is automatic
        c.tick(SHOTGUN, st, inv, true, false);
        for (int i = 0; i < SHOTGUN.reloadTicks(); i++) {
            assertFalse(
                c.tick(SHOTGUN, st, inv, true, false)
                    .shot()
                    .wasFired());
        }
        assertEquals(8, st.loaded());
    }

    @Test
    void aPullWithNothingLoadedFiresTheFirstChamberAsSoonAsItIsIn() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 1);
        assertEquals(Event.STARTED, c.forceReload(REVOLVER, st, inv));
        c.triggerPressed();
        c.triggerReleased();
        boolean fired = false;
        for (int i = 0; i < REVOLVER.chamberTicks(0) + 1 && !fired; i++) {
            fired = c.tick(REVOLVER, st, inv, true, false)
                .shot()
                .wasFired();
        }
        assertTrue(fired);
        assertEquals(0, st.loaded());
        assertEquals(5, st.credit());
    }

    // ----- idle reload --------------------------------------------------------------------------------------------

    @Test
    void anIdleRevolverStartsItsChambersAfterTheDelay() {
        GunController c = new GunController();
        GunState st = new GunState(3);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 1);
        Event ev = Event.NONE;
        int startedAt = -1;
        for (int t = 1; t <= 60 && startedAt < 0; t++) {
            c.tick(REVOLVER, st, inv, true, false);
            ev = c.idleReload(REVOLVER, st, inv, 40);
            if (ev == Event.STARTED) {
                startedAt = t;
            }
        }
        assertEquals(40, startedAt);
        assertEquals(
            REVOLVER.chamberTicksBetween(3, 6),
            c.reload()
                .totalTicks());
        for (int i = 0; i < 6; i++) {
            c.tick(REVOLVER, st, inv, true, false);
        }
        c.triggerPressed();
        assertTrue(
            c.tick(REVOLVER, st, inv, true, false)
                .shot()
                .wasFired(),
            "a pull stops the idle reload like any chamber reload");
        assertFalse(
            c.reload()
                .isReloading());
    }

    @Test
    void theIdleReloadIsOffAtZeroAndSkipsOtherGunsAndFullOrDryOnes() {
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 9)
            .with(AmmoIds.SMG_MAGAZINE, 2);
        assertEquals(Event.NONE, idleAfter(REVOLVER, new GunState(2), inv, 0, 200));
        assertEquals(Event.NONE, idleAfter(THOMPSON, new GunState(2), inv, 40, 200));
        assertEquals(Event.NONE, idleAfter(REVOLVER, GunState.full(REVOLVER), inv, 40, 200));
        assertEquals(Event.NONE, idleAfter(REVOLVER, new GunState(2), new MapAmmoInventory(), 40, 200));
        assertEquals(Event.STARTED, idleAfter(REVOLVER, new GunState(2), inv, 40, 200));
    }

    @Test
    void holdingTheTriggerOrAShotResetsTheIdleCount() {
        GunController c = new GunController();
        GunState st = new GunState(6);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 1);
        for (int i = 0; i < 30; i++) {
            c.tick(REVOLVER, st, inv, true, false);
        }
        assertEquals(30, c.idleTicks());
        c.triggerPressed();
        assertTrue(
            c.tick(REVOLVER, st, inv, true, false)
                .shot()
                .wasFired());
        assertEquals(0, c.idleTicks());
        c.tick(REVOLVER, st, inv, false, false);
        assertEquals(0, c.idleTicks(), "a gun that is not held is never idle");
    }

    private static Event idleAfter(GunStats s, GunState st, AmmoInventory inv, int delay, int ticks) {
        GunController c = new GunController();
        for (int t = 0; t < ticks; t++) {
            c.tick(s, st, inv, true, false);
            Event ev = c.idleReload(s, st, inv, delay);
            if (ev != Event.NONE) {
                return ev;
            }
        }
        return Event.NONE;
    }
}
