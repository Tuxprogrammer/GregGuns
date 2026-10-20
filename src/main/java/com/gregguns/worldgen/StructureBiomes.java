package com.gregguns.worldgen;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

/**
 * Biome questions for structure placement, answered from {@link BiomeDictionary} types only (GGS-11-01: no numeric
 * biome key, no identity test; WGEN-17, WGEN-32). The decision methods take a plain set of types so tests need no
 * biome; {@link #typesAt} is the one edge that reads the world.
 *
 * <p>
 * Blacklists and the bug-nest whitelist are parameters (G1's {@code WorldgenConfig} owns the config lists).
 */
public final class StructureBiomes {

    /** Camp camouflage choice (GGS-11-04). */
    public enum CampColor {
        SNOW,
        DESERT,
        NETHER,
        WOODLAND
    }

    /** Default bug-nest whitelist (GGS-11-03). */
    public static final Set<Type> BUG_NEST_DEFAULT = Collections.unmodifiableSet(asSet(Type.SANDY, Type.WASTELAND));
    /** Types that veto a bug nest even when the whitelist matches: RWG's hot river is SANDY (GGS-11-03). */
    private static final Set<Type> BUG_NEST_EXCLUDED = Collections
        .unmodifiableSet(asSet(Type.RIVER, Type.OCEAN, Type.BEACH));

    private StructureBiomes() {}

    private static Set<Type> asSet(Type... types) {
        Set<Type> s = new HashSet<>();
        Collections.addAll(s, types);
        return s;
    }

    /** GGS-11-02: water is exactly the OCEAN type. A RIVER biome is land. */
    public static boolean isWater(Set<Type> types) {
        return types.contains(Type.OCEAN);
    }

    /** A land structure may go here: not ocean and none of the admin's blacklisted types (GGS-11-05). */
    public static boolean allowsLand(Set<Type> types, Set<Type> blacklist) {
        return !isWater(types) && !intersects(types, blacklist);
    }

    /** A ship may go here: ocean and none of the blacklisted types. */
    public static boolean allowsWater(Set<Type> types, Set<Type> blacklist) {
        return isWater(types) && !intersects(types, blacklist);
    }

    /**
     * GGS-11-03: {@code (any whitelist type) and not (RIVER or OCEAN or BEACH)}. Pass {@link #BUG_NEST_DEFAULT} for
     * the default.
     */
    public static boolean allowsBugNest(Set<Type> types, Set<Type> whitelist) {
        return intersects(types, whitelist) && !intersects(types, BUG_NEST_EXCLUDED);
    }

    /**
     * GGS-11-04, GGS-11-06: COLD or SNOWY is snow, SANDY or MESA is desert, NETHER is nether, anything else
     * (including no types at all) is woodland. BEACH selects nothing: in RWG every ocean base biome is BEACH.
     */
    public static CampColor campColor(Set<Type> types) {
        if (types.contains(Type.COLD) || types.contains(Type.SNOWY)) {
            return CampColor.SNOW;
        }
        if (types.contains(Type.SANDY) || types.contains(Type.MESA)) {
            return CampColor.DESERT;
        }
        if (types.contains(Type.NETHER)) {
            return CampColor.NETHER;
        }
        return CampColor.WOODLAND;
    }

    /** Turns config type names into types; blank names are skipped, unknown names match nothing. */
    public static Set<Type> parseTypes(Iterable<String> names) {
        Set<Type> out = new HashSet<>();
        for (String n : names) {
            if (n != null && !n.trim()
                .isEmpty()) {
                out.add(Type.getType(n.trim()
                    .toUpperCase(Locale.ROOT)));
            }
        }
        return out;
    }

    /**
     * Types of the biome at a block column, read through the chunk manager so no chunk is loaded or generated
     * (WGEN-04). A biome with no registered types, or any registry failure, gives the empty set, which every
     * decision above treats as land and woodland (GGS-11-06).
     */
    public static Set<Type> typesAt(WorldChunkManager manager, int blockX, int blockZ) {
        return typesOf(manager.getBiomeGenAt(blockX, blockZ));
    }

    /** Dictionary types of a biome; empty for null or on failure, never throws (GGS-11-06). */
    public static Set<Type> typesOf(BiomeGenBase biome) {
        if (biome == null) {
            return Collections.emptySet();
        }
        try {
            Type[] types = BiomeDictionary.getTypesForBiome(biome);
            if (types == null || types.length == 0) {
                return Collections.emptySet();
            }
            Set<Type> s = new HashSet<>();
            Collections.addAll(s, types);
            return s;
        } catch (RuntimeException e) {
            return Collections.emptySet();
        }
    }

    private static boolean intersects(Set<Type> a, Set<Type> b) {
        for (Type t : b) {
            if (a.contains(t)) {
                return true;
            }
        }
        return false;
    }
}
