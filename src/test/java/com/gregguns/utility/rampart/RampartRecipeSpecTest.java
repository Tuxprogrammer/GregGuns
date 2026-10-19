package com.gregguns.utility.rampart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.utility.rampart.RampartRecipeSpec.Row;
import com.gregguns.utility.turret.TurretLogic;

/** GGS-08-76, GGS-09-37 and GGS-09-38: the Gun Mount Hatch ids, recipes and tier gate. */
class RampartRecipeSpecTest {

    @Test
    void theHatchesAreTheRostersContiguousRunMvToIv() {
        // machine-roster.md: 20121 MV, 20122 HV, 20123 EV, 20124 IV; the Rampart is 20120.
        int id = 20121;
        for (Row row : Row.values()) {
            assertEquals(id++, row.mteId());
        }
        assertEquals(20120, GregGunsMTEIDs.RAMPART);
        assertEquals(20124, GregGunsMTEIDs.GUN_MOUNT_HATCH_IV);
        assertEquals(4, Row.values().length);
    }

    @Test
    void everyHatchHoldsItsTiersGateMaterial() {
        assertEquals("Aluminium", Row.MV.plateMaterial());
        assertEquals("StainlessSteel", Row.HV.plateMaterial());
        assertEquals("Titanium", Row.EV.plateMaterial());
        assertEquals("TungstenSteel", Row.IV.plateMaterial());
    }

    @Test
    void theRecipeRunsAtItsTierVoltageNeverAbove() {
        // GGS-09-37 and GGS-09-38
        for (Row row : Row.values()) {
            assertTrue(
                row.recipeEut() <= row.tier()
                    .voltage(),
                row.name());
            assertTrue(
                row.recipeEut() > row.tier()
                    .voltage() / 4,
                row.name());
        }
        assertEquals(1, RampartRecipeSpec.HATCH_CIRCUIT);
        assertEquals(2, RampartRecipeSpec.HATCH_PLATES);
        assertEquals(300, RampartRecipeSpec.HATCH_TICKS);
    }

    @Test
    void thereIsNoHatchBelowMvOrAboveIv() {
        assertThrows(IllegalArgumentException.class, () -> RampartRecipeSpec.rowFor(Tier.LV));
        assertThrows(IllegalArgumentException.class, () -> RampartRecipeSpec.rowFor(Tier.LUV));
        assertEquals(Row.HV, RampartRecipeSpec.rowFor(Tier.HV));
    }

    @Test
    void aHatchRefusesAGunAboveItsTier() {
        // GGS-08-76: the same gate the single Turret uses.
        for (Row row : Row.values()) {
            int hatch = row.tier()
                .voltageIndex();
            assertTrue(TurretLogic.gunAllowed(hatch, hatch), row.name() + " takes its own tier");
            assertTrue(TurretLogic.gunAllowed(Tier.LV.voltageIndex(), hatch), row.name() + " takes an LV gun");
            assertFalse(TurretLogic.gunAllowed(hatch + 1, hatch), row.name() + " refuses the next tier");
        }
    }
}
