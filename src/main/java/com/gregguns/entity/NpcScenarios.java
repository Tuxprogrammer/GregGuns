package com.gregguns.entity;

import com.gregguns.core.config.ModuleFlags;
import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * Scenarios of the NPC core. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-23, GGS-10-24)
public final class NpcScenarios {

    private NpcScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new ModuleConfigScenario());
        NpcEntityScenarios.registerAll(registry);
        NpcLootScenarios.registerAll(registry);
        NpcEggScenarios.registerAll(registry);
        NpcSuperMutantScenarios.registerAll(registry);
        NpcCapScenarios.registerAll(registry);
        NpcBossScenarios.registerAll(registry);
        CyberDemonScenarios.registerAll(registry);
        AttackHelicopterScenarios.registerAll(registry);
        NpcAlienBugScenarios.registerAll(registry);
        NpcTurretScenarios.registerAll(registry);
        NpcInfernalScenarios.registerAll(registry);
        SoldierScenarios.registerAll(registry);
        EnergySoldierScenarios.registerAll(registry);
        NpcSpawnScenarios.registerAll(registry);
        ZombieScenarios.registerAll(registry);
        BanditScenarios.registerAll(registry);
        UndeadScenarios.registerAll(registry);
    }

    /** The module is enabled and the config carries the documented defaults. */
    private static final class ModuleConfigScenario implements Scenario {

        @Override
        public String name() {
            return "npc.module_config";
        }

        @Override
        public String description() {
            return "The npcs module is enabled and NpcConfig has the documented spawn defaults.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(ModuleFlags.isEnabled(ModuleId.NPCS), "npcs module flag should be on");
            out.checkEquals("spawn dimension count", 1, NpcConfig.spawnDimensions.length);
            out.checkEquals("spawn dimension", 0, NpcConfig.spawnDimensions[0]);
            out.checkEquals("per-chunk cap", 4, NpcConfig.perChunkCap);
            out.checkEquals("per-player cap", 8, NpcConfig.perPlayerCap);
            out.check(!NpcConfig.helicopter && !NpcConfig.cyberdemon && !NpcConfig.alienBug, "special mobs off");
            return out.build();
        }
    }
}
