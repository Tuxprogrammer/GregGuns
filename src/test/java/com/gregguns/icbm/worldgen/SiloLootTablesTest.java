package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.loot.Arsenal;
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
                java.util.Map<String, Integer> volts = new java.util.HashMap<>();
                for (SiloLootTables.Line l : SiloLootTables.lines(key, c)) volts.put(l.ref(), l.voltage());
                for (int richness = 0; richness <= 2; richness++) {
                    for (int chest = 0; chest < 100; chest++) {
                        for (LootDraw.Roll roll : LootDraw.draw(cat, richness, rand)) {
                            LootEntry e = roll.entry();
                            assertTrue(
                                e.tier()
                                    .voltageIndex() <= c,
                                cat.name() + " drew " + e.ref());
                            Integer v = volts.get(e.ref());
                            assertTrue(v != null && v <= c, cat.name() + " drew " + e.ref());
                        }
                    }
                }
            }
        }
    }

    /** @return the share of a table's weight that comes from the named pools */
    private static double share(String key, int ceiling, String... pools) {
        java.util.Set<String> want = new java.util.HashSet<>(java.util.Arrays.asList(pools));
        long in = 0, all = 0;
        for (LootEntry e : SiloLootTables.category(key, ceiling)
            .entries()) {
            all += e.weight();
            if (want.contains(e.pool())) in += e.weight();
        }
        return in / (double) all;
    }

    @Test
    void theRoomThemesHold() {
        for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
            String[] food = { SiloPools.MEALS, SiloPools.SNACKS, SiloPools.DRINKS, SiloPools.INGREDIENTS,
                SiloPools.PRODUCE, SiloPools.RAWMEAT, SiloPools.PRESERVED };
            assertTrue(share("galley", c, food) >= 0.70, "galley food at " + c + ": " + share("galley", c, food));
            assertTrue(share("canteen", c, food) >= 0.70, "canteen food at " + c);
            assertTrue(share("pantry", c, food) >= 0.85, "pantry food at " + c);
            assertTrue(share("bunks", c, SiloPools.BUNKARMOR) >= 0.45, "bunks armor at " + c);
            assertTrue(
                share("range", c, Arsenal.GUN, Arsenal.AMMO, Arsenal.GRENADE) >= 0.60,
                "range guns and ammo at " + c);
            assertTrue(
                share("armory", c, Arsenal.GUN, Arsenal.AMMO, Arsenal.GRENADE) >= 0.60,
                "armory guns and ammo at " + c);
            assertTrue(
                share("assembly", c, SiloPools.MISSILEBODY, SiloPools.WARHEADPARTS) >= 0.60,
                "assembly missile parts at " + c);
            assertTrue(
                share("magazine", c, SiloPools.WARHEADPARTS, SiloPools.EXPLOSIVES) >= 0.70,
                "magazine warhead parts at " + c);
        }
    }

    @Test
    void noLiveWarheadChipOrSchematicAndBodiesOnlyOnTheAssemblyFloor() {
        for (String key : SiloLootTables.KEYS) {
            for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
                for (SiloLootTables.Line l : SiloLootTables.lines(key, c)) {
                    String r = l.ref();
                    assertFalse(
                        r.startsWith("gregguns:") && (r.contains("chip") || r.contains("schematic")),
                        key + ": " + r);
                    assertFalse(r.startsWith("gregguns:warhead") || r.startsWith("gregguns:missile_t"), key + ": " + r);
                    if (r.startsWith("gregguns:missile_body")) {
                        assertEquals("assembly", key, "a whole body on the assembly floor only: " + key);
                        assertEquals("gregguns:missile_body_t" + ComplexContents.bodyTier(c), r);
                    }
                }
            }
        }
        boolean body = false;
        for (SiloLootTables.Line l : SiloLootTables.lines("assembly", SiloCeiling.EV)) {
            body |= l.ref()
                .equals("gregguns:missile_body_t1");
        }
        assertTrue(body, "the assembly floor holds a rare whole body at EV");
    }

    @Test
    void theOverworldHoldsNothingAboveEv() {
        for (String key : SiloLootTables.KEYS) {
            for (LootEntry e : SiloLootTables.category(key, SiloCeiling.EV)
                .entries()) {
                assertTrue(
                    e.tier()
                        .ordinal() <= com.gregguns.api.Tier.EV.ordinal(),
                    key + ": " + e.ref());
                assertTrue(SiloLootTables.voltageOf(e) <= SiloCeiling.EV, key + ": " + e.ref());
            }
        }
    }

    @Test
    void everyRoomKindHasATableAndEveryTableBuilds() {
        for (com.gregguns.icbm.complex.RoomKind k : com.gregguns.icbm.complex.RoomKind.values()) {
            assertTrue(
                RoomLoot.kinds()
                    .contains(k.name()),
                k + " has no loot row");
        }
        assertEquals(RoomLoot.tables(), java.util.Arrays.asList(SiloLootTables.KEYS));
        assertEquals("canteen", RoomLoot.table("CAFETERIA", "galley"), "the room picks the table");
        assertEquals("safe", RoomLoot.table("COMMANDER_OFFICE", "safe"));
        assertEquals("stockpile", RoomLoot.table("NO_SUCH_ROOM", "nonsense"));
        assertEquals("brig", RoomLoot.table("NO_SUCH_ROOM", "brig"));
        assertFalse(
            SiloLootTables.junk()
                .entries()
                .isEmpty());
    }

    @Test
    void namesAreStable() {
        assertEquals("gregguns:silo_safe_ev", SiloLootTables.categoryName("safe", 4));
        assertEquals("gregguns:silo_armory_umv", SiloLootTables.categoryName("armory", 12));
    }
}
