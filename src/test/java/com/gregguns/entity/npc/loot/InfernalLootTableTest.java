package com.gregguns.entity.npc.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DirectDamageSource;
import com.gregguns.combat.RangedDamageSource;

class InfernalLootTableTest {

    private static Map<String, Tier> tiers() {
        Map<String, Tier> m = new HashMap<>();
        for (InfernalLootTable.Entry e : InfernalLootTable.entries()) {
            m.put(
                e.drop()
                    .ref(),
                e.tier());
        }
        return m;
    }

    @Test
    void rollCountFollowsModifiers() {
        assertEquals(
            1,
            InfernalLootTable.roll(new Random(1), 2, Tier.HV)
                .size());
        assertEquals(
            2,
            InfernalLootTable.roll(new Random(1), 10, Tier.HV)
                .size());
        assertEquals(
            3,
            InfernalLootTable.roll(new Random(1), 15, Tier.HV)
                .size());
    }

    @Test
    void neverAboveBandOrNpcCeiling() {
        Map<String, Tier> tiers = tiers();
        Random rand = new Random(42);
        for (int i = 0; i < 5000; i++) {
            for (LootDrop d : InfernalLootTable.roll(rand, 3, Tier.HV)) {
                assertTrue(
                    tiers.get(d.ref())
                        .ordinal() <= Tier.LV.ordinal(),
                    "elite " + d.ref());
            }
            for (LootDrop d : InfernalLootTable.roll(rand, 8, Tier.HV)) {
                assertTrue(
                    tiers.get(d.ref())
                        .ordinal() <= Tier.MV.ordinal(),
                    "ultra " + d.ref());
            }
            for (LootDrop d : InfernalLootTable.roll(rand, 12, Tier.STEAM)) {
                assertTrue(
                    tiers.get(d.ref())
                        .ordinal() <= Tier.STEAM.ordinal(),
                    "steam npc " + d.ref());
            }
        }
    }

    @Test
    void onlyInfernalBandRollsHv() {
        Map<String, Tier> tiers = tiers();
        Random rand = new Random(9);
        boolean ultraHv = false;
        boolean infernalHv = false;
        for (int i = 0; i < 5000; i++) {
            for (LootDrop d : InfernalLootTable.roll(rand, 8, Tier.HV)) {
                ultraHv |= tiers.get(d.ref()) == Tier.HV;
            }
            for (LootDrop d : InfernalLootTable.roll(rand, 12, Tier.HV)) {
                infernalHv |= tiers.get(d.ref()) == Tier.HV;
            }
        }
        assertFalse(ultraHv);
        assertTrue(infernalHv);
    }

    @Test
    void sameSeedSameDrops() {
        List<LootDrop> a = InfernalLootTable.roll(new Random(5), 12, Tier.HV);
        List<LootDrop> b = InfernalLootTable.roll(new Random(5), 12, Tier.HV);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(
                a.get(i)
                    .ref(),
                b.get(i)
                    .ref());
        }
    }

    @Test
    void registryNamesHaveNoDash() {
        for (InfernalLootTable.Entry e : InfernalLootTable.entries()) {
            assertFalse(
                e.drop()
                    .ref()
                    .contains("-"));
        }
    }

    @Test
    void optOutOnlyForSpawnersWhenNotAllowed() {
        assertTrue(InfernalOptOut.shouldOptOut(true, false));
        assertFalse(InfernalOptOut.shouldOptOut(true, true));
        assertFalse(InfernalOptOut.shouldOptOut(false, false));
        assertFalse(InfernalOptOut.MARKER.isEmpty());
    }

    /** GGS-10-06: ranged sources are indirect, melee ones are not. */
    @Test
    void sourceHierarchyMatchesInfernalContactRules() {
        assertTrue(EntityDamageSourceIndirect.class.isAssignableFrom(RangedDamageSource.class));
        assertTrue(EntityDamageSource.class.isAssignableFrom(DirectDamageSource.class));
        assertFalse(EntityDamageSourceIndirect.class.isAssignableFrom(DirectDamageSource.class));
        assertTrue(DamageSource.class.isAssignableFrom(RangedDamageSource.class));
    }
}
