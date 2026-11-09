package com.gregguns.worldgen.camp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything {@code TileCampFlag.init} needs, in world terms: where the flag stands, the footprint, offsets from the
 * footprint's minimum corner, spawn offsets, NPC names and the final cap, plus the base's
 * faction (the flag cloth) and, on a red base, the commander and his post (D353). The Military Base builds one from its
 * {@link PlacedCamp}; the Aircraft Carrier (S3) builds its own with {@link #of}, a blue flag with no commander.
 * Immutable.
 */
// Spec: camp_parts.md (Flag); PLAN-M8 section 2 (carrier: ArmySoldier + Commando); D353 (faction and commander)
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
    /** The flag cloth's side; blue unless {@link #withCommand} says otherwise. */
    public final CampFaction faction;
    /** The commander's entity name, or null for a base without one. */
    public final String commander;
    private final int[] post;

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
        this.faction = CampFaction.BLUE;
        this.commander = null;
        this.post = new int[3];
    }

    private FlagSpec(FlagSpec base, CampFaction faction, String commander, int[] post) {
        this.x = base.x;
        this.y = base.y;
        this.z = base.z;
        this.sizeX = base.sizeX;
        this.sizeZ = base.sizeZ;
        this.offsetX = base.offsetX;
        this.offsetY = base.offsetY;
        this.offsetZ = base.offsetZ;
        this.spawnPoints = base.spawnPoints;
        this.names = base.names;
        this.cap = base.cap;
        this.faction = faction;
        this.commander = commander;
        this.post = post.clone();
    }

    /**
     * @param faction   the base's side
     * @param commander the commander's entity name, or null for none
     * @param post      the commander's post as {@code {dx, dy, dz}} from the flag
     * @return a copy with the faction and the commander set
     */
    public FlagSpec withCommand(CampFaction faction, String commander, int[] post) {
        return new FlagSpec(this, faction, commander, post);
    }

    /** @return the commander's post as {@code {dx, dy, dz}} from the flag */
    public int[] post() {
        return post.clone();
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

    /**
     * As {@link #of} with a fixed cap instead of one computed from the footprint (the carrier holds nine whatever its
     * size).
     */
    public static FlagSpec ofCap(int x, int y, int z, int originX, int originY, int originZ, int sizeX, int sizeZ,
        int[] spawnOffsets, List<String> names, int cap) {
        return new FlagSpec(x, y, z, sizeX, sizeZ, x - originX, y - originY, z - originZ, spawnOffsets, names, cap);
    }

    public int[] spawnPoints() {
        return spawnPoints.clone();
    }

    public List<String> names() {
        return names;
    }
}
