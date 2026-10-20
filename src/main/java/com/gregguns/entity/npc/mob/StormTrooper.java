package com.gregguns.entity.npc.mob;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * The Stormtrooper: a soldier in the full T3 HEV suit (silver camo) that carries the Blaster Rifle or the Laser Rifle.
 * Gun AI, gear and drops come from {@link NpcEntity} and the {@code Stormtrooper} entry of {@link NpcProfiles}; this
 * class only sets the stats. Energy guns draw on {@code ChargeSource.INFINITE}. Egg and camp only (no natural spawn).
 * The class name keeps Techguns' spelling; the registry name is {@code Stormtrooper}. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (10. Stormtrooper), docs/gregguns-spec/10-entities-npcs/index.md
public class StormTrooper extends NpcEntity {

    /** Registry name key in {@link NpcProfiles}. */
    public static final String PROFILE = "Stormtrooper";
    /** Maximum health in half hearts. */
    public static final double HEALTH = 35.0;
    /** Melee damage in half hearts. */
    public static final double MELEE_DAMAGE = 4.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.30;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 75.0;

    /**
     * @param world the world; the entity registry calls this when it loads a StormTrooper
     */
    public StormTrooper(World world) {
        super(world);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        // Base values only: difficulty never scales these (GGS-10-29).
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(MELEE_DAMAGE);
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get(PROFILE);
    }
}
