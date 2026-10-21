package com.gregguns.worldgen;

import java.util.Set;

import com.gregguns.core.config.WorldgenConfig;

/**
 * Reads {@link WorldgenConfig} per structure, so other worldgen pieces never name a config field. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-12, GGS-11-15)
public final class WorldgenSettings {

    private WorldgenSettings() {}

    /**
     * @param s a structure
     * @return its {@code enabled} flag
     */
    public static boolean enabled(StructureId s) {
        switch (s) {
            case FACTORY_SMALL:
                return WorldgenConfig.enabledFactorySmall;
            case TRAIN_STATION:
                return WorldgenConfig.enabledTrainStation;
            case GAS_STATION:
                return WorldgenConfig.enabledGasStation;
            case HOUSE_MEDIUM:
                return WorldgenConfig.enabledHouseMedium;
            case BIG_BUNKER:
                return WorldgenConfig.enabledBigBunker;
            case ALIEN_BUG_NEST:
                return WorldgenConfig.enabledAlienBugNest;
            case MILITARY_BASE:
                return WorldgenConfig.enabledMilitaryBase;
            case AIRCRAFT_CARRIER:
                return WorldgenConfig.enabledAircraftCarrier;
            case SUBMARINE:
                return WorldgenConfig.enabledSubmarine;
            default:
                return s.enabledByDefault();
        }
    }

    /**
     * @param s a structure
     * @return its loot tier ceiling, 1 (LV) to 3 (HV)
     */
    public static int lootTierCeiling(StructureId s) {
        switch (s) {
            case FACTORY_SMALL:
                return WorldgenConfig.lootTierCeilingFactorySmall;
            case TRAIN_STATION:
                return WorldgenConfig.lootTierCeilingTrainStation;
            case GAS_STATION:
                return WorldgenConfig.lootTierCeilingGasStation;
            case HOUSE_MEDIUM:
                return WorldgenConfig.lootTierCeilingHouseMedium;
            case BIG_BUNKER:
                return WorldgenConfig.lootTierCeilingBigBunker;
            case ALIEN_BUG_NEST:
                return WorldgenConfig.lootTierCeilingAlienBugNest;
            case MILITARY_BASE:
                return WorldgenConfig.lootTierCeilingMilitaryBase;
            case AIRCRAFT_CARRIER:
                return WorldgenConfig.lootTierCeilingAircraftCarrier;
            case SUBMARINE:
                return WorldgenConfig.lootTierCeilingSubmarine;
            default:
                return s.defaultCeiling();
        }
    }

    /**
     * @param s a structure
     * @return its loot richness, 0 to 2
     */
    public static int lootTier(StructureId s) {
        switch (s) {
            case FACTORY_SMALL:
                return WorldgenConfig.lootTierFactorySmall;
            case TRAIN_STATION:
                return WorldgenConfig.lootTierTrainStation;
            case GAS_STATION:
                return WorldgenConfig.lootTierGasStation;
            case HOUSE_MEDIUM:
                return WorldgenConfig.lootTierHouseMedium;
            case BIG_BUNKER:
                return WorldgenConfig.lootTierBigBunker;
            case ALIEN_BUG_NEST:
                return WorldgenConfig.lootTierAlienBugNest;
            case MILITARY_BASE:
                return WorldgenConfig.lootTierMilitaryBase;
            case AIRCRAFT_CARRIER:
                return WorldgenConfig.lootTierAircraftCarrier;
            case SUBMARINE:
                return WorldgenConfig.lootTierSubmarine;
            default:
                return 1;
        }
    }

    /**
     * @param s a structure
     * @return its biome-type blacklist as upper-case names (blank entries dropped)
     */
    public static Set<String> biomeBlacklist(StructureId s) {
        String[] raw;
        switch (s) {
            case FACTORY_SMALL:
                raw = WorldgenConfig.biomeBlacklistFactorySmall;
                break;
            case TRAIN_STATION:
                raw = WorldgenConfig.biomeBlacklistTrainStation;
                break;
            case GAS_STATION:
                raw = WorldgenConfig.biomeBlacklistGasStation;
                break;
            case HOUSE_MEDIUM:
                raw = WorldgenConfig.biomeBlacklistHouseMedium;
                break;
            case BIG_BUNKER:
                raw = WorldgenConfig.biomeBlacklistBigBunker;
                break;
            case ALIEN_BUG_NEST:
                raw = WorldgenConfig.biomeBlacklistAlienBugNest;
                break;
            case MILITARY_BASE:
                raw = WorldgenConfig.biomeBlacklistMilitaryBase;
                break;
            case AIRCRAFT_CARRIER:
                raw = WorldgenConfig.biomeBlacklistAircraftCarrier;
                break;
            case SUBMARINE:
                raw = WorldgenConfig.biomeBlacklistSubmarine;
                break;
            default:
                raw = null;
                break;
        }
        return WorldgenGate.normalizeNames(raw);
    }

    /**
     * @param dimension the world's dimension id
     * @param worldType the world type name
     * @return whether structures may be placed in this world at all
     */
    public static boolean placementAllowed(int dimension, String worldType) {
        return WorldgenGate.placementAllowed(
            WorldgenConfig.structureDimensions,
            WorldgenConfig.structureWorldTypes,
            dimension,
            worldType);
    }

    /**
     * @return whether the structure generator should be registered now (GGS-11-07)
     */
    public static boolean generatorWanted() {
        return WorldgenGate.shouldRegisterGenerator(
            com.gregguns.core.module.ModuleState.isActive(com.gregguns.core.module.ModuleId.WORLDGEN),
            WorldgenConfig.spawnStructures);
    }
}
