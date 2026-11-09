package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * D355: the Vending Machine at its room's state. The controller is the middle cell of the right-hand column as a
 * player facing the machine sees it, for every facing (StructureLib's A axis). Charlie and Bravo rooms (staffed, in
 * service, mothballed) get a working controller facing into the room; an Alpha (gutted) room a dead one with the top
 * casing of the other column knocked out; a caved-in room nothing but its rubble. Over real plans, each level's
 * contents carry exactly that.
 */
class VendingEggTest {

    private static final int[] DX = { 0, -1, 0, 1 }, DZ = { 1, 0, -1, 0 };

    @Test
    void theControllerIsOnTheViewersRight() {
        // Front north (hang 2): the viewer stands north looking south; their right is west, so the controller is the
        // western column (StructureLib: a front facing NORTH lays A to the WEST).
        assertArrayEquals(new int[] { 4, 7 }, VendingEgg.controllerColumn(new int[] { 5, 7 }, new int[] { 4, 7 }, 2));
        assertArrayEquals(new int[] { 4, 7 }, VendingEgg.controllerColumn(new int[] { 4, 7 }, new int[] { 5, 7 }, 2));
        // Front south: A is east. Front west: A is south. Front east: A is north.
        assertArrayEquals(new int[] { 5, 7 }, VendingEgg.controllerColumn(new int[] { 4, 7 }, new int[] { 5, 7 }, 0));
        assertArrayEquals(new int[] { 4, 8 }, VendingEgg.controllerColumn(new int[] { 4, 7 }, new int[] { 4, 8 }, 1));
        assertArrayEquals(new int[] { 4, 7 }, VendingEgg.controllerColumn(new int[] { 4, 7 }, new int[] { 4, 8 }, 3));
        // Columns along the facing (one behind the other) are no machine.
        assertNull(VendingEgg.controllerColumn(new int[] { 4, 7 }, new int[] { 4, 8 }, 0));
    }

    /** A machine's six marks: casing column {@code (cx, cz)}, the other one step along {@code step}, facing hang. */
    private static List<Mark> machine(int cx, int cz, int step, int hang) {
        List<Mark> out = new ArrayList<Mark>();
        long group = cx * 4096L + cz;
        for (int y = 1; y <= 3; y++) {
            out.add(new Mark(MarkKind.FURNITURE, cx, y, cz, FurniturePiece.VENDING_MACHINE.key(0, group), hang, 1));
            out.add(
                new Mark(
                    MarkKind.FURNITURE,
                    cx + DX[step],
                    y,
                    cz + DZ[step],
                    FurniturePiece.VENDING_MACHINE.key(0, group),
                    hang,
                    1));
        }
        out.add(new Mark(MarkKind.FURNITURE, 9, 1, 9, FurniturePiece.CRATE.key(0), 0, 1)); // not part of it
        return out;
    }

    @Test
    void workingInCharlieAndBravoDeadInAlphaNothingWhenCavedIn() {
        for (int hang = 0; hang < 4; hang++) {
            int right = (hang + 3) & 3;
            List<Mark> marks = machine(10, 20, right, hang);
            int[] ctrl = { 10 + DX[right], 20 + DZ[right] };
            for (RoomState st : RoomState.values()) {
                List<Placement> out = new ArrayList<Placement>();
                VendingEgg.room(marks, st, false, out);
                String where = "hang " + hang + " " + st;
                Placement c = out.get(0);
                assertEquals(Kind.VENDING, c.kind(), where);
                assertEquals(ctrl[0], c.x(), where);
                assertEquals(2, c.y(), where + ": the middle layer");
                assertEquals(ctrl[1], c.z(), where);
                assertEquals((hang + 2) & 3, c.front(), where + ": faces into the room");
                if (st == RoomState.GUTTED) {
                    assertEquals(VendingEgg.DEAD, c.key(), where);
                    assertEquals(2, out.size(), where);
                    Placement gap = out.get(1);
                    assertEquals(VendingEgg.GAP, gap.key(), where);
                    assertEquals(10, gap.x(), where + ": the other column");
                    assertEquals(3, gap.y(), where + ": its top casing");
                    assertEquals(20, gap.z(), where);
                } else {
                    assertEquals(VendingEgg.LIVE, c.key(), where);
                    assertEquals(1, out.size(), where);
                }
                List<Placement> crushed = new ArrayList<Placement>();
                VendingEgg.room(marks, st, true, crushed);
                assertTrue(crushed.isEmpty(), where + ": a caved-in room is rubble");
            }
        }
    }

    @Test
    void eachLevelsContentsCarryItsMachine() {
        int seen = 0;
        for (long s = 0; s < 120 && seen < 25; s++) {
            ComplexPlan plan = ComplexLayout.generate(s * 31L + 5, (int) s % 7, 3);
            Room hall = null;
            for (Room r : plan.rooms()) {
                for (Mark m : r.marks()) {
                    if (FurniturePiece.of(m.key()) == FurniturePiece.VENDING_MACHINE) hall = r;
                }
            }
            if (hall == null) continue;
            seen++;
            for (ActivityLevel level : ActivityLevel.values()) {
                int live = 0, dead = 0, gaps = 0;
                for (Placement p : ComplexContents.build(plan, level, SiloCeiling.EV, null)) {
                    if (p.kind() != Kind.VENDING) continue;
                    if (VendingEgg.LIVE.equals(p.key())) live++;
                    else if (VendingEgg.DEAD.equals(p.key())) dead++;
                    else gaps++;
                }
                String where = "seed " + s + " " + level;
                if (ComplexContents.collapsed(plan.seed(), hall, level)) {
                    assertEquals(0, live + dead + gaps, where + ": a caved-in hall");
                } else if (level == ActivityLevel.ABANDONED) {
                    assertEquals(0, live, where);
                    assertEquals(1, dead, where);
                    assertEquals(1, gaps, where);
                } else {
                    assertEquals(1, live, where);
                    assertEquals(0, dead + gaps, where);
                }
            }
        }
        assertTrue(seen >= 10, "plans with a machine: " + seen);
    }
}
