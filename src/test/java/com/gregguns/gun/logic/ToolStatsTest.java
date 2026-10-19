package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.ToolProbeDef;

class ToolStatsTest {

    private final ToolStats saw = ToolStats.builder()
        .toolClass("axe", 3)
        .digSpeed(14.0)
        .melee(12.0, 2.0, 0.25)
        .build();

    @Test
    void defaultsAreOneRoundPerActionAndAFiveTickSwingGap() {
        assertEquals(1000, saw.hitCostMilli());
        assertEquals(1000, saw.blockCostMilli());
        assertEquals(5, saw.swingSoundGapTicks());
    }

    @Test
    void aLoadedGunDigsItsOwnClassesAndClasslessBlocksFast() {
        assertEquals(14.0, saw.digSpeed(true, "axe"));
        assertEquals(14.0, saw.digSpeed(true, null));
        assertEquals(1.0, saw.digSpeed(true, "pickaxe"));
    }

    @Test
    void anEmptyGunDigsAtSpeedOne() {
        assertEquals(1.0, saw.digSpeed(false, "axe"));
    }

    @Test
    void meleeDamageDropsWhenEmpty() {
        assertEquals(12.0, saw.meleeDamage(true));
        assertEquals(2.0, saw.meleeDamage(false));
    }

    @Test
    void harvestLevelIsMinusOneForAnUnknownClass() {
        assertEquals(3, saw.harvestLevel("axe"));
        assertEquals(-1, saw.harvestLevel("pickaxe"));
    }

    @Test
    void doesNotReproduceGUN28_noInternalDefaultToolClass() {
        assertFalse(
            saw.toolClasses()
                .contains("default"));
        assertEquals(
            1,
            saw.toolClasses()
                .size());
    }

    @Test
    void aHammerStyleToolMinesTwoClassesInOrder() {
        ToolStats hammer = ToolStats.builder()
            .toolClass("pickaxe", 2)
            .toolClass("shovel", 2)
            .digSpeed(10.0)
            .melee(6.0, 2.0, 0.0)
            .build();
        assertTrue(
            hammer.toolClasses()
                .containsAll(java.util.Arrays.asList("pickaxe", "shovel")));
        assertEquals(10.0, hammer.digSpeed(true, "shovel"));
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ToolStats.builder()
                .digSpeed(0.5)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> ToolStats.builder()
                .melee(1, 1, 1.5)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> ToolStats.builder()
                .costs(-1, 0)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> ToolStats.builder()
                .toolClass("axe", -1));
        assertThrows(
            IllegalArgumentException.class,
            () -> ToolStats.builder()
                .swingSoundGap(0)
                .build());
    }

    @Test
    void theProbeDefinitionIsATwoPartToolGun() {
        assertEquals(ToolProbeDef.TOOL, ToolProbeDef.DEFINITION.tool());
        assertEquals("guns.probe_tool_gun.swing", ToolProbeDef.DEFINITION.swingEvent());
        assertEquals("guns.probe_tool_gun.hit", ToolProbeDef.DEFINITION.hitEvent());
        assertEquals(500, ToolProbeDef.TOOL.blockCostMilli());
    }

    @Test
    void aToolGunMayAlsoBeAChargeGun() {
        // The Power Hammers are tool guns whose right button is a charge-hold shot (I10); they are fluid, not energy.
        GunStats.builder("charge_tool_probe")
            .ammoType("tank_air")
            .magazine(300)
            .charge(20, ChargeMode.SCALED, 10)
            .build();
    }
}
