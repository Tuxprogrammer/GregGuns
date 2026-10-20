package com.gregguns.worldgen.camp;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.block.Block;

import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StructureBiomes.CampColor;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.MarkListener;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.ResolvedBlock;
import com.gregguns.worldgen.layout.StampPlan;
import com.gregguns.worldgen.layout.Stamper;

/**
 * Writes a {@link PlacedCamp} one populate window at a time: fence, roads, the parts of every leaf and the cobblestone
 * fallback. Never reads the world and never requests a chunk (GGS-11-09): every write goes through the window-clipped
 * sink and only touches columns of the window. The flag is a separate post-step ({@link #flagStep}).
 *
 * <p>
 * The generator (G9) adopts {@link CampWriter}: call {@code write} for each window the camp {@code overlaps}, then
 * the flag step, then act on the marks. The camo color comes from the center biome's types through
 * {@code StructureBiomes.campColor} (WGEN-32), read once by the caller.
 */
// Spec: camp_parts.md (Base rules, Choosing a part, Fence, Roads); PLAN-M8 section 1 (G10)
public final class CampPlacer implements CampWriter {

    /** Ground level (the first air block) of a column; the default is the anchor Y everywhere. */
    public interface GroundAt {

        int groundY(int x, int z);
    }

    private final Stamper stamper;
    private final PaletteKeys keys;
    private final PartLayouts layouts;
    private final Supplier<TileCampFlag> tiles;

    public CampPlacer(PaletteKeys keys, PartLayouts layouts) {
        this(keys, layouts, TileCampFlag::new);
    }

    public CampPlacer(PaletteKeys keys, PartLayouts layouts, Supplier<TileCampFlag> tiles) {
        this.stamper = new Stamper(keys);
        this.keys = keys;
        this.layouts = layouts;
        this.tiles = tiles;
    }

    @Override
    public int write(WindowedSink sink, PlacedCamp camp, PopulateWindow window, CampColor color, MarkListener marks) {
        final int y = camp.anchorY();
        return write(sink, camp, window, color, (x, z) -> y, marks);
    }

    /** As {@link #write(WindowedSink, PlacedCamp, PopulateWindow, CampColor, MarkListener)} with a per-column ground. */
    public int write(WindowedSink sink, PlacedCamp camp, PopulateWindow window, CampColor color, GroundAt ground,
        MarkListener marks) {
        if (!camp.overlaps(window)) {
            return 0;
        }
        int n = 0;
        n += writeRoads(sink, camp, window, ground);
        n += writeFence(sink, camp, window, ground);
        for (CampLeaf leaf : camp.plan()
            .leaves()) {
            if (!leaf.flagLeaf) {
                n += writeLeaf(sink, camp, leaf, window, color, ground, marks);
            }
        }
        return n;
    }

    @Override
    public CampFlags.FlagPostStep flagStep(PlacedCamp camp, Block flagBlock, float capFactor,
        CampFlags.Recorder recorder) {
        FlagSpec spec = camp.flagSpec(capFactor);
        return (sink, window) -> CampFlags.place(sink, window, flagBlock, spec, tiles, recorder);
    }

    // ---- surface: roads and fence -------------------------------------------------------------------------------

    private int writeRoads(WindowedSink sink, PlacedCamp camp, PopulateWindow window, GroundAt ground) {
        ResolvedBlock gravel = keys.get("minecraft:gravel");
        ResolvedBlock concrete = keys.get("concrete");
        int n = 0;
        for (CampRoad r : camp.plan()
            .roads()) {
            int[] box = camp.worldRect(r.x, r.z, r.w, r.d);
            boolean narrowIsX = box[2] - box[0] < box[3] - box[1];
            int width = r.width();
            for (int x = Math.max(box[0], window.minX()); x <= Math.min(box[2], window.maxX()); x++) {
                for (int z = Math.max(box[1], window.minZ()); z <= Math.min(box[3], window.maxZ()); z++) {
                    int across = narrowIsX ? x - box[0] : z - box[1];
                    // Concrete down the middle once the road is 4 wide; gravel at the edges and on narrow roads.
                    boolean middle = width >= 4 && across > 0 && across < width - 1;
                    if (put(sink, middle ? concrete : gravel, x, ground.groundY(x, z) - 1, z)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private int writeFence(WindowedSink sink, PlacedCamp camp, PopulateWindow window, GroundAt ground) {
        ResolvedBlock bars = keys.get("minecraft:iron_bars");
        int x0 = camp.anchorX();
        int z0 = camp.anchorZ();
        int n = 0;
        for (int x = Math.max(x0, window.minX()); x <= Math.min(x0 + camp.footprintX() - 1, window.maxX()); x++) {
            for (int z = Math.max(z0, window.minZ()); z <= Math.min(z0 + camp.footprintZ() - 1, window.maxZ()); z++) {
                if (camp.plan()
                    .isFence(camp.localX(x, z), camp.localZ(x, z))) {
                    int g = ground.groundY(x, z); // each column at its own ground height
                    n += (put(sink, bars, x, g, z) ? 1 : 0) + (put(sink, bars, x, g + 1, z) ? 1 : 0);
                }
            }
        }
        return n;
    }

    private static boolean put(WindowedSink sink, ResolvedBlock rb, int x, int y, int z) {
        return rb != null && sink.setBlock(x, y, z, rb.block, rb.meta);
    }

    // ---- leaves ---------------------------------------------------------------------------------------------------

    /** Where a layout lands in a leaf. */
    private static final class Fit {

        final Layout layout;
        final int turns;
        final int x;
        final int z;
        final int w;
        final int d;

        Fit(Layout layout, int turns, int x, int z, int w, int d) {
            this.layout = layout;
            this.turns = turns;
            this.x = x;
            this.z = z;
            this.w = w;
            this.d = d;
        }
    }

    private int writeLeaf(WindowedSink sink, PlacedCamp camp, CampLeaf leaf, PopulateWindow window, CampColor color,
        GroundAt ground, MarkListener marks) {
        Fit fit = leaf.part == null ? null : fit(leaf, layouts.choices(leaf.part, color));
        if (fit == null) {
            return writeFallback(sink, camp, leaf, window, ground);
        }
        int[] box = camp.worldRect(fit.x, fit.z, fit.w, fit.d);
        int turns = (fit.turns + camp.rotation()) & 3;
        long variant = camp.plan()
            .seed() * 31L + leaf.index;
        return stamper
            .write(sink, new StampPlan(fit.layout, box[0], camp.anchorY(), box[1], turns, variant), window, marks);
    }

    /**
     * Picks the largest layout that fits the leaf. The entrance faces the road beside the leaf; if nothing fits that
     * way the part turns a quarter either way (thin leaves turn their entrance, camp_parts.md). The part hugs the
     * side it faces, except a tower, which is centered.
     */
    private static Fit fit(CampLeaf leaf, List<Layout> choices) {
        int[] tries = { leaf.facing, (leaf.facing + 1) & 3, (leaf.facing + 3) & 3 };
        for (int q : tries) {
            Layout best = null;
            long bestArea = -1;
            for (Layout l : choices) {
                int fw = l.footprintX(q);
                int fd = l.footprintZ(q);
                if (fw > leaf.w || fd > leaf.d) {
                    continue;
                }
                if (leaf.part == CampPart.TOWER && (fw > leaf.sizeHint || fd > leaf.sizeHint)) {
                    continue;
                }
                long area = (long) fw * fd;
                if (area > bestArea) {
                    bestArea = area;
                    best = l;
                }
            }
            if (best != null) {
                int fw = best.footprintX(q);
                int fd = best.footprintZ(q);
                int cx = leaf.x + (leaf.w - fw) / 2;
                int cz = leaf.z + (leaf.d - fd) / 2;
                int px = cx;
                int pz = cz;
                if (leaf.part != CampPart.TOWER) {
                    switch (q) {
                        case 0: // entrance south: against the south edge
                            pz = leaf.z + leaf.d - fd;
                            break;
                        case 1: // west
                            px = leaf.x;
                            break;
                        case 2: // north
                            pz = leaf.z;
                            break;
                        default: // east
                            px = leaf.x + leaf.w - fw;
                            break;
                    }
                }
                return new Fit(best, q, px, pz, fw, fd);
            }
        }
        return null;
    }

    /** A leaf with no part (or no shipped layout for it): one flat layer of cobblestone, nothing else. */
    private int writeFallback(WindowedSink sink, PlacedCamp camp, CampLeaf leaf, PopulateWindow window,
        GroundAt ground) {
        ResolvedBlock cobble = keys.get("minecraft:cobblestone");
        int[] box = camp.worldRect(leaf.x, leaf.z, leaf.w, leaf.d);
        int n = 0;
        for (int x = Math.max(box[0], window.minX()); x <= Math.min(box[2], window.maxX()); x++) {
            for (int z = Math.max(box[1], window.minZ()); z <= Math.min(box[3], window.maxZ()); z++) {
                if (put(sink, cobble, x, ground.groundY(x, z) - 1, z)) {
                    n++;
                }
            }
        }
        return n;
    }
}
