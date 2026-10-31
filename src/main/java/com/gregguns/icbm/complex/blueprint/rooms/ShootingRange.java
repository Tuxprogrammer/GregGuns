package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.Legend;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/**
 * The shooting range (D305), its door on a short wall: a hay-bale backstop on the back wall, a wool target on a
 * fence post downrange in each lane, iron-bar lane dividers, shooting booths at the firing line, ammo chests and rifle
 * racks down both side walls and in a row behind the line, the range officer's desk. No spine runs downrange: the
 * door's clear path stops at the firing line.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Shooting range); D305
public final class ShootingRange implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        int L = k.lateral(), D = k.depth();
        k.defaultLoot(LootKeys.ARMORY); // the chest budget picks the table by room kind
        k.legend()
            .set('H', Legend.piece(FurniturePiece.HAY, FurniturePiece.HAY, FurniturePiece.HAY));
        int fire = D - 7;
        k.spine(fire + 1);
        k.strip(0, 1, L - 2, "H");
        // Side walls: ammo chests and rifle racks, clear of the lanes.
        k.column(0, 1, fire - 1, "Cr.");
        k.column(L - 1, 1, fire - 1, "Cr.");
        // Lanes 2 wide between dividers, from a 1 to a L - 2; a target in the middle of each, downrange.
        for (int a = 1; a <= L - 2; a += 3) k.column(a, 2, fire - 1, "x");
        for (int a = 2; a + 1 <= L - 2; a += 3) {
            if (!k.put(a, 1, 'W')) k.put(a + 1, 1, 'W');
        }
        k.strip(fire, 1, L - 2, "nn.");
        k.strip(fire + 3, 1, L - 2, "C..");
        k.put(L - 2, fire + 2, 'd');
        k.put(L - 3, fire + 2, 'h');
    }
}
