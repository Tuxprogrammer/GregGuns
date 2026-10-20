package com.gregguns.worldgen.layout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The loaded layouts, keyed by structure id. Loading happens once in postInit (after {@link PaletteKeys} exists).
 * A missing file, a malformed file or an unknown palette key disables only that structure: the error is reported
 * through the supplied consumer and the id is simply absent from {@link #get} (GGS-11-28, WGEN-23). Nothing here throws
 * to the caller, and nothing runs in a static initializer.
 */
public final class LayoutRegistry {

    /** An id that has no file: an IOException the registry can tell apart from a read failure. */
    public static final class MissingLayoutException extends IOException {

        private static final long serialVersionUID = 1L;

        public MissingLayoutException(String message) {
            super(message);
        }
    }

    /** Where layout text comes from; throws IOException when the id has no file (MissingLayoutException ideally). */
    public interface Source {

        String read(String id) throws IOException;
    }

    /** Reads {@code <prefix><id>.txt} from the classpath as UTF-8. */
    public static final class ClasspathSource implements Source {

        /** Where the shipped layouts live. */
        public static final String STRUCTURES = "/assets/gregguns/structures/";

        private final String prefix;

        public ClasspathSource(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String read(String id) throws IOException {
            String path = prefix + id + ".txt";
            try (InputStream in = LayoutRegistry.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new MissingLayoutException("layout resource not found: " + path);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }

    private final Map<String, Layout> layouts = new LinkedHashMap<>();
    private final Map<String, String> disabled = new HashMap<>();

    private LayoutRegistry() {}

    /** Loads and validates every id; failures are passed to {@code error} and disable that structure only. */
    public static LayoutRegistry load(Source source, Iterable<String> ids, PaletteKeys keys, Consumer<String> error) {
        return load(source, ids, Collections.emptySet(), keys, error, error);
    }

    /**
     * As {@link #load(Source, Iterable, PaletteKeys, Consumer)}, but an id in {@code optional} whose file does not
     * exist is reported to {@code quiet} instead of {@code error}: optional ids (the camp color variants) are asked for
     * speculatively and are expected to be mostly absent. A present but broken optional file still goes to
     * {@code error}.
     */
    public static LayoutRegistry load(Source source, Iterable<String> ids, Set<String> optional, PaletteKeys keys,
        Consumer<String> error, Consumer<String> quiet) {
        LayoutRegistry reg = new LayoutRegistry();
        for (String id : ids) {
            try {
                Layout layout = LayoutParser.parse(id, source.read(id));
                if (!layout.id()
                    .equals(id)) {
                    throw new LayoutFormatException(
                        id + ": header id '" + layout.id() + "' does not match the file name");
                }
                checkKeys(id, layout, keys);
                reg.layouts.put(id, layout);
            } catch (MissingLayoutException e) {
                String msg = "Structure '" + id + "' disabled: " + e.getMessage();
                reg.disabled.put(id, msg);
                (optional.contains(id) ? quiet : error).accept(msg);
            } catch (IOException | LayoutFormatException | RuntimeException e) {
                String msg = "Structure '" + id + "' disabled: " + e.getMessage();
                reg.disabled.put(id, msg);
                error.accept(msg);
            }
        }
        return reg;
    }

    private static void checkKeys(String id, Layout layout, PaletteKeys keys) throws LayoutFormatException {
        for (PaletteEntry e : layout.entries()) {
            for (PaletteChoice c : e.choices()) {
                if (!keys.has(c.key)) {
                    throw new LayoutFormatException(id + ": unknown or unresolved palette key '" + c.key + "'");
                }
            }
        }
    }

    /** The layout, or null when the structure is disabled or was never requested. */
    public Layout get(String id) {
        return layouts.get(id);
    }

    public boolean isEnabled(String id) {
        return layouts.containsKey(id);
    }

    /** Why the structure is disabled, or null. */
    public String disabledReason(String id) {
        return disabled.get(id);
    }

    public Iterable<String> enabledIds() {
        return layouts.keySet();
    }
}
