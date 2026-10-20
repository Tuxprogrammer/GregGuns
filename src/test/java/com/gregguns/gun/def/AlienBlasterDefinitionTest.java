package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.fx.FxRegistry;
import com.gregguns.fx.FxResources;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.projectile.AlienBlasterProjectile;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.ProductionTable;

/** The Alien Blaster against the Techguns sheet and the recipe sheet. */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Alien Blaster); docs/gregguns-spec/02-guns/recipes.md
class AlienBlasterDefinitionTest {

    @Test
    void carriesTheSheetNumbers() {
        GunStats s = AlienBlasterDef.STATS;
        assertEquals(16.0, s.damage());
        assertEquals(16.0, s.damageMin(), "no drop-off: start and end are both 50");
        assertEquals(1.5, s.projectileSpeed());
        assertEquals(50, s.lifetimeTicks());
        assertEquals(0.0, s.spread());
        assertEquals(0.2, s.penetration());
        assertEquals(FireMode.AUTO, s.fireMode());
        assertEquals(8, s.fireRateTicks());
        assertEquals(5, s.recoilTicks());
        assertEquals(10, s.muzzleFlashTicks());
        assertFalse(s.hasScope(), "right-click does nothing");
        assertEquals(ProjectileKind.ALIEN_BLAST, s.projectileKind());
        assertEquals(Tier.LUV, AlienBlasterDef.DEFINITION.tier());
        assertEquals(DamageType.FIRE, AlienBlasterDef.DEFINITION.damageType());
    }

    @Test
    void isAnEnergyGunWithTenShotsInAFullCharge() {
        GunStats s = AlienBlasterDef.STATS;
        assertTrue(s.isEnergy());
        assertEquals(10, s.magazineSize(), "the sheet's 10 round clip is 10 shots of charge");
        assertEquals(AlienBlasterDef.CAPACITY_EU, s.euPerShot() * 10);
        assertEquals(1_000_000_000L, AlienBlasterDef.CAPACITY_EU, "the LuV Lapotronic Orb II baseline");
    }

    @Test
    void theAssemblyRowIsLuvAndGatedByHssg() {
        ProductionTable.Row row = AlienBlasterDef.DEFINITION.assembly();
        assertNotNull(row);
        assertEquals("GR-95", row.id());
        assertEquals(AlienBlasterDef.NAME, row.output());
        assertEquals(Tier.LUV, row.tier());
        assertEquals(ProductionTable.Target.ASSEMBLER, row.target());
        assertEquals("HSSG", GateRule.gateMaterial(row.tier()));
        assertTrue(
            row.inputs()
                .stream()
                .anyMatch(i -> "plate".equals(i.prefix()) && "HSSG".equals(i.material())),
            "an HSS-G plate is the gate");
        assertTrue(
            row.inputs()
                .size() <= 8,
            "eight item inputs plus the circuit selector fit the nine assembler slots");
    }

    @Test
    void thePassOneFxNamesAreCatalogEntries() {
        FxRegistry reg = FxResources.shared();
        assertEquals(
            3,
            reg.resolve(AlienBlasterProjectile.TRAIL_FX)
                .size(),
            "AlienBlasterTrail: flare, wave and glow");
        assertEquals(
            2,
            reg.resolve(AlienBlasterProjectile.IMPACT_FX)
                .size(),
            "AlienExplosion: flare and shockwave");
    }

    @Test
    void doesNotReproducePFX01_theTrailNeedsATrackedEntityThatTheModRegisters() {
        // PFX-01: Techguns sent the trail for an entity it never registered, so no client ever saw the bolt. The id
        // must be a real, distinct mod-local id, and the trail must be sent after the tracker has listed watchers.
        assertTrue(AlienBlasterProjectile.ENTITY_ID > 26, "past the Techguns ids 1 to 26");
        assertTrue(AlienBlasterProjectile.TRAIL_TICK >= 2, "not on the spawn tick, before the tracker lists watchers");
        assertEquals("AlienBlasterTrail", AlienBlasterProjectile.TRAIL_FX);
    }

    @Test
    void theBlastIsTinyAndNeverReachesFarther() {
        assertEquals(0.5F, AlienBlasterProjectile.BLAST_SIZE);
        assertEquals(0.25F, AlienBlasterProjectile.BLAST_INNER);
        assertEquals(0.75F, AlienBlasterProjectile.BLAST_OUTER);
    }
}
