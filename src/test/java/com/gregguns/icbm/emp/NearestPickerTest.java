package com.gregguns.icbm.emp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** The device cap: the nearest N survive, nearest first (D16). */
class NearestPickerTest {

    @Test
    void keepsTheNearestAndOrdersThemNearestFirst() {
        NearestPicker<String> p = new NearestPicker<>(3);
        p.offer("e", 25);
        p.offer("a", 1);
        p.offer("d", 16);
        p.offer("b", 4);
        p.offer("c", 9);
        assertEquals(Arrays.asList("a", "b", "c"), p.nearestFirst());
        assertEquals(5, p.offered());
    }

    @Test
    void fewerThanTheCapKeepsAll() {
        NearestPicker<Integer> p = new NearestPicker<>(10);
        p.offer(2, 2);
        p.offer(1, 1);
        assertEquals(Arrays.asList(1, 2), p.nearestFirst());
    }

    @Test
    void aTieKeepsTheEarlierCandidate() {
        NearestPicker<String> p = new NearestPicker<>(2);
        p.offer("first", 4);
        p.offer("second", 4);
        p.offer("third", 4);
        assertEquals(Arrays.asList("first", "second"), p.nearestFirst());
    }

    @Test
    void aZeroOrNegativeCapKeepsNothing() {
        NearestPicker<String> p = new NearestPicker<>(0);
        p.offer("a", 1);
        assertEquals(Collections.emptyList(), p.nearestFirst());
        assertEquals(Collections.emptyList(), new NearestPicker<String>(-3).nearestFirst());
    }

    @Test
    void agreesWithAFullSortOnAThousandRandomCandidates() {
        Random rng = new Random(42);
        int n = 1000;
        double[] d = new double[n];
        NearestPicker<Integer> p = new NearestPicker<>(64);
        for (int i = 0; i < n; i++) {
            d[i] = rng.nextInt(500);
            p.offer(i, d[i]);
        }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        // Stable sort by distance gives the same tie order as "earlier candidate wins".
        Arrays.sort(order, (a, b) -> Double.compare(d[a], d[b]));
        List<Integer> expected = Arrays.asList(order)
            .subList(0, 64);
        assertEquals(expected, p.nearestFirst());
    }
}
