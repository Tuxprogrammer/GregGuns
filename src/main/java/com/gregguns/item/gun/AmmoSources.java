package com.gregguns.item.gun;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

/**
 * The hook through which other modules add inventories the gun reload path searches <em>before</em> the player's main
 * inventory. The armor module registers the Ammo Pouch here (design 0009 decision 30; GGS-05-03); {@link
 * PlayerAmmoStore} consults it for every count and removal, so a reload, the HUD count and a forced-reload refund all
 * see the same ammo.
 *
 * <p>
 * A provider appends zero or more inventories for a player. Each inventory must write its changes back itself in
 * {@code setInventorySlotContents} / {@code markDirty}. Registered once at startup (the list is a registry, not
 * gameplay state). Both sides: the client reads it for the ammo HUD.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03); docs/gregguns-spec/02-guns/gun-lifecycle.md section 7
public final class AmmoSources {

    /** Supplies the extra ammo inventories a player carries. */
    public interface Provider {

        /**
         * @param player the shooter
         * @param out    append the player's extra ammo inventories here, in search order
         */
        void collect(EntityPlayer player, List<IInventory> out);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private AmmoSources() {}

    /**
     * Registers a provider. Call during startup.
     *
     * @param provider the provider
     */
    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    /**
     * @param player a player
     * @return the extra inventories to search before the main inventory, in provider order; empty when none
     */
    public static List<IInventory> extra(EntityPlayer player) {
        if (PROVIDERS.isEmpty()) {
            return new ArrayList<>(0);
        }
        List<IInventory> out = new ArrayList<>(2);
        for (Provider p : PROVIDERS) {
            p.collect(player, out);
        }
        return out;
    }
}
