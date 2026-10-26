package com.gregguns.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.GunsItemOrder;
import com.gregguns.item.NeiOrder;
import com.gregguns.machine.GregGunsMTEIDs;

/**
 * Static check of the in-game guide that ships in the jar ({@code assets/gregguns/guidenh/_en_us}), read by the
 * GuideNH mod through the resource tree alone: no GuideNH class is loaded here or in the mod. A broken page would
 * otherwise only show up when a player opens the guide.
 */
// Spec: docs/gregguns-spec/01-core/index.md (in-game guide, GuideNH)
class GuidePagesTest {

    private static final Path ROOT = Paths.get("src/main/resources/assets/gregguns/guidenh/_en_us");
    private static final String GT_MACHINES = "gregtech:gt.blockmachines";
    private static final Pattern ITEM_TAG = Pattern
        .compile("<(?:ItemImage|ItemLink|ItemIcon)\\b[^>]*?\\bid=\"([^\"]+)\"");
    private static final Pattern LINK = Pattern.compile("\\]\\(([^)\\s]+)\\)");
    private static final Map<String, String> PAGES = new HashMap<>();

    @BeforeAll
    static void load() throws IOException {
        try (Stream<Path> s = Files.list(ROOT)) {
            for (Path p : s.collect(Collectors.toList())) {
                PAGES.put(
                    p.getFileName()
                        .toString(),
                    new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
            }
        }
    }

    private static String frontmatter(String name) {
        String t = PAGES.get(name);
        assertTrue(t.startsWith("---\n"), name + " has no frontmatter");
        int end = t.indexOf("\n---\n", 4);
        assertTrue(end > 0, name + " frontmatter is not closed");
        return t.substring(4, end);
    }

    private static Set<String> machineMetas() throws IllegalAccessException {
        Set<String> ids = new HashSet<>();
        for (Field f : GregGunsMTEIDs.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class
                && !f.getName()
                    .startsWith("BLOCK_")) {
                ids.add(String.valueOf(f.getInt(null)));
            }
        }
        return ids;
    }

    private static void checkItem(String page, String ref, Set<String> names, Set<String> metas) {
        if (ref.startsWith("gregguns:")) {
            assertTrue(names.contains(ref.substring(9)), page + ": not a registered GregGuns name: " + ref);
        } else if (ref.startsWith(GT_MACHINES + ":")) {
            assertTrue(
                metas.contains(ref.substring(GT_MACHINES.length() + 1)),
                page + ": not a GregGuns machine: " + ref);
        } else {
            throw new AssertionError(page + ": unexpected item reference " + ref);
        }
    }

    @Test
    void theGuideHasItsPages() {
        for (String p : new String[] { "index.md", "getting-started.md", "guns.md", "ammo.md", "armor.md",
            "machines.md", "ammunition-works.md", "armory-gantry.md", "rampart.md", "enemies.md",
            "pack-integration.md" }) {
            assertNotNull(PAGES.get(p), "missing page " + p);
        }
    }

    @Test
    void everyPageHasNavigationAndAResolvingParent() {
        for (String name : PAGES.keySet()) {
            assertTrue(name.endsWith(".md"), "not a page: " + name);
            String fm = frontmatter(name);
            assertTrue(fm.contains("navigation:") && fm.contains("  title: "), name + " has no navigation title");
            Matcher m = Pattern.compile("(?m)^  parent: (\\S+)$")
                .matcher(fm);
            if (m.find()) {
                assertTrue(PAGES.containsKey(m.group(1)), name + ": parent does not exist: " + m.group(1));
            } else {
                assertEquals("index.md", name, name + " has no parent");
            }
            assertTrue(
                PAGES.get(name)
                    .contains("\n# "),
                name + " has no heading");
        }
    }

    @Test
    void everyLinkResolves() {
        for (Map.Entry<String, String> e : PAGES.entrySet()) {
            Matcher m = LINK.matcher(e.getValue());
            while (m.find()) {
                String target = m.group(1);
                int hash = target.indexOf('#');
                String file = hash >= 0 ? target.substring(0, hash) : target;
                if (file.isEmpty() || file.startsWith("http")) continue;
                assertTrue(PAGES.containsKey(file), e.getKey() + ": broken link " + target);
            }
        }
    }

    @Test
    void everyReferencedItemExists() throws Exception {
        Set<String> names = new HashSet<>(NeiOrder.names());
        Set<String> metas = machineMetas();
        int seen = 0;
        for (Map.Entry<String, String> e : PAGES.entrySet()) {
            Matcher m = ITEM_TAG.matcher(e.getValue());
            while (m.find()) {
                checkItem(e.getKey(), m.group(1), names, metas);
                seen++;
            }
            Matcher ids = Pattern.compile("(?m)^  - (\\S+)$")
                .matcher(frontmatter(e.getKey()));
            while (ids.find()) {
                checkItem(e.getKey(), ids.group(1), names, metas);
                seen++;
            }
        }
        assertTrue(seen > 100, "suspiciously few item references: " + seen);
    }

    @Test
    void gunsSitUnderTheirRealTier() {
        List<String> bad = new ArrayList<>();
        Map<String, Tier> tiers = new HashMap<>();
        for (GunsItemOrder.Slot s : GunsItemOrder.build()) {
            tiers.put(s.name(), s.tier());
        }
        Tier current = null;
        Set<String> listed = new HashSet<>();
        for (String line : PAGES.get("guns.md")
            .split("\n")) {
            if (line.startsWith("## ")) {
                String head = line.substring(3)
                    .split("[ (]")[0];
                current = Tier.valueOf(head.toUpperCase());
            } else if (line.startsWith("- <ItemImage")) {
                Matcher m = ITEM_TAG.matcher(line);
                assertTrue(m.find());
                String name = m.group(1)
                    .substring(9);
                listed.add(name);
                // Akimbo pairs sit with the tier of their pair's first gun; melee and grenades with their own.
                if (tiers.get(name) != current) bad.add(name + " is " + tiers.get(name) + ", page says " + current);
            }
        }
        assertTrue(bad.isEmpty(), "wrong tier on the guns page: " + bad);
        List<String> missing = new ArrayList<>();
        for (GunDefinition g : GunDefinitions.ALL) {
            if (!listed.contains(g.name())) missing.add(g.name());
        }
        assertTrue(
            missing.isEmpty() || missing.equals(java.util.Arrays.asList("golden_gun")),
            "guns missing from the guns page: " + missing);
    }

    @Test
    void pagesDoNotLeakDevelopmentJargon() {
        for (Map.Entry<String, String> e : PAGES.entrySet()) {
            String t = e.getValue()
                .toLowerCase();
            assertFalse(
                t.contains("techguns") || t.contains("docs/") || t.contains("ggs-") || t.contains("todo"),
                e.getKey() + " mentions development material");
        }
    }

    @Test
    void guideNhIsASoftDependencyByResourcesAlone() throws IOException {
        // GuideNH discovers assets/<modid>/guidenh/_<lang> by itself, so no GregGuns class may touch its API: the mod
        // loads the same with or without GuideNH.
        try (Stream<Path> s = Files.walk(Paths.get("src/main/java"))) {
            for (Path p : s.filter(
                f -> f.toString()
                    .endsWith(".java"))
                .collect(Collectors.toList())) {
                String code = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                assertFalse(code.contains("com.hfstudio.guidenh"), p + " references GuideNH classes");
            }
        }
        assertFalse(
            Files.readAllLines(Paths.get("dependencies.gradle"))
                .stream()
                .anyMatch(
                    l -> l.contains("GuideNH") && !l.trim()
                        .startsWith("//")),
            "GuideNH needs no dependency entry");
    }
}
