package com.gregguns.network.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * An ordered set of validation rules a packet declares for itself. These rules need no sender, so both server-bound
 * and client-bound packets use them. {@link SenderRules} and {@link ClientRules} are the concrete sets;
 * {@link SenderRules} adds the sender-dependent rules.
 *
 * Rules are evaluated lazily, in declaration order, by {@link #firstViolation()}. Target lookups are passed as
 * suppliers so they only run once the cheaper range checks have passed.
 *
 * Side: both.
 */
public abstract class Rules<T extends Rules<T>> {

    private final List<Rule> rules = new ArrayList<>();

    /** Returns this object as its concrete type, so declarations chain. */
    protected abstract T self();

    /** Adds a custom rule and returns this set. */
    public T custom(Rule rule) {
        rules.add(rule);
        return self();
    }

    /**
     * Requires {@code 0 <= value < size}, for slot, list and array indices.
     *
     * @param name  the field name, for the log
     * @param value the untrusted value from the packet
     * @param size  the exclusive upper bound
     */
    public T indexInRange(String name, int value, int size) {
        return inRange(name, value, 0, (long) size - 1);
    }

    /**
     * Requires {@code min <= value <= max}.
     *
     * @param name  the field name, for the log
     * @param value the untrusted value from the packet
     * @param min   inclusive lower bound
     * @param max   inclusive upper bound
     */
    public T inRange(String name, long value, long min, long max) {
        return custom(
            () -> value < min || value > max
                ? new Violation(Rejection.OUT_OF_RANGE, name + "=" + value + " not in [" + min + ", " + max + "]")
                : null);
    }

    /**
     * Requires the ordinal to be a valid constant of the enum, so it can safely index {@code values()}.
     *
     * @param name    the field name, for the log
     * @param ordinal the untrusted value from the packet
     * @param type    the enum class
     */
    public T ordinalInRange(String name, int ordinal, Class<? extends Enum<?>> type) {
        return indexInRange(name, ordinal, type.getEnumConstants().length);
    }

    /**
     * Requires a finite number, so NaN and infinity from a hostile client never reach distance maths.
     *
     * @param name  the field name, for the log
     * @param value the untrusted value from the packet
     */
    public T finite(String name, double value) {
        return custom(
            () -> Double.isNaN(value) || Double.isInfinite(value)
                ? new Violation(Rejection.OUT_OF_RANGE, name + "=" + value + " is not finite")
                : null);
    }

    /**
     * Requires the lookup to find something.
     *
     * @param name   what is being looked up, for the log
     * @param lookup resolves the target, returning {@code null} when it is missing or unloaded. Must not load chunks.
     */
    public T targetExists(String name, Supplier<?> lookup) {
        return targetOfType(name, lookup, Object.class);
    }

    /**
     * Requires the lookup to find something of the expected type, so no unchecked cast follows (CORE-40).
     *
     * @param name     what is being looked up, for the log
     * @param lookup   resolves the target, returning {@code null} when it is missing or unloaded. Must not load
     *                 chunks.
     * @param expected the type the handler will cast to
     */
    public T targetOfType(String name, Supplier<?> lookup, Class<?> expected) {
        return custom(() -> {
            Object target = lookup.get();
            if (target == null) {
                return new Violation(Rejection.TARGET_MISSING, name + " not found");
            }
            if (!expected.isInstance(target)) {
                return new Violation(
                    Rejection.WRONG_TARGET_TYPE,
                    name + " is "
                        + target.getClass()
                            .getSimpleName()
                        + ", expected "
                        + expected.getSimpleName());
            }
            return null;
        });
    }

    /** Returns the first violation, evaluating rules in declaration order, or {@code null} when all pass. */
    public Violation firstViolation() {
        for (Rule rule : rules) {
            Violation violation = rule.check();
            if (violation != null) {
                return violation;
            }
        }
        return null;
    }

    /** Returns the number of declared rules. */
    public int size() {
        return rules.size();
    }
}
