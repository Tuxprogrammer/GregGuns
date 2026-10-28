package com.gregguns.entity.npc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gregguns.entity.npc.ai.EntityAIHurtByFaction;
import com.gregguns.entity.npc.ai.EntityAINpcMelee;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;
import com.gregguns.entity.npc.spawn.SpawnerData;
import com.gregguns.entity.npc.spawn.SpawnerOrigin;
import com.gregguns.mob.model.MobAnimSource;

/**
 * The Alien Bug: a fast, always-aggressive melee spider. It extends vanilla {@link EntitySpider} (so it keeps wall
 * climbing and poison immunity) but runs the same hostile {@code EntityMob} task list the other NPCs use: melee task,
 * revenge target by faction, then the nearest damageable player within 16 blocks. Unlike the spider it ignores
 * daylight and spawns in any light. Stats: 20 HP, 4 attack, speed 1.0, follow range 16, hitbox 1.1 x 1.2.
 *
 * <p>
 * Each bite sets a 10-tick attack clock that drives the jaw-snap pose through {@link #attackProgress}; the server
 * broadcasts entity status {@link #BITE_STATUS} so clients start the same clock. Both sides; the AI runs on the server.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (15. Alien Bug), ai.md (7), BUGS.md NPC-07
// Deviation: the bite uses the shared melee task (20-tick swing cooldown, reach from the hitbox) instead of the
// legacy per-tick check; no spider jockey; vanilla spider sounds (no dedicated sound assets yet); the aggro sound
// plays once per new target, not every tick (NPC-07).
public class AlienBug extends EntitySpider implements HasNpcFaction, MobAnimSource, SpawnerOrigin.NaturalCandidate {

    /** Hitbox width in blocks. */
    public static final float WIDTH = 1.1F;
    /** Hitbox height in blocks. */
    public static final float HEIGHT = 1.2F;
    /** Maximum health. */
    public static final double MAX_HEALTH = 20.0;
    /** Melee damage (attack attribute). */
    public static final double ATTACK_DAMAGE = 4.0;
    /** Movement speed attribute (the spider's is 0.8). */
    public static final double SPEED = 1.0;
    /** Target scan and follow range, in blocks. */
    public static final double FOLLOW_RANGE = 16.0;
    /** Length of the bite animation clock, in ticks. */
    public static final int BITE_TICKS = 10;
    /** Entity status byte the server broadcasts on each bite. */
    public static final byte BITE_STATUS = 4;

    private static final String PROFILE_ID = "AlienBug";
    private static final double PATH_SPEED = 1.0;
    private static final double WANDER_SPEED = 0.8;
    private static final float WATCH_RANGE = 8.0F;
    private static final float AGGRO_VOLUME = 1.0F;
    private static final float BITE_VOLUME = 0.75F;

    private int attackTimer;
    /** Set by the natural-spawn gate; never saved. */
    private boolean naturalCandidate;

    /**
     * @param world the world; the entity registry calls this when it loads a bug
     */
    public AlienBug(World world) {
        super(world);
        setSize(WIDTH, HEIGHT);
        // Drop the spider's leap and daylight-aware tasks; install the NPC set.
        tasks.taskEntries.clear();
        targetTasks.taskEntries.clear();
        tasks.addTask(1, new EntityAISwimming(this));
        tasks.addTask(4, new EntityAINpcMelee(this, PATH_SPEED, false));
        tasks.addTask(5, new EntityAIWander(this, WANDER_SPEED));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, WATCH_RANGE));
        tasks.addTask(6, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByFaction(this, false));
        targetTasks.addTask(
            2,
            new EntityAINearestAttackableTarget(
                this,
                EntityPlayer.class,
                NpcEntity.SCAN_CHANCE,
                true,
                false,
                e -> e instanceof EntityPlayer
                    && NpcTargetRules.canSeekPlayer(((EntityPlayer) e).capabilities.disableDamage)));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        // Base values only: difficulty never scales these (GGS-10-29).
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ATTACK_DAMAGE);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public NpcFaction npcFaction() {
        return NpcFaction.HOSTILE;
    }

    /** The bug spawns in any light level. */
    @Override
    protected boolean isValidLightLevel() {
        return true;
    }

    @Override
    public void markNaturalCandidate() {
        naturalCandidate = true;
    }

    /**
     * Vanilla's rules, except when a GregGuns-marked spawner places the bug: then the light checks are skipped (see
     * {@link SpawnerOrigin}). Natural spawns keep every rule.
     */
    @Override
    public boolean getCanSpawnHere() {
        if (SpawnerOrigin.placedByMarkedSpawner(this, naturalCandidate)) {
            return SpawnerOrigin.canSpawnFromSpawner(this);
        }
        return super.getCanSpawnHere();
    }

    /**
     * A GregGuns vanilla spawner places the bug: its {@code SpawnData} was just read and it has not joined the world,
     * so this is where it takes the Infernal Mobs opt-out. The bug carries no equipment and no danger.
     */
    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (SpawnerData.isFromSpawner(tag) && worldObj != null && !worldObj.isRemote) {
            InfernalHooks.markSpawnerNpc(this);
        }
    }

    /** No spider jockey and no random potion buffs from an egg or spawner. */
    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        return data;
    }

    @Override
    protected boolean canDespawn() {
        return DespawnRules.followsVanillaDespawn(NpcMob.ALIEN_BUG);
    }

    // --- loot ----------------------------------------------------------------------------------------------------

    /** @return the loot profile; the bug carries no equipment */
    protected NpcProfile npcProfile() {
        return NpcProfiles.get(PROFILE_ID);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        NpcProfile profile = npcProfile();
        if (profile != null) {
            NpcDrops.dropFewItems(this, profile, looting);
        }
    }

    // --- combat ----------------------------------------------------------------------------------------------------

    /** Plays the aggro sound once when a target is first acquired, not every tick (NPC-07). */
    @Override
    public void setAttackTarget(EntityLivingBase target) {
        boolean acquired = target != null && getAttackTarget() == null;
        super.setAttackTarget(target);
        if (acquired && !worldObj.isRemote) {
            playSound("mob.spider.say", AGGRO_VOLUME, 1.0F);
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        attackTimer = BITE_TICKS;
        if (!worldObj.isRemote) {
            worldObj.setEntityState(this, BITE_STATUS);
            playSound("mob.spider.step", BITE_VOLUME, 1.0F);
        }
        return hit;
    }

    @Override
    public void handleHealthUpdate(byte status) {
        if (status == BITE_STATUS) {
            attackTimer = BITE_TICKS;
        } else {
            super.handleHealthUpdate(status);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (attackTimer > 0) {
            attackTimer--;
        }
    }

    /** @return ticks left on the bite clock, 0 when idle */
    public int attackTimer() {
        return attackTimer;
    }

    // --- model pose ------------------------------------------------------------------------------------------------

    @Override
    public boolean isAiming() {
        return false;
    }

    @Override
    public float attackProgress(float partialTicks) {
        if (attackTimer <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, Math.max(0.0F, 1.0F - (attackTimer - partialTicks) / BITE_TICKS));
    }
}
