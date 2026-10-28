package com.gregguns.icbm.complex.palette;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.worldgen.ActivityLevel;

/**
 * D261: a room's ceiling never shows its wall's block. Checked over every style group, dimension, activity level and
 * a range of seeds, for every combination of the mods a chain can name (each one present or absent), at every cell of
 * rooms of several sizes, so no fallback chain can collapse a ceiling onto its wall.
 */
class CeilingContrastTest {

    /** Every mod id a palette chain names, besides vanilla. */
    private static final String[] MODS = { "chisel", "gregtech", "Ztones", "GalacticraftCore", "GalacticraftMars",
        "gregguns" };

    private static final String[] DIMS = { "Overworld", "moon", "mars", "ceres", null };

    /** One lookup per subset of {@link #MODS}: vanilla only, the full pack, and every mix between. */
    private static List<BlockRef.Lookup> lookups() {
        List<BlockRef.Lookup> l = new ArrayList<>();
        for (int mask = 0; mask < 1 << MODS.length; mask++) {
            Set<String> present = new HashSet<>();
            present.add(BlockRef.VANILLA);
            for (int i = 0; i < MODS.length; i++) if ((mask & 1 << i) != 0) present.add(MODS[i]);
            l.add((m, n) -> present.contains(m));
        }
        return l;
    }

    @Test
    void ceilingNeverResolvesToTheWallBlock() {
        List<BlockRef.Lookup> lookups = lookups();
        int[][] sizes = { { 8, 8 }, { 13, 11 }, { 22, 18 } };
        Set<String> seen = new HashSet<>();
        int checked = 0;
        for (StyleGroup g : StyleGroup.values()) {
            for (String dim : DIMS) {
                for (ActivityLevel lv : ActivityLevel.values()) {
                    for (long seed = 0; seed < 48; seed++) {
                        RoomStyle s = ComplexPalette.styleFor(g, seed * 7919L + 13, dim, lv);
                        for (int[] size : sizes) {
                            // Identical styles give identical answers; skip a repeat of the same wall and ceiling.
                            String sig = s.setId() + s.wallRef() + candidates(s, size) + size[0];
                            if (!seen.add(sig)) continue;
                            for (BlockRef.Lookup lookup : lookups) {
                                BlockRef.Entry wall = s.wallRef()
                                    .resolve(lookup);
                                for (int x = 0; x < size[0]; x++) {
                                    for (int z = 0; z < size[1]; z++) {
                                        BlockRef.Entry c = s.ceilingEntry(x, z, size[0], size[1], lookup);
                                        assertNotEquals(
                                            wall,
                                            c,
                                            g + " " + dim + " " + s.setId() + " at " + x + "," + z);
                                        checked++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked > 100_000, "checked " + checked);
    }

    private static String candidates(RoomStyle s, int[] size) {
        StringBuilder b = new StringBuilder();
        for (int x = 0; x < size[0]; x++) {
            for (int z = 0; z < size[1]; z++) b.append(s.ceilingCandidates(x, z, size[0], size[1]));
        }
        return b.toString();
    }

    @Test
    void theDrawnDesignStandsWhereItDiffersFromTheWall() {
        BlockRef.Lookup all = (m, n) -> true;
        for (StyleGroup g : StyleGroup.values()) {
            for (long seed = 0; seed < 40; seed++) {
                RoomStyle s = ComplexPalette.styleFor(g, seed, "Overworld", ActivityLevel.OPERATIONAL);
                BlockRef.Entry wall = s.wallRef()
                    .resolve(all);
                for (int x = 0; x < 13; x++) {
                    for (int z = 0; z < 11; z++) {
                        BlockRef.Entry drawn = s.ceilingRef(x, z, 13, 11)
                            .resolve(all);
                        if (!drawn.equals(wall)) assertEquals(drawn, s.ceilingEntry(x, z, 13, 11, all));
                    }
                }
            }
        }
    }

    @Test
    void theLastTwoContrastBlocksDiffer() {
        BlockRef.Lookup none = (m, n) -> false;
        assertNotEquals(RoomStyle.CONTRAST_A.resolve(none), RoomStyle.CONTRAST_B.resolve(none));
    }

    @Test
    void theLabSetHasTheDarkSteelWallsAndTheCheckerTile() {
        StyleSet lab = null;
        for (StyleSet s : ComplexPalette.sets()) if (s.id.equals("lab_dark")) lab = s;
        assertNotNull(lab, "lab_dark set");
        Set<String> walls = new HashSet<>();
        for (BlockRef w : lab.walls) walls.add(
            w.chain()
                .get(0)
                .key());
        assertTrue(
            walls.contains("chisel:laboratoryblock@11") && walls.contains("chisel:laboratoryblock@12"),
            "" + walls);
        assertEquals(
            "chisel:laboratoryblock@8",
            lab.floors.get(0)
                .at(0, 0, 9, 9)
                .chain()
                .get(0)
                .key());
        assertTrue(lab.groups.contains(StyleGroup.MEDICAL) && lab.groups.contains(StyleGroup.SERVER_ROOM));
    }
}
