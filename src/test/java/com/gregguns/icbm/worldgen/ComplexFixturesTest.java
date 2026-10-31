package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.icbm.worldgen.ComplexFixtures.BlastDoor;
import com.gregguns.icbm.worldgen.ComplexFixtures.Column;
import com.gregguns.icbm.worldgen.ComplexFixtures.Run;
import com.gregguns.icbm.worldgen.ComplexFixtures.Service;

/** D335 to D337: blast doors and service runs, by the hall's state. */
class ComplexFixturesTest {

    private static final int PLANS = 30;

    private static ComplexRaster raster(long seed) {
        return new ComplexRaster(ComplexLayout.generate(seed, 1, 2));
    }

    @Test
    void everyHallMouthOffTheHubGetsADoorInItsPortal() {
        for (long s = 0; s < PLANS; s++) {
            ComplexRaster r = raster(s);
            ComplexPlan plan = r.plan();
            int marks = 0;
            for (Tunnel t : plan.tunnels()) {
                for (Mark m : t.marks()) if (m.kind() == MarkKind.BLAST_DOOR) marks++;
            }
            ComplexFixtures f = ComplexFixtures.build(r, ActivityLevel.OPERATIONAL, false);
            assertEquals(
                marks,
                f.doors()
                    .size(),
                "seed " + s);
            assertTrue(marks >= 3, "three or four halls leave the hub: seed " + s);
            for (BlastDoor d : f.doors()) {
                assertEquals(d.hall(), r.ownerAt(d.x(d.a0()), d.z(d.a0())), "the door stands in the hall's own wall");
                for (int a = d.a0(); a <= d.a1(); a++) {
                    for (int y = d.y0(); y <= d.y1(); y++) {
                        assertEquals(Cell.AIR, r.at(d.x(a), y, d.z(a)), "every piece fills the portal's air");
                    }
                    assertEquals(Column.CLOSED, d.column(a), "an operational hall is sealed");
                }
                assertTrue(d.a1() - d.a0() + 1 >= 16, "a hall mouth is the hall's full interior width");
                assertTrue(
                    d.frame()
                        .isEmpty(),
                    "no tier casings outside Ross128");
            }
        }
    }

    @Test
    void partlyAbandonedDoorsStandOpenWhenTheHallIsWorkedAndSealedWhenItIsDark() {
        int open = 0, shut = 0;
        for (long s = 0; s < PLANS; s++) {
            ComplexRaster r = raster(s);
            for (BlastDoor d : ComplexFixtures.build(r, ActivityLevel.PARTLY_ABANDONED, false)
                .doors()) {
                Column want = d.state() == RoomState.IN_SERVICE ? Column.OPEN : Column.CLOSED;
                assertTrue(d.state() == RoomState.IN_SERVICE || d.state() == RoomState.MOTHBALLED);
                for (int a = d.a0(); a <= d.a1(); a++) assertEquals(want, d.column(a));
                if (want == Column.OPEN) open++;
                else shut++;
                assertTrue(
                    d.rubble()
                        .isEmpty(),
                    "only a gutted door leaves rubble");
            }
        }
        assertTrue(open > 0 && shut > 0, "both looks occur: " + open + " open, " + shut + " shut");
    }

    @Test
    void aGuttedDoorIsStuckOpenOrBreachedAndAlwaysLeavesAWayThrough() {
        int stuck = 0, breached = 0;
        for (long s = 0; s < PLANS * 2; s++) {
            ComplexRaster r = raster(s);
            for (BlastDoor d : ComplexFixtures.build(r, ActivityLevel.ABANDONED, false)
                .doors()) {
                assertEquals(RoomState.GUTTED, d.state());
                int width = d.a1() - d.a0() + 1, closed = 0, run = 0, best = 0;
                for (int a = d.a0(); a <= d.a1(); a++) {
                    Column c = d.column(a);
                    if (c == Column.CLOSED) closed++;
                    // A way through: neighboring columns with no piece at foot or head height.
                    boolean passable = c == Column.OPEN || c == Column.TORN;
                    run = passable ? run + 1 : 0;
                    best = Math.max(best, run);
                    if (c == Column.JAMMED) {
                        int h = d.hang(a);
                        assertTrue(h >= 2 && h <= 5, "a jammed column hangs 2 to 5 pieces: " + h);
                        // Its pile reaches from the floor to the piece above.
                        int pile = 0;
                        for (int[] c3 : d.rubble()) {
                            if (c3[0] == d.x(a) && c3[2] == d.z(a)) pile++;
                        }
                        assertEquals(d.y1() - h - d.y0() + 1, pile, "the pile under a jammed column");
                    }
                }
                assertTrue(best >= ComplexFixtures.BREACH_MIN, "at least four columns stand clear: " + best);
                if (closed == 0) stuck++;
                else {
                    breached++;
                    assertTrue(width - closed >= ComplexFixtures.BREACH_MIN);
                }
            }
        }
        assertTrue(stuck > 0 && breached > 0, "both gutted looks occur: " + stuck + " stuck, " + breached);
    }

    @Test
    void rubbleNeverLandsOnAMarkOrOffTheFloor() {
        for (long s = 0; s < PLANS; s++) {
            ComplexRaster r = raster(s);
            Set<Long> marks = new HashSet<>();
            for (Mark m : r.plan()
                .allMarks()) marks.add(ComplexFixtures.key(m.x(), m.y(), m.z()));
            ComplexFixtures f = ComplexFixtures.build(r, ActivityLevel.ABANDONED, false);
            for (BlastDoor d : f.doors()) {
                for (int[] c : d.rubble()) {
                    assertFalse(marks.contains(ComplexFixtures.key(c[0], c[1], c[2])), "rubble over a mark");
                    assertEquals(Cell.AIR, r.at(c[0], c[1], c[2]));
                }
            }
            for (int[] c : f.fallen()) {
                assertFalse(marks.contains(ComplexFixtures.key(c[0], c[1], c[2])), "a fallen tray over a mark");
                assertEquals(Cell.AIR, r.at(c[0], 1, c[2]));
                assertEquals(Cell.FLOOR, r.at(c[0], 0, c[2]), "a fallen tray lies on the floor");
            }
        }
    }

    @Test
    void rossTierCasingsFrameEachDoorInItsOwnPlane() {
        ComplexRaster r = raster(7);
        for (BlastDoor d : ComplexFixtures.build(r, ActivityLevel.OPERATIONAL, true)
            .doors()) {
            assertFalse(
                d.frame()
                    .isEmpty());
            for (int[] c : d.frame()) {
                assertEquals(Cell.DOOR_FRAME, r.at(c[0], c[1], c[2]));
                assertEquals(d.plane(), d.alongX() ? c[2] : c[0], "the frame is in the door's wall layer");
            }
        }
    }

    @Test
    void runsHangInOpenAirUnderTheCeilingAndAreWholeUnlessGutted() {
        for (long s = 0; s < PLANS; s++) {
            ComplexRaster r = raster(s);
            ComplexFixtures whole = ComplexFixtures.build(r, ActivityLevel.OPERATIONAL, false);
            ComplexFixtures broken = ComplexFixtures.build(r, ActivityLevel.ABANDONED, false);
            assertFalse(
                whole.runs()
                    .isEmpty());
            assertTrue(
                whole.fallen()
                    .isEmpty());
            int wholeCells = 0, brokenCells = 0;
            for (int i = 0; i < whole.runs()
                .size(); i++) {
                Run w = whole.runs()
                    .get(i),
                    b = broken.runs()
                        .get(i);
                Tunnel t = tunnel(r.plan(), w.section());
                assertFalse(t.gallery(), "the silo gallery carries no runs");
                assertTrue(
                    w.y() >= t.bounds()
                        .sizeY() - 3,
                    "under the ceiling");
                for (int a = w.a0(); a <= w.a1(); a++) {
                    if (w.has(a)) assertEquals(Cell.AIR, r.at(w.x(a), w.y(), w.z(a)));
                    if (b.has(a)) assertTrue(w.has(a), "gutting only takes pieces away");
                }
                if (w.service() == Service.PIPE) assertTrue(t.major(), "pipes run in the major halls");
                wholeCells += w.count();
                brokenCells += b.count();
            }
            assertTrue(brokenCells < wholeCells * 0.85, "a gutted complex's runs are broken: " + brokenCells);
        }
    }

    @Test
    void noCableOrPipeTouchesADoorPiece() {
        for (long s = 0; s < PLANS; s++) {
            ComplexFixtures f = ComplexFixtures.build(raster(s), ActivityLevel.PARTLY_ABANDONED, false);
            Set<Long> door = new HashSet<>();
            for (BlastDoor d : f.doors()) {
                for (int a = d.a0(); a <= d.a1(); a++) {
                    for (int y = d.y0(); y <= d.y1(); y++) door.add(ComplexFixtures.key(d.x(a), y, d.z(a)));
                }
            }
            int[][] faces = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            for (Run r : f.runs()) {
                for (int a = r.a0(); a <= r.a1(); a++) {
                    if (!r.has(a)) continue;
                    for (int[] c : faces) {
                        assertFalse(
                            door.contains(ComplexFixtures.key(r.x(a) + c[0], r.y() + c[1], r.z(a) + c[2])),
                            "seed " + s + ": a " + r.service() + " beside a door piece would shut it");
                    }
                }
            }
        }
    }

    private static Tunnel tunnel(ComplexPlan plan, int id) {
        for (Tunnel t : plan.tunnels()) if (t.id() == id) return t;
        throw new AssertionError("no tunnel " + id);
    }
}
