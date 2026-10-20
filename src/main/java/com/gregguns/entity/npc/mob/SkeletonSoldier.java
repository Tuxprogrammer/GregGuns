package com.gregguns.entity.npc.mob;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * A skeleton in light combat gear that fires a revolver, Thompson or hand cannon. Stats 25.0 health, 4.0 melee,
 * speed 0.25, follow range 50.0. Gear and drops come from its {@link NpcProfiles} entry. Like the Techguns mob it is a
 * plain {@link NpcEntity}, so it does not burn in daylight and never shoots arrows.
 *
 * <p>
 * Deviation: the creature attribute is {@code UNDEAD} (Techguns left it {@code UNDEFINED}). Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (5. Skeleton Soldier),
// docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md
public class SkeletonSoldier extends NpcEntity {

    /** Registry name key in {@link NpcProfiles}. */
    public static final String PROFILE = "SkeletonSoldier";
    /** Max health. */
    public static final double HEALTH = 25.0;
    /** Melee damage. */
    public static final double ATTACK = 4.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.25;
    /** Follow range in blocks. */
    public static final double FOLLOW = 50.0;

    private static final float STEP_VOLUME = 0.15F;

    /**
     * @param world the world
     */
    public SkeletonSoldier(World world) {
        super(world);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(HEALTH);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ATTACK);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(FOLLOW);
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get(PROFILE);
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected String getLivingSound() {
        return "mob.skeleton.say";
    }

    @Override
    protected String getHurtSound() {
        return "mob.skeleton.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.skeleton.death";
    }

    /** The skeleton step replaces the block step sound (spec). */
    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        playSound("mob.skeleton.step", STEP_VOLUME, 1.0F);
    }
}
