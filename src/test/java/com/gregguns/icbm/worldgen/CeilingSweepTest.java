package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.LootConfig;
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
        int saved = LootConfig.maxTier;
        try {
            // The default world-loot cap (EV) and one raised past every ceiling: either way nothing loot-bearing
            // exceeds the lower of the dimension's ceiling and the cap (D364).
            for (int cap : new int[] { 4, 12 }) {
                LootConfig.maxTier = cap;
                sweep(cap);
            }
        } finally {
            LootConfig.maxTier = saved;
        }
    }

    private static void sweep(int cap) {
        int chests = 0;
        for (String dim : PlanetTable.names()) {
            for (long s = 0; s < 25; s++) {
                ComplexPlan plan = ComplexLayout.generate(s * 7919 + dim.hashCode(), (int) s, 3);
                int ceiling = ceilingFor(dim, plan.seed());
                assertTrue(ceiling >= SiloCeiling.EV && ceiling <= SiloCeiling.UMV, dim + ": " + ceiling);
                int loot = Math.max(SiloCeiling.EV, Math.min(ceiling, cap));
                assertEquals(loot, ComplexContents.lootTable(ceiling), dim);
                for (ActivityLevel level : ActivityLevel.values()) {
                    List<Placement> all = ComplexContents.build(plan, level, ceiling, new int[] { 5, 6 });
                    for (Placement p : all) {
                        if (p.kind() == Kind.CHEST || p.kind() == Kind.FRAME_WALL) {
                            chests++;
                            assertEquals(SiloLootTables.categoryName(p.key(), loot), p.category(), dim);
                            for (SiloLootTables.Line l : SiloLootTables.lines(p.key(), loot)) {
                                assertTrue(l.voltage() <= loot, dim + " " + p.key() + ": " + l.ref());
                            }
                        } else if (p.kind() == Kind.MACHINE) {
                            assertTrue(
                                p.tier() <= Math.min(ceiling, cap),
                                dim + " " + p.role() + " tier " + p.tier() + " at cap " + cap);
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
