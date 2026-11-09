package com.gregguns.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.ItemList;

/**
 * GregTech's two IC2 circuit replacements that no recipe in the pack makes: the Basic Electronic Circuit and the
 * Advanced Integrated Circuit. GT files them under the LV and HV circuit ore names with no unification, so NEI cycles
 * them through every recipe that takes "any LV circuit" or "any HV circuit", ours included. GregGuns never produces,
 * requires or names them; its NEI plugin hides them, and the scenario {@code item.ghost_circuit.none} checks that no
 * GregGuns recipe resolves to one. Both sides.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/MetaGeneratedItem03.java#L1882-L1896
// (both registered as OrePrefixes.circuit of Materials.LV and Materials.HV with SubTag.NO_UNIFICATION)
// Spec: docs/gregguns-icbm-spec/DECISIONS.md (D362)
public final class GhostCircuits {

    /** The GT {@code ItemList} names of the two circuits. */
    public static final List<String> NAMES = Collections
        .unmodifiableList(Arrays.asList("Circuit_BasicElectronic", "Circuit_Integrated_Advanced"));

    private GhostCircuits() {}

    /** @return one of each ghost circuit GT registered (none before GT's items exist) */
    public static List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemList entry : new ItemList[] { ItemList.Circuit_BasicElectronic, ItemList.Circuit_Integrated_Advanced }) {
            if (entry.hasBeenSet()) {
                out.add(entry.get(1));
            }
        }
        return out;
    }

    /**
     * @param stack a stack, or {@code null}
     * @return whether it is one of the ghost circuits (same item and damage)
     */
    public static boolean isGhost(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        for (ItemStack ghost : stacks()) {
            if (ghost.getItem() == stack.getItem() && ghost.getItemDamage() == stack.getItemDamage()) {
                return true;
            }
        }
        return false;
    }
}
