package com.gregguns.icbm.soak;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoakCurveTest {

    private static SoakCurve of(double... v) {
        SoakCurve c = new SoakCurve(2);
        for (double d : v) {
            c.add(d);
        }
        return c;
    }

    @Test
    void growsPastItsCapacityAndKeepsOrder() {
        SoakCurve c = new SoakCurve(1);
        for (int i = 0; i < 100; i++) {
            c.add(i);
        }
        assertEquals(100, c.size());
        assertArrayEquals(new double[] { 97, 98, 99 }, c.tail(3), 1e-9);
        assertEquals(4950.0, c.totalMs(), 1e-9);
        assertEquals(
            99.0,
            c.stats()
                .max(),
            1e-9);
    }

    @Test
    void tailIsShortWhenFewTicksAreRecorded() {
        assertArrayEquals(new double[] { 1, 2 }, of(1, 2).tail(20), 1e-9);
        assertEquals(0, new SoakCurve(4).tail(5).length);
    }

    @Test
    void trimmedMeanLeavesOutTheSlowestTicks() {
        double[] v = new double[100];
        for (int i = 0; i < 99; i++) {
            v[i] = 10.0;
        }
        v[99] = 2000.0; // one world save
        SoakCurve c = of(v);
        assertEquals(
            29.9,
            c.stats()
                .mean(),
            1e-9);
        assertEquals(10.0, c.trimmedMean(0.05), 1e-9);
        assertEquals(29.9, c.trimmedMean(0.0), 1e-9);
        assertEquals(0.0, new SoakCurve(4).trimmedMean(0.05), 1e-9);
    }

    @Test
    void tpsIsTwentyUntilTheTickIsLongerThanFiftyMs() {
        assertEquals(20.0, SoakCurve.tps(3.0), 1e-9);
        assertEquals(20.0, SoakCurve.tps(50.0), 1e-9);
        assertEquals(10.0, SoakCurve.tps(100.0), 1e-9);
    }

    @Test
    void recoveredAtIsTheEndOfTheLastSlowSecond() {
        SoakCurve c = new SoakCurve(100);
        for (int i = 0; i < 40; i++) {
            c.add(200.0); // two slow seconds
        }
        for (int i = 0; i < 20; i++) {
            c.add(5.0);
        }
        for (int i = 0; i < 20; i++) {
            c.add(i == 0 ? 1100.0 : 5.0); // one more slow second (mean 59.75)
        }
        for (int i = 0; i < 40; i++) {
            c.add(5.0);
        }
        assertEquals(80, c.recoveredAt());
        assertEquals(0, of(5, 5, 5).recoveredAt());
        assertEquals(3, of(5, 500, 5).recoveredAt());
    }

    @Test
    void bucketsPrintMeanAndMaxPerSecondAndElideTheMiddle() {
        SoakCurve c = new SoakCurve(100);
        for (int i = 0; i < 20; i++) {
            c.add(i == 0 ? 30.0 : 10.0);
        }
        for (int i = 0; i < 10; i++) {
            c.add(4.0);
        }
        assertEquals("11/30 4/4", c.buckets(10));
        assertEquals("-", new SoakCurve(4).buckets(10));
        SoakCurve longCurve = new SoakCurve(400);
        for (int i = 0; i < 400; i++) {
            longCurve.add(i / 20);
        }
        String b = longCurve.buckets(4);
        assertTrue(b.startsWith("0/0 1/1 ..."), b);
        assertTrue(b.endsWith("18/18 19/19"), b);
    }
}
