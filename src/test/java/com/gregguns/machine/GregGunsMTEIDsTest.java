package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GregGunsMTEIDsTest {

    @Test
    void everyIdIsInsideTheReservedBlockAndUnique() {
        // GGS-08-01 / GGS-08-48
        Set<Integer> seen = new HashSet<>();
        for (int id : GregGunsMTEIDs.all()) {
            assertTrue(id >= GregGunsMTEIDs.BLOCK_FIRST && id <= GregGunsMTEIDs.BLOCK_LAST, "id " + id);
            assertTrue(seen.add(id), "duplicate id " + id);
        }
    }

    @Test
    void theAmmoPressIdsAreTheRostersContiguousRun() {
        // machine-roster.md: 20100 Steam, 20101 LV, 20102 MV, 20103 HV, 20104 EV.
        assertEquals(20100, GregGunsMTEIDs.AMMO_PRESS_STEAM);
        assertEquals(20101, GregGunsMTEIDs.AMMO_PRESS_LV);
        assertEquals(20102, GregGunsMTEIDs.AMMO_PRESS_MV);
        assertEquals(20103, GregGunsMTEIDs.AMMO_PRESS_HV);
        assertEquals(20104, GregGunsMTEIDs.AMMO_PRESS_EV);
    }

    @Test
    void theAmmunitionWorksFollowsThePressRun() {
        // machine-roster.md: the Ammunition Works is 20105.
        assertEquals(20105, GregGunsMTEIDs.AMMO_WORKS);
        assertTrue(
            java.util.Arrays.stream(GregGunsMTEIDs.all())
                .anyMatch(id -> id == GregGunsMTEIDs.AMMO_WORKS));
    }

    @Test
    void theReservedBlockIs64Ids() {
        assertEquals(64, GregGunsMTEIDs.BLOCK_LAST - GregGunsMTEIDs.BLOCK_FIRST + 1);
    }

    @Test
    void theReactionMachinesUseTheRostersIds() {
        // machine-roster.md: 20106 EV chamber, 20107 IV chamber, 20108 Retort.
        assertEquals(20106, GregGunsMTEIDs.REACTION_CHAMBER_EV);
        assertEquals(20107, GregGunsMTEIDs.REACTION_CHAMBER_IV);
        assertEquals(20108, GregGunsMTEIDs.RETORT);
        java.util.List<Integer> all = new java.util.ArrayList<>();
        for (int id : GregGunsMTEIDs.all()) {
            all.add(id);
        }
        assertTrue(all.contains(GregGunsMTEIDs.REACTION_CHAMBER_EV));
        assertTrue(all.contains(GregGunsMTEIDs.REACTION_CHAMBER_IV));
        assertTrue(all.contains(GregGunsMTEIDs.RETORT));
    }

    @Test
    void theBuiltMachinesAreTheContiguousRunFromTheBlockStart() {
        // M6 utility core: every built or reserved-for-M6 machine is recorded, 20100 to 20124, so the pack-side
        // MetaTileEntityIDs rows (PACK-CHANGES.md) cover exactly these ids.
        int[] ids = GregGunsMTEIDs.all()
            .clone();
        java.util.Arrays.sort(ids);
        assertEquals(25, ids.length);
        for (int i = 0; i < ids.length; i++) {
            assertEquals(GregGunsMTEIDs.BLOCK_FIRST + i, ids[i]);
        }
    }

    @Test
    void theUtilityMachinesUseTheRostersIds() {
        // machine-roster.md (GGS-08-48): Turret MV to IV 20116 to 20119, Rampart 20120, Gun Mount Hatch MV to IV
        // 20121 to 20124. Each tier run is contiguous in tier order.
        assertEquals(20116, GregGunsMTEIDs.TURRET_MV);
        assertEquals(GregGunsMTEIDs.TURRET_MV + 1, GregGunsMTEIDs.TURRET_HV);
        assertEquals(GregGunsMTEIDs.TURRET_HV + 1, GregGunsMTEIDs.TURRET_EV);
        assertEquals(GregGunsMTEIDs.TURRET_EV + 1, GregGunsMTEIDs.TURRET_IV);
        assertEquals(20120, GregGunsMTEIDs.RAMPART);
        assertEquals(20121, GregGunsMTEIDs.GUN_MOUNT_HATCH_MV);
        assertEquals(GregGunsMTEIDs.GUN_MOUNT_HATCH_MV + 1, GregGunsMTEIDs.GUN_MOUNT_HATCH_HV);
        assertEquals(GregGunsMTEIDs.GUN_MOUNT_HATCH_HV + 1, GregGunsMTEIDs.GUN_MOUNT_HATCH_EV);
        assertEquals(20124, GregGunsMTEIDs.GUN_MOUNT_HATCH_IV);
    }
}
