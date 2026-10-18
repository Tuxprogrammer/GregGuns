package com.gregguns.core.selfcheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The recipe-coverage rule for crafting components: every registered GregGuns component must be the output of at least
 * one recipe, unless it is on the explicit allowlist with a reason (loot-only items, items made by use). A stale or
 * reasonless allowlist entry is a violation too, so the list cannot rot.
 *
 * <p>
 * Names are full registry names ({@code gregguns:part_receiver_iron}). Pure logic, both sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (GGS-06-34, GGS-06-47), docs/testing/self-check.md
public final class ComponentChecks {

    private ComponentChecks() {}

    /**
     * @param components    the registered components
     * @param recipeOutputs every item that is the output of some recipe (crafting table or any GT map)
     * @param allowlist     components excused from needing a recipe, with the reason
     * @param deferred      components whose recipe is skipped because a module it needs is off
     * @return the violations, in component order, then allowlist order
     */
    public static List<String> check(Collection<String> components, Collection<String> recipeOutputs,
        Map<String, String> allowlist, Collection<String> deferred) {
        List<String> violations = new ArrayList<>();
        for (String component : components) {
            boolean hasRecipe = recipeOutputs.contains(component);
            if (!hasRecipe && !allowlist.containsKey(component) && !deferred.contains(component)) {
                violations.add(
                    "component " + component
                        + " has no recipe; add one to ComponentTable or allowlist it in PartCatalog with a reason");
            }
        }
        for (Map.Entry<String, String> entry : allowlist.entrySet()) {
            String reason = entry.getValue();
            if (reason == null || reason.trim()
                .isEmpty()) {
                violations.add("allowlist entry " + entry.getKey() + " has no reason");
            }
            if (recipeOutputs.contains(entry.getKey())) {
                violations.add("allowlist entry " + entry.getKey() + " now has a recipe; remove it from the allowlist");
            }
        }
        return violations;
    }
}
