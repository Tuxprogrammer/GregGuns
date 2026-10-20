package com.gregguns.core.testing;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Wraps a scenario that needs content only the development workspace registers (the probe tool gun, the test NPC). When
 * the content is missing, as in the real pack, the scenario reports {@link ScenarioResult#skip} with the reason instead
 * of failing. Plain Java.
 */
public final class SkipUnless implements Scenario {

    private final Scenario inner;
    private final Predicate<ScenarioContext> available;
    private final String reason;

    private SkipUnless(Scenario inner, Predicate<ScenarioContext> available, String reason) {
        this.inner = inner;
        this.available = available;
        this.reason = reason;
    }

    /**
     * @param inner     the scenario to guard
     * @param available evaluated when the scenario runs; false skips it
     * @param reason    why it is skipped, shown in the report
     * @return the guarded scenario, with the same name, description and area
     */
    public static Scenario of(Scenario inner, BooleanSupplier available, String reason) {
        return new SkipUnless(inner, ctx -> available.getAsBoolean(), reason);
    }

    /**
     * As {@link #of(Scenario, BooleanSupplier, String)}, for a condition that needs the scenario's world.
     *
     * @param inner     the scenario to guard
     * @param available evaluated with the scenario's context when it runs; false skips it
     * @param reason    why it is skipped, shown in the report
     * @return the guarded scenario
     */
    public static Scenario inContext(Scenario inner, Predicate<ScenarioContext> available, String reason) {
        return new SkipUnless(inner, available, reason);
    }

    @Override
    public String name() {
        return inner.name();
    }

    @Override
    public String description() {
        return inner.description();
    }

    @Override
    public boolean includedInAll() {
        return inner.includedInAll();
    }

    @Override
    public int areaRadius() {
        return inner.areaRadius();
    }

    @Override
    public ScenarioResult run(ScenarioContext context) {
        if (!available.test(context)) {
            return ScenarioResult.skip(name(), reason);
        }
        return inner.run(context);
    }
}
