package com.gregguns.icbm.worldgen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.StructureGenerator;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;

/**
 * Real-pack setups for the ore check (D348): walk a player's chunk loading into a naturally generated silo complex, so
 * every window of it populates in the order a player walking in causes, with the whole pack's worldgen (GregTech's ore
 * veins last), then count the ore blocks inside the complex. Operations come from {@code ore-ops.txt} in the server's
 * working directory, one per line:
 * <ul>
 * <li>{@code walk <cellX> <cellZ> <W|E|N|S> <radius> <perTick>}: loads chunks as a player walking in from that side
 * with that view radius would, a few per tick, until the far side is passed;</li>
 * <li>{@code grid <cellX> <cellZ> <perTick>}: loads every chunk of the box and a margin, row by row;</li>
 * <li>{@code wait <ticks>}: lets the world tick (the complex keeper runs on the world tick);</li>
 * <li>{@code audit <cellX> <cellZ>}: logs, per kind of complex cell, how many hold an ore ("ORE audit" lines).</li>
 * </ul>
 * Dev only, never part of {@code test all}. Server side.
 */
public final class OreAuditSetups {

    private static final File OPS = new File("ore-ops.txt");

    private OreAuditSetups() {}

    /** @param registry where to add the setups */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Do());
    }

    /** One queued operation: returns true once done. */
    private interface Job {

        boolean step(World world);
    }

    private static final class Do implements Scenario {

        @Override
        public String name() {
            return "testing.icbm_ore_do";
        }

        @Override
        public String description() {
            return "Ore check setup (dev only): runs the operations in ore-ops.txt (walk, grid, wait, audit) on naturally generated complexes. Not part of test all.";
        }

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public int areaRadius() {
            return 1;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<String> lines;
            try {
                if (!OPS.exists()) {
                    GregGuns.LOG.info("ORE do: no ops file");
                    return ScenarioResult.builder(name())
                        .build();
                }
                lines = Files.readAllLines(OPS.toPath(), StandardCharsets.UTF_8);
                Files.move(
                    OPS.toPath(),
                    new File("ore-ops.done").toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                return ScenarioResult.fail(name(), e.toString());
            }
            final World world = ctx.world();
            final Deque<Job> jobs = new ArrayDeque<Job>();
            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty() || l.startsWith("#")) continue;
                Job j = parse(world, l.split("\\s+"));
                if (j != null) jobs.add(j);
            }
            return ScenarioResult.pending(name(), 20 * 60 * 60, ticks -> {
                long end = System.nanoTime() + 200_000_000L;
                while (!jobs.isEmpty() && System.nanoTime() < end) {
                    if (!jobs.peekFirst()
                        .step(world)) return null;
                    jobs.removeFirst();
                }
                if (!jobs.isEmpty()) return null;
                GregGuns.LOG.info("ORE do: all done");
                return ScenarioResult.builder(name())
                    .build();
            });
        }
    }

    private static Job parse(World world, String[] t) {
        switch (t[0]) {
            case "walk":
                return walk(
                    world,
                    Integer.parseInt(t[1]),
                    Integer.parseInt(t[2]),
                    t[3],
                    Integer.parseInt(t[4]),
                    Integer.parseInt(t[5]));
            case "grid":
                return grid(world, Integer.parseInt(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3]));
            case "wait": {
                final int[] left = { Integer.parseInt(t[1]) };
                return w -> --left[0] <= 0;
            }
            case "audit": {
                final int cx = Integer.parseInt(t[1]), cz = Integer.parseInt(t[2]);
                return w -> {
                    audit(w, cx, cz);
                    return true;
                };
            }
            case "keepexamples":
                return w -> {
                    List<String> ex = com.gregguns.icbm.complex.gen.ComplexKeeper.game()
                        .examples();
                    for (int i = 0; i < ex.size(); i += 10) {
                        GregGuns.LOG.info("ORE keep set back: {}", ex.subList(i, Math.min(ex.size(), i + 10)));
                    }
                    return true;
                };
            case "column": {
                // column <x> <z> <y0> <y1>: logs every block of one column, for a look at what replaced what
                final int x = Integer.parseInt(t[1]), z = Integer.parseInt(t[2]), y0 = Integer.parseInt(t[3]),
                    y1 = Integer.parseInt(t[4]);
                return w -> {
                    StringBuilder b = new StringBuilder();
                    for (int y = y0; y <= y1; y++) {
                        b.append(' ')
                            .append(y)
                            .append('=')
                            .append(Block.blockRegistry.getNameForObject(w.getBlock(x, y, z)))
                            .append(':')
                            .append(w.getBlockMetadata(x, y, z));
                    }
                    GregGuns.LOG.info("ORE column {},{}:{}", x, z, b);
                    return true;
                };
            }
            case "slice": {
                // slice <x0> <z0> <x1> <z1> <y>: one layer as a letter grid (s stone, a air, o ore, w complex wall,
                // . anything else), a row per z
                final int x0 = Integer.parseInt(t[1]), z0 = Integer.parseInt(t[2]), x1 = Integer.parseInt(t[3]),
                    z1 = Integer.parseInt(t[4]), y = Integer.parseInt(t[5]);
                return w -> {
                    for (int z = z0; z <= z1; z++) {
                        StringBuilder b = new StringBuilder();
                        for (int x = x0; x <= x1; x++) {
                            Block k = w.getBlock(x, y, z);
                            b.append(
                                isOre(w, x, y, z) ? 'o'
                                    : k == net.minecraft.init.Blocks.stone ? 's'
                                        : k == net.minecraft.init.Blocks.air ? 'a'
                                            : k == com.gregguns.icbm.complex.wall.BlockComplexWall.instance() ? 'w'
                                                : '.');
                        }
                        GregGuns.LOG.info("ORE slice y={} z={} x={}..{} {}", y, z, x0, x1, b);
                    }
                    return true;
                };
            }
            default:
                GregGuns.LOG.warn("ORE unknown op {}", t[0]);
                return null;
        }
    }

    private static StructurePlan plan(World world, int cellX, int cellZ) {
        Optional<StructurePlan> p = StructurePopulator
            .complexGridPlanAt(world.getSeed(), world.provider.dimensionId, cellX, cellZ);
        if (!p.isPresent()) throw new IllegalStateException("no complex plan in cell " + cellX + "," + cellZ);
        return p.get();
    }

    /** Chunks in the order a walker's view loads them: per step, the ring around the walker, nearest first. */
    private static Job walk(World world, int cellX, int cellZ, String side, int radius, int perTick) {
        StructurePlan p = plan(world, cellX, cellZ);
        int x0 = p.anchorX() >> 4, x1 = (p.anchorX() + ComplexLayout.SIZE - 1) >> 4;
        int z0 = p.anchorZ() >> 4, z1 = (p.anchorZ() + ComplexLayout.SIZE - 1) >> 4;
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        List<int[]> path = new ArrayList<int[]>();
        int far = radius + 2;
        switch (side) {
            case "W":
                for (int x = x0 - far; x <= x1 + far; x++) path.add(new int[] { x, mz });
                break;
            case "E":
                for (int x = x1 + far; x >= x0 - far; x--) path.add(new int[] { x, mz });
                break;
            case "N":
                for (int z = z0 - far; z <= z1 + far; z++) path.add(new int[] { mx, z });
                break;
            default:
                for (int z = z1 + far; z >= z0 - far; z--) path.add(new int[] { mx, z });
        }
        final Deque<int[]> order = new ArrayDeque<int[]>();
        Set<Long> seen = new LinkedHashSet<Long>();
        for (int[] at : path) {
            List<int[]> ring = new ArrayList<int[]>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = at[0] + dx, cz = at[1] + dz;
                    if (seen.add(((long) cx << 32) ^ (cz & 0xFFFFFFFFL)))
                        ring.add(new int[] { cx, cz, dx * dx + dz * dz });
                }
            }
            ring.sort((a, b) -> Integer.compare(a[2], b[2]));
            order.addAll(ring);
        }
        GregGuns.LOG.info(
            "ORE walk cell {},{} anchor {},{} from {}: {} chunks over {} steps",
            cellX,
            cellZ,
            p.anchorX(),
            p.anchorZ(),
            side,
            order.size(),
            path.size());
        return loader(world, order, perTick, "walk " + cellX + "," + cellZ);
    }

    private static Job grid(World world, int cellX, int cellZ, int perTick) {
        StructurePlan p = plan(world, cellX, cellZ);
        Deque<int[]> order = new ArrayDeque<int[]>();
        for (int cx = (p.anchorX() >> 4) - 3; cx <= ((p.anchorX() + ComplexLayout.SIZE - 1) >> 4) + 3; cx++) {
            for (int cz = (p.anchorZ() >> 4) - 3; cz <= ((p.anchorZ() + ComplexLayout.SIZE - 1) >> 4) + 3; cz++) {
                order.add(new int[] { cx, cz });
            }
        }
        return loader(world, order, perTick, "grid " + cellX + "," + cellZ);
    }

    private static Job loader(World world, Deque<int[]> order, int perTick, String what) {
        final ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        final long t0 = System.nanoTime();
        final int total = order.size();
        return w -> {
            for (int i = 0; i < perTick && !order.isEmpty(); i++) {
                int[] c = order.removeFirst();
                cps.loadChunk(c[0], c[1]);
            }
            if (!order.isEmpty()) return false;
            GregGuns.LOG.info("ORE {} loaded {} chunks in {} ms", what, total, (System.nanoTime() - t0) / 1_000_000L);
            return true;
        };
    }

    /** @return the game keeper's totals since boot, in words */
    private static String keepStats() {
        long[] s = com.gregguns.icbm.complex.gen.ComplexKeeper.game()
            .stats();
        return s[0] + " chunks kept, "
            + s[1]
            + " cells checked, "
            + s[2]
            + " set back, keeping "
            + s[3] / 1_000_000L
            + " ms, recording "
            + s[4] / 1_000_000L
            + " ms, "
            + s[5]
            + " records read from saved chunks; set back cells held "
            + com.gregguns.icbm.complex.gen.ComplexKeeper.game()
                .replaced();
    }

    /** Kinds of cell the audit counts, by where in the complex they lie. */
    private enum Where {
        LEVEL,
        SKIN,
        SHAFT,
        LADDER,
        ROCK
    }

    /**
     * Counts ore blocks in a placed complex: the level's cells, its skin (D320), the silo shafts and the escape ladder
     * above the level (all the complex's own), and the rock the box leaves natural. Only loaded chunks are read.
     */
    static void audit(World world, int cellX, int cellZ) {
        StructurePlan plan = plan(world, cellX, cellZ);
        Decision d = StructureState.forWorld(world)
            .get(PlanKey.of(plan));
        ComplexWriter writer = StructureGenerator.complexWriter();
        if (d == null || !d.isPlaced() || !(writer instanceof ComplexPlacer)) {
            GregGuns.LOG.info("ORE audit cell {},{}: not placed ({})", cellX, cellZ, d);
            return;
        }
        ComplexPlacer placer = (ComplexPlacer) writer;
        placer.enabledIn(world);
        int floor = ComplexPlacer.floorY(world, d.groundY(), d.sink());
        ComplexRaster r = placer.rasterOf(plan);
        ComplexPlan cp = r.plan();
        int ax = plan.anchorX(), az = plan.anchorZ();
        long[] ore = new long[Where.values().length], cells = new long[Where.values().length],
            stone = new long[Where.values().length];
        long unloaded = 0, cutOff = 0;
        StringBuilder examples = new StringBuilder();
        int ex = cp.escapeX(), ez = cp.escapeZ(), em = cp.escapeMeta();
        int bx = ex + (em == 4 ? 1 : em == 5 ? -1 : 0), bz = ez + (em == 2 ? 1 : em == 3 ? -1 : 0);
        for (int lx = -1; lx <= ComplexLayout.SIZE; lx++) {
            for (int lz = -1; lz <= ComplexLayout.SIZE; lz++) {
                int x = ax + lx, z = az + lz;
                if (!world.getChunkProvider()
                    .chunkExists(x >> 4, z >> 4)) {
                    unloaded++;
                    continue;
                }
                // A column the cut-off mask (D207) left natural, under a lake or a valley, holds no complex.
                com.gregguns.worldgen.HeightProbe probe = new com.gregguns.worldgen.WindowedHeightProbe(
                    new com.gregguns.worldgen.PopulateWindow((x - 8) >> 4, (z - 8) >> 4),
                    new com.gregguns.worldgen.WorldColumnReader(world));
                if (!siloColumn(r, lx, lz) && ComplexPlacer.cutOff(probe, floor, x, z)) {
                    cutOff++;
                    continue;
                }
                boolean inBox = lx >= 0 && lz >= 0 && lx < ComplexLayout.SIZE && lz < ComplexLayout.SIZE;
                int top = -1;
                for (int y = ComplexRaster.LEVELS - 1; y >= 0 && top < 0; y--)
                    if (r.at(lx, y, lz) != Cell.NONE) top = y;
                for (int y = -1; y <= ComplexRaster.LEVELS; y++) {
                    byte c = r.at(lx, y, lz);
                    Where k;
                    if (c != Cell.NONE) k = Where.LEVEL;
                    else if (y == -1 ? top >= 0
                        : r.at(lx + 1, y, lz) != Cell.NONE || r.at(lx - 1, y, lz) != Cell.NONE
                            || r.at(lx, y, lz + 1) != Cell.NONE
                            || r.at(lx, y, lz - 1) != Cell.NONE
                            || r.at(lx, y - 1, lz) != Cell.NONE && !siloColumn(r, lx, lz))
                        k = Where.SKIN;
                    else if (inBox) k = Where.ROCK;
                    else continue;
                    count(world, x, floor + y, z, k, c, ore, cells, stone, examples);
                }
                boolean silo = siloColumn(r, lx, lz);
                boolean escape = lx == ex && lz == ez || lx == bx && lz == bz;
                // The writer builds a shaft or the ladder only up through land: under a lake there is none.
                if ((silo || escape) && probe.landY(x, z) != com.gregguns.worldgen.HeightProbe.NONE) {
                    int from = silo ? floor + ComplexRaster.LEVELS + 1 : floor + cp.escapeTop();
                    int to = world.getHeightValue(x, z);
                    for (int y = from; y <= to; y++) {
                        count(
                            world,
                            x,
                            y,
                            z,
                            silo ? Where.SHAFT : Where.LADDER,
                            (byte) -1,
                            ore,
                            cells,
                            stone,
                            examples);
                    }
                }
            }
        }
        StringBuilder sum = new StringBuilder();
        for (Where k : Where.values()) {
            sum.append(' ')
                .append(k)
                .append('=')
                .append(ore[k.ordinal()])
                .append('/')
                .append(cells[k.ordinal()])
                .append(" stone ")
                .append(stone[k.ordinal()]);
        }
        long own = ore[Where.LEVEL.ordinal()] + ore[Where.SKIN.ordinal()]
            + ore[Where.SHAFT.ordinal()]
            + ore[Where.LADDER.ordinal()];
        GregGuns.LOG.info(
            "ORE audit cell {},{} anchor {},{} floor {} level {} silos {}: ore in complex cells {} ({}), unloaded columns {}, cut-off columns {}; keep: {} records pending, {}",
            cellX,
            cellZ,
            ax,
            az,
            floor,
            placer.levelOf(plan),
            cp.silos()
                .size(),
            own,
            sum.toString()
                .trim(),
            unloaded,
            cutOff,
            com.gregguns.icbm.complex.gen.ComplexKeeper.game()
                .pendingCount(world),
            keepStats());
        if (examples.length() > 0) GregGuns.LOG.info("ORE audit examples:{}", examples);
    }

    private static void count(World world, int x, int y, int z, Where k, byte cell, long[] ore, long[] cells,
        long[] stone, StringBuilder examples) {
        if (y < 0 || y > 255) return;
        cells[k.ordinal()]++;
        if (isGtStone(world, x, y, z)) {
            stone[k.ordinal()]++;
            if (k != Where.ROCK && examples.length() < 3000) {
                examples.append(" S:")
                    .append(k)
                    .append('@')
                    .append(x)
                    .append(',')
                    .append(y)
                    .append(',')
                    .append(z)
                    .append('=')
                    .append(Block.blockRegistry.getNameForObject(world.getBlock(x, y, z)))
                    .append(':')
                    .append(world.getBlockMetadata(x, y, z))
                    .append("(cell ")
                    .append(cell)
                    .append(')');
            }
        }
        if (!isOre(world, x, y, z)) return;
        ore[k.ordinal()]++;
        if (k != Where.ROCK && examples.length() < 1500) {
            examples.append(' ')
                .append(k)
                .append('@')
                .append(x)
                .append(',')
                .append(y)
                .append(',')
                .append(z)
                .append("(cell ")
                .append(cell)
                .append(')');
        }
    }

    /** @return whether the block is a GregTech, BartWorks or GT++ ore, read through GregTech's own ore lookup */
    static boolean isOre(World world, int x, int y, int z) {
        try (gregtech.common.ores.OreInfo<?> info = gregtech.common.ores.OreManager.getOreInfo(world, x, y, z)) {
            return info != null;
        } catch (Exception | LinkageError e) {
            Block b = world.getBlock(x, y, z);
            String n = String.valueOf(Block.blockRegistry.getNameForObject(b));
            return n.toLowerCase(java.util.Locale.ROOT)
                .contains("ore");
        }
    }

    /**
     * @return whether the block is one GregTech's ore veins replace (plain stone, raw granite, marble or basalt,
     *         deepslate and the rest of its stone types in this dimension)
     */
    static boolean isGtStone(World world, int x, int y, int z) {
        try {
            return gregtech.api.enums.StoneType.findStoneType(world, x, y, z) != null;
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    /** For silo columns: a complex-local column belongs to a silo's footprint. */
    static boolean siloColumn(ComplexRaster r, int lx, int lz) {
        return r.ownerAt(lx, lz) < 0 && r.at(lx, 0, lz) != Cell.NONE;
    }
}
