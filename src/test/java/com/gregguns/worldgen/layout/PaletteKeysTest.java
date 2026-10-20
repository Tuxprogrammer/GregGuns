package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.Test;

/** GGS-11-25..27: Chisel is optional, a null field falls back with one warning, and only this class names Chisel. */
class PaletteKeysTest {

    @Test
    void chiselAbsentResolvesEveryKeyToItsVanillaFallback() {
        TestKeys.Blocks vanilla = new TestKeys.Blocks();
        List<String> warnings = new ArrayList<>();
        PaletteKeys keys = PaletteKeys.resolve(null, vanilla, warnings::add);
        ResolvedBlock wall = keys.get("factory_wall");
        assertNotNull(wall);
        assertSame(vanilla.byName("minecraft:iron_block"), wall.block);
        assertTrue(wall.fallback);
        assertTrue(keys.fallbackCount() > 5);
        assertTrue(warnings.isEmpty(), "absent Chisel is normal, not a warning");
        for (String k : keys.roleKeys()) {
            assertNotNull(keys.get(k).block, k);
        }
    }

    @Test
    void chiselFieldIsUsedWhenPresent() {
        TestKeys.Blocks vanilla = new TestKeys.Blocks();
        Block chiselConcrete = new Block(Material.rock) {};
        PaletteKeys keys = PaletteKeys
            .resolve(name -> name.equals("concrete") ? chiselConcrete : null, vanilla, s -> {});
        assertSame(chiselConcrete, keys.get("concrete").block);
        assertFalse(keys.get("concrete").fallback);
        // every other Chisel field was null, so those fell back
        assertTrue(keys.get("brick").fallback);
    }

    @Test
    void aNullFieldWarnsOncePerKeyNamingItAndNeverThrows() {
        List<String> warnings = new ArrayList<>();
        PaletteKeys keys = PaletteKeys.resolve(name -> null, new TestKeys.Blocks(), warnings::add);
        assertEquals(1, count(warnings, "'lantern'"));
        assertEquals(1, count(warnings, "'concrete'"));
        assertNotNull(keys.get("lantern"));
        // "air" has no Chisel field, so it never warns
        assertEquals(0, count(warnings, "'air'"));
    }

    @Test
    void aFallbackThatDoesNotResolveDropsTheKeyWithAWarning() {
        List<String> warnings = new ArrayList<>();
        PaletteKeys keys = PaletteKeys
            .resolve(null, n -> n.equals("minecraft:glowstone") ? null : new Block(Material.rock) {}, warnings::add);
        assertNull(keys.get("lantern"));
        assertFalse(keys.has("lantern"));
        assertEquals(1, count(warnings, "'lantern'"));
    }

    @Test
    void directNamesResolveLazilyAndUnknownOnesDoNot() {
        TestKeys.Blocks vanilla = new TestKeys.Blocks();
        PaletteKeys keys = TestKeys.chiselAbsent(vanilla);
        ResolvedBlock bars = keys.get("minecraft:iron_bars");
        assertNotNull(bars);
        assertSame(bars, keys.get("minecraft:iron_bars"));
        assertNull(keys.get("minecraft:no_such_block"));
        assertNull(keys.get("not_a_key"));
        assertTrue(keys.has("air"));
    }

    private static int count(List<String> lines, String needle) {
        int n = 0;
        for (String s : lines) {
            if (s.contains(needle)) {
                n++;
            }
        }
        return n;
    }

    /** GGS-11-25: scan main sources; only PaletteKeys may name the Chisel package. */
    @Test
    void onlyPaletteKeysNamesChisel() throws IOException {
        Path root = Paths.get("src", "main", "java");
        String needle = "team." + "chisel";
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : (Iterable<Path>) files.filter(
                f -> f.toString()
                    .endsWith(".java"))::iterator) {
                if (p.getFileName()
                    .toString()
                    .equals("PaletteKeys.java")) {
                    continue;
                }
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                if (text.contains(needle)) {
                    offenders.add(p.toString());
                }
            }
        }
        assertTrue(offenders.isEmpty(), "Chisel is named outside PaletteKeys: " + offenders);
    }
}
