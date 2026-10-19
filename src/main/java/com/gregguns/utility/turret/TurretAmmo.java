package com.gregguns.utility.turret;

import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;

/**
 * The ammo rules of the Turret: use a round per shot and reload instantly from the ammo slots. Pure logic, no
 * Minecraft types; the MTE adapts its slots to {@link Store}.
 *
 * <p>
 * A reload of a magazine gun takes {@code ammoItemsPerReload} items all or nothing and refills the gun; an
 * incremental gun takes up to that many items, each worth {@code roundsPerItem} rounds. A magazine reload returns the
 * gun's empty container, once per magazine. The empties go to the output slots; whatever does not fit is dropped, but
 * only when something is left over (MUTL-24: Techguns dropped a zero-size stack every time).
 */
// Spec: docs/techguns-spec/09-machines-utility/turret.md (Ammunition), docs/techguns-spec/BUGS.md (MUTL-24)
// Deviation: the spec's "keep taking ammo items up to ammoCount" is mapped onto ReloadMode (MAGAZINE all or nothing,
// INCREMENTAL topped up), so the Turret reloads exactly as a player's gun does.
public final class TurretAmmo {

    private TurretAmmo() {}

    /** The ammo slots and the empty-magazine output, as the rules need them. */
    public interface Store {

        /**
         * @param ammoId an ammo item id (a GregGuns registry name without the mod id)
         * @return how many of that item the ammo slots hold
         */
        int count(String ammoId);

        /**
         * @param ammoId a registry name
         * @param max    most to take
         * @return how many were taken from the ammo slots
         */
        int take(String ammoId, int max);

        /**
         * Puts empty containers into the output slots; the implementation drops what does not fit.
         *
         * @param emptyId a registry name
         * @param count   how many, at least 1
         */
        void putEmpty(String emptyId, int count);
    }

    /** The outcome of one shot request. */
    public static final class Result {

        private final boolean fired;
        private final int loaded;

        Result(boolean fired, int loaded) {
            this.fired = fired;
            this.loaded = loaded;
        }

        /** @return true when a round was used and the shot is allowed */
        public boolean fired() {
            return fired;
        }

        /** @return the rounds loaded in the gun afterwards */
        public int loaded() {
            return loaded;
        }
    }

    /**
     * Uses one shot's rounds, reloading first when the gun is short.
     *
     * @param stats  the mounted gun's stats
     * @param loaded rounds loaded now
     * @param store  the ammo slots
     * @return whether the shot is allowed and the rounds loaded afterwards
     */
    public static Result consume(GunStats stats, int loaded, Store store) {
        int rounds = Math.max(0, loaded);
        int perShot = stats.ammoPerShot();
        if (rounds < perShot) {
            rounds = reload(stats, rounds, store);
        }
        if (rounds < perShot) {
            return new Result(false, rounds);
        }
        return new Result(true, rounds - perShot);
    }

    private static int reload(GunStats stats, int rounds, Store store) {
        String ammo = stats.ammoType();
        if (stats.reloadMode() == ReloadMode.MAGAZINE) {
            int items = stats.ammoItemsPerReload();
            if (store.count(ammo) < items) {
                return rounds;
            }
            int taken = store.take(ammo, items);
            if (taken < items) {
                return rounds;
            }
            giveEmpties(stats, taken, store);
            return stats.magazineSize();
        }
        int room = Math.max(0, stats.magazineSize() - rounds) / Math.max(1, stats.roundsPerItem());
        int taken = store.take(ammo, Math.min(stats.ammoItemsPerReload(), room));
        return Math.min(stats.magazineSize(), rounds + taken * stats.roundsPerItem());
    }

    private static void giveEmpties(GunStats stats, int count, Store store) {
        String empty = stats.emptyContainerType();
        if (empty != null && !empty.isEmpty() && count > 0) {
            store.putEmpty(empty, count);
        }
    }

    /**
     * MUTL-24: whether an overflow of empties should be dropped as an item at all.
     *
     * @param overflow items that did not fit
     * @return true only for a positive overflow, so no zero-size stack is ever spawned
     */
    public static boolean shouldDrop(int overflow) {
        return overflow > 0;
    }
}
