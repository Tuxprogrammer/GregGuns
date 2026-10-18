package com.gregguns.gun.logic;

/** How a reload moves ammo from the inventory into the gun. */
public enum ReloadMode {

    /**
     * The whole magazine is swapped: {@link GunStats#ammoItemsPerReload()} items are consumed all or nothing and the
     * gun ends full. Rounds still loaded are handed back (GUN-13).
     */
    MAGAZINE,
    /**
     * Rounds are topped up: up to {@link GunStats#ammoItemsPerReload()} items are consumed, each worth
     * {@link GunStats#roundsPerItem()} rounds (shell-by-shell guns). Rounds already loaded stay.
     */
    INCREMENTAL
}
