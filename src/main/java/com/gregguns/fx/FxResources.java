package com.gregguns.fx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.gregguns.GregGuns;

/**
 * Loads the shipped FX data files into the shared {@link FxRegistry}. Common code: the server uses the same registry
 * for scenarios and tests, the client for spawning.
 *
 * <p>
 * To add a data file, append its path to {@link #FILES}; later files may {@code EXTENDS} earlier systems.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (2: data files); docs/gregguns-spec/04-projectiles-fx
public final class FxResources {

    /** Data files under {@code assets/gregguns/fx/}, loaded in this order. Append only. */
    public static final String[] FILES = { "fx_lists.txt" };

    private static final String ROOT = "/assets/gregguns/fx/";

    // Loaded once on the first read and then only read: a registry, not gameplay state.
    private static FxRegistry shared;

    private FxResources() {}

    /**
     * Returns the shared registry, loading the shipped files on first use. Parse errors are logged, not thrown.
     *
     * @return the registry; never null
     */
    public static synchronized FxRegistry shared() {
        if (shared == null) {
            FxRegistry r = new FxRegistry();
            for (String file : FILES) {
                for (String error : r.load(read(file), file)) {
                    GregGuns.LOG.error("FX data: {}", error);
                }
            }
            shared = r;
        }
        return shared;
    }

    /**
     * Reads a shipped data file.
     *
     * @param file a name from {@link #FILES}
     * @return its text, or the empty string (after logging) when it is missing
     */
    public static String read(String file) {
        try (InputStream in = FxResources.class.getResourceAsStream(ROOT + file)) {
            if (in == null) {
                GregGuns.LOG.error("FX data file missing: {}{}", ROOT, file);
                return "";
            }
            try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name())) {
                return s.useDelimiter("\\A")
                    .hasNext() ? s.next() : "";
            }
        } catch (IOException e) {
            GregGuns.LOG.error("FX data file unreadable: {}{}", ROOT, file, e);
            return "";
        }
    }
}
