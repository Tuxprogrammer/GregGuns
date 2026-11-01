package com.gregguns.worldgen.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;

/** The pool mixer and the Roguelike-style pack mix it builds (GGS-11-26). */
class LootMixTest {

    private static LootPools.Pool pool(String name, LootEntry... lines) {
        return new LootPools.Pool(name, Arrays.asList(lines));
    }

    private static LootEntry line(String ref, int weight, Tier tier) {
        return new LootEntry(LootDrop.range(ref, 1, 2), weight, tier, "", null);
    }

    private static LootEntry find(LootCategory c, String ref) {
        for (LootEntry e : c.entries()) {
            if (e.ref()
                .equals(ref)) {
                return e;
            }
        }
        return null;
    }

    @Test
    void sharesSplitOverAPoolByItsWeights() {
        LootCategory c = LootMix.of("gregguns:t", Tier.LV, 1, 2)
            .pool(30, pool("a", line("minecraft:a", 1, Tier.STEAM), line("minecraft:b", 2, Tier.STEAM)))
            .pool(10, pool("c", line("minecraft:c", 5, Tier.LV)))
            .build();
        assertEquals(1000, find(c, "minecraft:a").weight());
        assertEquals(2000, find(c, "minecraft:b").weight());
        assertEquals(1000, find(c, "minecraft:c").weight());
        assertEquals("a", find(c, "minecraft:b").pool());
    }

    @Test
    void linesAboveTheCeilingDropAndTheShareGoesToTheRest() {
        LootCategory c = LootMix.of("gregguns:t", Tier.LV, 1, 2)
            .pool(10, pool("a", line("minecraft:low", 1, Tier.LV), line("minecraft:high", 9, Tier.HV)))
            .pool(10, pool("b", line("minecraft:only_hv", 1, Tier.HV)))
            .build();
        assertNull(find(c, "minecraft:high"));
        assertNull(find(c, "minecraft:only_hv"));
        assertEquals(10 * LootMix.SCALE, find(c, "minecraft:low").weight());
        assertEquals(
            1,
            c.entries()
                .size());
    }

    @Test
    void aReferenceInTwoPoolsBecomesOneLine() {
        LootEntry gun = new LootEntry(
            LootDrop.range("gregguns:revolver", 1, 1),
            1,
            Tier.LV,
            "gun",
            LootDrop.range("gregguns:ammo_9mm_bullet", 4, 8));
        LootCategory c = LootMix.of("gregguns:t", Tier.MV, 1, 2)
            .pool(10, pool("junk", new LootEntry(LootDrop.range("minecraft:arrow", 3, 9), 1, Tier.STEAM, "junk", null)))
            .pool(
                10,
                pool(
                    "melee",
                    new LootEntry(LootDrop.range("minecraft:arrow", 4, 16), 1, Tier.STEAM, "melee", null),
                    gun))
            .build();
        LootEntry arrow = find(c, "minecraft:arrow");
        assertEquals(1000 + 500, arrow.weight());
        assertEquals(
            3,
            arrow.drop()
                .min());
        assertEquals(
            16,
            arrow.drop()
                .max());
        assertEquals("junk", arrow.pool());
        assertEquals(
            "gregguns:ammo_9mm_bullet",
            find(c, "gregguns:revolver").companion()
                .ref());
        assertEquals(
            2,
            c.entries()
                .size());
    }

    @Test
    void aPoolWithNothingUnderTheCeilingAddsNothing() {
        LootMix m = LootMix.of("gregguns:t", Tier.LV, 1, 2)
            .pool(10, pool("hv", line("minecraft:x", 1, Tier.HV)));
        assertTrue(
            m.shares()
                .isEmpty());
        assertThrows(IllegalArgumentException.class, () -> m.pool(0, pool("a", line("minecraft:y", 1, Tier.LV))));
    }

    @Test
    void supplyAndWeaponChestsMixEveryPackPoolAndTheGregGunsPools() {
        // GGS-11-26: a supply chest is the Roguelike top-floor mix plus GregGuns gear; a weapon chest leans weapons
        // but is not GregGuns-only.
        List<String> supplyPools = Arrays.asList(
            LootPools.FOOD,
            LootPools.JUNK,
            LootPools.ORE,
            LootPools.TOOLS,
            LootPools.POTIONS,
            LootPools.MELEE,
            LootPools.ARMOR,
            LootPools.ENCHANTING,
            Arsenal.AMMO,
            Arsenal.GRENADE,
            Arsenal.PART,
            Arsenal.GUN);
        List<String> weaponPools = Arrays.asList(
            Arsenal.AMMO,
            Arsenal.GUN,
            Arsenal.GRENADE,
            Arsenal.PART,
            LootPools.MELEE,
            LootPools.ARMOR,
            LootPools.POTIONS,
            LootPools.FOOD,
            LootPools.JUNK,
            LootPools.ORE);
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            assertEquals(
                new HashSet<>(supplyPools),
                LootCategoryDefs.shares(LootCategoryDefs.supplyChest(t))
                    .keySet(),
                "supply " + t);
            assertEquals(
                new HashSet<>(weaponPools),
                LootCategoryDefs.shares(LootCategoryDefs.weaponChest(t))
                    .keySet(),
                "weapon " + t);
            Set<String> mods = new HashSet<>();
            for (LootEntry e : LootCategoryDefs.get(LootCategoryDefs.supplyChest(t))
                .entries()) {
                mods.add(
                    e.ref()
                        .substring(
                            0,
                            e.ref()
                                .indexOf(':')));
            }
            for (String mod : new String[] { "minecraft", "gt", "harvestcraft", "gregguns", "TConstruct",
                "Forestry" }) {
                assertTrue(mods.contains(mod), "supply " + t + " has no " + mod + " line");
            }
        }
    }

    @Test
    void supplyChestsAreMostlyPackLootAndWeaponChestsMostlyArms() {
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            for (int r = 0; r <= LootDraw.MAX_RICHNESS; r++) {
                LootOdds supply = new LootOdds(LootCategoryDefs.get(LootCategoryDefs.supplyChest(t)), r);
                assertTrue(supply.gregGunsPerPick() < 0.45, "supply " + t + " r" + r + " " + supply.gregGunsPerPick());
                assertTrue(supply.chestHas(e -> LootPools.FOOD.equals(e.pool())) > 0.6, "supply " + t + " food");
                LootOdds weapon = new LootOdds(LootCategoryDefs.get(LootCategoryDefs.weaponChest(t)), r);
                double gg = weapon.gregGunsPerPick();
                assertTrue(gg > 0.4 && gg < 0.8, "weapon " + t + " r" + r + " " + gg);
            }
        }
    }

    @Test
    void sharesOfEachKindAddToAHundred() {
        for (String name : LootCategories.names()) {
            Map<String, Integer> s = LootCategoryDefs.shares(name);
            int sum = 0;
            List<String> pools = new ArrayList<>(s.keySet());
            for (int v : s.values()) {
                sum += v;
            }
            assertEquals(100, sum, name + " " + pools);
        }
    }
}
