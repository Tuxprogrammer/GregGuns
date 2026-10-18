package com.gregguns.armor.slots.logic;

/**
 * The shapeless Gas Mask refilter rule (GGS-05-37): exactly one Gas Mask, exactly one Gas Mask Filter, and nothing
 * else in the grid. A grid without a Gas Mask never produces one. Plain Java, both sides.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md (Gas Mask refilter; GGS-05-37)
public final class RefilterRule {

    private RefilterRule() {}

    /**
     * @param masks   Gas Masks in the grid
     * @param filters Gas Mask Filters in the grid
     * @param others  any other non-empty stacks in the grid
     * @return whether the recipe matches
     */
    public static boolean matches(int masks, int filters, int others) {
        return masks == 1 && filters == 1 && others == 0;
    }
}
