package com.gregguns.icbm.control;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The Launch Control's silo reach (D70). */
class SiloSearchTest {

    @Test
    void findsTheNearestSiloInReach() {
        assertArrayEquals(new int[] { 2, 0, 3 }, SiloSearch.nearest((dx, dy, dz) -> dx == 2 && dy == 0 && dz == 3));
        assertArrayEquals(
            new int[] { 1, 0, 0 },
            SiloSearch.nearest((dx, dy, dz) -> dy == 0 && (dx == 1 && dz == 0 || dx == 5 && dz == 5)));
    }

    @Test
    void ignoresASiloBeyondTheReachOrTheBand() {
        assertNull(SiloSearch.nearest((dx, dy, dz) -> dx == SiloSearch.REACH + 1));
        assertNull(SiloSearch.nearest((dx, dy, dz) -> dy == SiloSearch.BAND + 1));
    }
}
