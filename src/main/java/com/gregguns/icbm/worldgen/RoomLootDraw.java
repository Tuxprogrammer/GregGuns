package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.entity.npc.spawn.WeightedPick;
import com.gregguns.worldgen.loot.LootDraw;
import com.gregguns.worldgen.loot.LootEntry;

/**
 * What one silo complex chest holds, by its room's state (D317). Pure: it picks lines and counts and says which picks
 * spoil and how worn the gear is; {@link SiloChests} turns the picks into stacks.
 *
 * <table>
 * <caption>Per state</caption>
 * <tr>
 * <th>State</th>
 * <th>Real picks</th>
 * <th>Junk</th>
 * <th>Counts</th>
 * <th>Spoil</th>
 * <th>Wear</th>
 * <th>Richness</th>
 * </tr>
 * <tr>
 * <td>STAFFED</td>
 * <td>5-8</td>
 * <td>0</td>
 * <td>x1</td>
 * <td>0</td>
 * <td>none</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>IN_SERVICE</td>
 * <td>4-7</td>
 * <td>0</td>
 * <td>x1</td>
 * <td>0</td>
 * <td>none</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>MOTHBALLED</td>
 * <td>2-5</td>
 * <td>1-2</td>
 * <td>x0.75</td>
 * <td>20%</td>
 * <td>half the gear, 10-40%</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td>GUTTED</td>
 * <td>1-3</td>
 * <td>1-3</td>
 * <td>x0.5</td>
 * <td>50%</td>
 * <td>all gear, 40-90%</td>
 * <td>0</td>
 * </tr>
 * </table>
 *
 * <p>
 * The never-empty rule: the first pick never spoils, so every chest holds at least one real stack at every level; a
 * table with no line left falls back to {@link #FALLBACK}. A gun brings its ammo, and spoil and wear never touch that
 * companion. A whole missile body is intact in a staffed, in-service or mothballed room and always a salvage body in a
 * gutted one (owner, alpha 7); a salvage body still counts as a real find.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Draw by room state, GGI-08-18); D317
public final class RoomLootDraw {

    /** The line a chest falls back to when its table has nothing left (a pack retune emptied it). */
    public static final LootEntry FALLBACK = new LootEntry(
        LootDrop.range("minecraft:torch", 4, 8),
        1,
        Tier.STEAM,
        SiloPools.COMMON,
        null);

    private RoomLootDraw() {}

    /** The numbers of one room state. */
    public static final class Profile {

        final int realMin, realMax, junkMin, junkMax, richness;
        final double countScale, spoilChance, wearShare, wearMin, wearMax;

        Profile(int realMin, int realMax, int junkMin, int junkMax, double countScale, double spoilChance,
            double wearShare, double wearMin, double wearMax, int richness) {
            this.realMin = realMin;
            this.realMax = realMax;
            this.junkMin = junkMin;
            this.junkMax = junkMax;
            this.countScale = countScale;
            this.spoilChance = spoilChance;
            this.wearShare = wearShare;
            this.wearMin = wearMin;
            this.wearMax = wearMax;
            this.richness = richness;
        }

        /** @return fewest real picks before the table's offset */
        public int realMin() {
            return realMin;
        }

        /** @return most real picks before the table's offset */
        public int realMax() {
            return realMax;
        }

        /** @return fewest junk picks */
        public int junkMin() {
            return junkMin;
        }

        /** @return most junk picks */
        public int junkMax() {
            return junkMax;
        }

        /** @return the factor every stack count is scaled by (rounded up, at least 1) */
        public double countScale() {
            return countScale;
        }

        /** @return the chance a spoilable pick after the first spoils */
        public double spoilChance() {
            return spoilChance;
        }

        /** @return the share of picks whose gear is worn */
        public double wearShare() {
            return wearShare;
        }

        /** @return the richness the table's top-tier lines are weighted by (0 to 2) */
        public int richness() {
            return richness;
        }
    }

    private static final Profile STAFFED = new Profile(5, 8, 0, 0, 1.0, 0, 0, 0, 0, 2);
    private static final Profile IN_SERVICE = new Profile(4, 7, 0, 0, 1.0, 0, 0, 0, 0, 1);
    private static final Profile MOTHBALLED = new Profile(2, 5, 1, 2, 0.75, 0.2, 0.5, 0.1, 0.4, 0);
    private static final Profile GUTTED = new Profile(1, 3, 1, 3, 0.5, 0.5, 1.0, 0.4, 0.9, 0);

    /**
     * @param s a room state; null counts as staffed
     * @return its numbers
     */
    public static Profile profile(RoomState s) {
        if (s == RoomState.GUTTED) {
            return GUTTED;
        }
        if (s == RoomState.MOTHBALLED) {
            return MOTHBALLED;
        }
        if (s == RoomState.IN_SERVICE) {
            return IN_SERVICE;
        }
        return STAFFED;
    }

    /** One stack to put in the chest. */
    public static final class Pick {

        private final LootEntry entry;
        private final int index;
        private final String ref;
        private final int count;
        private final boolean real;
        private final boolean spoiled;
        private final double wear;
        private final boolean companion;
        private final boolean junk;

        Pick(LootEntry entry, int index, String ref, int count, boolean real, boolean spoiled, double wear,
            boolean companion, boolean junk) {
            this.entry = entry;
            this.index = index;
            this.ref = ref;
            this.count = count;
            this.real = real;
            this.spoiled = spoiled;
            this.wear = wear;
            this.companion = companion;
            this.junk = junk;
        }

        /** @return the line picked (for a companion, the gun's line) */
        public LootEntry entry() {
            return entry;
        }

        /** @return the line's index in the list it came from (table or junk), -1 for the fallback */
        public int index() {
            return index;
        }

        /** @return what to make: the line's item, its companion's, or its spoil item */
        public String ref() {
            return ref;
        }

        /** @return the stack size */
        public int count() {
            return count;
        }

        /** @return true for a real find: not junk and not spoiled (a salvage body counts as real) */
        public boolean real() {
            return real;
        }

        /** @return true when the pick became its spoil item */
        public boolean spoiled() {
            return spoiled;
        }

        /** @return the share of durability gear loses (0 for none); only damageable items take it */
        public double wear() {
            return wear;
        }

        /** @return true for the ammo a gun brought along */
        public boolean companion() {
            return companion;
        }

        /** @return true for a pick from the junk list */
        public boolean junk() {
            return junk;
        }
    }

    /**
     * @param lines      the table's lines that resolve this run, in a stable order
     * @param junk       the junk lines that resolve this run
     * @param state      the room's state
     * @param pickOffset the table's pick offset ({@link RoomLoot#pickOffset})
     * @param rand       the chest's seeded random
     * @return the picks, real ones first; never empty
     */
    public static List<Pick> draw(List<LootEntry> lines, List<LootEntry> junk, RoomState state, int pickOffset,
        Random rand) {
        Profile p = profile(state);
        List<Pick> out = new ArrayList<>();
        List<LootEntry> table = lines == null || lines.isEmpty() ? Collections.singletonList(FALLBACK) : lines;
        int[] weights = weights(table, p.richness);
        int real = Math.max(1, p.realMin + rand.nextInt(p.realMax - p.realMin + 1) + pickOffset);
        for (int i = 0; i < real; i++) {
            int idx = WeightedPick.pick(weights, rand);
            if (idx < 0) {
                idx = 0; // every weight is zero: the first line still fills the chest
            }
            LootEntry e = table.get(idx);
            int count = scale(
                e.drop()
                    .count(rand),
                p.countScale);
            String spoil = SiloPools.spoilOf(e.pool(), e.ref());
            boolean body = SiloPools.isBody(e.ref());
            boolean spoiled;
            if (body) {
                spoiled = state == RoomState.GUTTED;
            } else {
                // the first pick never spoils: the never-empty rule
                spoiled = spoil != null && i > 0 && rand.nextDouble() < p.spoilChance;
            }
            double wear = rand.nextDouble() < p.wearShare ? p.wearMin + rand.nextDouble() * (p.wearMax - p.wearMin) : 0;
            int index = table == lines ? idx : -1;
            if (spoiled) {
                out.add(
                    new Pick(e, index, spoil, body ? 1 : Math.max(1, (count + 1) / 2), body, true, 0, false, false));
            } else {
                out.add(new Pick(e, index, e.ref(), count, true, false, wear, false, false));
            }
            if (e.companion() != null) {
                out.add(
                    new Pick(
                        e,
                        index,
                        e.companion()
                            .ref(),
                        e.companion()
                            .count(rand),
                        true,
                        false,
                        0,
                        true,
                        false));
            }
        }
        if (junk != null && !junk.isEmpty() && p.junkMax > 0) {
            int[] jw = weights(junk, 0);
            int n = p.junkMin + rand.nextInt(p.junkMax - p.junkMin + 1);
            for (int i = 0; i < n; i++) {
                int idx = WeightedPick.pick(jw, rand);
                if (idx < 0) {
                    break;
                }
                LootEntry e = junk.get(idx);
                out.add(
                    new Pick(
                        e,
                        idx,
                        e.ref(),
                        e.drop()
                            .count(rand),
                        false,
                        false,
                        0,
                        false,
                        true));
            }
        }
        return out;
    }

    private static int[] weights(List<LootEntry> lines, int richness) {
        Tier top = Tier.STEAM;
        for (LootEntry e : lines) {
            if (e.tier()
                .ordinal() > top.ordinal()) {
                top = e.tier();
            }
        }
        int[] w = new int[lines.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = LootDraw.weight(lines.get(i), top, richness);
        }
        return w;
    }

    /** @return a count scaled and rounded up, at least 1 */
    static int scale(int count, double factor) {
        return Math.max(1, (int) Math.ceil(count * factor - 1e-9));
    }
}
