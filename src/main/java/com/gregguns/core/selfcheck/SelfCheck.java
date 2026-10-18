package com.gregguns.core.selfcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs every dev self-check over plain descriptors and collects all violations, not just the first.
 *
 * <p>
 * Spec: CONTRIBUTING.md (Testing, dev self-check). Both sides, no Minecraft types; {@link SelfCheckRunner} supplies the
 * descriptors from the live game.
 */
public final class SelfCheck {

    private SelfCheck() {}

    /** The outcome of one self-check run. */
    public static final class Report {

        private final List<String> violations;
        private final int contentCount;
        private final int recipeCount;

        Report(List<String> violations, int contentCount, int recipeCount) {
            this.violations = Collections.unmodifiableList(violations);
            this.contentCount = contentCount;
            this.recipeCount = recipeCount;
        }

        /**
         * @return every violation found, in check order; empty when the run was clean
         */
        public List<String> violations() {
            return violations;
        }

        /**
         * @return the one-line INFO summary for a clean run
         */
        public String summary() {
            return "Self-check passed: " + contentCount
                + " items/blocks, "
                + recipeCount
                + " recipes, sounds.json, lang keys and textures all resolve";
        }

        /**
         * @return the failure message listing every violation
         */
        public String failureMessage() {
            StringBuilder message = new StringBuilder(
                "GregGuns self-check failed with " + violations.size() + " violation(s):");
            for (String violation : violations) {
                message.append("\n  - ")
                    .append(violation);
            }
            return message.toString();
        }

        /**
         * Stops the dev server if the run found anything.
         *
         * @throws SelfCheckFailedException listing every violation, when there is at least one
         */
        public void throwIfFailed() {
            if (!violations.isEmpty()) {
                throw new SelfCheckFailedException(failureMessage());
            }
        }
    }

    /**
     * Runs all checks.
     *
     * @param contents     every registered GregGuns item and block
     * @param recipes      every recipe that has a GregGuns input or output
     * @param resources    the mod's classpath resources
     * @param glueProblems problems the glue code met while reading the game (for example an unreadable field), reported
     *                     as violations
     * @return the report
     */
    public static Report run(List<ContentDescriptor> contents, List<RecipeDescriptor> recipes, ResourceSource resources,
        List<String> glueProblems) {
        List<String> violations = new ArrayList<>(glueProblems);
        violations.addAll(ContentChecks.check(contents, resources));
        for (RecipeDescriptor recipe : recipes) {
            violations.addAll(RecipeChecks.check(recipe));
        }
        violations.addAll(SoundChecks.check(resources));
        return new Report(violations, contents.size(), recipes.size());
    }
}
