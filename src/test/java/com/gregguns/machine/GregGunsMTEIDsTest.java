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
}
