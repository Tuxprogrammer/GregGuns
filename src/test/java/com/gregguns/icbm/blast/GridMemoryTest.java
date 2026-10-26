package com.gregguns.icbm.blast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The shared cap on the resistance cubes of running blasts. */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (memory cap)
class GridMemoryTest {

    @Test
    void aBlastPastTheCapWaitsUntilOthersGiveMemoryBack() {
        GridMemory mem = new GridMemory();
        long cube = GridMemory.bytesFor(50);
        long cap = cube * 2 + 1;
        assertTrue(mem.tryReserve(cube, cap));
        assertTrue(mem.tryReserve(cube, cap));
        assertFalse(mem.tryReserve(cube, cap), "the third cube would pass the cap: it is queued");
        mem.release(cube);
        assertTrue(mem.tryReserve(cube, cap), "space came back");
        assertEquals(cube * 2, mem.used());
    }

    @Test
    void aLoneBlastAlwaysRunsEvenIfItIsBiggerThanTheCap() {
        GridMemory mem = new GridMemory();
        assertTrue(mem.tryReserve(GridMemory.bytesFor(200), 1024L));
        assertFalse(mem.tryReserve(1L, 1024L), "while it runs nothing else fits");
        mem.release(GridMemory.bytesFor(200));
        assertEquals(0L, mem.used());
    }

    @Test
    void sixteenMaxRadiusBlastsDoNotAllFitUnderTheDefaultCap() {
        GridMemory mem = new GridMemory();
        long cap = 256L * 1024 * 1024;
        int running = 0;
        for (int i = 0; i < 16; i++) {
            if (mem.tryReserve(GridMemory.bytesFor(200), cap)) {
                running++;
            }
        }
        assertEquals(1, running, "radius 200 is ~190 MB a blast, so the cap runs them one at a time");
    }
}
