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
        return GAP;
    }

    /** @return the room's area step count: one per {@link #AREA_STEP} interior cells, at least 1 */
    public static int steps(Kit k) {
        return Math.max(1, k.lateral() * k.depth() / AREA_STEP);
    }

    @Override
    public void lay(Kit k) {
        // Every works and stores kind has its own blueprint now (C2); a kind still without one stands empty.
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
