package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.gregguns.GregGuns;

/**
 * Runs scenarios one at a time, each with a fresh context that is always cleaned up. A scenario that throws, or whose
 * cleanup throws, fails instead of aborting the run. Plain Java.
 */
public final class ScenarioRunner {

    private final Function<Scenario, ScenarioContext> contexts;

    /**
     * @param contexts makes the context for a scenario; the runner cleans it up after the run
     */
    public ScenarioRunner(Function<Scenario, ScenarioContext> contexts) {
        this.contexts = contexts;
    }

    /**
     * @param scenarios what to run, in order
     * @return the aggregated results
     */
    public ScenarioSummary run(List<Scenario> scenarios) {
        List<ScenarioResult> results = new ArrayList<>();
        for (Scenario s : scenarios) {
            results.add(runOne(s));
        }
        return new ScenarioSummary(results);
    }

    private ScenarioResult runOne(Scenario scenario) {
        ScenarioContext context = null;
        ScenarioResult result;
        try {
            context = contexts.apply(scenario);
            result = scenario.run(context);
            if (result == null) {
                result = ScenarioResult.fail(scenario.name(), "scenario returned no result");
            }
        } catch (RuntimeException | AssertionError e) {
            GregGuns.LOG.error("Scenario {} threw", scenario.name(), e);
            result = ScenarioResult.fail(scenario.name(), "threw " + e);
        }
        if (context != null) {
            try {
                context.cleanup();
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Cleanup after scenario {} threw", scenario.name(), e);
                result = ScenarioResult.fail(scenario.name(), "cleanup threw " + e);
            }
        }
        return result;
    }
}
