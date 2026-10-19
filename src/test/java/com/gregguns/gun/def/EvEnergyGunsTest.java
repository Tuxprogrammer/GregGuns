package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.client.render.profile.ClientGunDefinitions;
import com.gregguns.client.render.profile.InHandTargets;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.projectile.TeslaProjectile;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.ProductionTable;

/**
 * The EV energy guns' definitions: stats from the Techguns sheets, an EV energy item sized to the Techguns clip, the
 * titanium-gated assembly row, and the client profile and in-hand target.
 */
class EvEnergyGunsTest {

    @Test
    void laserRifleMatchesItsSheet() {
        GunStats s = LaserRifleDef.STATS;
        assertEquals(12.0, s.damage());
        assertEquals(12.0, s.damageMin(), "flat: no drop-off");
        assertEquals(FireMode.AUTO, s.fireMode());
        assertEquals(5, s.fireRateTicks(), "4 shots per second");
        assertEquals(0.75, s.zoomFactor());
        assertTrue(s.hasScope());
        assertEquals(ProjectileKind.LASER, s.projectileKind());
        assertEquals(7.5 * 5, s.projectileSpeed() * s.lifetimeTicks(), 1e-9, "37.5 blocks of reach");
        assertEquals(0.0, s.spread());
        assertEquals(0.0, s.penetration());
    }

    @Test
    void teslaRifleMatchesItsSheet() {
        GunStats s = TeslaRifleDef.STATS;
        assertEquals(12.0, s.damage());
        assertEquals(12.0, s.damageMin(), "flat: no drop-off");
        assertEquals(FireMode.AUTO, s.fireMode());
        assertEquals(8, s.fireRateTicks(), "2.5 shots per second");
        assertEquals(0.75, s.zoomFactor());
        assertEquals(1.0, s.zoomSpreadMultiplier());
        assertEquals(ProjectileKind.TESLA, s.projectileKind());
        assertEquals(50.0, s.projectileSpeed() * s.lifetimeTicks(), 1e-9, "50 blocks of reach");
        assertEquals(DamageType.LIGHTNING, TeslaRifleDef.DEFINITION.damageType());
        assertEquals(3, TeslaProjectile.MAX_JUMPS);
        assertEquals(5.0, TeslaProjectile.CHAIN_RANGE);
        assertEquals("TeslaFlare", TeslaProjectile.FLARE_FX);
    }

    @Test
    void teslaRifleIsAnEvBatteryOfTwentyFiveShots() {
        GunStats s = TeslaRifleDef.STATS;
        assertTrue(s.isEnergy());
        assertEquals(25, s.magazineSize());
        assertEquals(s.capacityEu() / 25, s.euPerShot());
        assertEquals(Tier.EV, TeslaRifleDef.DEFINITION.tier());
    }

    @Test
    void laserRifleIsAnEvBatteryOfFortyFiveShots() {
        GunStats s = LaserRifleDef.STATS;
        assertTrue(s.isEnergy());
        assertEquals(LaserRifleDef.SHOTS, s.magazineSize());
        assertEquals(6_400_000L, s.capacityEu(), "the EV battery hull baseline");
        assertEquals(s.capacityEu() / 45, s.euPerShot());
        assertEquals(Tier.EV, LaserRifleDef.DEFINITION.tier());
        assertEquals(DamageType.ENERGY, LaserRifleDef.DEFINITION.damageType());
    }

    @Test
    void everyEvEnergyGunHasATitaniumGatedEvRowAndItsSounds() {
        for (GunDefinition def : new GunDefinition[] { LaserRifleDef.DEFINITION, TeslaRifleDef.DEFINITION }) {
            ProductionTable.Row row = def.assembly();
            assertNotNull(row, def.name());
            assertEquals(Tier.EV, row.tier(), def.name());
            assertTrue(
                GateRule.satisfied(row, ProductionTable.byOutput(), ProductionTable.GATE_EXEMPT),
                def.name() + " gate");
            assertEquals("guns." + def.name() + ".fire", def.fireEvent());
            assertEquals("guns." + def.name() + ".reload", def.reloadEvent());
            assertNull(def.startEvent());
            assertTrue(GunDefinitions.ALL.contains(def), def.name() + " is registered");
        }
    }

    @Test
    void everyEvEnergyGunHasAProfileAndAnInHandTarget() {
        for (String name : new String[] { LaserRifleDef.NAME, TeslaRifleDef.NAME }) {
            assertNotNull(ClientGunDefinitions.byName(name), name + " profile");
            assertNotNull(InHandTargets.of(name), name + " in-hand target");
        }
    }
}
