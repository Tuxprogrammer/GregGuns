package com.gregguns.worldgen.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;

/**
 * The plain rules behind {@link ObtainTier} (D363): an item is judged by its whole crafting chain, never by the voltage
 * of a recipe that outputs it, and anything nobody vouched for is never loot.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Loot tier cap)
class ObtainTierRulesTest {

    private static final int EV = 4;

    private static boolean atDefaultCap(int tier) {
        return ObtainTierRules.allowed(tier, Integer.MAX_VALUE - 1, EV);
    }

    @Test
    void theOwnersFindsAreNotLootAtEv() {
        // The white dwarf nugget a low-voltage Fluid Solidifier casts, and the ZPM circuit, by both its ore names.
        int nugget = ObtainTierRules.materialItem("nugget", "WhiteDwarfMatter");
        assertEquals(12, nugget);
        assertFalse(atDefaultCap(nugget));
        assertEquals(7, ObtainTierRules.circuit("circuitUltimate"));
        assertEquals(7, ObtainTierRules.circuit("circuitZPM"));
        assertEquals(7, ObtainTierRules.materialItem("circuit", "Ultimate"));
        assertFalse(atDefaultCap(ObtainTierRules.circuit("circuitZPM")));
    }

    @Test
    void lateSolidifierMaterialsAreNotLootAtEv() {
        for (String m : Arrays.asList(
            "Neutronium",
            "Infinity",
            "Naquadria",
            "Trinium",
            "Iridium",
            "Osmium",
            "NaquadahAlloy",
            "Plutonium241",
            "Universium")) {
            int t = ObtainTierRules.materialItem("ingot", m);
            assertFalse(atDefaultCap(t), m + " is " + ObtainTierRules.name(t));
        }
    }

    @Test
    void evAndHvItemsAreLoot() {
        assertTrue(atDefaultCap(ObtainTierRules.materialItem("plate", "StainlessSteel")));
        assertEquals(3, ObtainTierRules.materialItem("plate", "StainlessSteel"));
        assertEquals(4, ObtainTierRules.materialItem("plate", "TungstenSteel"));
        assertTrue(atDefaultCap(ObtainTierRules.materialItem("plate", "TungstenSteel")));
        assertEquals(4, ObtainTierRules.circuit("circuitData"));
        assertEquals(3, ObtainTierRules.circuit("circuitAdvanced"));
        assertEquals(4, ObtainTierRules.itemList("Electric_Motor_EV"));
        assertEquals(3, ObtainTierRules.itemList("Electric_Pump_HV"));
        assertTrue(atDefaultCap(ObtainTierRules.itemList("Electric_Motor_EV")));
        assertEquals(0, ObtainTierRules.materialItem("ingot", "Iron"));
        assertEquals(0, ObtainTierRules.vanilla("minecraft:iron_ingot"));
    }

    @Test
    void unknownThingsFailClosed() {
        assertEquals(ObtainTierRules.UNKNOWN, ObtainTierRules.material("Unobtainium"));
        assertEquals(ObtainTierRules.UNKNOWN, ObtainTierRules.materialItem("ingot", "Unobtainium"));
        assertEquals(ObtainTierRules.UNKNOWN, ObtainTierRules.materialItem("noSuchPrefix", "Iron"));
        assertEquals(ObtainTierRules.UNKNOWN, ObtainTierRules.itemList("Some_Gadget"));
        assertEquals(ObtainTierRules.UNKNOWN, ObtainTierRules.circuit("circuitWhatever"));
        assertEquals(ObtainTierRules.UNKNOWN, ObtainTierRules.allowed("othermod:widget", 0));
        assertEquals(ObtainTierRules.UNKNOWN, ObtainTierRules.vanilla("minecraft:command_block"));
        assertFalse(ObtainTierRules.allowed(ObtainTierRules.UNKNOWN, Integer.MAX_VALUE - 1, 12));
    }

    @Test
    void ghostCircuitsAreNeverLootWhateverTheirOreName() {
        assertTrue(ObtainTierRules.neverLoot("Circuit_BasicElectronic"));
        assertTrue(ObtainTierRules.neverLoot("Circuit_Integrated_Advanced"));
        assertEquals(ObtainTierRules.UNKNOWN, ObtainTierRules.itemList("Circuit_BasicElectronic"));
        assertFalse(ObtainTierRules.neverLoot("Circuit_Data"));
    }

    @Test
    void allowListedModItemsResolveByMetaThenNameThenMod() {
        assertEquals(0, ObtainTierRules.allowed("harvestcraft:anything", 3));
        assertEquals(0, ObtainTierRules.allowed("Backpack:tannedLeather", 0));
    }

    @Test
    void theCeilingAndTheCapBothClamp() {
        assertTrue(ObtainTierRules.allowed(4, 4, 4));
        assertFalse(ObtainTierRules.allowed(5, 12, 4));
        assertFalse(ObtainTierRules.allowed(4, 3, 12));
        assertTrue(ObtainTierRules.allowed(7, 12, 12));
        assertFalse(ObtainTierRules.allowed(12, 9, 12));
    }

    @Test
    void everyCuratedMaterialHasATier() {
        for (Map.Entry<String, Integer> e : ObtainTierRules.materials()
            .entrySet()) {
            int t = e.getValue();
            assertTrue(t >= 0 && t <= 12, e.getKey() + " " + t);
        }
        assertEquals("EV", ObtainTierRules.name(4));
        assertEquals("LuV", ObtainTierRules.name(6));
    }

    @Test
    void theAuditNamesMisdeclaredAndUnknownLines() {
        Map<String, Integer> tiers = new HashMap<>();
        tiers.put("a", 1);
        tiers.put("b", 7);
        tiers.put("c", ObtainTierRules.UNKNOWN);
        tiers.put("ammo", 5);
        List<LootEntry> lines = new ArrayList<>();
        lines.add(new LootEntry(LootDrop.one("a"), 1, Tier.LV));
        lines.add(new LootEntry(LootDrop.one("b"), 1, Tier.EV));
        lines.add(new LootEntry(LootDrop.one("c"), 1, Tier.LV));
        lines.add(new LootEntry(LootDrop.one("absent"), 1, Tier.LV));
        lines.add(new LootEntry(LootDrop.one("a"), 1, Tier.HV, "p", LootDrop.one("ammo")));
        List<String> bad = LootTierCheck
            .obtainViolations("t", lines, ref -> tiers.containsKey(ref) ? tiers.get(ref) : -1);
        assertEquals(3, bad.size(), bad.toString());
        assertTrue(
            bad.get(0)
                .contains("b declares EV but its chain needs ZPM"));
        assertTrue(
            bad.get(1)
                .contains("c has no known obtain tier"));
        assertTrue(
            bad.get(2)
                .contains("ammo declares HV but its chain needs IV"));
    }
}
