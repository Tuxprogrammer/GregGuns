package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Stamper;

/**
 * The no-cascade scenario (M8 G12, WGEN-04): a fresh far-away square is generated with every shipped structure on a
 * forced 2-chunk grid, and {@link PopulateGuard} must record no chunk load during any GregGuns populate. As a control
 * a second fresh square is generated the same way with a structure-less generator; the set of chunks each run loaded
 * (relative to its square) must be identical, so the structures pulled in nothing extra. Dev environment only; needs
 * {@code utility} and {@code worldgen} in {@code GG_MODULES}. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-08, GGS-11-09); fixes WGEN-04
public final class CascadeScenarios {

    /** Chunks per side of the generated square. */
    static final int SIDE = 40;
    /** Chunks of margin around the square that must be free of generated terrain before the run. */
    private static final int MARGIN = 3;

    private CascadeScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new NoCascade());
    }

    /** A populator over the real shipped layouts and a forced 2-chunk grid for all three size classes. */
    static StructurePopulator denseShippedPopulator(PaletteKeys keys, List<String> errors) {
        LayoutRegistry layouts = LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            StructureCatalog.idsToLoad(s -> true),
            keys,
            errors::add);
        return new StructurePopulator(
            new StructureGrid(2, 2, 2, StructureCatalog.pools(layouts, s -> layouts.get(s.id()) != null)),
            layouts,
            new Stamper(keys),
            Blocks.dirt,
            Blocks.air,
            StructurePopulator.CONFIG_RULES);
    }

    /** A populator whose pools are empty: it places nothing, but still runs inside the same guard. */
    static StructurePopulator emptyPopulator(PaletteKeys keys) {
        LayoutRegistry none = LayoutRegistry
            .load(id -> { throw new java.io.IOException("none"); }, Collections.<String>emptyList(), keys, s -> {});
        return new StructurePopulator(
            new StructureGrid(2, 2, 2, StructureCatalog.pools(none, s -> true)),
            none,
            new Stamper(keys),
            Blocks.dirt,
            Blocks.air,
            StructurePopulator.CONFIG_RULES);
    }

    private static boolean freeAround(ChunkProviderServer cps, int bx, int bz) {
        for (int dx = -MARGIN; dx < SIDE + MARGIN; dx++) {
            for (int dz = -MARGIN; dz < SIDE + MARGIN; dz++) {
                if (cps.chunkExists(bx + dx, bz + dz)) return false;
            }
        }
        return true;
    }

    /** The minimum chunk of a never-generated square plus margin, far from spawn, or null. */
    private static int[] freshSquare(World world, Random rnd) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        for (int attempt = 0; attempt < 12; attempt++) {
            int bx = 3000 + rnd.nextInt(9000);
            int bz = 3000 + rnd.nextInt(9000);
            if (freeAround(cps, bx, bz)) return new int[] { bx, bz };
        }
        return null;
    }

    /** Chunks loaded by a run, relative to the square's minimum chunk. */
    private static Set<String> runSquare(World world, int bx, int bz) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        Set<String> before = loadedKeys(cps);
        for (int dx = 0; dx < SIDE; dx++) {
            for (int dz = 0; dz < SIDE; dz++) {
                cps.provideChunk(bx + dx, bz + dz);
            }
        }
        Set<String> after = loadedKeys(cps);
        after.removeAll(before);
        Set<String> rel = new HashSet<>();
        for (String k : after) {
            String[] p = k.split(",");
            rel.add((Integer.parseInt(p[0]) - bx) + "," + (Integer.parseInt(p[1]) - bz));
        }
        return rel;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> loadedKeys(ChunkProviderServer cps) {
        Set<String> out = new HashSet<>();
        for (Object o : new ArrayList<Object>(cps.loadedChunks)) {
            Chunk c = (Chunk) o;
            out.add(c.xPosition + "," + c.zPosition);
        }
        return out;
    }

    private static final class NoCascade implements Scenario {

        @Override
        public String name() {
            return "worldgen.no_cascade";
        }

        @Override
        public String description() {
            return "Generating a fresh " + SIDE
                + "x"
                + SIDE
                + " chunk square with every shipped structure on a forced 2-chunk grid records no PopulateGuard"
                + " violation and no write outside a window, stamps structures, and loads exactly the chunks a"
                + " structure-less control square loads (WGEN-04, GGS-11-08, GGS-11-09).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            PaletteKeys keys = PaletteKeys.resolveForPack(s -> {});
            List<String> errors = new ArrayList<>();
            StructurePopulator dense = denseShippedPopulator(keys, errors);
            out.check(errors.isEmpty(), "shipped layouts load clean: " + errors);
            Random rnd = new Random(System.nanoTime());
            try {
                PopulateGuard.register();
                // Structures on. A square can be all ocean for a land-only pool; retry on a new square until one
                // stamps.
                Set<String> withStructures = null;
                int[] sq = null;
                long stamped = 0;
                long rejected = 0;
                long populates = 0;
                long violations = 0;
                for (int i = 0; i < 4 && stamped == 0; i++) {
                    sq = freshSquare(world, rnd);
                    if (sq == null) return ScenarioResult.fail(name(), "no fresh square found");
                    StructureGenerator.setOverride(dense);
                    PopulateGuard.clearViolations();
                    long s0 = StructureGenerator.stampedCount();
                    long r0 = StructureGenerator.rejectedWrites();
                    long p0 = StructureGenerator.populateCount();
                    withStructures = runSquare(world, sq[0], sq[1]);
                    stamped = StructureGenerator.stampedCount() - s0;
                    rejected = StructureGenerator.rejectedWrites() - r0;
                    populates = StructureGenerator.populateCount() - p0;
                    violations = PopulateGuard.violationCount();
                }
                out.check(populates > 0, "the generator ran on populated chunks (" + populates + ")");
                out.check(stamped > 0, "structures were stamped, so the run is not vacuous (" + stamped + ")");
                out.checkEquals("PopulateGuard violations " + PopulateGuard.violations(), 0L, violations);
                out.checkEquals("writes dropped outside a window", 0L, rejected);
                out.check(withStructures != null && withStructures.size() >= SIDE * SIDE, "the whole square loaded");

                // Control: a fresh square, same size and path, a generator that places nothing.
                int[] ctl = freshSquare(world, rnd);
                if (ctl == null) return ScenarioResult.fail(name(), "no fresh control square found");
                StructureGenerator.setOverride(emptyPopulator(keys));
                PopulateGuard.clearViolations();
                long cs0 = StructureGenerator.stampedCount();
                Set<String> control = runSquare(world, ctl[0], ctl[1]);
                out.checkEquals("control stamped nothing", 0L, StructureGenerator.stampedCount() - cs0);
                out.checkEquals("control guard violations", 0, PopulateGuard.violationCount());
                out.check(
                    control.equals(withStructures),
                    "loaded-chunk set equals the structures-off run: " + withStructures.size()
                        + " vs "
                        + control.size()
                        + diff(withStructures, control));
            } finally {
                StructureGenerator.setOverride(null);
                PopulateGuard.clearViolations();
            }
            return out.build();
        }

        private static String diff(Set<String> a, Set<String> b) {
            Set<String> extra = new HashSet<>(a);
            extra.removeAll(b);
            Set<String> missing = new HashSet<>(b);
            missing.removeAll(a);
            if (extra.isEmpty() && missing.isEmpty()) return "";
            return " (extra with structures: " + head(extra) + "; missing: " + head(missing) + ")";
        }

        private static String head(Set<String> s) {
            List<String> l = new ArrayList<>(s);
            return l.size() > 5 ? l.subList(0, 5) + "+" + (l.size() - 5) : l.toString();
        }
    }
}
