package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GunStatsTest {

    @Test
    void pistolMatchesTheSpecSheet() {
        GunStats p = GunCatalog.PISTOL;
        assertEquals(8.0, p.damage());
        assertEquals(5.0, p.damageMin());
        assertEquals(18.0, p.dropStart());
        assertEquals(25.0, p.dropEnd());
        assertEquals(30, p.lifetimeTicks());
        assertEquals(1.5, p.projectileSpeed());
        assertEquals(45.0, p.reachBlocks(), 1e-9);
        assertEquals(0.025, p.spread());
        assertEquals(FireMode.SEMI, p.fireMode());
        assertEquals(4, p.fireRateTicks());
        assertEquals(18, p.magazineSize());
        assertEquals(35, p.reloadTicks());
        assertEquals(0.0, p.penetration());
        assertFalse(p.hasScope());
        assertEquals(24.0, p.aiRange());
        assertEquals(30, p.aiAttackTime());
        assertEquals(3, p.aiBurstCount());
        assertEquals(10, p.aiBurstDelay());
        assertEquals("mag_9mm_pistol", p.ammoType());
        assertEquals("ammo_9mm_bullet", p.looseAmmoType());
        assertEquals("mag_9mm_pistol_empty", p.emptyContainerType());
    }

    @Test
    void defaultsFollowTheCatalog() {
        GunStats s = GunStats.builder("x")
            .damage(4)
            .magazine(6)
            .ammoType("a")
            .build();
        assertEquals(4.0, s.damageMin());
        assertEquals(45.0, s.dropStart(), 1e-9);
        assertEquals(s.dropStart(), s.dropEnd());
        assertEquals("a", s.looseAmmoType());
        assertNull(s.emptyContainerType());
        assertEquals(1, s.bulletsPerShot());
    }

    @Test
    void inconsistentValuesAreRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> GunStats.builder("x")
                .magazine(5)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> GunStats.builder("x")
                .ammoType("a")
                .magazine(5)
                .fire(FireMode.SEMI, 0)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> GunStats.builder("x")
                .ammoType("a")
                .magazine(5)
                .reloadMode(ReloadMode.INCREMENTAL, 2)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> GunStats.builder("x")
                .ammoType("a")
                .magazine(5)
                .zoom(1.5, 1)
                .build());
    }

    @Test
    void incrementalRoundsPerItemSplitsTheMagazine() {
        GunStats s = GunStats.builder("shotgun")
            .ammoType("shell")
            .magazine(8)
            .reloadMode(ReloadMode.INCREMENTAL, 8)
            .build();
        assertEquals(1, s.roundsPerItem());
    }
}
