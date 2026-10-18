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
     * @param tier any tier from Steam to ZPM
     * @return the GT material name from the tier map
     * @throws IllegalArgumentException for a tier with no gate material
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
            case LUV:
                return "HSSG";
            case ZPM:
                return "NaquadahAlloy";
            default:
                throw new IllegalArgumentException("gate material for " + tier + " is not defined yet");
        }
    }

    /**
     * The GT circuit material name ({@code Materials} {@code mName}) of a tier.
     *
     * @param tier a tier from LV to ZPM
     * @return "Basic", "Good", "Advanced", "Data", "Elite", "Master" or "Ultimate"; {@code null} for Steam
     */
    public static String circuitMaterial(Tier tier) {
        switch (tier) {
            case LV:
                return "Basic";
            case MV:
                return "Good";
            case HV:
                return "Advanced";
            case EV:
                return "Data";
            case IV:
                return "Elite";
            case LUV:
                return "Master";
            case ZPM:
                return "Ultimate";
            default:
                return null;
        }
    }

    /**
     * Whether a GT part of the row's own tier carries the tier's gate material by itself: a circuit of the tier's
     * circuit material, or a GT machine part named for the tier ({@code Emitter_ZPM}). The spec gates RC-19 "through
     * the ZPM emitter" and RC-20 "via circuit.EV", so a tier part counts as the gate.
     */
    // Deviation: GGS-06-36 asks for an input of the gate material; the sheet's own gate column names these GT tier
    // parts as the gate for RC-19 and RC-20, so the rule accepts them.
    static boolean carriesTierGate(ProductionTable.Input input, Tier tier) {
        if ("circuit".equals(input.prefix())) {
            return input.material() != null && input.material().equals(circuitMaterial(tier));
        }
        return false;
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
            if (input.special() != null && input.special().startsWith("gt:")
                && input.special().endsWith("_" + row.tier().name().replace("LUV", "LuV"))) {
                return true;
            }
            if (!input.isGregGuns()) {
                if (gate.equals(input.material()) || carriesTierGate(input, row.tier())) {
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
