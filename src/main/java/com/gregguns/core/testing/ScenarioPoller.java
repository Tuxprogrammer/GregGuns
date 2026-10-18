package com.gregguns.core.testing;

/**
 * The second half of a multi-tick scenario: asked once per server tick until it has an answer. Plain Java.
 */
@FunctionalInterface
public interface ScenarioPoller {

    /**
     * Checks whether the scenario can finish.
     *
     * @param ticksElapsed server ticks since the scenario started: 1 on the first poll, then 2, 3 and so on
     * @return the final result, or {@code null} to be asked again next tick
     */
    ScenarioResult poll(int ticksElapsed);
}
