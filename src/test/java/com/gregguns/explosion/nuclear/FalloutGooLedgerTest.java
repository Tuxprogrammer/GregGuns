package com.gregguns.explosion.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/** The goo ledger that holds a chunk's irradiated earth back from decay (D21). */
class FalloutGooLedgerTest {

    @Test
    void gooHoldsItsWholeChunkAndReleasesItWhenTheLastBlockGoes() {
        FalloutSavedData d = new FalloutSavedData("t");
        d.addGoo(-5, 64, 20); // chunk (-1, 1)
        d.addGoo(-1, 70, 17); // the same chunk
        assertTrue(d.gooInChunkOf(-16, 31));
        assertFalse(d.gooInChunkOf(0, 20));
        assertTrue(d.removeGoo(-5, 64, 20));
        assertTrue(d.gooInChunkOf(-1, 17), "one block is still standing");
        assertTrue(d.removeGoo(-1, 70, 17));
        assertFalse(d.gooInChunkOf(-1, 17));
        assertFalse(d.removeGoo(-1, 70, 17), "a second removal is a no-op");
        assertEquals(0, d.gooCount());
    }

    @Test
    void theLedgerSurvivesNbtWithNegativeCoordinates() {
        FalloutSavedData d = new FalloutSavedData("t");
        d.addGoo(-30000000, 200, 29999999);
        d.addGoo(12, 5, -7);
        d.addGoo(12, 5, -7); // a repeat changes nothing
        NBTTagCompound tag = new NBTTagCompound();
        d.writeToNBT(tag);
        FalloutSavedData back = new FalloutSavedData("t");
        back.readFromNBT(tag);
        assertEquals(2, back.gooCount());
        assertTrue(back.gooInChunkOf(-30000000, 29999999));
        java.util.List<int[]> listed = back.gooPositions();
        assertEquals(2, listed.size());
        for (int[] p : listed) {
            // The accessor must agree with what writeToNBT stores, signs included (review item 10).
            assertTrue(
                (p[0] == -30000000 && p[1] == 200 && p[2] == 29999999) || (p[0] == 12 && p[1] == 5 && p[2] == -7),
                "listed " + java.util.Arrays.toString(p));
        }
        assertTrue(back.removeGoo(-30000000, 200, 29999999));
        assertTrue(back.removeGoo(12, 5, -7));
    }

    @Test
    void aJobKeepsItsGradeAndGooCountAcrossNbt() {
        FalloutSavedData.Job j = new FalloutSavedData.Job();
        j.radius = 9;
        j.grade = 1;
        j.goo = 7;
        FalloutSavedData.Job back = FalloutSavedData.Job.read(j.write());
        assertEquals(1, back.grade);
        assertEquals(7, back.goo);
    }
}
