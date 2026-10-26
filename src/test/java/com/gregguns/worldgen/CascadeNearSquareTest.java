package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * {@link CascadeScenarios#nearSquare}: the no-cascade scenario compares only the chunks in or near its square, because
 * in a real pack other mods load unrelated chunks far away while it runs.
 */
class CascadeNearSquareTest {

    private static Set<String> keys(String... k) {
        return new HashSet<>(Arrays.asList(k));
    }

    @Test
    void keepsTheSquareAndItsMargin() {
        Set<String> in = keys("0,0", "39,39", "-3,5", "42,0", "10,-3", "10,42");
        assertEquals(in, CascadeScenarios.nearSquare(in, 40, 3));
    }

    @Test
    void dropsChunksFartherAwayThanTheMargin() {
        Set<String> kept = CascadeScenarios
            .nearSquare(keys("0,0", "-4,0", "43,0", "0,-4", "0,43", "-9514,-6660"), 40, 3);
        assertEquals(keys("0,0"), kept);
    }

    @Test
    void emptyStaysEmpty() {
        assertTrue(
            CascadeScenarios.nearSquare(keys(), 40, 3)
                .isEmpty());
    }
}
