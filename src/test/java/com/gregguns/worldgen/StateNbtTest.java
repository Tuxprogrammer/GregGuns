package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.Test;

/** NBT round trip, and tolerance of unknown or missing keys. */
class StateNbtTest {

    private static PlanLedger sample() {
        PlanLedger l = new PlanLedger();
        l.decideOnce("MEDIUM:1:2", () -> Decision.placed(64));
        l.decideOnce("BIG:-3:0", () -> Decision.placed(70));
        l.addFlag("BIG:-3:0", new FlagPos(-100, 71, 5));
        l.addFlag("BIG:-3:0", new FlagPos(-90, 71, 9));
        l.decideOnce("SMALL:0:9", () -> Decision.rejected("water"));
        return l;
    }

    private static NBTTagCompound entry(PlanLedger l, int index) {
        NBTTagCompound tag = new NBTTagCompound();
        StateNbt.write(l, tag);
        return tag.getTagList(StateNbt.PLANS, 10)
            .getCompoundTagAt(index);
    }

    @Test
    void roundTripKeepsEverythingInOrder() {
        PlanLedger in = sample();
        NBTTagCompound tag = new NBTTagCompound();
        StateNbt.write(in, tag);
        PlanLedger out = StateNbt.read(tag);

        assertEquals(in.all(), out.all());
        assertEquals(
            3,
            out.all()
                .size());
        assertEquals(
            "[MEDIUM:1:2, BIG:-3:0, SMALL:0:9]",
            out.all()
                .keySet()
                .toString());
        Decision big = out.get("BIG:-3:0");
        assertEquals(70, big.groundY());
        assertEquals(
            new FlagPos(-100, 71, 5),
            big.flags()
                .get(0));
        assertEquals(
            new FlagPos(-90, 71, 9),
            big.flags()
                .get(1));
        assertEquals(
            "water",
            out.get("SMALL:0:9")
                .reason());
    }

    @Test
    void aRestoredLedgerIsNotMarkedChanged() {
        NBTTagCompound tag = new NBTTagCompound();
        StateNbt.write(sample(), tag);
        assertEquals(
            0,
            StateNbt.read(tag)
                .revision());
    }

    @Test
    void emptyAndNullInputsGiveAnEmptyLedger() {
        assertEquals(
            0,
            StateNbt.read(new NBTTagCompound())
                .size());
        assertEquals(
            0,
            StateNbt.read(null)
                .size());
    }

    @Test
    void unknownKeysAreIgnoredAtEveryLevel() {
        NBTTagCompound tag = new NBTTagCompound();
        StateNbt.write(sample(), tag);
        tag.setString("FromTheFuture", "x");
        tag.setInteger(StateNbt.VERSION, 99);
        NBTTagList list = tag.getTagList(StateNbt.PLANS, 10);
        list.getCompoundTagAt(0)
            .setString("Extra", "y");

        PlanLedger out = StateNbt.read(tag);
        assertEquals(3, out.size());
        assertEquals(
            64,
            out.get("MEDIUM:1:2")
                .groundY());
    }

    @Test
    void entryMissingItsKeyOrDecisionFlagIsSkipped() {
        NBTTagCompound tag = new NBTTagCompound();
        StateNbt.write(sample(), tag);
        NBTTagList list = tag.getTagList(StateNbt.PLANS, 10);
        list.getCompoundTagAt(0)
            .removeTag(StateNbt.KEY);
        list.getCompoundTagAt(1)
            .removeTag(StateNbt.PLACED);

        PlanLedger out = StateNbt.read(tag);
        assertEquals(1, out.size());
        assertTrue(out.get("SMALL:0:9") != null);
        assertNull(out.get("BIG:-3:0"));
    }

    @Test
    void placedEntryWithoutGroundYIsSkippedNotGuessed() {
        NBTTagCompound tag = new NBTTagCompound();
        StateNbt.write(sample(), tag);
        tag.getTagList(StateNbt.PLANS, 10)
            .getCompoundTagAt(0)
            .removeTag(StateNbt.GROUND_Y);
        PlanLedger out = StateNbt.read(tag);
        assertNull(out.get("MEDIUM:1:2"));
        assertEquals(2, out.size());
    }

    @Test
    void missingFlagsAndReasonUseDefaults() {
        NBTTagCompound tag = new NBTTagCompound();
        StateNbt.write(sample(), tag);
        NBTTagList list = tag.getTagList(StateNbt.PLANS, 10);
        list.getCompoundTagAt(1)
            .removeTag(StateNbt.FLAGS);
        list.getCompoundTagAt(2)
            .removeTag(StateNbt.REASON);

        PlanLedger out = StateNbt.read(tag);
        assertTrue(
            out.get("BIG:-3:0")
                .flags()
                .isEmpty());
        assertEquals(
            "unknown",
            out.get("SMALL:0:9")
                .reason());
    }

    @Test
    void aTruncatedFlagArrayKeepsWholeTriplesOnly() {
        NBTTagCompound tag = new NBTTagCompound();
        StateNbt.write(sample(), tag);
        tag.getTagList(StateNbt.PLANS, 10)
            .getCompoundTagAt(1)
            .setIntArray(StateNbt.FLAGS, new int[] { 1, 2, 3, 4, 5 });
        PlanLedger out = StateNbt.read(tag);
        assertEquals(
            1,
            out.get("BIG:-3:0")
                .flags()
                .size());
        assertEquals(
            new FlagPos(1, 2, 3),
            out.get("BIG:-3:0")
                .flags()
                .get(0));
    }

    @Test
    void wrongTypedPlansTagIsIgnored() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(StateNbt.PLANS, "oops");
        assertEquals(
            0,
            StateNbt.read(tag)
                .size());
    }

    @Test
    void rejectedEntryCarriesNoGroundOrFlags() {
        NBTTagCompound e = entry(sample(), 2);
        assertFalse(e.getBoolean(StateNbt.PLACED));
        assertFalse(e.hasKey(StateNbt.GROUND_Y));
        assertFalse(e.hasKey(StateNbt.FLAGS));
    }

    @Test
    void structureStateWrapperRoundTripsAndTracksDirty() {
        StructureState a = new StructureState(StructureState.NAME);
        assertFalse(a.isDirty());
        a.decideOnce("SMALL:1:1", () -> Decision.placed(65));
        assertTrue(a.isDirty());
        NBTTagCompound tag = new NBTTagCompound();
        a.writeToNBT(tag);

        StructureState b = new StructureState(StructureState.NAME);
        b.readFromNBT(tag);
        assertEquals(
            65,
            b.get("SMALL:1:1")
                .groundY());
        assertEquals(
            66,
            b.decideOnce("SMALL:2:2", () -> Decision.placed(66))
                .groundY());
        assertEquals(
            65,
            b.decideOnce("SMALL:1:1", () -> Decision.placed(1))
                .groundY());
    }
}
