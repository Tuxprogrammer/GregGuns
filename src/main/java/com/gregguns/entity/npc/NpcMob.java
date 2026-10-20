package com.gregguns.entity.npc;

/**
 * Every NPC kind GregGuns reserves a registration for: its name, its fixed mod-local entity ID and its spawn-egg
 * colors. Pure data, no Minecraft types, so the ID plan is unit-tested. IDs 61 to 79 are the mobs, in the order of the
 * Techguns catalog; 80 is the dev-only test NPC; 60 is the player turret ({@code TurretEntity}). The projectile IDs
 * (3 to 27) are never touched. The name is the Forge entity name suffix, so the full name is {@code gregguns.<Name>}
 * (kept CamelCase because Infernal Mobs configures mobs by class simple name).
 *
 * <p>
 * Usage: {@code NpcMob.ZOMBIE_SOLDIER.fullName()} for {@code EntityList} lookups and
 * {@code NpcMob.ZOMBIE_SOLDIER.langKey()} for the display name. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/mob-catalog.md (registration table), docs/gregguns-spec/10-entities-npcs
public enum NpcMob {

    ZOMBIE_SOLDIER("ZombieSoldier", 61, 0x757468, 0x38B038),
    ZOMBIE_FARMER("ZombieFarmer", 62, 0x757468, 0x38B038),
    ZOMBIE_MINER("ZombieMiner", 63, 0x757468, 0x38B038),
    ZOMBIE_PIGMAN_SOLDIER("ZombiePigmanSoldier", 64, 0xFF1111, 0x770000),
    ATTACK_HELICOPTER("AttackHelicopter", 65, 0x373D23, 0x8EC0D7),
    ALIEN_BUG("AlienBug", 66, 0xC6A96B, 0x71552E),
    CYBER_DEMON("CyberDemon", 67, 0xFF1111, 0x777777),
    ARMY_SOLDIER("ArmySoldier", 68, 0x74806E, 0x191512),
    BANDIT("Bandit", 69, 0x8F9D59, 0x2C3117),
    OUTCAST("Outcast", 70, 0x101010, 0xFF0000),
    SKELETON_SOLDIER("SkeletonSoldier", 71, 0x404040, 0xF0F0F0),
    /** The hostile turret; no egg, placed by the turret base only. */
    SIMPLE_TURRET("SimpleTurret", 72, -1, -1),
    PSYCHO_STEVE("PsychoSteve", 73, 0x757468, 0xF0F0F0),
    DICTATOR_DAVE("DictatorDave", 74, 0x8F9D59, 0xA0A000),
    COMMANDO("Commando", 75, 0x191512, 0x74806E),
    STORMTROOPER("Stormtrooper", 76, 0xF0F0F0, 0x404040),
    SUPER_MUTANT_BASIC("SuperMutantBasic", 77, 0xC6A96B, 0x71552E),
    SUPER_MUTANT_HEAVY("SuperMutantHeavy", 78, 0xC6A96B, 0x71552E),
    SUPER_MUTANT_ELITE("SuperMutantElite", 79, 0xC6A96B, 0x71552E),
    /** The base NPC, registered in the dev environment only, for scenarios and the test renderer. */
    NPC_TEST("npc_test", NpcEntity.TEST_ENTITY_ID, 0xFF00FF, 0x00FFFF);

    /** First entity ID reserved for NPC mobs. */
    public static final int FIRST_ID = 61;
    /** Last entity ID reserved for NPC mobs (the dev test NPC). */
    public static final int LAST_ID = 80;

    private final String name;
    private final int entityId;
    private final int eggBase;
    private final int eggSpot;

    NpcMob(String name, int entityId, int eggBase, int eggSpot) {
        this.name = name;
        this.entityId = entityId;
        this.eggBase = eggBase;
        this.eggSpot = eggSpot;
    }

    /**
     * @return the Forge entity name suffix, for example {@code ZombieSoldier}
     */
    public String registryName() {
        return name;
    }

    /**
     * @return the full entity name, {@code gregguns.<Name>}, as {@code EntityList} and spawners know it
     */
    public String fullName() {
        return "gregguns." + name;
    }

    /**
     * @return the fixed mod-local entity ID (never changes when modules are toggled)
     */
    public int entityId() {
        return entityId;
    }

    /**
     * @return the lang key of the mob's display name, {@code entity.gregguns.<Name>.name}
     */
    public String langKey() {
        return "entity." + fullName() + ".name";
    }

    /**
     * @return true if the mob has a spawn egg
     */
    public boolean hasEgg() {
        return eggBase != -1;
    }

    /**
     * @return the egg's base color, {@code 0xRRGGBB}, or -1 without an egg
     */
    public int eggBase() {
        return eggBase;
    }

    /**
     * @return the egg's spot color, {@code 0xRRGGBB}, or -1 without an egg
     */
    public int eggSpot() {
        return eggSpot;
    }

    /**
     * @return true for the test NPC, which only exists in the dev environment
     */
    public boolean isDevOnly() {
        return this == NPC_TEST;
    }

    /**
     * @param id an entity ID (also the spawn egg's item damage)
     * @return the mob with that ID, or null
     */
    public static NpcMob byId(int id) {
        for (NpcMob mob : values()) {
            if (mob.entityId == id) {
                return mob;
            }
        }
        return null;
    }
}
