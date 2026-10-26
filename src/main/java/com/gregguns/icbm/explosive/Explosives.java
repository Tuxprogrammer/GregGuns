package com.gregguns.icbm.explosive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one shared explosive registry (D12). Content registers during init; ids are the warhead NBT {@code explosive}
 * value and never change. Lookup is by id, never by a numeric slot (avoids ICBM-02-18, reused retired slots). Both
 * sides, no Minecraft types.
 *
 * <p>
 * Avoids: ICBM-02-18 (ids are names, not first-free integers), ICBM-02-24 (duplicate ids throw, late registration
 * throws).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/explosive-registry.md
public final class Explosives {

    private static final Map<String, ExplosiveDef> BY_ID = new LinkedHashMap<>();

    private Explosives() {}

    /**
     * @param def the explosive to add
     * @throws IllegalStateException    when the id is taken
     * @throws IllegalArgumentException when the def is null
     */
    public static synchronized void register(ExplosiveDef def) {
        if (def == null) {
            throw new IllegalArgumentException("def");
        }
        if (BY_ID.containsKey(def.id())) {
            throw new IllegalStateException("duplicate explosive id " + def.id());
        }
        BY_ID.put(def.id(), def);
    }

    /**
     * @param id a registry id
     * @return the explosive, or {@code null} when unknown (an NBT from a removed type)
     */
    public static synchronized ExplosiveDef get(String id) {
        return BY_ID.get(id);
    }

    /** @return every registered explosive in registration order, a copy */
    public static synchronized List<ExplosiveDef> all() {
        return new ArrayList<>(BY_ID.values());
    }

    /** @return the registered ids, a copy */
    public static synchronized Collection<String> ids() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.keySet()));
    }

    /** Test hook: forget everything. */
    public static synchronized void clearForTests() {
        BY_ID.clear();
    }
}
