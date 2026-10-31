package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.blueprint.Dir;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The armory (D289, D305): free-standing display partitions across the room every six rows while they fit, a gun in an
 * item frame every five cells on each face (staggered) with its ammo chest below, with no cap on rows or guns (the cap
 * is lifted for alpha, D309). Ammo chests and crates on the back wall, rifle racks with chests down the left wall,
 * armor stands with chests down the right, chests at the partition ends, ammo crate islands with a chest in the middle
 * row between two partitions, cleaning benches at the front.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Armory); D289; D305
public final class Armory implements RoomBlueprint {

    /** The least loot chest spots, display chests not counted (D315). */
    static final int MIN_CHESTS = 27;
    /** Lateral cells between two guns on one face of a partition. */
    static final int DISPLAY_STRIDE = 5;
    /** Rows between two display partitions. */
    static final int DISPLAY_PITCH = 6;

    @Override
    public int lampGap() {
        return 4;
    }

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.ARMORY);
        k.spine();
        displays(k);
        k.mirrorStrip(0, 1, L - 2, 1, "C.kC.");
        k.column(0, 3, D - 4, "rC");
        k.column(L - 1, 3, D - 4, "aC");
        k.anchor(3, D - 4, "TT.TT");
        k.anchor(3, D - 5, "hh.hh");
        // Ammo crate islands with a chest in the middle row between two display partitions (and after the last),
        // clear of both gun faces and of the cleaning benches.
        for (int d = 4 + DISPLAY_PITCH / 2; d <= D - 4; d += DISPLAY_PITCH) {
            Works.pack(k, d, d, 1, 2, new Module[] { k.module("kkCkk"), k.module("kCk"), k.module("k") });
        }
        Works.corners(k);
        topUpLoot(k, MIN_CHESTS);
    }

    /** Displays as D289, every {@link #DISPLAY_PITCH} rows while they fit, every gun spot taken. */
    static void displays(Kit k) {
        int L = k.lateral(), D = k.depth();
        if (L < 10) return;
        Legend.Entry wall = Legend.piece(FurniturePiece.PARTITION, FurniturePiece.PARTITION)
            .facing(Dir.TO_DOOR);
        for (int dw = 4; dw + 1 <= D - 4; dw += DISPLAY_PITCH) {
            for (int a = 3; a <= L - 4; a++) k.put(a, dw, wall, 'Z');
            for (int face = -1; face <= 1; face += 2) {
                Legend.Entry gun = Legend.mark(MarkKind.GUN_DISPLAY, LootKeys.ARMORY)
                    .facing(face > 0 ? Dir.TO_DOOR : Dir.TO_BACK);
                for (int a = face > 0 ? 3 : 5; a <= L - 4; a += DISPLAY_STRIDE) {
                    if (k.pieceAt(a, dw) == FurniturePiece.PARTITION) k.put(a, dw + face, gun, 'Z');
                }
            }
            k.put(2, dw, 'C');
            k.put(L - 3, dw, 'C');
        }
    }

    /** Adds loot chests (not displays) until there are {@code min}. */
    private static void topUpLoot(Kit k, int min) {
        int have = 0;
        for (int a = 0; a < k.lateral(); a++) {
            for (int d = 0; d < k.depth(); d++) {
                Legend.Entry e = k.at(a, d);
                if (e != null && e.chest()) have++;
            }
        }
        if (have < min) Works.topUp(k, Works.chests(k) + min - have);
    }
}
