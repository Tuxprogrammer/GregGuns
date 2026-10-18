package com.gregguns.item.gun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.GunCatalogue;
import com.gregguns.gun.logic.GunCatalogue.AmmoIds;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadStateMachine;

class StoreAmmoInventoryTest {

    /** A store with a configurable number of free item slots. */
    static final class FakeStore implements AmmoStore {

        final Map<String, Integer> held = new HashMap<>();
        final Map<String, Integer> dropped = new HashMap<>();
        int room = Integer.MAX_VALUE;

        FakeStore with(String id, int n) {
            held.put(id, n);
            return this;
        }

        @Override
        public int count(String itemId) {
            return held.getOrDefault(itemId, 0);
        }

        @Override
        public int remove(String itemId, int max) {
            int taken = Math.min(count(itemId), max);
            held.put(itemId, count(itemId) - taken);
            return taken;
        }

        @Override
        public int add(String itemId, int amount) {
            int fits = Math.min(amount, room);
            room -= fits;
            held.merge(itemId, fits, Integer::sum);
            return amount - fits;
        }

        @Override
        public void drop(String itemId, int amount) {
            dropped.merge(itemId, amount, Integer::sum);
        }
    }

    private static final GunStats PISTOL = GunCatalogue.PISTOL;

    @Test
    void countsAndWithdrawsFromTheStore() {
        FakeStore store = new FakeStore().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 3);
        StoreAmmoInventory inv = new StoreAmmoInventory(store, 6);
        assertEquals(3, inv.count(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
        assertEquals(1, inv.withdraw(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1));
        assertEquals(2, inv.count(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
        assertEquals(2, inv.withdraw(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 5), "never more than is held");
        assertEquals(0, inv.withdraw(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1));
    }

    @Test
    void nonPositiveRequestsDoNothing() {
        FakeStore store = new FakeStore().with("x", 4);
        StoreAmmoInventory inv = new StoreAmmoInventory(store, 6);
        assertEquals(0, inv.withdraw("x", 0));
        assertEquals(0, inv.withdraw("x", -3));
        inv.depositItems("x", -2);
        inv.depositRounds("x", -12);
        assertEquals(4, store.count("x"));
    }

    @Test
    void depositedItemsThatDoNotFitAreDroppedNotVoided() {
        // ITEM-07 / CORE-04: a full inventory must neither duplicate nor delete the overflow.
        FakeStore store = new FakeStore();
        store.room = 1;
        new StoreAmmoInventory(store, 6).depositItems(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY, 3);
        assertEquals(1, store.count(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY));
        assertEquals(2, (int) store.dropped.get(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY));
    }

    @Test
    void roundsBecomeWholeLooseItems() {
        FakeStore store = new FakeStore();
        new StoreAmmoInventory(store, 6).depositRounds(AmmoIds.NINE_MM, 17);
        assertEquals(2, store.count(AmmoIds.NINE_MM), "17 rounds is two bullets worth 6 rounds each");
    }

    @Test
    void roundsPerLooseItemForThePistolIsSix() {
        assertEquals(6, GunItems.roundsPerLooseItem(PISTOL));
    }

    @Test
    void doesNotReproduceGUN13_forcedReloadKeepsLoadedRounds() {
        FakeStore store = new FakeStore().with(AmmoIds.NINE_MM_PISTOL_MAGAZINE, 1);
        StoreAmmoInventory inv = new StoreAmmoInventory(store, GunItems.roundsPerLooseItem(PISTOL));
        GunState state = new GunState(12);
        ReloadStateMachine reload = new ReloadStateMachine();
        assertEquals(ReloadStateMachine.Event.STARTED, reload.start(PISTOL, state, inv));
        ReloadStateMachine.Event last = ReloadStateMachine.Event.NONE;
        for (int i = 0; i < PISTOL.reloadTicks(); i++) {
            last = reload.tick(PISTOL, state, inv);
        }
        assertEquals(ReloadStateMachine.Event.COMPLETED, last);
        assertEquals(18, state.loaded());
        assertEquals(2, store.count(AmmoIds.NINE_MM), "12 rounds left in the gun come back as 2 bullets");
        assertEquals(1, store.count(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY));
        assertEquals(0, store.count(AmmoIds.NINE_MM_PISTOL_MAGAZINE));
    }
}
