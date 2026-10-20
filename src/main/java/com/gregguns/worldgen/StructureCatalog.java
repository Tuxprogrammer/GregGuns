package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.gregguns.worldgen.camp.CampFootprint;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;

/**
 * Which size class each natural structure belongs to, whether it floats, and how the pools are built from the loaded
 * layouts. Pure; no Minecraft types. Pool weights are equal (one entry, one share): the Techguns "1/2 or 1/3" odds
 * come from its pick sequence, which a weighted pool replaces (deviation noted in structure-placement.md).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10, GGS-11-15); PLAN-M8 section 2 (Q1, Q2)
public final class StructureCatalog {

    /** How many palette variants a plan may draw; the variant only seeds weighted palette picks (GGS-11-10). */
    public static final int VARIANTS = 8;

    private StructureCatalog() {}

    /**
     * @param s a structure
     * @return its size class
     */
    public static SizeClass sizeClass(StructureId s) {
        switch (s) {
            case FACTORY_SMALL:
            case TRAIN_STATION:
            case GAS_STATION:
                return SizeClass.SMALL;
            case HOUSE_MEDIUM:
            case BIG_BUNKER:
            case ALIEN_BUG_NEST:
                return SizeClass.MEDIUM;
            default:
                return SizeClass.BIG;
        }
    }

    /**
     * @param s a structure
     * @return whether it sits on water (the ships) rather than on land
     */
    public static boolean isWater(StructureId s) {
        return s == StructureId.AIRCRAFT_CARRIER || s == StructureId.SUBMARINE;
    }

    /**
     * @param layoutId a layout id
     * @return the matching structure, or null for an id the catalog does not know (a test layout)
     */
    public static StructureId byId(String layoutId) {
        for (StructureId s : StructureId.values()) {
            if (s.id()
                .equals(layoutId)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Builds the weighted pools: one entry per structure that is enabled and whose layout loaded. A structure with no
     * layout is simply absent, so the pools are empty until layouts exist.
     *
     * @param layouts the loaded layouts
     * @param enabled the per-structure enable flag
     * @return the pools by size class
     */
    public static Map<SizeClass, List<PoolEntry>> pools(LayoutRegistry layouts, Predicate<StructureId> enabled) {
        return pools(layouts, enabled, false);
    }

    /**
     * As {@link #pools(LayoutRegistry, Predicate)}; with {@code camps} the Military Base joins the BIG pool, sized by
     * its own plan (G10) instead of a layout file.
     */
    public static Map<SizeClass, List<PoolEntry>> pools(LayoutRegistry layouts, Predicate<StructureId> enabled,
        boolean camps) {
        Map<SizeClass, List<PoolEntry>> out = new EnumMap<SizeClass, List<PoolEntry>>(SizeClass.class);
        for (SizeClass c : SizeClass.values()) {
            out.put(c, new ArrayList<PoolEntry>());
        }
        for (StructureId s : StructureId.values()) {
            if (!enabled.test(s)) continue;
            if (s == StructureId.MILITARY_BASE) {
                if (camps) {
                    out.get(SizeClass.BIG)
                        .add(new PoolEntry(s.id(), 1, 1, CampFootprint.MAX, CampFootprint::of));
                }
                continue;
            }
            Layout l = layouts.get(s.id());
            if (l == null) continue;
            out.get(sizeClass(s))
                .add(new PoolEntry(s.id(), 1, VARIANTS, new Footprint(l.sizeX(), l.sizeY(), l.sizeZ())));
        }
        return Collections.unmodifiableMap(out);
    }

    /** The layout ids worth loading: every enabled structure with a layout file (the base is planned, not stamped). */
    public static List<String> idsToLoad(Predicate<StructureId> enabled) {
        List<String> ids = new ArrayList<String>();
        for (StructureId s : StructureId.values()) {
            if (enabled.test(s) && s != StructureId.MILITARY_BASE) ids.add(s.id());
        }
        return ids;
    }
}
