package com.gregguns.worldgen;

/**
 * The natural structures GregGuns can place (M8), with the defaults each one ships with. Pure; no Minecraft types.
 * The id is the layout file name under {@code assets/gregguns/structures/} and the config key suffix.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-15), docs/gregguns-spec/11-worldgen/
// structure-loot.md (ceilings), docs/ROADMAP.md (M8)
public enum StructureId {

    /** Small Factory Building. */
    FACTORY_SMALL("factory_small", true, 1),
    /** Small Train Station. */
    TRAIN_STATION("train_station", true, 1),
    /** Gas Station. */
    GAS_STATION("gas_station", true, 1),
    /** Medium House. */
    HOUSE_MEDIUM("house_medium", true, 1),
    /** Big Bunker. */
    BIG_BUNKER("big_bunker", true, 2),
    /** Alien Bug Nest. */
    ALIEN_BUG_NEST("alien_bug_nest", false, 1),
    /** Military Base. */
    MILITARY_BASE("military_base", true, 2),
    /** Aircraft Carrier. */
    AIRCRAFT_CARRIER("aircraft_carrier", true, 2),
    /** Submarine. */
    SUBMARINE("submarine", true, 2);

    private final String id;
    private final boolean enabledByDefault;
    private final int defaultCeiling;

    StructureId(String id, boolean enabledByDefault, int defaultCeiling) {
        this.id = id;
        this.enabledByDefault = enabledByDefault;
        this.defaultCeiling = defaultCeiling;
    }

    /** @return the layout and config id, for example {@code big_bunker} */
    public String id() {
        return id;
    }

    /** @return whether the structure is placed unless an admin turns it off (false only for the Alien Bug Nest) */
    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    /** @return the default loot tier ceiling: 1 is LV, 2 is MV, 3 is HV (Q7: ships stay at MV) */
    public int defaultCeiling() {
        return defaultCeiling;
    }
}
