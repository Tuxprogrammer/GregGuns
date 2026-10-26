package com.gregguns.worldgen;

import com.gregguns.GregGuns;

import bartworks.common.configs.Configuration;

/**
 * The dimension IDs of Ross128b and its moon Ross128ba, read from BartWorks' own config so a pack that moves them
 * (or turns the system off) is followed without a GregGuns change. Never a literal ID (GGS-11-12). Server side, after
 * BartWorks has loaded its config (the IDs are read when a world is populated, long after that).
 */
// Spec: docs/design/0019-structures-on-ross128.md; docs/gregguns-spec/11-worldgen/structure-placement.md (Dimension
// gate)
public final class Ross128Dimensions {

    private static final int[] NONE = new int[0];
    /** Read once: BartWorks' config is fixed after load ("DO NOT CHANGE AFTER WORLD GENERATION"). */
    private static volatile int[] cached;

    private Ross128Dimensions() {}

    /**
     * @return {Ross128b, Ross128ba} as configured by BartWorks ({@code ross128BID}, {@code ross128BAID}; 64 and 63 in
     *         the pack), or an empty array when the system is switched off ({@code Ross128Enabled}) or BartWorks'
     *         config cannot be read
     */
    // Source: BartWorks config fields, GT5-Unofficial 5.09.54.205:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/configs/Configuration.java#L22-L45
    public static int[] ids() {
        int[] c = cached;
        if (c == null) {
            c = read();
            cached = c;
        }
        return c.clone();
    }

    private static int[] read() {
        try {
            Configuration.CrossModInteractions c = Configuration.crossModInteractions;
            if (!c.Ross128Enabled) {
                return NONE;
            }
            return new int[] { c.ross128BID, c.ross128BAID };
        } catch (Throwable t) { // NoClassDefFoundError if BartWorks is stripped from a custom GT build
            GregGuns.LOG.warn("Ross128 dimension IDs unavailable, structures stay off there: {}", t.toString());
            return NONE;
        }
    }
}
