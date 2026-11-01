package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The pump room (D305, D321): two large pools sunk {@link Kit#POOL_DEPTH} deep into the floor, one each side of the
 * aisle from the door to the back wall, and the pumps down that shared aisle: a pump with a frame-box pipe run beside
 * it every {@link #PUMP_PITCH} cells on each pool's aisle-side rim, facing the aisle. A pool keeps a walkable rim on
 * every side and stops {@link #POOL_MAX} cells out; in a wide room the floor past it takes tank rows (container prisms,
 * crates and a chest). Crates and chests along the back wall and in the corners, chests along the side walls beside
 * the pools. Flood cells on every other rim cell, which fill when the room is abandoned (D36): the pools overflowed.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Pump room); D305; D321
public final class PumpRoom implements RoomBlueprint {

    /** The least pool, either way across. */
    public static final int POOL_MIN = 3;
    /** The widest pool, across. */
    public static final int POOL_MAX = 12;
    /** Rows between pumps on a pool's rim. */
    public static final int PUMP_PITCH = 4;

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.PUMPS);
        Works.machine(k, 'M', MachineRole.PUMP);
        Works.piece(k, 'z', FurniturePiece.CASING_FRAME);
        k.spine();
        int lo = k.aisleLo(), hi = k.aisleHi();
        // The pools: from one cell past the pump rim out toward the side wall, rows 2 to D - 5 (a rim row at the
        // back, a rim row and the front rows by the door).
        int d0 = 2, d1 = D - 5;
        int[] right = k.sinkPool(hi + 2, Math.min(L - 3, hi + 1 + POOL_MAX), d0, d1, POOL_MIN);
        int[] left = k.sinkPool(Math.max(2, lo - 1 - POOL_MAX), lo - 2, d0, d1, POOL_MIN);
        if (right != null) pumps(k, right[0] - 1, right[2], right[3]);
        if (left != null) pumps(k, left[1] + 1, left[2], left[3]);
        k.mirrorStrip(0, 1, L - 2, 1, "k.C.");
        k.column(0, 3, D - 5, "C..k..");
        k.column(L - 1, 3, D - 5, "C..k..");
        // Past a pool held to its widest, the rest of the floor takes tank rows.
        Module[] tanks = { k.module("KKKk", "KKKk", "kCzk"), k.module("KKK", "KKK", "kCk"), k.module("KKK", "kCk"),
            k.module("kC") };
        if (right != null && right[1] + 3 <= L - 3) outer(k, right[1] + 2, L - 2, tanks);
        if (left != null && left[0] - 3 >= 2) outer(k, 1, left[0] - 2, tanks);
        if (right == null || left == null) Works.fillBands(k, 2, D - 3, tanks);
        Works.corners(k);
        // Flood cells on every other rim cell (D36).
        for (int a = 0; a < L; a++) {
            for (int d = 0; d < D; d++) {
                if ((a + d & 1) == 0 && besidePool(k, a, d)) k.floorMark(a, d, MarkKind.FLOOD_CELL, "flood");
            }
        }
    }

    /**
     * A pump every {@link #PUMP_PITCH} rows down column {@code a}, the pool's rim on the aisle side, each with a
     * frame-box pipe run in the row behind it, so the pumps line the aisle; a pump that meets a light tile takes the
     * next row.
     */
    private static void pumps(Kit k, int a, int d0, int d1) {
        for (int d = d0; d <= d1; d += PUMP_PITCH) {
            for (int s = 0; s < 2 && d + s <= d1; s++) {
                if (k.put(a, d + s, 'M')) {
                    k.put(a, d + s + 1, 'z');
                    break;
                }
            }
        }
    }

    /** Tank rows over the floor from lateral a0 to a1 (inclusive), every band between the lamp rows. */
    private static void outer(Kit k, int a0, int a1, Module[] tanks) {
        int cut = Works.cutOff(k);
        for (int[] b : k.bands(2, k.depth() - 4)) {
            int h = b[1] - b[0] + 1;
            for (int a = a0; a <= a1;) {
                int w = 0;
                for (Module m : tanks) {
                    if (m.height > h || a + m.width - 1 > a1) continue;
                    if (k.anchorIf(m, a, b[0] + (h - m.height) / 2, kit -> Works.cutOff(kit) <= cut)) {
                        w = m.width;
                        break;
                    }
                }
                a += w > 0 ? w + 1 : 1;
            }
        }
    }

    /** @return whether a free cell touches a pool cell */
    private static boolean besidePool(Kit k, int a, int d) {
        return k.free(a, d) && (k.pool(a + 1, d) || k.pool(a - 1, d) || k.pool(a, d + 1) || k.pool(a, d - 1));
    }
}
