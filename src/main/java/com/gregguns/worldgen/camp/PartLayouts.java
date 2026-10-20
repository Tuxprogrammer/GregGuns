package com.gregguns.worldgen.camp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.gregguns.worldgen.StructureBiomes.CampColor;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;

/**
 * Where the placer finds the layouts of a camp part. The shipped implementation reads the {@link LayoutRegistry};
 * tests and the scenario supply placeholders. A part that returns no layouts is built as the cobblestone fallback.
 */
// Spec: camp_parts.md (builder decides fixed-size layouts per part, picked by leaf size)
public interface PartLayouts {

    /** All size variants available for a part in a camo color; the placer picks the largest that fits the leaf. */
    List<Layout> choices(CampPart part, CampColor color);

    /**
     * Registry-backed choices: for each tier 1..{@link CampPart#TIERS} the color-specific id (for example
     * {@code camp_tent_2_snow}) wins over the plain id ({@code camp_tent_2}); a missing or disabled layout is skipped.
     */
    final class FromRegistry implements PartLayouts {

        private final LayoutRegistry registry;

        public FromRegistry(LayoutRegistry registry) {
            this.registry = registry;
        }

        /** The plain layout id of every part and tier: the ones that must exist. */
        public static List<String> plainIds() {
            List<String> ids = new ArrayList<>();
            for (CampPart p : CampPart.values()) {
                for (int tier = 1; tier <= p.shippedTiers(); tier++) {
                    ids.add(p.layoutId(tier));
                }
            }
            return ids;
        }

        /** The per-color variant ids: optional, most do not exist, and the plain id is the fallback. */
        public static List<String> variantIds() {
            List<String> ids = new ArrayList<>();
            for (CampPart p : CampPart.values()) {
                for (int tier = 1; tier <= p.shippedTiers(); tier++) {
                    for (CampColor c : CampColor.values()) {
                        ids.add(p.layoutId(tier, suffix(c)));
                    }
                }
            }
            return ids;
        }

        /** Every layout id the registry should be asked to load for the camp: plain and per-color, all tiers. */
        public static List<String> allIds() {
            List<String> ids = new ArrayList<>();
            for (CampPart p : CampPart.values()) {
                for (int tier = 1; tier <= p.shippedTiers(); tier++) {
                    ids.add(p.layoutId(tier));
                    for (CampColor c : CampColor.values()) {
                        ids.add(p.layoutId(tier, suffix(c)));
                    }
                }
            }
            return ids;
        }

        @Override
        public List<Layout> choices(CampPart part, CampColor color) {
            List<Layout> out = new ArrayList<>();
            for (int tier = 1; tier <= CampPart.TIERS; tier++) {
                Layout l = registry.get(part.layoutId(tier, suffix(color)));
                if (l == null) {
                    l = registry.get(part.layoutId(tier));
                }
                if (l != null) {
                    out.add(l);
                }
            }
            return out;
        }
    }

    /** The id suffix of a camo color. The Nether has no camps (dimension whitelist), so it reads as woodland. */
    static String suffix(CampColor color) {
        return (color == CampColor.NETHER ? CampColor.WOODLAND : color).name()
            .toLowerCase(Locale.ROOT);
    }
}
