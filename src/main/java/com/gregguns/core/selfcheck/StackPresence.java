package com.gregguns.core.selfcheck;

/**
 * Decides whether a recipe stack counts as "present" for the dev self-check, as pure logic.
 *
 * <p>
 * A crafting-table or output stack must have a positive size. A GT machine recipe input is different: GT uses a stack
 * size of 0 for an input that is required but not consumed (programmed circuits, molds, lenses), so size 0 is a valid
 * input there and only a missing item or a negative size is a hole.
 *
 * <p>
 * Both sides, no Minecraft types.
 */
public final class StackPresence {

    private StackPresence() {}

    /**
     * @param hasItem     whether the stack and its item are non-null
     * @param stackSize   the stack's size
     * @param zeroIsValid {@code true} for a GT machine-recipe input, where size 0 means "not consumed"
     * @return whether the slot resolves to something real
     */
    public static boolean isPresent(boolean hasItem, int stackSize, boolean zeroIsValid) {
        if (!hasItem) {
            return false;
        }
        return zeroIsValid ? stackSize >= 0 : stackSize > 0;
    }
}
