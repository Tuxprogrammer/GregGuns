package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageFalloff;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.def.BioGunDef;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.Event;
import com.gregguns.projectile.BioRules;
import com.gregguns.recipe.ProductionTable;

/**
 * Unit tests of the Bio Gun (M5 B3a) through the pure gun logic: the sheet's figures, the charge levels and the rounds
 * they spend (GUN-02), the Bio Tank reload, the sounds and the EV assembly row.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Bio Gun); docs/techguns-spec/02-guns/per-gun-behavior.md;
// docs/gregguns-spec/02-guns/fluid-ammo.md
class BioGunLogicTest {

    private static final GunStats STATS = BioGunDef.STATS;

    private static int levelAfterHolding(int ticks, int loaded) {
        ChargeState charge = new ChargeState();
        assertTrue(charge.begin(true));
        for (int i = 0; i < ticks; i++) {
            charge.tick(STATS);
        }
        ChargeRelease release = charge.release(STATS, loaded);
        return release == null ? 0 : release.units();
    }

    @Test
    void theSheetsFiguresAreKept() {
        assertEquals(16.0, STATS.damage());
        assertEquals(13.0, STATS.damageMin());
        assertEquals(8, STATS.dropStart());
        assertEquals(15, STATS.dropEnd());
        assertEquals(35, STATS.lifetimeTicks());
        assertEquals(1.0, STATS.projectileSpeed());
        assertEquals(0.015, STATS.spread());
        assertEquals(0.01, STATS.projectileGravity());
        assertEquals(0.15, STATS.penetration());
        assertEquals(FireMode.AUTO, STATS.fireMode());
        assertEquals(6, STATS.fireRateTicks());
        assertEquals(30, STATS.magazineSize());
        assertEquals(45, STATS.reloadTicks());
        assertEquals(ProjectileKind.BIO, STATS.projectileKind());
        assertEquals(AmmoIds.BIO_TANK, STATS.ammoType());
        assertEquals(AmmoIds.BIO_TANK, STATS.emptyContainerType());
        assertEquals(DamageType.POISON, BioGunDef.DEFINITION.damageType());
        assertEquals(Tier.EV, BioGunDef.DEFINITION.tier());
        assertFalse(STATS.hasScope());
        assertFalse(STATS.isEnergy());
    }

    @Test
    void damageHoldsToEightBlocksThenFallsToTheMinimumAtFifteen() {
        assertEquals(10.0, DamageFalloff.damageAt(10, 8, 8, 15, 8), 1e-9);
        assertEquals(8.0, DamageFalloff.damageAt(10, 8, 8, 15, 15), 1e-9);
        assertEquals(8.0, DamageFalloff.damageAt(10, 8, 8, 15, 30), 1e-9);
    }

    @Test
    void theChargeTakesThirtyTicksAndGivesLevelsAtFortyFiveAndNinetyFivePercent() {
        assertTrue(STATS.isCharge());
        assertEquals(30, STATS.chargeFullTicks());
        assertEquals(ChargeMode.LEVELS, STATS.chargeMode());
        assertEquals(1, levelAfterHolding(0, 30));
        assertEquals(1, levelAfterHolding(13, 30), "13/30 = 0.43 is still level 1");
        assertEquals(2, levelAfterHolding(14, 30), "14/30 = 0.47 is level 2");
        assertEquals(2, levelAfterHolding(28, 30), "28/30 = 0.93 is still level 2");
        assertEquals(3, levelAfterHolding(29, 30), "29/30 = 0.97 is level 3");
        assertEquals(3, levelAfterHolding(200, 30), "the hold stops growing at a full charge");
    }

    @Test
    void doesNotReproduceGun02_aLevelNeverSpendsMoreRoundsThanAreLoaded() {
        assertEquals(2, levelAfterHolding(40, 2), "a full charge with 2 rounds is level 2, spending 2");
        assertEquals(1, levelAfterHolding(40, 1));
        assertEquals(0, levelAfterHolding(40, 0), "an empty gun fires nothing");
        GunState state = new GunState(2);
        ChargeState charge = new ChargeState();
        charge.begin(true);
        for (int i = 0; i < 40; i++) {
            charge.tick(STATS);
        }
        ChargeRelease release = charge.release(STATS, state.loaded());
        assertNotNull(release);
        assertTrue(release.limited());
        assertEquals(2, state.use(release.units()));
        assertEquals(0, state.loaded());
    }

    @Test
    void holdingTheLeftButtonBurnsOneRoundEverySixTicks() {
        GunController c = new GunController();
        GunState st = new GunState(STATS.magazineSize());
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int shots = 0;
        int last = -1;
        for (int i = 0; i < 300 && shots < STATS.magazineSize(); i++) {
            if (c.tick(STATS, st, inv, true, false)
                .shot()
                .wasFired()) {
                if (last >= 0) {
                    assertEquals(6, i - last);
                }
                last = i;
                shots++;
            }
        }
        assertEquals(30, shots, "a whole tank is 30 rounds");
        assertEquals(0, st.loaded());
        assertEquals(1000.0 / 30.0, FluidAmmo.mbPerRound(1000, STATS.magazineSize()), 1e-9);
    }

    @Test
    void aFullBioTankReloadsAnEmptyGunInFortyFiveTicks() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.BIO_TANK, 1);
        c.triggerPressed();
        assertEquals(
            Event.STARTED,
            c.tick(STATS, st, inv, true, false)
                .reload());
        for (int i = 0; i < STATS.reloadTicks(); i++) {
            c.tick(STATS, st, inv, true, false);
        }
        assertEquals(30, st.loaded());
    }

    @Test
    void withNoFullTankTheGunDoesNotReload() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        GunTickResult r = c.tick(STATS, st, inv, true, false);
        assertEquals(Event.REJECTED_NO_AMMO, r.reload());
        assertTrue(r.outOfAmmo());
    }

    @Test
    void soundsAreFireReloadAndImpactAndThereIsNoChargeSound() {
        assertEquals("guns.bio_gun.fire", BioGunDef.DEFINITION.fireEvent());
        assertEquals("guns.bio_gun.reload", BioGunDef.DEFINITION.reloadEvent());
        assertNull(BioGunDef.DEFINITION.chargeEvent(), "the original has no charge sound");
        assertNull(BioGunDef.DEFINITION.startEvent());
        assertNull(BioGunDef.DEFINITION.rechamberEvent());
        assertSame(BioGunDef.DEFINITION, GunDefinitions.byName("bio_gun"));
    }

    @Test
    void theAssemblyRowIsTheSheetsAndTheTankIsAmmoNotAnInput() {
        ProductionTable.Row row = BioGunDef.DEFINITION.assembly();
        assertNotNull(row);
        assertEquals("GR-65", row.id());
        assertEquals(Tier.EV, row.tier());
        assertEquals(30, row.seconds());
        assertEquals(7, row.circuit());
        boolean plate = false;
        boolean tank = false;
        for (ProductionTable.Input in : row.inputs()) {
            plate |= "plate".equals(in.prefix()) && "Titanium".equals(in.material()) && in.count() == 2;
            tank |= in.isGregGuns() && in.itemName()
                .equals(AmmoIds.BIO_TANK);
        }
        assertTrue(plate, "two titanium plates are the EV gate");
        assertFalse(tank, "the Bio Tank is ammo, not an input: the gun is made empty");
    }

    @Test
    void poisonIsFourForOneHundredTicksAndLevelsClampToOneToThree() {
        assertEquals(3, BioRules.POISON_AMPLIFIER, "amplifier 3 is Poison IV");
        assertEquals(100, BioRules.POISON_TICKS);
        assertEquals(1, BioRules.clampLevel(-4));
        assertEquals(1, BioRules.clampLevel(1));
        assertEquals(2, BioRules.clampLevel(2));
        assertEquals(3, BioRules.clampLevel(9));
    }

    @Test
    void doesNotReproducePfx28_blobsNeedSafeModeOff() {
        assertFalse(BioRules.mayTouchBlocks(false), "explosions.blockDamage false is safe mode: no blobs");
        assertTrue(BioRules.mayTouchBlocks(true));
    }
}
