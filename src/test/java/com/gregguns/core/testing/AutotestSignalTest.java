package com.gregguns.core.testing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AutotestSignalTest {

    @Test
    void roundTripsCommandAndArguments() {
        String line = AutotestSignal.format("shot", "b-third");
        assertArrayEquals(new String[] { "shot", "b-third" }, AutotestSignal.parse(line));
    }

    @Test
    void ignoresFormattingCodesInFront() {
        assertArrayEquals(
            new String[] { "view", "2" },
            AutotestSignal.parse("\u00a7f" + AutotestSignal.format("view", "2")));
    }

    @Test
    void ordinaryChatIsNotASignal() {
        assertNull(AutotestSignal.parse("<Alice> hello"));
        assertNull(AutotestSignal.parse(null));
        assertNull(AutotestSignal.parse(AutotestSignal.PREFIX));
    }

    @Test
    void argsDropsTheCommandWord() {
        assertArrayEquals(new String[] { "1", "2" }, AutotestSignal.args(new String[] { "gui", "1", "2" }));
    }
}
