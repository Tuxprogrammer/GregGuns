package com.gregguns.projectile;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;

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
        Projectiles.install(new BulletLauncher());
    }

    /** Registers the dev scenarios. Call from {@code GunsModule.init}. */
    public static void init() {
        if (DevEnvironment.isActive()) {
            ProjectileScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }
}
