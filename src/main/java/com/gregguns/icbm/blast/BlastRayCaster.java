package com.gregguns.icbm.blast;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * The spherical ray sweep that decides which blocks a blast reaches: rays leave the center in an even grid of yaw and
 * pitch, each with 50% to 100% of the blast's energy, and lose energy for every half-block step and for every block
 * they cross. Runs on a worker thread over a {@link BlastGrid} snapshot, never over the world.
 *
 * <p>
 * The result is the set of breakable cells that kept energy after their own resistance, each once, nearest the center
 * first, as {@link BlastPacking} offsets.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     int[] hit = BlastRayCaster.cast(grid, 10, BlastRayCaster.defaultEnergy(10), new Random(1), () -> false);
 * }
 * </pre>
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (section: ray caster)
// Ported from ICBM-Classic (MIT):
// https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/threaded/BlastNuclear.java#L34-L124
// Ported from ICBM-Classic (MIT):
// https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/thread/ThreadSmallExplosion.java#L24-L91
// Avoids: ICBM-02-02, ICBM-02-03, ICBM-02-04, ICBM-02-05, ICBM-02-60
public final class BlastRayCaster {

    /** Energy every half-block step costs, air included (0.3 x 0.75 x 5 in the original). */
    public static final double STEP_COST = 1.125;
    /** Cost of a liquid cell. */
    public static final double LIQUID_COST = 0.25;
    /** Distance a ray moves per step, blocks. */
    private static final double STEP = 0.5;
    /** Distance a small-caster ray moves per step, blocks. */
    private static final double GRID_STEP = 0.3;

    private BlastRayCaster() {}

    /**
     * The starting energy that lets even the weakest ray (half energy) cross open air to the edge of the radius.
     *
     * @param radius the blast radius, blocks
     * @return the energy (fixes ICBM-02-05: the configured radius is reachable)
     */
    public static double defaultEnergy(double radius) {
        return radius / STEP * STEP_COST / 0.5;
    }

    /**
     * @param radius the blast radius, blocks (at least 3)
     * @return the number of slices per half turn: {@code ceil(pi * radius)}
     */
    public static int steps(int radius) {
        return (int) Math.ceil(Math.PI * Math.max(3, radius));
    }

    /**
     * @param radius the blast radius, blocks
     * @return how many rays {@link #cast} sends: {@code 2 x steps x steps}, each direction once (ICBM-02-03)
     */
    public static long rayCount(int radius) {
        long s = steps(radius);
        return 2L * s * s;
    }

    /**
     * Casts every ray.
     *
     * @param grid      the resistance snapshot; its radius bounds the cast
     * @param radius    the blast radius, blocks, at most the grid's
     * @param energy    the starting energy of a full-strength ray
     * @param random    the source of the per-ray strength
     * @param cancelled polled once per yaw slice; true ends the cast early with what it has
     * @return packed offsets of the cells reached, each once, nearest first
     */
    public static int[] cast(BlastGrid grid, int radius, double energy, Random random, BooleanSupplier cancelled) {
        return cast(grid, radius, 0.5, 0.5, 0.5, energy, random, cancelled);
    }

    /**
     * Casts every ray from a point inside the center cell.
     *
     * @param grid      the resistance snapshot
     * @param radius    the blast radius, blocks, at most the grid's
     * @param fx        the center's position inside its cell, 0 to 1 (a blast at a block's corner is not shifted)
     * @param fy        as {@code fx}, Y
     * @param fz        as {@code fx}, Z
     * @param energy    the starting energy of a full-strength ray
     * @param random    the source of the per-ray strength
     * @param cancelled polled once per yaw slice
     * @return packed offsets of the cells reached, each once, nearest first
     */
    public static int[] cast(BlastGrid grid, int radius, double fx, double fy, double fz, double energy, Random random,
        BooleanSupplier cancelled) {
        int r = Math.min(radius, grid.radius());
        int steps = steps(r);
        BitSet reached = new BitSet(grid.volume());
        int count = 0;
        double r2 = (double) r * r;
        for (int yawSlice = 0; yawSlice < 2 * steps; yawSlice++) {
            if (cancelled.getAsBoolean()) {
                break;
            }
            double yaw = Math.PI / steps * yawSlice;
            for (int pitchSlice = 0; pitchSlice < steps; pitchSlice++) {
                double pitch = Math.PI / steps * pitchSlice;
                double power = energy - energy * random.nextDouble() / 2.0;
                double sx = STEP * Math.sin(pitch) * Math.cos(yaw);
                double sy = STEP * Math.cos(pitch);
                double sz = STEP * Math.sin(pitch) * Math.sin(yaw);
                double px = fx;
                double py = fy;
                double pz = fz;
                int pcx = Integer.MIN_VALUE;
                int pcy = 0;
                int pcz = 0;
                while (power > 0.0) {
                    double dx = px - fx;
                    double dy = py - fy;
                    double dz = pz - fz;
                    if (dx * dx + dy * dy + dz * dz > r2) {
                        break;
                    }
                    power -= STEP_COST;
                    int cx = (int) Math.floor(px);
                    int cy = (int) Math.floor(py);
                    int cz = (int) Math.floor(pz);
                    // A block is charged once per ray: compare by value (fixes ICBM-02-02).
                    if (cx != pcx || cy != pcy || cz != pcz) {
                        short cell = grid.get(cx, cy, cz);
                        if (cell == BlastGrid.UNLOADED || cell == BlastGrid.UNBREAKABLE) {
                            break;
                        }
                        if (cell != BlastGrid.AIR) {
                            power -= BlastGrid.resistance(cell);
                            if (power > 0.0) {
                                // A bit set keeps each cell once (fixes ICBM-02-04 and ICBM-02-60).
                                int idx = grid.index(cx, cy, cz);
                                if (!reached.get(idx)) {
                                    reached.set(idx);
                                    count++;
                                }
                            }
                        }
                    }
                    pcx = cx;
                    pcy = cy;
                    pcz = cz;
                    px += sx;
                    py += sy;
                    pz += sz;
                }
            }
        }
        return sortedOffsets(grid, reached, count);
    }

    /**
     * The small caster: rays toward the surface of a cube, the vanilla-style crater (rougher and smaller than
     * {@link #cast}). An unbreakable block stops a ray at once. Each cell is returned once, nearest first.
     *
     * @param grid      the resistance snapshot
     * @param radius    the blast radius, blocks, at most the grid's
     * @param fx        the center's position inside its cell, 0 to 1
     * @param fy        as {@code fx}, Y
     * @param fz        as {@code fx}, Z
     * @param random    the source of the per-ray strength
     * @param cancelled polled once per cube slice
     * @return packed offsets of the cells reached, each once, nearest first
     */
    public static int[] castGrid(BlastGrid grid, int radius, double fx, double fy, double fz, Random random,
        BooleanSupplier cancelled) {
        int r = Math.min(radius, grid.radius());
        int samples = Math.max(2, r);
        BitSet reached = new BitSet(grid.volume());
        int count = 0;
        double r2 = (double) r * r;
        for (int i = 0; i < samples; i++) {
            if (cancelled.getAsBoolean()) {
                break;
            }
            for (int j = 0; j < samples; j++) {
                for (int k = 0; k < samples; k++) {
                    // Only the cube's surface: one coordinate at its minimum or maximum.
                    if (i != 0 && i != samples - 1 && j != 0 && j != samples - 1 && k != 0 && k != samples - 1) {
                        continue;
                    }
                    double dx = i / (samples - 1.0) * 2.0 - 1.0;
                    double dy = j / (samples - 1.0) * 2.0 - 1.0;
                    double dz = k / (samples - 1.0) * 2.0 - 1.0;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx = dx / len * GRID_STEP;
                    dy = dy / len * GRID_STEP;
                    dz = dz / len * GRID_STEP;
                    double power = r * (0.7 + random.nextDouble() * 0.6);
                    double px = fx;
                    double py = fy;
                    double pz = fz;
                    while (power > 0.0) {
                        double ex = px - fx;
                        double ey = py - fy;
                        double ez = pz - fz;
                        if (ex * ex + ey * ey + ez * ez > r2) {
                            break;
                        }
                        int cx = (int) Math.floor(px);
                        int cy = (int) Math.floor(py);
                        int cz = (int) Math.floor(pz);
                        short cell = grid.get(cx, cy, cz);
                        if (cell == BlastGrid.UNLOADED || cell == BlastGrid.UNBREAKABLE) {
                            break;
                        }
                        if (cell != BlastGrid.AIR) {
                            power -= BlastGrid.resistance(cell);
                        }
                        if (power > 0.0 && cell != BlastGrid.AIR) {
                            int idx = grid.index(cx, cy, cz);
                            if (!reached.get(idx)) {
                                reached.set(idx);
                                count++;
                            }
                        }
                        power -= GRID_STEP * 0.75;
                        px += dx;
                        py += dy;
                        pz += dz;
                    }
                }
            }
        }
        return sortedOffsets(grid, reached, count);
    }

    private static int[] sortedOffsets(BlastGrid grid, BitSet reached, int count) {
        int size = grid.size();
        int radius = grid.radius();
        long[] keys = new long[count];
        int n = 0;
        for (int i = reached.nextSetBit(0); i >= 0; i = reached.nextSetBit(i + 1)) {
            int oz = i % size - radius;
            int oy = (i / size) % size - radius;
            int ox = i / (size * size) - radius;
            long d2 = (long) ox * ox + (long) oy * oy + (long) oz * oz;
            keys[n++] = d2 << 32 | BlastPacking.pack(ox, oy, oz) & 0xFFFFFFFFL;
        }
        Arrays.sort(keys);
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            out[i] = (int) keys[i];
        }
        return out;
    }
}
