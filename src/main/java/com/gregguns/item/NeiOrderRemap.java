package com.gregguns.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the {@link NeiOrder} list into "effective ids" for NEI's Item ID sort, so GregGuns items sort in table order
 * even when a saved world gave them different numeric ids.
 *
 * <p>
 * The trick keeps NEI's sort a valid total order. The GregGuns items occupy some set of numeric ids. This hands those
 * same ids back in list order: the first listed item gets the smallest of them, the next the next smallest, and so on.
 * An item outside the list keeps its own id. Because the mapping is a permutation of the ids GregGuns already holds,
 * every other mod's items still compare to GregGuns items exactly as they did, and no comparison cycle can appear.
 * Pure logic, no Minecraft types.
 */
// Spec: docs/gregguns-spec/06-items/nei-order.md (existing worlds)
public final class NeiOrderRemap {

    private NeiOrderRemap() {}

    /**
     * @param orderedNames the list order (bare or qualified names, all in one style)
     * @param idOf         the numeric id of a name, or {@code null} when it is not registered (module off, dev item)
     * @return a map from each registered listed name to its effective id; names the game lacks are left out
     */
    public static Map<String, Integer> effectiveIds(List<String> orderedNames,
        java.util.function.Function<String, Integer> idOf) {
        List<String> present = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (String name : orderedNames) {
            Integer id = idOf.apply(name);
            if (id != null) {
                present.add(name);
                ids.add(id);
            }
        }
        Collections.sort(ids);
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < present.size(); i++) {
            out.put(present.get(i), ids.get(i));
        }
        return out;
    }
}
