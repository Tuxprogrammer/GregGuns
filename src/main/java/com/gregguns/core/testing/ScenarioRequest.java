package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A parsed {@code /gregguns test ...} argument list. Plain Java.
 */
public final class ScenarioRequest {

    /** The keyword that runs every scenario. */
    public static final String ALL = "all";
    /** The keyword that lists scenarios. */
    public static final String LIST = "list";

    /** What the arguments ask for. */
    public enum Kind {
        /** No arguments, or too many. */
        USAGE,
        /** List the registered scenarios. */
        LIST,
        /** Run every scenario. */
        ALL,
        /** Run the named scenario. */
        NAMED,
        /** The named scenario does not exist. */
        UNKNOWN
    }

    private final Kind kind;
    private final String argument;
    private final Scenario scenario;

    private ScenarioRequest(Kind kind, String argument, Scenario scenario) {
        this.kind = kind;
        this.argument = argument;
        this.scenario = scenario;
    }

    /**
     * Parses the arguments that follow {@code test}.
     *
     * @param args     the arguments
     * @param registry where to look names up
     * @return the request
     */
    public static ScenarioRequest parse(String[] args, ScenarioRegistry registry) {
        if (args.length != 1) {
            return new ScenarioRequest(Kind.USAGE, null, null);
        }
        String arg = args[0].toLowerCase(Locale.ROOT);
        if (LIST.equals(arg)) {
            return new ScenarioRequest(Kind.LIST, arg, null);
        }
        if (ALL.equals(arg)) {
            return new ScenarioRequest(Kind.ALL, arg, null);
        }
        Scenario found = registry.get(arg);
        return found == null ? new ScenarioRequest(Kind.UNKNOWN, args[0], null)
            : new ScenarioRequest(Kind.NAMED, arg, found);
    }

    /**
     * Completes the first argument.
     *
     * @param args     the arguments typed so far (the last one is being completed)
     * @param registry the scenarios
     * @return keywords and scenario names starting with the typed prefix, sorted; empty past the first argument
     */
    public static List<String> complete(String[] args, ScenarioRegistry registry) {
        List<String> out = new ArrayList<>();
        if (args.length != 1) {
            return out;
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> candidates = new ArrayList<>();
        candidates.add(ALL);
        candidates.add(LIST);
        for (Scenario s : registry.all()) {
            candidates.add(s.name());
        }
        for (String c : candidates) {
            if (c.startsWith(prefix)) {
                out.add(c);
            }
        }
        return out;
    }

    /** @return what was asked for */
    public Kind kind() {
        return kind;
    }

    /** @return the argument as typed ({@code null} for usage) */
    public String argument() {
        return argument;
    }

    /** @return the scenario for {@link Kind#NAMED}, otherwise {@code null} */
    public Scenario scenario() {
        return scenario;
    }
}
