package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The metal block props of a complex (D308): one real storage block per prop, drawn from a tier ladder that never
 * passes the complex's ceiling. Lower metals are common and the ceiling's own metal is a find: a rung weighs 4 at LV
 * and MV, 2 up to two rungs under the ceiling, and 1 at the top two rungs. Where Chisel has a crate face for a metal
 * the prop uses it; the rest are GT storage blocks. Every prop falls back to a vanilla iron block, so a pack without
 * Chisel or GT still reads. Pure: block names and metas only.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Props); D308
public final class MetalProps {

    /** One prop block: a registry name and meta, with the vanilla block it falls back to. */
    public static final class Prop {

        public final String block;
        public final int meta;
        /** The voltage index of the metal (1 LV up). */
        public final int tier;

        Prop(String block, int meta, int tier) {
            this.block = block;
            this.meta = meta;
            this.tier = tier;
        }

        /** @return the block a run without this one writes instead */
        public String fallback() {
            return FALLBACK;
        }

        @Override
        public String toString() {
            return block + ":" + meta + "@" + tier;
        }
    }

    /** Every prop falls back to this. */
    public static final String FALLBACK = "minecraft:iron_block";

    private static final List<List<Prop>> RUNGS = new ArrayList<List<Prop>>();

    static {
        for (int t = 0; t <= 12; t++) RUNGS.add(new ArrayList<Prop>());
        rung(1, "chisel:iron2", 1); // iron, crate face
        rung(1, "chisel:copperblock", 1);
        rung(1, "chisel:tinblock", 1);
        rung(1, "chisel:bronzeblock", 1);
        rung(1, "chisel:leadblock", 1);
        rung(1, "chisel:steelblock", 1);
        rung(2, "chisel:aluminumblock", 1);
        // GT storage blocks: gt.blockmetalN's materials in LoaderGTBlockFluid order at 5.09.54.205
        rung(3, "gregtech:gt.blockmetal6", 12); // stainless steel
        rung(4, "gregtech:gt.blockmetal7", 9); // titanium
        rung(5, "gregtech:gt.blockmetal7", 12); // tungstensteel
        rung(6, "gregtech:gt.blockmetal8", 9); // HSS-G
        rung(7, "gregtech:gt.blockmetal4", 13); // naquadah alloy
        rung(8, "gregtech:gt.blockmetal5", 2); // neutronium (UV)
        rung(9, "gregtech:gt.blockmetal5", 2); // neutronium (UHV)
        rung(10, "gregtech:gt.blockmetal5", 2); // neutronium (UEV)
        rung(11, "gregtech:gt.blockmetal9", 4); // transcendent metal
        rung(12, "gregtech:gt.blockmetal9", 3); // space-time
    }

    private static void rung(int tier, String block, int meta) {
        RUNGS.get(tier)
            .add(new Prop(block, meta, tier));
    }

    private MetalProps() {}

    /**
     * @param tier    a rung (voltage index 1 to 12)
     * @param ceiling the complex's ceiling
     * @return the rung's weight under that ceiling; 0 above it
     */
    public static int weight(int tier, int ceiling) {
        if (tier < 1 || tier > ceiling || tier >= RUNGS.size()) return 0;
        if (tier >= ceiling - 1) return 1;
        if (tier <= 2) return 4;
        return 2;
    }

    /** @return the props of one rung, read only */
    public static List<Prop> rung(int tier) {
        return tier < 1 || tier >= RUNGS.size() ? Collections.<Prop>emptyList()
            : Collections.unmodifiableList(RUNGS.get(tier));
    }

    /**
     * @param ceiling the complex's ceiling (voltage index)
     * @param roll    any value (a hash of the prop's cell); the same roll gives the same prop
     * @return the prop, never above the ceiling
     */
    public static Prop pick(int ceiling, long roll) {
        int c = Math.max(1, Math.min(RUNGS.size() - 1, ceiling));
        int total = 0;
        for (int t = 1; t <= c; t++) total += weight(t, c);
        long r = Long.remainderUnsigned(roll, total);
        for (int t = 1; t <= c; t++) {
            r -= weight(t, c);
            if (r < 0) {
                List<Prop> rung = RUNGS.get(t);
                return rung.get((int) Long.remainderUnsigned(roll >>> 17, rung.size()));
            }
        }
        return RUNGS.get(1)
            .get(0);
    }
}
