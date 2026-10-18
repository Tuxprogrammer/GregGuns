package com.gregguns.api;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.logic.RepairSpec;

/**
 * An item the Repair Bench and the Armory Gantry can restore. An item implements this together with {@link Tiered};
 * the machines read the repair list from here and the tier from {@code getTier()}. GT tools do not implement it, so
 * the machines never touch their {@code GT.ToolStats} NBT (GGS-09-25).
 *
 * <p>
 * Guns do not implement it yet: their item damage is "rounds missing", not wear (see {@code ItemGun}). A gun with real
 * wear adds this interface and nothing else. Both sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/repair-bench.md (GGS-09-25)
public interface BenchRepairable {

    /**
     * @param stack a stack of this item
     * @return what repairing it costs, or null when this stack cannot be repaired on a bench
     */
    @Nullable
    RepairSpec repairSpec(ItemStack stack);
}
