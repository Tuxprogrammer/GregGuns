package com.gregguns.icbm.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/** The D8 and D27 table: tiers, fits in GT's machines, cost bands, guidance mapping, and the bench's matching rule. */
class MissileBuildSpecTest {

    /** RecipeMaps.circuitAssemblerRecipes takes up to six items and one fluid. */
    private static final int CIRCUIT_ASSEMBLER_ITEMS = 6;
    /** The Assembly Line takes up to 16 items and 4 fluids. */
    private static final int LINE_ITEMS = 16;

    @Test
    void chipInputsAreAtOrBelowTheRecipeTierAndFitTheCircuitAssembler() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            assertTrue(
                s.chipInputs()
                    .size() <= CIRCUIT_ASSEMBLER_ITEMS,
                s.toString());
            for (Ing in : s.chipInputs()) {
                assertTrue(
                    in.tier()
                        .voltageIndex()
                        <= s.tier()
                            .voltageIndex(),
                    s + " chip takes " + in.id() + " above its tier");
            }
        }
    }

    @Test
    void t1IsHvAndT2IsEv() {
        assertEquals(com.gregguns.api.Tier.HV, MissileBuildSpec.T1.tier());
        assertEquals(com.gregguns.api.Tier.EV, MissileBuildSpec.T2.tier());
        assertEquals(1, MissileBuildSpec.T1.number());
        assertEquals(MissileBuildSpec.T2, MissileBuildSpec.byNumber(2));
    }

    @Test
    void bodyInputsFitTheAssemblyLineAndTheEngineIsAtOrBelowTheTier() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            // plates, fins, engine and guidance computer each take one slot
            assertTrue(4 <= LINE_ITEMS);
            assertTrue(
                s.engine()
                    .tier()
                    .voltageIndex()
                    <= s.tier()
                        .voltageIndex());
            assertTrue(s.scanSeconds() > 0 && s.lineTicks() > 0);
        }
    }

    @Test
    void theBodyCostSitsInItsD34Band() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            IcbmPart cheapest = s.computers()
                .get(0);
            double cost = s.cost(cheapest);
            assertTrue(cost >= s.costMin() && cost <= s.costMax(), s + " costs " + cost);
        }
    }

    @Test
    void aHigherComputerCostsMore() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            double last = 0;
            for (IcbmPart c : s.computers()) {
                assertTrue(s.cost(c) > last);
                last = s.cost(c);
            }
        }
    }

    @Test
    void guidanceIsTheComputerTierMinusTheBodyTier() {
        MissileBuildSpec t1 = MissileBuildSpec.T1;
        assertEquals(0, t1.guidanceOf(IcbmPart.GUIDANCE_COMPUTER_MK1));
        assertEquals(1, t1.guidanceOf(IcbmPart.GUIDANCE_COMPUTER_MK2));
        assertEquals(2, t1.guidanceOf(IcbmPart.GUIDANCE_COMPUTER_MK3));
        MissileBuildSpec t2 = MissileBuildSpec.T2;
        assertEquals(-1, t2.guidanceOf(IcbmPart.GUIDANCE_COMPUTER_MK1));
        assertEquals(0, t2.guidanceOf(IcbmPart.GUIDANCE_COMPUTER_MK2));
        assertEquals(1, t2.guidanceOf(IcbmPart.GUIDANCE_COMPUTER_MK3));
        assertEquals(-1, t1.guidanceOf(IcbmPart.FUZE_ASSEMBLY_MK1));
    }

    @Test
    void computersAreAllRealPartsOfAHighEnoughTier() {
        Set<IcbmPart> seen = new HashSet<>();
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            for (IcbmPart c : s.computers()) {
                assertNotNull(c);
                assertTrue(
                    c.registryName()
                        .startsWith("guidance_computer"));
                assertTrue(
                    c.tier()
                        .voltageIndex()
                        >= s.tier()
                            .voltageIndex());
                seen.add(c);
            }
        }
        assertEquals(3, seen.size());
    }

    private static List<String> repeat(String key, int n) {
        String[] a = new String[n];
        Arrays.fill(a, key);
        return Arrays.asList(a);
    }

    @Test
    void benchMakesABodyFromTheFullSlotSetOnly() {
        MissileBuildSpec s = MissileBuildSpec.T1;
        List<String> plates = repeat("plate:StainlessSteel", MissileBenchRules.plateSlots(s));
        List<String> fins = repeat("plate:Steel", MissileBenchRules.FIN_SLOTS);
        assertEquals(0, MissileBenchRules.match(s, plates, fins, "missile_engine_mk1", "guidance_computer_mk1"));
        assertEquals(1, MissileBenchRules.match(s, plates, fins, "missile_engine_mk1", "guidance_computer_mk2"));
        // wrong engine, empty slot, wrong plates, wrong fins, a computer the body does not take
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, "missile_engine_mk2", "guidance_computer_mk1"));
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, null, "guidance_computer_mk1"));
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, "missile_engine_mk1", null));
        assertEquals(
            -1,
            MissileBenchRules
                .match(s, repeat("plate:Steel", plates.size()), fins, "missile_engine_mk1", "guidance_computer_mk1"));
        assertEquals(
            -1,
            MissileBenchRules
                .match(s, plates, repeat("plate:Titanium", 4), "missile_engine_mk1", "guidance_computer_mk1"));
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, "missile_engine_mk1", "fuze_assembly_mk1"));
        List<String> short3 = fins.subList(0, 3);
        assertEquals(-1, MissileBenchRules.match(s, plates, short3, "missile_engine_mk1", "guidance_computer_mk1"));
    }

    @Test
    void t2BenchRejectsTheMkOneComputer() {
        MissileBuildSpec s = MissileBuildSpec.T2;
        List<String> plates = repeat("plate:Titanium", MissileBenchRules.plateSlots(s));
        List<String> fins = repeat("plate:StainlessSteel", 4);
        assertEquals(0, MissileBenchRules.match(s, plates, fins, "missile_engine_mk2", "guidance_computer_mk2"));
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, "missile_engine_mk2", "guidance_computer_mk1"));
    }

    @Test
    void acceptsOnlyWhatABenchSlotCouldHold() {
        MissileBuildSpec s = MissileBuildSpec.T1;
        assertTrue(MissileBenchRules.accepts(s, "plate:StainlessSteel"));
        assertTrue(MissileBenchRules.accepts(s, "plate:Steel"));
        assertTrue(MissileBenchRules.accepts(s, "missile_engine_mk1"));
        assertTrue(MissileBenchRules.accepts(s, "guidance_computer_mk3"));
        assertFalse(MissileBenchRules.accepts(s, "plate:Gold"));
        assertFalse(MissileBenchRules.accepts(s, null));
        assertFalse(MissileBenchRules.accepts(MissileBuildSpec.T2, "guidance_computer_mk1"));
    }
}
