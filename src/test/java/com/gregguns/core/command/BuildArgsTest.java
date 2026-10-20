package com.gregguns.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BuildArgsTest {

    @Test
    void parsesPositionWithDefaultCount() {
        BuildArgs a = BuildArgs.parse(new String[] { "10", "-4", "300" });
        assertEquals(10, a.x);
        assertEquals(-4, a.y);
        assertEquals(300, a.z);
        assertEquals(0, a.count);
    }

    @Test
    void parsesCount() {
        assertEquals(6, BuildArgs.parse(new String[] { "0", "64", "0", "6" }).count);
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> BuildArgs.parse(new String[] {}));
        assertThrows(IllegalArgumentException.class, () -> BuildArgs.parse(new String[] { "1", "2" }));
        assertThrows(IllegalArgumentException.class, () -> BuildArgs.parse(new String[] { "1", "2", "x" }));
        assertThrows(IllegalArgumentException.class, () -> BuildArgs.parse(new String[] { "1", "2", "3", "0" }));
        assertThrows(IllegalArgumentException.class, () -> BuildArgs.parse(new String[] { "1", "2", "3", "65" }));
        assertThrows(IllegalArgumentException.class, () -> BuildArgs.parse(new String[] { "1", "2", "3", "4", "5" }));
    }
}
