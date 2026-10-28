package com.gregguns.icbm.complex.palette;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.worldgen.ActivityLevel;

class ComplexPaletteTest {

    /** Vanilla 1.7.10 registry names the chains may end in. */
    private static final Set<String> VANILLA_NAMES = new HashSet<>(
        Arrays.asList(
            "stone",
            "double_stone_slab",
            "stonebrick",
            "quartz_block",
            "glowstone",
            "sandstone",
            "stained_hardened_clay",
            "wool",
            "wooden_slab",
            "stone_slab",
            "bookshelf"));

    /** A registry with only vanilla in it: no pack mod, and no GregTech. */
    private static final BlockRef.Lookup VANILLA_ONLY = (m, n) -> BlockRef.VANILLA.equals(m);

    private static List<BlockRef> allRefs(RoomStyle s, int sx, int sz) {
        java.util.ArrayList<BlockRef> l = new java.util.ArrayList<>();
        l.add(s.wallRef());
        l.add(s.lampRef(true));
        l.add(s.lampRef(false));
        if (s.accentRef() != null) {
            l.add(s.accentRef());
        }
        for (Furniture f : Furniture.values()) {
            if (s.furnitureRef(f) != null) {
                l.add(s.furnitureRef(f));
            }
        }
        for (int x = 0; x < sx; x++) {
            for (int z = 0; z < sz; z++) {
                l.add(s.floorRef(x, z, sx, sz));
                l.add(s.ceilingRef(x, z, sx, sz));
            }
        }
        return l;
    }

    @Test
    void everyStyleResolvesToVanillaWithNoPackMods() {
        for (StyleGroup g : StyleGroup.values()) {
            for (String dim : new String[] { "Overworld", "moon", "mars", "ceres", null }) {
                for (ActivityLevel lv : ActivityLevel.values()) {
                    for (long seed = 0; seed < 40; seed++) {
                        RoomStyle s = ComplexPalette.styleFor(g, seed * 7919L, dim, lv);
                        for (BlockRef r : allRefs(s, 13, 11)) {
                            BlockRef.Entry e = r.resolve(VANILLA_ONLY);
                            assertNotNull(e);
                            assertEquals(BlockRef.VANILLA, e.modId, g + " " + r);
                            assertTrue(VANILLA_NAMES.contains(e.name), "unknown vanilla block " + e);
                        }
                        assertNotNull(s.lampRef(true));
                        assertNotNull(s.lampRef(false));
                    }
                }
            }
        }
    }

    @Test
    void everyRegistryNameIsWellFormed() {
        for (StyleSet set : ComplexPalette.sets()) {
            Set<BlockRef> refs = new LinkedHashSet<>(set.walls);
            refs.add(set.lampLit);
            refs.add(set.lampUnlit);
            if (set.accent != null) {
                refs.add(set.accent);
            }
            if (set.worn != null) {
                refs.add(set.worn);
            }
            refs.addAll(set.furniture.values());
            RoomStyle probe = ComplexPalette.styleFor(
                set.groups.iterator()
                    .next(),
                5L,
                "Overworld",
                ActivityLevel.OPERATIONAL);
            for (BlockRef r : refs) {
                checkChain(set.id, r);
            }
            for (CellPattern p : set.floors) {
                for (int x = 0; x < 12; x++) {
                    for (int z = 0; z < 12; z++) {
                        checkChain(set.id, p.at(x, z, 12, 12));
                    }
                }
            }
            for (CellPattern p : set.ceilings) {
                for (int x = 0; x < 12; x++) {
                    for (int z = 0; z < 12; z++) {
                        checkChain(set.id, p.at(x, z, 12, 12));
                    }
                }
            }
            assertNotNull(probe);
        }
        for (BlockRef.Entry e : Arrays.asList(
            PaletteSets.MOON_STONE,
            PaletteSets.MOON_BRICK,
            PaletteSets.MARS_STONE,
            PaletteSets.MARS_COBBLE,
            PaletteSets.MARS_BRICK)) {
            assertTrue(e.wellFormed(), e.toString());
        }
    }

    private static void checkChain(String id, BlockRef r) {
        assertFalse(
            r.chain()
                .isEmpty(),
            id);
        for (BlockRef.Entry e : r.chain()) {
            assertTrue(e.wellFormed(), id + " " + e);
        }
        assertEquals(
            BlockRef.VANILLA,
            r.chain()
                .get(
                    r.chain()
                        .size() - 1).modId,
            id);
    }

    @Test
    void wallIsUniformWithinARoomAndStyleIsDeterministic() {
        for (RoomKind k : RoomKind.values()) {
            for (long seed = 0; seed < 60; seed++) {
                RoomStyle a = ComplexPalette.styleFor(k, seed, "Overworld", ActivityLevel.PARTLY_ABANDONED);
                RoomStyle b = ComplexPalette.styleFor(k, seed, "Overworld", ActivityLevel.PARTLY_ABANDONED);
                assertEquals(a.setId(), b.setId());
                assertEquals(a.wallRef(), b.wallRef());
                for (int x = 0; x < 9; x++) {
                    for (int z = 0; z < 7; z++) {
                        assertEquals(a.floorRef(x, z, 9, 7), b.floorRef(x, z, 9, 7));
                        assertEquals(a.ceilingRef(x, z, 9, 7), b.ceilingRef(x, z, 9, 7));
                    }
                }
                // The wall is one method with no cell argument: one block for the whole room.
                assertSame(a.wallRef(), a.wallRef());
            }
        }
    }

    @Test
    void everyGroupHasSetsAndTheSetsVary() {
        assertTrue(
            ComplexPalette.sets()
                .size() >= 8
                && ComplexPalette.sets()
                    .size() <= 12);
        for (StyleGroup g : StyleGroup.values()) {
            Set<String> ids = new HashSet<>();
            Set<BlockRef> walls = new HashSet<>();
            Set<String> floors = new HashSet<>();
            for (long seed = 0; seed < 300; seed++) {
                RoomStyle s = ComplexPalette.styleFor(g, seed, "Overworld", ActivityLevel.OPERATIONAL);
                ids.add(s.setId());
                walls.add(s.wallRef());
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < 10; x++) {
                    for (int z = 0; z < 10; z++) {
                        sb.append(
                            s.floorRef(x, z, 10, 10)
                                .hashCode())
                            .append(',');
                    }
                }
                floors.add(sb.toString());
            }
            assertTrue(ids.size() >= 2, g + " has one style set only");
            assertTrue(walls.size() >= 4, g + " walls " + walls.size());
            assertTrue(floors.size() >= 4, g + " floors " + floors.size());
        }
    }

    @Test
    void complexWallIsOneWallChoice() {
        boolean seen = false;
        for (long seed = 0; seed < 300 && !seen; seed++) {
            RoomStyle s = ComplexPalette.styleFor(StyleGroup.STORAGE, seed, "Overworld", ActivityLevel.OPERATIONAL);
            seen = s.wallRef()
                .chain()
                .get(0).name.equals("complex_wall");
        }
        assertTrue(seen);
    }

    @Test
    void planetsUseLocalStoneAndOtherDimensionsDoNot() {
        boolean moon = false;
        boolean mars = false;
        for (long seed = 0; seed < 200; seed++) {
            if (ComplexPalette.styleFor(StyleGroup.HALL_MAJOR, seed, "moon", ActivityLevel.OPERATIONAL)
                .usesLocalStone()) {
                moon = true;
            }
            if (ComplexPalette.styleFor(StyleGroup.POWER, seed, "Mars", ActivityLevel.OPERATIONAL)
                .usesLocalStone()) {
                mars = true;
            }
            assertFalse(
                ComplexPalette.styleFor(StyleGroup.POWER, seed, "Overworld", ActivityLevel.OPERATIONAL)
                    .usesLocalStone());
            assertFalse(
                ComplexPalette.styleFor(StyleGroup.MEDICAL, seed, "moon", ActivityLevel.OPERATIONAL)
                    .usesLocalStone());
        }
        assertTrue(moon);
        assertTrue(mars);
    }

    @Test
    void localStoneFallsBackWhenGalacticraftIsAbsent() {
        BlockRef r = ComplexPalette.styleFor(StyleGroup.STORAGE, 3L, "moon", ActivityLevel.OPERATIONAL)
            .wallRef();
        assertEquals(BlockRef.VANILLA, r.resolve(VANILLA_ONLY).modId);
    }

    @Test
    void lampsAreBothLitAndUnlitInEveryStyle() {
        for (StyleSet s : ComplexPalette.sets()) {
            assertNotEquals(s.lampLit, s.lampUnlit, s.id);
            assertEquals(
                "glowstone",
                s.lampLit.chain()
                    .get(
                        s.lampLit.chain()
                            .size() - 1).name);
        }
    }

    @Test
    void chainWithoutVanillaTailIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BlockRef.of(BlockRef.e("chisel", "tyrian", 0)));
    }
}
