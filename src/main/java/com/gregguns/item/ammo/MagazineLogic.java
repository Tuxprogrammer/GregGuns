package com.gregguns.item.ammo;

/**
 * Pure counting rules for filling and unloading magazines. No Minecraft types; the items and the crafting recipe call
 * these so the numbers live in one tested place.
 *
 * <p>
 * Spec: docs/techguns-spec/06-items/ammo.md (refill: empty + N rounds; sneak-use: 1 empty + N rounds back).
 * Regression: ITEM-07 / CORE-04 (ammo dupe on partial fit) - {@link #returnedToInventory} never reports more than was
 * produced and the caller drops whatever did not fit.
 */
public final class MagazineLogic {

    private MagazineLogic() {}

    /**
     * How many loaded magazines can be made from the given stacks.
     *
     * @param type        the magazine family
     * @param emptyCount  empty magazines available (negative counts as 0)
     * @param looseRounds loose rounds available (negative counts as 0)
     * @return magazines fillable: one empty plus {@code roundsPerMagazine} rounds each
     */
    public static int fillable(MagazineType type, int emptyCount, int looseRounds) {
        int empties = Math.max(0, emptyCount);
        int rounds = Math.max(0, looseRounds);
        return Math.min(empties, rounds / type.roundsPerMagazine());
    }

    /**
     * @param type  the magazine family
     * @param count how many magazines are being filled (at least 0)
     * @return the loose rounds consumed
     */
    public static int roundsConsumed(MagazineType type, int count) {
        return Math.max(0, count) * type.roundsPerMagazine();
    }

    /**
     * Rounds returned when {@code count} loaded magazines are unloaded (sneak-use unloads one at a time; crafting
     * can do more).
     *
     * @param type  the magazine family
     * @param count loaded magazines unloaded
     * @return the loose rounds returned; the empties returned equal {@code count}
     */
    public static int roundsReturned(MagazineType type, int count) {
        return roundsConsumed(type, count);
    }

    /**
     * How many of {@code produced} items fit in a stack that already holds {@code present}, given a stack limit, so
     * a caller can drop only the remainder instead of duplicating or voiding it.
     *
     * @param present  items already in the target stack
     * @param produced items to add
     * @param limit    the stack size limit
     * @return the count that fits; the caller handles {@code produced - result}
     */
    public static int returnedToInventory(int present, int produced, int limit) {
        int room = Math.max(0, limit - Math.max(0, present));
        return Math.min(Math.max(0, produced), room);
    }

    /**
     * Whether two stacks of a magazine merge. Loaded magazines carry no per-stack state, so any two of the same
     * family stack up to the item's stack size.
     *
     * @param a one magazine family
     * @param b the other
     * @return true when they are the same family
     */
    public static boolean canMerge(MagazineType a, MagazineType b) {
        return a == b;
    }
}
