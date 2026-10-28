package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexArena;
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
import com.gregguns.worldgen.layout.ResolvedBlock;

/**
 * The role audit in the scenarios (D267). Every {@code icbm.complex.*} scenario is registered through
 * {@link #audited}, which fails it when the audit logged a violation while it ran, and notes how many complexes and
 * cells were read back. The probe scenario proves the audit can fail: it writes a complex whose styles put one block in
 * every role and checks the audit reports it. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (The role audit); D267
public final class RoleAuditScenarios {

    /** The probe's name; it expects violations, so it is never wrapped. */
    static final String PROBE = "icbm.complex.role_audit_probe";

    private RoleAuditScenarios() {}

    /** @param registry where to register the probe */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Probe());
    }

    /**
     * Registers every scenario of {@code from} into {@code into}, the {@code icbm.complex.*} ones wrapped by
     * {@link #audited}.
     */
    public static void registerAudited(ScenarioRegistry from, ScenarioRegistry into) {
        for (Scenario s : from.all()) into.register(audited(s));
    }

    /** @return the scenario, failing on a role audit violation when it is an {@code icbm.complex.*} scenario */
    public static Scenario audited(Scenario s) {
        return s.name()
            .startsWith("icbm.complex.") && !PROBE.equals(s.name()) ? new Audited(s) : s;
    }

    private static final class Audited implements Scenario {

        private final Scenario inner;

        Audited(Scenario inner) {
            this.inner = inner;
        }

        @Override
        public String name() {
            return inner.name();
        }

        @Override
        public String description() {
            return inner.description() + " Fails on any role audit violation (D267).";
        }

        @Override
        public boolean includedInAll() {
            return inner.includedInAll();
        }

        @Override
        public int areaRadius() {
            return inner.areaRadius();
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final int before = RoleAudit.total();
            final int audits = RoleAudit.audits();
            final long cells = RoleAudit.cellsChecked();
            ScenarioResult r = inner.run(ctx);
            if (r.isPending()) {
                return ScenarioResult.pending(r.scenario(), r.timeoutTicks(), t -> {
                    ScenarioResult x = r.poller()
                        .poll(t);
                    return x == null ? null : finish(x, before, audits, cells);
                });
            }
            return finish(r, before, audits, cells);
        }

        private ScenarioResult finish(ScenarioResult r, int before, int audits, long cells) {
            // A complex whose last window never ran in this scenario is still read back, as far as it was written.
            RoleAudit.flush();
            if (r.skipped()) return r;
            int found = RoleAudit.total() - before;
            int read = RoleAudit.audits() - audits;
            if (found == 0 && read == 0) return r;
            ScenarioResult.Builder out = ScenarioResult.builder(r.scenario());
            for (String m : r.messages()) {
                if (r.passed()) out.note(m);
                else out.check(false, m);
            }
            out.note(
                "role audit: " + read
                    + " complexes read back, "
                    + (RoleAudit.cellsChecked() - cells)
                    + " floor, wall and ceiling cells, "
                    + found
                    + " violations");
            if (found > 0) {
                List<String> recent = RoleAudit.recent();
                List<String> mine = recent.subList(Math.max(0, recent.size() - Math.min(found, 5)), recent.size());
                out.check(false, found + " role audit violations (D267), the last: " + mine);
            }
            return out.build();
        }
    }

    /** Writes a complex whose every room has one block for floor, wall and ceiling, and expects the audit to say so. */
    private static final class Probe implements Scenario {

        private static final int GROUND = 63;

        @Override
        public String name() {
            return PROBE;
        }

        @Override
        public String description() {
            return "The role audit can fail (D267): a complex written with one block for every floor, wall and ceiling"
                + " is reported, section by section, with the shell's write path; the lines are captured, not logged.";
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
            StructurePlan plan = g.planAt(0xA0D17L, world.provider.dimensionId, SizeClass.BIG, 5, 4)
                .get();
            final ResolvedBlock one = new ResolvedBlock(Blocks.stonebrick, 0, false);
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
                }).withContents(false)
                    .withStyles((raster, dim, level) -> new Styles() {

                        @Override
                        public ResolvedBlock block(byte role, int x, int y, int z) {
                            return role == Cell.FLOOR || role == Cell.WALL || role == Cell.CEILING ? one : null;
                        }

                        @Override
                        public ResolvedBlock litLamp(int x, int y, int z) {
                            return null;
                        }

                        @Override
                        public String path(byte role, int x, int y, int z) {
                            return "the probe's one-block style";
                        }
                    });
            List<String> lines = new ArrayList<>();
            int before = RoleAudit.total();
            RoleAudit.capture(lines);
            try {
                ComplexArena.load(world, plan);
                ComplexArena.slab(world, plan, false, (x, z) -> GROUND, 0, GROUND);
                placer.enabledIn(world);
                StructureState state = new StructureState("gregguns_role_audit_probe");
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
                RoleAudit.flush();
            } finally {
                RoleAudit.capture(null);
                ComplexArena.slab(world, plan, true, (x, z) -> GROUND, 0, GROUND);
            }
            int sections = placer.rasterOf(plan)
                .plan()
                .sectionCount();
            out.check(RoleAudit.enabled(), "the audit is on in scenarios");
            out.checkEquals("captured lines did not reach the log count", before, RoleAudit.total());
            // Every section shows the one block as floor, wall and ceiling: one line each.
            out.checkEquals("a violation line per section", sections, lines.size());
            boolean named = !lines.isEmpty() && lines.get(0)
                .contains("minecraft:stonebrick")
                && lines.get(0)
                    .contains("FLOOR and WALL and CEILING")
                && lines.get(0)
                    .contains("the probe's one-block style");
            out.check(
                named,
                "the line names the block, its roles and the write path: " + (lines.isEmpty() ? "" : lines.get(0)));
            return out.build();
        }
    }
}
