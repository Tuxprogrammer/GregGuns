package com.gregguns.worldgen.layout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The metas Chisel registers a variation for, per block. Chisel draws its missing texture for a meta it does not
 * define (for example meta 0 of its glass pane, which is the vanilla pane's slot), so palette code asks here before it
 * places a Chisel block and substitutes its vanilla fallback when the pair is not valid. Blocks the list does not name
 * are allowed. The list is generated from Chisel's source by {@code tools/chisel-variants/gen.py}.
 */
// Source: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java
public final class ChiselVariants {

    private static final String RESOURCE = "/assets/gregguns/worldgen/chisel_variants.txt";
    private static final Map<String, Set<Integer>> VALID = load();

    private ChiselVariants() {}

    /** @return true when Chisel block {@code name} (no mod prefix) has a variation at {@code meta}, or is unlisted */
    public static boolean isValid(String name, int meta) {
        Set<Integer> metas = VALID.get(name);
        return metas == null || metas.contains(meta);
    }

    /** @return the listed block names, for tests */
    public static Set<String> names() {
        return VALID.keySet();
    }

    static Map<String, Set<Integer>> parse(BufferedReader in) throws IOException {
        Map<String, Set<Integer>> out = new HashMap<>();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(" ");
            Set<Integer> metas = new HashSet<>();
            for (String m : parts[1].split(",")) {
                metas.add(Integer.parseInt(m));
            }
            out.put(parts[0], Collections.unmodifiableSet(metas));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Set<Integer>> load() {
        try (InputStream raw = ChiselVariants.class.getResourceAsStream(RESOURCE)) {
            if (raw == null) {
                return Collections.emptyMap();
            }
            return parse(new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException e) {
            return Collections.emptyMap();
        }
    }
}
