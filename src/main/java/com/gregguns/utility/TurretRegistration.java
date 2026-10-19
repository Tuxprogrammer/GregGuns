package com.gregguns.utility;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.utility.turret.TurretMachines;
import com.gregguns.utility.turret.TurretScenarios;
import com.gregguns.utility.turret.entity.TurretEntity;

/**
 * Registration of the Turret (chunk m6/turret): the entity, the four MTEs, their recipes and the scenarios.
 * {@link UtilityModule} calls the three hooks. Common side.
 */
// Spec: docs/ROADMAP.md (M6), docs/gregguns-spec/09-machines-utility/turret.md
public final class TurretRegistration {

    private TurretRegistration() {}

    /**
     * Config-dependent registration: the turret entity.
     *
     * @param registrar the utility module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        registrar.registerEntity(
            "turret",
            TurretEntity.class,
            TurretEntity.ENTITY_ID,
            TurretEntity.TRACKING_RANGE,
            TurretEntity.UPDATE_FREQUENCY,
            true);
    }

    /** Machines (inside GT's MTE window) and recipes. */
    public static void init() {
        TurretMachines.register();
        TurretMachines.registerRecipes();
    }

    /**
     * Dev-only scenarios.
     *
     * @param registry where to add them
     */
    public static void registerScenarios(ScenarioRegistry registry) {
        TurretScenarios.registerAll(registry);
    }
}
