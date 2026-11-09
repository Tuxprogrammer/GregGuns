package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.gen.KeepRule.Kind;

/** When a cell the complex wrote is still its own (D356), and when a later generator changed it. */
class KeepRuleTest {

    private static boolean intact(String kept, int km, Kind kk, String now, int nm, Kind nk) {
        return KeepRule.intact(kept, km, kk, now, nm, nk);
    }

    @Test
    void oreOrStoneInAComplexCellIsALateWrite() {
        assertFalse(intact("gregguns:complex_wall", 0, Kind.PLAIN, "gregtech:gt.blockores2", 8526, Kind.TILE));
        assertFalse(intact("minecraft:stone", 0, Kind.PLAIN, "gregtech:gt.blockores2", 24032, Kind.PLAIN));
        assertFalse(intact("minecraft:cobblestone", 0, Kind.PLAIN, "gregtech:gt.blockgranites", 0, Kind.PLAIN));
        assertFalse(intact("minecraft:air", 0, Kind.PLAIN, "minecraft:mossy_cobblestone", 0, Kind.PLAIN));
        assertFalse(intact("minecraft:chest", 2, Kind.TILE, "minecraft:stone", 0, Kind.PLAIN));
    }

    @Test
    void aBlobThroughAGtBrickChangesOnlyTheMetaAndIsStillALateWrite() {
        assertFalse(intact("gregtech:gt.blockgranites", 3, Kind.PLAIN, "gregtech:gt.blockgranites", 0, Kind.PLAIN));
        assertTrue(intact("gregtech:gt.blockgranites", 3, Kind.PLAIN, "gregtech:gt.blockgranites", 3, Kind.PLAIN));
    }

    @Test
    void blocksThatChangeTheirOwnMetaKeepIt() {
        assertTrue(intact("minecraft:chest", 2, Kind.TILE, "minecraft:chest", 5, Kind.TILE), "a chest's facing");
        assertTrue(intact("minecraft:wheat", 1, Kind.STATEFUL, "minecraft:wheat", 7, Kind.STATEFUL), "crops grow");
        assertTrue(intact("minecraft:water", 0, Kind.LIQUID, "minecraft:water", 3, Kind.LIQUID));
    }

    @Test
    void partnerBlocksAreTheSameBlockInAnotherState() {
        assertTrue(intact("minecraft:lit_furnace", 3, Kind.TILE, "minecraft:furnace", 3, Kind.TILE), "burnt out");
        assertTrue(intact("minecraft:redstone_lamp", 0, Kind.PLAIN, "minecraft:lit_redstone_lamp", 0, Kind.PLAIN));
        assertTrue(intact("minecraft:water", 0, Kind.LIQUID, "minecraft:flowing_water", 1, Kind.LIQUID));
        assertTrue(intact("minecraft:dirt", 1, Kind.PLAIN, "etfuturum:coarse_dirt", 0, Kind.PLAIN), "Et Futurum's own");
        assertEquals("minecraft:furnace", KeepRule.family("minecraft:lit_furnace"));
        assertEquals("gregguns:complex_wall", KeepRule.family("gregguns:complex_wall"));
        assertFalse(intact("minecraft:water", 0, Kind.LIQUID, "minecraft:lava", 0, Kind.LIQUID));
    }

    @Test
    void aPieceThatBuildsItselfIntoAMultiblockIsLeftAlone() {
        assertTrue(
            intact("GalacticraftCore:tile.landingPad", 0, Kind.TILE, "GalacticraftCore:tile.dummyblock", 2, Kind.TILE),
            "nine pads formed one full pad");
        assertTrue(
            intact(
                "GalacticraftCore:tile.landingPad",
                0,
                Kind.TILE,
                "GalacticraftCore:tile.landingPadFull",
                0,
                Kind.TILE));
        assertFalse(intact("minecraft:air", 0, Kind.PLAIN, "Thaumcraft:blockAiry", 0, Kind.TILE), "a node in a room");
    }

    @Test
    void fallenRubbleIsItsOwnBusiness() {
        assertTrue(intact("minecraft:gravel", 0, Kind.FALLING, "minecraft:air", 0, Kind.PLAIN), "fell out");
        assertTrue(intact("minecraft:air", 0, Kind.PLAIN, "minecraft:gravel", 0, Kind.FALLING), "landed here");
        assertFalse(intact("minecraft:gravel", 0, Kind.FALLING, "gregtech:gt.blockores2", 1, Kind.PLAIN));
        assertFalse(intact("minecraft:cobblestone", 0, Kind.PLAIN, "minecraft:air", 0, Kind.PLAIN));
    }

    @Test
    void theKeepWaitsForTheTwoWritingChunksAndOneMoreOnEachSide() {
        assertEquals(-2, ComplexKeeper.reachMin(0));
        assertEquals(1, ComplexKeeper.reachMax(0));
        assertEquals(-12, ComplexKeeper.reachMin(-10));
        assertEquals(-9, ComplexKeeper.reachMax(-10));
    }
}
