package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SoakStatsTest {

    @Test
    void computesMeanP95AndMax() {
        double[] v = new double[100];
        for (int i = 0; i < 100; i++) {
            v[i] = i + 1;
        }
        SoakStats s = SoakStats.of(v);
        assertEquals(50.5, s.mean(), 1e-9);
        assertEquals(95.0, s.p95(), 1e-9);
        assertEquals(100.0, s.max(), 1e-9);
        assertEquals(100, s.count());
    }

    @Test
    void emptyIsZero() {
        assertEquals(
            0,
            SoakStats.of(new double[0])
                .count());
    }

    @Test
    void passesUnderBudgetAndMargin() {
        SoakStats base = SoakStats.of(new double[] { 2, 2, 2 });
        SoakStats load = SoakStats.of(new double[] { 8, 9, 10 });
        assertNull(SoakStats.verdict(base, load, 50, 10));
    }

    @Test
    void failsOverBudget() {
        SoakStats base = SoakStats.of(new double[] { 45 });
        SoakStats load = SoakStats.of(new double[] { 50, 52 });
        assertNotNull(SoakStats.verdict(base, load, 50, 100));
    }

    @Test
    void failsOverRegressionMargin() {
        SoakStats base = SoakStats.of(new double[] { 2 });
        SoakStats load = SoakStats.of(new double[] { 20 });
        assertNotNull(SoakStats.verdict(base, load, 50, 10));
    }

    @Test
    void failsWithoutSamples() {
        assertNotNull(SoakStats.verdict(SoakStats.of(new double[0]), SoakStats.of(new double[] { 1 }), 50, 10));
    }
}
