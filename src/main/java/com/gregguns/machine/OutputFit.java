package com.gregguns.machine;

import net.minecraft.item.ItemStack;

import gregtech.api.util.GTUtility;

/**
 * Decides whether a recipe's item outputs fit in a machine's output slots, the way GT's basic machine will place them
 * when the recipe finishes. GT's own {@code MTEBasicMachine#canOutput} compares output {@code i} with slot {@code i}
 * only, so a press with a full first slot stalls although the other output slots are empty. At the end of a recipe GT
 * already tries every slot, starting at the output's own index and wrapping; this check simulates exactly that, so a
 * press only stalls when there is no room anywhere. Both sides, no world access.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L610-L614
// (the placement loop: output i is tried in slots i, i+1, ... and goes whole into the first slot that is empty or holds
// the same item with room) and #L774-L785 (the per-index canOutput this replaces).
final class OutputFit {

    private OutputFit() {}

    /**
     * @param slots   the machine's output slots, in order; entries may be null; not modified
     * @param outputs the recipe's item outputs; entries may be null
     * @return whether every output fits whole into some slot, in GT's placement order
     */
    static boolean fits(ItemStack[] slots, ItemStack[] outputs) {
        if (outputs == null) {
            return true;
        }
        int n = slots.length;
        int[] sizes = new int[n];
        ItemStack[] held = new ItemStack[n];
        for (int i = 0; i < n; i++) {
            held[i] = slots[i];
            sizes[i] = slots[i] == null ? 0 : slots[i].stackSize;
        }
        for (int i = 0; i < outputs.length && i < n; i++) {
            ItemStack out = outputs[i];
            if (out == null || out.stackSize <= 0) {
                continue;
            }
            boolean placed = false;
            for (int j = 0; j < n && !placed; j++) {
                int slot = (i + j) % n;
                if (held[slot] == null) {
                    held[slot] = out;
                    sizes[slot] = out.stackSize;
                    placed = true;
                } else if (GTUtility.areStacksEqual(held[slot], out, false)
                    && sizes[slot] + out.stackSize <= Math.min(out.getMaxStackSize(), 64)) {
                        sizes[slot] += out.stackSize;
                        placed = true;
                    }
            }
            if (!placed) {
                return false;
            }
        }
        return true;
    }
}
