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
     * @param worldTypes the configured allow-list of {@code WorldType.getWorldTypeName()} values
     * @param worldType  the type name of the world; null means unknown
     * @return true if the name is listed (ignoring case and surrounding blanks); unknown and unlisted types are out
     */
    // Source: RWG registers its type as new WorldTypeRealistic("RWG"):
    // https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/RWG.java#L31
    public static boolean worldTypeAllowed(String[] worldTypes, String worldType) {
        if (worldTypes == null || worldType == null) {
            return false;
        }
        String wanted = worldType.trim();
        if (wanted.isEmpty()) {
            return false;
        }
        for (String listed : worldTypes) {
            if (listed != null && listed.trim()
                .equalsIgnoreCase(wanted)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param dimensions the dimension allow-list
     * @param worldTypes the world-type allow-list
     * @param dimension  the dimension id of the world
     * @param worldType  the type name of the world
     * @return true only if both lists allow it
     */
    public static boolean placementAllowed(int[] dimensions, String[] worldTypes, int dimension, String worldType) {
        return dimensionAllowed(dimensions, dimension) && worldTypeAllowed(worldTypes, worldType);
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
