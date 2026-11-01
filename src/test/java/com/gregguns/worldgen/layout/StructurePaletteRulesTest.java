package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The palette rules of design record 0025, over every shipped layout and role: a weighted entry draws from one family,
 * a role takes its own meta, no role is a sign or an animated overlay, the block a role places in the pack is not one
 * GregTech's stone layers overwrite, every chain ends in a block that always exists, and the Gas Station canopy carries
 * a whole railing.
 */
class StructurePaletteRulesTest {

    private static final Path DIR = Paths.get("src", "main", "resources", "assets", "gregguns", "structures");

    /** Blocks GregTech's stone layer generator replaces wherever a blob passes (see PaletteSets.GT_REPLACEABLE). */
    private static final Set<String> GT_REPLACEABLE = new HashSet<>(
        Arrays.asList(
            "minecraft:stone",
            "minecraft:stained_hardened_clay",
            "minecraft:cobblestone",
            "gregtech:gt.blockgranites",
            "gregtech:gt.blockstones"));

    /**
     * Direct blocks that count as one family when mixed: rubble for a track bed, leaves of two trees for a camo roof.
     */
    private static final Map<String, String> DIRECT_FAMILY = new java.util.HashMap<>();

    static {
        for (String rubble : Arrays
            .asList("minecraft:gravel", "minecraft:cobblestone", "minecraft:mossy_cobblestone")) {
            DIRECT_FAMILY.put(rubble, "rubble");
        }
    }

    private static List<String> shippedIds() throws IOException {
        List<String> ids = new ArrayList<>();
        try (Stream<Path> files = Files.list(DIR)) {
            files.map(
                p -> p.getFileName()
                    .toString())
                .filter(n -> n.endsWith(".txt"))
                .forEach(n -> ids.add(n.substring(0, n.length() - 4)));
        }
        Collections.sort(ids);
        return ids;
    }

    private static LayoutRegistry loadAll(List<String> errors) throws IOException {
        return LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            shippedIds(),
            TestKeys.chiselAbsent(new TestKeys.Blocks()),
            errors::add);
    }

    private static String familyOf(PaletteChoice c) {
        if (c.key.indexOf(':') < 0) return PaletteKeys.family(c.key);
        return DIRECT_FAMILY.getOrDefault(c.key, c.key);
    }

    @Test
    void everyShippedLayoutLoads() throws IOException {
        List<String> errors = new ArrayList<>();
        LayoutRegistry reg = loadAll(errors);
        assertTrue(errors.isEmpty(), "layout errors: " + errors);
        assertTrue(shippedIds().size() > 40, "the scan saw the shipped layouts");
        for (String id : shippedIds()) assertNotNull(reg.get(id), id);
    }

    @Test
    void aWeightedEntryDrawsFromOneFamily() throws IOException {
        LayoutRegistry reg = loadAll(new ArrayList<>());
        List<String> bad = new ArrayList<>();
        int mixed = 0;
        for (String id : shippedIds()) {
            Layout l = reg.get(id);
            for (PaletteEntry e : l.entries()) {
                Set<String> families = new TreeSet<>();
                for (PaletteChoice c : e.choices()) {
                    if (!c.key.equals("air")) families.add(familyOf(c));
                }
                if (e.choices()
                    .size() > 1) mixed++;
                if (families.size() > 1) bad.add(id + " " + families);
            }
        }
        assertTrue(mixed > 10, "the scan saw the weighted entries");
        assertTrue(bad.isEmpty(), "entries mixing families: " + bad);
    }

    @Test
    void aRoleKeyWithAMetaIsRejected() {
        List<String> errors = new ArrayList<>();
        LayoutRegistry.Source src = id -> "gregguns-layout 1\nid: tinted\nsize: 1 1 1\npalette:\nA = concrete#4\nlayers:\nlayer 0\nA\n";
        LayoutRegistry reg = LayoutRegistry
            .load(src, Arrays.asList("tinted"), TestKeys.chiselAbsent(new TestKeys.Blocks()), errors::add);
        assertFalse(reg.isEnabled("tinted"));
        assertEquals(1, errors.size());
        assertTrue(
            errors.get(0)
                .contains("takes its own meta"));
    }

    @Test
    void noRoleIsASignOrAnAnimatedOverlay() {
        // The first palette used Chisel's radiation sign as hazard trim, its glowing hex plating as hull and roof, and
        // its flame lantern as every light: icons and animations repeated over a wall are what read as jarring.
        Set<String> banned = new HashSet<>(
            Arrays.asList("chisel:warningSign", "chisel:hexPlating", "chisel:hexLargePlating", "chisel:lantern"));
        for (Map.Entry<String, List<String>> e : PaletteKeys.table()
            .entrySet()) {
            for (String cand : e.getValue()) {
                String name = cand.substring(0, cand.indexOf('#'));
                assertFalse(banned.contains(name), e.getKey() + " uses " + cand);
            }
        }
        assertTrue(
            PaletteKeys.table()
                .get("hazard")
                .get(0)
                .equals("chisel:factoryblock#6"),
            "hazard is Chisel's hazard stripe");
    }

    @Test
    void thePackBlockOfARoleIsNotOneGregTechStoneLayersOverwrite() {
        for (Map.Entry<String, List<String>> e : PaletteKeys.table()
            .entrySet()) {
            String first = e.getValue()
                .get(0);
            String name = first.substring(0, first.indexOf('#'));
            assertFalse(GT_REPLACEABLE.contains(name), e.getKey() + " places " + first + " in the pack");
        }
    }

    @Test
    void layoutsNameNoGtReplaceableBlockExceptTheTrackBed() throws IOException {
        LayoutRegistry reg = loadAll(new ArrayList<>());
        List<String> bad = new ArrayList<>();
        for (String id : shippedIds()) {
            for (PaletteEntry e : reg.get(id)
                .entries()) {
                for (PaletteChoice c : e.choices()) {
                    if (!GT_REPLACEABLE.contains(c.key)) continue;
                    // Ground fill under the rails: a granite blob through it still reads as a track bed.
                    if (id.equals("train_station") && "rubble".equals(familyOf(c))) continue;
                    bad.add(id + " " + c.key);
                }
            }
        }
        assertTrue(bad.isEmpty(), "GT-replaceable blocks: " + bad);
    }

    @Test
    void everyChainEndsInABlockThatAlwaysExists() {
        for (Map.Entry<String, List<String>> e : PaletteKeys.table()
            .entrySet()) {
            List<String> chain = e.getValue();
            String last = chain.get(chain.size() - 1);
            assertTrue(last.startsWith("minecraft:"), e.getKey() + " ends in " + last);
            assertNotNull(PaletteKeys.family(e.getKey()), e.getKey());
        }
    }

    /** The owner's "saloon" roof: the canopy carries a whole iron-bar railing, not three stray blocks in a corner. */
    @Test
    void gasStationCanopyCarriesAWholeRailing() throws IOException {
        Layout l = loadAll(new ArrayList<>()).get("gas_station");
        int top = l.sizeY() - 1;
        int roof = top - 1;
        int x0 = Integer.MAX_VALUE, x1 = -1, z0 = Integer.MAX_VALUE, z1 = -1;
        // The canopy is the roof layer's plated rectangle at the front (south) of the station.
        for (int x = 0; x < l.sizeX(); x++) {
            for (int z = 0; z < l.sizeZ(); z++) {
                int i = l.cell(x, roof, z);
                if (i == Layout.SKIP) continue;
                if (!l.entry(i)
                    .choices()
                    .get(0).key.equals("plating")) continue;
                x0 = Math.min(x0, x);
                x1 = Math.max(x1, x);
                z0 = Math.min(z0, z);
                z1 = Math.max(z1, z);
            }
        }
        assertTrue(x1 - x0 >= 8 && z1 - z0 >= 5, "found the canopy: " + x0 + ".." + x1 + ", " + z0 + ".." + z1);
        int rail = 0;
        for (int x = 0; x < l.sizeX(); x++) {
            for (int z = 0; z < l.sizeZ(); z++) {
                int i = l.cell(x, top, z);
                boolean edge = x >= x0 && x <= x1 && z >= z0 && z <= z1 && (x == x0 || x == x1 || z == z0 || z == z1);
                String key = i == Layout.SKIP ? null
                    : l.entry(i)
                        .choices()
                        .get(0).key;
                if (edge) {
                    assertEquals("minecraft:iron_bars", key, "railing at " + x + "," + z);
                    rail++;
                } else {
                    assertEquals(null, key, "nothing else on the roof at " + x + "," + z);
                }
            }
        }
        assertEquals(2 * (x1 - x0 + 1) + 2 * (z1 - z0 - 1), rail);
    }
}
