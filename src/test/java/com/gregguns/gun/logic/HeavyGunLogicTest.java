package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageFalloff;
import com.gregguns.gun.def.AkimboMac10sDef;
import com.gregguns.gun.def.As50Def;
import com.gregguns.gun.def.LmgDef;
import com.gregguns.gun.def.Mac10Def;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.Event;
import com.gregguns.item.gun.GunItems;

/**
 * Unit tests of the batch B4 guns through the pure gun logic: the AS50's scope, damage and penetration, the LMG's
 * 100-round magazine and sustained fire, and the Akimbo Mac10s' double shot (two rounds and two projectiles per pull).
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (AS50, LMG); sheets-handguns-smgs.md (Akimbo Mac10s)
class HeavyGunLogicTest {

    private static final GunStats AS50 = As50Def.STATS;
    private static final GunStats LMG = LmgDef.STATS;
    private static final GunStats AKIMBO = AkimboMac10sDef.STATS;

    // ----- AS50 --------------------------------------------------------------------------------------------------

    @Test
    void theAs50SheetFiguresAreKept() {
        assertEquals(32.0, AS50.damage());
        assertEquals(24.0, AS50.damageMin());
        assertEquals(30, AS50.dropStart());
        assertEquals(40, AS50.dropEnd());
        assertEquals(50, AS50.lifetimeTicks());
        assertEquals(2.25, AS50.projectileSpeed());
        assertEquals(0.0625, AS50.spread());
        assertEquals(0.25, AS50.penetration());
        assertEquals(FireMode.SEMI, AS50.fireMode());
        assertEquals(10, AS50.fireRateTicks());
        assertEquals(10, AS50.magazineSize());
        assertEquals(80, AS50.reloadTicks());
        assertEquals(10, AS50.recoilTicks());
        assertEquals(1, AS50.bulletsPerShot());
        assertEquals(Tier.HV, As50Def.DEFINITION.tier());
    }

    @Test
    void theAs50ScopeIsFovPoint35AndSpreadPointOneTwoFive() {
        assertTrue(AS50.hasScope());
        ZoomState zoom = new ZoomState();
        assertFalse(zoom.isZoomed(AS50));
        assertTrue(zoom.toggle(AS50));
        assertEquals(0.35, zoom.fovMultiplier(AS50), 1e-9);
        assertEquals(0.0625, SpreadModel.mainSpread(AS50, ShotContext.player(false, 0.0)), 1e-9);
        assertEquals(0.0625 * 0.125, SpreadModel.mainSpread(AS50, ShotContext.player(true, 0.0)), 1e-9);
    }

    @Test
    void theAs50DamageHoldsToThirtyBlocksThenFallsToTwentyFourAtForty() {
        assertEquals(32.0, DamageFalloff.damageAt(AS50.damage(), AS50.damageMin(), 30, 40, 30.0), 1e-9);
        assertEquals(28.0, DamageFalloff.damageAt(AS50.damage(), AS50.damageMin(), 30, 40, 35.0), 1e-9);
        assertEquals(24.0, DamageFalloff.damageAt(AS50.damage(), AS50.damageMin(), 30, 40, 40.0), 1e-9);
        assertEquals(24.0, DamageFalloff.damageAt(AS50.damage(), AS50.damageMin(), 30, 40, 100.0), 1e-9);
    }

    @Test
    void theAs50FiresOneRoundPerPullEvenWithTheTriggerHeld() {
        GunController c = new GunController();
        GunState st = new GunState(10);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int shots = 0;
        for (int i = 0; i < 30; i++) { // held down for 30 ticks: semi-auto fires once
            if (c.tick(AS50, st, inv, true, false)
                .shot()
                .wasFired()) {
                shots++;
            }
        }
        assertEquals(1, shots);
        assertEquals(9, st.loaded());
    }

    @Test
    void anAs50MagazineIsTenRoundsAndForcedReloadRefundsAtFivePerHighCaliberRound() {
        assertEquals(5, GunItems.roundsPerLooseItem(AS50));
        assertEquals(AmmoIds.HIGH_CALIBER_ROUND, AS50.looseAmmoType());
        GunController c = new GunController();
        GunState st = new GunState(7);
        MapAmmoInventory inv = new MapAmmoInventory().with(AS50.ammoType(), 1);
        assertEquals(Event.STARTED, c.forceReload(AS50, st, inv));
        for (int i = 0; i < AS50.reloadTicks(); i++) {
            c.tick(AS50, st, inv, true, false);
        }
        assertEquals(10, st.loaded());
        assertEquals(7, inv.rounds(AmmoIds.HIGH_CALIBER_ROUND), "all seven left rounds come back as loose rounds");
        assertEquals(1, inv.items(AmmoIds.AS50_MAGAZINE_EMPTY));
    }

    // ----- LMG ---------------------------------------------------------------------------------------------------

    @Test
    void theLmgSheetFiguresAreKept() {
        assertEquals(8.0, LMG.damage());
        assertEquals(6.0, LMG.damageMin());
        assertEquals(30, LMG.dropStart());
        assertEquals(40, LMG.dropEnd());
        assertEquals(50, LMG.lifetimeTicks());
        assertEquals(1.5, LMG.projectileSpeed());
        assertEquals(0.02, LMG.spread());
        assertEquals(0.15, LMG.penetration());
        assertEquals(FireMode.AUTO, LMG.fireMode());
        assertEquals(2, LMG.fireRateTicks());
        assertEquals(100, LMG.magazineSize());
        assertEquals(100, LMG.reloadTicks());
        assertEquals(5, LMG.recoilTicks());
        assertEquals(Tier.HV, LmgDef.DEFINITION.tier());
        assertTrue(LMG.hasScope());
        assertEquals(0.75, LMG.zoomFactor());
        assertEquals(0.75, LMG.zoomSpreadMultiplier());
    }

    @Test
    void theLmgSustainsTenRoundsASecondAndDrainsTheMagazineIn200Ticks() {
        GunController c = new GunController();
        GunState st = new GunState(100);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int shots = 0;
        int emptiedOnTick = -1;
        for (int i = 0; i < 260; i++) {
            if (c.tick(LMG, st, inv, true, false)
                .shot()
                .wasFired()) {
                shots++;
                if (st.loaded() == 0 && emptiedOnTick < 0) {
                    emptiedOnTick = i;
                }
            }
        }
        assertEquals(100, shots, "exactly the 100 loaded rounds are fired");
        assertEquals(0, st.loaded());
        assertEquals(198, emptiedOnTick, "100 rounds at one per 2 ticks, the first on tick 0, the last on tick 198");
    }

    @Test
    void theLmgMagazineIsEightRifleBulletsAtTwelveRoundsEach() {
        // 100 / 8 = 12 whole rounds per bullet (the sheet's 12.5, floored: a deviation recorded in the spec).
        assertEquals(12, GunItems.roundsPerLooseItem(LMG));
        assertEquals(AmmoIds.RIFLE, LMG.looseAmmoType());
        GunController c = new GunController();
        GunState st = new GunState(97);
        MapAmmoInventory inv = new MapAmmoInventory().with(LMG.ammoType(), 1);
        assertEquals(Event.STARTED, c.forceReload(LMG, st, inv));
        for (int i = 0; i < LMG.reloadTicks(); i++) {
            c.tick(LMG, st, inv, true, false);
        }
        assertEquals(100, st.loaded());
        assertEquals(97, inv.rounds(AmmoIds.RIFLE));
        assertEquals(1, inv.items(AmmoIds.LMG_MAGAZINE_EMPTY));
        assertEquals(8, 97 / GunItems.roundsPerLooseItem(LMG));
    }

    // ----- Akimbo Mac10s -----------------------------------------------------------------------------------------

    @Test
    void theAkimboMac10SheetFiguresAreKept() {
        assertTrue(AKIMBO.akimbo());
        assertTrue(AKIMBO.doubleShot());
        assertEquals(2, AKIMBO.ammoPerShot());
        assertEquals(64, AKIMBO.magazineSize());
        assertEquals(2, AKIMBO.ammoItemsPerReload());
        assertEquals(68, AKIMBO.reloadTicks());
        assertEquals(0.07, AKIMBO.spread());
        assertEquals(2, AKIMBO.fireRateTicks());
        assertEquals(FireMode.AUTO, AKIMBO.fireMode());
        assertEquals(Mac10Def.STATS.damage(), AKIMBO.damage());
        assertEquals(Mac10Def.STATS.damageMin(), AKIMBO.damageMin());
        assertEquals(1, AKIMBO.bulletsPerShot());
    }

    @Test
    void aDoubleShotSpendsTwoRoundsAndRollsTwoProjectilesOneFromEachHand() {
        FireControl fc = new FireControl();
        GunState st = GunState.full(AKIMBO);
        fc.triggerPressed();
        fc.tick();
        ShotResult r = fc.step(AKIMBO, st, false);
        assertTrue(r.wasFired());
        assertEquals(2, r.roundsUsed());
        assertEquals(62, st.loaded());
        assertEquals(0, r.attackType(), "a double shot does not alternate hands");

        ShotOffset[] offsets = SpreadModel.roll(new Random(5), AKIMBO, ShotContext.player(false, 0.0), r.attackType());
        assertEquals(2, offsets.length, "two projectiles per pull");
        assertFalse(offsets[0].leftHand());
        assertTrue(offsets[1].leftHand());
    }

    @Test
    void anAkimboPullNeedsTwoRoundsSoOneLeftRoundCannotFire() {
        assertFalse(new GunState(1).canFire(AKIMBO));
        assertTrue(new GunState(2).canFire(AKIMBO));
    }

    @Test
    void aFullAkimboClipIsThirtyTwoPulls() {
        GunController c = new GunController();
        GunState st = new GunState(64);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int pulls = 0;
        for (int i = 0; i < 100; i++) {
            if (c.tick(AKIMBO, st, inv, true, false)
                .shot()
                .wasFired()) {
                pulls++;
            }
        }
        assertEquals(32, pulls);
        assertEquals(0, st.loaded());
    }

    @Test
    void doesNotReproduceGun14_theAkimboMac10RefundIsTheSingleMac10sRate() {
        // GUN-14: the baseline's factor 2 gave 32 rounds per bullet on a 64 round clip.
        assertEquals(16, GunItems.roundsPerLooseItem(Mac10Def.STATS));
        assertEquals(16, GunItems.roundsPerLooseItem(AKIMBO));
        GunController c = new GunController();
        GunState st = new GunState(48);
        MapAmmoInventory inv = new MapAmmoInventory().with(AKIMBO.ammoType(), 2);
        assertEquals(Event.STARTED, c.forceReload(AKIMBO, st, inv));
        for (int i = 0; i < AKIMBO.reloadTicks(); i++) {
            c.tick(AKIMBO, st, inv, true, false);
        }
        assertEquals(64, st.loaded());
        assertEquals(48, inv.rounds(AmmoIds.NINE_MM));
        assertEquals(3, 48 / GunItems.roundsPerLooseItem(AKIMBO));
        assertEquals(2, inv.items(AmmoIds.SMG_MAGAZINE_EMPTY));
    }

    @Test
    void theAkimboMac10sReuseTheMac10sTextures() {
        assertEquals("mac10", AkimboMac10sDef.DEFINITION.textureName());
        assertEquals(Mac10Def.DEFINITION.camoCount(), AkimboMac10sDef.DEFINITION.camoCount());
    }
}
