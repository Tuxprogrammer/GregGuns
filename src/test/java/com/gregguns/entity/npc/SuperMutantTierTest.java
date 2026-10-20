package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.loot.NpcProfiles;
import com.gregguns.gun.def.GunDefinitions;

// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (14. Super Mutants), BUGS.md NPC-25
class SuperMutantTierTest {

    @Test
    void statsMatchTheSpecTable() {
        assertEquals(30.0, SuperMutantTier.BASIC.health());
        assertEquals(40.0, SuperMutantTier.HEAVY.health());
        assertEquals(50.0, SuperMutantTier.ELITE.health());
        assertEquals(7.0, SuperMutantTier.BASIC.damage());
        assertEquals(9.0, SuperMutantTier.HEAVY.damage());
        assertEquals(11.0, SuperMutantTier.ELITE.damage());
        assertEquals(0.30, SuperMutantTier.BASIC.speed());
        assertEquals(0.25, SuperMutantTier.HEAVY.speed());
        assertEquals(0.20, SuperMutantTier.ELITE.speed());
        assertEquals(7, SuperMutantTier.BASIC.armor());
        assertEquals(12, SuperMutantTier.HEAVY.armor());
        assertEquals(10, SuperMutantTier.ELITE.armor());
        assertEquals(50.0, SuperMutantTier.FOLLOW_RANGE);
    }

    @Test
    void hitboxHeightIsOnePointEightTimesScale() {
        assertEquals(2.43F, SuperMutantTier.BASIC.height(), 1e-4F);
        assertEquals(2.70F, SuperMutantTier.HEAVY.height(), 1e-4F);
        assertEquals(2.97F, SuperMutantTier.ELITE.height(), 1e-4F);
    }

    @Test
    void doesNotReproduceNpc25_hitboxWidthGrowsWithTheModel() {
        // Techguns kept 0.6 wide for every tier; each tier must be wider than the last and wider than 0.6.
        assertTrue(SuperMutantTier.BASIC.width() > SuperMutantTier.BASE_WIDTH);
        assertTrue(SuperMutantTier.HEAVY.width() > SuperMutantTier.BASIC.width());
        assertTrue(SuperMutantTier.ELITE.width() > SuperMutantTier.HEAVY.width());
        assertEquals(0.81F, SuperMutantTier.BASIC.width(), 1e-4F);
    }

    @Test
    void tiersMapToTheirMobsAndProfiles() {
        assertEquals(NpcMob.SUPER_MUTANT_BASIC, SuperMutantTier.BASIC.mob());
        assertEquals(NpcMob.SUPER_MUTANT_HEAVY, SuperMutantTier.HEAVY.mob());
        assertEquals(NpcMob.SUPER_MUTANT_ELITE, SuperMutantTier.ELITE.mob());
        for (SuperMutantTier t : SuperMutantTier.values()) {
            assertNotNull(NpcProfiles.get(t.profileId()), t.profileId());
        }
    }

    @Test
    void everyProfileWeaponIsARealGun() {
        for (String gun : new String[] { "rocket_launcher", "ak47", "combat_shotgun", "laser_rifle" }) {
            assertNotNull(GunDefinitions.byName(gun), gun);
            assertNotNull(NpcGunProfile.forGun(gun), gun);
        }
        assertNotSame(NpcGunProfile.DEFAULT, NpcGunProfile.forGun("rocket_launcher"));
    }
}
