package com.gregguns.api;

/**
 * Marker for a GregGuns crafting component: a part, tank, cell or consumable that other recipes consume or players
 * craft. The dev self-check requires every registered component to have a recipe, unless it is on the explicit
 * allowlist in {@code PartCatalog} with a reason.
 *
 * <p>
 * Spec: docs/gregguns-spec/06-items/recipes.md (component index, GGS-06-47). Both sides.
 */
public interface GregGunsComponent {
}
