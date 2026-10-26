package com.gregguns.core.text;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Translation readiness (M9): {@code en_US.lang} is the source of truth, and every other {@code .lang} file in
 * {@code assets/gregguns/lang/} is a community translation that must stay compatible with it. See
 * {@code docs/translating.md}.
 *
 * <p>
 * What this guards: (1) every lang-key literal in the Java sources has an English entry, so a tooltip, name, GUI label
 * or chat line can never ship as a raw key; (2) a translation has no key English lacks and no key whose
 * {@code %s}-style
 * placeholders differ from English (a mismatch throws or scrambles the line in game); (3) no file defines a key twice.
 * Registered item, block and entity names are built from dynamic names, so they are checked at boot by
 * {@code ContentChecks}, which reads the same file.
 */
class LangFilesTest {

    private static final Path LANG_DIR = Paths.get("src/main/resources/assets/gregguns/lang");
    private static final Path JAVA_DIR = Paths.get("src/main/java");

    /**
     * Literals that look like one of our lang keys. A trailing {@code .} or {@code _} marks a prefix built up later.
     */
    private static final Pattern KEY_LITERAL = Pattern.compile(
        "\"((?:gregguns|item\\.gregguns|tile\\.gregguns|entity\\.gregguns|gui\\.gregguns|key\\.gregguns"
            + "|key\\.categories\\.gregguns|container\\.gregguns|itemGroup\\.gregguns|death\\.attack\\.gregguns"
            + "|gt\\.blockmachines\\.gregguns)(?:\\.[A-Za-z0-9_]+)+)\"");

    /** {@code %s}, {@code %1$s}, {@code %d}, {@code %.1f}; {@code %%} and {@code %n} take no argument. */
    private static final Pattern SPECIFIER = Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z%]");

    /** Literals that look like a key but are not one (config categories, channels and the like). */
    private static final java.util.Set<String> NOT_LANG_KEYS = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "gregguns.autotest",
            "gregguns.autotest.connect",
            "gregguns.explosions",
            "gregguns.projectile",
            "gregguns.showcase.player",
            "gregguns.soak.regressionMs",
            "gregguns.testHooks",
            "gregguns.test_melee"));

    /**
     * Parses a {@code .lang} file the way Forge does: {@code key=value}, {@code #} comments, first {@code =} splits.
     */
    static Map<String, String> parse(Path file, List<String> duplicates) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            int eq = trimmed.indexOf('=');
            if (trimmed.isEmpty() || trimmed.startsWith("#") || eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq)
                .trim();
            if (out.put(key, trimmed.substring(eq + 1)) != null) {
                duplicates.add(file.getFileName() + ": " + key);
            }
        }
        return out;
    }

    /** @return the placeholders in a value, sorted, so argument order may change but the count and types may not */
    static List<String> specifiers(String value) {
        List<String> out = new ArrayList<>();
        Matcher m = SPECIFIER.matcher(value);
        while (m.find()) {
            String spec = m.group();
            if (!spec.endsWith("%") && !spec.endsWith("n")) {
                // %1$s and %s differ only in position; compare conversions and the explicit index.
                out.add(spec);
            }
        }
        Collections.sort(out);
        return out;
    }

    private static List<Path> langFiles() throws IOException {
        try (Stream<Path> s = Files.list(LANG_DIR)) {
            return s.filter(
                p -> p.getFileName()
                    .toString()
                    .endsWith(".lang"))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    @Test
    void englishExistsAndHasNoDuplicateKeys() throws IOException {
        Path en = LANG_DIR.resolve("en_US.lang");
        assertTrue(Files.exists(en), "en_US.lang is the source of truth");
        List<String> dupes = new ArrayList<>();
        Map<String, String> keys = parse(en, dupes);
        assertTrue(dupes.isEmpty(), "duplicate keys: " + dupes);
        keys.forEach(
            (k, v) -> assertFalse(
                v.trim()
                    .isEmpty(),
                "empty English value for " + k));
    }

    @Test
    void everyKeyLiteralInTheSourcesHasAnEnglishEntry() throws IOException {
        Map<String, String> en = parse(LANG_DIR.resolve("en_US.lang"), new ArrayList<>());
        TreeSet<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(JAVA_DIR)) {
            for (Path file : (Iterable<Path>) files.filter(
                p -> p.toString()
                    .endsWith(".java"))::iterator) {
                Matcher m = KEY_LITERAL.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                while (m.find()) {
                    String key = m.group(1);
                    if (!en.containsKey(key) && !NOT_LANG_KEYS.contains(key)
                        && !isPrefixOfKnownKey(en, key)
                        && !en.containsKey("gt.blockmachines." + key + ".name")) {
                        missing.add(key + "  (" + JAVA_DIR.relativize(file) + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "lang keys used in code but missing from en_US.lang: " + missing);
    }

    /** A literal such as {@code "gregguns.turret.mode"} that is completed with a suffix at the call site. */
    private static boolean isPrefixOfKnownKey(Map<String, String> en, String key) {
        for (String k : en.keySet()) {
            // "gregguns.turret.tier.0" + (i + 1) is completed with a digit.
            if (k.startsWith(key + ".") || k.startsWith(key + "_") || k.startsWith("gt.blockmachines." + key)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void translationsHaveNoKeysEnglishLacksAndMatchingPlaceholders() throws IOException {
        Map<String, String> en = parse(LANG_DIR.resolve("en_US.lang"), new ArrayList<>());
        List<String> problems = new ArrayList<>();
        for (Path file : langFiles()) {
            if (file.getFileName()
                .toString()
                .equals("en_US.lang")) {
                continue;
            }
            List<String> dupes = new ArrayList<>();
            Map<String, String> other = parse(file, dupes);
            dupes.forEach(d -> problems.add("duplicate key " + d));
            for (Map.Entry<String, String> e : other.entrySet()) {
                String english = en.get(e.getKey());
                if (english == null) {
                    problems.add(file.getFileName() + ": key not in en_US.lang: " + e.getKey());
                } else if (!specifiers(english).equals(specifiers(e.getValue()))) {
                    problems.add(
                        file.getFileName() + ": placeholders differ from English for "
                            + e.getKey()
                            + ": "
                            + specifiers(english)
                            + " vs "
                            + specifiers(e.getValue()));
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void placeholderComparisonCatchesTheUsualMistakes() {
        assertTrue(specifiers("Usage: /gregguns <%s>").equals(specifiers("Utilisation : /gregguns <%s>")));
        assertTrue(specifiers("100%% of %s%n").equals(specifiers("%s 100%%")));
        assertFalse(specifiers("%s of %s").equals(specifiers("%s")));
        assertFalse(specifiers("%d shots").equals(specifiers("%s shots")));
    }

    /**
     * BetterQuesting reads {@code betterquesting.quest.<id>.name/.desc} before the inline text
     * ({@code QuestTranslation}, BQ 3.8.89-GTNH), so the lang file carries each quest's text for translators. The block
     * is generated by {@code tools/questbook/build_quests.py --lang}; this fails when it is stale.
     */
    @Test
    void questLangKeysMatchTheQuestFiles() throws IOException {
        Map<String, String> en = parse(LANG_DIR.resolve("en_US.lang"), new ArrayList<>());
        Path quests = Paths.get("src/main/resources/assets/gregguns/questbook/Quests");
        Pattern fileName = Pattern.compile(".*-([A-Za-z0-9_-]{22})==\\.json");
        List<String> problems = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> files = Files.walk(quests)) {
            for (Path file : (Iterable<Path>) files.filter(
                p -> p.toString()
                    .endsWith(".json"))::iterator) {
                Matcher m = fileName.matcher(
                    file.getFileName()
                        .toString());
                assertTrue(m.matches(), "unexpected quest file name " + file);
                com.google.gson.JsonObject props = new com.google.gson.JsonParser()
                    .parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonObject("properties:10")
                    .getAsJsonObject("betterquesting:10");
                for (String field : new String[] { "name", "desc" }) {
                    String expected = props.get(field + ":8")
                        .getAsString()
                        .replace("%", "%%")
                        .replace("\r", "")
                        .replace("\n", "%n");
                    String key = "betterquesting.quest." + m.group(1) + "." + field;
                    if (!expected.equals(en.get(key))) {
                        problems.add(key + " should be '" + expected + "' but is '" + en.get(key) + "'");
                    }
                }
                checked++;
            }
        }
        assertTrue(checked > 100, "quest files found: " + checked);
        assertTrue(problems.isEmpty(), "run tools/questbook/build_quests.py with --lang: " + problems);
    }
}
