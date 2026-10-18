package com.gregguns.recipe;

import java.util.Map;
import java.util.Set;

import com.gregguns.api.Tier;

/**
 * The D1 gate rule for machine recipes, as pure logic: every recipe has at least one input of its tier's gate material,
 * except the fixed exemption list, and a GregGuns input counts when that item is itself gated.
 *
 * <p>
 * Spec: docs/design/0001-tier-map.md (gate materials), docs/gregguns-spec/06-items/recipes.md (GGS-06-36). Both sides,
 * no Minecraft types.
 */
public final class GateRule {

    private GateRule() {}

    /**
     * The GT material name that gates a tier's recipes.
     *
     * @param tier a tier from Steam to IV; later tiers are not defined until a recipe needs them
     * @return the GT material name from the tier map
     * @throws IllegalArgumentException for a tier above IV
     */
    public static String gateMaterial(Tier tier) {
        switch (tier) {
            case STEAM:
                return "Bronze";
            case LV:
                return "Steel";
            case MV:
                return "Aluminium";
            case HV:
                return "StainlessSteel";
            case EV:
                return "Titanium";
            case IV:
                return "TungstenSteel";
            default:
                throw new IllegalArgumentException("gate material for " + tier + " is not defined yet");
        }
    }

    /**
     * Whether a row meets the gate rule.
     *
     * @param row      the recipe row
     * @param byOutput every row of the table keyed by output name, to follow GregGuns inputs to their own recipes
     * @param exempt   outputs excused from the rule
     * @return {@code true} when the output is exempt, or an input carries the tier's gate material directly or
     *         through a gated GregGuns item
     */
    public static boolean satisfied(ProductionTable.Row row, Map<String, ProductionTable.Row> byOutput,
        Set<String> exempt) {
        if (exempt.contains(row.output())) {
            return true;
        }
        String gate = gateMaterial(row.tier());
        for (ProductionTable.Input input : row.inputs()) {
            if (!input.isGregGuns()) {
                if (gate.equals(input.material())) {
                    return true;
                }
                continue;
            }
            ProductionTable.Row made = byOutput.get(input.itemName());
            if (made != null && made != row && satisfied(made, byOutput, exempt)) {
                return true;
            }
        }
        return false;
    }
}
