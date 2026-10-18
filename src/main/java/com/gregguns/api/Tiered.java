package com.gregguns.api;

/**
 * Implemented by every GregGuns {@code Item} and {@code Block} to declare its single tier in the D1 tier map.
 *
 * <p>
 * "Every item has exactly one tier" (CONTRIBUTING.md, Tier gating). The tier is declared where the item is defined and
 * is read by the recipe tier check in the dev self-check. A GregGuns item that does not implement this interface, or
 * returns {@code null}, is a self-check violation. For a block, declare the tier on the {@code Block}; its
 * {@code ItemBlock} is covered automatically.
 *
 * <p>
 * Spec: docs/design/0001-tier-map.md, docs/design/0010-material-remap-and-tier-additions.md. Both sides.
 */
public interface Tiered {

    /**
     * @return this item's tier; never {@code null}
     */
    Tier getTier();
}
