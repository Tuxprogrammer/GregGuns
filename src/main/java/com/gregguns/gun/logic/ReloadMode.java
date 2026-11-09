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
     * {@link GunStats#roundsPerItem()} rounds (shell-by-shell launchers). Rounds already loaded stay.
     */
    INCREMENTAL,
    /**
     * One chamber at a time (revolvers and shotguns): each chamber takes the full reload time divided by the
     * magazine size, the reload goes on by itself while ammo lasts, and a trigger pull between chambers stops it and
     * fires what is loaded. Each round costs one item for a one-round item (a shell); an item worth several rounds (the
     * revolver's 9mm bullet, 6 rounds) is paid when its first round goes in and the rest stay on the gun as chamber
     * credit ({@link GunState#credit()}), so nothing is lost. Rounds already loaded stay. Design record 0026.
     */
    CHAMBER
}
