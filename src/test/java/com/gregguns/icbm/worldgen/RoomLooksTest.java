package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * D309: how a room looks at each activity level. Over real plans: every chest, machine and guard a room's doors reach
 * when staffed is still reached after wear and rubble at every level; crops stand only in lit rooms; a lit furnace or a
 * full cauldron only in a lit room; and the levels read true (a partly abandoned room keeps most of its pieces, an
 * abandoned one at least half, rubble counted).
 */
class RoomLooksTest {

    private static final int PLANS = 40;

    private static boolean inside(Box b, Placement p) {
        return p.x() > b.minX && p.x() < b.maxX - 1 && p.z() > b.minZ && p.z() < b.maxZ - 1;
    }

    private static boolean collapsed(Room r, List<Placement> ps) {
        for (Placement p : ps) {
            if (p.kind() == Kind.COLLAPSE && p.box() != null
                && p.box().minX == r.bounds().minX
                && p.box().minZ == r.bounds().minZ) return true;
        }
        return false;
    }

    @Test
    void wearAndRubbleNeverCutAnythingOffAndTheLevelsReadTrue() {
        Map<RoomKind, double[]> fill = new EnumMap<RoomKind, double[]>(RoomKind.class);
        Map<RoomKind, int[]> rooms = new EnumMap<RoomKind, int[]>(RoomKind.class);
        int cut = 0, checked = 0;
        for (long s = 0; s < PLANS; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (ActivityLevel level : ActivityLevel.values()) {
                List<Placement> ps = ComplexContents.build(plan, level, SiloCeiling.EV, null);
                for (Room r : plan.rooms()) {
                    if (r.kind()
                        .small() || collapsed(r, ps)) continue;
                    List<Placement> mine = new ArrayList<Placement>();
                    for (Placement p : ps) if (inside(r.bounds(), p)) mine.add(p);
                    FurnitureWear.RoomGrid before = FurnitureWear.RoomGrid
                        .of(plan, r.id(), r.marks(), r.bounds(), new ArrayList<Placement>());
                    FurnitureWear.RoomGrid after = FurnitureWear.RoomGrid.of(plan, r.id(), r.marks(), r.bounds(), mine);
                    if (before == null || after == null) continue;
                    after.after(mine);
                    List<int[]> was = before.cutOff(before.reach());
                    List<int[]> now = after.cutOff(after.reach());
                    for (int[] t : now) {
                        boolean already = false;
                        for (int[] w : was) already |= w[0] == t[0] && w[1] == t[1];
                        if (!already) {
                            cut++;
                            System.out.println(
                                "RoomLooksTest cut off: seed " + s
                                    + " "
                                    + level
                                    + " "
                                    + r.kind()
                                    + " at "
                                    + t[0]
                                    + ","
                                    + t[1]);
                        }
                    }
                    checked++;
                    fill.computeIfAbsent(r.kind(), k -> new double[3])[level.ordinal()] += after.fill();
                    rooms.computeIfAbsent(r.kind(), k -> new int[3])[level.ordinal()]++;
                }
            }
        }
        assertTrue(checked > 1000, "rooms checked: " + checked);
        assertTrue(cut == 0, cut + " chests, machines or guards cut off by wear");
        System.out.println("RoomLooksTest blocked share of the floor by kind: Alpha / Bravo / Charlie");
        for (Map.Entry<RoomKind, double[]> e : fill.entrySet()) {
            int[] n = rooms.get(e.getKey());
            double a = e.getValue()[0] / Math.max(1, n[0]), b = e.getValue()[1] / Math.max(1, n[1]),
                c = e.getValue()[2] / Math.max(1, n[2]);
            System.out.println(String.format("  %-18s %.2f / %.2f / %.2f", e.getKey(), a, b, c));
            if (c > 0.05) {
                assertTrue(b >= 0.75 * c, e.getKey() + " Bravo keeps most: " + b + " vs " + c);
                assertTrue(a >= 0.5 * c, e.getKey() + " Alpha keeps half, rubble counted: " + a + " vs " + c);
            }
        }
    }

    @Test
    void cropsGrowAndFurnacesBurnOnlyInLitRooms() {
        int crops = 0, lit = 0;
        for (long s = 0; s < PLANS; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (ActivityLevel level : ActivityLevel.values()) {
                List<Placement> ps = ComplexContents.build(plan, level, SiloCeiling.EV, null);
                for (Room r : plan.rooms()) {
                    RoomState st = ComplexContents.roomState(plan.seed(), r, level);
                    for (Placement p : ps) {
                        if (p.kind() != Kind.BLOCK || !inside(r.bounds(), p)) continue;
                        boolean crop = p.block()
                            .equals("minecraft:wheat")
                            || p.block()
                                .equals("minecraft:carrots")
                            || p.block()
                                .equals("minecraft:potatoes");
                        boolean burning = p.block()
                            .equals("minecraft:lit_furnace")
                            || p.block()
                                .equals("minecraft:cauldron") && p.meta() == 3;
                        if (crop) crops++;
                        if (burning) lit++;
                        if (crop || burning) {
                            assertTrue(FurnitureWear.lit(st), "seed " + s + " " + r.kind() + " " + st + " " + p);
                        }
                    }
                }
            }
        }
        assertTrue(crops > 0, "some farm grew crops");
        assertTrue(lit > 0, "some furnace burned");
    }

    @Test
    void metalPropsNeverPassTheCeiling() {
        for (long s = 0; s < 10; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            int props = 0;
            for (Mark m : plan.allMarks()) {
                if (m.kind() == MarkKind.FURNITURE && FurniturePiece.of(m.key()) == FurniturePiece.METAL_PROP) props++;
            }
            if (props == 0) continue;
            for (int ceiling = SiloCeiling.EV; ceiling <= SiloCeiling.UMV; ceiling++) {
                for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, ceiling, null)) {
                    if (p.kind() != Kind.BLOCK || p.block() == null) continue;
                    for (int t = ceiling + 1; t <= 12; t++) {
                        for (com.gregguns.icbm.complex.gen.MetalProps.Prop prop : com.gregguns.icbm.complex.gen.MetalProps
                            .rung(t)) {
                            boolean lower = false;
                            for (int u = 1; u <= ceiling; u++) {
                                for (com.gregguns.icbm.complex.gen.MetalProps.Prop q : com.gregguns.icbm.complex.gen.MetalProps
                                    .rung(u)) lower |= q.block.equals(prop.block) && q.meta == prop.meta;
                            }
                            assertTrue(
                                lower || !(prop.block.equals(p.block()) && prop.meta == p.meta()),
                                "ceiling " + ceiling + " placed " + p);
                        }
                    }
                }
            }
        }
    }
}
