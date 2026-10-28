package com.gregguns.icbm.complex.palette;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * The palette's ids and its quiet look: Ztones ids have the form the pack registers, and every vanilla stand-in is a
 * muted block (grays, stone, quartz, dark clay), never a saturated wool or clay.
 */
class PaletteLookTest {

    /** Every block chain the sets hold, floors and ceilings sampled over a room. */
    private static List<BlockRef> allChains() {
        List<BlockRef> l = new ArrayList<>();
        for (StyleSet s : ComplexPalette.sets()) {
            l.addAll(s.walls);
            l.add(s.lampLit);
            l.add(s.lampUnlit);
            if (s.accent != null) l.add(s.accent);
            if (s.worn != null) l.add(s.worn);
            l.addAll(s.furniture.values());
            for (CellPattern p : s.floors)
                for (int x = 0; x < 12; x++) for (int z = 0; z < 12; z++) l.add(p.at(x, z, 12, 12));
            for (CellPattern p : s.ceilings)
                for (int x = 0; x < 12; x++) for (int z = 0; z < 12; z++) l.add(p.at(x, z, 12, 12));
        }
        l.addAll(RoomStyle.FLOOR_STAND_INS);
        l.addAll(RoomStyle.CEILING_STAND_INS);
        return l;
    }

    @Test
    void ztonesIdsAreTheRegisteredTileNames() {
        // Ztones 2.2.2 registers each metal set block as Ztones:tile.<set>Block (the pack's FML registry).
        Pattern id = Pattern.compile("tile\\.(agon|korp|lave|tank|vect)Block");
        int seen = 0;
        for (BlockRef r : allChains()) {
            for (BlockRef.Entry e : r.chain()) {
                if (!"Ztones".equals(e.modId)) continue;
                assertTrue(
                    id.matcher(e.name)
                        .matches(),
                    "Ztones id " + e);
                assertTrue(e.meta >= 0 && e.meta <= 15, "Ztones meta " + e);
                seen++;
            }
        }
        assertTrue(seen > 0);
        assertEquals("tile.agonBlock", PaletteSets.ztonesName("agon"));
    }

    /** The vanilla blocks a chain may end in: muted grays, stone, quartz, sandstone, dark or brown clay, gray wool. */
    private static final Set<String> MUTED = new HashSet<>(
        Arrays.asList(
            "stone@0",
            "double_stone_slab@0",
            "stonebrick@0",
            "stonebrick@2",
            "stonebrick@3",
            "quartz_block@0",
            "glowstone@0",
            "sandstone@2",
            "stained_hardened_clay@7",
            "stained_hardened_clay@12",
            "stained_hardened_clay@15",
            "wool@0",
            "wool@7",
            "wool@8",
            "wooden_slab@1",
            "stone_slab@7",
            "bookshelf@0"));

    @Test
    void everyVanillaStandInIsMuted() {
        for (BlockRef r : allChains()) {
            for (BlockRef.Entry e : r.chain()) {
                if (!BlockRef.VANILLA.equals(e.modId)) continue;
                assertTrue(MUTED.contains(e.name + "@" + e.meta), "loud vanilla stand-in " + e + " in " + r);
            }
        }
    }

    @Test
    void marsKeepsToItsOwnReds() {
        // Galacticraft's Mars Dungeon Brick (meta 7) is green and mossy: the Mars brick is its cobblestone.
        assertEquals(4, PaletteSets.MARS_BRICK.meta);
        assertEquals(9, PaletteSets.MARS_STONE.meta);
    }
}
