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
     * Called once per rejected plan with what was in the way; the implementation logs both.
     *
     * @param detail where the blocking structure lies (its start chunk and box), or empty
     */
    default void skipped(StructurePlan plan, String reason, String detail) {
        skipped(plan, reason);
    }

    /**
     * The variant recorded with the decision, so later config changes cannot split the complex (D304): the
     * complex's activity level ordinal, rolled with the weights in force when the plan is first decided.
     *
     * @return 0 or more, or {@link Decision#NO_VARIANT} to record none
     */
    int decisionVariant(StructurePlan plan);

    /**
     * How much deeper the complex goes because a surface structure (a village) will stand over its box (D302): its
     * own roll from 10 to 20, cut to what still fits above the bottom of the world.
     *
     * @param groundY the ground height the decision measured
     * @return blocks to sink, 10 or more; or -1 when the complex cannot sink 10 blocks and stay clear of bedrock
     */
    int sinkFor(StructurePlan plan, int groundY);

    /**
     * Called once when a plan is placed sunk under a surface structure; the implementation logs it.
     *
     * @param detail what lies over the box (the reason tag and where), for the log
     */
    default void sunk(StructurePlan plan, int sink, String detail) {}

    /**
     * Writes the part of the complex inside the window.
     *
     * @param decision the placed decision: the ground height, the recorded variant and the sink
     * @param probe    ground heights, readable inside the window only
     * @return the number of blocks the sink accepted
     */
    int write(StructurePlan plan, Decision decision, PopulateWindow window, WindowedSink sink, HeightProbe probe);
}
