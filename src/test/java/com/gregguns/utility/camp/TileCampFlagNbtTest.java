package com.gregguns.utility.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

class TileCampFlagNbtTest {

    private static TileCampFlag roundTrip(TileCampFlag in) {
        NBTTagCompound tag = new NBTTagCompound();
        in.writeFlag(tag);
        TileCampFlag out = new TileCampFlag();
        out.readFlag(tag);
        return out;
    }

    /** MUTL-38: the cap is saved, not recomputed from the size on load. */
    @Test
    void doesNotReproduceMUTL38_maxNpcsIsSaved() {
        TileCampFlag flag = new TileCampFlag();
        flag.init(32, 32, 1, 2, 3, new int[] { 1, 0, 1, 2, 0, 2 }, Arrays.asList("a", "b"), 9);
        TileCampFlag loaded = roundTrip(flag);
        assertEquals(9, loaded.maxNpcs());
        assertTrue(loaded.isInitialized());
        assertEquals(2, loaded.spawnPointCount());
        assertEquals(Arrays.asList("a", "b"), loaded.npcNames());
    }

    @Test
    void theCommanderAndWhetherHeWasPlacedAreSaved() {
        TileCampFlag flag = new TileCampFlag();
        flag.init(32, 32, 1, 2, 3, new int[] { 1, 0, 1 }, Arrays.asList("ArmySoldier"), 9);
        assertFalse(flag.commanderPending(), "no commander by default");
        assertFalse(roundTrip(flag).commanderPending());
        flag.setCommander("DictatorDave", 0, 0, -2);
        TileCampFlag loaded = roundTrip(flag);
        assertTrue(loaded.commanderPending());
        assertEquals("DictatorDave", loaded.commander());
        assertEquals(Arrays.toString(new int[] { 0, 0, -2 }), Arrays.toString(loaded.commanderPost()));
        assertFalse(loaded.placeCommander(), "no world: nothing is placed and nothing throws");
        assertTrue(loaded.commanderPending());
    }

    @Test
    void defaultCapUsesTheFormula() {
        TileCampFlag flag = new TileCampFlag();
        flag.init(32, 32, 0, 0, 0, null, null, -1);
        assertEquals(24, flag.maxNpcs());
    }

    @Test
    void raggedSpawnPointArraysDropTheTail() {
        TileCampFlag flag = new TileCampFlag();
        flag.init(8, 8, 0, 0, 0, new int[] { 1, 2, 3, 4, 5 }, Arrays.asList("a"), 1);
        assertEquals(1, flag.spawnPointCount());
    }

    /** MUTL-39: an uninitialized flag survives a save and a tick with no world. */
    @Test
    void doesNotReproduceMUTL39_uninitializedFlagIsSafe() {
        TileCampFlag flag = new TileCampFlag();
        assertFalse(roundTrip(flag).isInitialized());
        flag.updateEntity();
    }
}
