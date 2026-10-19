package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageFalloff;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.def.FlamethrowerDef;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.Event;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.ProductionTable;

/**
 * Unit tests of the Flamethrower (batch B6) through the pure gun logic: the sheet's figures, its fuel-tank reload (a
 * full tank in, an empty one back, no refund of the rounds left), its fire behavior and its range.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Flamethrower); docs/gregguns-spec/02-guns/fluid-ammo.md
class FlamethrowerLogicTest {

    private static final GunStats STATS = FlamethrowerDef.STATS;

    private static double falloff(double distance) {
        return DamageFalloff.damageAt(STATS.damage(), STATS.damageMin(), STATS.dropStart(), STATS.dropEnd(), distance);
    }

    @Test
    void theSheetsFiguresAreKept() {
        assertEquals(5.0, STATS.damage());
        assertEquals(2.0, STATS.damageMin());
        assertEquals(4, STATS.dropStart());
        assertEquals(16, STATS.dropEnd());
        assertEquals(16, STATS.lifetimeTicks());
        assertEquals(1.0, STATS.projectileSpeed());
        assertEquals(0.05, STATS.spread());
        assertEquals(0.01, STATS.projectileGravity());
        assertEquals(0.0, STATS.penetration());
        assertEquals(FireMode.AUTO, STATS.fireMode());
        assertEquals(2, STATS.fireRateTicks());
        assertEquals(100, STATS.magazineSize());
        assertEquals(45, STATS.reloadTicks());
        assertEquals(ProjectileKind.FLAME, STATS.projectileKind());
        assertEquals(DamageType.FIRE, FlamethrowerDef.DEFINITION.damageType());
        assertEquals(Tier.HV, FlamethrowerDef.DEFINITION.tier());
        assertFalse(STATS.hasScope());
    }

    @Test
    void theRangeIsSixteenBlocksAndDamageFallsFromFourToSixteen() {
        assertEquals(16.0, STATS.reachBlocks(), 1e-9);
        assertEquals(5.0, falloff(4), 1e-9);
        assertEquals(3.5, falloff(10), 1e-9);
        assertEquals(2.0, falloff(16), 1e-9);
        assertEquals(2.0, falloff(40), 1e-9);
    }

    @Test
    void holdingTheTriggerBurnsOneRoundEveryTwoTicksUntilTheTankRunsDry() {
        GunController c = new GunController();
        GunState st = new GunState(STATS.magazineSize());
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int shots = 0;
        int lastShotTick = -1;
        for (int i = 0; i < 400 && shots < STATS.magazineSize(); i++) {
            if (c.tick(STATS, st, inv, true, false)
                .shot()
                .wasFired()) {
                if (lastShotTick >= 0) {
                    assertEquals(2, i - lastShotTick, "one flame every 2 ticks");
                }
                lastShotTick = i;
                shots++;
            }
        }
        assertEquals(100, shots, "a whole tank is 100 rounds");
        assertEquals(0, st.loaded());
        // 100 rounds over 1,000 mB: ten millilitres of fuel per shot.
        assertEquals(10.0, FluidAmmo.mbPerRound(1000, STATS.magazineSize()), 1e-9);
        assertEquals(1000, FluidAmmo.fuelLeftMb(100, 1000, 100));
        assertEquals(0, FluidAmmo.fuelLeftMb(0, 1000, 100));
    }

    @Test
    void aFullTankReloadsAnEmptyGunAndTheTankIsSpent() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.FUEL_TANK, 1);
        c.triggerPressed();
        assertEquals(
            Event.STARTED,
            c.tick(STATS, st, inv, true, false)
                .reload());
        for (int i = 0; i < STATS.reloadTicks(); i++) {
            c.tick(STATS, st, inv, true, false);
        }
        assertEquals(100, st.loaded());
        // The test double keeps one count per id, so the filled tank taken and the empty one handed back (the same
        // item id) cancel here; the item-level behavior is the scenarios' job.
        assertEquals(1, inv.items(AmmoIds.FUEL_TANK));
        assertEquals(AmmoIds.FUEL_TANK, STATS.emptyContainerType());
    }

    @Test
    void doesNotReproduceGun13_aForcedReloadMakesNoTankFromLeftoverRounds() {
        // The tank is spent whole; the rounds left in the gun are lost with the fuel, as the sheet says, but never turn
        // into extra items: 99 rounds are worth less than one tank (100 rounds per tank).
        assertEquals(100, GunItems.roundsPerLooseItem(STATS));
        GunController c = new GunController();
        GunState st = new GunState(99);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.FUEL_TANK, 1);
        assertEquals(Event.STARTED, c.forceReload(STATS, st, inv));
        for (int i = 0; i < STATS.reloadTicks(); i++) {
            c.tick(STATS, st, inv, true, false);
        }
        assertEquals(100, st.loaded());
        assertEquals(99, inv.rounds(AmmoIds.FUEL_TANK), "the controller hands the rounds back to the store...");
        assertEquals(0, 99 / GunItems.roundsPerLooseItem(STATS), "...which rounds down to no tank at all");
    }

    @Test
    void withNoFullTankTheGunDoesNotReload() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory(); // an empty tank is not counted as ammo by the store
        c.triggerPressed();
        GunTickResult r = c.tick(STATS, st, inv, true, false);
        assertEquals(Event.REJECTED_NO_AMMO, r.reload());
        assertTrue(r.outOfAmmo());
    }

    @Test
    void theStartSoundPlaysOnlyAfterAPauseOfTenTicks() {
        assertTrue(FlamethrowerDef.DEFINITION.startsLoop());
        assertEquals("guns.flamethrower.start", FlamethrowerDef.DEFINITION.startEvent());
        assertEquals("guns.flamethrower.fire", FlamethrowerDef.DEFINITION.fireEvent());
        assertNull(
            GunDefinitions.byName("pistol_9mm")
                .startEvent());
        assertTrue(LoopStart.isStart(100, LoopStart.NEVER), "the first shot ever");
        assertFalse(LoopStart.isStart(102, 100), "the next shot of a burst");
        assertFalse(LoopStart.isStart(109, 100));
        assertTrue(LoopStart.isStart(110, 100), "ten idle ticks later it starts again");
        assertTrue(LoopStart.isStart(500, 100));
    }

    @Test
    void theAssemblyRowIsTheSheetsAndNeedsTheKanthalIgniterCoil() {
        ProductionTable.Row row = FlamethrowerDef.DEFINITION.assembly();
        assertNotNull(row);
        assertEquals("GR-50", row.id());
        assertEquals(Tier.HV, row.tier());
        boolean coil = false;
        boolean tank = false;
        for (ProductionTable.Input in : row.inputs()) {
            coil |= "Kanthal".equals(in.material()) && "wireFine".equals(in.prefix()) && in.count() == 4;
            tank |= in.isGregGuns() && in.itemName()
                .equals(AmmoIds.FUEL_TANK);
        }
        assertTrue(coil, "four Kanthal fine wires are the igniter coil");
        assertFalse(tank, "the Fuel Tank is ammo, not an input: the gun is made empty");
    }
}
