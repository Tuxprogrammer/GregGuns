package com.gregguns.gun.logic;

/**
 * How a held charge turns into rounds spent. Pure.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (Bio Gun, Power Hammer).
 */
public enum ChargeMode {

    /** Three discrete levels (Bio Gun): the level is the rounds spent. */
    LEVELS,
    /**
     * A scaled amount, {@code floor(max * charge)} clamped to 1..max (Power Hammer): the amount is the rounds spent.
     */
    SCALED
}
