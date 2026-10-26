package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WarheadRecipeSpecTest {

    @Test
    void everyBlastHasExactlyOneRecipe() {
        Set<WarheadBlast> seen = EnumSet.noneOf(WarheadBlast.class);
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            assertTrue(seen.add(spec.blast()), "duplicate recipe for " + spec.blast());
        }
        assertEquals(EnumSet.allOf(WarheadBlast.class), seen);
    }

    @Test
    void noRecipeNeedsAMachineAboveTheWarheadsTier() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            assertTrue(
                spec.machineTier()
                    .voltageIndex()
                    <= spec.outputTier()
                        .voltageIndex(),
                spec + " runs above its output tier");
        }
    }

    @Test
    void theGtMachinesOfTheRoadmapAreAllUsed() {
        Set<WarheadRecipeSpec.Machine> machines = EnumSet.noneOf(WarheadRecipeSpec.Machine.class);
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            machines.add(spec.machine());
        }
        assertTrue(machines.contains(WarheadRecipeSpec.Machine.CHEMICAL_REACTOR));
        assertTrue(machines.contains(WarheadRecipeSpec.Machine.IMPLOSION_COMPRESSOR));
    }
}
