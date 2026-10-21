package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.item.ammo.def.GoldenRoundAmmo;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.ProductionTable;
import com.gregguns.recipe.ProductionTable.Row;

/** The Golden Gun's stats, its upgrade row and the Golden Round's row. */
// Spec: docs/gregguns-spec/02-guns/index.md (Golden Gun); docs/gregguns-spec/02-guns/recipes.md (GR-104);
// docs/gregguns-spec/06-items/recipes.md (RC-66)
class GoldenGunDefTest {

    @Test
    void carriesTheDesignedStats() {
        GunStats s = GoldenGunDef.STATS;
        assertTrue(s.annihilates());
        assertEquals(FireMode.SEMI, s.fireMode());
        assertEquals(1, s.magazineSize());
        assertEquals(60, s.reloadTicks());
        assertEquals(0.01, s.spread());
        assertEquals(ProjectileKind.BULLET, s.projectileKind());
        assertEquals(PistolDef.STATS.projectileSpeed(), s.projectileSpeed());
        assertEquals(PistolDef.STATS.lifetimeTicks(), s.lifetimeTicks());
        assertEquals(AmmoIds.GOLDEN_ROUND, s.ammoType());
        assertEquals(Tier.LUV, GoldenGunDef.DEFINITION.tier());
        assertEquals(DamageType.PROJECTILE, GoldenGunDef.DEFINITION.damageType());
        assertFalse(PistolDef.STATS.annihilates());
    }

    @Test
    void isAppendedToTheRegistryOrder() {
        assertSame(GoldenGunDef.DEFINITION, GunDefinitions.byName("golden_gun"));
        assertSame(GoldenGunDef.DEFINITION, GunDefinitions.ALL.get(GunDefinitions.ALL.size() - 1));
        assertSame(GoldenRoundAmmo.DEFINITION, AmmoDefinitions.ALL.get(AmmoDefinitions.ALL.size() - 1));
    }

    @Test
    void theGunIsAnAssemblerUpgradeOfThePistolAtLuv() {
        Row row = GoldenGunDef.DEFINITION.assembly();
        assertNotNull(row);
        assertEquals("GR-104", row.id());
        assertEquals("golden_gun", row.output());
        assertEquals(ProductionTable.Target.ASSEMBLER, row.target());
        assertEquals(Tier.LUV, row.tier());
        assertEquals(30, row.seconds());
        // Slot 0 is the Pistol (any NBT), then 8 gold plates, 2 LuV circuits and the HSS-G gate plate.
        assertEquals(
            4,
            row.inputs()
                .size());
        assertEquals(
            "pistol_9mm",
            row.inputs()
                .get(0)
                .itemName());
        assertEquals(
            "Gold",
            row.inputs()
                .get(1)
                .material());
        assertEquals(
            8,
            row.inputs()
                .get(1)
                .count());
        assertEquals(
            "circuit",
            row.inputs()
                .get(2)
                .prefix());
        assertEquals(
            "Master",
            row.inputs()
                .get(2)
                .material());
        assertEquals(
            2,
            row.inputs()
                .get(2)
                .count());
        assertEquals(
            "HSSG",
            row.inputs()
                .get(3)
                .material());
    }

    @Test
    void theRoundIsAnAssemblerRowAtLuvAndMeetsTheGateRule() {
        Row row = GoldenRoundAmmo.ROW;
        assertEquals("golden_round", row.output());
        assertEquals(ProductionTable.Target.ASSEMBLER, row.target());
        assertEquals(Tier.LUV, row.tier());
        assertEquals(10, row.seconds());
        assertEquals(
            3,
            row.inputs()
                .size());
        assertEquals(
            "ammo_9mm_bullet",
            row.inputs()
                .get(0)
                .itemName());
        assertEquals(
            "foil",
            row.inputs()
                .get(1)
                .prefix());
        assertEquals(
            2,
            row.inputs()
                .get(1)
                .count());
        assertEquals(
            "plate",
            row.inputs()
                .get(2)
                .prefix());
        assertFalse(ProductionTable.ROWS.contains(row), "an Assembler row, not an Ammo Press row");
        Map<String, Row> all = new LinkedHashMap<>(ProductionTable.byOutput());
        assertTrue(GateRule.satisfied(row, all, ProductionTable.GATE_EXEMPT));
    }
}
