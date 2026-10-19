package com.gregguns.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The loaded particle systems and FX lists, looked up by name without regard to case. Pure: no Minecraft types.
 *
 * <p>
 * An unknown name resolves to an empty list; it never throws (Techguns returns null and its callers then crash,
 * PFX-02). The caller decides whether to log. A list may name another list; resolution recurses to a depth of
 * {@value #MAX_DEPTH} so a cycle cannot loop.
 *
 * <p>
 * Not thread safe: load on one thread, then read on the client thread.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (1: lookups, 2.2: lists); Fixes PFX-02
public final class FxRegistry {

    /** Deepest list nesting that resolves. */
    public static final int MAX_DEPTH = 8;

    private final Map<String, ParticleSystemDef> systems = new HashMap<>();
    private final Map<String, List<String>> lists = new HashMap<>();
    private final List<String> errors = new ArrayList<>();

    /**
     * Parses a file into this registry. Later files may {@code EXTENDS} systems from earlier ones, and a later
     * definition replaces an earlier one of the same name.
     *
     * @param text   the file's text
     * @param source label for error messages
     * @return the errors this file produced (also kept in {@link #errors()})
     */
    public List<String> load(String text, String source) {
        FxParser.Result r = FxParser.parse(text, source, systems);
        systems.putAll(r.systems());
        lists.putAll(r.lists());
        errors.addAll(r.errors());
        return r.errors();
    }

    /** @return every parse error since this registry was made */
    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * @param name a system or list name, any case
     * @return true when it names a list or a system
     */
    public boolean has(String name) {
        String k = key(name);
        return lists.containsKey(k) || systems.containsKey(k);
    }

    /** @return every list name, lowercase */
    public Set<String> listNames() {
        return Collections.unmodifiableSet(lists.keySet());
    }

    /** @return every system name, lowercase */
    public Set<String> systemNames() {
        return Collections.unmodifiableSet(systems.keySet());
    }

    /**
     * @param name a system name, any case
     * @return the system, or null
     */
    public ParticleSystemDef system(String name) {
        return systems.get(key(name));
    }

    /**
     * Expands a name into the systems to spawn together. A system name gives that one system; a list name gives its
     * members in order, with unknown members skipped.
     *
     * @param name a list or system name, any case
     * @return the systems; empty (never null) for an unknown name
     */
    public List<ParticleSystemDef> resolve(String name) {
        List<ParticleSystemDef> out = new ArrayList<>();
        expand(key(name), out, 0);
        return out;
    }

    private void expand(String k, List<ParticleSystemDef> out, int depth) {
        List<String> members = lists.get(k);
        if (members != null) {
            if (depth >= MAX_DEPTH) {
                return;
            }
            for (String m : members) {
                expand(key(m), out, depth + 1);
            }
            return;
        }
        ParticleSystemDef d = systems.get(k);
        if (d != null) {
            out.add(d);
        }
    }

    /**
     * Finds list members that name nothing. Techguns skips these silently at spawn time; a shipped data file should
     * have none.
     *
     * @return "list: member" strings, in no particular order; empty when every member resolves
     */
    public Set<String> danglingMembers() {
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> e : lists.entrySet()) {
            for (String m : e.getValue()) {
                if (!has(m)) {
                    out.add(e.getKey() + ": " + m);
                }
            }
        }
        return out;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
