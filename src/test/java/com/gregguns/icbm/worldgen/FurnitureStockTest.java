package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * D323: what a room's furniture holds by its state. Over real plans: a staffed room stocks every piece that holds
 * anything, the share falls with the state, no stock lands in a caved-in room or on a piece the wear pass removed,
 * every count lies in its range, every cauldron holds water and only working machines hold finished work.
 */
class FurnitureStockTest {

    private static final int PLANS = 30;

    private static long cell(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 32) ^ (z & 0xFFFFFFFFL);
    }

    @Test
    void stockFollowsTheRoomStateAndOnlyStandingPieces() {
        Map<RoomState, int[]> share = new EnumMap<>(RoomState.class); // stockable pieces, stocked
        for (RoomState st : RoomState.values()) share.put(st, new int[2]);
        Set<String> sources = new HashSet<>();
        for (long s = 0; s < PLANS; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (ActivityLevel level : ActivityLevel.values()) {
                List<Placement> ps = ComplexContents.build(plan, level, SiloCeiling.EV, null);
                Map<Long, Placement> stock = new HashMap<>();
                Set<Long> gone = new HashSet<>();
                for (Placement p : ps) {
                    if (p.kind() == Kind.STOCK) {
                        assertTrue(stock.put(cell(p.x(), p.y(), p.z()), p) == null, "stocked twice: " + p);
                        assertNotNull(p.roomState(), "stock without a state: " + p);
                        assertNotNull(p.roomKind(), "stock without a room: " + p);
                        sources.add(p.key());
                    } else if (p.kind() == Kind.AIR || p.kind() == Kind.RUBBLE) {
                        gone.add(cell(p.x(), p.y(), p.z()));
                    }
                }
                for (Room r : plan.rooms()) {
                    boolean collapsed = ComplexContents.collapsed(plan.seed(), r, level);
                    RoomState st = ComplexContents.roomState(plan.seed(), r, level);
                    for (Mark m : r.marks()) {
                        if (m.kind() != MarkKind.FURNITURE) continue;
                        FurniturePiece piece = FurniturePiece.of(m.key());
                        Placement p = stock.get(cell(m.x(), m.y(), m.z()));
                        String source = piece == null ? null : FurnitureStock.source(piece, r.kind());
                        if (source == null) {
                            assertEquals(null, p, "a piece that holds nothing is stocked: " + m);
                            continue;
                        }
                        if (collapsed) {
                            assertEquals(null, p, "stock in a caved-in room: " + m);
                            continue;
                        }
                        if (gone.contains(cell(m.x(), m.y(), m.z()))) {
                            assertEquals(null, p, "stock on a removed piece: " + m);
                            continue;
                        }
                        if (piece == FurniturePiece.WOOD_TABLE) continue; // may hold a plate or a pot instead
                        share.get(st)[0]++;
                        if (p == null) continue;
                        share.get(st)[1]++;
                        assertEquals(st, p.roomState());
                        assertEquals(source, p.key());
                        assertTrue(p.variant() >= (FurnitureStock.DESK.equals(source) ? 0 : 1), "count: " + p);
                    }
                }
            }
        }
        double[] rate = new double[4];
        for (RoomState st : RoomState.values()) {
            int[] n = share.get(st);
            rate[st.ordinal()] = n[0] == 0 ? 0 : n[1] / (double) n[0];
            System.out.println(String.format("  %-11s %d pieces, %.2f stocked", st, n[0], rate[st.ordinal()]));
        }
        assertEquals(1.0, rate[RoomState.STAFFED.ordinal()], 1e-9, "a staffed room stocks every piece");
        assertTrue(rate[1] > rate[2] && rate[2] > rate[3], "stock falls with the state");
        assertTrue(rate[3] > 0.2, "a ruin still holds a few things");
        for (String src : new String[] { FurnitureStock.BOOKS, FurnitureStock.WEAPONS, FurnitureStock.TOOLS,
            FurnitureStock.ARMOR_COMBAT, FurnitureStock.ARMOR_HAZMAT, FurnitureStock.DESK, FurnitureStock.TYPEWRITER,
            FurnitureStock.CLIPBOARD, FurnitureStock.PLATE, FurnitureStock.COOKIES, FurnitureStock.PLANT,
            FurnitureStock.FURNACE, FurnitureStock.BREWING, FurnitureStock.MEDICINE }) {
            assertTrue(sources.contains(src), "some piece draws " + src + ": " + sources);
        }
    }

    @Test
    void countsLieInTheirRangesAndFallWithTheState() {
        for (String src : new String[] { FurnitureStock.BOOKS, FurnitureStock.MEDICINE, FurnitureStock.WEAPONS,
            FurnitureStock.PLATE, FurnitureStock.COOKIES, FurnitureStock.ARMOR_CREW }) {
            int last = Integer.MAX_VALUE;
            for (RoomState st : RoomState.values()) {
                int lo = FurnitureStock.count(src, st, 0), hi = FurnitureStock.count(src, st, 0.999999);
                assertTrue(lo >= 1 && lo <= hi, src + " " + st);
                assertTrue(hi <= last, src + " holds less as the room empties");
                last = hi;
            }
        }
    }

    @Test
    void everyCauldronHoldsWater() {
        int cauldrons = 0;
        for (long s = 0; s < PLANS; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (ActivityLevel level : ActivityLevel.values()) {
                Map<Long, Placement> blocks = new HashMap<>();
                for (Placement p : ComplexContents.build(plan, level, SiloCeiling.EV, null)) {
                    if (p.kind() == Kind.BLOCK) blocks.put(cell(p.x(), p.y(), p.z()), p);
                }
                for (Room r : plan.rooms()) {
                    if (ComplexContents.collapsed(plan.seed(), r, level)) continue;
                    RoomState st = ComplexContents.roomState(plan.seed(), r, level);
                    for (Mark m : r.marks()) {
                        if (m.kind() != MarkKind.FURNITURE || FurniturePiece.of(m.key()) != FurniturePiece.CAULDRON)
                            continue;
                        cauldrons++;
                        Placement p = blocks.get(cell(m.x(), m.y(), m.z()));
                        assertNotNull(p, "a cauldron with no water: " + m + " " + st);
                        assertEquals("minecraft:cauldron", p.block());
                        assertEquals(FurnitureWear.cauldronLevel(st), p.meta());
                        assertTrue(p.meta() >= 1 && p.meta() <= 3);
                    }
                }
            }
        }
        assertTrue(cauldrons > 0);
    }

    @Test
    void onlyWorkingMachinesHoldFinishedWork() {
        int stocked = 0;
        for (long s = 0; s < PLANS; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (ActivityLevel level : ActivityLevel.values()) {
                for (Placement p : ComplexContents.build(plan, level, SiloCeiling.EV, null)) {
                    if (p.kind() != Kind.MACHINE || !p.stocked()) continue;
                    stocked++;
                    assertFalse(p.dead(), "a husk holds work: " + p);
                }
            }
        }
        assertTrue(stocked > 0);
    }

    @Test
    void cropsInAStaffedFarmGrowAtMixedStages() {
        Set<Integer> stages = new HashSet<>();
        for (long s = 0; s < PLANS; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, SiloCeiling.EV, null)) {
                if (p.kind() == Kind.BLOCK && FurnitureWear.CROPS[0].equals(p.block())) stages.add(p.meta());
            }
        }
        assertTrue(stages.contains(7) && stages.size() >= 4, "ripe and growing wheat: " + stages);
    }
}
