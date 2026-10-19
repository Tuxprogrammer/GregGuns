package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.logic.ChargeMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ToolStats;

/** The Power Hammers and the Chainsaw against the Techguns sheets and the recipe sheet. */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md; docs/gregguns-spec/02-guns/recipes.md
class ToolGunDefinitionsTest {

    @Test
    void powerHammerCarriesTheSheetNumbers() {
        GunStats s = PowerHammerDef.STATS;
        ToolStats t = PowerHammerDef.TOOL;
        assertEquals(300, s.magazineSize());
        assertEquals("tank_air", s.ammoType());
        assertEquals(45, s.reloadTicks());
        assertTrue(s.isCharge());
        assertEquals(20, s.chargeFullTicks());
        assertEquals(ChargeMode.SCALED, s.chargeMode());
        assertEquals(10, s.chargeMaxUnits());
        assertEquals(6.0, t.meleeDamage());
        assertEquals(2.0, t.emptyMeleeDamage());
        assertEquals(2, t.harvestLevel("pickaxe"));
        assertEquals(2, t.harvestLevel("shovel"));
        assertEquals(12.0, t.digSpeed());
        assertEquals(1000, t.hitCostMilli());
        assertEquals(1000, t.blockCostMilli());
        assertEquals(Tier.HV, PowerHammerDef.DEFINITION.tier());
        assertNotNull(PowerHammerDef.DEFINITION.chargeEvent());
        assertEquals("guns.power_hammer.swing", PowerHammerDef.DEFINITION.swingEvent());
    }

    @Test
    void theAdvancedHammerIsStrongerAndAnUpgradeOfTheHammer() {
        ToolStats t = PowerHammerAdvDef.TOOL;
        assertEquals(7.0, t.meleeDamage());
        assertEquals(3, t.harvestLevel("pickaxe"));
        assertEquals(3, t.harvestLevel("shovel"));
        assertEquals(15.0, t.digSpeed());
        assertEquals(Tier.EV, PowerHammerAdvDef.DEFINITION.tier());
        assertEquals(PowerHammerDef.STATS.magazineSize(), PowerHammerAdvDef.STATS.magazineSize());
        assertEquals(
            "GR-92",
            PowerHammerAdvDef.DEFINITION.assembly()
                .id());
        assertEquals(
            PowerHammerDef.NAME,
            PowerHammerAdvDef.DEFINITION.assembly()
                .inputs()
                .get(0)
                .itemName());
        assertTrue(
            PowerHammerAdvDef.DEFINITION.assembly()
                .inputs()
                .get(0)
                .anyDamage(),
            "the base hammer matches at any damage, ignoring its NBT (GGS-02-40)");
    }

    @Test
    void chainsawCarriesTheSheetNumbers() {
        GunStats s = ChainsawDef.STATS;
        ToolStats t = ChainsawDef.TOOL;
        assertEquals(10.0, s.damage());
        assertEquals(0.25, s.penetration());
        assertEquals(0.0, s.knockback());
        assertEquals(3, s.fireRateTicks());
        assertEquals(300, s.magazineSize());
        assertEquals("tank_fuel", s.ammoType());
        assertEquals(1.5, s.projectileSpeed());
        assertEquals(2, s.lifetimeTicks());
        assertEquals(ProjectileKind.INVISIBLE, s.projectileKind());
        assertEquals(12.0, t.meleeDamage());
        assertEquals(2.0, t.emptyMeleeDamage());
        assertEquals(3, t.harvestLevel("axe"));
        assertEquals(14.0, t.digSpeed());
        assertEquals(Tier.HV, ChainsawDef.DEFINITION.tier());
        assertTrue(ChainsawDef.DEFINITION.startsLoop());
    }

    @Test
    void theRowsAreOnTheirTiersSheetIds() {
        assertEquals(
            "GR-90",
            ChainsawDef.DEFINITION.assembly()
                .id());
        assertEquals(
            "GR-91",
            PowerHammerDef.DEFINITION.assembly()
                .id());
        assertEquals(
            35,
            ChainsawDef.DEFINITION.assembly()
                .seconds());
        assertEquals(
            35,
            PowerHammerDef.DEFINITION.assembly()
                .seconds());
        assertEquals(
            30,
            PowerHammerAdvDef.DEFINITION.assembly()
                .seconds());
    }
}
