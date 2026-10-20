package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The results of one run, aggregated, with the machine-greppable report. Plain Java.
 */
public final class ScenarioSummary {

    private final List<ScenarioResult> results;

    /**
     * @param results the per-scenario results, in run order
     */
    public ScenarioSummary(List<ScenarioResult> results) {
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
    }

    /** @return the results in run order */
    public List<ScenarioResult> results() {
        return results;
    }

    /** @return how many scenarios passed */
    public int passed() {
        int n = 0;
        for (ScenarioResult r : results) {
            if (r.passed()) {
                n++;
            }
        }
        return n;
    }

    /** @return how many scenarios were skipped (they did not apply here; neither passed nor failed) */
    public int skipped() {
        int n = 0;
        for (ScenarioResult r : results) {
            if (r.skipped()) {
                n++;
            }
        }
        return n;
    }

    /** @return how many scenarios failed */
    public int failed() {
        return results.size() - passed() - skipped();
    }

    /** @return {@code true} when nothing failed (an empty run counts as success) */
    public boolean allPassed() {
        return failed() == 0;
    }

    /**
     * @return the final line, for example {@code [GregGuns] Scenarios: 2 passed, 0 failed}; {@code , N skipped} is
     *         appended only when something was skipped, so the shape scripts grep for does not change
     */
    public String summaryLine() {
        return "[GregGuns] Scenarios: " + passed() + " passed, " + failed() + " failed" + skippedSuffix();
    }

    /** @return {@code ", N skipped"}, or the empty string when nothing was skipped */
    public String skippedSuffix() {
        int n = skipped();
        return n == 0 ? "" : ", " + n + " skipped";
    }

    /**
     * Formats the whole report: one {@code PASS name} / {@code FAIL name} / {@code SKIP name} line per scenario, each
     * message indented
     * under it, and {@link #summaryLine()} last.
     *
     * @return the lines
     */
    public List<String> reportLines() {
        List<String> lines = new ArrayList<>();
        for (ScenarioResult r : results) {
            lines.add("[GregGuns] " + (r.passed() ? "PASS " : r.skipped() ? "SKIP " : "FAIL ") + r.scenario());
            for (String m : r.messages()) {
                lines.add("[GregGuns]   " + m);
            }
        }
        lines.add(summaryLine());
        return lines;
    }
}
