package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The outcome of one scenario: pass or fail, plus the messages the checks produced. Immutable. Plain Java.
 */
public final class ScenarioResult {

    private final String scenario;
    private final boolean passed;
    private final List<String> messages;

    private ScenarioResult(String scenario, boolean passed, List<String> messages) {
        this.scenario = scenario;
        this.passed = passed;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
    }

    /**
     * @param scenario the scenario name
     * @return a builder that records checks for that scenario
     */
    public static Builder builder(String scenario) {
        return new Builder(scenario);
    }

    /**
     * @param scenario the scenario name
     * @param message  why it failed
     * @return a failed result with one message
     */
    public static ScenarioResult fail(String scenario, String message) {
        return new ScenarioResult(scenario, false, Collections.singletonList(message));
    }

    /**
     * @param scenario the scenario name
     * @return a passed result with no messages
     */
    public static ScenarioResult pass(String scenario) {
        return new ScenarioResult(scenario, true, Collections.<String>emptyList());
    }

    /** @return the scenario name */
    public String scenario() {
        return scenario;
    }

    /** @return {@code true} when every check passed */
    public boolean passed() {
        return passed;
    }

    /** @return the failure messages (or notes for a pass); never {@code null} */
    public List<String> messages() {
        return messages;
    }

    /**
     * Collects checks. A failed check records its message and fails the result; the scenario keeps running so one run
     * reports every problem. Not thread-safe.
     */
    public static final class Builder {

        private final String scenario;
        private final List<String> messages = new ArrayList<>();
        private boolean passed = true;

        private Builder(String scenario) {
            this.scenario = Objects.requireNonNull(scenario, "scenario");
        }

        /**
         * @param condition what must hold
         * @param message   reported when it does not
         * @return this builder
         */
        public Builder check(boolean condition, String message) {
            if (!condition) {
                passed = false;
                messages.add(message);
            }
            return this;
        }

        /**
         * Checks two values are equal, reporting both when they are not.
         *
         * @param what     describes the value, for example {@code "pig health"}
         * @param expected the expected value
         * @param actual   the observed value
         * @return this builder
         */
        public Builder checkEquals(String what, Object expected, Object actual) {
            return check(
                Objects.equals(expected, actual),
                what + ": expected <" + expected + "> but was <" + actual + ">");
        }

        /**
         * Records an informational note that does not affect pass or fail.
         *
         * @param message the note
         * @return this builder
         */
        public Builder note(String message) {
            messages.add(message);
            return this;
        }

        /** @return the finished result */
        public ScenarioResult build() {
            return new ScenarioResult(scenario, passed, messages);
        }
    }
}
