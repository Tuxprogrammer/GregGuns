package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.client.render.profile.ClientGunDefinition;
import com.gregguns.client.render.profile.ClientGunDefinitions;
import com.gregguns.client.render.profile.InHandTargets;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.AkimboNbt;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ShotPattern;

/**
 * The IV energy batch (M5 B4): Blaster Rifle, De-Atomizer Pistol, Akimbo De-Atomizer Pistols and Scatter Laser Rifle.
 * Pins each definition to its Techguns sheet row and to the energy rules (shots of charge, no reload), and the client
 * list to the in-hand targets.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md; docs/gregguns-spec/02-guns/recipes.md (IV table)
class EnergyBatchDefinitionsTest {

    @Test
    void theBlasterRifleIsAnIvScopedEnergyBoltGunOfFiftyShots() {
        GunStats s = BlasterRifleDef.STATS;
        assertTrue(s.isEnergy());
        assertEquals(50, s.magazineSize());
        assertEquals(2_000_000L, s.euPerShot());
        assertEquals(ProjectileKind.ENERGY_BLAST, s.projectileKind());
        assertEquals(FireMode.AUTO, s.fireMode());
        assertEquals(5, s.fireRateTicks());
        assertEquals(0.5, s.zoomFactor());
        assertTrue(s.hasScope());
        assertEquals(24.0, s.damage());
        assertEquals(19.0, s.damageMin());
        assertEquals(0.2, s.penetration());
        assertEquals(Tier.IV, BlasterRifleDef.DEFINITION.tier());
        assertEquals(DamageType.ENERGY, BlasterRifleDef.DEFINITION.damageType());
    }

    @Test
    void theDeAtomizerIsASemiAutoBlueBlastGunOfTwentyShotsWithNoAreaBlast() {
        GunStats s = DeatomizerPistolDef.STATS;
        assertTrue(s.isEnergy());
        assertEquals(20, s.magazineSize());
        assertEquals(FireMode.SEMI, s.fireMode());
        assertEquals(8, s.fireRateTicks());
        assertEquals(ProjectileKind.ENERGY_BLAST, s.projectileKind());
        assertEquals(0.0, s.blastRadius(), "Techguns commented the area explosion out");
        assertFalse(s.hasScope());
        assertFalse(s.akimbo());
        assertEquals(32.0, s.damage());
        assertEquals(Tier.IV, DeatomizerPistolDef.DEFINITION.tier());
    }

    @Test
    void theAkimboIsTwoPistolsOfChargeWithAlternatingHandsAndNoMachineRow() {
        GunDefinition akimbo = AkimboDeatomizerPistolsDef.DEFINITION;
        GunStats s = akimbo.stats();
        assertTrue(s.isEnergy());
        assertTrue(s.akimbo());
        assertFalse(s.doubleShot());
        assertEquals(40, s.magazineSize());
        assertEquals(2 * DeatomizerPistolDef.STATS.capacityEu(), s.capacityEu());
        assertEquals(DeatomizerPistolDef.STATS.euPerShot(), s.euPerShot());
        assertEquals(5, s.fireRateTicks());
        assertEquals(DeatomizerPistolDef.NAME, akimbo.singleGun());
        assertEquals(Tier.IV, akimbo.tier());
        assertTrue(
            akimbo.rows()
                .isEmpty(),
            "crafting table only (GGS-02-07)");
        assertNotNull(akimbo.recipes());
        assertEquals("guns.deatomizer_pistol.fire", akimbo.fireEvent());
    }

    @Test
    void theScatterLaserFiresAFanOfFiveHitscanBeamsOfFortyShots() {
        GunStats s = ScatterLaserRifleDef.STATS;
        assertTrue(s.isEnergy());
        assertEquals(40, s.magazineSize());
        assertEquals(ProjectileKind.LASER, s.projectileKind());
        assertEquals(
            ShotPattern.Shape.FAN,
            s.pattern()
                .shape());
        assertEquals(
            5,
            s.pattern()
                .projectileCount());
        assertEquals(0.1, s.spread());
        assertEquals(12.0, s.damage());
        assertEquals(12.0, s.damageMin(), "no drop-off");
        assertEquals(37.5, s.reachBlocks(), 1e-9);
        assertTrue(s.hasScope());
        assertEquals(0.75, s.zoomFactor());
        assertEquals(Tier.IV, ScatterLaserRifleDef.DEFINITION.tier());
    }

    @Test
    void everyEnergyGunHoldsAFullChargeOfTheIvBaselineAndRowsUseTheTungstenSteelGate() {
        for (GunDefinition def : new GunDefinition[] { BlasterRifleDef.DEFINITION, DeatomizerPistolDef.DEFINITION,
            ScatterLaserRifleDef.DEFINITION }) {
            assertEquals(
                100_000_000L,
                def.stats()
                    .capacityEu(),
                def.name());
            assertEquals(
                def.stats()
                    .capacityEu(),
                def.stats()
                    .euPerShot()
                    * def.stats()
                        .magazineSize(),
                def.name() + " shots times cost is the capacity");
            assertNotNull(def.assembly(), def.name());
            assertEquals(
                Tier.IV,
                def.assembly()
                    .tier());
            assertTrue(
                def.assembly()
                    .inputs()
                    .stream()
                    .anyMatch(i -> "TungstenSteel".equals(i.material())),
                def.name() + " has the tungsten steel gate input");
        }
    }

    @Test
    void theClientListHoldsTheThreeProfilesAndTheAkimboPair() {
        for (String name : new String[] { "blaster_rifle", "deatomizer_pistol", "scatter_laser_rifle" }) {
            ClientGunDefinition c = ClientGunDefinitions.byName(name);
            assertNotNull(c, name);
            assertFalse(c.akimbo());
            assertNotNull(InHandTargets.of(name), name + " has in-hand targets");
        }
        ClientGunDefinition akimbo = ClientGunDefinitions.byName("akimbo_deatomizer_pistols");
        assertNotNull(akimbo);
        assertTrue(akimbo.akimbo());
        assertEquals(
            ClientGunDefinitions.byName("deatomizer_pistol")
                .textures()[0],
            akimbo.textures()[0],
            "the pair wears the single pistol's skin");
        assertNull(InHandTargets.of("akimbo_deatomizer_pistols"), "the pair is placed through the single gun");
    }

    @Test
    void aSplitHandsTheOddEuToTheSecondGunAndConservesTheTotal() {
        long[] even = AkimboNbt.splitCharge(15_000_000L);
        assertEquals(7_500_000L, even[0]);
        assertEquals(7_500_000L, even[1]);
        long[] odd = AkimboNbt.splitCharge(15_000_001L);
        assertEquals(7_500_000L, odd[0]);
        assertEquals(7_500_001L, odd[1]);
        long[] none = AkimboNbt.splitCharge(0L);
        assertEquals(0L, none[0] + none[1]);
        assertEquals(0L, AkimboNbt.splitCharge(-5L)[1]);
    }
}
