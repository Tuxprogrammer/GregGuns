package com.gregguns.worldgen;

import com.gregguns.core.config.ModuleFlags;
import com.gregguns.core.config.WorldgenConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * Scenarios of the worldgen module. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-07, GGS-11-12, GGS-11-15)
public final class WorldgenScenarios {

    private WorldgenScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new ConfigDefaultsScenario());
        StateScenarios.registerAll(registry);
    }

    /** The module is enabled and the config carries the documented defaults. */
    private static final class ConfigDefaultsScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.config_defaults";
        }

        @Override
        public String description() {
            return "The worldgen module is enabled and WorldgenConfig has the documented defaults.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(ModuleFlags.isEnabled(ModuleId.WORLDGEN), "worldgen module flag should be on");
            out.check(WorldgenConfig.spawnStructures, "spawnStructures default true");
            out.check(WorldgenSettings.generatorWanted(), "generator wanted with module and spawnStructures on");
            out.checkEquals("spacing small", 16, WorldgenConfig.spacingSmall);
            out.checkEquals("spacing medium", 32, WorldgenConfig.spacingMedium);
            out.checkEquals("spacing big", 64, WorldgenConfig.spacingBig);
            out.checkEquals("dimension count", 1, WorldgenConfig.structureDimensions.length);
            out.checkEquals("dimension", 0, WorldgenConfig.structureDimensions[0]);
            out.checkEquals("world type count", 4, WorldgenConfig.structureWorldTypes.length);
            out.check(WorldgenSettings.placementAllowed(0, "RWG"), "dim 0 with RWG allowed");
            out.check(WorldgenSettings.placementAllowed(0, "default"), "dim 0 with default allowed");
            out.check(!WorldgenSettings.placementAllowed(0, "flat"), "flat denied");
            out.check(!WorldgenSettings.placementAllowed(-1, "default"), "Nether denied");
            for (StructureId s : StructureId.values()) {
                out.checkEquals(s.id() + " enabled", s.enabledByDefault(), WorldgenSettings.enabled(s));
                out.checkEquals(s.id() + " ceiling", s.defaultCeiling(), WorldgenSettings.lootTierCeiling(s));
                out.checkEquals(s.id() + " richness", 1, WorldgenSettings.lootTier(s));
                out.check(
                    WorldgenSettings.biomeBlacklist(s)
                        .isEmpty(),
                    s.id() + " blacklist empty");
            }
            return out.build();
        }
    }
}
