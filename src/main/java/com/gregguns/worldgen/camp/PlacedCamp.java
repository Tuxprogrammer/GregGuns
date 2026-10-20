package com.gregguns.worldgen.camp;

import com.gregguns.worldgen.FlagPos;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.layout.Rotations;

/**
 * A {@link CampPlan} put in the world: an anchor (the rotated footprint's minimum corner, at the first air level above
 * the ground) and a whole-base rotation of 0..3 quarter turns. All the camp-to-world math lives here, so the flag, the
 * spawn points, the roads and the parts cannot disagree about rotation (fixes WGEN-10, where Techguns rotated the
 * footprint but not the flag offset). Pure.
 */
// Spec: camp_parts.md (Flag, Rotation); WGEN-10
public final class PlacedCamp {

    private final CampPlan plan;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final int rotation;

    PlacedCamp(CampPlan plan, int anchorX, int anchorY, int anchorZ, int rotation) {
        this.plan = plan;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.rotation = rotation & 3;
    }

    public CampPlan plan() {
        return plan;
    }

    public int anchorX() {
        return anchorX;
    }

    public int anchorY() {
        return anchorY;
    }

    public int anchorZ() {
        return anchorZ;
    }

    public int rotation() {
        return rotation;
    }

    /** Footprint width along world x after rotation. */
    public int footprintX() {
        return Rotations.width(rotation, plan.sizeX(), plan.sizeZ());
    }

    /** Footprint depth along world z after rotation. */
    public int footprintZ() {
        return Rotations.depth(rotation, plan.sizeX(), plan.sizeZ());
    }

    /** World x of a camp-local column (works for the road tails outside the footprint too). */
    public int worldX(int lx, int lz) {
        return anchorX + Rotations.offsetX(rotation, lx, lz, plan.sizeX(), plan.sizeZ());
    }

    /** World z of a camp-local column. */
    public int worldZ(int lx, int lz) {
        return anchorZ + Rotations.offsetZ(rotation, lx, lz, plan.sizeX(), plan.sizeZ());
    }

    /** Camp-local x of the world column {@code (wx, wz)}; the inverse of {@link #worldX}. */
    public int localX(int wx, int wz) {
        return Rotations.layoutX(rotation, wx - anchorX, wz - anchorZ, plan.sizeX(), plan.sizeZ());
    }

    /** Camp-local z of the world column. */
    public int localZ(int wx, int wz) {
        return Rotations.layoutZ(rotation, wx - anchorX, wz - anchorZ, plan.sizeX(), plan.sizeZ());
    }

    /**
     * The world bounds of a camp-local rectangle, as {@code {minX, minZ, maxX, maxZ}} inclusive.
     */
    public int[] worldRect(int lx, int lz, int w, int d) {
        int ax = worldX(lx, lz);
        int az = worldZ(lx, lz);
        int bx = worldX(lx + w - 1, lz + d - 1);
        int bz = worldZ(lx + w - 1, lz + d - 1);
        return new int[] { Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz) };
    }

    /** The flag's world position (WGEN-10: rotated with the footprint). */
    public FlagPos flagPos() {
        return new FlagPos(worldX(plan.flagX(), plan.flagZ()), anchorY, worldZ(plan.flagX(), plan.flagZ()));
    }

    /** Flag position minus the footprint's minimum corner, X. This is {@code TileCampFlag}'s offset after rotation. */
    public int flagOffsetX() {
        return flagPos().x - anchorX;
    }

    public int flagOffsetY() {
        return 0;
    }

    public int flagOffsetZ() {
        return flagPos().z - anchorZ;
    }

    /** Spawn points as offsets from the footprint's minimum corner, rotated like the flag (flat x, y, z triples). */
    public int[] spawnOffsets() {
        int[] local = plan.spawnPoints();
        int[] out = new int[local.length];
        for (int i = 0; i + 2 < local.length; i += 3) {
            out[i] = worldX(local[i], local[i + 2]) - anchorX;
            out[i + 1] = local[i + 1];
            out[i + 2] = worldZ(local[i], local[i + 2]) - anchorZ;
        }
        return out;
    }

    /** The flag's init data for this camp at the given cap factor. */
    public FlagSpec flagSpec(float capFactor) {
        FlagPos f = flagPos();
        return new FlagSpec(
            f.x,
            f.y,
            f.z,
            footprintX(),
            footprintZ(),
            flagOffsetX(),
            flagOffsetY(),
            flagOffsetZ(),
            spawnOffsets(),
            CampPlan.BASE_NPC_NAMES,
            CampCap.cap(footprintX(), footprintZ(), capFactor));
    }

    /** Smallest world x any write of this camp can touch (the road tails reach past the fence). */
    public int boundsMinX() {
        return anchorX - CampPlan.ROAD_TAIL;
    }

    public int boundsMinZ() {
        return anchorZ - CampPlan.ROAD_TAIL;
    }

    public int boundsMaxX() {
        return anchorX + footprintX() - 1 + CampPlan.ROAD_TAIL;
    }

    public int boundsMaxZ() {
        return anchorZ + footprintZ() - 1 + CampPlan.ROAD_TAIL;
    }

    /** True when any write of this camp can land in the window. */
    public boolean overlaps(PopulateWindow window) {
        return window.overlapsBox(boundsMinX(), boundsMinZ(), boundsMaxX(), boundsMaxZ());
    }
}
