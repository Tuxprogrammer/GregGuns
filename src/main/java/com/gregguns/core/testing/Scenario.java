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

    /**
     * Whether {@code /gregguns test all} runs this scenario. Scenarios that need a setup the console run can't give,
     * such as connected client players, return false and are run by name from their own harness.
     *
     * @return true unless the scenario needs an external setup
     */
    default boolean includedInAll() {
        return true;
    }

    /**
     * The half-size in blocks of the area around this scenario's origin that is kept loaded (Forge chunk tickets) and
     * cleared to air over a stone floor before {@link #run}. Clamped to {@link ScenarioArea#MAX_RADIUS}.
     *
     * @return the radius; the default covers origin +- {@value ScenarioArea#RADIUS}
     */
    default int areaRadius() {
        return ScenarioArea.RADIUS;
    }
}
