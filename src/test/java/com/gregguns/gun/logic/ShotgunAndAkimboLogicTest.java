package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.AkimboPistolsDef;
import com.gregguns.gun.def.AkimboRevolversDef;
import com.gregguns.gun.def.CombatShotgunDef;
import com.gregguns.gun.def.PistolDef;
import com.gregguns.gun.def.RevolverDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.Event;
import com.gregguns.item.gun.GunItems;

/**
 * Unit tests of the batch B2 guns through the pure gun logic: the Combat Shotgun's pellets, spread and shell-by-shell
 * reload, and the Akimbo Pistols and Revolvers' alternating hands, doubled reload items and single-gun refund rate.
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (Combat Shotgun); sheets-handguns-smgs.md (Akimbo
// Pistols, Akimbo Revolvers); gun-lifecycle.md section 8
class ShotgunAndAkimboLogicTest {

    private static final GunStats SHOTGUN = CombatShotgunDef.STATS;
    private static final GunStats PISTOLS = AkimboPistolsDef.STATS;
    private static final GunStats REVOLVERS = AkimboRevolversDef.STATS;

    // ----- Combat Shotgun ----------------------------------------------------------------------------------------

    @Test
    void theShotgunSheetFiguresAreKept() {
        assertEquals(4.0, SHOTGUN.damage());
        assertEquals(1.5, SHOTGUN.damageMin());
        assertEquals(2, SHOTGUN.dropStart());
        assertEquals(5, SHOTGUN.dropEnd());
        assertEquals(15, SHOTGUN.lifetimeTicks());
        assertEquals(0.01, SHOTGUN.spread());
        assertEquals(0.15, SHOTGUN.pelletSpread());
        assertEquals(0.15, SHOTGUN.penetration());
        assertEquals(FireMode.AUTO, SHOTGUN.fireMode());
        assertEquals(14, SHOTGUN.fireRateTicks());
        assertEquals(8, SHOTGUN.magazineSize());
        assertEquals(50, SHOTGUN.reloadTicks());
        assertEquals(12, SHOTGUN.recoilTicks());
        assertFalse(SHOTGUN.hasScope());
    }

    @Test
    void oneRoundFiresEightPelletsWithTheCentreAtMainSpreadAndSevenAtPelletSpread() {
        assertEquals(8, SHOTGUN.bulletsPerShot());
        ShotContext ctx = ShotContext.player(false, 0.0);
        ShotOffset[] offsets = SpreadModel.roll(new Random(42), SHOTGUN, ctx, 0);
        assertEquals(8, offsets.length);
        // The spread is +-(spread * 40) degrees: 0.4 for the centre projectile, 6 for the pellets (the sheet's +-6).
        assertTrue(Math.abs(offsets[0].yawDegrees()) <= 0.4 + 1e-9);
        assertTrue(Math.abs(offsets[0].pitchDegrees()) <= 0.4 + 1e-9);
        boolean someWide = false;
        for (int i = 1; i < offsets.length; i++) {
            assertTrue(Math.abs(offsets[i].yawDegrees()) <= 6.0 + 1e-9, "pellet yaw " + offsets[i].yawDegrees());
            assertTrue(Math.abs(offsets[i].pitchDegrees()) <= 6.0 + 1e-9);
            assertEquals(0.0, offsets[i].forwardTickFraction(), "pellets spawn together, not in line");
            someWide |= Math.abs(offsets[i].yawDegrees()) > 0.4 || Math.abs(offsets[i].pitchDegrees()) > 0.4;
        }
        assertTrue(someWide, "the pellets must spread wider than the centre projectile");
        assertEquals(0.15, SpreadModel.pelletSpread(SHOTGUN, ctx), 1e-9);
    }

    @Test
    void theSameSeedRollsTheSamePellets() {
        ShotContext ctx = ShotContext.player(false, 0.0);
        ShotOffset[] a = SpreadModel.roll(new Random(7), SHOTGUN, ctx, 0);
        ShotOffset[] b = SpreadModel.roll(new Random(7), SHOTGUN, ctx, 0);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i].yawDegrees(), b[i].yawDegrees());
            assertEquals(a[i].pitchDegrees(), b[i].pitchDegrees());
        }
    }

    @Test
    void aShotSpendsOneRoundNotEight() {
        GunController c = new GunController();
        GunState st = new GunState(8);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        assertTrue(
            c.tick(SHOTGUN, st, inv, true, false)
                .shot()
                .wasFired());
        assertEquals(7, st.loaded());
    }

    @Test
    void theShotgunFiresEveryFourteenTicksWhileHeld() {
        GunController c = new GunController();
        GunState st = new GunState(8);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int shots = 0;
        for (int i = 0; i < 29; i++) { // ticks 0, 14 and 28
            if (c.tick(SHOTGUN, st, inv, true, false)
                .shot()
                .wasFired()) {
                shots++;
            }
        }
        assertEquals(3, shots);
    }

    @Test
    void theShotgunReloadsUpToEightShellsAndKeepsTheRoundsLoaded() {
        GunController c = new GunController();
        GunState st = new GunState(5);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.SHOTGUN_SHELL, 5);
        assertEquals(Event.STARTED, c.forceReload(SHOTGUN, st, inv));
        for (int i = 0; i < SHOTGUN.reloadTicks(); i++) {
            c.tick(SHOTGUN, st, inv, true, false);
        }
        assertEquals(8, st.loaded());
        assertEquals(2, inv.items(AmmoIds.SHOTGUN_SHELL), "only the three missing rounds were loaded");
    }

    @Test
    void anEmptyShotgunWithTenShellsLoadsEightAndGivesNothingBack() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.SHOTGUN_SHELL, 10);
        assertEquals(Event.STARTED, c.forceReload(SHOTGUN, st, inv));
        for (int i = 0; i < SHOTGUN.reloadTicks(); i++) {
            c.tick(SHOTGUN, st, inv, true, false);
        }
        assertEquals(8, st.loaded());
        assertEquals(2, inv.items(AmmoIds.SHOTGUN_SHELL));
    }

    // ----- Akimbo ------------------------------------------------------------------------------------------------

    @Test
    void theAkimboSheetFiguresAreKept() {
        assertTrue(PISTOLS.akimbo());
        assertFalse(PISTOLS.doubleShot());
        assertEquals(36, PISTOLS.magazineSize());
        assertEquals(2, PISTOLS.ammoItemsPerReload());
        assertEquals(52, PISTOLS.reloadTicks());
        assertEquals(0.035, PISTOLS.spread());
        assertEquals(3, PISTOLS.fireRateTicks());
        assertEquals(1, PISTOLS.bulletsPerShot());
        assertTrue(REVOLVERS.akimbo());
        assertFalse(REVOLVERS.doubleShot());
        assertEquals(12, REVOLVERS.magazineSize());
        assertEquals(70, REVOLVERS.reloadTicks());
        assertEquals(0.035, REVOLVERS.spread());
        assertEquals(6.0, REVOLVERS.damageMin());
        assertEquals(FireMode.SEMI, REVOLVERS.fireMode());
        assertEquals(RevolverDef.STATS.damage(), REVOLVERS.damage());
        assertEquals(PistolDef.STATS.damage(), PISTOLS.damage());
    }

    @Test
    void theHandsAlternateByTheParityOfTheRoundsLeft() {
        FireControl fc = new FireControl();
        GunState st = GunState.full(PISTOLS);
        int[] hands = new int[6];
        int[] left = new int[6];
        for (int i = 0; i < hands.length; i++) {
            fc.triggerPressed();
            fc.tick();
            left[i] = st.loaded();
            ShotResult r = fc.step(PISTOLS, st, false);
            assertTrue(r.wasFired());
            assertEquals(1, r.roundsUsed(), "one round per pull");
            hands[i] = r.attackType();
            fc.triggerReleased();
            for (int t = 0; t < PISTOLS.fireRateTicks(); t++) {
                fc.tick();
            }
        }
        for (int i = 0; i < hands.length; i++) {
            assertEquals(left[i] % 2, hands[i], "odd rounds left means the left hand");
            assertEquals(i % 2, hands[i], "starting from an even clip the hands alternate");
        }
        assertEquals(30, st.loaded());
    }

    @Test
    void theAkimboShotCarriesOneBulletAndTheLeftHandFlagFollowsTheHand() {
        ShotContext ctx = ShotContext.player(false, 0.0);
        ShotOffset[] right = SpreadModel.roll(new Random(1), PISTOLS, ctx, 0);
        ShotOffset[] left = SpreadModel.roll(new Random(1), PISTOLS, ctx, 1);
        assertEquals(1, right.length);
        assertEquals(1, left.length);
        assertFalse(right[0].leftHand());
        assertTrue(left[0].leftHand());
    }

    @Test
    void aReloadTakesTwoMagazinesAndReturnsTwoEmptyOnes() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(PISTOLS.ammoType(), 2);
        assertEquals(Event.STARTED, c.forceReload(PISTOLS, st, inv));
        for (int i = 0; i < PISTOLS.reloadTicks(); i++) {
            c.tick(PISTOLS, st, inv, true, false);
        }
        assertEquals(36, st.loaded());
        assertEquals(0, inv.items(PISTOLS.ammoType()));
        assertEquals(2, inv.items(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY));
    }

    @Test
    void oneMagazineIsNotEnoughToReloadAkimboPistols() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(PISTOLS.ammoType(), 1);
        assertEquals(Event.REJECTED_NO_AMMO, c.forceReload(PISTOLS, st, inv));
    }

    @Test
    void akimboRevolversReloadFromTwoBulletsWithNoEmptyContainer() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 2);
        assertEquals(Event.STARTED, c.forceReload(REVOLVERS, st, inv));
        for (int i = 0; i < REVOLVERS.reloadTicks(); i++) {
            c.tick(REVOLVERS, st, inv, true, false);
        }
        assertEquals(12, st.loaded());
        assertEquals(0, inv.items(AmmoIds.NINE_MM));
    }

    @Test
    void theAkimboRefundIsTheSingleGunsRateNotDoubleOrHalf() {
        // GUN-14 is a refund factor computed from the wrong clip. Each akimbo clip is two single clips, so the rate
        // per loose bullet must equal the single gun's.
        assertEquals(6, GunItems.roundsPerLooseItem(PistolDef.STATS));
        assertEquals(GunItems.roundsPerLooseItem(PistolDef.STATS), GunItems.roundsPerLooseItem(PISTOLS));
        assertEquals(6, GunItems.roundsPerLooseItem(RevolverDef.STATS));
        assertEquals(GunItems.roundsPerLooseItem(RevolverDef.STATS), GunItems.roundsPerLooseItem(REVOLVERS));

        GunController c = new GunController();
        GunState st = new GunState(30);
        MapAmmoInventory inv = new MapAmmoInventory().with(PISTOLS.ammoType(), 2);
        assertEquals(Event.STARTED, c.forceReload(PISTOLS, st, inv));
        for (int i = 0; i < PISTOLS.reloadTicks(); i++) {
            c.tick(PISTOLS, st, inv, true, false);
        }
        assertEquals(30, inv.rounds(AmmoIds.NINE_MM), "all 30 left rounds are handed back (GUN-13)");
        assertEquals(5, 30 / GunItems.roundsPerLooseItem(PISTOLS), "worth 5 bullets, as the sheet's 6 rounds each");
    }

    @Test
    void akimboGunsReuseTheSingleGunsTextures() {
        assertEquals("pistol_9mm", AkimboPistolsDef.DEFINITION.textureName());
        assertEquals("revolver", AkimboRevolversDef.DEFINITION.textureName());
        assertEquals(PistolDef.DEFINITION.camoCount(), AkimboPistolsDef.DEFINITION.camoCount());
        assertEquals(RevolverDef.DEFINITION.camoCount(), AkimboRevolversDef.DEFINITION.camoCount());
        assertEquals("combat_shotgun", CombatShotgunDef.DEFINITION.textureName());
    }
}
