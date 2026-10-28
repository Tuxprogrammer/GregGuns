package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.PlanetTable;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * The loot ceiling sweep (exit box 3, D38): over many generated layouts, in every dimension the module hosts and at
 * every activity level, no chest draws from a line above the dimension's ceiling, no machine asks for a tier above it,
 * and Ross128's rolled ceiling stays inside EV to UMV and uses the whole range.
 */
class CeilingSweepTest {

    private static int ceilingFor(String dimension, long seed) {
        return ComplexPlacer.ceilingFor(dimension, seed, SiloCeiling.DEFAULT_ROSS_WEIGHTS);
    }

    @Test
    void noChestLineOrMachineExceedsTheCeilingInAnyDimension() {
        int chests = 0;
        for (String dim : PlanetTable.names()) {
            for (long s = 0; s < 25; s++) {
                ComplexPlan plan = ComplexLayout.generate(s * 7919 + dim.hashCode(), (int) s, 3);
                int ceiling = ceilingFor(dim, plan.seed());
                assertTrue(ceiling >= SiloCeiling.EV && ceiling <= SiloCeiling.UMV, dim + ": " + ceiling);
                for (ActivityLevel level : ActivityLevel.values()) {
                    List<Placement> all = ComplexContents.build(plan, level, ceiling, new int[] { 5, 6 });
                    for (Placement p : all) {
                        if (p.kind() == Kind.CHEST || p.kind() == Kind.FRAME_WALL) {
                            chests++;
                            assertEquals(SiloLootTables.categoryName(p.key(), ceiling), p.category(), dim);
                            for (SiloLootTables.Line l : SiloLootTables.lines(p.key(), ceiling)) {
                                assertTrue(l.voltage() <= ceiling, dim + " " + p.key() + ": " + l.ref());
                            }
                        } else if (p.kind() == Kind.MACHINE) {
                            assertTrue(p.tier() <= ceiling, dim + " " + p.role() + " tier " + p.tier());
                        }
                    }
                }
            }
        }
        assertTrue(chests > 5000, "the sweep covered " + chests + " chests");
    }

    @Test
    void ross128RollsTheWholeRangeAndNothingOutsideIt() {
        Set<Integer> seen = new HashSet<>();
        for (long seed = 0; seed < 20000; seed++) {
            int c = SiloCeiling.roll(seed * 0x9E3779B97F4A7C15L + 1, SiloCeiling.DEFAULT_ROSS_WEIGHTS);
            assertTrue(c >= SiloCeiling.EV && c <= SiloCeiling.UMV, "rolled " + c);
            seen.add(c);
        }
        assertEquals(SiloCeiling.STEPS, seen.size(), "every ceiling from EV to UMV is reachable: " + seen);
    }

    @Test
    void everyHostedDimensionHasACeilingWithinEvToUmv() {
        for (String dim : PlanetTable.names()) {
            if (PlanetTable.tierOf(dim) == PlanetTable.ROLLED) {
                continue;
            }
            int c = PlanetTable.ceilingVoltage(dim);
            assertTrue(c >= SiloCeiling.EV && c <= SiloCeiling.UMV, dim + ": " + c);
        }
    }
}
