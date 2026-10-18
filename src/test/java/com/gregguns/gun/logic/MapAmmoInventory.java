package com.gregguns.gun.logic;

import java.util.HashMap;
import java.util.Map;

/** Test double: an item counter per ammo type, plus a separate tally of loose rounds handed back. */
final class MapAmmoInventory implements AmmoInventory {

    final Map<String, Integer> items = new HashMap<>();
    final Map<String, Integer> rounds = new HashMap<>();

    MapAmmoInventory with(String type, int count) {
        items.put(type, count);
        return this;
    }

    int items(String type) {
        return items.getOrDefault(type, 0);
    }

    int rounds(String type) {
        return rounds.getOrDefault(type, 0);
    }

    @Override
    public int count(String ammoType) {
        return items(ammoType);
    }

    @Override
    public int withdraw(String ammoType, int maxItems) {
        int taken = Math.min(items(ammoType), Math.max(0, maxItems));
        items.put(ammoType, items(ammoType) - taken);
        return taken;
    }

    @Override
    public void depositItems(String ammoType, int count) {
        items.merge(ammoType, count, Integer::sum);
    }

    @Override
    public void depositRounds(String ammoType, int count) {
        rounds.merge(ammoType, count, Integer::sum);
    }
}
