package com.gregguns.worldgen.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.part.PartCatalog;

/** The GregGuns pools: guns rare, ammo common, every gun with its own ammo (GGS-11-26). */
class ArsenalTest {

    private static final String G = "gregguns:";

    @Test
    void everyGunBringsItsOwnAmmoOrIsAnEnergyGun() {
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            for (LootEntry e : Arsenal.guns(t)
                .lines()) {
                GunDefinition def = GunDefinitions.byName(
                    e.ref()
                        .substring(G.length()));
                assertNotNull(def, e.ref());
                assertEquals(def.tier(), e.tier(), e.ref() + " declares its definition's tier");
                String ammo = Arsenal.ammoItem(def);
                if (ammo == null) {
                    assertNull(e.companion(), e.ref() + " is an energy gun");
                    continue;
                }
                assertNotNull(e.companion(), e.ref());
                String want = G + ammo + (ammo.startsWith("tank_") ? Arsenal.FULL : "");
                assertEquals(
                    want,
                    e.companion()
                        .ref(),
                    e.ref());
                assertTrue(
                    Arsenal.ammoTier(ammo)
                        .ordinal()
                        <= Arsenal.ceiling(t)
                            .ordinal(),
                    e.ref() + " ammo above table " + t);
            }
        }
    }

    @Test
    void theAmmoPoolHoldsExactlyTheAmmoOfTheTablesGuns() {
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            Set<String> want = new HashSet<>();
            for (LootEntry e : Arsenal.guns(t)
                .lines()) {
                if (e.companion() != null) {
                    want.add(
                        e.companion()
                            .ref());
                }
            }
            Set<String> got = new HashSet<>();
            for (LootEntry e : Arsenal.ammo(t)
                .lines()) {
                got.add(e.ref());
                String bare = e.ref()
                    .substring(G.length())
                    .replace(Arsenal.FULL, "");
                assertEquals(Arsenal.ammoTier(bare), e.tier(), e.ref() + " declares its real tier");
            }
            assertEquals(want, got, "table " + t);
        }
    }

    @Test
    void gunsAndPartsStayAtOrBelowTheirTable() {
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            Tier ceiling = Arsenal.ceiling(t);
            for (LootPools.Pool p : new LootPools.Pool[] { Arsenal.guns(t), Arsenal.ammo(t), Arsenal.grenades(t),
                Arsenal.parts(t) }) {
                for (LootEntry e : p.lines()) {
                    assertTrue(
                        e.tier()
                            .ordinal() <= ceiling.ordinal(),
                        e.ref() + " in table " + t);
                }
            }
            for (LootEntry e : Arsenal.parts(t)
                .lines()) {
                assertEquals(
                    PartCatalog.tierOf(
                        e.ref()
                            .substring(G.length())),
                    e.tier(),
                    e.ref());
            }
        }
        assertEquals(Tier.LV, Arsenal.ceiling(1));
        assertEquals(Tier.ZPM, Arsenal.ceiling(7));
        assertEquals(Tier.ZPM, Arsenal.ceiling(9));
    }

    @Test
    void noAkimboPowerToolOrGoldenGunIsLoot() {
        for (String name : Arsenal.allGuns()) {
            assertFalse(name.startsWith("akimbo_"), name);
            assertFalse(name.startsWith("power_hammer"), name);
            assertFalse(name.equals("chainsaw") || name.equals("golden_gun"), name);
        }
        assertEquals("revolver", Arsenal.gunName("gregguns:revolver"));
        assertNull(Arsenal.gunName("gregguns:mag_smg"));
        assertNull(Arsenal.gunName("minecraft:bow"));
    }

    /** Owner, 2026-10-30: guns rare, ammo common. Checked at every table, kind and richness. */
    @Test
    void gunsAreRareAndAmmoIsCommon() {
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            for (int r = 0; r <= LootDraw.MAX_RICHNESS; r++) {
                LootOdds weapon = new LootOdds(LootCategoryDefs.get(LootCategoryDefs.weaponChest(t)), r);
                String w = "weapon " + t + " r" + r;
                assertTrue(weapon.chestHasGun() < 0.4, w + " gun in " + weapon.chestHasGun());
                assertTrue(weapon.chestHasAmmo() > 0.9, w + " ammo in " + weapon.chestHasAmmo());
                assertTrue(weapon.ammo() >= 6 * weapon.guns(), w + " ammo " + weapon.ammo() + " guns " + weapon.guns());
                LootOdds supply = new LootOdds(LootCategoryDefs.get(LootCategoryDefs.supplyChest(t)), r);
                String s = "supply " + t + " r" + r;
                assertTrue(supply.chestHasGun() < 0.2, s + " gun in " + supply.chestHasGun());
                assertTrue(supply.ammo() >= 5 * supply.guns(), s + " ammo " + supply.ammo() + " guns " + supply.guns());
                for (String other : new String[] { LootCategoryDefs.reinforcedChest(t), LootCategoryDefs.BARRACKS }) {
                    assertEquals(0.0, new LootOdds(LootCategoryDefs.get(other), r).guns(), other);
                }
            }
        }
    }

    @Test
    void aDrawnGunComesWithItsAmmoInTheSameChest() {
        LootCategory c = LootCategoryDefs.get(LootCategoryDefs.weaponChest(2));
        Random rand = new Random(42);
        int guns = 0;
        for (int i = 0; i < 2000; i++) {
            List<LootDraw.Roll> rolls = LootDraw.draw(c, 1, rand);
            for (int k = 0; k < rolls.size(); k++) {
                LootDraw.Roll roll = rolls.get(k);
                if (!Arsenal.GUN.equals(
                    roll.entry()
                        .pool())
                    || roll.companion()) {
                    continue;
                }
                guns++;
                LootDraw.Roll next = rolls.get(k + 1);
                assertTrue(next.companion());
                assertEquals(
                    roll.entry()
                        .companion()
                        .ref(),
                    next.ref());
            }
        }
        assertTrue(guns > 100, "guns drawn " + guns);
    }

    @Test
    void richnessNeverMakesGunsCommoner() {
        // A gun line keeps its base weight; everything else at the top tier gains (GGS-11-23, GGS-11-26).
        LootCategory c = LootCategoryDefs.get(LootCategoryDefs.weaponChest(1));
        double[] perPick = new double[3];
        for (int r = 0; r <= 2; r++) {
            perPick[r] = new LootOdds(c, r).perPick(e -> Arsenal.GUN.equals(e.pool()));
        }
        assertTrue(perPick[1] <= perPick[0] && perPick[2] <= perPick[1]);
        LootEntry revolver = null;
        for (LootEntry e : c.entries()) {
            if (e.ref()
                .equals("gregguns:revolver")) {
                revolver = e;
            }
        }
        assertNotNull(revolver);
        assertEquals(revolver.weight(), LootDraw.weight(revolver, Tier.LV, 2));
    }

    @Test
    void siloArmsAreGregGunsOnlyWithSmallWeights() {
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            List<LootEntry> arms = LootCategoryDefs.arms(t);
            int guns = 0;
            int ammo = 0;
            List<String> refs = new ArrayList<>();
            for (LootEntry e : arms) {
                refs.add(e.ref());
                assertTrue(
                    e.ref()
                        .startsWith(G),
                    e.ref());
                assertNull(e.companion());
                assertTrue(e.weight() <= 100, e.ref() + " " + e.weight());
                if (Arsenal.GUN.equals(e.pool()) && Arsenal.ammoItem(
                    GunDefinitions.byName(
                        e.ref()
                            .substring(G.length())))
                    != null) {
                    // Energy guns have no ammo to balance against.
                    guns += e.weight();
                } else if (Arsenal.AMMO.equals(e.pool())) {
                    ammo += e.weight();
                }
            }
            assertTrue(guns > 0, "table " + t);
            assertTrue(ammo >= 2 * guns, "table " + t + " ammo " + ammo + " guns " + guns);
            assertEquals(refs.size(), new HashSet<>(refs).size(), "table " + t + " repeats a line");
        }
    }
}
