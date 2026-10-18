package com.gregguns.network.validation;

/**
 * One declarative validation check. Rules are evaluated in declaration order and the first violation wins, so cheap
 * checks should be declared before checks that look up targets.
 *
 * Side: both.
 */
@FunctionalInterface
public interface Rule {

    /** Returns {@code null} when the rule passes, or the violation when it fails. */
    Violation check();
}
