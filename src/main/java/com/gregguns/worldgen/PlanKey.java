package com.gregguns.worldgen;

/**
 * The identity of one plan inside a dimension's {@link PlanLedger}: structure id, size class and grid cell. The
 * rotation and anchor derive from the seed and the cell. The structure id is in the key because two runs with different
 * pools in one world can put different structures in one cell, and one must not reuse the other's decision. The
 * dimension is not in the key because
 * each dimension has its own saved data (see {@link StructureState}).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10)
public final class PlanKey {

    private PlanKey() {}

    public static String of(String structureId, SizeClass sizeClass, int cellX, int cellZ) {
        return structureId + ":" + sizeClass.name() + ":" + cellX + ":" + cellZ;
    }

    public static String of(StructurePlan plan) {
        return of(plan.id(), plan.sizeClass(), plan.cellX(), plan.cellZ());
    }
}
