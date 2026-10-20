package com.gregguns.entity.npc;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * Psycho Steve, the early-game mini-boss: 75 health, melee 7, very fast (0.60), follow range 60, 25 base XP. He swings
 * a chainsaw in the red Miner armor and never despawns. He is an {@link IBossDisplayData}, and the client draws the
 * vanilla boss bar for him (see {@code BossBarHandler}), which fixes NPC-22.
 *
 * <p>
 * Combat: the chainsaw is a tool gun with AI range 3, so the gun task walks him up to melee range and calls
 * {@link #attackEntityWithRangedAttack}. He answers with a vanilla melee strike at his attack damage instead of a
 * projectile. The hit deals exactly {@link #ATTACK_DAMAGE}: the vanilla melee path would add the chainsaw's own
 * attribute modifier on top (19 instead of 7). Both sides; combat is server side.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (section 12), docs/techguns-spec/BUGS.md (NPC-22, NPC-28)
public class PsychoSteve extends NpcEntity implements IBossDisplayData {

    /** Maximum health. */
    public static final double MAX_HEALTH = 75.0;
    /** Melee damage in half hearts. */
    public static final double ATTACK_DAMAGE = 7.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.60;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 60.0;
    /** Base experience. */
    public static final int XP = 25;
    /** Distance in blocks within which the chainsaw strike lands. */
    public static final double REACH = 3.5;

    /**
     * @param world the world; the entity registry calls this when it loads him
     */
    public PsychoSteve(World world) {
        super(world);
        experienceValue = XP;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ATTACK_DAMAGE);
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get("PsychoSteve");
    }

    @Override
    protected boolean isPersistentNpc() {
        return true;
    }

    /** The chainsaw strike: a melee hit when the target is in reach, nothing otherwise. */
    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float factor) {
        if (worldObj.isRemote || getDistanceToEntity(target) > REACH) {
            return;
        }
        swingItem();
        target.attackEntityFrom(DamageSource.causeMobDamage(this), (float) ATTACK_DAMAGE);
    }
}
