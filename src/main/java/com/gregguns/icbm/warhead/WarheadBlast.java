package com.gregguns.icbm.warhead;

import com.gregguns.icbm.explosive.BlastShape;
import com.gregguns.icbm.explosive.ExplosiveTier;

/**
 * The T1 and T2 blast types kept by D10: their id, tier, shape, full-casing radius and center damage, and which effect
 * they run. The numbers are the balance table of {@code blast-types.md}. Only types marked as damaging carry a center
 * damage; gas, push, fire and utility types are balanced on their effect. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md
public enum WarheadBlast {

    /** Plain high explosive: a crater and a damage ramp. */
    CONDENSED("condensed", ExplosiveTier.T1, BlastShape.RAY_GRID, 9.0, 1200.0, Kind.NONE),
    /** Fragments: a smaller crater, and everything in the zone is set alight. */
    SHRAPNEL("shrapnel", ExplosiveTier.T1, BlastShape.RAY_GRID, 10.0, 1200.0, Kind.SHRAPNEL),
    /** Anvil fragments: a heavy blast that also slows everything it hits. */
    ANVIL("anvil", ExplosiveTier.T1, BlastShape.RAY_GRID, 8.0, 1200.0, Kind.ANVIL),
    /** Fire: sets the ground and everything in the zone alight; no crater. */
    INCENDIARY("incendiary", ExplosiveTier.T1, BlastShape.NONE, 12.0, 0.0, Kind.FIRE),
    /** Gas that weakens and slows. */
    DEBILITATION("debilitation", ExplosiveTier.T1, BlastShape.NONE, 12.0, 0.0, Kind.GAS_DEBILITATION),
    /** Poison gas. */
    CHEMICAL("chemical", ExplosiveTier.T1, BlastShape.NONE, 12.0, 0.0, Kind.GAS_CHEMICAL),
    /** A marker cloud; harmless. */
    SMOKE("smoke", ExplosiveTier.T1, BlastShape.NONE, 10.0, 0.0, Kind.SMOKE),
    /** Throws every entity in the zone away from the center; no crater. */
    REPULSIVE("repulsive", ExplosiveTier.T1, BlastShape.NONE, 12.0, 0.0, Kind.REPEL),
    /** Pulls every entity in the zone toward the center; no crater. */
    ATTRACTIVE("attractive", ExplosiveTier.T1, BlastShape.NONE, 12.0, 0.0, Kind.ATTRACT),
    /** Fragmentation: a larger blast with a burning zone. */
    FRAGMENTATION("fragmentation", ExplosiveTier.T2, BlastShape.RAY_SPHERE, 14.0, 3000.0, Kind.SHRAPNEL),
    /** Gas with a stronger poison. */
    CONTAGIOUS("contagious", ExplosiveTier.T2, BlastShape.NONE, 14.0, 0.0, Kind.GAS_CONTAGIOUS),
    /** Shatters glass and stuns everything in the zone; no crater (the shape is NONE). */
    SONIC("sonic", ExplosiveTier.T2, BlastShape.NONE, 14.0, 0.0, Kind.SONIC),
    /** Bores a narrow tunnel straight down. */
    BREACHING("breaching", ExplosiveTier.T2, BlastShape.NONE, 12.0, 0.0, Kind.BREACH),
    /** The big conventional blast: the widest crater of the tier. */
    THERMOBARIC("thermobaric", ExplosiveTier.T2, BlastShape.RAY_SPHERE, 20.0, 3000.0, Kind.NONE),
    /** The hot T3 blast: a wide crater, everything in it burns. */
    EXOTHERMIC("exothermic", ExplosiveTier.T3, BlastShape.RAY_SPHERE, 28.0, 8000.0, Kind.SHRAPNEL),
    /** The cold T3 blast: a wide crater, everything in it is slowed to a crawl. */
    ENDOTHERMIC("endothermic", ExplosiveTier.T3, BlastShape.RAY_SPHERE, 28.0, 8000.0, Kind.ANVIL),
    /** The nuclear warhead (ZPM): the widest crater, annihilation in the kill zone, fallout. */
    NUCLEAR("nuclear", ExplosiveTier.T3, BlastShape.RAY_SPHERE, 64.0, 8000.0, Kind.NUCLEAR);

    /** Which effect class runs after the engine's terrain and damage work. */
    public enum Kind {
        /** No extra effect. */
        NONE,
        /** Fragments: sets entities alight. */
        SHRAPNEL,
        /** Anvil fragments: slows. */
        ANVIL,
        /** Fire on the ground and on entities. */
        FIRE,
        /** Weakness, slowness and nausea cloud. */
        GAS_DEBILITATION,
        /** Poison cloud. */
        GAS_CHEMICAL,
        /** Strong poison and hunger cloud. */
        GAS_CONTAGIOUS,
        /** Visual cloud only. */
        SMOKE,
        /** Pushes entities out. */
        REPEL,
        /** Pulls entities in. */
        ATTRACT,
        /** Shatters glass, stuns. */
        SONIC,
        /** A vertical tunnel. */
        BREACH,
        /** Annihilation in the kill zone (set on the definition) and fallout when it ends. */
        NUCLEAR
    }

    private final String id;
    private final ExplosiveTier tier;
    private final BlastShape shape;
    private final double fullRadius;
    private final double fullDamage;
    private final Kind kind;

    WarheadBlast(String id, ExplosiveTier tier, BlastShape shape, double fullRadius, double fullDamage, Kind kind) {
        this.id = id;
        this.tier = tier;
        this.shape = shape;
        this.fullRadius = fullRadius;
        this.fullDamage = fullDamage;
        this.kind = kind;
    }

    /** @return the registry id, the warhead NBT value */
    public String id() {
        return id;
    }

    /** @return the tier, which is also the casing tier that carries it */
    public ExplosiveTier tier() {
        return tier;
    }

    /** @return how terrain is removed */
    public BlastShape shape() {
        return shape;
    }

    /** @return the radius with a full casing, blocks */
    public double fullRadius() {
        return fullRadius;
    }

    /** @return the center damage with a full casing, half hearts; 0 for types balanced on their effect */
    public double fullDamage() {
        return fullDamage;
    }

    /** @return which effect runs */
    public Kind kind() {
        return kind;
    }

    /** @return the fill cap, the cap of this type's casing */
    public int fillCap() {
        return WarheadCasing.forTier(tier)
            .fillCap();
    }

    /** @return true when the type does center damage and so must meet its tier's D34 target at full fill */
    public boolean damaging() {
        return fullDamage > 0.0;
    }

    /**
     * @param id a registry id
     * @return the blast with that id, or {@code null}
     */
    public static WarheadBlast byId(String id) {
        for (WarheadBlast b : values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }
}
