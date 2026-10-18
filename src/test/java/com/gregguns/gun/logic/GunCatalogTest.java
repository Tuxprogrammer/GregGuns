package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class GunCatalogTest {

    @Test
    void doesNotReproduceGUN16_pistolHasOneDropOffAndOneMuzzleFlash() {
        // Techguns set the drop-off twice (15/22/6, then 18/25/5) and the muzzle effect twice. GregGuns defines each
        // once, with the winning values: full damage to 18 blocks, falling to 5 at 25.
        GunStats pistol = GunCatalog.PISTOL;
        assertEquals(8.0, pistol.damage());
        assertEquals(18.0, pistol.dropStart());
        assertEquals(25.0, pistol.dropEnd());
        assertEquals(5.0, pistol.damageMin());
        assertEquals(5, pistol.muzzleFlashTicks());
    }

    @Test
    void revolverMatchesItsSheet() {
        // docs/techguns-spec/02-guns/sheets-handguns-smgs.md, Revolver, section 3.
        GunStats r = GunCatalog.REVOLVER;
        assertEquals(8.0, r.damage());
        assertEquals(6.0, r.damageMin());
        assertEquals(12.0, r.dropStart());
        assertEquals(20.0, r.dropEnd());
        assertEquals(25, r.lifetimeTicks());
        assertEquals(1.5, r.projectileSpeed());
        assertEquals(0.025, r.spread());
        assertEquals(6, r.fireRateTicks());
        assertEquals(6, r.magazineSize());
        assertEquals(45, r.reloadTicks());
        assertEquals(6, r.recoilTicks());
        assertEquals(5, r.muzzleFlashTicks());
        assertEquals(18.0, r.aiRange());
        assertEquals(90, r.aiAttackTime());
        assertEquals(6, r.aiBurstCount());
        assertEquals(20, r.aiBurstDelay());
        assertFalse(r.hasScope());
        assertEquals(FireMode.SEMI, r.fireMode());
    }

    @Test
    void revolverLoadsStraightFromOneBulletWithNoContainer() {
        GunStats r = GunCatalog.REVOLVER;
        assertEquals(GunCatalog.AmmoIds.NINE_MM, r.ammoType());
        assertEquals(r.ammoType(), r.looseAmmoType());
        assertNull(r.emptyContainerType());
        assertEquals(1, r.ammoItemsPerReload());
        assertEquals(6, r.roundsPerItem());
    }

    @Test
    void handCannonMatchesItsSheet() {
        // docs/techguns-spec/02-guns/sheets-handguns-smgs.md, Hand Cannon, section 3.
        GunStats h = GunCatalog.HANDCANNON;
        assertEquals(8.0, h.damage());
        assertEquals(5.0, h.damageMin());
        assertEquals(10.0, h.dropStart());
        assertEquals(25.0, h.dropEnd());
        assertEquals(25, h.lifetimeTicks());
        assertEquals(1.0, h.projectileSpeed());
        assertEquals(0.035, h.spread());
        assertEquals(12, h.fireRateTicks());
        assertEquals(1, h.magazineSize());
        assertEquals(30, h.reloadTicks());
        assertEquals(5, h.recoilTicks());
        assertEquals(5, h.muzzleFlashTicks());
        assertEquals(12.0, h.aiRange());
        assertEquals(60, h.aiAttackTime());
        assertEquals(0, h.aiBurstCount());
        assertEquals(0, h.aiBurstDelay());
        assertEquals(0.0, h.penetration());
        assertFalse(h.hasScope());
        assertEquals(FireMode.AUTO, h.fireMode());
        assertEquals(ProjectileKind.GRAVITY_BULLET, h.projectileKind());
    }

    @Test
    void handCannonLoadsOneStoneBulletWithNoContainerAndNoRefund() {
        GunStats h = GunCatalog.HANDCANNON;
        assertEquals(GunCatalog.AmmoIds.STONE, h.ammoType());
        assertEquals(h.ammoType(), h.looseAmmoType());
        assertNull(h.emptyContainerType());
        assertEquals(1, h.ammoItemsPerReload());
        assertEquals(1, h.roundsPerItem());
    }

    @Test
    void gunsDefaultToABulletWithVanillaKnockback() {
        assertEquals(ProjectileKind.BULLET, GunCatalog.PISTOL.projectileKind());
        assertEquals(1.0, GunCatalog.PISTOL.knockback());
        assertEquals(ProjectileKind.BULLET, GunCatalog.REVOLVER.projectileKind());
    }
}
