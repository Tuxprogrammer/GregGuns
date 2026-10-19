package com.gregguns.gun.logic;

/**
 * Which projectile entity a gun fires. {@code GunStats.projectileKind()} names it; the projectile launcher looks the
 * kind up in its factory table and spawns that entity. Pure data, both sides.
 *
 * <p>
 * Only {@link #BULLET} and {@link #GRAVITY_BULLET} have an entity today. The other constants reserve the kinds the
 * projectile sheet lists for the later guns; the milestone that builds a kind registers its factory with
 * {@code BulletLauncher.register}. A gun whose kind has no factory drops its shots and logs an error.
 *
 * <p>
 * Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md section 3 (per-projectile reference); the section number
 * is given on each constant.
 */
public enum ProjectileKind {

    /** The straight, fast bullet ({@code GenericProjectile}); every ballistic gun by default. Section 2. */
    BULLET,
    /** A bullet that drops under gravity ({@code SimpleProjectile}); the Hand Cannon. Section 3.1. */
    GRAVITY_BULLET,
    /** Rocket, M2 launchers. Section 3.4. */
    ROCKET,
    /** Grenade family, M2 launchers and thrown grenades. Section 3.7. */
    GRENADE,
    /** Flame stream, M5. Section 3.2. */
    FLAME,
    /** Laser bolt, M5. Section 3.3. */
    LASER,
    /** Bio blob, M5. Section 3.5. */
    BIO,
    /** Chain lightning, M5. Section 3.6. */
    TESLA,
    /** Energy blast (MIB gun), M5. Section 3.10. */
    ENERGY_BLAST,
    /** Sonic shotgun rings, M5. Section 3.13. */
    SONIC,
    /** Continuous radiation beam (Nuclear Death Ray), M5. Section 3.14. */
    BEAM
}
