package com.gregguns.core.testing;

/**
 * One reproducible in-game situation with a checked outcome. Implementations set the situation up through the
 * {@link ScenarioContext}, check the result and return it. Anything spawned or changed through the context is cleaned
 * up by the
 * runner afterwards, even when {@link #run} throws.
 *
 * <p>
 * Names are lower case, dot-separated by feature, for example {@code gun.pistol.damage}.
 */
public interface Scenario {

    /** @return the unique scenario name, matching {@code [a-z0-9_.-]+} */
    String name();

    /** @return a one-line English description for {@code test list} */
    String description();

    /**
     * Runs the scenario on the server thread.
     *
     * @param context the world, origin and helpers
     * @return the structured result
     */
    ScenarioResult run(ScenarioContext context);
}
