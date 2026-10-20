package com.gregguns.core.testing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AutotestArgsTest {

    @Test
    void parsesYawAndPitch() {
        assertArrayEquals(new float[] { -90f, 12.5f }, AutotestArgs.parseLook(new String[] { "-90", "12.5" }));
    }

    @Test
    void acceptsYawBeyondOneTurnAndPitchLimits() {
        assertArrayEquals(new float[] { 450f, 90f }, AutotestArgs.parseLook(new String[] { "450", "90" }));
        assertArrayEquals(new float[] { 0f, -90f }, AutotestArgs.parseLook(new String[] { "0", "-90" }));
    }

    @Test
    void rejectsPitchOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.parseLook(new String[] { "0", "90.5" }));
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.parseLook(new String[] { "0", "-91" }));
    }

    @Test
    void rejectsWrongCountNonNumbersAndNonFinite() {
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.parseLook(new String[] {}));
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.parseLook(new String[] { "1" }));
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.parseLook(new String[] { "1", "2", "3" }));
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.parseLook(new String[] { "east", "0" }));
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.parseLook(new String[] { "NaN", "0" }));
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.parseLook(new String[] { "0", "Infinity" }));
    }

    @Test
    void useTakesNoArguments() {
        AutotestArgs.requireNone(new String[] {});
        assertThrows(IllegalArgumentException.class, () -> AutotestArgs.requireNone(new String[] { "x" }));
    }

    @Test
    void lookSurvivesTheSignalRoundTrip() {
        String[] parts = AutotestSignal.parse(AutotestSignal.format("look", "-90", "0"));
        assertArrayEquals(new float[] { -90f, 0f }, AutotestArgs.parseLook(AutotestSignal.args(parts)));
    }
}
