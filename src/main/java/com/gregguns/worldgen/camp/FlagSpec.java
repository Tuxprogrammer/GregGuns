package com.gregguns.worldgen.camp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything {@code TileCampFlag.init} needs, in world terms: where the flag stands, the footprint, offsets from the
 * footprint's minimum corner, spawn offsets, NPC names and the final cap. The Military Base builds one from its
 * {@link PlacedCamp}; the Aircraft Carrier (S3) builds its own with {@link #of}. Immutable.
 */
// Spec: camp_parts.md (Flag); PLAN-M8 section 2 (carrier: ArmySoldier + Commando)
public final class FlagSpec {

    public final int x;
    public final int y;
    public final int z;
    public final int sizeX;
    public final int sizeZ;
    public final int offsetX;
    public final int offsetY;
    public final int offsetZ;
    private final int[] spawnPoints;
    private final List<String> names;
    public final int cap;

    public FlagSpec(int x, int y, int z, int sizeX, int sizeZ, int offsetX, int offsetY, int offsetZ, int[] spawnPoints,
        List<String> names, int cap) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.spawnPoints = spawnPoints.clone();
        this.names = Collections.unmodifiableList(new ArrayList<>(names));
        this.cap = cap;
    }

    /**
     * For a structure that is not the Military Base: the flag at world {@code (x, y, z)} in a footprint whose minimum
     * corner is {@code (originX, originY, originZ)}; the cap is computed from the footprint and the factor.
     */
    public static FlagSpec of(int x, int y, int z, int originX, int originY, int originZ, int sizeX, int sizeZ,
        int[] spawnOffsets, List<String> names, float capFactor) {
        return new FlagSpec(
            x,
            y,
            z,
            sizeX,
            sizeZ,
            x - originX,
            y - originY,
            z - originZ,
            spawnOffsets,
            names,
            CampCap.cap(sizeX, sizeZ, capFactor));
    }

    public int[] spawnPoints() {
        return spawnPoints.clone();
    }

    public List<String> names() {
        return names;
    }
}
