package com.gregguns.projectile;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.gun.logic.ProjectileKind;

/**
 * Wires the projectile framework into the guns module: registers the projectile entities, installs the launcher, and
 * in the dev environment registers the {@code projectile.*} scenarios. Both sides.
 */
// Spec: docs/ROADMAP.md (M1, Projectile framework); docs/techguns-spec/04-projectiles-fx/projectiles.md (1
// Registration)
public final class ProjectileModule {

    /** Mod-local entity ID of {@link GenericProjectile}; the same number Techguns uses. */
    public static final int GENERIC_PROJECTILE_ID = 1;
    /** Mod-local entity ID of {@link SimpleProjectile}. */
    public static final int SIMPLE_PROJECTILE_ID = 2;

    private ProjectileModule() {}

    /**
     * Registers the entities and installs the launcher. Call from {@code GunsModule.preInit}.
     *
     * @param registrar the module registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        registrar.registerEntity(
            "generic_projectile",
            GenericProjectile.class,
            GENERIC_PROJECTILE_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        registrar.registerEntity(
            "simple_projectile",
            SimpleProjectile.class,
            SIMPLE_PROJECTILE_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        // The launcher kinds: a rocket and a fired 40 mm grenade (ProjectileKind.ROCKET and GRENADE).
        registrar.registerEntity(
            "rocket_projectile",
            RocketProjectile.class,
            RocketProjectile.ENTITY_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        registrar.registerEntity(
            "grenade_launcher_projectile",
            GrenadeLauncherProjectile.class,
            GrenadeLauncherProjectile.ENTITY_ID,
            GrenadeProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        registrar.registerEntity(
            "flamethrower_projectile",
            FlamethrowerProjectile.class,
            FlamethrowerProjectile.ENTITY_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        // The energy kinds: an instant laser (a damage carrier over Hitscan), its beam visual, and the energy bolt.
        registrar.registerEntity(
            "laser_projectile",
            LaserProjectile.class,
            LaserProjectile.ENTITY_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        registrar.registerEntity(
            "beam_visual",
            BeamVisual.class,
            BeamVisual.ENTITY_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            false);
        registrar.registerEntity(
            "energy_blast_projectile",
            EnergyBlastProjectile.class,
            EnergyBlastProjectile.ENTITY_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        // The Chainsaw's cut: a plain bullet that is never drawn.
        registrar.registerEntity(
            "invisible_projectile",
            InvisibleProjectile.class,
            InvisibleProjectile.ENTITY_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        BulletLauncher.register(ProjectileKind.INVISIBLE, InvisibleProjectile::new);
        registrar.registerEntity(
            "tesla_projectile",
            TeslaProjectile.class,
            TeslaProjectile.ENTITY_ID,
            GenericProjectile.TRACKING_RANGE,
            GenericProjectile.UPDATE_FREQUENCY,
            GenericProjectile.SENDS_VELOCITY);
        BulletLauncher.register(ProjectileKind.TESLA, TeslaProjectile::new);
        BulletLauncher.register(ProjectileKind.LASER, LaserProjectile::new);
        BulletLauncher.register(ProjectileKind.ENERGY_BLAST, EnergyBlastProjectile::new);
        BulletLauncher.register(ProjectileKind.FLAME, FlamethrowerProjectile::new);
        BulletLauncher.register(ProjectileKind.ROCKET, RocketProjectile::new);
        BulletLauncher.register(ProjectileKind.GRENADE, GrenadeLauncherProjectile::new);
        Projectiles.install(new BulletLauncher());
    }

    /** Registers the dev scenarios. Call from {@code GunsModule.init}. */
    public static void init() {
        if (DevEnvironment.isActive()) {
            ProjectileScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }
}
