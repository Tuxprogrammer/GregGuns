package com.gregguns.armor.logic;

import java.util.HashMap;
import java.util.Map;

/**
 * Remembers which bonuses are applied to which wearer, so the game side applies only the difference and can strip
 * everything on logout, death or dimension change. Generic over the wearer key (a player UUID in the game), so it
 * is unit-tested without Minecraft. Not thread-safe; use it from the server thread.
 *
 * <p>
 * Usage each tick: {@code Change c = lifecycle.update(id, desired)}; if {@code c.changed()}, rewrite the attribute
 * modifiers from {@code c.now()}. On logout, death or dimension change: {@code lifecycle.forget(id)} and strip.
 */
// Spec: docs/gregguns-spec/05-armor/index.md (deviation notes: bonus lifecycle)
public final class BonusLifecycle<K> {

    /** The result of {@link #update}. */
    public static final class Change {

        private final BonusMap before;
        private final BonusMap now;

        Change(BonusMap before, BonusMap now) {
            this.before = before;
            this.now = now;
        }

        /** @return what was applied before this update */
        public BonusMap before() {
            return before;
        }

        /** @return what is applied now */
        public BonusMap now() {
            return now;
        }

        /** @return true when the applied bonuses differ, so the game side must rewrite them */
        public boolean changed() {
            return !before.equals(now);
        }
    }

    private final Map<K, BonusMap> applied = new HashMap<>();

    /**
     * Records the wanted bonuses for a wearer.
     *
     * @param key     the wearer
     * @param desired what the wearer's gear gives now
     * @return what was applied and what is applied now
     */
    public Change update(K key, BonusMap desired) {
        BonusMap before = applied.getOrDefault(key, BonusMap.EMPTY);
        if (desired.isEmpty()) {
            applied.remove(key);
        } else {
            applied.put(key, desired);
        }
        return new Change(before, desired);
    }

    /**
     * Drops a wearer (logout, death, dimension change).
     *
     * @param key the wearer
     * @return what was applied, so the caller can strip it; {@link BonusMap#EMPTY} if nothing was
     */
    public BonusMap forget(K key) {
        BonusMap before = applied.remove(key);
        return before == null ? BonusMap.EMPTY : before;
    }

    /**
     * @param key the wearer
     * @return the bonuses applied to the wearer
     */
    public BonusMap applied(K key) {
        return applied.getOrDefault(key, BonusMap.EMPTY);
    }

    /** @return how many wearers currently have bonuses applied */
    public int size() {
        return applied.size();
    }
}
