package com.gregguns.icbm.worldgen;

/**
 * The numbers behind one {@link ActivityLevel}: how much of the complex is damaged, how many guards stand in it and how
 * rich its chests are. All config-tunable starting values (D36); the orchestrator tunes them in I7. Immutable, pure.
 *
 * <p>
 * Chances are 0 to 1. "Section" means a room, a tunnel stretch or a silo shaft; "depth band" is how far the section
 * lies from the central hall: 0 the hall itself, 1 the first ring of rooms and tunnels, 2 and up deeper.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (GGI-08-20..30); D36
public final class ActivityProfile {

    private static final ActivityProfile ABANDONED = new ActivityProfile(
        ActivityLevel.ABANDONED,
        0.30,
        0.12,
        0.10,
        0.12,
        0.05,
        0.18,
        0.35,
        true,
        new double[] { 0, 0, 0, 0 },
        0.0,
        0,
        0.08,
        0.25);
    private static final ActivityProfile PARTLY = new ActivityProfile(
        ActivityLevel.PARTLY_ABANDONED,
        0.10,
        0.04,
        0.04,
        0.03,
        0.45,
        0.50,
        0.45,
        true,
        new double[] { 0.10, 0.20, 0.45, 0.65 },
        0.0,
        1,
        0.10,
        0.40);
    private static final ActivityProfile OPERATIONAL = new ActivityProfile(
        ActivityLevel.OPERATIONAL,
        0.0,
        0.0,
        0.01,
        0.0,
        1.0,
        0.0,
        0.0,
        false,
        new double[] { 0.50, 0.75, 1.0, 1.0 },
        1.0,
        2,
        0.25,
        0.60);

    private final ActivityLevel level;
    private final double collapseChance;
    private final double breachChance;
    private final double rubbleChance;
    private final double holeChance;
    private final double litChance;
    private final double brokenCasingChance;
    private final double deadMachineChance;
    private final boolean flooded;
    private final double[] spawnerByDepth;
    private final double shaftSpawnerChance;
    private final int richness;
    private final double chipChance;
    private final double siloPartChance;

    private ActivityProfile(ActivityLevel level, double collapse, double breach, double rubble, double hole, double lit,
        double brokenCasing, double deadMachine, boolean flooded, double[] spawnerByDepth, double shaftSpawner,
        int richness, double chip, double siloPart) {
        this.level = level;
        this.collapseChance = collapse;
        this.breachChance = breach;
        this.rubbleChance = rubble;
        this.holeChance = hole;
        this.litChance = lit;
        this.brokenCasingChance = brokenCasing;
        this.deadMachineChance = deadMachine;
        this.flooded = flooded;
        this.spawnerByDepth = spawnerByDepth;
        this.shaftSpawnerChance = shaftSpawner;
        this.richness = richness;
        this.chipChance = chip;
        this.siloPartChance = siloPart;
    }

    /**
     * @param level a level
     * @return its profile
     */
    public static ActivityProfile of(ActivityLevel level) {
        switch (level) {
            case PARTLY_ABANDONED:
                return PARTLY;
            case OPERATIONAL:
                return OPERATIONAL;
            default:
                return ABANDONED;
        }
    }

    /** @return the level this profile describes */
    public ActivityLevel level() {
        return level;
    }

    /**
     * @return chance that a whole room or tunnel stretch is collapsed (filled with rubble, passable only by digging)
     */
    public double collapseChance() {
        return collapseChance;
    }

    /** @return per damage-eligible wall cell, the chance of a breach (a hole through the wall) */
    public double breachChance() {
        return breachChance;
    }

    /** @return per damage-eligible floor or ceiling-adjacent cell, the chance of rubble lying there */
    public double rubbleChance() {
        return rubbleChance;
    }

    /** @return per damage-eligible floor cell, the chance of a hole (the floor is simply gone) */
    public double holeChance() {
        return holeChance;
    }

    /** @return per light mark, the chance it is lit (unlit lights are removed, never placed dark) */
    public double litChance() {
        return litChance;
    }

    /** @return per machine casing or hull cell, the chance it is broken (replaced by rubble or left out) */
    public double brokenCasingChance() {
        return brokenCasingChance;
    }

    /** @return per machine slot, the chance the machine is a dead husk (a casing, not a working machine) */
    public double deadMachineChance() {
        return deadMachineChance;
    }

    /** @return true when the pump room is flooded */
    public boolean flooded() {
        return flooded;
    }

    /**
     * @param depthBand 0 hall, 1 first ring, 2 deeper, 3 deepest (values above 3 count as 3)
     * @return the chance that a guard-post mark at that depth holds a spawner
     */
    public double spawnerChance(int depthBand) {
        return spawnerByDepth[Math.max(0, Math.min(3, depthBand))];
    }

    /** @return the chance that a silo shaft or the control room mark holds a spawner (operational only) */
    public double shaftSpawnerChance() {
        return shaftSpawnerChance;
    }

    /** @return chest richness 0 to 2 ({@code LootDraw} semantics: more picks, heavier top-tier lines) */
    public int richness() {
        return richness;
    }

    /** @return chance a chest in the comms or office holds the schematic chip */
    public double chipChance() {
        return chipChance;
    }

    /** @return chance the silo's missing part is lying in a chest of the complex */
    public double siloPartChance() {
        return siloPartChance;
    }
}
