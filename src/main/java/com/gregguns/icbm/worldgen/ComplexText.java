package com.gregguns.icbm.worldgen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.StatCollector;

/**
 * Text the world generator writes into the world (sign lines, the data stick's name and lore), resolved from the
 * mod's lang keys at generation time. A dedicated server may not have the mod's lang table loaded in
 * {@link StatCollector}, so a key it cannot translate is read from the en_US file inside the jar; the key itself is
 * never written. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Signs)
public final class ComplexText {

    private static final String LANG = "/assets/gregguns/lang/en_US.lang";
    private static Map<String, String> bundled;

    private ComplexText() {}

    /**
     * @param key  a lang key
     * @param args {@link String#format} arguments, or none
     * @return the text, or the key when no lang file knows it
     */
    public static String get(String key, Object... args) {
        String raw = raw(key);
        if (args == null || args.length == 0) {
            return raw;
        }
        try {
            return String.format(raw, args);
        } catch (java.util.IllegalFormatException e) {
            return raw;
        }
    }

    /**
     * @param key  a lang key whose text holds up to four lines divided by {@code |}
     * @param args {@link String#format} arguments, or none
     * @return exactly four lines, empty where the text has fewer
     */
    public static String[] lines(String key, Object... args) {
        String[] parts = get(key, args).split("\\|", -1);
        String[] out = { "", "", "", "" };
        for (int i = 0; i < 4 && i < parts.length; i++) {
            out[i] = parts[i];
        }
        return out;
    }

    private static String raw(String key) {
        if (StatCollector.canTranslate(key)) {
            return StatCollector.translateToLocal(key);
        }
        String v = bundled().get(key);
        return v != null ? v : key;
    }

    private static synchronized Map<String, String> bundled() {
        if (bundled == null) {
            bundled = parse(ComplexText.class.getResourceAsStream(LANG));
        }
        return bundled;
    }

    /** @return the key=value lines of a lang stream (comments and blanks skipped); empty for a null stream */
    static Map<String, String> parse(InputStream in) {
        Map<String, String> map = new HashMap<>();
        if (in == null) {
            return map;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq > 0 && !line.startsWith("#")) {
                    map.put(
                        line.substring(0, eq)
                            .trim(),
                        line.substring(eq + 1));
                }
            }
        } catch (IOException e) {
            // an unreadable table leaves keys untranslated; the callers show the key
        }
        return map;
    }
}
