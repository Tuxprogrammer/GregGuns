package com.gregguns.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

class DamageSourceTest {

    /** An entity with no world: enough for identity checks on a source. */
    private static final class Dummy extends Entity {

        Dummy() {
            super(null);
        }

        @Override
        protected void entityInit() {}

        @Override
        protected void readEntityFromNBT(NBTTagCompound tag) {}

        @Override
        protected void writeEntityToNBT(NBTTagCompound tag) {}
    }

    private static GunDamageSource ranged(DamageType type) {
        return DamageSources.ranged(type, null, null, 0.0);
    }

    @Test
    void damageTypeStringIsPrefixedPerType() {
        for (DamageType t : DamageType.values()) {
            assertEquals("gregguns." + t.id(), ranged(t).asDamageSource().damageType);
            assertEquals("gregguns." + t.id(), DamageSources.direct(t, null, 0.0).damageType);
        }
    }

    @Test
    void doesNotReproduceGUN44_fireSetsTheVanillaFireFlag() {
        assertTrue(
            ranged(DamageType.FIRE).asDamageSource()
                .isFireDamage());
        assertTrue(
            DamageSources.direct(DamageType.FIRE, null, 0)
                .isFireDamage());
        assertFalse(
            ranged(DamageType.ENERGY).asDamageSource()
                .isFireDamage());
    }

    @Test
    void explosionAndProjectileFlags() {
        assertTrue(
            ranged(DamageType.EXPLOSION).asDamageSource()
                .isExplosion());
        assertTrue(
            ranged(DamageType.PROJECTILE).asDamageSource()
                .isProjectile());
        assertFalse(
            ranged(DamageType.PHYSICAL).asDamageSource()
                .isProjectile());
        assertFalse(
            ranged(DamageType.PHYSICAL).asDamageSource()
                .isExplosion());
        assertTrue(
            ranged(DamageType.ENERGY).asDamageSource()
                .isMagicDamage());
        assertTrue(
            ranged(DamageType.POISON).asDamageSource()
                .isMagicDamage());
    }

    @Test
    void onlyUnresistableIsUnblockable() {
        for (DamageType t : DamageType.values()) {
            assertEquals(
                t == DamageType.UNRESISTABLE,
                ranged(t).asDamageSource()
                    .isUnblockable(),
                t.toString());
            assertEquals(
                t == DamageType.UNRESISTABLE,
                DamageSources.direct(t, null, 0.0)
                    .isUnblockable(),
                t.toString());
        }
        assertTrue(
            ranged(DamageType.UNRESISTABLE).asDamageSource()
                .isDamageAbsolute());
    }

    @Test
    void armorSettledMakesAnyHitUnblockableUntilReset() {
        RangedDamageSource s = DamageSources.ranged(DamageType.PROJECTILE, null, null, 0.1);
        assertFalse(s.isUnblockable());
        s.hit()
            .setArmorSettled(true);
        assertTrue(s.isUnblockable());
        s.hit()
            .setArmorSettled(false);
        assertFalse(s.isUnblockable());
    }

    @Test
    void doesNotReproduceGUN37_rangedSourceKeepsTheShooter() {
        Entity projectile = new Dummy();
        Entity shooter = new Dummy();
        RangedDamageSource s = DamageSources.ranged(DamageType.PROJECTILE, projectile, shooter, 0.15);
        assertSame(shooter, s.getEntity());
        assertSame(projectile, s.getSourceOfDamage());
        DirectDamageSource d = DamageSources.direct(DamageType.PHYSICAL, shooter, 0.0);
        assertSame(shooter, d.getEntity());
        assertSame(shooter, d.getSourceOfDamage());
    }

    @Test
    void penetrationTravelsWithTheSource() {
        assertEquals(
            0.15,
            DamageSources.ranged(DamageType.PROJECTILE, null, null, 0.15)
                .hit()
                .penetration(),
            1e-12);
        assertEquals(
            1.0,
            DamageSources.ranged(DamageType.PROJECTILE, null, null, 7)
                .hit()
                .penetration(),
            1e-12);
        assertEquals(
            0.0,
            DamageSources.direct(DamageType.PHYSICAL, null, Double.NaN)
                .hit()
                .penetration(),
            1e-12);
        assertEquals(
            0.0,
            DamageSources.direct(DamageType.PHYSICAL, null, -1)
                .hit()
                .penetration(),
            1e-12);
    }

    @Test
    void switchesDefaultFromTheTypeAndCanBeOverridden() {
        GunDamageSource s = DamageSources.ranged(DamageType.PROJECTILE, null, null, 0);
        assertTrue(
            s.hit()
                .ignoresInvulnerabilityFrames());
        s.ignoringInvulnerabilityFrames(false);
        assertFalse(
            s.hit()
                .ignoresInvulnerabilityFrames());
        assertTrue(
            s.hit()
                .secondaryEffectsEnabled());
        s.withoutSecondaryEffects();
        assertFalse(
            s.hit()
                .secondaryEffectsEnabled());
        assertFalse(
            DamageSources.direct(DamageType.PHYSICAL, null, 0)
                .hit()
                .ignoresInvulnerabilityFrames());
    }
}
