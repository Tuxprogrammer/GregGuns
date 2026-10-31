package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.blueprint.Dir;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The machines, chests, guards and marks the works and stores rooms had before their blueprints (D234, D252, D289),
 * on the kit: the stand-in {@link com.gregguns.icbm.complex.blueprint.RoomBlueprints} gives a kind until its own
 * blueprint class is registered. Each count grows one step per {@link #AREA_STEP} interior cells.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D234; D252; D289
public final class LegacyRoom implements RoomBlueprint {

    /** Chest, machine and guard counts grow by one step per this many interior cells (D234). */
    public static final int AREA_STEP = 450;

    /** Partition rows in the armory, and how tall a partition stands (the frame hangs at its middle layer). */
    static final int DISPLAY_ROWS = 2, PARTITION_HEIGHT = 2;
    /** Lateral cells between two guns on one face of a partition. */
    static final int DISPLAY_STRIDE = 5;
    /** Most guns one armory shows (D289). */
    static final int MAX_DISPLAYS = 24;

    private final RoomKind kind;

    public LegacyRoom(RoomKind kind) {
        this.kind = kind;
    }

    @Override
    public int lampGap() {
        return kind == RoomKind.STOCKPILE || kind == RoomKind.ARMORY ? 4 : GAP;
    }

    /** @return the room's area step count: one per {@link #AREA_STEP} interior cells, at least 1 */
    public static int steps(Kit k) {
        return Math.max(1, k.lateral() * k.depth() / AREA_STEP);
    }

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth(), ma = L / 2, md = D / 2, last = L - 1;
        int q = steps(k);
        switch (kind) {
            case WARHEAD_MAGAZINE: {
                int n = 0;
                for (int d = 2; d <= D - 4 && n < 4 + 2 * q; d += 3) {
                    for (int a = 2; a <= last - 2 && n < 4 + 2 * q; a += 3) {
                        if (k.free(a, d) && k.machineNear(a, d, MachineRole.MAGAZINE_CASING)) n++;
                    }
                }
                for (int i = 0; i < Math.max(2, q); i++) {
                    k.floorMark(
                        k.pick(7, i, 0, L),
                        1 + k.pick(8, i, 0, Math.max(1, D - 3)),
                        MarkKind.RUBBLE,
                        "fallout");
                }
                backRow(k, q, LootKeys.MAGAZINE);
                k.guards(q, 2);
                break;
            }
            case FUEL_DEPOT:
                for (int d = 2, n = 0; d <= D - 4 && n < 2 + q; d += 4, n += 2) {
                    k.machineNear(1, d, MachineRole.FUEL_TANK);
                    k.machineNear(last - 1, d, MachineRole.FUEL_TANK);
                }
                backRow(k, q, LootKeys.FUEL);
                break;
            case GENERATOR_HALL:
                for (int a = 2, n = 0; a <= last - 2 && n < 2 + q; a += 4, n++) {
                    k.machineNear(a, md, MachineRole.GENERATOR);
                }
                k.machineNear(last, 0, MachineRole.BATTERY_BUFFER);
                k.guards(q, 2);
                break;
            case AIR_HANDLING:
                for (int i = 0; i < q; i++) k.machineNear((int) ((i + 0.5) * L / q), md, MachineRole.AIR_HANDLER);
                break;
            case PUMP_ROOM:
                for (int i = 0; i < q; i++) k.machineNear((int) ((i + 0.5) * L / q), md, MachineRole.PUMP);
                for (int a = 0; a <= last; a += 2) k.floorMark(a, md + 1, MarkKind.FLOOD_CELL, "flood");
                break;
            case WORKSHOP:
                k.anchorNear(nasa(k), 1, 1);
                k.machineNear(ma + 2, md, MachineRole.WORKBENCH);
                if (q > 2) k.machineNear(ma + 6, md, MachineRole.WORKBENCH);
                k.putNear(last, 0, 'C');
                break;
            case STOCKPILE:
                for (int a = 1; a <= last; a += 4) wallChest(k, a, LootKeys.STOCKPILE);
                break;
            case ARMORY:
                displays(k);
                backRow(k, q, LootKeys.ARMORY);
                k.guards(q, 2);
                k.column(0, 3, D - 4, "rrr.");
                k.column(last, 3, D - 4, "a.");
                break;
            case VEHICLE_BAY:
                k.machineNear(0, 0, MachineRole.BATTERY_BUFFER);
                k.machineNear(last, 0, MachineRole.BATTERY_BUFFER);
                k.machineNear(ma, 0, MachineRole.WORKBENCH);
                k.putNear(1, 0, 'C');
                break;
            case MISSILE_ASSEMBLY:
                k.anchorNear(nasa(k), ma - 2, md - 2);
                k.machineNear(2, md, MachineRole.WORKBENCH);
                k.machineNear(last - 2, md, MachineRole.WORKBENCH);
                for (int i = 0; i < 1 + q; i++) k.machineNear(3 + i * 3, 0, MachineRole.MAGAZINE_CASING);
                k.putNear(0, 0, 'C');
                k.putNear(last, 0, 'C');
                k.guards(q, 2);
                break;
            case WATER_TREATMENT:
                for (int a = 2; a + 2 <= last - 2; a += 6) {
                    for (int d = 2; d <= D - 5; d++) for (int da = 0; da < 3; da++) k.put(a + da, d, 'o');
                }
                for (int i = 0; i < q; i++) k.machineNear((int) ((i + 0.5) * L / q), 0, MachineRole.PUMP);
                break;
            default:
                break;
        }
    }

    /** The NASA Workbench (D252): the core 3 x 3 strictly free, the ring free or on a light tile. */
    public static com.gregguns.icbm.complex.blueprint.Module nasa(Kit k) {
        k.legend()
            .set('N', Legend.machine(MachineRole.NASA_WORKBENCH));
        return k.module("+++++", "+***+", "+*N*+", "+***+", "+++++");
    }

    /** {@code n} chests spread along the back wall, each on the nearest cell free there. */
    static void backRow(Kit k, int n, String key) {
        for (int i = 0; i < n; i++) wallChest(k, (int) ((i + 0.5) * k.lateral() / n), key);
    }

    private static void wallChest(Kit k, int a, String key) {
        Legend.Entry c = Legend.chest(key);
        for (int off = 0; off < k.lateral(); off++) {
            if (k.put(a + off, 0, c, 'C') || k.put(a - off, 0, c, 'C')) return;
        }
        k.markNear(a, 0, c, 'C');
    }

    /**
     * Free-standing display walls across the armory (D289): partitions two blocks high of the room's wall block, a
     * walkway between rows, a gun in an item frame every {@link #DISPLAY_STRIDE} cells on each face (staggered), its
     * ammo chest on the floor below it.
     */
    static void displays(Kit k) {
        int L = k.lateral(), D = k.depth();
        if (L < 10) return;
        Legend.Entry wall = Legend.piece(FurniturePiece.PARTITION, FurniturePiece.PARTITION)
            .facing(Dir.TO_DOOR);
        int shown = 0;
        for (int i = 0; i < DISPLAY_ROWS; i++) {
            int dw = 4 + 6 * i;
            if (dw + 1 > D - 4) break; // the front chest and its walkway must stay clear of the door rows
            for (int a = 3; a <= L - 4; a++) k.put(a, dw, wall, 'Z');
            for (int face = -1; face <= 1; face += 2) {
                Legend.Entry gun = Legend.mark(MarkKind.GUN_DISPLAY, LootKeys.ARMORY)
                    .facing(face > 0 ? Dir.TO_DOOR : Dir.TO_BACK);
                for (int a = face > 0 ? 3 : 5; a <= L - 4; a += DISPLAY_STRIDE) {
                    if (shown >= MAX_DISPLAYS || k.pieceAt(a, dw) != FurniturePiece.PARTITION) continue;
                    if (k.put(a, dw + face, gun, 'C')) shown++;
                }
            }
        }
    }
}
