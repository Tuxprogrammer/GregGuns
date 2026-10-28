package com.gregguns.worldgen;

import java.util.List;

/** Public door to the placement harness for other modules' scenarios (the silo complex). Dev environment only. */
public final class PlacementHarnessAccess {

    private PlacementHarnessAccess() {}

    public static StructurePopulator populator() {
        return PlacementHarness.populator();
    }

    public static List<int[]> windowsOf(StructurePlan plan, boolean reverse) {
        return PlacementHarness.windowsOf(plan, reverse);
    }
}
