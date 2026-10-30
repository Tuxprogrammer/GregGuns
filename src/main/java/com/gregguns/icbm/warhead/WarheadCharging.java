package com.gregguns.icbm.warhead;

/**
 * The rules for loading Plastic Explosive charges into a warhead (D282, the D23 knob). One charge is one unit of fill.
 * Charges go into an inert warhead (with or without a payload) or top up a live one, up to the casing's cap. Charges
 * alone fill only a T1 casing, as condensed; a bare T2 to T5 casing needs a payload first. A load that would pass the
 * cap is refused whole, so nothing is ever lost. Pure, no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead chain)
public final class WarheadCharging {

    /** The most charges one crafting-grid load takes: nine slots less the warhead. */
    public static final int MAX_PER_CRAFT = 8;

    private WarheadCharging() {}

    /**
     * The blast a load makes.
     *
     * @param casing  the casing
     * @param payload the payload of an inert warhead (a blast id), or null for a bare inert warhead
     * @return the explosive id the live warhead carries: the payload, or condensed for a bare T1 casing; null when a
     *         bare casing above T1 cannot take charges alone
     */
    public static String explosiveFor(WarheadCasing casing, String payload) {
        if (payload != null && !payload.isEmpty()) {
            return payload;
        }
        return casing == WarheadCasing.T1 ? WarheadData.DEFAULT_EXPLOSIVE : null;
    }

    /**
     * @param fill  the charges already loaded, 0 for an inert warhead
     * @param added the charges being added
     * @param cap   the cap of this explosive in this casing
     * @return the fill after the load, or -1 when the load adds nothing or would pass the cap
     */
    public static int loadedFill(int fill, int added, int cap) {
        if (added < 1 || fill < 0) {
            return -1;
        }
        int total = fill + added;
        return total > cap ? -1 : total;
    }

    /**
     * @param fill the charges loaded
     * @param cap  the cap
     * @return the charges that would fill the casing, 0 when it is full
     */
    public static int room(int fill, int cap) {
        return Math.max(0, cap - Math.max(0, fill));
    }
}
