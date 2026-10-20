package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraftforge.common.BiomeDictionary.Type;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.StructureBiomes.CampColor;

/** GGS-11-01 to 06 over the RWG base-biome rows of rwg-biomes.md, as plain type sets. */
class StructureBiomesTest {

    private static Set<Type> t(Type... types) {
        return new HashSet<>(Arrays.asList(types));
    }

    private static final Set<Type> NONE = Collections.emptySet();
    private static final Set<Type> TEMPERATE_OCEAN = t(Type.OCEAN, Type.BEACH);
    private static final Set<Type> HOT_RIVER = t(Type.RIVER, Type.HOT, Type.SANDY);
    private static final Set<Type> SNOW_DESERT = t(Type.COLD, Type.SNOWY, Type.WASTELAND);
    private static final Set<Type> COLD_PLAINS = t(Type.COLD, Type.WASTELAND);
    private static final Set<Type> HOT_DESERT = t(Type.HOT, Type.DRY, Type.SANDY);
    private static final Set<Type> HOT_PLAINS = t(Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE);
    private static final Set<Type> PLAINS = t(Type.PLAINS);

    @Test
    void waterIsOceanAndRiverIsLand() {
        assertTrue(StructureBiomes.isWater(TEMPERATE_OCEAN));
        assertFalse(StructureBiomes.isWater(HOT_RIVER));
        assertFalse(StructureBiomes.isWater(PLAINS));
        assertTrue(StructureBiomes.allowsLand(HOT_RIVER, NONE));
        assertFalse(StructureBiomes.allowsLand(TEMPERATE_OCEAN, NONE));
        assertTrue(StructureBiomes.allowsWater(TEMPERATE_OCEAN, NONE));
        assertFalse(StructureBiomes.allowsWater(PLAINS, NONE));
    }

    @Test
    void bugNestDefaultExcludesRiverOceanBeach() {
        Set<Type> d = StructureBiomes.BUG_NEST_DEFAULT;
        assertTrue(StructureBiomes.allowsBugNest(HOT_DESERT, d));
        assertTrue(StructureBiomes.allowsBugNest(SNOW_DESERT, d));
        assertTrue(StructureBiomes.allowsBugNest(COLD_PLAINS, d));
        // RWG's hot river is SANDY but a river: vetoed.
        assertFalse(StructureBiomes.allowsBugNest(HOT_RIVER, d));
        assertFalse(StructureBiomes.allowsBugNest(t(Type.SANDY, Type.BEACH), d));
        assertFalse(StructureBiomes.allowsBugNest(t(Type.SANDY, Type.OCEAN), d));
        assertFalse(StructureBiomes.allowsBugNest(HOT_PLAINS, d));
        assertFalse(StructureBiomes.allowsBugNest(NONE, d));
    }

    @Test
    void bugNestWhitelistIsAParameter() {
        assertTrue(StructureBiomes.allowsBugNest(PLAINS, t(Type.PLAINS)));
        assertFalse(StructureBiomes.allowsBugNest(HOT_DESERT, t(Type.PLAINS)));
    }

    @Test
    void campColorFollowsTypesNotBeach() {
        assertEquals(CampColor.SNOW, StructureBiomes.campColor(SNOW_DESERT));
        assertEquals(CampColor.SNOW, StructureBiomes.campColor(COLD_PLAINS));
        assertEquals(CampColor.DESERT, StructureBiomes.campColor(HOT_DESERT));
        assertEquals(CampColor.DESERT, StructureBiomes.campColor(t(Type.MESA)));
        assertEquals(CampColor.NETHER, StructureBiomes.campColor(t(Type.NETHER)));
        assertEquals(CampColor.WOODLAND, StructureBiomes.campColor(HOT_PLAINS));
        assertEquals(CampColor.WOODLAND, StructureBiomes.campColor(PLAINS));
        // BEACH alone selects nothing (every RWG ocean base biome is BEACH): not desert.
        assertEquals(CampColor.WOODLAND, StructureBiomes.campColor(TEMPERATE_OCEAN));
        // Snow outranks sand, as the Techguns list order does.
        assertEquals(CampColor.SNOW, StructureBiomes.campColor(t(Type.SNOWY, Type.SANDY)));
    }

    @Test
    void noTypesIsLandAndWoodlandAndNeverThrows() {
        assertFalse(StructureBiomes.isWater(NONE));
        assertTrue(StructureBiomes.allowsLand(NONE, t(Type.MUSHROOM)));
        assertEquals(CampColor.WOODLAND, StructureBiomes.campColor(NONE));
        assertTrue(
            StructureBiomes.typesOf(null)
                .isEmpty());
    }

    @Test
    void blacklistExcludesByType() {
        Set<Type> black = StructureBiomes.parseTypes(Arrays.asList(" river ", "MUSHROOM", ""));
        assertEquals(2, black.size());
        assertFalse(StructureBiomes.allowsLand(HOT_RIVER, black));
        assertFalse(StructureBiomes.allowsLand(t(Type.MUSHROOM), black));
        assertTrue(StructureBiomes.allowsLand(PLAINS, black));
        assertFalse(StructureBiomes.allowsWater(t(Type.OCEAN, Type.RIVER), black));
    }

    /** GGS-11-01: worldgen classes never touch a numeric biome key. */
    @Test
    void noBiomeIdInWorldgenSources() throws IOException {
        Path dir = Paths.get("src/main/java/com/gregguns/worldgen");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files.filter(
                f -> f.toString()
                    .endsWith(".java"))::iterator) {
                String src = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).toLowerCase();
                assertFalse(src.contains("biomeid"), p + " mentions a biome id");
                assertFalse(src.contains("biomegenbase.ocean"), p + " compares biome identity");
            }
        }
    }
}
