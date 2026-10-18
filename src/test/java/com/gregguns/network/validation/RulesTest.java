package com.gregguns.network.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class RulesTest {

    enum Phase {
        A,
        B,
        C
    }

    private static ClientRules rules() {
        return new ClientRules();
    }

    @Test
    void acceptsIndexInsideBounds() {
        assertNull(
            rules().indexInRange("slot", 0, 9)
                .indexInRange("slot", 8, 9)
                .firstViolation());
    }

    @Test
    void rejectsIndexAtOrAboveSizeAndNegative() {
        assertEquals(
            Rejection.OUT_OF_RANGE,
            rules().indexInRange("slot", 9, 9)
                .firstViolation()
                .rejection());
        assertEquals(
            Rejection.OUT_OF_RANGE,
            rules().indexInRange("slot", -1, 9)
                .firstViolation()
                .rejection());
        assertEquals(
            Rejection.OUT_OF_RANGE,
            rules().indexInRange("slot", Integer.MIN_VALUE, 9)
                .firstViolation()
                .rejection());
    }

    @Test
    void rejectsAnyIndexForEmptyRange() {
        assertEquals(
            Rejection.OUT_OF_RANGE,
            rules().indexInRange("slot", 0, 0)
                .firstViolation()
                .rejection());
    }

    @Test
    void ordinalMustBeAConstantOfTheEnum() {
        assertNull(
            rules().ordinalInRange("phase", 2, Phase.class)
                .firstViolation());
        assertEquals(
            Rejection.OUT_OF_RANGE,
            rules().ordinalInRange("phase", 3, Phase.class)
                .firstViolation()
                .rejection());
        assertEquals(
            Rejection.OUT_OF_RANGE,
            rules().ordinalInRange("phase", -1, Phase.class)
                .firstViolation()
                .rejection());
    }

    @Test
    void finiteRejectsNaNAndInfinity() {
        assertNull(
            rules().finite("x", 1.5)
                .firstViolation());
        assertEquals(
            Rejection.OUT_OF_RANGE,
            rules().finite("x", Double.NaN)
                .firstViolation()
                .rejection());
        assertEquals(
            Rejection.OUT_OF_RANGE,
            rules().finite("x", Double.POSITIVE_INFINITY)
                .firstViolation()
                .rejection());
    }

    @Test
    void missingTargetIsRejected() {
        assertEquals(
            Rejection.TARGET_MISSING,
            rules().targetOfType("tile", () -> null, String.class)
                .firstViolation()
                .rejection());
        assertEquals(
            Rejection.TARGET_MISSING,
            rules().targetExists("tile", () -> null)
                .firstViolation()
                .rejection());
    }

    @Test
    void wrongTypeTargetIsRejected() {
        assertEquals(
            Rejection.WRONG_TARGET_TYPE,
            rules().targetOfType("entity", () -> Integer.valueOf(3), String.class)
                .firstViolation()
                .rejection());
    }

    @Test
    void expectedTypeTargetIsAccepted() {
        assertNull(
            rules().targetOfType("entity", () -> "ok", CharSequence.class)
                .firstViolation());
    }

    @Test
    void firstViolationWinsAndLaterLookupsAreSkipped() {
        AtomicBoolean lookedUp = new AtomicBoolean();
        Violation v = rules().indexInRange("slot", 99, 9)
            .targetExists("tile", () -> {
                lookedUp.set(true);
                return "x";
            })
            .firstViolation();
        assertEquals(Rejection.OUT_OF_RANGE, v.rejection());
        assertFalse(lookedUp.get(), "target lookups must not run once a cheaper rule has failed");
    }
}
