package com.gregguns.worldgen;

/**
 * The three structure grids. The spacing of each grid (in chunks) comes from the worldgen config and is handed to
 * {@link StructureGrid} by its caller, so this enum carries no numbers.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10)
public enum SizeClass {
    SMALL,
    MEDIUM,
    BIG
}
