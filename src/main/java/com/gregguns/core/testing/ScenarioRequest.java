package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.Collections;
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
        /** Run every scenario whose name starts with the prefix ({@code icbm.} or {@code icbm.blast*}). */
        PREFIX,
        /** The named scenario does not exist, or the prefix matches nothing. */
        UNKNOWN
    }

    private final Kind kind;
    private final String argument;
    private final Scenario scenario;
    private final List<Scenario> matches;

    private ScenarioRequest(Kind kind, String argument, Scenario scenario) {
        this(kind, argument, scenario, Collections.<Scenario>emptyList());
    }

    private ScenarioRequest(Kind kind, String argument, Scenario scenario, List<Scenario> matches) {
        this.kind = kind;
        this.argument = argument;
        this.scenario = scenario;
        this.matches = matches;
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
        if (found != null) {
            return new ScenarioRequest(Kind.NAMED, arg, found);
        }
        if (arg.endsWith(".") || arg.endsWith("*")) {
            String prefix = arg.endsWith("*") ? arg.substring(0, arg.length() - 1) : arg;
            // Like "all", a prefix skips scenarios that are not includedInAll (they run by exact name only).
            List<Scenario> matched = new ArrayList<>();
            for (Scenario s : registry.forAll()) {
                if (s.name()
                    .startsWith(prefix)) {
                    matched.add(s);
                }
            }
            if (!matched.isEmpty()) {
                return new ScenarioRequest(Kind.PREFIX, arg, null, matched);
            }
        }
        return new ScenarioRequest(Kind.UNKNOWN, args[0], null);
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

    /** @return the scenarios a {@link Kind#PREFIX} request runs, in name order; empty for other kinds */
    public List<Scenario> matches() {
        return matches;
    }
}
