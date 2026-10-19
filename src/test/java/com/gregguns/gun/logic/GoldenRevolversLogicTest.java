package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageFalloff;
import com.gregguns.gun.def.AkimboGoldenRevolversDef;
import com.gregguns.gun.def.GoldenRevolverDef;
import com.gregguns.gun.def.RevolverDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.Event;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.ProductionTable;

/**
 * Unit tests of the Golden Revolver and the Akimbo Golden Revolvers (batch B6): the sheets' figures, the akimbo pair's
 * damage 16 against the single gun's 14 (a Techguns quirk kept on purpose), the shared 9mm reload and the upgrade row.
 */
// Spec: docs/techguns-spec/02-guns/sheets-handguns-smgs.md (Golden Revolver, Akimbo Golden Revolvers)
class GoldenRevolversLogicTest {

    private static final GunStats GOLD = GoldenRevolverDef.STATS;
    private static final GunStats PAIR = AkimboGoldenRevolversDef.STATS;

    @Test
    void theGoldenRevolverKeepsItsSheet() {
        assertEquals(14.0, GOLD.damage());
        assertEquals(6.0, GOLD.damageMin());
        assertEquals(12, GOLD.dropStart());
        assertEquals(30, GOLD.dropEnd());
        assertEquals(25, GOLD.lifetimeTicks());
        assertEquals(1.5, GOLD.projectileSpeed());
        assertEquals(0.015, GOLD.spread());
        assertEquals(FireMode.SEMI, GOLD.fireMode());
        assertEquals(8, GOLD.fireRateTicks());
        assertEquals(6, GOLD.magazineSize());
        assertEquals(45, GOLD.reloadTicks());
        assertEquals(8, GOLD.recoilTicks());
        assertEquals(Tier.IV, GoldenRevolverDef.DEFINITION.tier());
        assertEquals(1, GoldenRevolverDef.DEFINITION.camoCount());
        // Drop-off: 14 at 12 blocks, 6 at 30, halfway between at 21.
        assertEquals(14.0, DamageFalloff.damageAt(14, 6, 12, 30, 12), 1e-9);
        assertEquals(10.0, DamageFalloff.damageAt(14, 6, 12, 30, 21), 1e-9);
        assertEquals(6.0, DamageFalloff.damageAt(14, 6, 12, 30, 35), 1e-9);
    }

    @Test
    void theGoldenRevolverShootsFurtherAndHarderThanTheRevolverAndLoadsTheSame() {
        assertTrue(GOLD.damage() > RevolverDef.STATS.damage());
        assertTrue(GOLD.dropEnd() > RevolverDef.STATS.dropEnd());
        assertTrue(GOLD.spread() < RevolverDef.STATS.spread(), "more accurate");
        assertEquals(RevolverDef.STATS.ammoType(), GOLD.ammoType());
        assertNull(GOLD.emptyContainerType());
        assertEquals(6, GunItems.roundsPerLooseItem(GOLD));
    }

    @Test
    void theAkimboPairDoesSixteenAgainstTheSinglesFourteenOnPurpose() {
        // Techguns lists 16 for the pair and 14 for the single (catalog row 21); GregGuns keeps the numbers as listed.
        assertEquals(16.0, PAIR.damage());
        assertEquals(14.0, GOLD.damage());
        assertEquals(15, PAIR.dropStart());
        assertEquals(30, PAIR.dropEnd());
        assertEquals(0.035, PAIR.spread());
        assertEquals(6, PAIR.fireRateTicks());
        assertEquals(12, PAIR.magazineSize());
        assertEquals(70, PAIR.reloadTicks());
        assertEquals(2, PAIR.ammoItemsPerReload());
        assertTrue(PAIR.akimbo());
        assertFalse(PAIR.doubleShot());
        assertEquals(Tier.IV, AkimboGoldenRevolversDef.DEFINITION.tier());
        assertEquals(GoldenRevolverDef.NAME, AkimboGoldenRevolversDef.DEFINITION.singleGun());
        assertEquals("guns.golden_revolver.fire", AkimboGoldenRevolversDef.DEFINITION.fireEvent());
        assertEquals(GoldenRevolverDef.DEFINITION.camoCount(), AkimboGoldenRevolversDef.DEFINITION.camoCount());
    }

    @Test
    void doesNotReproduceGun14_theAkimboPairRefundsAtTheSingleGunsRate() {
        // Two items fill 12 rounds, so each item is 6 rounds, the same as one bullet in the single gun.
        assertEquals(GunItems.roundsPerLooseItem(GOLD), GunItems.roundsPerLooseItem(PAIR));
        GunController c = new GunController();
        GunState st = new GunState(11);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 2);
        assertEquals(Event.STARTED, c.forceReload(PAIR, st, inv));
        for (int i = 0; i < PAIR.reloadTicks(); i++) {
            c.tick(PAIR, st, inv, true, false);
        }
        assertEquals(12, st.loaded());
        assertEquals(0, inv.items(AmmoIds.NINE_MM));
        assertEquals(11, inv.rounds(AmmoIds.NINE_MM), "the controller hands back what was loaded");
        assertEquals(1, 11 / GunItems.roundsPerLooseItem(PAIR), "which the store turns into one bullet, floored");
    }

    @Test
    void theGoldenRevolverReloadsFromOneBulletForSixRounds() {
        GunController c = new GunController();
        GunState st = new GunState(0);
        MapAmmoInventory inv = new MapAmmoInventory().with(AmmoIds.NINE_MM, 1);
        c.triggerPressed();
        assertEquals(
            Event.STARTED,
            c.tick(GOLD, st, inv, true, false)
                .reload());
        for (int i = 0; i < GOLD.reloadTicks(); i++) {
            c.tick(GOLD, st, inv, true, false);
        }
        assertEquals(6, st.loaded());
        assertEquals(0, inv.items(AmmoIds.NINE_MM));
    }

    @Test
    void theUpgradeRowConsumesARevolverAtAnyDamageAndIsGatedByTungstenSteel() {
        ProductionTable.Row row = GoldenRevolverDef.DEFINITION.assembly();
        assertNotNull(row);
        assertEquals("GR-51", row.id());
        assertEquals(Tier.IV, row.tier());
        ProductionTable.Input base = row.inputs()
            .get(0);
        assertEquals(RevolverDef.NAME, base.itemName());
        assertTrue(base.anyDamage(), "a used or camouflaged Revolver is accepted (GGS-02-40)");
        int gold = 0;
        boolean gate = false;
        for (ProductionTable.Input in : row.inputs()) {
            if ("Gold".equals(in.material())) {
                gold = in.count();
            }
            gate |= "TungstenSteel".equals(in.material());
        }
        assertEquals(8, gold, "eight gold plates stand for the original's eight gold ingots");
        assertTrue(gate);
    }
}
