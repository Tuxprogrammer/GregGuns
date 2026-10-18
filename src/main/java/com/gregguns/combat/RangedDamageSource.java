package com.gregguns.combat;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

/**
 * A GregGuns hit from a projectile fired by a shooter. It is an {@link EntityDamageSourceIndirect}, so
 * {@code getSourceOfDamage()} is the projectile and {@code getEntity()} is the shooter, which is what Infernal Mobs,
 * GT++ listeners and the kill credit see (fixes GUN-37). Server side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (GGS-02-17, 19, 21); docs/design/0009 decision 3
public final class RangedDamageSource extends EntityDamageSourceIndirect implements GunDamageSource {

    private final HitProperties hit;

    RangedDamageSource(DamageType type, Entity projectile, Entity shooter, double penetration) {
        super(type.sourceName(), projectile, shooter);
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
    public RangedDamageSource ignoringInvulnerabilityFrames(boolean ignore) {
        hit.setIgnoreInvulnerabilityFrames(ignore);
        return this;
    }

    @Override
    public RangedDamageSource withoutSecondaryEffects() {
        hit.disableSecondaryEffects();
        return this;
    }

    /**
     * Vanilla's unblockable flag, which both vanilla and mod armor read. True for the unresistable type, and for the
     * rest of a hit once the damage handler has applied armor itself (so vanilla's own armor step does not run a
     * second time).
     */
    @Override
    public boolean isUnblockable() {
        return super.isUnblockable() || hit.isArmorSettled();
    }
}
