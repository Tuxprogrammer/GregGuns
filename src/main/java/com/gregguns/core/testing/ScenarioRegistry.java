package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * The set of known scenarios, sorted by name. Features register theirs on {@link #SCENARIOS}. Plain Java.
 */
public final class ScenarioRegistry {

    /** The shared registry the {@code /gregguns test} command reads. */
    public static final ScenarioRegistry SCENARIOS = new ScenarioRegistry();

    private static final Pattern NAME = Pattern.compile("[a-z0-9_.-]+");

    private final Map<String, Scenario> byName = new TreeMap<>();

    /**
     * Adds a scenario.
     *
     * @param scenario the scenario
     * @throws IllegalArgumentException if the name is invalid, reserved ({@code all}, {@code list}) or taken
     */
    public synchronized void register(Scenario scenario) {
        String name = scenario.name();
        if (name == null || !NAME.matcher(name)
            .matches()) {
            throw new IllegalArgumentException("Invalid scenario name: " + name);
        }
        if (ScenarioRequest.ALL.equals(name) || ScenarioRequest.LIST.equals(name)) {
            throw new IllegalArgumentException("Reserved scenario name: " + name);
        }
        if (byName.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate scenario name: " + name);
        }
        byName.put(name, scenario);
    }

    /**
     * @param name a scenario name
     * @return the scenario, or {@code null} when unknown
     */
    public synchronized Scenario get(String name) {
        return byName.get(name);
    }

    /** @return all scenarios in name order (a snapshot) */
    public synchronized List<Scenario> all() {
        return Collections.unmodifiableList(new ArrayList<>(byName.values()));
    }

    /** @return the number of registered scenarios */
    public synchronized int size() {
        return byName.size();
    }
}
