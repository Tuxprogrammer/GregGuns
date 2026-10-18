package com.gregguns.core.selfcheck;

import java.util.ArrayList;
import java.util.List;

import com.gregguns.api.Tier;

/**
 * The D1 tier-map rule for one recipe output, as pure logic: a recipe may not need a machine voltage above its
 * output's tier, and may not consume a GregGuns input of a higher tier than its output.
 *
 * <p>
 * Spec: docs/design/0001-tier-map.md ("The dev self-check enforces this table"), CONTRIBUTING.md (Tier gating). Both
 * sides, no Minecraft types.
 */
public final class RecipeTierCheck {

    /** The kind of tier-map breach. */
    public enum Kind {
        /** The recipe needs a machine of a higher tier than the output's tier. */
        MACHINE_ABOVE_OUTPUT,
        /** A GregGuns input has a higher tier than the output. */
        INPUT_ABOVE_OUTPUT
    }

    /** One breach of the tier map. */
    public static final class Violation {

        private final Kind kind;
        private final String message;

        Violation(Kind kind, String message) {
            this.kind = kind;
            this.message = message;
        }

        /**
         * @return what kind of breach this is
         */
        public Kind kind() {
            return kind;
        }

        /**
         * @return a human-readable explanation
         */
        public String message() {
            return message;
        }
    }

    private RecipeTierCheck() {}

    /**
     * Checks one recipe output against the tier map.
     *
     * @param outputTier the output item's tier
     * @param eut        the recipe's EU/t; zero or negative for a crafting-table recipe, which needs no machine
     * @param inputTiers tiers of the inputs; a {@code null} entry is an input whose tier is unknown (a GT part, say)
     *                   and
     *                   is skipped
     * @return every breach found; empty when the recipe fits the tier map
     */
    public static List<Violation> check(Tier outputTier, long eut, List<Tier> inputTiers) {
        List<Violation> violations = new ArrayList<>();
        if (eut > 0) {
            Tier machine = Tier.lowestForEut(eut);
            // A recipe above ZPM voltage has no tier, so it is above any output.
            if (machine == null || machine.compareTo(outputTier) > 0) {
                String needed = machine == null ? "a machine above " + Tier.ZPM.gtName() : machine.gtName();
                violations.add(
                    new Violation(
                        Kind.MACHINE_ABOVE_OUTPUT,
                        eut + " EU/t needs "
                            + needed
                            + " but the output is "
                            + outputTier.gtName()
                            + " (limit "
                            + outputTier.voltage()
                            + " EU/t)"));
            }
        }
        for (Tier input : inputTiers) {
            if (input != null && input.compareTo(outputTier) > 0) {
                violations.add(
                    new Violation(
                        Kind.INPUT_ABOVE_OUTPUT,
                        "a " + input.gtName() + " input goes into a " + outputTier.gtName() + " output"));
            }
        }
        return violations;
    }
}
