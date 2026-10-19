package com.gregguns.gun.def;

/**
 * Shared numbers of the EV energy guns (Laser, Tesla and Sonic Rifle). Pure constants, both sides.
 */
// Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (capacity table); plan M5 Q9
final class EvEnergy {

    /**
     * Capacity of a full charge in EU: GT's {@code BatteryHull_EV_Full}, the EV baseline of the capacity table. Each
     * gun spends {@code CAPACITY_EU / clip} per pull, so a full charge is the Techguns clip.
     */
    static final long CAPACITY_EU = 6_400_000L;

    private EvEnergy() {}
}
