package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.RolledCeiling;

/** The D38 planet table and the Ross128 roll. */
class SiloCeilingTest {

    @Test
    void rossRollsStayInRangeAndAreSeeded() {
        int[] seen = new int[SiloCeiling.UMV + 1];
        for (long s = 0; s < 20000; s++) {
            int c = SiloCeiling.roll(s, SiloCeiling.DEFAULT_ROSS_WEIGHTS);
            assertTrue(c >= SiloCeiling.EV && c <= SiloCeiling.UMV);
            assertEquals(c, SiloCeiling.roll(s, SiloCeiling.DEFAULT_ROSS_WEIGHTS));
            seen[c]++;
        }
        for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
            assertTrue(seen[c] > 0, "ceiling " + c + " never rolled");
        }
        assertTrue(seen[SiloCeiling.EV] > seen[SiloCeiling.UMV] * 5, "weighted to the low end");
        assertEquals(SiloCeiling.EV, SiloCeiling.roll(5, null));
        assertEquals(SiloCeiling.UMV, SiloCeiling.roll(5, new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
    }

    @Test
    void namesAndDanger() {
        assertEquals("EV", SiloCeiling.name(4));
        assertEquals("UMV", SiloCeiling.name(12));
        for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
            assertEquals(RolledCeiling.danger(c), SiloCeiling.guardDanger(c), "ceiling " + c);
        }
        assertEquals(7, SiloCeiling.tableTier(12));
        assertEquals(4, SiloCeiling.tableTier(4));
    }
}
