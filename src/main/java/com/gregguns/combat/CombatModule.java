package com.gregguns.combat;

import net.minecraftforge.common.MinecraftForge;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.testing.ScenarioRegistry;

/**
 * Wires the damage system into the game: registers the {@code LivingAttackEvent} / {@code LivingHurtEvent} handlers
 * and, in the dev environment, the {@code combat.*} scenarios. Called once from {@code GunsModule.init}. Both sides
 * (the handlers ignore the client world).
 */
// Spec: docs/design/0009 decision 3; docs/ROADMAP.md (M1, Damage system)
public final class CombatModule {

    private CombatModule() {}

    /** Registers the event handlers and the dev scenarios. */
    public static void init() {
        MinecraftForge.EVENT_BUS.register(new CombatEvents());
        if (DevEnvironment.testHooksEnabled()) {
            CombatScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }
}
