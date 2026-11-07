package com.gregguns.worldgen;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The structure gates as pure functions: the dimension allow-list, the world-type allow-list, the registration rule and
 * the biome-type blacklist. No Minecraft types and no config reads, so unit tests pass the lists in. No dimension id or
 * world type name is a literal here: everything comes from the lists (GGS-11-12).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-07, GGS-11-12, GGS-11-15); docs/gregguns-spec/
// 11-worldgen/rwg-biomes.md (RWG world type)
public final class WorldgenGate {

    private WorldgenGate() {}

    /**
     * @param dimensions the configured allow-list
     * @param dimension  the dimension id of the world
     * @return true if the dimension is listed; a null or empty list allows nothing
     */
    public static boolean dimensionAllowed(int[] dimensions, int dimension) {
        if (dimensions == null) {
            return false;
        }
        for (int d : dimensions) {
            if (d == dimension) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param dimensions     the configured allow-list
     * @param ross128        the Ross128b and Ross128ba IDs read from BartWorks' config (empty when unavailable)
     * @param includeRoss128 {@code worldgen.structureRoss128}
     * @param dimension      the dimension id of the world
     * @return true if the list allows it, or Ross128 is included and this is one of its dimensions (design record
     *         0019). The Ross128 IDs come from the caller, never from a literal here.
     */
    public static boolean dimensionAllowed(int[] dimensions, int[] ross128, boolean includeRoss128, int dimension) {
        return dimensionAllowed(dimensions, dimension) || (includeRoss128 && dimensionAllowed(ross128, dimension));
    }

    /**
     * A blacklist, not an allow-list, so a terrain mod the pack adds later gets structures with no config change. GTNH
     * 2.9.0 added RWG2 next to RWG, and an allow-list naming only RWG left RWG2 worlds with no structures at all.
     *
     * @param blacklist the configured blacklist of {@code WorldType.getWorldTypeName()} values; null or empty blocks
     *                  nothing
     * @param worldType the type name of the world; null or blank means unknown
     * @return false if the name is listed (ignoring case and surrounding blanks) or unknown, otherwise true
     */
    // Source: RWG registers its type as new WorldTypeRealistic("RWG"):
    // https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/RWG.java#L32
    // Source: RWG2 registers "RWG2" and "RWG2_CONTINENTS" (the GTNH 2.9.0 default):
    // https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/RWG.java#L39-L40
    public static boolean worldTypeAllowed(String[] blacklist, String worldType) {
        if (worldType == null) {
            return false;
        }
        String wanted = worldType.trim();
        if (wanted.isEmpty()) {
            return false;
        }
        if (blacklist == null) {
            return true;
        }
        for (String listed : blacklist) {
            if (listed != null && listed.trim()
                .equalsIgnoreCase(wanted)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param dimensions         the dimension allow-list
     * @param worldTypeBlacklist the world-type blacklist
     * @param dimension          the dimension id of the world
     * @param worldType          the type name of the world
     * @return true only if the dimension is listed and the world type is not blacklisted
     */
    public static boolean placementAllowed(int[] dimensions, String[] worldTypeBlacklist, int dimension,
        String worldType) {
        return dimensionAllowed(dimensions, dimension) && worldTypeAllowed(worldTypeBlacklist, worldType);
    }

    /**
     * As {@link #placementAllowed(int[], String[], int, String)}, with the Ross128 dimensions allowed on top of the
     * list when {@code includeRoss128} is set (design record 0019).
     */
    public static boolean placementAllowed(int[] dimensions, int[] ross128, boolean includeRoss128,
        String[] worldTypeBlacklist, int dimension, String worldType) {
        return dimensionAllowed(dimensions, ross128, includeRoss128, dimension)
            && worldTypeAllowed(worldTypeBlacklist, worldType);
    }

    /**
     * @param moduleOn        {@code modules.worldgen}
     * @param spawnStructures {@code worldgen.spawnStructures}
     * @return true if the structure generator may be registered (GGS-11-07)
     */
    public static boolean shouldRegisterGenerator(boolean moduleOn, boolean spawnStructures) {
        return moduleOn && spawnStructures;
    }

    /**
     * @param names raw config names, may be null
     * @return the trimmed, upper-cased, non-blank names as an unmodifiable set
     */
    public static Set<String> normalizeNames(String[] names) {
        if (names == null) {
            return Collections.emptySet();
        }
        Set<String> out = new HashSet<>();
        for (String name : names) {
            if (name != null && !name.trim()
                .isEmpty()) {
                out.add(
                    name.trim()
                        .toUpperCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * @param blacklist  normalized blacklist names
     * @param biomeTypes the dictionary type names of the biome, any case
     * @return true if any of the biome types is blacklisted
     */
    public static boolean biomeBlocked(Set<String> blacklist, Collection<String> biomeTypes) {
        if (blacklist == null || blacklist.isEmpty() || biomeTypes == null) {
            return false;
        }
        for (String type : biomeTypes) {
            if (type != null && blacklist.contains(type.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
