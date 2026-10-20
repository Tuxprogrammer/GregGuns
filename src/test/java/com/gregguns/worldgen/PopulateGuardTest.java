package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GGS-11-09: a chunk load during a populate is recorded; outside a populate it is not. */
class PopulateGuardTest {

    @BeforeEach
    @AfterEach
    void reset() {
        PopulateGuard.clearViolations();
    }

    @Test
    void flagIsOffByDefault() {
        assertFalse(PopulateGuard.isPopulating());
        assertNull(PopulateGuard.current());
        assertFalse(PopulateGuard.noteChunkLoad(5, 5));
        assertEquals(0, PopulateGuard.violationCount());
    }

    @Test
    void flagIsSetInsideScopeAndClearedAfter() {
        try (PopulateGuard.Scope s = PopulateGuard.enter(-4, 9)) {
            assertTrue(PopulateGuard.isPopulating());
            assertEquals(new PopulateWindow(-4, 9), PopulateGuard.current());
        }
        assertFalse(PopulateGuard.isPopulating());
    }

    @Test
    void chunkLoadDuringPopulateIsRecorded() {
        try (PopulateGuard.Scope s = PopulateGuard.enter(-4, 9)) {
            assertTrue(PopulateGuard.noteChunkLoad(-3, 9));
            assertTrue(PopulateGuard.noteChunkLoad(-4, 10));
        }
        assertEquals(2, PopulateGuard.violationCount());
        PopulateGuard.Violation v = PopulateGuard.violations()
            .get(0);
        assertEquals(-3, v.loadedX);
        assertEquals(9, v.loadedZ);
        assertEquals(-4, v.populatingX);
        assertEquals(9, v.populatingZ);
        assertTrue(
            v.toString()
                .contains("-3,9"));
    }

    @Test
    void chunkLoadAfterScopeIsNotRecorded() {
        PopulateGuard.enter(1, 1)
            .close();
        assertFalse(PopulateGuard.noteChunkLoad(2, 2));
        assertEquals(0, PopulateGuard.violationCount());
    }

    @Test
    void nestedScopeRestoresTheOuterPopulate() {
        try (PopulateGuard.Scope outer = PopulateGuard.enter(1, 1)) {
            try (PopulateGuard.Scope inner = PopulateGuard.enter(2, 2)) {
                assertEquals(new PopulateWindow(2, 2), PopulateGuard.current());
            }
            assertEquals(new PopulateWindow(1, 1), PopulateGuard.current());
        }
        assertFalse(PopulateGuard.isPopulating());
    }

    @Test
    void closeTwiceIsHarmless() {
        PopulateGuard.Scope outer = PopulateGuard.enter(1, 1);
        PopulateGuard.Scope inner = PopulateGuard.enter(2, 2);
        inner.close();
        inner.close();
        assertEquals(new PopulateWindow(1, 1), PopulateGuard.current());
        outer.close();
        assertFalse(PopulateGuard.isPopulating());
    }

    @Test
    void flagIsPerThread() throws InterruptedException {
        boolean[] otherThreadSawFlag = new boolean[1];
        try (PopulateGuard.Scope s = PopulateGuard.enter(0, 0)) {
            Thread t = new Thread(() -> otherThreadSawFlag[0] = PopulateGuard.isPopulating());
            t.start();
            t.join();
        }
        assertFalse(otherThreadSawFlag[0]);
    }

    @Test
    void clearViolationsEmptiesTheRecord() {
        try (PopulateGuard.Scope s = PopulateGuard.enter(0, 0)) {
            PopulateGuard.noteChunkLoad(1, 1);
        }
        PopulateGuard.clearViolations();
        assertTrue(
            PopulateGuard.violations()
                .isEmpty());
    }
}
