package com.gregguns.icbm.complex.palette;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.gregguns.worldgen.layout.ResolvedBlock;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Turns a {@link BlockRef} into a block through the Forge registry, on first use, never at class load (D241). A block
 * that is not registered falls to the next entry of the chain, and the results are cached. Server side only.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Resolution); D241
// Source: GameRegistry.findBlock(modId, name) returns null for an unknown block (Forge 1.7.10 GameRegistry).
public final class PaletteResolver {

    private static final ConcurrentMap<BlockRef, ResolvedBlock> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Object> SUPPLIERS = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    private PaletteResolver() {}

    /** @return the block of the first entry that is registered; stone when not even vanilla is (never expected) */
    public static ResolvedBlock resolve(BlockRef ref) {
        ResolvedBlock hit = CACHE.get(ref);
        if (hit != null) {
            return hit;
        }
        ResolvedBlock r = lookup(ref);
        CACHE.put(ref, r);
        return r;
    }

    private static ResolvedBlock lookup(BlockRef ref) {
        boolean first = true;
        for (BlockRef.Entry e : ref.chain()) {
            Block b = find(e.modId, e.name);
            if (b != null) {
                return new ResolvedBlock(b, e.meta, !first);
            }
            first = false;
        }
        return new ResolvedBlock(Blocks.stone, 0, true);
    }

    private static Block find(String modId, String name) {
        String key = modId + ":" + name;
        Object c = SUPPLIERS.get(key);
        if (c == null) {
            Block b = GameRegistry.findBlock(modId, name);
            SUPPLIERS.put(key, b == null ? NONE : b);
            return b;
        }
        return c == NONE ? null : (Block) c;
    }

    /** @return the registry-backed lookup, for logging which mod supplied what */
    public static BlockRef.Lookup registry() {
        return (m, n) -> find(m, n) != null;
    }

    /** Drops the caches (a scenario that changes what is registered). */
    public static void clear() {
        CACHE.clear();
        SUPPLIERS.clear();
    }
}
