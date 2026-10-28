package com.gregguns.worldgen;

import java.util.Set;

import net.minecraftforge.common.BiomeDictionary.Type;

/**
 * The seam between the M8 populate pipeline and a procedural underground complex (the ICBM silo complex, D128). The
 * pipeline owns the grid, the one-time placement decision and the window; the writer owns the shape. Nothing here names
 * ICBM, so the worldgen module builds without it.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md
public interface ComplexWriter {

    /** The plan id the complex grid uses. */
    String ID = "silo_complex";
    /** Footprint on X and Z, blocks. */
    int SIZE = 240;
    /** Height above the ground the terrain check must keep clear (the complex is underground). */
    int ABOVE = 4;

    /** @return whether complexes generate in this world (module on, dimension flag set) */
    boolean enabledIn(net.minecraft.world.World world);

    /** @return the share of grid cells that attempt a complex, 0 to 100 */
    int chancePercent();

    /** @return whether the biome types may host a complex in this dimension (Overworld whitelist) */
    boolean biomeOk(int dimension, Set<Type> types);

    /** @return the lowest ground Y a complex may sit under (the level lies this deep below it) */
    int minGroundY();

    /** Called once per rejected plan; the implementation logs it (skip-and-log). */
    void skipped(StructurePlan plan, String reason);

    /**
     * Writes the part of the complex inside the window.
     *
     * @param groundY the ground height recorded when the plan was decided
     * @param probe   ground heights, readable inside the window only
     * @return the number of blocks the sink accepted
     */
    int write(StructurePlan plan, int groundY, PopulateWindow window, WindowedSink sink, HeightProbe probe);
}
