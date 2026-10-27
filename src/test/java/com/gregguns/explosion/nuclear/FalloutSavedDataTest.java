package com.gregguns.explosion.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/** The decay record's retry bookkeeping survives a save: the unloaded-entry count and the age cap. */
class FalloutSavedDataTest {

    @Test
    void decayKeepsRetryStateAcrossNbt() {
        FalloutSavedData.Decay d = new FalloutSavedData.Decay();
        d.due = 500L;
        d.deadline = 9000L;
        d.blocks = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        d.cursor = 2;
        d.kept = 1;
        NBTTagCompound tag = d.write();
        FalloutSavedData.Decay back = FalloutSavedData.Decay.read(tag);
        assertEquals(500L, back.due);
        assertEquals(9000L, back.deadline);
        assertEquals(2, back.cursor);
        assertEquals(1, back.kept);
        assertEquals(9, back.blocks.length);
    }

    @Test
    void oldRecordsGetTheDefaultAgeCapAndNoKeptEntries() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("due", 1000L);
        tag.setIntArray("blocks", new int[] { 1, 2, 3 });
        FalloutSavedData.Decay back = FalloutSavedData.Decay.read(tag);
        assertEquals(1000L + FalloutSavedData.Decay.MAX_AGE_TICKS, back.deadline);
        assertEquals(0, back.kept);
    }

    @Test
    void keptNeverExceedsTheCursor() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("blocks", new int[] { 1, 2, 3, 4, 5, 6 });
        tag.setInteger("cursor", 1);
        tag.setInteger("kept", 5);
        assertEquals(1, FalloutSavedData.Decay.read(tag).kept);
    }
}
