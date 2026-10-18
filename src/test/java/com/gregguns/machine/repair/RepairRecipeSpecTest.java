package com.gregguns.machine.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.machine.repair.RepairRecipeSpec.Row;

class RepairRecipeSpecTest {

    @Test
    void theBenchesAreTheRostersContiguousRunLvToIv() {
        // machine-roster.md: 20109 LV, 20110 MV, 20111 HV, 20112 EV, 20113 IV; GGS-09-33.
        int id = 20109;
        for (Row row : Row.values()) {
            assertEquals(id++, row.mteId());
        }
        assertEquals(20113, GregGunsMTEIDs.REPAIR_BENCH_IV);
        assertEquals(20114, GregGunsMTEIDs.ARMORY_GANTRY);
        assertEquals(20115, GregGunsMTEIDs.CAMO_BENCH);
    }

    @Test
    void everyBenchPlateIsItsTiersGateMaterial() {
        // recipes.md tier table and docs/design/0001-tier-map.md.
        assertEquals("Steel", Row.LV.plateMaterial());
        assertEquals("Aluminium", Row.MV.plateMaterial());
        assertEquals("StainlessSteel", Row.HV.plateMaterial());
        assertEquals("Titanium", Row.EV.plateMaterial());
        assertEquals("TungstenSteel", Row.IV.plateMaterial());
    }

    @Test
    void theCircuitIsTheTiersOwn() {
        assertEquals("LV", Row.LV.circuitMaterial());
        assertEquals("IV", Row.IV.circuitMaterial());
    }

    @Test
    void cablesFollowTheSheet() {
        assertEquals("Tin", Row.LV.cableMaterial());
        assertEquals("AnyCopper", Row.MV.cableMaterial());
        assertEquals("Gold", Row.HV.cableMaterial());
        assertEquals("Aluminium", Row.EV.cableMaterial());
        assertEquals("Tungsten", Row.IV.cableMaterial());
    }

    @Test
    void theGantryAndCamoBenchTiersFollowTheSheet() {
        // GGS-09-34: HV controller with two Robot_Arm_HV; the Camo Bench is LV.
        assertEquals(Tier.HV, RepairRecipeSpec.GANTRY_TIER);
        assertEquals(2, RepairRecipeSpec.GANTRY_ROBOT_ARMS);
        assertEquals(Tier.LV, RepairRecipeSpec.CAMO_TIER);
        assertEquals(3, RepairRecipeSpec.CAMO_DYES);
    }

    @Test
    void noBenchExistsOutsideLvToIv() {
        assertThrows(IllegalArgumentException.class, () -> RepairRecipeSpec.rowFor(Tier.STEAM));
        assertThrows(IllegalArgumentException.class, () -> RepairRecipeSpec.rowFor(Tier.ZPM));
        assertEquals(Row.EV, RepairRecipeSpec.rowFor(Tier.EV));
    }
}
