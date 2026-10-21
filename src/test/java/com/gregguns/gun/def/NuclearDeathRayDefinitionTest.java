package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.GunLineTable;
import com.gregguns.recipe.ProductionTable;

/** The Nuclear Death Ray against the Techguns sheet and the recipe sheet. */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Nuclear Death Ray); docs/gregguns-spec/02-guns/recipes.md
class NuclearDeathRayDefinitionTest {

    @Test
    void carriesTheSheetNumbers() {
        GunStats s = NuclearDeathRayDef.STATS;
        assertEquals(30.0, s.damage());
        assertEquals(5.0, s.damageMin());
        assertEquals(20.0, s.dropStart());
        assertEquals(40.0, s.dropEnd());
        assertEquals(0.0, s.spread());
        assertEquals(1.0, s.penetration());
        assertEquals(5, s.recoilTicks());
        assertEquals(5, s.muzzleFlashTicks());
        assertEquals(24.0, s.aiRange());
        assertEquals(40, s.aiAttackTime());
        assertEquals(5, s.aiBurstCount());
        assertEquals(5, s.aiBurstDelay());
        assertFalse(s.hasScope(), "right-click does nothing");
        assertEquals(Tier.ZPM, NuclearDeathRayDef.DEFINITION.tier());
        assertEquals(DamageType.RADIATION, NuclearDeathRayDef.DEFINITION.damageType());
    }

    @Test
    void firesABeamTickEveryTick() {
        GunStats s = NuclearDeathRayDef.STATS;
        assertEquals(ProjectileKind.BEAM, s.projectileKind());
        assertEquals(FireMode.AUTO, s.fireMode());
        assertEquals(1, s.fireRateTicks());
    }

    @Test
    void isAnEnergyGunWhoseChargeIsTheOldHundredRoundsAtOneEveryFiveTicks() {
        GunStats s = NuclearDeathRayDef.STATS;
        assertTrue(s.isEnergy());
        assertEquals(NuclearDeathRayDef.CAPACITY_EU, s.capacityEu());
        assertEquals(10_000_000_000L, NuclearDeathRayDef.CAPACITY_EU, "the ZPM Energy Module baseline");
        assertEquals(20_000_000L, s.euPerShot(), "one shot per tick of beam");
        assertEquals(500, s.magazineSize(), "100 rounds at one per 5 ticks is 500 ticks of beam");
        assertEquals(NuclearDeathRayDef.CAPACITY_EU, s.euPerShot() * s.magazineSize());
    }

    @Test
    void theBeamPlaysItsOwnSoundsSoTheGunHasNoLoopStartAndNoPerShotSound() {
        assertFalse(NuclearDeathRayDef.DEFINITION.startsLoop(), "the beam plays its own start sound");
        assertNull(NuclearDeathRayDef.DEFINITION.startEvent());
        assertEquals("guns.nuclear_death_ray.fire", NuclearDeathRayDef.DEFINITION.fireEvent());
    }

    @Test
    void theRecipeIsAZpmAssemblyLineRowGatedByNaquadahAlloy() {
        ComponentSpecs spec = new ComponentSpecs();
        ProductionTable.Row row = spec.row;
        assertEquals("GR-103", row.id());
        assertEquals(NuclearDeathRayDef.NAME, row.output());
        assertEquals(Tier.ZPM, row.tier());
        assertEquals(ProductionTable.Target.ASSEMBLY_LINE, row.target());
        assertEquals(60, row.seconds());
        assertEquals(
            8,
            row.inputs()
                .size());
        assertTrue(
            GateRule.satisfied(row, java.util.Collections.emptyMap(), java.util.Collections.emptySet()),
            "carries the ZPM gate");
        assertEquals(
            2,
            GunLineTable.byOutput(GunLineTable.NUCLEAR_DEATH_RAY)
                .fluids()
                .size(),
            "solder and lubricant");
        assertEquals(
            Tier.LUV,
            GunLineTable.byOutput(GunLineTable.NUCLEAR_DEATH_RAY)
                .research()
                .scanTier(),
            "scanned one tier below");
    }

    /** Reads the single spec of the NDR line table. */
    private static final class ComponentSpecs {

        final ProductionTable.Row row = GunLineTable.byOutput(GunLineTable.NUCLEAR_DEATH_RAY)
            .row();
    }
}
