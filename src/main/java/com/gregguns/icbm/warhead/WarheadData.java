package com.gregguns.icbm.warhead;

/**
 * The two values a warhead stores in NBT, {@code explosive} and {@code fill}, and the rules that make a hand-edited
 * or stale stack safe: an unknown or missing explosive reads as {@link #DEFAULT_EXPLOSIVE}, and the fill is clamped to
 * {@code 1..cap}. Immutable. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead item; GGS-ICBM-02-01, -02)
public final class WarheadData {

    /** The explosive a stack without a valid one reads as. */
    public static final String DEFAULT_EXPLOSIVE = "condensed";

    private final String explosive;
    private final int fill;

    private WarheadData(String explosive, int fill) {
        this.explosive = explosive;
        this.fill = fill;
    }

    /**
     * @param explosive the stored id; null, empty or not a known id (per {@code known}) falls back to the default
     * @param fill      the stored fill
     * @param cap       the fill cap of the explosive on this casing
     * @param known     whether the id is registered
     * @return the safe value
     */
    public static WarheadData of(String explosive, int fill, int cap, boolean known) {
        String id = explosive == null || explosive.isEmpty() || !known ? DEFAULT_EXPLOSIVE : explosive;
        return new WarheadData(id, Math.max(1, Math.min(fill, Math.max(1, cap))));
    }

    /** @return the explosive id */
    public String explosive() {
        return explosive;
    }

    /** @return the fill, 1 to the cap */
    public int fill() {
        return fill;
    }
}
