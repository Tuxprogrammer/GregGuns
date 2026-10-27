package com.gregguns.icbm.production;

import java.util.List;

import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * The matching rule of the NASA Workbench page, on plain keys (D8): which slot holds what is reduced to a key string
 * ({@code compressed:Steel} for a GT compressed plate, {@code modid:name@damage} for a Galacticraft fin or engine, the
 * registry name for a GregGuns part) by the container, and this class decides
 * whether the slots make a body and with how much guidance. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NASA Workbench page)
public final class MissileBenchRules {

    /** Number of fin slots on every page. */
    public static final int FIN_SLOTS = 4;

    private MissileBenchRules() {}

    /** @return the key of a plate, fin or engine ingredient, such as {@code compressed:Steel} */
    public static String keyOf(Ing gt) {
        return com.gregguns.icbm.compat.GalacticraftParts.resolve(gt.id());
    }

    /**
     * @param spec the page's body
     * @return how many plate slots the page has
     */
    public static int plateSlots(MissileBuildSpec spec) {
        return spec.plates()
            .count();
    }

    /**
     * @param spec     the page's body
     * @param plates   the key in each plate slot, null for empty; length {@link #plateSlots}
     * @param fins     the key in each fin slot, null for empty; length {@link #FIN_SLOTS}
     * @param engine   the key in the engine slot (a Galacticraft part id), null for empty
     * @param guidance the key in the guidance slot (a part's registry name), null for empty
     * @return the guidance steps of the body this makes (0 to 3), or -1 when the slots do not make a body
     */
    public static int match(MissileBuildSpec spec, List<String> plates, List<String> fins, String engine,
        String guidance) {
        if (plates == null || fins == null || plates.size() != plateSlots(spec) || fins.size() != FIN_SLOTS) {
            return -1;
        }
        String plateKey = keyOf(spec.plates());
        for (String p : plates) {
            if (!plateKey.equals(p)) {
                return -1;
            }
        }
        String finKey = keyOf(spec.fins());
        for (String f : fins) {
            if (!finKey.equals(f)) {
                return -1;
            }
        }
        if (engine == null || !engine.equals(keyOf(spec.engine()))) {
            return -1;
        }
        if (guidance == null) {
            return -1;
        }
        IcbmPart computer = IcbmPart.byRegistryName(guidance);
        return computer == null ? -1 : spec.guidanceOf(computer);
    }

    /**
     * @param spec a page's body
     * @param key  a key
     * @return true when some slot of the page takes this key (a plate, a fin, the engine or an allowed computer)
     */
    public static boolean accepts(MissileBuildSpec spec, String key) {
        if (key == null) {
            return false;
        }
        if (key.equals(keyOf(spec.plates())) || key.equals(keyOf(spec.fins())) || key.equals(keyOf(spec.engine()))) {
            return true;
        }
        IcbmPart computer = IcbmPart.byRegistryName(key);
        return computer != null && spec.guidanceOf(computer) >= 0;
    }
}
