package com.gregguns.core.selfcheck;

import java.util.ArrayList;
import java.util.List;

import com.gregguns.api.Tier;

/**
 * Self-check over recipe descriptors: every slot resolves, and no recipe breaks the tier map.
 *
 * <p>
 * Spec: CONTRIBUTING.md (Testing, dev self-check), docs/design/0001-tier-map.md. Both sides, no Minecraft types.
 */
public final class RecipeChecks {

    private RecipeChecks() {}

    /**
     * Checks one recipe.
     *
     * @param recipe the recipe, already filtered to those that involve a GregGuns item
     * @return one message per violation, each prefixed with the recipe id; empty when the recipe is fine
     */
    public static List<String> check(RecipeDescriptor recipe) {
        List<String> violations = new ArrayList<>();
        String prefix = "recipe " + recipe.id() + ": ";

        for (StackDescriptor input : recipe.inputs()) {
            checkResolves(prefix, "input", input, recipe.emptySlotsAllowed(), violations);
        }
        for (StackDescriptor output : recipe.outputs()) {
            checkResolves(prefix, "output", output, false, violations);
        }

        List<Tier> inputTiers = new ArrayList<>();
        for (StackDescriptor input : recipe.inputs()) {
            inputTiers.add(input.isGregGuns() && !input.isUpgrade() ? input.tier() : null);
        }
        for (StackDescriptor output : recipe.outputs()) {
            if (!output.isGregGuns() || !output.isPresent()) {
                continue;
            }
            if (output.tier() == null) {
                violations.add(prefix + "output " + output.label() + " has no tier, so the tier map cannot be checked");
                continue;
            }
            for (RecipeTierCheck.Violation violation : RecipeTierCheck.check(output.tier(), recipe.eut(), inputTiers)) {
                violations.add(prefix + "output " + output.label() + ": " + violation.message());
            }
        }
        return violations;
    }

    private static void checkResolves(String prefix, String role, StackDescriptor slot, boolean emptySlotAllowed,
        List<String> violations) {
        switch (slot.kind()) {
            case EMPTY_SLOT:
                if (!emptySlotAllowed) {
                    violations.add(prefix + role + " is a null or empty slot where an item is required");
                }
                break;
            case ORE_DICT:
                if (!slot.isPresent()) {
                    violations.add(prefix + role + " ore-dict entry " + slot.label() + " has no items");
                }
                break;
            case FLUID:
                if (!slot.isPresent()) {
                    violations.add(prefix + role + " fluid " + slot.label() + " is null or unregistered");
                }
                break;
            case ITEM:
            default:
                if (!slot.isPresent()) {
                    violations.add(prefix + role + " item " + slot.label() + " is null or empty");
                }
                break;
        }
    }
}
