package com.gregguns.entity.npc;

/**
 * The numbers of the three Super Mutant tiers: health, melee damage, speed, vanilla armor value, model scale and
 * hitbox. Pure data, no Minecraft types, so the table is unit-tested against the spec.
 *
 * <p>
 * Hitbox: height is {@code 1.8 * scale} as in Techguns. Width is {@code 0.6 * scale}; Techguns left it at 0.6 for a
 * model up to 1.65 times larger (NPC-25), so a mutant's arms stuck out of its hitbox. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (14. Super Mutants), BUGS.md NPC-25
public enum SuperMutantTier {

    BASIC(NpcMob.SUPER_MUTANT_BASIC, 30.0, 7.0, 0.30, 7, 1.35F),
    HEAVY(NpcMob.SUPER_MUTANT_HEAVY, 40.0, 9.0, 0.25, 12, 1.50F),
    ELITE(NpcMob.SUPER_MUTANT_ELITE, 50.0, 11.0, 0.20, 10, 1.65F);

    /** Follow range of every tier, in blocks. */
    public static final double FOLLOW_RANGE = 50.0;
    /** Hitbox width of a scale-1 biped, in blocks. */
    public static final float BASE_WIDTH = 0.6F;
    /** Hitbox height of a scale-1 biped, in blocks. */
    public static final float BASE_HEIGHT = 1.8F;

    private final NpcMob mob;
    private final double health;
    private final double damage;
    private final double speed;
    private final int armor;
    private final float scale;

    SuperMutantTier(NpcMob mob, double health, double damage, double speed, int armor, float scale) {
        this.mob = mob;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.armor = armor;
        this.scale = scale;
    }

    /** @return the NPC kind of this tier */
    public NpcMob mob() {
        return mob;
    }

    /** @return the loot profile id, which is the registry name */
    public String profileId() {
        return mob.registryName();
    }

    /** @return maximum health in half hearts */
    public double health() {
        return health;
    }

    /** @return melee attack damage in half hearts */
    public double damage() {
        return damage;
    }

    /** @return the movement speed attribute */
    public double speed() {
        return speed;
    }

    /** @return the vanilla armor value (points) */
    public int armor() {
        return armor;
    }

    /** @return the model scale */
    public float scale() {
        return scale;
    }

    /** @return the hitbox width in blocks (NPC-25 fix: scaled with the model) */
    public float width() {
        return BASE_WIDTH * scale;
    }

    /** @return the hitbox height in blocks */
    public float height() {
        return BASE_HEIGHT * scale;
    }
}
