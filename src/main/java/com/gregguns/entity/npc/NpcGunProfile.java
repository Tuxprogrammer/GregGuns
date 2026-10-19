package com.gregguns.entity.npc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunStats;

/**
 * How an NPC fires one gun: range, time between bursts, burst size and the delay inside a burst. Pure data, no
 * Minecraft types.
 *
 * <p>
 * The profile of every M2 gun is read from its {@link GunStats} AI fields (set with {@code .ai(...)} in the gun's
 * definition), so there is one source of truth; the test pins them to the Techguns table. Guns defined later (the M5
 * energy guns, the chainsaw) plug in through {@link #register}: call it once at startup with the gun's registry name.
 * A burst count of 0 means a single shot per attack.
 *
 * <p>
 * Spec: docs/techguns-spec/10-entities-npcs/ai.md section 3.
 */
public final class NpcGunProfile {

    /** The Techguns defaults for a gun that sets no AI stats: range 15, attack time 60, no burst. */
    public static final NpcGunProfile DEFAULT = new NpcGunProfile(15, 60, 0, 0);

    private static final Map<String, NpcGunProfile> EXTRA = new LinkedHashMap<>();

    private final double range;
    private final int attackTime;
    private final int burstCount;
    private final int burstDelay;

    /**
     * @param range      attack range in blocks (greater than 0)
     * @param attackTime maximum ticks between bursts (greater than 0)
     * @param burstCount shots per burst; 0 or 1 means a single shot
     * @param burstDelay ticks between shots inside a burst
     */
    public NpcGunProfile(double range, int attackTime, int burstCount, int burstDelay) {
        if (range <= 0 || attackTime <= 0 || burstCount < 0 || burstDelay < 0) {
            throw new IllegalArgumentException("invalid NPC gun profile");
        }
        this.range = range;
        this.attackTime = attackTime;
        this.burstCount = burstCount;
        this.burstDelay = burstDelay;
    }

    /** The profile carried by a gun's AI stats. */
    public static NpcGunProfile of(GunStats stats) {
        return new NpcGunProfile(stats.aiRange(), stats.aiAttackTime(), stats.aiBurstCount(), stats.aiBurstDelay());
    }

    /**
     * Extension point for guns that are not in {@link GunDefinitions}: registers the profile for a registry name
     * (without the mod id). A later call for the same name replaces the earlier one.
     */
    public static synchronized void register(String gunName, NpcGunProfile profile) {
        EXTRA.put(Objects.requireNonNull(gunName), Objects.requireNonNull(profile));
    }

    /**
     * @param gunName a gun's registry name without the mod id, for example {@code "m4"}
     * @return its profile: the registered one, else the one from its definition, else null if the gun is unknown
     */
    public static synchronized NpcGunProfile forGun(String gunName) {
        NpcGunProfile extra = EXTRA.get(gunName);
        if (extra != null) {
            return extra;
        }
        GunDefinition def = GunDefinitions.byName(gunName);
        return def == null ? null : of(def.stats());
    }

    /** Like {@link #forGun} but falls back to {@link #DEFAULT} for an unknown gun. */
    public static NpcGunProfile forGunOrDefault(String gunName) {
        NpcGunProfile profile = forGun(gunName);
        return profile == null ? DEFAULT : profile;
    }

    /** The profiles registered through {@link #register}, for tests and diagnostics. */
    public static synchronized Map<String, NpcGunProfile> registered() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(EXTRA));
    }

    /** Removes every registered profile. Tests only. */
    static synchronized void clearRegistered() {
        EXTRA.clear();
    }

    /** Attack range in blocks. */
    public double range() {
        return range;
    }

    /** Maximum ticks between bursts, reached at the edge of range. */
    public int attackTime() {
        return attackTime;
    }

    /** Shots per burst; 0 means a single shot. */
    public int burstCount() {
        return burstCount;
    }

    /** Ticks between shots inside a burst. */
    public int burstDelay() {
        return burstDelay;
    }

    /** Minimum ticks between bursts, reached point-blank: {@code attackTime / 3} (integer division). */
    public int variance() {
        return attackTime / 3;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NpcGunProfile)) {
            return false;
        }
        NpcGunProfile p = (NpcGunProfile) o;
        return Double.compare(range, p.range) == 0 && attackTime == p.attackTime
            && burstCount == p.burstCount
            && burstDelay == p.burstDelay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(range, attackTime, burstCount, burstDelay);
    }

    @Override
    public String toString() {
        return "NpcGunProfile[range=" + range
            + ", attackTime="
            + attackTime
            + ", burst="
            + burstCount
            + ", burstDelay="
            + burstDelay
            + "]";
    }
}
