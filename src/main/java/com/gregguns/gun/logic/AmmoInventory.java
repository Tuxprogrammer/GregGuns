package com.gregguns.gun.logic;

/**
 * What a reload needs from the shooter's inventory, with no Minecraft types. The item glue adapts the player's
 * inventory (ammo pouch slots first, then the main inventory); an NPC uses {@link #INFINITE}.
 *
 * <p>
 * All counts are items of the given ammo type id (see {@link GunCatalog.AmmoIds}), except
 * {@link #depositRounds}, which counts rounds. The logic always asks this interface; it never trusts a client count.
 * Implementations run on the server thread. Overflow that does not fit (full inventory) is the implementation's
 * problem: it drops the excess at the shooter, as Techguns does.
 */
public interface AmmoInventory {

    /** An unlimited source that accepts and discards everything: NPCs and turrets never spend ammo. */
    AmmoInventory INFINITE = new AmmoInventory() {

        @Override
        public int count(String ammoType) {
            return Integer.MAX_VALUE;
        }

        @Override
        public int withdraw(String ammoType, int maxItems) {
            return Math.max(0, maxItems);
        }

        @Override
        public void depositItems(String ammoType, int items) {}

        @Override
        public void depositRounds(String ammoType, int rounds) {}

        @Override
        public ChargeSource charge() {
            return ChargeSource.INFINITE;
        }
    };

    /**
     * The charge an energy gun spends from (GGS-02-12), read by {@link GunController} when the gun's stats are
     * energy stats. The item glue returns an adapter over the stack's IC2 charge; creative and NPC shooters use
     * {@link ChargeSource#INFINITE}. A round gun never asks.
     */
    default ChargeSource charge() {
        return ChargeSource.NONE;
    }

    /** Items of {@code ammoType} available. A pure query. */
    int count(String ammoType);

    /**
     * Removes up to {@code maxItems} items of {@code ammoType}.
     *
     * @return how many were actually removed, between 0 and {@code maxItems}
     */
    int withdraw(String ammoType, int maxItems);

    /** Hands {@code items} items (for example empty magazines) to the shooter. */
    void depositItems(String ammoType, int items);

    /**
     * Hands back loose rounds, for example the rounds left in a magazine on a forced reload. The implementation
     * decides how to represent them (bullet items, a partly filled magazine); it must not lose them (GUN-13).
     */
    void depositRounds(String ammoType, int rounds);
}
