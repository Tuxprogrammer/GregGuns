package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** The silo complex grid: its own seeds, the ledger that outlives the config, and the surface it keeps clear. */
class ComplexGridTest {

    private static StructureGrid shipGridLikeTheComplexOne() {
        return new StructureGrid(
            64,
            64,
            64,
            Collections.singletonMap(
                SizeClass.BIG,
                Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
    }

    @Test
    void aComplexNeverSharesItsSeedWithTheOrdinaryShipOfTheSameCell() {
        int different = 0, cells = 0;
        for (int c = 3; c < 40; c++) {
            Optional<StructurePlan> complex = StructurePopulator.complexPlanAt(77L, 0, c, c + 1, 100);
            Optional<StructurePlan> ship = shipGridLikeTheComplexOne().planAt(77L, 0, SizeClass.BIG, c, c + 1);
            if (!complex.isPresent() || !ship.isPresent()) continue;
            cells++;
            if (complex.get()
                .chestSeedBase()
                != ship.get()
                    .chestSeedBase())
                different++;
        }
        assertTrue(cells > 20);
        assertEquals(cells, different, "the salt separates every cell's seed from the BIG grid's");
    }

    @Test
    void aPlanTheLedgerPlacedIsWrittenWhateverTheRollSaysAndARejectedOneNever() {
        // Find a window whose plan rolls out at 0 percent and in at 100.
        for (int cell = 3; cell < 30; cell++) {
            Optional<StructurePlan> found = StructurePopulator.complexPlanAt(5L, 0, cell, cell, 100);
            if (!found.isPresent()) continue;
            StructurePlan p = found.get();
            int wcx = Math.floorDiv(p.anchorX(), 16) + 3, wcz = Math.floorDiv(p.anchorZ(), 16) + 3;
            assertTrue(
                StructurePopulator.complexPlans(5L, 0, wcx, wcz, 0)
                    .isEmpty(),
                "no roll at 0 percent");
            StructureState state = new StructureState(StructureState.NAME);
            state.decideOnce(PlanKey.of(p), () -> Decision.placed(70));
            assertEquals(
                1,
                StructurePopulator.complexPlans(5L, 0, wcx, wcz, 0, state)
                    .size(),
                "a placed plan stays written at chance 0");
            StructureState rejected = new StructureState(StructureState.NAME);
            rejected.decideOnce(PlanKey.of(p), () -> Decision.rejected("water"));
            assertTrue(
                StructurePopulator.complexPlans(5L, 0, wcx, wcz, 100, rejected)
                    .isEmpty(),
                "a rejected plan is never written, whatever the roll");
            return;
        }
        throw new AssertionError("no complex found to test with");
    }

    @Test
    void ordinaryStructuresKeepOffAComplexFootprint() {
        List<StructurePlan> all = new ArrayList<>();
        for (int c = 3; c < 30; c++) StructurePopulator.complexPlanAt(77L, 0, c, c, 100)
            .ifPresent(all::add);
        assertFalse(all.isEmpty());
        StructurePlan k = all.get(0);
        StructurePlan onTop = new StructurePlan(
            "ship",
            SizeClass.MEDIUM,
            0,
            0,
            0,
            k.anchorX() + 100,
            k.anchorZ() + 100,
            0,
            0,
            new Footprint(30, 20, 30),
            1L,
            2L,
            0);
        assertTrue(StructurePopulator.meetsComplex(onTop, 77L, 100, null));
        assertFalse(StructurePopulator.meetsComplex(onTop, 77L, 0, null), "nothing to avoid when none rolls");
        StructurePlan far = new StructurePlan(
            "ship",
            SizeClass.MEDIUM,
            0,
            0,
            0,
            k.anchorX() + 240 + 200,
            k.anchorZ() + 240 + 200,
            0,
            0,
            new Footprint(30, 20, 30),
            1L,
            2L,
            0);
        assertFalse(StructurePopulator.meetsComplex(far, 77L, 100, null), "well clear of the complex");
    }
}
