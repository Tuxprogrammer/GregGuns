package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * The warhead chain's recipe tables (D282): one inert warhead per casing, then one payload and one fill recipe per
 * blast, each at its tier, with inputs at or below that tier, and no explosive anywhere but the Plastic Explosive
 * charges.
 */
class WarheadRecipeSpecTest {

    /** The slots of GT's Assembler (RecipeMaps.assemblerRecipes is maxIO(9, 1, 1, 0)). */
    private static final int ITEM_SLOTS = 9;

    @Test
    void everyBlastHasExactlyOneSpec() {
        Map<WarheadBlast, Integer> seen = new EnumMap<>(WarheadBlast.class);
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            seen.merge(spec.blast(), 1, Integer::sum);
        }
        for (WarheadBlast blast : WarheadBlast.values()) {
            assertEquals(Integer.valueOf(1), seen.get(blast), "specs of " + blast);
        }
    }

    @Test
    void onlyCondensedIsChargesAlone() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            assertEquals(spec.blast() == WarheadBlast.CONDENSED, spec.chargesOnly(), spec + " payload");
        }
        assertEquals(ExplosiveTier.T1, WarheadBlast.CONDENSED.tier());
    }

    @Test
    void everyRecipeRunsAtItsCasingTier() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            Tier casing = spec.blast()
                .tier() == ExplosiveTier.T1 ? Tier.HV
                    : spec.blast()
                        .tier() == ExplosiveTier.T2 ? Tier.EV
                            : spec.blast()
                                .tier() == ExplosiveTier.T4
                                || spec.blast()
                                    .tier() == ExplosiveTier.T5 ? Tier.ZPM
                                        : spec.blast() == WarheadBlast.NUCLEAR ? Tier.ZPM : Tier.LUV;
            assertEquals(casing, spec.machineTier(), spec + " runs at the casing tier");
            // The real machine: UV for antimatter, UHV for red matter.
            if (spec.blast()
                .tier() == ExplosiveTier.T4) {
                assertEquals(8, spec.machineVoltageIndex());
            }
            if (spec.blast()
                .tier() == ExplosiveTier.T5) {
                assertEquals(9, spec.machineVoltageIndex());
            }
        }
    }

    @Test
    void theInertWarheadRunsAtItsCasingTier() {
        int[] volts = { 3, 4, 6, 8, 9 };
        for (InertWarheadSpec spec : InertWarheadSpec.values()) {
            assertEquals(spec.casing(), WarheadCasing.values()[spec.ordinal()]);
            assertEquals(volts[spec.ordinal()], spec.voltageIndex(), spec + " voltage");
            assertEquals(
                spec.casing()
                    .tier()
                    .minVoltageTier(),
                spec.voltageIndex(),
                spec + " matches the casing tier");
            for (Ing in : spec.inputs()) {
                assertTrue(
                    in.tier()
                        .voltageIndex() <= Math.min(spec.voltageIndex(), Tier.ZPM.voltageIndex()),
                    spec + " takes " + in.id() + " above its tier");
            }
        }
    }

    @Test
    void theInertWarheadIsTheCasingPlatesAndTheFuze() {
        String[] plates = { "plate:StainlessSteel", "plate:Titanium", "plate:TungstenSteel", "plate:Naquadria",
            "plate:Neutronium" };
        int[] counts = { WarheadRecipes.T1_PLATES, WarheadRecipes.T2_PLATES, WarheadRecipes.T3_PLATES,
            WarheadRecipes.T4_PLATES, WarheadRecipes.T5_PLATES };
        IcbmPart[] fuzes = { IcbmPart.FUZE_ASSEMBLY_MK1, IcbmPart.FUZE_ASSEMBLY_MK2, IcbmPart.FUZE_ASSEMBLY_MK3,
            IcbmPart.FUZE_ASSEMBLY_MK3, IcbmPart.FUZE_ASSEMBLY_MK3 };
        for (InertWarheadSpec spec : InertWarheadSpec.values()) {
            int i = spec.ordinal();
            assertEquals(
                2,
                spec.inputs()
                    .size(),
                spec + " inputs");
            Ing casing = spec.inputs()
                .get(0);
            assertEquals(Ing.Src.GT, casing.src());
            assertEquals(plates[i], casing.id());
            assertEquals(counts[i], casing.count(), spec + " casing plates");
            Ing fuze = spec.inputs()
                .get(1);
            assertEquals(Ing.Src.PART, fuze.src());
            assertEquals(fuzes[i].name(), fuze.id());
        }
    }

    @Test
    void noInputIsAboveTheRecipesTier() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            for (Ing in : spec.inputs()) {
                if (in.isOptional()) {
                    continue;
                }
                assertTrue(
                    in.tier()
                        .voltageIndex()
                        <= spec.machineTier()
                            .voltageIndex(),
                    spec + " takes " + in.id() + " above its tier");
            }
        }
    }

    @Test
    void noPayloadCarriesExplosiveOrCasingParts() {
        // D282 and the owner's alpha note: Plastic Explosive is made from Industrial TNT, so a recipe that takes both
        // counts the explosive twice. The explosive is the charges alone; the casing plates and fuze are the inert
        // warhead's.
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            for (Ing in : spec.inputs()) {
                assertFalse(
                    in.src() == Ing.Src.TNT || in.src() == Ing.Src.INDUSTRIAL_TNT || in.src() == Ing.Src.DYNAMITE,
                    spec + " carries explosive " + in.id());
                assertFalse(
                    in.src() == Ing.Src.PART && (in.id()
                        .equals("PLASTIC_EXPLOSIVE")
                        || in.id()
                            .startsWith("FUZE_")),
                    spec + " takes " + in.id());
            }
        }
    }

    @Test
    void circuitsAreUniqueAndValidProgrammedCircuits() {
        Set<Integer> seen = new HashSet<>();
        Set<Integer> heavy = new HashSet<>();
        Set<Integer> annihilation = new HashSet<>();
        assertTrue(InertWarheadSpec.CIRCUIT >= 1 && InertWarheadSpec.CIRCUIT <= 24);
        seen.add(InertWarheadSpec.CIRCUIT);
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            assertTrue(spec.circuit() >= 1 && spec.circuit() <= 24, spec + " circuit " + spec.circuit());
            // The T1 and T2 payload and fill recipes share the circuit space with the inert warhead and the parts.
            // A T3 recipe takes a T3 inert warhead no other recipe has, so T3 circuits only need to differ among
            // themselves; T4 and T5 likewise.
            boolean t3 = spec.blast()
                .tier() == ExplosiveTier.T3;
            boolean t45 = spec.blast()
                .tier() == ExplosiveTier.T4
                || spec.blast()
                    .tier() == ExplosiveTier.T5;
            assertTrue(
                (t45 ? annihilation : t3 ? heavy : seen).add(spec.circuit()),
                spec + " reuses circuit " + spec.circuit());
        }
        // The warhead parts share the warheads' circuit space (their inputs overlap: plates, circuits, wire). The I2
        // missile parts (engines, guidance computers) take motors, pumps and their own circuits, so no warhead or
        // warhead part recipe can match their inputs; they only need to be unique among themselves.
        Set<Integer> missile = new HashSet<>();
        for (IcbmPart part : IcbmPart.values()) {
            assertTrue(part.circuit() >= 1 && part.circuit() <= 24, part + " circuit " + part.circuit());
            boolean missilePart = part.registryName()
                .startsWith("missile_")
                || part.registryName()
                    .startsWith("guidance_");
            assertTrue((missilePart ? missile : seen).add(part.circuit()), part + " reuses circuit " + part.circuit());
        }
    }

    @Test
    void recipesFitTheAssembler() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            assertTrue(spec.itemSlots() <= ITEM_SLOTS, spec + " uses " + spec.itemSlots() + " item slots");
            assertTrue(spec.fluidInputs() <= 1, spec + " has more than one fluid");
            // The fill recipe: the inert warhead, the charges in stacks of at most 64, and the circuit.
            int fillSlots = 2 + (spec.blast()
                .fillCap() + 63) / 64;
            assertTrue(fillSlots <= ITEM_SLOTS, spec + " fill uses " + fillSlots + " item slots");
        }
    }

    @Test
    void costsMeetD34() {
        // The estimate in warheads-and-fill.md: a T1 launch is about 20 plate equivalents, a T2 launch about 27; a T1
        // warhead is about one launch and a T2 about one and a half to two. The cost counts the inert warhead, the
        // payload and a full casing of Plastic Explosive charges.
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            double cost = spec.cost();
            if (spec.blast()
                .tier() == ExplosiveTier.T4
                || spec.blast()
                    .tier() == ExplosiveTier.T5) {
                // T4 and T5 (D134): the endgame warheads, a few T3 launches' worth.
                assertTrue(cost >= 100.0 && cost <= 400.0, spec + " costs " + cost);
            } else if (spec.blast()
                .tier() == ExplosiveTier.T3) {
                    // T3 (D80): the warhead is the expensive part; about two to four T3 launches.
                    assertTrue(cost >= 90.0 && cost <= 180.0, spec + " costs " + cost);
                } else if (spec.outputTier() == Tier.HV) {
                    assertTrue(cost >= 14.0 && cost <= 26.0, spec + " costs " + cost);
                } else {
                    assertTrue(cost >= 38.0 && cost <= 52.0, spec + " costs " + cost);
                }
        }
    }

    @Test
    void aFullWarheadCostsTheInertCasingThePayloadAndItsCharges() {
        WarheadRecipeSpec s = WarheadRecipeSpec.SHRAPNEL;
        double expected = InertWarheadSpec.T1.cost() + s.payloadCost() + 5 * WarheadRecipeSpec.CHARGE_COST;
        assertEquals(expected, s.cost(), 1e-9);
        assertEquals(
            InertWarheadSpec.T1.cost() + 5 * WarheadRecipeSpec.CHARGE_COST,
            WarheadRecipeSpec.CONDENSED.cost(),
            1e-9);
    }

    @Test
    void theGasEffectsUseGeneralIndustrialFluids() {
        Set<String> allowed = new HashSet<>(java.util.Arrays.asList("NitrousOxide", "Ammonia", "Creosote", "Biomass"));
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            for (Ing in : spec.inputs()) {
                if ((in.src() == Ing.Src.GAS || in.src() == Ing.Src.FLUID) && (spec.blast()
                    .kind() == WarheadBlast.Kind.GAS_DEBILITATION
                    || spec.blast()
                        .kind() == WarheadBlast.Kind.GAS_CHEMICAL
                    || spec.blast()
                        .kind() == WarheadBlast.Kind.GAS_CONTAGIOUS
                    || spec.blast()
                        .kind() == WarheadBlast.Kind.SMOKE)) {
                    assertTrue(allowed.contains(in.id()), spec + " fill " + in.id());
                }
            }
        }
    }

    @Test
    void mustardSeedsAreOptionalAndOnlyInTheChemicalWarhead() {
        int found = 0;
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            for (Ing in : spec.inputs()) {
                if (in.src() == Ing.Src.OPTIONAL_MOD_ITEM) {
                    found++;
                    assertEquals(WarheadBlast.CHEMICAL, spec.blast());
                    assertEquals("harvestcraft:mustardseedsItem", in.id());
                    assertTrue(in.count() <= 4);
                }
            }
        }
        assertEquals(1, found);
    }
}
