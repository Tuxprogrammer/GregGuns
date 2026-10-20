package com.gregguns.entity.npc.mob;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.NpcEntity;

/**
 * Shared base of the three zombie NPCs (Soldier, Farmer, Miner): the zombie sounds, the undead attribute and a stats
 * setter. They are {@link NpcEntity} subclasses, so they never burn in daylight (the base is not a vanilla
 * {@code EntityZombie}); Techguns zombies do not burn either.
 *
 * <p>
 * Deviation: the creature attribute is {@code UNDEAD} (Techguns left it {@code UNDEFINED}) so Smite and healing
 * potions treat them as the zombies they look like. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (1. Zombie Soldier; sounds, no daylight burning)
public abstract class ZombieNpc extends NpcEntity {

    private static final float STEP_VOLUME = 0.15F;

    /**
     * @param world the world
     */
    protected ZombieNpc(World world) {
        super(world);
    }

    /**
     * Sets the base stats of a zombie kind.
     *
     * @param health max health in half-hearts
     * @param attack melee damage
     * @param speed  movement speed attribute
     * @param follow follow range in blocks
     */
    protected final void applyStats(double health, double attack, double speed, double follow) {
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(health);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(attack);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(speed);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(follow);
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected String getLivingSound() {
        return "mob.zombie.say";
    }

    @Override
    protected String getHurtSound() {
        return "mob.zombie.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.zombie.death";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        super.func_145780_a(x, y, z, block);
        playSound("mob.zombie.step", STEP_VOLUME, 1.0F);
    }
}
