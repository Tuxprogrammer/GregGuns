package com.gregguns.icbm.complex.gen;

import java.util.HashMap;
import java.util.Map;

/**
 * When a cell the complex wrote still holds what it wrote (D348). Pure: blocks by registry name, with the few kinds of
 * block whose own life changes them told apart by {@link Kind}, which the world side reads from the block.
 *
 * <p>
 * A cell is intact when it holds the same block and meta. The same block with another meta is intact too when the
 * block changes its own meta in play: a tile entity's block (a chest's or a machine's facing; its contents are never
 * compared), a plant or crop that grows, a door or a lever, a liquid. A block that changes into its partner block in
 * play (a lamp or furnace lighting up or going out, a torch, a repeater or comparator switching, flowing and still
 * liquid) is intact in either state, and so is a falling block (gravel rubble) that has fallen out of its cell, and the
 * empty cell it landed in. Every
 * other difference was made by something that ran after the complex: an ore vein, a stone layer, another mod's
 * populate.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface: the complex keep)
public final class KeepRule {

    private KeepRule() {}

    /** How a block's own life changes it. */
    public enum Kind {
        /** Never changes by itself: block and meta must match. */
        PLAIN,
        /** Carries a tile entity: its block must match, its meta and contents are its own. */
        TILE,
        /** Changes its own meta (grows, opens, switches): its block must match. */
        STATEFUL,
        /** A liquid: flows, so only the liquid matters. */
        LIQUID,
        /** Falls when unsupported: may leave its cell empty. */
        FALLING
    }

    static final String AIR = "minecraft:air";

    /** Blocks that turn into a partner block in play, mapped to one name for the pair. */
    private static final Map<String, String> FAMILY = new HashMap<String, String>();

    static {
        pair("minecraft:lit_redstone_lamp", "minecraft:redstone_lamp");
        pair("minecraft:lit_furnace", "minecraft:furnace");
        pair("minecraft:unlit_redstone_torch", "minecraft:redstone_torch");
        pair("minecraft:powered_repeater", "minecraft:unpowered_repeater");
        pair("minecraft:powered_comparator", "minecraft:unpowered_comparator");
        pair("minecraft:lit_redstone_ore", "minecraft:redstone_ore");
        pair("minecraft:flowing_water", "minecraft:water");
        pair("minecraft:flowing_lava", "minecraft:lava");
    }

    private static void pair(String state, String base) {
        FAMILY.put(state, base);
    }

    /** @return the name both blocks of a pair share, or the name itself */
    static String family(String name) {
        String f = FAMILY.get(name);
        return f == null ? name : f;
    }

    /**
     * @param kept      the block name the complex wrote
     * @param keptMeta  its meta
     * @param keptKind  how that block changes by itself
     * @param world     the block name the world holds now
     * @param worldMeta its meta
     * @param worldKind how the world's block changes by itself
     * @return whether the cell still holds what the complex wrote
     */
    public static boolean intact(String kept, int keptMeta, Kind keptKind, String world, int worldMeta,
        Kind worldKind) {
        if (kept.equals(world)) {
            return keptMeta == worldMeta || keptKind == Kind.TILE
                || keptKind == Kind.STATEFUL
                || keptKind == Kind.LIQUID;
        }
        if (family(kept).equals(family(world))) return true;
        // Rubble that fell: its old cell is empty and the cell it landed in holds it.
        return keptKind == Kind.FALLING && AIR.equals(world) || worldKind == Kind.FALLING && AIR.equals(kept);
    }
}
