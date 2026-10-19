package com.gregguns.item.gun;

import com.gregguns.gun.logic.AmmoInventory;
import com.gregguns.gun.logic.ChargeSource;

/**
 * Adapts an {@link AmmoStore} to the gun logic's {@link AmmoInventory}. Pure Java.
 *
 * <p>
 * Items come out of the store and go back into it; anything that does not fit is dropped, never voided (ITEM-07 /
 * CORE-04). Rounds handed back by a forced reload become loose-round items, one per {@code roundsPerLooseItem}
 * rounds, so the loaded rounds are kept (GUN-13).
 *
 * <p>
 * Spec: gun-lifecycle.md section 7 (forced reload refunds {@code floor(oldAmmo / roundsPerItem)} bullet items).
 */
// Deviation: a refund of fewer rounds than one loose item (at most 5 for the 9mm pistol) cannot be represented as an
// item and is lost, as in Techguns' floor. Conserving it would need a per-player round bank; recorded as an open
// question rather than rounding up, which would let a player gain a round per swap.
public final class StoreAmmoInventory implements AmmoInventory {

    private final AmmoStore store;
    private final int roundsPerLooseItem;
    private final ChargeSource charge;

    /**
     * @param store              the shooter's ammo store
     * @param roundsPerLooseItem rounds one loose round item is worth (6 for the 9mm bullet); at least 1
     */
    public StoreAmmoInventory(AmmoStore store, int roundsPerLooseItem) {
        this(store, roundsPerLooseItem, ChargeSource.NONE);
    }

    /**
     * @param store              the shooter's ammo store
     * @param roundsPerLooseItem rounds one loose round item is worth; at least 1
     * @param charge             the held energy gun's charge, returned by {@link #charge()}
     */
    public StoreAmmoInventory(AmmoStore store, int roundsPerLooseItem, ChargeSource charge) {
        this.store = store;
        this.roundsPerLooseItem = Math.max(1, roundsPerLooseItem);
        this.charge = charge;
    }

    @Override
    public ChargeSource charge() {
        return charge;
    }

    @Override
    public int count(String ammoType) {
        return store.count(ammoType);
    }

    @Override
    public int withdraw(String ammoType, int maxItems) {
        return maxItems <= 0 ? 0 : store.remove(ammoType, maxItems);
    }

    @Override
    public void depositItems(String ammoType, int items) {
        if (items <= 0) {
            return;
        }
        int leftover = store.add(ammoType, items);
        if (leftover > 0) {
            store.drop(ammoType, leftover);
        }
    }

    @Override
    public void depositRounds(String ammoType, int rounds) {
        depositItems(ammoType, Math.max(0, rounds) / roundsPerLooseItem);
    }
}
