package com.gregguns.entity.npc;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.gregguns.combat.TurretAttacker;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.NpcFiring;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.sound.GunSoundPlayer;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.projectile.Projectiles;
import com.gregguns.utility.turret.entity.TurretDisplay;
import com.gregguns.utility.turret.entity.TurretEntity;

/**
 * The hostile camp turret: a 30 HP creature that never moves, carries a fixed gun with unlimited ammo and shoots the
 * nearest valid target in sight, chosen by {@link NpcTurretRules}. It is a member of the camp faction, so it never
 * shoots camp NPCs. It is not an {@code IMob} and not {@code IAnimals} (Infernal Mobs leaves it alone) and has no
 * drops and no experience. It is persistent: camps keep their turret. It fires through the projectile framework as a
 * {@link TurretAttacker}, so its hits on players use the turret damage factor. It draws with the player turret's head
 * model and the iron plate sheet. Both sides; the behavior runs on the server.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (17. Turret), docs/techguns-spec/10-entities-npcs/ai.md (5)
// Deviation: it has no base block, ammo or power, and no retaliation task (like TurretEntity, it fires at what the
// target rules choose); it targets players and enemy factions instead of every IMob.
public class NpcTurret extends EntityCreature implements HasNpcFaction, TurretAttacker, TurretDisplay {

    private static final double EYE_HEIGHT = 0.8;
    private static final float WIDTH = 0.6F;
    private static final float HEIGHT = 1.0F;

    private int scanCountdown;
    private int attackCooldown;
    private int burstLeft;
    private EntityLivingBase target;

    /**
     * @param world the world; the entity registry calls this when it loads a turret
     */
    public NpcTurret(World world) {
        super(world);
        setSize(WIDTH, HEIGHT);
        getNavigator().setCanSwim(false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(NpcTurretRules.MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1.0);
    }

    @Override
    public NpcFaction npcFaction() {
        return NpcTurretRules.FACTION;
    }

    /** @return the fixed gun item, or null before registration */
    private static ItemGun gunItem() {
        return GunItems.get(NpcTurretRules.GUN);
    }

    // --- behavior (server) ------------------------------------------------------------------------------------

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        motionX = 0;
        motionZ = 0;
        ItemGun gun = gunItem();
        if (gun == null) {
            target = null;
            return;
        }
        GunStats stats = gun.stats()
            .forNpc();
        double range = stats.aiRange();
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (target != null
            && (getDistanceSqToEntity(target) > range * range || !wouldTarget(target) || !canEntityBeSeen(target))) {
            target = null;
        }
        if (target == null && ++scanCountdown >= NpcTurretRules.SCAN_INTERVAL) {
            scanCountdown = 0;
            target = findTarget(range);
        }
        if (target == null) {
            burstLeft = 0;
            return;
        }
        aimAt(target);
        if (attackCooldown <= 0) {
            fire(gun, stats, getDistanceToEntity(target));
        }
    }

    @SuppressWarnings("unchecked")
    private EntityLivingBase findTarget(double range) {
        List<EntityLivingBase> nearby = worldObj.getEntitiesWithinAABB(
            EntityLivingBase.class,
            boundingBox.expand(range, NpcTurretRules.VERTICAL_REACH, range));
        EntityLivingBase best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityLivingBase candidate : nearby) {
            double dist = getDistanceSqToEntity(candidate);
            if (dist > range * range || dist >= bestDist || !wouldTarget(candidate) || !canEntityBeSeen(candidate)) {
                continue;
            }
            best = candidate;
            bestDist = dist;
        }
        return best;
    }

    /**
     * @param entity a possible target
     * @return true when the camp turret's rules choose the entity: alive, not itself, and a damageable player, a
     *         player turret or an NPC of an enemy faction
     */
    public boolean wouldTarget(EntityLivingBase entity) {
        if (entity == null || entity == this || entity.isDead || entity.getHealth() <= 0) {
            return false;
        }
        boolean player = entity instanceof EntityPlayer;
        boolean faction = entity instanceof HasNpcFaction;
        return NpcTurretRules.isTarget(
            player,
            player && ((EntityPlayer) entity).capabilities.disableDamage,
            faction,
            faction ? ((HasNpcFaction) entity).npcFaction() : null,
            entity instanceof TurretEntity);
    }

    /** @return the current target, for tests; null for none */
    public EntityLivingBase currentTarget() {
        return target;
    }

    private void aimAt(Entity t) {
        double dx = t.posX - posX;
        double dz = t.posZ - posZ;
        double dy = t.posY + t.height * 0.6 - (posY + EYE_HEIGHT);
        double horizontal = MathHelper.sqrt_double(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, horizontal) * 180.0 / Math.PI);
        rotationYaw = yaw;
        rotationYawHead = yaw;
        renderYawOffset = yaw;
        rotationPitch = pitch;
    }

    private void fire(ItemGun gun, GunStats stats, double distance) {
        NpcFiring.Scaling scaling = NpcFiring
            .scaling(NpcFiring.Difficulty.fromVanillaId(worldObj.difficultySetting.getDifficultyId()), true);
        ShotOffset[] offsets = NpcFiring.roll(getRNG(), stats, scaling);
        for (ShotOffset offset : offsets) {
            Projectiles.launcher()
                .launch(worldObj, this, stats, offset, 1.0F);
        }
        GunSoundPlayer.playFire(this, gun.fireEvent(), null);
        if (burstLeft <= 0) {
            burstLeft = Math.max(1, stats.aiBurstCount());
        }
        burstLeft--;
        attackCooldown = burstLeft > 0 ? stats.aiBurstDelay() : NpcFiring.nextAttackDelay(stats, distance);
    }

    // --- body, armor, display ---------------------------------------------------------------------------------

    @Override
    public int getTotalArmorValue() {
        return NpcTurretRules.ARMOR_VALUE;
    }

    @Override
    public int displayArmorValue() {
        return NpcTurretRules.ARMOR_VALUE;
    }

    @Override
    public ItemStack displayGun() {
        ItemGun gun = gunItem();
        return gun == null ? null : new ItemStack(gun);
    }

    @Override
    public float getEyeHeight() {
        return (float) EYE_HEIGHT;
    }

    /** Camp turrets persist (the plan keeps them with their camp). */
    @Override
    protected boolean canDespawn() {
        return false;
    }

    /** @return true: a camp turret never despawns */
    public boolean persists() {
        return !canDespawn();
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected String getHurtSound() {
        return "mob.irongolem.hit";
    }

    @Override
    protected String getDeathSound() {
        return "mob.irongolem.death";
    }

    /** No drops. */
    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {}
}
