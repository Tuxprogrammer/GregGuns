package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.worldgen.Placement.Kind;

/** D309: a room's state is one answer that its lamps and machines agree on. */
class RoomStateTest {

    private static final int PLANS = 40;

    private static boolean inside(Box b, Placement p) {
        return p.x() >= b.minX && p.x() < b.maxX && p.z() >= b.minZ && p.z() < b.maxZ;
    }

    @Test
    void operationalIsStaffedAndAbandonedGuttedThroughout() {
        ComplexPlan plan = ComplexLayout.generate(3L, 1, 2);
        for (Room r : plan.rooms()) {
            assertEquals(RoomState.STAFFED, ComplexContents.roomState(plan.seed(), r, ActivityLevel.OPERATIONAL));
            assertEquals(RoomState.GUTTED, ComplexContents.roomState(plan.seed(), r, ActivityLevel.ABANDONED));
        }
    }

    @Test
    void aPartlyAbandonedRoomIsInServiceExactlyWhenItsLampsBurn() {
        int lit = 0, dark = 0;
        for (long s = 0; s < PLANS; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 1, 2);
            List<Placement> ps = ComplexContents.build(plan, ActivityLevel.PARTLY_ABANDONED, SiloCeiling.EV, null);
            for (Room r : plan.rooms()) {
                RoomState st = ComplexContents.roomState(plan.seed(), r, ActivityLevel.PARTLY_ABANDONED);
                boolean hasLamp = false;
                for (Mark m : r.marks()) hasLamp |= m.kind() == MarkKind.LIGHT;
                if (!hasLamp) continue;
                boolean burns = false;
                for (Placement p : ps) {
                    if (p.kind() == Kind.LIGHT && inside(r.bounds(), p)) burns = true;
                }
                assertEquals(
                    st == RoomState.IN_SERVICE,
                    burns,
                    "seed " + s + " room " + r.id() + " " + r.kind() + " " + st);
                if (burns) lit++;
                else dark++;
            }
        }
        double share = lit / (double) (lit + dark);
        assertTrue(share > 0.30 && share < 0.60, "about 45% of rooms in service: " + share);
    }

    @Test
    void machinesFollowTheRoomState() {
        int[] dead = new int[4], all = new int[4];
        for (long s = 0; s < PLANS; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 1, 2);
            for (ActivityLevel level : ActivityLevel.values()) {
                List<Placement> ps = ComplexContents.build(plan, level, SiloCeiling.EV, null);
                for (Room r : plan.rooms()) {
                    RoomState st = ComplexContents.roomState(plan.seed(), r, level);
                    for (Placement p : ps) {
                        if (p.kind() != Kind.MACHINE || !inside(r.bounds(), p)) continue;
                        all[st.ordinal()]++;
                        if (p.dead()) dead[st.ordinal()]++;
                    }
                }
            }
        }
        assertEquals(0, dead[RoomState.STAFFED.ordinal()], "a staffed room's machines all work");
        assertTrue(rate(dead, all, RoomState.IN_SERVICE) < 0.2, "few husks in service");
        assertTrue(rate(dead, all, RoomState.MOTHBALLED) > 0.8, "mothballed machines are husks");
        assertTrue(rate(dead, all, RoomState.GUTTED) > 0.8, "gutted machines are husks (D284)");
    }

    private static double rate(int[] dead, int[] all, RoomState st) {
        return all[st.ordinal()] == 0 ? 0 : dead[st.ordinal()] / (double) all[st.ordinal()];
    }
}
