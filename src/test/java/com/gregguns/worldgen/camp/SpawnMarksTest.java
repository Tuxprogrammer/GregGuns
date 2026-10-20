package com.gregguns.worldgen.camp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutParser;

class SpawnMarksTest {

    private static Layout layout() throws Exception {
        return LayoutParser.parse(
            "t",
            "gregguns-layout 1\nid: t\nsize: 4 2 3\nground: 1\n\npalette:\nS = minecraft:stone\n\n"
                + "marks:\nspawn.npc 3 1 0\nchest.weapon 0 0 0\nspawn.npc 0 1 2\nflag 1 1 1\n\nlayers:\nlayer 0\nSSSS\nSSSS\nSSSS\n");
    }

    @Test
    void onlySpawnMarksBecomePointsAndRotateWithTheStamp() throws Exception {
        Layout l = layout();
        assertArrayEquals(
            new int[] { 3, 0, 0, 0, 0, 2 },
            SpawnMarks.offsets(l, 10, 70, 20, 0, 10, 70, 20));
        // Rotation 1: offsetX = sz-1-z, offsetZ = x.
        assertArrayEquals(
            new int[] { 2, 0, 3, 0, 0, 0 },
            SpawnMarks.offsets(l, 10, 70, 20, 1, 10, 70, 20));
        assertEquals(
            0,
            SpawnMarks.offsets(layoutNone(), 0, 0, 0, 0, 0, 0, 0).length);
    }

    private static Layout layoutNone() throws Exception {
        return LayoutParser.parse(
            "n",
            "gregguns-layout 1\nid: n\nsize: 1 1 1\n\npalette:\nS = minecraft:stone\n\nlayers:\nlayer 0\nS\n");
    }
}
