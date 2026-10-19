package com.gregguns.utility.storage;

/**
 * What the Weapon Chest accepts: items registered in the {@code gregguns} namespace that are not blocks. The
 * Reinforced Chest accepts anything and does not use this class. Plain Java.
 */
// Spec: docs/gregguns-spec/07-blocks/storage-chests.md (open question, decided in docs M6 plan decision 12)
// Deviation: Techguns' Weapon Chest takes any item; this one is a guns, ammo and gear locker.
public final class WeaponChestFilter {

    private static final String PREFIX = "gregguns:";

    private WeaponChestFilter() {}

    /**
     * @param registryName the item's registry name such as {@code gregguns:pistol_9mm}; null for an unregistered item
     * @param blockItem    whether the item is an item block
     * @return true when the Weapon Chest takes it
     */
    public static boolean accepts(String registryName, boolean blockItem) {
        return registryName != null && !blockItem
            && registryName.startsWith(PREFIX)
            && registryName.length() > PREFIX.length();
    }
}
