package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.loot.LootCategory;
import com.gregguns.worldgen.loot.LootDraw;
import com.gregguns.worldgen.loot.LootEntry;
import com.gregguns.worldgen.loot.LootTierCheck;

/** No silo chest line, and no drawn stack, is above its ceiling (D38, exit box 3). */
class SiloLootTablesTest {

    @Test
    void noLineIsAboveItsCeilingForAnyKeyOrCeiling() {
        for (String key : SiloLootTables.KEYS) {
            for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
                for (SiloLootTables.Line l : SiloLootTables.lines(key, c)) {
                    assertTrue(l.voltage() <= c, key + "@" + c + ": " + l.ref() + " is voltage " + l.voltage());
                }
                assertFalse(
                    SiloLootTables.lines(key, c)
                        .isEmpty(),
                    key + "@" + c + " is empty");
            }
        }
    }

    @Test
    void anUnknownKeyStillGetsALimitedTable() {
        for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
            for (SiloLootTables.Line l : SiloLootTables.lines("nonsense", c)) {
                assertTrue(l.voltage() <= c);
            }
        }
    }

    @Test
    void theTopRungAppearsAtItsCeilingAndNeverBelow() {
        for (int c = 4; c <= 12; c++) {
            boolean top = false;
            for (SiloLootTables.Line l : SiloLootTables.lines("safe", c)) {
                if (l.ref()
                    .equals("gt:plate." + SiloLootTables.material(c))) {
                    top = true;
                }
            }
            assertTrue(top, "safe@" + c + " lacks its own rung");
            if (c < 12) {
                for (SiloLootTables.Line l : SiloLootTables.lines("safe", c)) {
                    assertFalse(
                        l.ref()
                            .contains(SiloLootTables.material(c + 1)),
                        "safe@" + c + " holds " + l.ref());
                }
            }
        }
    }

    @Test
    void thousandsOfDrawnChestsStayUnderTheCeiling() {
        Random rand = new Random(2026);
        for (String key : SiloLootTables.KEYS) {
            for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
                LootCategory cat = SiloLootTables.category(key, c);
                assertTrue(
                    LootTierCheck.violations(cat, null)
                        .isEmpty(),
                    cat.name());
                for (int richness = 0; richness <= 2; richness++) {
                    for (int chest = 0; chest < 300; chest++) {
                        for (LootDraw.Roll roll : LootDraw.draw(cat, richness, rand)) {
                            LootEntry e = roll.entry();
                            assertTrue(
                                e.tier()
                                    .voltageIndex() <= c,
                                cat.name() + " drew " + e.ref());
                            assertTrue(voltageOf(key, c, e.ref()) <= c, cat.name() + " drew " + e.ref());
                        }
                    }
                }
            }
        }
    }

    private static int voltageOf(String key, int ceiling, String ref) {
        for (SiloLootTables.Line l : SiloLootTables.lines(key, ceiling)) {
            if (l.ref()
                .equals(ref)) {
                return l.voltage();
            }
        }
        throw new AssertionError(ref + " is not a line of " + key + "@" + ceiling);
    }

    @Test
    void namesAreStable() {
        assertEquals("gregguns:silo_safe_ev", SiloLootTables.categoryName("safe", 4));
        assertEquals("gregguns:silo_armory_umv", SiloLootTables.categoryName("armory", 12));
    }
}
