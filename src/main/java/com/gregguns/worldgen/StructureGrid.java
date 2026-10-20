package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * One seeded structure attempt per grid cell, per size class. The cell is {@code spacing} chunks square; the structure
 * sits at a seeded offset inside it (the whole box fits when the cell is large enough) and never within 8 chunks of
 * chunk (0, 0). Pure logic: spacing and pool weights are constructor arguments, sizes come from {@link Footprint}.
 *
 * <p>
 * Cells of different classes are independent and may overlap in space; deciding between them is the populate step's
 * job (G6 records the decision).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10, GGS-11-11); fixes WGEN-30
public final class StructureGrid {

    /** No attempt is made when the box touches a chunk within this many chunks of (0, 0) (GGS-11-11). */
    public static final int SPAWN_EXCLUSION_CHUNKS = 8;

    private final Map<SizeClass, Integer> cellBlocks = new EnumMap<SizeClass, Integer>(SizeClass.class);
    private final Map<SizeClass, List<PoolEntry>> pools = new EnumMap<SizeClass, List<PoolEntry>>(SizeClass.class);
    private final Map<SizeClass, Integer> totalWeight = new EnumMap<SizeClass, Integer>(SizeClass.class);
    private final Map<SizeClass, Integer> maxExtent = new EnumMap<SizeClass, Integer>(SizeClass.class);

    /**
     * @param smallSpacing  chunks per SMALL cell (default 16)
     * @param mediumSpacing chunks per MEDIUM cell (default 32)
     * @param bigSpacing    chunks per BIG cell (default 64)
     * @param poolsByClass  the weighted pool of each class; a missing or all-zero pool means no attempts for it
     */
    public StructureGrid(int smallSpacing, int mediumSpacing, int bigSpacing,
        Map<SizeClass, List<PoolEntry>> poolsByClass) {
        cellBlocks.put(SizeClass.SMALL, spacingBlocks(smallSpacing));
        cellBlocks.put(SizeClass.MEDIUM, spacingBlocks(mediumSpacing));
        cellBlocks.put(SizeClass.BIG, spacingBlocks(bigSpacing));
        for (SizeClass c : SizeClass.values()) {
            List<PoolEntry> list = poolsByClass.get(c);
            if (list == null) list = Collections.emptyList();
            pools.put(c, Collections.unmodifiableList(new ArrayList<PoolEntry>(list)));
            int total = 0;
            int extent = 1;
            for (PoolEntry e : list) {
                total += e.weight();
                extent = Math.max(
                    extent,
                    Math.max(
                        e.footprint()
                            .sizeX(),
                        e.footprint()
                            .sizeZ()));
            }
            totalWeight.put(c, total);
            maxExtent.put(c, extent);
        }
    }

    private static int spacingBlocks(int chunks) {
        if (chunks < 1) throw new IllegalArgumentException("spacing must be at least 1 chunk: " + chunks);
        return chunks * 16;
    }

    /**
     * The attempt of one cell.
     *
     * @return the plan, or empty when the pool is empty or the box would touch the spawn exclusion zone
     */
    public Optional<StructurePlan> planAt(long worldSeed, int dimension, SizeClass sizeClass, int cellX, int cellZ) {
        int total = totalWeight.get(sizeClass);
        if (total <= 0) return Optional.empty();

        long h = mix(worldSeed + 0x1234ABCDL);
        h = mix(h + dimension * 0x9E3779B97F4A7C15L);
        h = mix(h + (sizeClass.ordinal() + 1) * 0xC2B2AE3D27D4EB4FL);
        h = mix(h + cellX * 0xD6E8FEB86659FD93L);
        h = mix(h + cellZ * 0xA0761D6478BD642FL);
        Random rnd = new Random(h);

        // Draw order is part of the contract: same seed, same plan (GGS-11-10).
        PoolEntry entry = pick(pools.get(sizeClass), rnd.nextInt(total));
        int rotation = rnd.nextInt(4);
        int variant = rnd.nextInt(entry.variants());
        Footprint f = entry.footprint();
        int sx = (rotation & 1) == 0 ? f.sizeX() : f.sizeZ();
        int sz = (rotation & 1) == 0 ? f.sizeZ() : f.sizeX();

        int cell = cellBlocks.get(sizeClass);
        // The offset range comes from the real (layout) size. A box bigger than the cell is pinned to the cell corner.
        int anchorX = cellX * cell + rnd.nextInt(Math.max(0, cell - sx) + 1);
        int anchorZ = cellZ * cell + rnd.nextInt(Math.max(0, cell - sz) + 1);

        if (touchesSpawn(anchorX, sx) && touchesSpawn(anchorZ, sz)) return Optional.empty();

        long chestBase = rnd.nextLong();
        return Optional.of(
            new StructurePlan(
                entry.id(),
                sizeClass,
                dimension,
                cellX,
                cellZ,
                anchorX,
                anchorZ,
                rotation,
                variant,
                f,
                chestBase));
    }

    /**
     * Every plan whose box touches the populate window of chunk (cx, cz), blocks cx*16+8 to cx*16+23.
     */
    public List<StructurePlan> plansForWindow(long worldSeed, int dimension, int cx, int cz) {
        List<StructurePlan> out = new ArrayList<StructurePlan>();
        int wx = cx * 16 + 8;
        int wz = cz * 16 + 8;
        for (SizeClass c : SizeClass.values()) {
            if (totalWeight.get(c) <= 0) continue;
            int cell = cellBlocks.get(c);
            int reach = maxExtent.get(c) - 1;
            // A box starts inside its cell and extends at most `reach` blocks toward +X/+Z.
            int x0 = Math.floorDiv(wx - reach, cell);
            int x1 = Math.floorDiv(wx + 15, cell);
            int z0 = Math.floorDiv(wz - reach, cell);
            int z1 = Math.floorDiv(wz + 15, cell);
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    Optional<StructurePlan> p = planAt(worldSeed, dimension, c, x, z);
                    if (p.isPresent() && p.get()
                        .overlapsWindow(cx, cz)) out.add(p.get());
                }
            }
        }
        return out;
    }

    /** Whether the box span [anchor, anchor+size-1] touches a chunk within the exclusion range on this axis. */
    private static boolean touchesSpawn(int anchor, int size) {
        int lo = Math.floorDiv(anchor, 16);
        int hi = Math.floorDiv(anchor + size - 1, 16);
        return hi >= -SPAWN_EXCLUSION_CHUNKS && lo <= SPAWN_EXCLUSION_CHUNKS;
    }

    private static PoolEntry pick(List<PoolEntry> pool, int roll) {
        int acc = 0;
        for (PoolEntry e : pool) {
            acc += e.weight();
            if (roll < acc) return e;
        }
        throw new IllegalStateException("roll outside pool");
    }

    /** SplitMix64 finalizer. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
