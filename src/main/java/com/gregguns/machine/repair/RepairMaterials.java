package com.gregguns.machine.repair;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;
import com.gregguns.armor.ArmorRepair;
import com.gregguns.armor.logic.RepairSpec;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Counts repair parts in a set of item slots and takes them out again, for the Repair Bench and the Armory Gantry. The
 * metal part matches by GT's unificator, so any mod's plate of the right material counts (GGS-09-21); the cloth part
 * matches by item. Counts feed {@link RepairPlanner}, which decides; this class only looks and removes. Server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/repair-bench.md (GGS-09-20, GGS-09-21, GGS-09-22)
public final class RepairMaterials {

    /** A list of item slots the parts live in: the bench's nine slots, or every slot of the Gantry's input buses. */
    public interface Slots {

        /** @return the number of slots */
        int size();

        /**
         * @param index a slot
         * @return its stack, or null when empty
         */
        @Nullable
        ItemStack get(int index);

        /**
         * @param index a slot
         * @param stack the new contents, or null to empty it
         */
        void set(int index, @Nullable ItemStack stack);
    }

    /** Material counts found in the slots, and a sample spec per key so the parts can be removed again. */
    public static final class Pool {

        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private final Map<String, RepairSpec> samples = new LinkedHashMap<>();

        /** @return the counts by material key (see {@link RepairPlanner#metalKey}), for {@link RepairPlanner#plan} */
        public Map<String, Integer> counts() {
            return counts;
        }
    }

    private RepairMaterials() {}

    /**
     * @param spec      a repair list
     * @param candidate a stack
     * @return true when the stack is the list's metal part (a GT unificator match)
     */
    public static boolean isMetal(RepairSpec spec, @Nullable ItemStack candidate) {
        if (candidate == null || spec.metalPrefix() == null) {
            return false;
        }
        ItemStack metal = ArmorRepair.metalStack(spec);
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L284
        return metal != null && GTOreDictUnificator.isInputStackEqual(candidate, metal);
    }

    /**
     * @param spec      a repair list
     * @param candidate a stack
     * @return true when the stack is the list's cloth part
     */
    public static boolean isCloth(RepairSpec spec, @Nullable ItemStack candidate) {
        if (candidate == null) {
            return false;
        }
        Item cloth = GameRegistry.findItem(GregGuns.MODID, spec.clothItem());
        return cloth != null && candidate.getItem() == cloth;
    }

    /**
     * Counts the parts every spec could ask for.
     *
     * @param slots the slots to look in
     * @param specs the repair lists of the items on offer
     * @return the counts, keyed as {@link RepairPlanner} does
     */
    public static Pool count(Slots slots, Collection<RepairSpec> specs) {
        Pool pool = new Pool();
        for (RepairSpec spec : specs) {
            if (spec.metalPrefix() != null) {
                String key = RepairPlanner.metalKey(spec);
                if (!pool.counts.containsKey(key)) {
                    pool.counts.put(key, total(slots, spec, true));
                    pool.samples.put(key, spec);
                }
            }
            String cloth = RepairPlanner.clothKey(spec);
            if (!pool.counts.containsKey(cloth)) {
                pool.counts.put(cloth, total(slots, spec, false));
                pool.samples.put(cloth, spec);
            }
        }
        return pool;
    }

    private static int total(Slots slots, RepairSpec spec, boolean metal) {
        int sum = 0;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack s = slots.get(i);
            if (s != null && (metal ? isMetal(spec, s) : isCloth(spec, s))) {
                sum += s.stackSize;
            }
        }
        return sum;
    }

    /**
     * Removes parts from the slots. The caller has planned the amounts against {@link #count}, so they are there; a
     * shortfall removes what exists.
     *
     * @param slots    the slots to take from
     * @param pool     the pool the plan was made from
     * @param consumed key to amount, from {@link RepairPlanner.Plan#consumed()}
     */
    public static void take(Slots slots, Pool pool, Map<String, Integer> consumed) {
        for (Map.Entry<String, Integer> e : consumed.entrySet()) {
            RepairSpec spec = pool.samples.get(e.getKey());
            if (spec == null) {
                continue;
            }
            boolean metal = e.getKey()
                .equals(RepairPlanner.metalKey(spec)) && spec.metalPrefix() != null;
            int left = e.getValue();
            for (int i = 0; i < slots.size() && left > 0; i++) {
                ItemStack s = slots.get(i);
                if (s == null || !(metal ? isMetal(spec, s) : isCloth(spec, s))) {
                    continue;
                }
                int used = Math.min(left, s.stackSize);
                s.stackSize -= used;
                left -= used;
                slots.set(i, s.stackSize <= 0 ? null : s);
            }
        }
    }
}
