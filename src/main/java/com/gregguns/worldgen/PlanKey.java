package com.gregguns.worldgen;

/**
 * The identity of one plan inside a dimension's {@link PlanLedger}: size class plus grid cell. The id, rotation and
 * anchor are all derived from the seed and these three, so they add nothing. The dimension is not in the key because
 * each dimension has its own saved data (see {@link StructureState}).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10)
public final class PlanKey {

    private PlanKey() {}

    public static String of(SizeClass sizeClass, int cellX, int cellZ) {
        return sizeClass.name() + ":" + cellX + ":" + cellZ;
    }

    public static String of(StructurePlan plan) {
        return of(plan.sizeClass(), plan.cellX(), plan.cellZ());
    }
}
