package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.loot.EquipmentProfile;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;

/** D312: the carried gun's own drops (rare gun, ammo, tank, energy salvage) and their NEI rows. */
class NpcGunDropsTest {

    /** Every gun some NPC profile carries, by registry name. */
    private static Map<String, GunDefinition> carriedGuns() {
        Map<String, GunDefinition> out = new TreeMap<>();
        for (NpcProfile p : NpcProfiles.all()
            .values()) {
            EquipmentProfile eq = p.equipment();
            if (eq == null) {
                continue;
            }
            for (EquipmentProfile.Weapon w : eq.weapons()) {
                GunDefinition def = GunDefinitions.byName(w.gun());
                if (def != null) {
                    out.put(w.gun(), def);
                }
            }
        }
        return out;
    }

    private static NpcDrops.GunDrop rule(String gun) {
        GunDefinition def = GunDefinitions.byName(gun);
        assertNotNull(def, gun);
        return NpcDrops.gunDrop(gun, def.stats());
    }

    /** {min, max} over many seeds of one gun's loose or tank drop. */
    private static int[] observedRange(String gun) {
        NpcDrops.GunDrop r = rule(gun);
        Random rand = new Random(42);
        int lo = Integer.MAX_VALUE, hi = 0;
        for (int i = 0; i < 2000; i++) {
            List<GunSalvage.Pick> picks = r.roll(rand);
            assertEquals(1, picks.size(), gun);
            lo = Math.min(
                lo,
                picks.get(0)
                    .count());
            hi = Math.max(
                hi,
                picks.get(0)
                    .count());
        }
        return new int[] { lo, hi };
    }

    @Test
    void everyCarriedGunHasADropRule() {
        Map<String, GunDefinition> guns = carriedGuns();
        assertTrue(guns.size() >= 15, "carried guns " + guns.keySet());
        for (Map.Entry<String, GunDefinition> g : guns.entrySet()) {
            NpcDrops.GunDrop r = NpcDrops.gunDrop(
                g.getKey(),
                g.getValue()
                    .stats());
            assertNotNull(r, g.getKey());
            if (g.getValue()
                .stats()
                .isEnergy()) {
                assertEquals(NpcDrops.GunDropKind.SALVAGE, r.kind(), g.getKey());
                assertFalse(
                    r.salvage()
                        .isEmpty(),
                    g.getKey());
            } else {
                assertTrue(r.minItems() >= 1, g.getKey());
                assertTrue(r.maxItems() >= r.minItems(), g.getKey());
            }
        }
    }

    /** The table in mob-drops-nei.md GGS-10-40, pinned. */
    @Test
    void ammoTableMatchesTheSheet() {
        Object[][] table = { { "pistol_9mm", "gregguns:ammo_9mm_bullet", 1, 2 },
            { "thompson_smg", "gregguns:ammo_9mm_bullet", 1, 1 }, { "revolver", "gregguns:ammo_9mm_bullet", 1, 1 },
            { "ak47", "gregguns:ammo_rifle_bullet", 1, 2 }, { "m4", "gregguns:ammo_rifle_bullet", 1, 2 },
            { "lmg", "gregguns:ammo_rifle_bullet", 2, 6 }, { "minigun", "gregguns:ammo_rifle_bullet", 4, 12 },
            { "bolt_action_rifle", "gregguns:ammo_rifle_bullet", 1, 1 }, { "combat_shotgun", null, 2, 6 },
            { "sawed_off", null, 1, 1 }, { "handcannon", null, 1, 1 }, { "rocket_launcher", null, 1, 1 },
            { "grim_reaper", null, 1, 3 }, { "nether_blaster", null, 1, 1 },
            { "flamethrower", "gregguns:tank_fuel~full", 1, 1 }, { "chainsaw", "gregguns:tank_fuel~full", 1, 1 },
            { "bio_gun", "gregguns:tank_bio~full", 1, 1 } };
        for (Object[] row : table) {
            String gun = (String) row[0];
            if (GunDefinitions.byName(gun) == null) {
                continue;
            }
            NpcDrops.GunDrop r = rule(gun);
            if (row[1] != null) {
                assertEquals(row[1], r.ref(), gun);
            }
            assertEquals(row[2], r.minItems(), gun + " min");
            assertEquals(row[3], r.maxItems(), gun + " max");
            int[] seen = observedRange(gun);
            assertEquals(row[2], seen[0], gun + " observed min");
            assertEquals(row[3], seen[1], gun + " observed max");
        }
    }

    @Test
    void rareGunChanceIsTwoPercentPlusOnePerLooting() {
        assertEquals(0.02, NpcDrops.rareGunChance(0), 1e-12);
        assertEquals(0.03, NpcDrops.rareGunChance(1), 1e-12);
        assertEquals(0.04, NpcDrops.rareGunChance(2), 1e-12);
        assertEquals(0.05, NpcDrops.rareGunChance(3), 1e-12);
    }

    @Test
    void energyRiflesDropPartsOfTheirRecipeAndNoBattery() {
        for (String gun : new String[] { "laser_rifle", "tesla_rifle", "blaster_rifle" }) {
            NpcDrops.GunDrop r = rule(gun);
            assertEquals(NpcDrops.GunDropKind.SALVAGE, r.kind(), gun);
            Set<String> refs = new HashSet<>();
            for (GunSalvage.Entry e : r.salvage()) {
                refs.add(e.ref());
                assertFalse(
                    e.ref()
                        .startsWith("gt:circuit"),
                    gun + " " + e);
                assertFalse(
                    e.ref()
                        .toLowerCase()
                        .contains("battery")
                        || e.ref()
                            .toLowerCase()
                            .contains("lapotron"),
                    gun + " " + e);
                assertTrue(e.min() >= 1 && e.max() <= GunSalvage.MAX_PART_COUNT, gun + " " + e);
            }
            // The gun's own screws and plates, and a receiver, are always in the pool.
            assertTrue(
                refs.stream()
                    .anyMatch(s -> s.startsWith("gt:screw.")),
                gun + " " + refs);
            assertTrue(
                refs.stream()
                    .anyMatch(s -> s.startsWith("gt:plate.")),
                gun + " " + refs);
            assertTrue(
                refs.stream()
                    .anyMatch(s -> s.startsWith("gregguns:part_receiver_")),
                gun + " " + refs);
        }
        List<GunSalvage.Entry> laser = rule("laser_rifle").salvage();
        Set<String> refs = new HashSet<>();
        laser.forEach(e -> refs.add(e.ref()));
        // Sub-parts one level down: the Hardened Receiver's stainless steel plates.
        assertTrue(refs.contains("gt:plate.StainlessSteel"), refs.toString());
        assertTrue(refs.contains("gt:screw.Titanium"), refs.toString());
    }

    @Test
    void salvageMakesTwoPicksFromThePool() {
        List<GunSalvage.Entry> pool = rule("tesla_rifle").salvage();
        Set<String> refs = new HashSet<>();
        pool.forEach(e -> refs.add(e.ref()));
        Random rand = new Random(3);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            List<GunSalvage.Pick> picks = GunSalvage.roll(pool, rand);
            assertEquals(GunSalvage.DRAWS, picks.size());
            for (GunSalvage.Pick p : picks) {
                assertTrue(refs.contains(p.ref()), p.ref());
                assertTrue(p.count() >= 1 && p.count() <= GunSalvage.MAX_PART_COUNT);
                seen.add(p.ref());
            }
        }
        assertEquals(refs, seen, "every pool entry is reachable");
    }

    @Test
    void dropsAreDeterministicForASeed() {
        for (String gun : new String[] { "lmg", "laser_rifle", "flamethrower" }) {
            NpcDrops.GunDrop r = rule(gun);
            List<String> a = new ArrayList<>(), b = new ArrayList<>();
            Random ra = new Random(99), rb = new Random(99);
            for (int i = 0; i < 50; i++) {
                r.roll(ra)
                    .forEach(p -> a.add(p.ref() + "x" + p.count()));
                r.roll(rb)
                    .forEach(p -> b.add(p.ref() + "x" + p.count()));
            }
            assertEquals(a, b, gun);
        }
    }

    @Test
    void expectedItemsAveragesEveryRoundCount() {
        NpcDrops.GunDrop lmg = rule("lmg");
        // 25 to 75 rounds, 12 a bullet: 2 for 25-35, 3 for 36-47, 4 for 48-59, 5 for 60-71, 6 for 72-75.
        double want = (11 * 2 + 12 * 3 + 12 * 4 + 12 * 5 + 4 * 6) / 51.0;
        assertEquals(want, lmg.expectedItems(), 1e-9);
        assertEquals(1.0, rule("flamethrower").expectedItems(), 1e-9);
    }

    @Test
    void neiRowsReplaceTheEquipmentGunRow() {
        Map<String, Double> guns = new LinkedHashMap<>();
        guns.put("ak47", 0.5);
        guns.put("laser_rifle", 1.0);
        List<NpcDropInfo.Row> rows = NpcDropInfo.transform(guns, true, NpcGunDropsTest::rule);
        Map<String, NpcDropInfo.Row> byRef = new LinkedHashMap<>();
        for (NpcDropInfo.Row r : rows) {
            byRef.put(r.type() + " " + r.ref(), r);
        }
        assertEquals(
            100,
            byRef.get("RARE gregguns:ak47")
                .chance(),
            "2% of half");
        assertEquals(
            200,
            byRef.get("RARE gregguns:laser_rifle")
                .chance(),
            "2% of all");
        NpcDropInfo.Row ammo = byRef.get("NORMAL gregguns:ammo_rifle_bullet");
        assertNotNull(
            ammo,
            byRef.keySet()
                .toString());
        assertEquals(Math.round(0.5 * rule("ak47").expectedItems() * 10000), ammo.chance());
        assertEquals(1, ammo.min());
        assertEquals(2, ammo.max());
        // The salvage rows add up to two picks' worth of items per death.
        double salvage = 0;
        for (NpcDropInfo.Row r : rows) {
            if (r.type() == NpcDropInfo.Type.NORMAL && !r.ref()
                .startsWith("gregguns:ammo")) {
                salvage += r.chance();
                assertEquals("laser_rifle", r.gun());
            }
        }
        double want = 0;
        List<GunSalvage.Entry> pool = rule("laser_rifle").salvage();
        for (GunSalvage.Entry e : pool) {
            want += GunSalvage.expectedCount(pool, e) * 10000;
        }
        assertEquals(want, salvage, pool.size());
    }

    @Test
    void neiRowsHaveNoRareGunWithoutTheRareDrop() {
        Map<String, Double> guns = new LinkedHashMap<>();
        guns.put("chainsaw", 1.0);
        List<NpcDropInfo.Row> rows = NpcDropInfo.transform(guns, false, NpcGunDropsTest::rule);
        for (NpcDropInfo.Row r : rows) {
            assertEquals(NpcDropInfo.Type.NORMAL, r.type(), r.toString());
        }
        assertEquals(1, rows.size(), rows.toString());
        assertEquals(
            "gregguns:tank_fuel~full",
            rows.get(0)
                .ref());
        assertEquals(
            10000,
            rows.get(0)
                .chance());
    }

    @Test
    void neiRowsMergeTheSameAmmo() {
        Map<String, Double> guns = new LinkedHashMap<>();
        guns.put("ak47", 0.5);
        guns.put("m4", 0.5);
        List<NpcDropInfo.Row> rows = NpcDropInfo.transform(guns, false, NpcGunDropsTest::rule);
        assertEquals(1, rows.size(), rows.toString());
        assertEquals(
            Math.round(0.5 * rule("ak47").expectedItems() * 10000)
                + Math.round(0.5 * rule("m4").expectedItems() * 10000),
            rows.get(0)
                .chance());
    }

    @Test
    void carryChancesFollowTheWeightsAndMinimumDanger() {
        EquipmentProfile eq = NpcProfiles.get("ZombieSoldier")
            .equipment();
        Map<String, Double> p = NpcDropInfo.carryChances(eq, 4, g -> true);
        double sum = 0;
        for (double v : p.values()) {
            sum += v;
        }
        assertEquals(1.0, sum, 1e-9, p.toString());
        // A weapon that needs danger 2 is only in the pool for two of the four levels.
        for (EquipmentProfile.Weapon w : eq.weapons()) {
            if (w.minDifficulty() >= 4) {
                assertFalse(p.containsKey(w.gun()), w.gun());
            }
        }
        // Only registered weapons count.
        Map<String, Double> none = NpcDropInfo.carryChances(eq, 4, g -> false);
        assertTrue(none.isEmpty());
    }
}
