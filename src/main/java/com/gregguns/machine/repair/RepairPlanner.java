package com.gregguns.machine.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.RepairSpec;

/**
 * Decides which damaged items a machine repairs and what that consumes: every item is all or nothing against its own
 * material list, an item above the machine's tier is skipped, and a batch stops at the machine's capacity. The bench
 * calls it with one candidate, the Gantry with up to one bay's worth per cycle. Materials are abstract keys with
 * counts, so the logic needs no Minecraft types. Both sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/repair-bench.md (GGS-09-22),
// docs/gregguns-spec/08-machines-production/multiblocks.md (Armory Gantry process, GGS-08-70, GGS-08-72)
public final class RepairPlanner {

    /** What happened to one candidate. */
    public enum Status {
        /** Repaired; its materials are in the plan's consumption. */
        REPAIRED,
        /** Damage is 0; nothing to do. */
        UNDAMAGED,
        /** The item has no repair list (or is a GT tool). */
        NOT_REPAIRABLE,
        /** The machine's tier is below the item's. */
        TIER_TOO_LOW,
        /** The pool lacks one of the materials; nothing of this item's was taken. */
        MISSING_MATERIALS,
        /** The machine had no bay left for it. */
        NO_CAPACITY
    }

    /** One item offered to the machine. */
    public static final class Candidate {

        private final int damage;
        private final int maxDamage;
        private final Tier tier;
        private final RepairSpec spec;

        /**
         * @param damage    current damage
         * @param maxDamage durability
         * @param tier      the item's tier
         * @param spec      its repair list, or null when it cannot be repaired here
         */
        public Candidate(int damage, int maxDamage, Tier tier, RepairSpec spec) {
            this.damage = damage;
            this.maxDamage = maxDamage;
            this.tier = tier;
            this.spec = spec;
        }

        /** @return the damage fraction, 0..1 */
        public double fraction() {
            return RepairMath.damageFraction(damage, maxDamage);
        }

        /** @return the materials to consume, keyed as {@link #metalKey}/{@link #clothKey}; empty when none */
        public Map<String, Integer> cost() {
            Map<String, Integer> cost = new LinkedHashMap<>();
            if (spec == null) {
                return cost;
            }
            RepairSpec.Cost c = spec.cost(damage, maxDamage);
            if (c.metal() > 0) {
                cost.put(metalKey(spec), c.metal());
            }
            if (c.cloth() > 0) {
                cost.put(clothKey(spec), c.cloth());
            }
            return cost;
        }
    }

    /** The result of {@link #plan}. */
    public static final class Plan {

        private final List<Status> statuses;
        private final Map<String, Integer> consumed;
        private final double maxFraction;
        private final int repaired;

        Plan(List<Status> statuses, Map<String, Integer> consumed, double maxFraction, int repaired) {
            this.statuses = Collections.unmodifiableList(statuses);
            this.consumed = Collections.unmodifiableMap(consumed);
            this.maxFraction = maxFraction;
            this.repaired = repaired;
        }

        /** @return one status per candidate, in the order they were offered */
        public List<Status> statuses() {
            return statuses;
        }

        /** @return materials taken from the pool by the whole batch */
        public Map<String, Integer> consumed() {
            return consumed;
        }

        /** @return the largest damage fraction among the repaired items, 0 when none */
        public double maxFraction() {
            return maxFraction;
        }

        /** @return how many items were repaired */
        public int repaired() {
            return repaired;
        }
    }

    private RepairPlanner() {}

    /**
     * @param spec a repair list with a metal part
     * @return the material key of its metal, {@code prefix:Material}
     */
    public static String metalKey(RepairSpec spec) {
        return spec.metalPrefix() + ":" + spec.metalMaterial();
    }

    /**
     * @param spec a repair list
     * @return the material key of its cloth part, {@code item:<registry name>}
     */
    public static String clothKey(RepairSpec spec) {
        return "item:" + spec.clothItem();
    }

    /**
     * Plans a batch. Candidates are taken in order; the pool is not modified.
     *
     * @param candidates the items offered, in scan order
     * @param pool       material counts available
     * @param machine    the machine tier
     * @param capacity   the most items to repair (1 for the bench, the bay count for the Gantry)
     * @return the statuses, the materials to take and the longest job's damage fraction
     */
    public static Plan plan(List<Candidate> candidates, Map<String, Integer> pool, Tier machine, int capacity) {
        Map<String, Integer> left = new LinkedHashMap<>(pool);
        Map<String, Integer> consumed = new LinkedHashMap<>();
        List<Status> statuses = new ArrayList<>(candidates.size());
        double maxFraction = 0.0;
        int repaired = 0;
        for (Candidate c : candidates) {
            Status status = decide(c, left, machine, repaired < capacity);
            if (status == Status.REPAIRED) {
                for (Map.Entry<String, Integer> e : c.cost()
                    .entrySet()) {
                    left.merge(e.getKey(), -e.getValue(), Integer::sum);
                    consumed.merge(e.getKey(), e.getValue(), Integer::sum);
                }
                maxFraction = Math.max(maxFraction, c.fraction());
                repaired++;
            }
            statuses.add(status);
        }
        return new Plan(statuses, consumed, maxFraction, repaired);
    }

    private static Status decide(Candidate c, Map<String, Integer> left, Tier machine, boolean hasBay) {
        if (c.spec == null) {
            return Status.NOT_REPAIRABLE;
        }
        if (c.damage <= 0) {
            return Status.UNDAMAGED;
        }
        if (!RepairMath.tierAllows(machine, c.tier)) {
            return Status.TIER_TOO_LOW;
        }
        if (!hasBay) {
            return Status.NO_CAPACITY;
        }
        for (Map.Entry<String, Integer> e : c.cost()
            .entrySet()) {
            if (left.getOrDefault(e.getKey(), 0) < e.getValue()) {
                return Status.MISSING_MATERIALS;
            }
        }
        return Status.REPAIRED;
    }
}
