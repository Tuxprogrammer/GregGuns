package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.gen.MetalProps;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * D330 (the balance pass's limit on what a complex gives away for free): over real Overworld plans, the room machines
 * keep their count at every activity level, but their tiers come from the metal props' ladder, so most are LV and MV
 * and
 * the ceiling's EV a find; the radar and comms gear keep their HV-and-up rule. Prints the per-complex figures the
 * loot-and-ceilings sheet quotes.
 */
class ComplexFreeValueTest {

    private static final int PLANS = 12;

    @Test
    void roomMachinesAreMostlyLowTierAndNeverPassTheCeiling() {
        for (ActivityLevel level : ActivityLevel.values()) {
            int working = 0, high = 0, props = 0, propIngots = 0;
            for (long s = 0; s < PLANS; s++) {
                ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
                List<Placement> ps = ComplexContents.build(plan, level, SiloCeiling.EV, null);
                for (Placement p : ps) {
                    if (p.kind() == Kind.MACHINE && p.tier() > 0) {
                        assertTrue(p.tier() <= SiloCeiling.EV, p + " passes the EV ceiling");
                        if (p.role() != MachineRole.RADAR && p.role() != MachineRole.COMMS && !p.dead()) {
                            working++;
                            if (p.tier() >= 3) high++;
                        }
                    }
                    if (p.kind() == Kind.BLOCK && isMetal(p.block(), p.meta())) {
                        props++;
                        propIngots += 9;
                    }
                }
            }
            System.out.printf(
                "D330 %s: %.1f working room machines per complex, %.1f of them HV or EV; %.1f metal props (%.0f"
                    + " ingots)%n",
                level,
                working / (double) PLANS,
                high / (double) PLANS,
                props / (double) PLANS,
                propIngots / (double) PLANS);
            if (working > 0) {
                assertTrue(
                    high <= working * 0.3,
                    level + ": " + high + " of " + working + " room machines are HV or EV");
            }
        }
    }

    @Test
    void theLadderMatchesTheMetalProps() {
        int[] seen = new int[SiloCeiling.EV + 1];
        for (long h = 0; h < 10000; h++) {
            int t = ComplexContents.roomMachineTier(SiloCeiling.EV, h * 0x9E3779B97F4A7C15L);
            assertTrue(t >= 1 && t <= SiloCeiling.EV, "tier " + t);
            seen[t]++;
        }
        // Weights 4, 4, 1, 1 (MetalProps.weight at an EV ceiling): LV and MV about 40% each, HV and EV about 10%.
        assertTrue(seen[1] > 3500 && seen[2] > 3500, "LV " + seen[1] + ", MV " + seen[2]);
        assertTrue(
            seen[3] > 700 && seen[3] < 1300 && seen[4] > 700 && seen[4] < 1300,
            "HV " + seen[3] + ", EV " + seen[4]);
    }

    private static boolean isMetal(String block, int meta) {
        for (int t = 1; t <= 12; t++) {
            for (MetalProps.Prop p : MetalProps.rung(t)) {
                if (p.block.equals(block) && p.meta == meta) return true;
            }
        }
        return false;
    }
}
