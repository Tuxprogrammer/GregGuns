package com.gregguns.combat;

import net.minecraft.util.DamageSource;

/**
 * A GregGuns damage source: a vanilla {@link DamageSource} that also carries a {@link HitProperties}. Implemented by
 * {@link RangedDamageSource} (projectile plus shooter) and {@link DirectDamageSource} (melee, explosions). Build them
 * through {@link DamageSources}. Server side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (GGS-02-17)
public interface GunDamageSource {

    /** @return the type, penetration and switches of this hit */
    HitProperties hit();

    /** @return this object as the vanilla source to pass to {@code attackEntityFrom} */
    DamageSource asDamageSource();

    /**
     * Overrides whether the hit ignores invulnerability frames (the default comes from the type).
     *
     * @param ignore true to land on every call regardless of frames, as shotgun pellets and miniguns must
     * @return this
     */
    GunDamageSource ignoringInvulnerabilityFrames(boolean ignore);

    /**
     * Turns off the type's secondary effect (burning, slowness, poison, radiation sickness, shock) for this hit.
     *
     * @return this
     */
    GunDamageSource withoutSecondaryEffects();
}
