package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Module;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The incinerator (D305): a bank of furnaces two high with iron-bar chimney risers along the back wall; then every
 * band takes in turn a recycler line (GT recyclers and arc furnaces on a frame-box feed, a furnace bank with a chimney
 * behind), ash heaps and coal bunkers with a chest, and sorting stations (crates and a chest over a sorting counter
 * with stools), each stepping down to a smaller module in a short or narrow band. Crates and chests down the walls.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Incinerator); D305
public final class Incinerator implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.INCINERATOR);
        Works.machine(k, 'M', MachineRole.RECYCLER);
        Works.piece(k, 'f', FurniturePiece.FURNACE, FurniturePiece.FURNACE);
        Works.piece(k, 'V', FurniturePiece.IRON_BARS, FurniturePiece.IRON_BARS, FurniturePiece.IRON_BARS);
        Works.piece(k, 'T', FurniturePiece.WOOD_TABLE);
        Works.piece(k, 'z', FurniturePiece.CASING_FRAME);
        k.spine();
        k.mirrorStrip(0, 1, L - 2, 1, "ffffV.");
        Module[] recyclers = { k.module("MzMzM", "zzzzz", "ffVff"), k.module("MzM", "zzz", "fVf"),
            k.module("MzM", "fVf"), k.module("MzM"), k.module("M") };
        Module[] ash = { k.module("&&&zOO", "&&&zOO", "&&kCOO"), k.module("&&O", "&&O", "kCO"), k.module("&&O", "kCO"),
            k.module("&kO"), k.module("&") };
        Module[] sorting = { k.module("kkCkk", "nTTTn", "sssss"), k.module("kCk", "nTn", "sss"), k.module("kCk", "nTn"),
            k.module("kCk"), k.module("k") };
        Works.fillBands(k, 2, D - 3, recyclers, ash, sorting);
        k.column(0, 3, D - 4, "k.");
        k.column(L - 1, 3, D - 4, "C..");
        Works.corners(k);
    }
}
