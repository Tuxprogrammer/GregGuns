package com.gregguns.entity.npc.mob;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * A fire-immune zombie pigman in arctic combat armor whose weapon pool grows with difficulty (up to flamethrower, LMG
 * and rocket launcher). Stats 25.0 health, 5.0 melee, speed 0.30, follow range 50.0. It has no natural spawn under
 * design record 0005: eggs and commands only. Gear and drops come from its {@link NpcProfiles} entry. Both sides.
 *
 * <p>
 * Deviation: the creature attribute is {@code UNDEAD} (Techguns left it {@code UNDEFINED}).
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (4. Zombie Pigman Soldier),
// docs/gregguns-spec/10-entities-npcs/spawning.md
public class ZombiePigmanSoldier extends NpcEntity {

    /** Registry name key in {@link NpcProfiles}. */
    public static final String PROFILE = "ZombiePigmanSoldier";
    /** Max health. */
    public static final double HEALTH = 25.0;
    /** Melee damage. */
    public static final double ATTACK = 5.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.30;
    /** Follow range in blocks. */
    public static final double FOLLOW = 50.0;

    private static final float STEP_VOLUME = 0.15F;

    /**
     * @param world the world
     */
    public ZombiePigmanSoldier(World world) {
        super(world);
        isImmuneToFire = true;
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
        return "mob.zombiepig.zpig";
    }

    @Override
    protected String getHurtSound() {
        return "mob.zombiepig.zpighurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.zombiepig.zpigdeath";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        super.func_145780_a(x, y, z, block);
        playSound("mob.zombie.step", STEP_VOLUME, 1.0F);
    }
}
