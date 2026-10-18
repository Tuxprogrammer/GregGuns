package com.gregguns.core.testing;

import java.util.List;
import java.util.function.Function;

/**
 * Runs scenarios one at a time, each with a fresh context that is always cleaned up. A scenario that throws, or whose
 * cleanup throws, fails instead of aborting the run. Plain Java.
 *
 * <p>
 * {@link #run} is the synchronous form: every scenario must finish inside its {@code run} call. {@link #begin} is the
 * form for scenarios that need server ticks to pass (see {@link ScenarioResult#pending}); the caller ticks the returned
 * {@link ScenarioRun}.
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
     * Runs everything now. A scenario that asks to wait fails, because nothing will tick it.
     *
     * @param scenarios what to run, in order
     * @return the aggregated results
     */
    public ScenarioSummary run(List<Scenario> scenarios) {
        ScenarioRun run = begin(scenarios);
        while (!run.isDone()) {
            run.abandonPending("needs server ticks; start it with ScenarioRunner.begin and tick the run");
        }
        return run.summary();
    }

    /**
     * Starts the scenarios in order, running synchronous ones straight away and stopping at the first that waits.
     *
     * @param scenarios what to run, in order
     * @return the run; call {@link ScenarioRun#tick()} once per server tick until {@link ScenarioRun#isDone()}
     */
    public ScenarioRun begin(List<Scenario> scenarios) {
        return new ScenarioRun(contexts, scenarios);
    }
}
