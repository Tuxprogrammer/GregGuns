package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.explosion.ExplosionRules;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.recipe.ComponentTable;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.GunLineTable;
import com.gregguns.recipe.ProductionTable;

/** The Nuke Launcher's stats against the Techguns sheet and its Assembly Line row against the recipe sheet. */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Nuke Launcher);
// docs/gregguns-spec/02-guns/recipes.md (LuV and ZPM table)
class NukeLauncherDefTest {

    @Test
    void carriesTheSheetNumbers() {
        GunStats s = NukeLauncherDef.STATS;
        assertEquals(400.0, s.damage());
        assertEquals(10.0, s.blastRadius());
        assertEquals(0.01, s.projectileGravity());
        assertEquals(1.0, s.projectileSpeed());
        assertEquals(100, s.lifetimeTicks());
        assertEquals(0.05, s.spread());
        assertEquals(0.0, s.penetration());
        assertEquals(10, s.fireRateTicks());
        assertEquals(1, s.magazineSize());
        assertEquals(40, s.reloadTicks());
        assertEquals(10.0, s.knockback());
        assertEquals(ProjectileKind.ROCKET, s.projectileKind());
        assertEquals(AmmoIds.ROCKET_NUKE, s.ammoType());
        assertEquals(Tier.ZPM, NukeLauncherDef.DEFINITION.tier());
        assertEquals(DamageType.EXPLOSION, NukeLauncherDef.DEFINITION.damageType());
    }

    @Test
    void itsBlastIsNukeScaleSoItIsTimeSlicedAndSendsTheLongRangeFx() {
        // RocketProjectile passes the radius as the explosion size; at this size ProjectileExplosion sends the 150
        // block FX packet and BlastTicker slices the block breaks.
        assertTrue(NukeLauncherDef.STATS.blastRadius() >= ExplosionRules.NUKE_FX_MIN_SIZE);
        assertTrue(RocketLauncherDef.STATS.blastRadius() < ExplosionRules.NUKE_FX_MIN_SIZE);
    }

    @Test
    void isRegisteredAfterTheOthersAndHasNoAssemblerRow() {
        assertSame(NukeLauncherDef.DEFINITION, GunDefinitions.byName("nuke_launcher"));
        assertTrue(
            NukeLauncherDef.DEFINITION.rows()
                .isEmpty(),
            "the recipe is on the Assembly Line, not the Assembler");
    }

    @Test
    void theAssemblyLineRowMatchesTheSheet() {
        ComponentTable.Spec spec = GunLineTable.byOutput(GunLineTable.NUKE_LAUNCHER);
        ProductionTable.Row row = spec.row();
        assertEquals("nuke_launcher", row.output());
        assertEquals(ProductionTable.Target.ASSEMBLY_LINE, row.target());
        assertEquals(Tier.ZPM, row.tier());
        assertEquals(60, row.seconds());
        assertEquals(1, row.outputCount());
        // Barrels x2, carbon receiver, ZPM motor, two ZPM circuits, lead x4, naquadah alloy plates x4, screws x8.
        assertEquals(
            7,
            row.inputs()
                .size());
        assertEquals(
            2,
            row.inputs()
                .get(0)
                .count());
        assertEquals(
            2,
            row.inputs()
                .get(3)
                .count());
        assertEquals(
            8,
            row.inputs()
                .get(6)
                .count());
        assertEquals(
            2,
            spec.fluids()
                .size());
        assertEquals(
            1152,
            spec.fluids()
                .get(0)
                .amount());
        assertEquals(
            500,
            spec.fluids()
                .get(1)
                .amount());
        assertNotNull(spec.research());
        assertTrue(
            spec.research()
                .scanTier()
                .ordinal() < Tier.ZPM.ordinal());
    }

    @Test
    void theResearchItemIsTheGrimReaper() {
        assertEquals(
            GunLineTable.GRIM_REAPER,
            GunLineTable.byOutput(GunLineTable.NUKE_LAUNCHER)
                .research()
                .item()
                .itemName());
    }

    @Test
    void theRowMeetsTheGateRule() {
        Map<String, ProductionTable.Row> all = new LinkedHashMap<>(ProductionTable.byOutput());
        assertTrue(
            GateRule.satisfied(
                GunLineTable.byOutput(GunLineTable.NUKE_LAUNCHER)
                    .row(),
                all,
                ProductionTable.GATE_EXEMPT));
    }
}
