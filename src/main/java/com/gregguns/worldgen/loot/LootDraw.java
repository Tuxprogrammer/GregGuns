package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.spawn.WeightedPick;

/**
 * One chest's draw from a {@link LootCategory}. Pure; everything comes from the caller's seeded {@link Random}, so
 * the same seed gives the same chest (GGS-11-20).
 *
 * <p>
 * Richness (0 to 2, the old {@code lootTier}) changes only how many picks a chest gets and how heavily the
 * category's top-tier lines are weighted. It never adds a line, so the ceiling holds at every richness (GGS-11-23).
 * Gun lines are the exception to the weighting: they keep their base weight at every richness, so a rich chest holds
 * more ammo and better gear but guns stay rare (GGS-11-26).
 *
 * <p>
 * A line with a companion (a gun's ammo) brings the companion stack along in the same pick (GGS-11-26).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-20, GGS-11-23)
public final class LootDraw {

    /** The highest richness. */
    public static final int MAX_RICHNESS = 2;

    private LootDraw() {}

    /** One picked stack, before it becomes an item. */
    public static final class Roll {

        private final LootEntry entry;
        private final int count;
        private final boolean companion;

        Roll(LootEntry entry, int count, boolean companion) {
            this.entry = entry;
            this.count = count;
            this.companion = companion;
        }

        /** @return the line that was picked (for a companion stack, the line that brought it) */
        public LootEntry entry() {
            return entry;
        }

        /** @return the item reference of this stack: the line's, or its companion's */
        public String ref() {
            return companion ? entry.companion()
                .ref() : entry.ref();
        }

        /** @return the stack size */
        public int count() {
            return count;
        }

        /** @return true when this stack is the companion of the line, not the line itself */
        public boolean companion() {
            return companion;
        }
    }

    /**
     * @param richness any int
     * @return it clamped to 0..{@link #MAX_RICHNESS}
     */
    public static int clamp(int richness) {
        return Math.max(0, Math.min(MAX_RICHNESS, richness));
    }

    /**
     * Picks in a chest. Both ends are inclusive (Forge's own draw excludes the maximum, so a "max" was never
     * reached); richness adds to both ends.
     *
     * @param min      category minimum
     * @param max      category maximum
     * @param richness 0..2
     * @param rand     the chest's random source
     * @return the number of picks
     */
    public static int pickCount(int min, int max, int richness, Random rand) {
        int r = clamp(richness);
        return min + r + (max > min ? rand.nextInt(max - min + 1) : 0);
    }

    /**
     * The weight of a line at a richness: a line at the category's top declared tier counts {@code 1 + richness}
     * times, others as written, so a richer chest holds better things from the same list.
     *
     * @param base     the line's weight
     * @param tier     the line's tier
     * @param topTier  the highest tier any line in the category declares
     * @param richness 0..2
     * @return the weight to draw with
     */
    public static int weight(int base, Tier tier, Tier topTier, int richness) {
        return tier == topTier ? base * (1 + clamp(richness)) : base;
    }

    /**
     * The weight of a category line at a richness: {@link #weight(int, Tier, Tier, int)}, except that a gun line
     * ({@link Arsenal#GUN} pool) keeps its base weight.
     *
     * @param line     the line
     * @param topTier  the highest tier any line in the category declares
     * @param richness 0..2
     * @return the weight to draw with
     */
    public static int weight(LootEntry line, Tier topTier, int richness) {
        return Arsenal.GUN.equals(line.pool()) ? line.weight() : weight(line.weight(), line.tier(), topTier, richness);
    }

    /**
     * @param category a category
     * @return the highest tier any of its lines declares
     */
    public static Tier topTier(LootCategory category) {
        Tier top = Tier.STEAM;
        for (LootEntry e : category.entries()) {
            if (e.tier()
                .ordinal() > top.ordinal()) {
                top = e.tier();
            }
        }
        return top;
    }

    /**
     * @param category a category
     * @param richness 0..2
     * @param rand     the chest's random source
     * @return the picked lines with their stack sizes, in draw order; a companion follows its line
     */
    public static List<Roll> draw(LootCategory category, int richness, Random rand) {
        List<LootEntry> entries = category.entries();
        Tier top = topTier(category);
        int[] weights = new int[entries.size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = weight(entries.get(i), top, richness);
        }
        int n = pickCount(category.minItems(), category.maxItems(), richness, rand);
        List<Roll> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int idx = WeightedPick.pick(weights, rand);
            if (idx < 0) {
                break;
            }
            LootEntry e = entries.get(idx);
            out.add(
                new Roll(
                    e,
                    e.drop()
                        .count(rand),
                    false));
            if (e.companion() != null) {
                out.add(
                    new Roll(
                        e,
                        e.companion()
                            .count(rand),
                        true));
            }
        }
        return out;
    }
}
