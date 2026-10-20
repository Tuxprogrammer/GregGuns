package com.gregguns.worldgen;

/**
 * What a block means to terrain code. Worldgen height logic never looks at a concrete {@code Block}: the edge
 * adapter ({@link WorldColumnReader}) classifies once, so the probes, the flatten plan and the village scan stay pure
 * and testable with synthetic columns.
 */
public enum BlockKind {

    /** Nothing there. */
    AIR,
    /** Natural terrain: ground, grass, rock, sand, clay. */
    GROUND,
    /** Water or lava. Never removed by terrain shaping (WGEN-26). */
    LIQUID,
    /** Leaves, plants, vines, cactus, snow layers: skipped when looking for the ground. */
    FOLIAGE,
    /** Logs and other wood-material blocks: skipped when looking for the ground (WGEN-33, trunks are not ground). */
    WOOD,
    /** Ice. Skipped like foliage so an ice sheet over water still reads as water (and over land as its floor). */
    ICE,
    /** Planks, cobblestone, glass panes, doors: the village-style blocks GGS-11-14 looks for. */
    VILLAGE,
    /** Any other solid block (ore, brick, machine casing). Counts as ground. */
    SOLID
}
