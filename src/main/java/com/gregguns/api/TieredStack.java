package com.gregguns.api;

import net.minecraft.item.ItemStack;

/**
 * Implemented by a GregGuns item whose damage values are items of different tiers (the Explosive Charge is MV, the
 * Advanced Explosive Charge HV), so a reader that needs the stack's own tier, such as the loot tier cap (D363), gets it
 * instead of the item's single {@link Tiered} tier.
 *
 * <p>
 * Spec: docs/design/0001-tier-map.md. Both sides.
 */
public interface TieredStack {

    /**
     * @param stack a stack of this item
     * @return that stack's tier; never {@code null}
     */
    Tier getTier(ItemStack stack);
}
