package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.utility.turret.TurretRecipeSpec.Row;

class TurretRecipeSpecTest {

    @Test
    void theTurretsAreTheRostersContiguousRunMvToIv() {
        // machine-roster.md: 20116 MV, 20117 HV, 20118 EV, 20119 IV.
        int id = 20116;
        for (Row row : Row.values()) {
            assertEquals(id++, row.mteId());
        }
        assertEquals(20119, GregGunsMTEIDs.TURRET_IV);
        assertEquals(4, Row.values().length);
    }

    @Test
    void everyTurretPlateIsItsTiersGateMaterial() {
        // GGS-09-38 and the tier table of recipes.md.
        assertEquals("Aluminium", Row.MV.plateMaterial());
        assertEquals("StainlessSteel", Row.HV.plateMaterial());
        assertEquals("Titanium", Row.EV.plateMaterial());
        assertEquals("TungstenSteel", Row.IV.plateMaterial());
    }

    @Test
    void theCircuitIsTheTiersOwnAndTheGridIsTheSheets() {
        assertEquals("MV", Row.MV.circuitMaterial());
        assertEquals("IV", Row.IV.circuitMaterial());
        assertEquals("PSP", TurretRecipeSpec.GRID[0]);
        assertEquals("CMC", TurretRecipeSpec.GRID[1]);
        assertEquals("KRK", TurretRecipeSpec.GRID[2]);
    }

    @Test
    void thereIsNoTurretBelowMvOrAboveIv() {
        assertThrows(IllegalArgumentException.class, () -> TurretRecipeSpec.rowFor(Tier.LV));
        assertThrows(IllegalArgumentException.class, () -> TurretRecipeSpec.rowFor(Tier.LUV));
        assertEquals(Row.HV, TurretRecipeSpec.rowFor(Tier.HV));
    }
}
