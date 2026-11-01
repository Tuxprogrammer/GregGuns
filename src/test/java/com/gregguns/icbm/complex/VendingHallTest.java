package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.blueprint.VendingSpot;

/**
 * D355: one Vending Machine per complex. Over many plans: a complex with a dining hall or a recreation lounge holds
 * exactly one machine (six furniture marks: two columns side by side, three layers each, one group, one facing),
 * in the hall the plan seed picks (the first of {@link VendingSpot#halls}) unless that hall had no spot; a dining
 * hall always wins over a lounge; a complex with neither holds none. Each column stands in the room's inner ring with
 * the room's wall right behind it, facing into the room, and the same plan always puts it in the same place.
 */
class VendingHallTest {

    private static final int SEEDS = 300;

    private static ComplexPlan plan(long seed) {
        return ComplexLayout.generate(seed * 104729L + 7, (int) (seed % 9) - 4, (int) (seed / 9 % 9) - 4);
    }

    private static List<Mark> machineMarks(Room r) {
        List<Mark> out = new ArrayList<Mark>();
        for (Mark m : r.marks()) {
            if (m.kind() == MarkKind.FURNITURE && FurniturePiece.of(m.key()) == FurniturePiece.VENDING_MACHINE) {
                out.add(m);
            }
        }
        return out;
    }

    private static final int[] DX = { 0, -1, 0, 1 }, DZ = { 1, 0, -1, 0 };

    @Test
    void everyComplexWithAHallHoldsExactlyOneMachineInIt() {
        int withHall = 0, inFirst = 0, withNone = 0;
        for (long s = 0; s < SEEDS; s++) {
            ComplexPlan p = plan(s);
            List<Room> halls = VendingSpot.halls(p.rooms(), p.seed());
            Room holder = null;
            int holders = 0;
            for (Room r : p.rooms()) {
                List<Mark> ms = machineMarks(r);
                if (ms.isEmpty()) continue;
                holders++;
                holder = r;
                checkMachine(r, ms, "seed " + s);
            }
            assertTrue(machineMarks(p.hall()).isEmpty(), "the hub holds no machine");
            if (halls.isEmpty()) {
                assertEquals(0, holders, "seed " + s + ": no hall, no machine");
                withNone++;
                continue;
            }
            withHall++;
            assertEquals(1, holders, "seed " + s + ": one machine per complex");
            assertTrue(halls.contains(holder), "seed " + s + ": the machine stands in a candidate hall");
            if (holder == halls.get(0)) inFirst++;
            boolean anyDining = false;
            for (Room r : halls) anyDining |= VendingSpot.dining(r.kind());
            if (anyDining) {
                assertTrue(
                    VendingSpot.dining(
                        halls.get(0)
                            .kind()),
                    "seed " + s + ": a dining hall comes before a lounge");
            }
        }
        assertTrue(withHall > SEEDS / 4, "complexes with a dining hall or lounge: " + withHall);
        assertTrue(inFirst >= withHall * 0.95, "the seed's hall takes it nearly always: " + inFirst + "/" + withHall);
        System.out.println(
            "D355: " + withHall
                + " of "
                + SEEDS
                + " complexes have a hall ("
                + withNone
                + " none); the seed's hall took the machine in "
                + inFirst);
    }

    private static void checkMachine(Room r, List<Mark> ms, String where) {
        assertEquals(2 * VendingSpot.HEIGHT, ms.size(), where + " " + ms);
        Box b = r.bounds();
        List<long[]> columns = new ArrayList<long[]>();
        int hang = ms.get(0)
            .facing() & 3;
        long group = FurniturePiece.groupOf(ms.get(0));
        for (Mark m : ms) {
            assertEquals(hang, m.facing() & 3, where + " one facing");
            assertEquals(group, FurniturePiece.groupOf(m), where + " one group");
            assertTrue(m.y() >= 1 && m.y() <= VendingSpot.HEIGHT, where + " layers 1 to 3: " + m);
            assertTrue(m.y() < b.sizeY() - 1, where + " under the ceiling: " + m);
            // The wall right behind it.
            int wx = m.x() - DX[hang], wz = m.z() - DZ[hang];
            assertTrue(
                wx == b.minX || wx == b.maxX - 1 || wz == b.minZ || wz == b.maxZ - 1,
                where + " backs onto the wall: " + m + " in " + b);
            boolean seen = false;
            for (long[] c : columns) seen |= c[0] == m.x() && c[1] == m.z();
            if (!seen) columns.add(new long[] { m.x(), m.z() });
        }
        assertEquals(2, columns.size(), where);
        long dx = columns.get(1)[0] - columns.get(0)[0], dz = columns.get(1)[1] - columns.get(0)[1];
        assertEquals(1, Math.abs(dx) + Math.abs(dz), where + " side by side");
        assertEquals(0, dx * DX[hang] + dz * DZ[hang], where + " side by side across its facing");
    }

    @Test
    void theSamePlanPutsItInTheSamePlace() {
        for (long s = 0; s < 20; s++) {
            String a = where(plan(s)), b = where(plan(s));
            assertEquals(a, b, "seed " + s);
        }
    }

    private static String where(ComplexPlan p) {
        StringBuilder sb = new StringBuilder();
        for (Room r : p.rooms()) for (Mark m : machineMarks(r)) sb.append(m)
            .append(' ')
            .append(m.facing())
            .append(';');
        return sb.toString();
    }

    @Test
    void theHallOrderPutsDiningHallsFirstAndFollowsTheSeed() {
        // Hall order is a pure function of the rooms and the seed: rebuilt lists agree, and a lounge never precedes a
        // dining hall.
        for (long s = 0; s < 60; s++) {
            ComplexPlan p = plan(s);
            List<Room> a = VendingSpot.halls(p.rooms(), p.seed()), b = VendingSpot.halls(p.rooms(), p.seed());
            assertEquals(a, b);
            boolean lounge = false;
            for (Room r : a) {
                if (r.kind() == RoomKind.REC_LOUNGE) lounge = true;
                else assertFalse(lounge, "seed " + s + ": a dining hall after a lounge");
                assertTrue(VendingSpot.candidate(r));
            }
        }
    }
}
