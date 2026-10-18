package com.gregguns.gun.net;

import com.gregguns.network.validation.Rejection;
import com.gregguns.network.validation.Violation;

/**
 * The held-gun rule shared by every gun intent packet, as plain Java so it can be unit tested without a player.
 *
 * <p>
 * The client names the hotbar slot it believes is selected. The server accepts the intent only if that is really the
 * selected slot and a gun is in it; a slot switched while the packet was in flight is dropped harmlessly.
 */
public final class GunPacketChecks {

    /** Hotbar size: the only slots a held item can be in. */
    public static final int HOTBAR_SLOTS = 9;

    private GunPacketChecks() {}

    /**
     * @param slot     the slot the packet claims, already range-checked
     * @param heldSlot the sender's actual selected hotbar slot
     * @param holdsGun true if the item in the selected slot is a gun
     * @return the violation, or {@code null} if the intent may proceed
     */
    public static Violation check(int slot, int heldSlot, boolean holdsGun) {
        if (slot != heldSlot) {
            return new Violation(Rejection.OUT_OF_RANGE, "slot " + slot + " is not the selected slot " + heldSlot);
        }
        if (!holdsGun) {
            return new Violation(Rejection.TARGET_MISSING, "no gun in the selected slot " + heldSlot);
        }
        return null;
    }

    /** What the server sees in the sender's hands. */
    public static final class Held {

        private final int selectedSlot;
        private final boolean holdsGun;

        /**
         * @param selectedSlot the selected hotbar slot
         * @param holdsGun     true if it holds a gun
         */
        public Held(int selectedSlot, boolean holdsGun) {
            this.selectedSlot = selectedSlot;
            this.holdsGun = holdsGun;
        }

        /** @return the selected hotbar slot */
        public int selectedSlot() {
            return selectedSlot;
        }

        /** @return true if a gun is held */
        public boolean holdsGun() {
            return holdsGun;
        }
    }
}
