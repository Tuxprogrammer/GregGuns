package com.gregguns.worldgen.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class LootCategoriesTest {

    @Test
    void fifteenCategoriesWithTheSpecCeilings() {
        assertEquals(
            15,
            LootCategoryDefs.all()
                .size());
        assertEquals(
            Tier.LV,
            LootCategoryDefs.get(LootCategoryDefs.WEAPONCHEST_T1)
                .ceiling());
        assertEquals(
            Tier.MV,
            LootCategoryDefs.get(LootCategoryDefs.WEAPONCHEST_T2)
                .ceiling());
        assertEquals(
            Tier.HV,
            LootCategoryDefs.get(LootCategoryDefs.WEAPONCHEST_T3)
                .ceiling());
        assertEquals(
            Tier.LV,
            LootCategoryDefs.get(LootCategoryDefs.REINFORCEDCHEST_T1)
                .ceiling());
        assertEquals(
            Tier.MV,
            LootCategoryDefs.get(LootCategoryDefs.REINFORCEDCHEST_T2)
                .ceiling());
        assertEquals(
            Tier.HV,
            LootCategoryDefs.get(LootCategoryDefs.REINFORCEDCHEST_T3)
                .ceiling());
        assertEquals(
            Tier.MV,
            LootCategoryDefs.get(LootCategoryDefs.BARRACKS)
                .ceiling());
        // The Ross128 tables (design record 0019): EV, IV, LuV, ZPM.
        Tier[] high = { Tier.EV, Tier.IV, Tier.LUV, Tier.ZPM };
        for (int i = 0; i < high.length; i++) {
            assertEquals(
                high[i],
                LootCategoryDefs.get("gregguns:weaponchest_t" + (4 + i))
                    .ceiling());
            assertEquals(
                high[i],
                LootCategoryDefs.get("gregguns:reinforcedchest_t" + (4 + i))
                    .ceiling());
        }
    }

    @Test
    void countsMatchTheTechgunsTables() {
        assertCounts(LootCategoryDefs.WEAPONCHEST_T1, 2, 8);
        assertCounts(LootCategoryDefs.WEAPONCHEST_T2, 4, 8);
        assertCounts(LootCategoryDefs.WEAPONCHEST_T3, 5, 10);
        assertCounts(LootCategoryDefs.REINFORCEDCHEST_T1, 3, 6);
        assertCounts(LootCategoryDefs.REINFORCEDCHEST_T2, 4, 8);
        assertCounts(LootCategoryDefs.REINFORCEDCHEST_T3, 5, 9);
        assertCounts(LootCategoryDefs.BARRACKS, 1, 4);
    }

    private static void assertCounts(String name, int min, int max) {
        LootCategory c = LootCategoryDefs.get(name);
        assertEquals(min, c.minItems(), name);
        assertEquals(max, c.maxItems(), name);
    }

    @Test
    void surfaceDefaultsHoldNothingAboveMv() {
        // GGS-11-22: the categories a default structure uses are every one but the tier 3 to 7 tables (3 is the
        // admin's, 4 to 7 are Ross128's).
        for (LootCategory c : LootCategoryDefs.all()) {
            if (c.name()
                .matches(".*_t[3-7]")) {
                continue;
            }
            assertTrue(
                c.ceiling()
                    .ordinal() <= Tier.MV.ordinal(),
                c.name());
        }
    }

    @Test
    void everyLineIsAtOrBelowItsCeiling() {
        for (LootCategory c : LootCategoryDefs.all()) {
            assertEquals(new ArrayList<String>(), LootTierCheck.violations(c, null), c.name());
        }
    }

    @Test
    void thompsonAndSmgMagazineAreNotInTierOne() {
        Set<String> refs = refs(LootCategoryDefs.get(LootCategoryDefs.WEAPONCHEST_T1));
        assertFalse(refs.contains("gregguns:thompson_smg"));
        assertFalse(refs.contains("gregguns:mag_smg"));
        assertTrue(refs(LootCategoryDefs.get(LootCategoryDefs.WEAPONCHEST_T2)).contains("gregguns:thompson_smg"));
    }

    @Test
    void flamethrowerAndRocketsOnlyFromTierThreeUp() {
        for (LootCategory c : LootCategoryDefs.all()) {
            boolean hv = c.name()
                .equals(LootCategoryDefs.WEAPONCHEST_T3);
            Set<String> refs = refs(c);
            boolean allowed = c.ceiling()
                .ordinal() >= Tier.HV.ordinal();
            if (!allowed) {
                assertFalse(refs.contains("gregguns:flamethrower"), c.name());
                assertFalse(refs.contains("gregguns:rocket_launcher"), c.name());
                assertFalse(refs.contains("gregguns:ammo_rocket"), c.name());
            }
            if (hv) {
                assertTrue(refs.contains("gregguns:flamethrower"), c.name());
                assertTrue(refs.contains("gregguns:rocket_launcher"), c.name());
                assertTrue(refs.contains("gregguns:ammo_rocket"), c.name());
            }
        }
    }

    @Test
    void materialsAreGtPartsNotRawMetals() {
        for (LootCategory c : LootCategoryDefs.all()) {
            for (LootEntry e : c.entries()) {
                assertFalse(
                    e.ref()
                        .startsWith("ore:"),
                    e.ref());
                assertFalse(
                    e.ref()
                        .contains("ingot_"),
                    e.ref());
            }
        }
        assertTrue(refs(LootCategoryDefs.get(LootCategoryDefs.REINFORCEDCHEST_T2)).contains("gt:ingot.Steel"));
    }

    @Test
    void refsHaveNoDashAndNoDuplicateLines() {
        // GGS-06-06 (no '-' in names) and a duplicate line would silently double a weight.
        for (LootCategory c : LootCategoryDefs.all()) {
            Set<String> seen = new HashSet<>();
            for (LootEntry e : c.entries()) {
                assertFalse(
                    e.ref()
                        .contains("-"),
                    e.ref());
                assertTrue(seen.add(e.ref()), c.name() + " repeats " + e.ref());
            }
        }
    }

    @Test
    void aLineAboveTheCeilingIsCaught() {
        LootCategory bad = LootCategory.builder("gregguns:bad", Tier.LV, 1, 2)
            .add("gregguns:ak47", 1, 1, 1, Tier.MV)
            .build();
        assertEquals(
            1,
            LootTierCheck.violations(bad, null)
                .size());
        assertFalse(
            LootTierCheck.drawViolations(bad, 0, 50, 1L)
                .isEmpty());
    }

    @Test
    void anItemLyingAboutItsTierIsCaught() {
        LootCategory c = LootCategory.builder("gregguns:liar", Tier.MV, 1, 2)
            .add("gregguns:rocket_launcher", 1, 1, 1, Tier.LV)
            .build();
        // The declared LV is below the item's real HV: both the ceiling and the declaration are wrong.
        assertEquals(
            2,
            LootTierCheck.violations(c, ref -> Tier.HV)
                .size());
    }

    @Test
    void thousandChestsStayUnderTheCeilingAtEveryRichness() {
        for (LootCategory c : LootCategoryDefs.all()) {
            for (int r = 0; r <= LootDraw.MAX_RICHNESS; r++) {
                assertEquals(
                    new ArrayList<String>(),
                    LootTierCheck.drawViolations(c, r, 1000, 99L + r),
                    c.name() + " richness " + r);
            }
        }
    }

    @Test
    void sameSeedSameChest() {
        LootCategory c = LootCategoryDefs.get(LootCategoryDefs.WEAPONCHEST_T2);
        assertEquals(describe(c, 1, 77L), describe(c, 1, 77L));
        assertFalse(describe(c, 1, 77L).equals(describe(c, 1, 78L)));
    }

    @Test
    void pickCountReachesBothEndsInclusive() {
        Random rand = new Random(5);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(LootDraw.pickCount(2, 8, 0, rand));
        }
        assertTrue(seen.contains(2));
        assertTrue(seen.contains(8));
        assertFalse(seen.contains(1));
        assertFalse(seen.contains(9));
    }

    @Test
    void richnessRaisesCountsAndTopTierWeightOnly() {
        LootCategory c = LootCategoryDefs.get(LootCategoryDefs.WEAPONCHEST_T2);
        assertEquals(5, LootDraw.pickCount(4, 4, 1, new Random(1)));
        assertEquals(6, LootDraw.pickCount(4, 4, 2, new Random(1)));
        assertEquals(3, LootDraw.weight(1, Tier.MV, Tier.MV, 2));
        assertEquals(1, LootDraw.weight(1, Tier.LV, Tier.MV, 2));
        assertEquals(Tier.MV, LootDraw.topTier(c));
        // Richness out of range is clamped, never an error and never more than the maximum.
        assertEquals(LootDraw.pickCount(4, 4, 2, new Random(1)), LootDraw.pickCount(4, 4, 9, new Random(1)));
        assertEquals(LootDraw.pickCount(4, 4, 0, new Random(1)), LootDraw.pickCount(4, 4, -3, new Random(1)));
        // The lines drawn at richness 2 are a subset of the category's lines.
        Set<String> allowed = refs(c);
        Random rand = new Random(3);
        for (int i = 0; i < 200; i++) {
            for (LootDraw.Roll r : LootDraw.draw(c, 2, rand)) {
                assertTrue(
                    allowed.contains(
                        r.entry()
                            .ref()));
                assertTrue(
                    r.count() >= r.entry()
                        .drop()
                        .min() && r.count()
                            <= r.entry()
                                .drop()
                                .max());
            }
        }
    }

    @Test
    void richChestsHoldMoreAndBetter() {
        LootCategory c = LootCategoryDefs.get(LootCategoryDefs.WEAPONCHEST_T2);
        double[] picks = new double[3];
        double[] top = new double[3];
        for (int r = 0; r <= 2; r++) {
            Random rand = new Random(11);
            int n = 0;
            int hits = 0;
            for (int i = 0; i < 2000; i++) {
                for (LootDraw.Roll roll : LootDraw.draw(c, r, rand)) {
                    n++;
                    if (roll.entry()
                        .tier() == Tier.MV) {
                        hits++;
                    }
                }
            }
            picks[r] = n;
            top[r] = hits / (double) n;
        }
        assertTrue(picks[1] > picks[0] && picks[2] > picks[1]);
        assertTrue(top[1] > top[0] && top[2] > top[1]);
    }

    @Test
    void builderRejectsABadCategory() {
        assertThrows(IllegalArgumentException.class, () -> LootCategory.builder("minecraft:chest", Tier.LV, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> LootCategory.builder("gregguns:x", Tier.LV, 3, 2));
        assertNotNull(LootCategory.builder("gregguns:x", Tier.LV, 1, 2));
        LootCategory.Builder b = LootCategory.builder("gregguns:x", Tier.LV, 1, 2);
        assertThrows(IllegalArgumentException.class, () -> b.add("gregguns:revolver", 1, 1, 0, Tier.LV));
    }

    private static Set<String> refs(LootCategory c) {
        Set<String> out = new HashSet<>();
        for (LootEntry e : c.entries()) {
            out.add(e.ref());
        }
        return out;
    }

    private static List<String> describe(LootCategory c, int richness, long seed) {
        List<String> out = new ArrayList<>();
        Random rand = new Random(seed);
        for (int i = 0; i < 20; i++) {
            for (LootDraw.Roll r : LootDraw.draw(c, richness, rand)) {
                out.add(
                    r.entry()
                        .ref() + "x"
                        + r.count());
            }
        }
        return out;
    }
}
