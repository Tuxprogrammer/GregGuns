package com.gregguns.core.registry;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Records what has been registered so duplicates fail loudly at startup instead of silently overwriting each other.
 * Pure logic, shared by every module's {@link ModuleRegistrar}.
 *
 * <p>
 * Items, blocks and tile entities each have their own namespace of names. Entity IDs are mod-local integers and must
 * be unique; they are chosen explicitly by the registering module so that toggling another module never renumbers
 * them.
 */
public final class RegistrationLedger {

    /** The kinds of content with an independent name space. */
    public enum Kind {
        ITEM,
        BLOCK,
        ENTITY,
        TILE_ENTITY
    }

    private final Map<Kind, Set<String>> names = new EnumMap<>(Kind.class);
    private final Map<Integer, String> entityIds = new HashMap<>();

    /**
     * Claims a name for one kind of content.
     *
     * @param kind the kind
     * @param name the bare content name (already validated)
     * @throws IllegalStateException if the name is already taken for that kind
     */
    public void claimName(Kind kind, String name) {
        if (!names.computeIfAbsent(kind, k -> new HashSet<>())
            .add(name)) {
            throw new IllegalStateException("Duplicate " + kind + " registration: " + name);
        }
    }

    /**
     * Claims a mod-local entity ID.
     *
     * @param id     the ID, from 0 up
     * @param entity the entity's name, for the error message
     * @throws IllegalArgumentException if the ID is negative
     * @throws IllegalStateException    if another entity holds the ID
     */
    public void claimEntityId(int id, String entity) {
        if (id < 0) {
            throw new IllegalArgumentException("Entity ID must not be negative: " + id + " for " + entity);
        }
        String previous = entityIds.putIfAbsent(id, entity);
        if (previous != null) {
            throw new IllegalStateException("Entity ID " + id + " used by both " + previous + " and " + entity);
        }
    }
}
