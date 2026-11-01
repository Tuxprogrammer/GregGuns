package com.gregguns.icbm.complex.gen;

import java.util.Collections;
import java.util.List;

import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.ComplexArena;
import com.gregguns.icbm.worldgen.MachineFacingAudit;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Footprint;
import com.gregguns.worldgen.PoolEntry;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WorldColumnReader;
import com.gregguns.worldgen.WorldTarget;

/**
 * Every GT machine a generated complex places faces north, south, east or west (unless its meta tile entity allows
 * only a vertical side), and a pump has a horizontal main facing. A full complex with its contents is written and
 * every GT tile read back from the world. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Machine facing)
public final class MachineFacingScenarios {

    private static final int GROUND = 63;

    private MachineFacingScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Facing());
    }

    private static final class Facing implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.machine_facing";
        }

        @Override
        public String description() {
            return "Writes a complete silo complex and reads every placed GT machine back: each faces a horizontal side"
                + " its meta tile entity allows (none left facing down), with the placing code path named on failure.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructureGrid g = new StructureGrid(
                64,
                64,
                64,
                Collections.singletonMap(
                    SizeClass.BIG,
                    Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
            StructurePlan plan = g.planAt(0xFAC1D6L, world.provider.dimensionId, SizeClass.BIG, 3, 4)
                .get();
            ComplexPlacer placer = new ComplexPlacer(
                new WorldBlocks(),
                new GtMachineHook(),
                MarkFiller.NONE,
                new ComplexPlacer.Settings() {

                    public boolean dimensionEnabled(String n) {
                        return true;
                    }

                    public int chancePercent() {
                        return 100;
                    }

                    public java.util.Set<Type> overworldBiomes() {
                        return Collections.emptySet();
                    }
                });
            int violationsBefore = MachineFacingAudit.total();
            int checkedBefore = MachineFacingAudit.checked();
            int[] machines = { 0 };
            List<String> found;
            try {
                ComplexArena.load(world, plan);
                ComplexArena.slab(world, plan, false, (x, z) -> GROUND, 0, GROUND);
                placer.enabledIn(world);
                StructureState state = new StructureState("gregguns_machine_facing");
                StructurePopulator populator = com.gregguns.worldgen.PlacementHarnessAccess.populator();
                for (int[] w : com.gregguns.worldgen.PlacementHarnessAccess.windowsOf(plan, false)) {
                    PopulateWindow window = new PopulateWindow(w[0], w[1]);
                    ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                    try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                        populator.populateComplex(
                            plan,
                            placer,
                            window,
                            sink,
                            new WorldColumnReader(world),
                            (x, z) -> Collections.singleton(Type.PLAINS),
                            state);
                    }
                }
                found = MachineFacingAudit.scan(
                    world,
                    plan.anchorX() - 8,
                    0,
                    plan.anchorZ() - 8,
                    plan.anchorX() + 260,
                    255,
                    plan.anchorZ() + 260,
                    machines);
            } finally {
                RoleAudit.flush();
                ComplexArena.slab(world, plan, true, (x, z) -> GROUND, 0, GROUND);
            }
            out.note(
                machines[0] + " GT machines read back, "
                    + (MachineFacingAudit.checked() - checkedBefore)
                    + " checked at placement");
            out.check(machines[0] > 0, "the complex placed no GT machine, so the check would pass vacuously");
            out.check(found.isEmpty(), "machines with a bad facing on read-back: " + found);
            out.checkEquals("placement-time violations", violationsBefore + found.size(), MachineFacingAudit.total());
            return out.build();
        }
    }
}
