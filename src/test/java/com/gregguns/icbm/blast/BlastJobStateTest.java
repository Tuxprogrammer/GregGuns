package com.gregguns.icbm.blast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/**
 * Save and load of a blast in progress. Names ICBM-02-20 and ICBM-02-76 (counters saved as full ints, not bytes),
 * ICBM-02-46 (a reload resumes at its stage and cursor) and ICBM-02-80 (the radius comes back as saved).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (save and load)
class BlastJobStateTest {

    private static BlastJobState sample() {
        BlastJobState s = new BlastJobState();
        s.explosiveId = "condensed";
        s.x = 10.25;
        s.y = 64.5;
        s.z = -3.75;
        s.fill = 5;
        s.radius = 17.5;
        s.sourceUuid = "11111111-2222-3333-4444-555555555555";
        s.sourceName = "player:Steve";
        s.age = 300;
        s.stage = BlastJobState.Stage.APPLY;
        s.damaged = true;
        s.effectStarted = true;
        s.breaksTerrain = true;
        s.edits = new int[] { BlastPacking.pack(1, 2, 3), BlastPacking.pack(-4, 0, 5), BlastPacking.pack(0, 0, 0) };
        s.cursor = 1;
        s.effectState.setInteger("cloud", 600);
        return s;
    }

    @Test
    void anApplyingBlastResumesAtItsCursor() {
        BlastJobState a = sample();
        BlastJobState b = BlastJobState.read(a.write(new NBTTagCompound()));
        assertEquals(BlastJobState.Stage.APPLY, b.stage, "ICBM-02-46: the stage is saved");
        assertEquals(1, b.cursor);
        assertEquals(2, b.remainingEdits());
        assertArrayEquals(a.edits, b.edits);
        assertEquals(17.5, b.radius, 0.0, "ICBM-02-80: the radius is not recomputed");
        assertEquals(5, b.fill);
        assertEquals("condensed", b.explosiveId);
        assertEquals(a.sourceUuid, b.sourceUuid);
        assertEquals(10.25, b.x, 0.0);
        assertTrue(b.damaged && b.effectStarted && b.breaksTerrain);
    }

    @Test
    void countersAreFullIntsNotBytes() {
        BlastJobState a = sample();
        a.age = 300; // the byte-saved fuse of ICBM-02-20 and ICBM-02-76 came back as 44
        a.effectState.setInteger("fuse", 100000);
        BlastJobState b = BlastJobState.read(a.write(new NBTTagCompound()));
        assertEquals(300, b.age);
        assertEquals(100000, b.effectState.getInteger("fuse"));
        assertEquals(600, b.effectState.getInteger("cloud"));
    }

    @Test
    void aBlastSavedWhileCastingRestartsFromTheSnapshotWithNoEdits() {
        BlastJobState a = sample();
        a.stage = BlastJobState.Stage.CAST;
        BlastJobState b = BlastJobState.read(a.write(new NBTTagCompound()));
        assertEquals(BlastJobState.Stage.SNAPSHOT, b.stage);
        assertEquals(0, b.edits.length);
        assertEquals(0, b.cursor);
    }

    @Test
    void corruptValuesFallBackToSafeOnes() {
        NBTTagCompound t = sample().write(new NBTTagCompound());
        t.setInteger("stage", 99);
        t.setInteger("cursor", 9999);
        t.setInteger("fill", -3);
        BlastJobState b = BlastJobState.read(t);
        assertEquals(BlastJobState.Stage.SNAPSHOT, b.stage);
        assertTrue(b.cursor <= b.edits.length);
        assertEquals(1, b.fill);
    }

    @Test
    void aBlastSavedMidDecideKeepsItsCastResultAndBothCursors() {
        // I3: a nuclear crater is about a million cells; re-casting it after a reload would cost real time.
        BlastJobState s = sample();
        s.stage = BlastJobState.Stage.DECIDE;
        s.damaged = true;
        s.edits = new int[] { BlastPacking.pack(1, 2, 3), BlastPacking.pack(-4, 0, 5), BlastPacking.pack(0, 0, 9),
            BlastPacking.pack(2, 2, 2) };
        s.cursor = 3;
        s.kept = 2;
        BlastJobState back = BlastJobState.read(s.write(new net.minecraft.nbt.NBTTagCompound()));
        assertEquals(BlastJobState.Stage.DECIDE, back.stage);
        assertEquals(4, back.edits.length);
        assertEquals(3, back.cursor);
        assertEquals(2, back.kept);
        assertTrue(back.damaged);
    }

    @Test
    void theKeptCountNeverPassesTheCursorOnLoad() {
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        BlastJobState s = sample();
        s.stage = BlastJobState.Stage.DECIDE;
        s.cursor = 1;
        s.kept = 1;
        s.write(tag);
        tag.setInteger("kept", 99);
        assertEquals(1, BlastJobState.read(tag).kept);
    }
}
