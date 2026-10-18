package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class GateRuleTest {

    @Test
    void gateMaterialsFollowTheTierMap() {
        // docs/design/0001-tier-map.md
        assertEquals("Bronze", GateRule.gateMaterial(Tier.STEAM));
        assertEquals("Steel", GateRule.gateMaterial(Tier.LV));
        assertEquals("Aluminium", GateRule.gateMaterial(Tier.MV));
        assertEquals("StainlessSteel", GateRule.gateMaterial(Tier.HV));
        assertEquals("Titanium", GateRule.gateMaterial(Tier.EV));
        assertEquals("TungstenSteel", GateRule.gateMaterial(Tier.IV));
        assertEquals("HSSG", GateRule.gateMaterial(Tier.LUV));
        assertEquals("NaquadahAlloy", GateRule.gateMaterial(Tier.ZPM));
    }

    @Test
    void everyRowOfTheTableMeetsTheGateRule() {
        Map<String, ProductionTable.Row> byOutput = ProductionTable.byOutput();
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            assertTrue(GateRule.satisfied(row, byOutput, ProductionTable.GATE_EXEMPT), row.id());
        }
    }

    @Test
    void theBulletMeetsTheRuleOnlyThroughTheExemption() {
        // GGS-06-36: the 9mm bullet has no Steel input; its gate is the LV machine voltage and the Brass case.
        Map<String, ProductionTable.Row> byOutput = ProductionTable.byOutput();
        ProductionTable.Row bullet = byOutput.get(ProductionTable.BULLET);
        assertTrue(GateRule.satisfied(bullet, byOutput, ProductionTable.GATE_EXEMPT));
        assertFalse(GateRule.satisfied(bullet, byOutput, Collections.<String>emptySet()));
    }

    @Test
    void exemptionListIsExactlyTheLooseLvAmmoOfTheSheet() {
        assertEquals(
            new HashSet<>(Arrays.asList("ammo_9mm_bullet", "ammo_shotgun_shell")),
            ProductionTable.GATE_EXEMPT);
        assertThrows(UnsupportedOperationException.class, () -> ProductionTable.GATE_EXEMPT.add("mag_9mm_pistol"));
    }

    @Test
    void theLoadedMagazineIsGatedThroughItsEmptyMagazineNotTheExemptBullet() {
        // RC-44 has no raw Steel; the gate is carried by the empty magazine (RC-37).
        Map<String, ProductionTable.Row> byOutput = ProductionTable.byOutput();
        assertTrue(
            GateRule.satisfied(byOutput.get(ProductionTable.MAGAZINE), byOutput, Collections.<String>emptySet()));
    }
}
