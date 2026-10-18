package com.gregguns.combat;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

/**
 * A GregGuns hit with no separate projectile: melee, explosions and area effects. The attacker is the direct source
 * and the killer. It is an {@link EntityDamageSource}. Server side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (GGS-02-17, 19, 21); docs/design/0009 decision 3
public final class DirectDamageSource extends EntityDamageSource implements GunDamageSource {

    private final HitProperties hit;

    DirectDamageSource(DamageType type, Entity attacker, double penetration) {
        super(type.sourceName(), attacker);
        this.hit = new HitProperties(type, penetration);
        switch (type) {
            case PROJECTILE:
                setProjectile();
                break;
            case FIRE:
                setFireDamage();
                break;
            case EXPLOSION:
                setExplosion();
                break;
            case UNRESISTABLE:
                setDamageBypassesArmor();
                setDamageIsAbsolute();
                break;
            case PHYSICAL:
                break;
            default:
                setMagicDamage();
                break;
        }
    }

    @Override
    public HitProperties hit() {
        return hit;
    }

    @Override
    public DamageSource asDamageSource() {
        return this;
    }

    @Override
    public DirectDamageSource ignoringInvulnerabilityFrames(boolean ignore) {
        hit.setIgnoreInvulnerabilityFrames(ignore);
        return this;
    }

    @Override
    public DirectDamageSource withoutSecondaryEffects() {
        hit.disableSecondaryEffects();
        return this;
    }

    /** See {@link RangedDamageSource#isUnblockable()}. */
    @Override
    public boolean isUnblockable() {
        return super.isUnblockable() || hit.isArmorSettled();
    }
}
