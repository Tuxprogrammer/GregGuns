package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.recipe.Ing;

/** The D57 recipe table: one Assembler recipe per warhead, at its casing tier, with inputs at or below that tier. */
class WarheadRecipeSpecTest {

    /** The slots of GT's Assembler (RecipeMaps.assemblerRecipes is maxIO(9, 1, 1, 0)). */
    private static final int ITEM_SLOTS = 9;

    @Test
    void everyBlastHasExactlyOneFullRecipe() {
        Map<WarheadBlast, Integer> full = new EnumMap<>(WarheadBlast.class);
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            if (!spec.isStarter()) {
                full.merge(spec.blast(), 1, Integer::sum);
            }
        }
        for (WarheadBlast blast : WarheadBlast.values()) {
            assertEquals(Integer.valueOf(1), full.get(blast), "recipes of " + blast);
        }
    }

    @Test
    void onlyCondensedHasAStarter() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            if (spec.isStarter()) {
                assertEquals(WarheadBlast.CONDENSED, spec.blast());
            }
        }
    }

    @Test
    void everyRecipeRunsAtItsCasingTier() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            Tier casing = spec.blast()
                .tier() == ExplosiveTier.T1 ? Tier.HV : Tier.EV;
            assertEquals(casing, spec.machineTier(), spec + " runs at the casing tier");
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
    void casingPlatesAreFirstAndTheDocumentedCount() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            Ing casing = spec.inputs()
                .get(0);
            assertEquals(Ing.Src.GT, casing.src());
            boolean t1 = spec.outputTier() == Tier.HV;
            assertEquals(t1 ? "plate:StainlessSteel" : "plate:Titanium", casing.id());
            int expected = spec.isStarter() ? 2 : t1 ? WarheadRecipes.T1_PLATES : WarheadRecipes.T2_PLATES;
            assertEquals(expected, casing.count(), spec + " casing plates");
        }
    }

    @Test
    void circuitsAreUniqueAndValidProgrammedCircuits() {
        Set<Integer> seen = new HashSet<>();
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            assertTrue(spec.circuit() >= 1 && spec.circuit() <= 24, spec + " circuit " + spec.circuit());
            assertTrue(seen.add(spec.circuit()), spec + " reuses circuit " + spec.circuit());
        }
        // The warhead parts share the warheads' circuit space (their inputs overlap: plates, circuits, wire). The I2
        // missile parts (engines, guidance computers) take motors, pumps and their own circuits, so no warhead or
        // warhead part recipe can match their inputs; they only need to be unique among themselves.
        Set<Integer> missile = new HashSet<>();
        for (com.gregguns.icbm.recipe.IcbmPart part : com.gregguns.icbm.recipe.IcbmPart.values()) {
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
        }
    }

    @Test
    void everyPayloadWarheadUsesPlasticExplosiveOrItsOwnFill() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            if (spec.isStarter()) {
                continue;
            }
            boolean hasPe = false;
            for (Ing in : spec.inputs()) {
                hasPe |= in.src() == Ing.Src.PART && in.id()
                    .equals("PLASTIC_EXPLOSIVE");
            }
            assertTrue(hasPe, spec + " has no Plastic Explosive");
        }
    }

    @Test
    void costsMeetD34() {
        // The estimate in warheads-and-fill.md: a T1 launch is about 20 plate equivalents, a T2 launch about 27; a T1
        // warhead is about one launch and a T2 about one and a half.
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            if (spec.isStarter()) {
                continue;
            }
            double cost = spec.cost();
            if (spec.outputTier() == Tier.HV) {
                assertTrue(cost >= 16.0 && cost <= 26.0, spec + " costs " + cost);
            } else {
                assertTrue(cost >= 34.0 && cost <= 46.0, spec + " costs " + cost);
            }
        }
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
