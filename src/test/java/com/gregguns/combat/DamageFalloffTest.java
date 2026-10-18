package com.gregguns.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DamageFalloffTest {

    private static final double EPS = 1e-9;

    @Test
    void fullDamageUpToAndIncludingStart() {
        assertEquals(10.0, DamageFalloff.damageAt(10, 4, 10, 30, 0), EPS);
        assertEquals(10.0, DamageFalloff.damageAt(10, 4, 10, 30, 10), EPS);
        assertEquals(10.0, DamageFalloff.damageAt(10, 4, 10, 30, -5), EPS);
    }

    @Test
    void interpolatesLinearlyBetweenStartAndEnd() {
        assertEquals(7.0, DamageFalloff.damageAt(10, 4, 10, 30, 20), EPS);
        assertEquals(4.0, DamageFalloff.damageAt(10, 4, 10, 30, 30), EPS);
        assertEquals(8.5, DamageFalloff.damageAt(10, 4, 10, 30, 15), EPS);
    }

    @Test
    void minimumDamageBeyondEnd() {
        assertEquals(4.0, DamageFalloff.damageAt(10, 4, 10, 30, 30.001), EPS);
        assertEquals(4.0, DamageFalloff.damageAt(10, 4, 10, 30, 500), EPS);
    }

    @Test
    void emptyWindowIsAHardStep() {
        assertEquals(10.0, DamageFalloff.damageAt(10, 4, 20, 20, 20), EPS);
        assertEquals(4.0, DamageFalloff.damageAt(10, 4, 20, 20, 20.5), EPS);
    }

    @Test
    void noDropWhenMinimumEqualsBase() {
        assertEquals(10.0, DamageFalloff.damageAt(10, 10, 5, 50, 200), EPS);
    }

    @Test
    void minimumAboveBaseCountsAsBase() {
        assertEquals(10.0, DamageFalloff.damageAt(10, 20, 5, 50, 200), EPS);
        assertEquals(10.0, DamageFalloff.damageAt(10, 20, 5, 50, 20), EPS);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> DamageFalloff.damageAt(10, 4, 30, 10, 5));
        assertThrows(IllegalArgumentException.class, () -> DamageFalloff.damageAt(-1, 4, 10, 30, 5));
        assertThrows(IllegalArgumentException.class, () -> DamageFalloff.damageAt(10, -1, 10, 30, 5));
        assertThrows(IllegalArgumentException.class, () -> DamageFalloff.damageAt(10, 4, -1, 30, 5));
        assertThrows(IllegalArgumentException.class, () -> DamageFalloff.damageAt(Double.NaN, 4, 10, 30, 5));
        assertThrows(IllegalArgumentException.class, () -> DamageFalloff.damageAt(10, 4, 10, 30, Double.NaN));
    }
}
