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

import com.gregguns.icbm.compat.GalacticraftParts;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/** The D8 and D27 table: tiers, fits in GT's machines, cost bands, guidance mapping, and the bench's matching rule. */
class MissileBuildSpecTest {

    /** RecipeMaps.circuitAssemblerRecipes takes up to six items and one fluid. */
    private static final int CIRCUIT_ASSEMBLER_ITEMS = 6;
    /** The Assembly Line takes up to 16 items and 4 fluids. */
    private static final int LINE_ITEMS = 16;
    /** RecipeMaps.assemblerRecipes, and the HV to IV single blocks, take up to nine items and one fluid. */
    private static final int ASSEMBLER_ITEMS = 9;

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
    void bodyInputsFitTheirMachineAndTheEngineIsAtOrBelowTheTier() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            // plates, fins, engine and circuits: GT's line needs 4 to 16 item inputs, the Assembler takes up to 9
            int items = 4;
            assertTrue(s.onAssemblyLine() ? items <= LINE_ITEMS : items <= ASSEMBLER_ITEMS, s.toString());
            assertTrue(
                s.bodyCircuits()
                    .tier()
                    .voltageIndex()
                    <= s.tier()
                        .voltageIndex());
            for (Ing in : Arrays.asList(s.plates(), s.fins(), s.engine())) {
                assertTrue(
                    in.tier()
                        .voltageIndex()
                        <= s.partTier()
                            .voltageIndex(),
                    s + " takes " + in.id() + " above its band");
            }
            assertTrue(s.bodyTicks() > 0);
        }
    }

    @Test
    void aBodysPartsStayInsideItsTierBand() {
        // D343, from the real pack: T1 and T3 bodies are buildable at their own tier; the T2 body's heavy fins and
        // engine need IV, the top of the T2 band (D5); the T3 body is the Tier 4 rocket's plates, fins and engine.
        assertEquals(MissileBuildSpec.T1.tier(), MissileBuildSpec.T1.partTier());
        assertEquals(com.gregguns.api.Tier.IV, MissileBuildSpec.T2.partTier());
        assertEquals(MissileBuildSpec.T3.tier(), MissileBuildSpec.T3.partTier());
        assertEquals(
            GalacticraftParts.HULL_T3,
            MissileBuildSpec.T3.plates()
                .id());
        assertEquals(
            GalacticraftParts.FINS_HEAVY,
            MissileBuildSpec.T3.fins()
                .id());
        assertEquals(
            GalacticraftParts.ENGINE_HEAVY,
            MissileBuildSpec.T3.engine()
                .id());
        assertEquals(4, MissileBenchRules.plateSlots(MissileBuildSpec.T3));
    }

    @Test
    void onlyLuvAndUpBodiesUseTheAssemblyLineAndOnlyTheyHaveResearch() {
        // D281 (owner, 0.4.0 alpha playtest): the Assembly Line is a LuV machine in GTNH, so nothing below LuV uses it.
        assertEquals(com.gregguns.api.Tier.LUV, MissileBuildSpec.ASSEMBLY_LINE_TIER);
        assertFalse(MissileBuildSpec.T1.onAssemblyLine());
        assertFalse(MissileBuildSpec.T2.onAssemblyLine());
        assertTrue(MissileBuildSpec.T3.onAssemblyLine());
        int luv = com.gregguns.api.Tier.LUV.voltageIndex();
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            boolean below = s.tier()
                .voltageIndex() < luv;
            assertEquals(!below, s.onAssemblyLine(), s.toString());
            if (s.onAssemblyLine()) {
                assertTrue(s.scanSeconds() > 0, s + " line recipe needs a research scan");
                assertNotNull(s.scanTier(), s.toString());
                assertTrue(
                    s.scanTier()
                        .voltageIndex()
                        <= s.tier()
                            .voltageIndex(),
                    s + " scan above its tier");
            } else {
                assertEquals(0, s.scanSeconds(), s + " Assembler body has no research");
                assertEquals(null, s.scanTier(), s.toString());
            }
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
                // T3 takes Mk III (IV), below its LuV tier: no guidance steps, but it fits (D70).
                assertTrue(
                    s == MissileBuildSpec.T3 || c.tier()
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
        String engine = GalacticraftParts.ENGINE_T1;
        List<String> plates = repeat("compressed:Steel", MissileBenchRules.plateSlots(s));
        List<String> fins = repeat(GalacticraftParts.FINS_T1, MissileBenchRules.FIN_SLOTS);
        assertEquals(0, MissileBenchRules.match(s, plates, fins, engine, "guidance_computer_mk1"));
        assertEquals(1, MissileBenchRules.match(s, plates, fins, engine, "guidance_computer_mk2"));
        // wrong engine, empty slot, wrong plates, wrong fins, a computer the body does not take
        assertEquals(
            -1,
            MissileBenchRules.match(s, plates, fins, GalacticraftParts.ENGINE_HEAVY, "guidance_computer_mk1"));
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, null, "guidance_computer_mk1"));
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, engine, null));
        assertEquals(
            -1,
            MissileBenchRules.match(s, repeat("plate:Steel", plates.size()), fins, engine, "guidance_computer_mk1"));
        assertEquals(
            -1,
            MissileBenchRules
                .match(s, plates, repeat(GalacticraftParts.FINS_HEAVY, 4), engine, "guidance_computer_mk1"));
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, engine, "fuze_assembly_mk1"));
        List<String> short3 = fins.subList(0, 3);
        assertEquals(-1, MissileBenchRules.match(s, plates, short3, engine, "guidance_computer_mk1"));
    }

    @Test
    void t2BenchRejectsTheMkOneComputer() {
        MissileBuildSpec s = MissileBuildSpec.T2;
        List<String> plates = repeat("compressed:Titanium", MissileBenchRules.plateSlots(s));
        List<String> fins = repeat(GalacticraftParts.FINS_HEAVY, 4);
        String engine = GalacticraftParts.ENGINE_HEAVY;
        assertEquals(0, MissileBenchRules.match(s, plates, fins, engine, "guidance_computer_mk2"));
        assertEquals(-1, MissileBenchRules.match(s, plates, fins, engine, "guidance_computer_mk1"));
    }

    @Test
    void acceptsOnlyWhatABenchSlotCouldHold() {
        MissileBuildSpec s = MissileBuildSpec.T1;
        assertTrue(MissileBenchRules.accepts(s, "compressed:Steel"));
        assertTrue(MissileBenchRules.accepts(s, GalacticraftParts.FINS_T1));
        assertTrue(MissileBenchRules.accepts(s, GalacticraftParts.ENGINE_T1));
        assertTrue(MissileBenchRules.accepts(s, "guidance_computer_mk3"));
        assertFalse(MissileBenchRules.accepts(s, "plate:StainlessSteel"));
        assertFalse(MissileBenchRules.accepts(s, GalacticraftParts.ENGINE_HEAVY));
        assertFalse(MissileBenchRules.accepts(s, null));
        assertFalse(MissileBenchRules.accepts(MissileBuildSpec.T2, "guidance_computer_mk1"));
    }

    @Test
    void bodiesUseGalacticraftPartsAndCompressedPlatesInOneColumn() {
        // D69: compressed plates (fewer of them), Galacticraft fins and engines, one vertical plate column.
        assertEquals(
            "compressed:Steel",
            MissileBuildSpec.T1.plates()
                .id());
        assertEquals(
            "compressed:Titanium",
            MissileBuildSpec.T2.plates()
                .id());
        assertTrue(MissileBenchRules.plateSlots(MissileBuildSpec.T1) <= 4);
        assertTrue(MissileBenchRules.plateSlots(MissileBuildSpec.T2) <= 4);
        assertEquals(1, MissileBenchLayout.PLATE_COLUMNS);
        assertEquals(
            GalacticraftParts.ENGINE_T1,
            MissileBuildSpec.T1.engine()
                .id());
        assertEquals(
            GalacticraftParts.ENGINE_HEAVY,
            MissileBuildSpec.T2.engine()
                .id());
        assertEquals(
            GalacticraftParts.FINS_T1,
            MissileBuildSpec.T1.fins()
                .id());
        assertEquals(
            GalacticraftParts.FINS_HEAVY,
            MissileBuildSpec.T2.fins()
                .id());
    }

    @Test
    void noIcbmRecipeRunsBelowHv() {
        // Owner 2026-10-26: nothing ICBM or rocketry runs below HV. Ingredients may be lower, machines may not.
        int hv = com.gregguns.api.Tier.HV.voltageIndex();
        for (IcbmPart p : IcbmPart.values()) {
            assertTrue(
                p.tier()
                    .voltageIndex() >= hv,
                p + " is below HV");
        }
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            assertTrue(
                s.tier()
                    .voltageIndex() >= hv,
                s + " is below HV");
            assertTrue(
                s.scanTier() == null || s.scanTier()
                    .voltageIndex() >= hv,
                s + " research scan is below HV");
        }
        for (com.gregguns.icbm.warhead.WarheadRecipeSpec w : com.gregguns.icbm.warhead.WarheadRecipeSpec.values()) {
            assertTrue(
                w.machineTier()
                    .voltageIndex() >= hv,
                w + " is below HV");
        }
    }

    @Test
    void eachTierHasItsOwnResearchBodyAndFitCircuit() {
        // GT's scanner keeps the first Assembly Line recipe of a research item, so one line recipe per tier needs its
        // own (the LuV and up bodies); the fit circuits keep the Assembler fits apart.
        Set<Integer> numbers = new HashSet<>();
        Set<Integer> circuits = new HashSet<>();
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            assertTrue(numbers.add(s.number()), s + " shares a body (research item)");
            assertTrue(circuits.add(s.fitCircuit()), s + " shares a fit circuit");
            assertTrue(s.fitCircuit() >= 1 && s.fitCircuit() <= 24);
        }
    }
}
