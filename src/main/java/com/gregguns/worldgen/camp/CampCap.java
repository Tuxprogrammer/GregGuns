package com.gregguns.worldgen.camp;

import com.gregguns.utility.camp.CampSpawnPlan;

/** The NPC cap of a generated camp. */
// Spec: camp_parts.md (Flag); fixes WGEN-11 (the cap is computed once here and saved by the flag)
public final class CampCap {

    private CampCap() {}

    /**
     * @param sizeX  camp footprint on X
     * @param sizeZ  camp footprint on Z
     * @param factor {@code UtilityConfig.campFlagCapFactor}, 0.75 by default
     * @return floor(((sizeX + sizeZ) / 2) * factor): 24 for a 32 by 32 base and 59 for 79 by 79
     */
    public static int cap(int sizeX, int sizeZ, float factor) {
        // One formula in one place: the flag's own default uses the same function, so they can never disagree.
        return CampSpawnPlan.defaultCap(sizeX, sizeZ, factor);
    }
}
