package com.gregguns.entity.npc.spawn;

import java.util.Random;

/**
 * A standard weighted pick that never selects a zero-weight entry. Techguns drew {@code nextInt(total)} and took the
 * first entry whose running total was {@code >= roll}, so a roll of 0 chose the first entry even at weight 0 and every
 * boundary shifted by one (NPC-14, NPC-15). Here an entry is chosen with probability weight / total. Pure.
 *
 * <p>
 * Usage: {@code int i = WeightedPick.pick(new int[] {100, 0, 50}, rnd)}; -1 means nothing can be picked. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/index.md (NPC-14, NPC-15), docs/gregguns-spec/10-entities-npcs/spawning.md
// (GGS-10-24)
public final class WeightedPick {

    private WeightedPick() {}

    /**
     * @param weights per-entry weights; zero and negative weights count as 0
     * @param rnd     the random source
     * @return the picked index, or -1 when every weight is 0 (or the array is empty)
     */
    public static int pick(int[] weights, Random rnd) {
        long total = 0;
        for (int w : weights) {
            total += Math.max(0, w);
        }
        if (total <= 0) {
            return -1;
        }
        long roll = (long) (rnd.nextDouble() * total);
        long acc = 0;
        for (int i = 0; i < weights.length; i++) {
            int w = Math.max(0, weights[i]);
            if (w == 0) {
                continue;
            }
            acc += w;
            if (roll < acc) {
                return i;
            }
        }
        // Rounding at the very top of the range: the last non-zero entry.
        for (int i = weights.length - 1; i >= 0; i--) {
            if (weights[i] > 0) {
                return i;
            }
        }
        return -1;
    }
}
