package com.gregguns.item.gun;

/**
 * Where a shooter keeps ammo items, with no Minecraft types: the seam that lets {@link StoreAmmoInventory} be unit
 * tested. {@link PlayerAmmoStore} is the player-inventory implementation. Items are named by GregGuns registry name
 * (for example {@code mag_9mm_pistol}). Server thread.
 */
public interface AmmoStore {

    /** Items of {@code itemId} held, summed over every stack. A pure query. */
    int count(String itemId);

    /**
     * Removes up to {@code max} items of {@code itemId}, taking from any stack.
     *
     * @return how many were removed, between 0 and {@code max}
     */
    int remove(String itemId, int max);

    /**
     * Adds items, filling existing stacks first.
     *
     * @return how many did <em>not</em> fit; the caller drops them
     */
    int add(String itemId, int amount);

    /** Drops {@code amount} items at the shooter so nothing is lost when the inventory is full (ITEM-07). */
    void drop(String itemId, int amount);
}
