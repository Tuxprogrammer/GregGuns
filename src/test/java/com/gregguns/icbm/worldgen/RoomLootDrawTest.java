package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.worldgen.loot.LootEntry;

/** The draw by room state (D317, GGI-08-18): never empty, junk and spoil only in a ruin, counts and wear by state. */
class RoomLootDrawTest {

    private static LootEntry line(String pool, String ref, int min, int max, LootDrop companion) {
        return new LootEntry(LootDrop.range(ref, min, max), 10, Tier.STEAM, pool, companion);
    }

    /** A canteen-like table: meals, a soup, drinks, a battery, a gun with its ammo, armor, a whole body. */
    private static List<LootEntry> table() {
        List<LootEntry> t = new ArrayList<>();
        t.add(line(SiloPools.MEALS, "minecraft:cooked_beef", 4, 8, null));
        t.add(line(SiloPools.MEALS, "harvestcraft:vegetablesoupItem", 2, 4, null));
        t.add(line(SiloPools.DRINKS, "harvestcraft:colasodaItem", 2, 4, null));
        t.add(line(SiloPools.POWER, "gregtech:gt.metaitem.01#32518", 1, 1, null));
        t.add(line("gun", "gregguns:ak47", 1, 1, LootDrop.range("gregguns:mag_assault_rifle", 1, 2)));
        t.add(line(SiloPools.BUNKARMOR, "gregguns:t2_combat_helmet", 1, 1, null));
        t.add(line(SiloPools.PRESERVED, "IC2:itemTinCanFilled", 8, 16, null));
        t.add(line(SiloPools.MISSILEBODY, "gregguns:missile_body_t1", 1, 1, null));
        return t;
    }

    private static List<LootEntry> junk() {
        List<LootEntry> j = new ArrayList<>();
        for (LootEntry e : SiloPools.junk()
            .lines()) {
            j.add(e);
        }
        return j;
    }

    @Test
    void everyChestHoldsARealStackAtEveryState() {
        for (RoomState s : RoomState.values()) {
            Random r = new Random(s.ordinal());
            for (int i = 0; i < 10000; i++) {
                List<RoomLootDraw.Pick> picks = RoomLootDraw.draw(table(), junk(), s, -1, r);
                assertFalse(picks.isEmpty());
                assertTrue(
                    picks.stream()
                        .anyMatch(RoomLootDraw.Pick::real),
                    s + " chest with nothing real");
                assertTrue(
                    picks.get(0)
                        .real(),
                    "the first pick is real");
            }
        }
    }

    @Test
    void realPicksStayInTheirBand() {
        for (RoomState s : RoomState.values()) {
            RoomLootDraw.Profile p = RoomLootDraw.profile(s);
            Random r = new Random(7);
            for (int i = 0; i < 10000; i++) {
                long real = RoomLootDraw.draw(table(), junk(), s, 0, r)
                    .stream()
                    .filter(k -> !k.junk() && !k.companion())
                    .count();
                assertTrue(real >= p.realMin() && real <= p.realMax(), s + ": " + real);
            }
        }
    }

    @Test
    void junkAndSpoilOnlyInARuin() {
        for (RoomState s : RoomState.values()) {
            boolean ruin = s == RoomState.MOTHBALLED || s == RoomState.GUTTED;
            Random r = new Random(11);
            boolean sawJunk = false, sawSpoil = false;
            for (int i = 0; i < 10000; i++) {
                for (RoomLootDraw.Pick k : RoomLootDraw.draw(table(), junk(), s, 0, r)) {
                    sawJunk |= k.junk();
                    sawSpoil |= k.spoiled() && !SiloPools.isBody(
                        k.entry()
                            .ref());
                }
            }
            assertEquals(ruin, sawJunk, s + " junk");
            assertEquals(ruin, sawSpoil, s + " spoil");
        }
    }

    @Test
    void spoilFollowsThePoolAndNeverTouchesPreservedFoodOrCompanions() {
        Random r = new Random(3);
        for (int i = 0; i < 20000; i++) {
            List<RoomLootDraw.Pick> picks = RoomLootDraw.draw(table(), junk(), RoomState.GUTTED, 2, r);
            assertFalse(
                picks.get(0)
                    .spoiled()
                    && !SiloPools.isBody(
                        picks.get(0)
                            .entry()
                            .ref()),
                "the first pick never spoils");
            for (RoomLootDraw.Pick k : picks) {
                String from = k.entry()
                    .ref();
                if (!k.spoiled()) continue;
                assertFalse(k.companion(), "a companion never spoils");
                assertFalse(from.startsWith("IC2:itemTinCan"), "canned food never spoils");
                assertFalse(from.startsWith("gregguns:t2_combat"), "armor wears, it does not spoil");
                if (from.contains("soup")) assertEquals(SiloPools.BOWL, k.ref());
                else if (from.equals("minecraft:cooked_beef")) assertEquals(SiloPools.ROTTEN, k.ref());
                else if (from.contains("soda")) assertEquals(SiloPools.BOTTLE, k.ref());
                else if (from.endsWith("#32518")) assertEquals(SiloPools.DEAD_BATTERY, k.ref());
            }
        }
    }

    @Test
    void aWholeBodyIsIntactUnlessTheRoomIsGutted() {
        List<LootEntry> body = Collections
            .singletonList(line(SiloPools.MISSILEBODY, "gregguns:missile_body_t1", 1, 1, null));
        for (RoomState s : RoomState.values()) {
            for (RoomLootDraw.Pick k : RoomLootDraw.draw(body, junk(), s, 0, new Random(5))) {
                if (k.junk()) continue;
                assertTrue(k.real(), "a body is a real find, salvaged or not");
                assertEquals(
                    s == RoomState.GUTTED ? "gregguns:missile_body_t1#1" : "gregguns:missile_body_t1",
                    k.ref(),
                    s.name());
            }
        }
    }

    @Test
    void countsScaleWithTheState() {
        double[] mean = new double[RoomState.values().length];
        List<LootEntry> beef = Collections.singletonList(line(SiloPools.PRESERVED, "IC2:itemTinCanFilled", 8, 8, null));
        for (RoomState s : RoomState.values()) {
            for (RoomLootDraw.Pick k : RoomLootDraw.draw(beef, null, s, 0, new Random(1))) {
                mean[s.ordinal()] = k.count();
            }
        }
        assertEquals(8, mean[RoomState.STAFFED.ordinal()]);
        assertEquals(8, mean[RoomState.IN_SERVICE.ordinal()]);
        assertEquals(6, mean[RoomState.MOTHBALLED.ordinal()]);
        assertEquals(4, mean[RoomState.GUTTED.ordinal()]);
        assertEquals(1, RoomLootDraw.scale(1, 0.5));
    }

    @Test
    void wearOnlyInARuinAndNeverOnACompanion() {
        for (RoomState s : RoomState.values()) {
            Random r = new Random(9);
            boolean worn = false;
            for (int i = 0; i < 2000; i++) {
                for (RoomLootDraw.Pick k : RoomLootDraw.draw(table(), junk(), s, 0, r)) {
                    if (k.companion() || k.junk() || k.spoiled()) assertEquals(0.0, k.wear());
                    worn |= k.wear() > 0;
                    assertTrue(k.wear() >= 0 && k.wear() < 1);
                }
            }
            assertEquals(s == RoomState.MOTHBALLED || s == RoomState.GUTTED, worn, s.name());
        }
    }

    @Test
    void aGunBringsItsAmmo() {
        List<LootEntry> gun = Collections
            .singletonList(line("gun", "gregguns:ak47", 1, 1, LootDrop.range("gregguns:mag_assault_rifle", 1, 2)));
        List<RoomLootDraw.Pick> picks = RoomLootDraw.draw(gun, null, RoomState.STAFFED, 0, new Random(2));
        for (int i = 0; i < picks.size(); i += 2) {
            assertEquals(
                "gregguns:ak47",
                picks.get(i)
                    .ref());
            assertEquals(
                "gregguns:mag_assault_rifle",
                picks.get(i + 1)
                    .ref());
            assertTrue(
                picks.get(i + 1)
                    .companion());
        }
    }

    @Test
    void anEmptyTableFallsBackToTorches() {
        List<RoomLootDraw.Pick> picks = RoomLootDraw.draw(new ArrayList<>(), null, RoomState.GUTTED, -5, new Random());
        assertEquals(1, picks.size());
        assertEquals(
            "minecraft:torch",
            picks.get(0)
                .ref());
        assertTrue(
            picks.get(0)
                .real());
    }

    @Test
    void sameSeedSamePicks() {
        for (RoomState s : RoomState.values()) {
            List<RoomLootDraw.Pick> a = RoomLootDraw.draw(table(), junk(), s, 0, new Random(42));
            List<RoomLootDraw.Pick> b = RoomLootDraw.draw(table(), junk(), s, 0, new Random(42));
            assertEquals(a.size(), b.size());
            for (int i = 0; i < a.size(); i++) {
                assertEquals(
                    a.get(i)
                        .ref(),
                    b.get(i)
                        .ref());
                assertEquals(
                    a.get(i)
                        .count(),
                    b.get(i)
                        .count());
            }
        }
    }
}
