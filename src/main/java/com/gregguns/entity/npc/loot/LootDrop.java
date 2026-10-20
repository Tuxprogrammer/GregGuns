package com.gregguns.entity.npc.loot;

import java.util.Objects;
import java.util.Random;

/**
 * One possible drop: an item reference and an inclusive count range. Pure data, no Minecraft types.
 *
 * <p>
 * The reference is {@code modid:name} (a registered item) or {@code ore:Name} (the first stack of an ore dictionary
 * entry, GT first). The count is drawn as {@code min + nextInt(max - min + 1)} from the caller's {@link Random}, which
 * is the entity's own {@code rand}; Techguns' {@code 1 + round(k * y)} float formulas are replaced by this uniform
 * integer draw so the drop is simulation-friendly and never rounds to zero (GGS-10-15, GGS-10-18).
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (GGS-10-15, GGS-10-18); BUGS NPC-04, NPC-05
public final class LootDrop {

    private final String ref;
    private final int min;
    private final int max;

    /**
     * @param ref item reference
     * @param min smallest count, at least 1
     * @param max largest count, at least {@code min}
     */
    public LootDrop(String ref, int min, int max) {
        if (min < 1 || max < min) {
            throw new IllegalArgumentException("count range " + min + ".." + max + " for " + ref);
        }
        this.ref = Objects.requireNonNull(ref);
        this.min = min;
        this.max = max;
    }

    /**
     * @param ref item reference
     * @return a drop of exactly one
     */
    public static LootDrop one(String ref) {
        return new LootDrop(ref, 1, 1);
    }

    /**
     * @param ref item reference
     * @param min smallest count
     * @param max largest count
     * @return a drop of {@code min..max}
     */
    public static LootDrop range(String ref, int min, int max) {
        return new LootDrop(ref, min, max);
    }

    /** @return the item reference */
    public String ref() {
        return ref;
    }

    /** @return the smallest count */
    public int min() {
        return min;
    }

    /** @return the largest count */
    public int max() {
        return max;
    }

    /**
     * @param rand the entity's own random source
     * @return the count, never below 1 (this is what fixes NPC-04's zero-size stack)
     */
    public int count(Random rand) {
        return min == max ? min : min + rand.nextInt(max - min + 1);
    }
}
