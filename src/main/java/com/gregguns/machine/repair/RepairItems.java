package com.gregguns.machine.repair;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.BenchRepairable;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.armor.logic.RepairSpec;

/**
 * Reads what a repair machine needs from an item stack: its repair list, its tier and its damage. Only items that
 * implement {@link BenchRepairable} count, so a GT tool or a vanilla item is never a candidate (GGS-09-25). Both
 * sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/repair-bench.md (GGS-09-25)
public final class RepairItems {

    private RepairItems() {}

    /**
     * @param stack any stack, may be null
     * @return its repair list, or null when the item is not repairable on a bench
     */
    public static @Nullable RepairSpec specOf(@Nullable ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof BenchRepairable)) {
            return null;
        }
        return ((BenchRepairable) stack.getItem()).repairSpec(stack);
    }

    /**
     * @param stack a repairable stack
     * @return the item's tier; LV when the item declares none
     */
    public static Tier tierOf(ItemStack stack) {
        return stack.getItem() instanceof Tiered ? ((Tiered) stack.getItem()).getTier() : Tier.LV;
    }

    /**
     * @param stack any stack
     * @return the planner's view of it; a stack with no repair list is a candidate that cannot be repaired
     */
    public static RepairPlanner.Candidate candidate(@Nullable ItemStack stack) {
        if (stack == null) {
            return new RepairPlanner.Candidate(0, 1, Tier.STEAM, null);
        }
        RepairSpec spec = specOf(stack);
        return new RepairPlanner.Candidate(
            stack.getItemDamage(),
            stack.getMaxDamage(),
            spec == null ? Tier.STEAM : tierOf(stack),
            spec);
    }
}
