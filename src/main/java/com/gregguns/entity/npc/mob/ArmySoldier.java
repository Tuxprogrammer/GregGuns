package com.gregguns.entity.npc.mob;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * The Army Soldier: a soldier in T2 combat armor that carries an M4, combat shotgun or bolt-action rifle. Gun AI, gear
 * and drops come from {@link NpcEntity} and the {@code ArmySoldier} entry of {@link NpcProfiles}; this class
 * only sets the stats. Camps and eggs only (no natural spawn). Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (ArmySoldier), docs/gregguns-spec/10-entities-npcs/index.md
public class ArmySoldier extends NpcEntity {

    /** Registry name key in {@link NpcProfiles}. */
    public static final String PROFILE = "ArmySoldier";
    /** Maximum health in half hearts. */
    public static final double HEALTH = 30.0;
    /** Melee damage in half hearts. */
    public static final double MELEE_DAMAGE = 4.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.30;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 75.0;

    /**
     * @param world the world; the entity registry calls this when it loads a Army Soldier
     */
    public ArmySoldier(World world) {
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
