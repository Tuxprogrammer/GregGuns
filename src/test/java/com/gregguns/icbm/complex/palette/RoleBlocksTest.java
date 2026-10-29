package com.gregguns.icbm.complex.palette;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.worldgen.ActivityLevel;

/**
 * D265: within a room the floor, the wall and the ceiling are three different blocks (registry names; another meta of
 * one block does not count). Checked at block identity: every chain entry of a set's roles, and every room over every
 * style group, dimension, activity level and many seeds, for every mix of the six mods a chain names (vanilla only,
 * the full pack and all between), counting every block a patterned floor or ceiling uses.
 */
class RoleBlocksTest {

    /** Every mod id a palette chain names, besides vanilla. */
    private static final String[] MODS = { "chisel", "gregtech", "Ztones", "GalacticraftCore", "GalacticraftMars",
        "gregguns" };

    private static final String[] DIMS = { "Overworld", "moon", "mars", "ceres", null };

    /** One lookup per subset of {@link #MODS}: index 0 is vanilla only, the last is the full pack. */
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

    private static Set<String> ids(Iterable<BlockRef> refs) {
        Set<String> s = new LinkedHashSet<>();
        for (BlockRef r : refs) for (BlockRef.Entry e : r.chain()) s.add(RoomStyle.blockId(e));
        return s;
    }

    private static List<BlockRef> refs(List<CellPattern> designs) {
        List<BlockRef> l = new ArrayList<>();
        for (CellPattern p : designs) l.addAll(p.refs());
        return l;
    }

    /**
     * The sets hold the rule by construction: no block appears, at any level of any chain, in two roles of one set.
     * So no mix of present and absent mods can make two roles resolve to one block.
     */
    @Test
    void everySetGivesEachRoleItsOwnBlocksAtEveryChainLevel() {
        for (StyleSet set : ComplexPalette.sets()) {
            Set<String> w = ids(set.walls);
            List<BlockRef> floorRefs = refs(set.floors);
            if (set.worn != null) floorRefs.add(set.worn);
            Set<String> f = ids(floorRefs);
            Set<String> c = ids(refs(set.ceilings));
            assertTrue(Collections.disjoint(w, f), set.id + " walls " + w + " and floors " + f);
            assertTrue(Collections.disjoint(w, c), set.id + " walls " + w + " and ceilings " + c);
            assertTrue(Collections.disjoint(f, c), set.id + " floors " + f + " and ceilings " + c);
            // GregTech's stone layers overwrite some blocks after the complex is written: at most one role may name
            // them, or a blob through two roles would leave both showing its block.
            int replaceable = 0;
            for (Set<String> role : Arrays.asList(w, f, c)) {
                if (!Collections.disjoint(role, PaletteSets.GT_REPLACEABLE)) replaceable++;
            }
            assertTrue(replaceable <= 1, set.id + ": " + replaceable + " roles name a block GT's stone layers replace");
            // The planet stone (D244) goes on walls or floors only, and those sets name no Galacticraft block.
            for (String id : c) assertFalse(id.startsWith("Galacticraft"), set.id + " ceiling " + id);
        }
    }

    @Test
    void everyRoomShowsThreeDifferentBlocksInEveryRegistry() {
        List<BlockRef.Lookup> lookups = lookups();
        int[][] sizes = { { 8, 8 }, { 13, 11 }, { 22, 18 } };
        Map<StyleGroup, Set<String>> setsSeen = new HashMap<>();
        Set<String> seen = new HashSet<>();
        int rooms = 0;
        for (StyleGroup g : StyleGroup.values()) {
            for (String dim : DIMS) {
                for (ActivityLevel lv : ActivityLevel.values()) {
                    for (long seed = 0; seed < 64; seed++) {
                        RoomStyle s = ComplexPalette.styleFor(g, seed * 7919L + 13, dim, lv);
                        setsSeen.computeIfAbsent(g, k -> new HashSet<>())
                            .add(s.setId());
                        // Identical draws give identical answers.
                        String sig = s.setId() + s.wallRef() + s.wallSource() + sources(s);
                        if (!seen.add(sig)) continue;
                        for (BlockRef.Lookup lookup : lookups) {
                            RoomStyle.Roles r = s.roles(lookup);
                            String where = g + " " + dim + " " + lv + " " + s.setId() + " seed " + seed;
                            assertEquals(0, r.floorIndex, where + ": the drawn floor needed the guard");
                            assertEquals(0, r.ceilingIndex, where + ": the drawn ceiling needed the guard");
                            Set<String> wall = new HashSet<>(), floor = new HashSet<>(), ceiling = new HashSet<>();
                            wall.add(
                                RoomStyle.blockId(
                                    s.wallRef()
                                        .resolve(lookup)));
                            for (int[] size : sizes) {
                                for (int x = 0; x < size[0]; x++) {
                                    for (int z = 0; z < size[1]; z++) {
                                        floor.add(
                                            RoomStyle.blockId(
                                                s.floorRef(x, z, size[0], size[1], lookup)
                                                    .resolve(lookup)));
                                        ceiling.add(
                                            RoomStyle.blockId(
                                                s.ceilingRef(x, z, size[0], size[1], lookup)
                                                    .resolve(lookup)));
                                    }
                                }
                            }
                            assertTrue(Collections.disjoint(wall, floor), where + " wall " + wall + " floor " + floor);
                            assertTrue(
                                Collections.disjoint(wall, ceiling),
                                where + " wall " + wall + " ceiling " + ceiling);
                            assertTrue(
                                Collections.disjoint(floor, ceiling),
                                where + " floor " + floor + " ceiling " + ceiling);
                            // The guard's sets cover every block the cells show.
                            assertTrue(r.floorBlocks.containsAll(floor), where + " floor refs " + r.floorBlocks);
                            assertTrue(
                                r.ceilingBlocks.containsAll(ceiling),
                                where + " ceiling refs " + r.ceilingBlocks);
                            rooms++;
                        }
                    }
                }
            }
        }
        assertTrue(rooms > 50_000, "rooms checked " + rooms);
        // Every set was drawn in every group it dresses.
        for (StyleSet set : ComplexPalette.sets()) {
            for (StyleGroup g : set.groups) {
                assertTrue(
                    setsSeen.get(g)
                        .contains(set.id),
                    set.id + " never drawn for " + g);
            }
        }
    }

    private static String sources(RoomStyle s) {
        StringBuilder b = new StringBuilder();
        b.append(
            s.floorChoices()
                .get(0).source)
            .append('|')
            .append(
                s.ceilingChoices()
                    .get(0).source);
        return b.toString();
    }

    @Test
    void theGuardStepsPastADesignThatSharesABlock() {
        BlockRef stone = BlockRef.of(BlockRef.v("stone", 0));
        BlockRef bricks = BlockRef.of(BlockRef.v("stonebrick", 0));
        BlockRef cracked = BlockRef.of(BlockRef.v("stonebrick", 2));
        // The wall is stone bricks; the drawn floor uses cracked stone bricks (another meta of the wall's block), and
        // the drawn ceiling is plain stone, the floor's other block.
        List<RoomStyle.Choice> floors = Arrays.asList(
            new RoomStyle.Choice(Patterns.checker(stone, cracked, 2), "drawn"),
            new RoomStyle.Choice(Patterns.plain(stone), "alternate"));
        List<RoomStyle.Choice> ceilings = Arrays.asList(new RoomStyle.Choice(Patterns.plain(stone), "drawn"));
        RoomStyle s = new RoomStyle(
            "probe",
            StyleGroup.GUARD,
            false,
            bricks,
            "probe wall",
            floors,
            ceilings,
            stone,
            stone,
            null,
            new java.util.EnumMap<Furniture, BlockRef>(Furniture.class));
        BlockRef.Lookup vanilla = (m, n) -> BlockRef.VANILLA.equals(m);
        RoomStyle.Roles r = s.roles(vanilla);
        assertEquals(1, r.floorIndex, "the alternate floor, which shares no block with the wall");
        assertEquals(Collections.singleton("minecraft:stone"), new HashSet<>(r.floorBlocks));
        assertTrue(r.ceilingIndex >= 1, "the stone ceiling is the floor's block: a stand-in takes over");
        assertTrue(Collections.disjoint(r.ceilingBlocks, r.floorBlocks));
        assertTrue(Collections.disjoint(r.ceilingBlocks, r.wallBlocks));
    }

    @Test
    void theStandInsAreDistinctEnoughForAnySet() {
        Set<String> c = new HashSet<>();
        for (BlockRef r : RoomStyle.CEILING_STAND_INS) c.add(
            RoomStyle.blockId(
                r.chain()
                    .get(0)));
        assertEquals(RoomStyle.CEILING_STAND_INS.size(), c.size());
        Set<String> f = new HashSet<>();
        for (BlockRef r : RoomStyle.FLOOR_STAND_INS) f.add(
            RoomStyle.blockId(
                r.chain()
                    .get(0)));
        assertEquals(RoomStyle.FLOOR_STAND_INS.size(), f.size());
        // A wall is one block and a set's floor design names at most four; six ceiling stand-ins always leave one.
        for (StyleSet set : ComplexPalette.sets()) {
            for (CellPattern p : set.floors) {
                assertTrue(
                    p.refs()
                        .size() + (set.worn == null ? 0 : 1) <= 4,
                    set.id + " floor design with many blocks");
            }
        }
        assertTrue(c.size() >= 6);
    }

    /** The owner's Laboratory looks stay (D260): its checker tile floor and its dark gray steel walls. */
    @Test
    void theLaboratoryFloorTileAndSteelWallsStayInUse() {
        Set<String> labFloors = new HashSet<>(), labWalls = new HashSet<>();
        Set<StyleGroup> labGroups = EnumSet.noneOf(StyleGroup.class);
        for (StyleSet set : ComplexPalette.sets()) {
            for (BlockRef w : set.walls) labWalls.add(
                w.chain()
                    .get(0)
                    .key());
            for (BlockRef r : refs(set.floors)) {
                String k = r.chain()
                    .get(0)
                    .key();
                if (k.equals("chisel:laboratoryblock@8")) {
                    labFloors.add(set.id);
                    labGroups.addAll(set.groups);
                    for (BlockRef c : refs(set.ceilings)) assertNotEquals(
                        "chisel:laboratoryblock",
                        RoomStyle.blockId(
                            c.chain()
                                .get(0)),
                        set.id + ": the lab ceiling is another block");
                }
            }
        }
        assertTrue(labFloors.contains("lab_dark"), "the checker tile floors lab_dark: " + labFloors);
        assertTrue(labGroups.containsAll(EnumSet.of(StyleGroup.MEDICAL, StyleGroup.SERVER_ROOM, StyleGroup.COMMAND)));
        assertTrue(
            labWalls.contains("chisel:laboratoryblock@11") && labWalls.contains("chisel:laboratoryblock@12"),
            "the dark gray steel walls: " + labWalls);
    }
}
