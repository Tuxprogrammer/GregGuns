package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.recipe.ProductionTable;

/** The Rocket Launcher and Grenade Launcher stats and ammo against the Techguns sheets and the recipe sheet. */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md; docs/gregguns-spec/02-guns/recipes.md
class LauncherDefinitionsTest {

    @Test
    void rocketLauncherCarriesTheSheetNumbers() {
        GunStats s = RocketLauncherDef.STATS;
        assertEquals(60.0, s.damage());
        assertEquals(1.75, s.blastRadius());
        assertEquals(0.01, s.projectileGravity());
        assertEquals(1.0, s.projectileSpeed());
        assertEquals(100, s.lifetimeTicks());
        assertEquals(0.05, s.spread());
        assertEquals(10, s.fireRateTicks());
        assertEquals(1, s.magazineSize());
        assertEquals(40, s.reloadTicks());
        assertEquals(3.0, s.knockback());
        assertEquals(ProjectileKind.ROCKET, s.projectileKind());
        assertEquals(Tier.HV, RocketLauncherDef.DEFINITION.tier());
        assertEquals("ammo_rocket", s.ammoType());
    }

    @Test
    void grenadeLauncherCarriesTheSheetNumbers() {
        GunStats s = GrenadeLauncherDef.STATS;
        assertEquals(36.0, s.damage());
        assertEquals(1.5, s.blastRadius());
        assertEquals(0.01, s.projectileGravity());
        assertEquals(1.0, s.projectileSpeed());
        assertEquals(100, s.lifetimeTicks());
        assertEquals(0.05, s.spread());
        assertEquals(0.0, s.penetration());
        assertEquals(5, s.fireRateTicks());
        assertEquals(6, s.magazineSize());
        assertEquals(100, s.reloadTicks());
        assertEquals(ReloadMode.INCREMENTAL, s.reloadMode());
        assertEquals(6, s.ammoItemsPerReload());
        assertEquals(ProjectileKind.GRENADE, s.projectileKind());
        assertEquals(Tier.HV, GrenadeLauncherDef.DEFINITION.tier());
        assertEquals("ammo_40mm_grenade", s.ammoType());
    }

    @Test
    void doesNotReproduceGUN04_theStatsHoldTheFiguresTechgunsHardCoded() {
        // Techguns ignored the gun's lifetime, accuracy, penetration and damage bonus for the launcher; here the stats
        // themselves are the 100 tick fuse, 0.05 spread and 0 penetration, so honoring them changes nothing else.
        GunStats s = GrenadeLauncherDef.STATS;
        assertEquals(100, s.lifetimeTicks());
        assertEquals(0.05, s.spread());
        assertEquals(0.0, s.penetration());
    }

    @Test
    void launcherAssemblyRowsAreHvStainlessSteelRowsWithTheirOwnCircuits() {
        ProductionTable.Row rocket = RocketLauncherDef.DEFINITION.assembly();
        ProductionTable.Row grenade = GrenadeLauncherDef.DEFINITION.assembly();
        assertEquals("GR-20", rocket.id());
        assertEquals("GR-21", grenade.id());
        assertEquals(Tier.HV, rocket.tier());
        assertEquals(Tier.HV, grenade.tier());
        assertEquals(30, rocket.seconds());
        assertEquals(30, grenade.seconds());
        assertEquals(8, rocket.circuit());
        assertEquals(7, grenade.circuit());
        assertTrue(
            rocket.inputs()
                .stream()
                .anyMatch(i -> "StainlessSteel".equals(i.material()) && "plate".equals(i.prefix())));
    }

    @Test
    void ammoRowsAreTheSheetsAndTheAmmoIsRegisteredAsTypes() {
        ProductionTable.Row grenade = null;
        ProductionTable.Row rocket = null;
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            if (row.id()
                .equals("RC-33")) {
                grenade = row;
            }
            if (row.id()
                .equals("RC-34")) {
                rocket = row;
            }
        }
        assertNotNull(grenade);
        assertNotNull(rocket);
        assertEquals("ammo_40mm_grenade", grenade.output());
        assertEquals(8, grenade.outputCount());
        assertEquals(Tier.MV, grenade.tier());
        assertEquals(8, grenade.seconds());
        assertEquals("ammo_rocket", rocket.output());
        assertEquals(4, rocket.outputCount());
        assertEquals(Tier.HV, rocket.tier());
        assertEquals(10, rocket.seconds());
        assertEquals(
            Tier.HV,
            AmmoTypes.ammo("rocket")
                .tier());
        assertEquals(
            Tier.MV,
            AmmoTypes.ammo("40mm_grenade")
                .tier());
        assertTrue(
            rocket.inputs()
                .stream()
                .anyMatch(i -> "circuit".equals(i.prefix())));
    }
}
