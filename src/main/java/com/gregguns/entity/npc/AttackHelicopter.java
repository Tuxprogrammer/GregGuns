package com.gregguns.entity.npc;

import java.util.List;

import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.NpcFiring;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.mob.model.MobAnimSource;
import com.gregguns.network.GregGunsNetwork;
import com.gregguns.projectile.GenericProjectile;
import com.gregguns.projectile.RocketProjectile;

/**
 * The Attack Helicopter boss: a 10 by 4 block gunship that hovers at altitude, strafes around its target at a standoff
 * distance and fires a seven-round machine-gun burst followed by a rocket (about 70 ticks per cycle). It is not in a
 * faction, but it is an {@link IMob}, so turrets and hostile checks see it. It never despawns and removes itself on
 * Peaceful. Both sides; the flight and fire AI run on the server.
 *
 * <p>
 * Flight is the legacy waypoint scheme ({@link HelicopterRules#pickWaypoint}): every few ticks it nudges its motion
 * toward the waypoint when the swept hitbox is clear, otherwise it drops the waypoint. It takes no fall damage and
 * has no gravity while alive.
 *
 * <p>
 * Crash: at zero health it stays in a dying state for {@link HelicopterRules#CRASH_TICKS} ticks, sinking faster and
 * spinning faster as {@link #deathProgress} rises to 1, then explodes (a sound, no block damage) and is removed. The
 * drops and the loot roll happen at the moment of death through the ordinary death event. The vanilla {@code
 * deathTime} stays 0 so the renderer neither topples nor tints it; the model's own death data drives the animation.
 *
 * <p>
 * Fixes of Techguns bugs: bullets and rockets carry the helicopter as their shooter (NPC-11); the gun pitch is signed,
 * so it can aim upward (NPC-10); a player's reflected fireball does not insta-kill it (NPC-32); it persists (NPC-28).
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 6), per-mob.md (16. Attack Helicopter), index.md (NPC-10,
// NPC-11, NPC-28, NPC-32), docs/gregguns-spec/10-entities-npcs/index.md
public class AttackHelicopter extends EntityFlying implements IMob, IBossDisplayData, MobAnimSource {

    /** Registry name of the loot profile. */
    public static final String PROFILE_ID = "AttackHelicopter";

    private static final int AIM_RESEND_TICKS = 40;
    private static final String CRASH_TAG = "GGCrashTicks";
    private static final double MOTION_NUDGE = 0.1;
    /** Distance of the machine-gun muzzle in front of the center, blocks. */
    private static final double MUZZLE_FORWARD = 6.0;
    private static final double MG_MUZZLE_Y = 0.5;
    private static final double ROCKET_MUZZLE_Y = 1.0;
    private static final int MAX_SWEEP_STEPS = 24;
    private static final double AIM_HEIGHT_FRACTION = 0.6;
    /** Gravity added per tick while crashing, scaled by the crash progress, blocks per tick squared. */
    private static final double CRASH_SINK = 0.04;

    private static final GunStats MACHINE_GUN = GunStats.builder("attack_helicopter_mg")
        .damage(12.0)
        .damageDrop(30.0, 40.0, 8.0)
        .spread(0.05)
        .ammoType("ammo_rifle_bullet")
        .projectile(1.0, 100)
        .penetration(0.25)
        .build();
    private static final GunStats ROCKET = GunStats.builder("attack_helicopter_rocket")
        .damage(20.0)
        .spread(0.05)
        .ammoType("ammo_rocket")
        .projectile(1.0, 100)
        .projectileKind(ProjectileKind.ROCKET)
        .blast(2.0, 0.0)
        .penetration(0.0)
        .build();

    private double waypointX;
    private double waypointY;
    private double waypointZ;
    private boolean hasWaypoint;
    private int courseCooldown;
    private int aggroCooldown;
    private int attackCounter;
    private int crashTicks;
    private boolean aiming;

    /**
     * @param world the world; the entity registry calls this when it loads the helicopter
     */
    public AttackHelicopter(World world) {
        super(world);
        setSize(HelicopterRules.WIDTH, HelicopterRules.HEIGHT);
        experienceValue = HelicopterRules.XP;
        ignoreFrustumCheck = true;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(HelicopterRules.MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(HelicopterRules.SCAN_RANGE);
    }

    // --- persistence ---------------------------------------------------------------------------------------------

    /** The helicopter never despawns by distance (NPC-28). */
    @Override
    protected boolean canDespawn() {
        return !DespawnRules.isPersistent(NpcMob.ATTACK_HELICOPTER);
    }

    @Override
    public boolean getCanSpawnHere() {
        // Natural spawning does not apply: eggs, commands and the camp port only.
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger(CRASH_TAG, crashTicks);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        crashTicks = tag.getInteger(CRASH_TAG);
    }

    // --- flight and fire AI ----------------------------------------------------------------------------------------

    @Override
    protected void updateEntityActionState() {
        if (worldObj.isRemote) {
            return;
        }
        if (worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
            setDead();
            return;
        }
        despawnEntity();
        flyTowardWaypoint();
        EntityLivingBase target = updateTarget();
        boolean engaged = target != null && getDistanceToEntity(target) <= HelicopterRules.ENGAGE_RANGE;
        setAimingFlag(engaged);
        if (engaged) {
            faceTarget(target);
            boolean visible = canEntityBeSeen(target);
            attackCounter = HelicopterRules.stepCounter(attackCounter, visible);
            if (visible) {
                act(HelicopterRules.actionAt(attackCounter), target);
                attackCounter = HelicopterRules.afterAction(attackCounter);
            }
        } else {
            attackCounter = HelicopterRules.stepCounter(attackCounter, false);
            double horizontal = motionX * motionX + motionZ * motionZ;
            if (horizontal > 1.0E-4) {
                float yaw = -(float) Math.toDegrees(Math.atan2(motionX, motionZ));
                rotationYaw = yaw;
                renderYawOffset = yaw;
                rotationYawHead = yaw;
            }
        }
    }

    private void flyTowardWaypoint() {
        double dx = waypointX - posX;
        double dy = waypointY - posY;
        double dz = waypointZ - posZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (!hasWaypoint || HelicopterRules.needsWaypoint(distSq)) {
            pickWaypoint();
            return;
        }
        if (courseCooldown-- > 0) {
            return;
        }
        courseCooldown += rand.nextInt(5) + 2;
        double dist = Math.sqrt(distSq);
        if (isCourseTraversable(dx / dist, dy / dist, dz / dist, dist)) {
            motionX += dx / dist * MOTION_NUDGE;
            motionY += dy / dist * MOTION_NUDGE;
            motionZ += dz / dist * MOTION_NUDGE;
        } else {
            // Blocked: forget the waypoint so the next tick picks a fresh one.
            waypointX = posX;
            waypointY = posY;
            waypointZ = posZ;
        }
    }

    private void pickWaypoint() {
        EntityLivingBase target = getAttackTarget();
        int ground = worldObj.getHeightValue(MathHelper.floor_double(posX), MathHelper.floor_double(posZ));
        double[] wp = HelicopterRules.pickWaypoint(
            rand,
            posX,
            posY,
            posZ,
            ground,
            target != null,
            target == null ? 0.0 : target.posX,
            target == null ? 0.0 : target.posZ);
        waypointX = wp[0];
        waypointY = wp[1];
        waypointZ = wp[2];
        hasWaypoint = true;
    }

    /** Sweeps the hitbox along the way to the waypoint; the step count is capped as a TPS guard. */
    private boolean isCourseTraversable(double ux, double uy, double uz, double dist) {
        int steps = (int) Math.min(MAX_SWEEP_STEPS, Math.max(1.0, Math.ceil(dist)));
        double stride = dist / steps;
        AxisAlignedBB box = boundingBox.copy();
        for (int i = 1; i <= steps; i++) {
            box.offset(ux * stride, uy * stride, uz * stride);
            if (!worldObj.getCollidingBoundingBoxes(this, box)
                .isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Keeps or finds the target: the closest vulnerable player within 100 blocks, rescanned every 20 ticks. */
    private EntityLivingBase updateTarget() {
        EntityLivingBase target = getAttackTarget();
        if (target != null && !target.isEntityAlive()) {
            setAttackTarget(null);
            target = null;
        }
        if (target == null || aggroCooldown-- <= 0) {
            EntityPlayer closest = worldObj.getClosestVulnerablePlayerToEntity(this, HelicopterRules.SCAN_RANGE);
            if (closest != null) {
                setAttackTarget(closest);
                target = closest;
                aggroCooldown = HelicopterRules.AGGRO_TICKS;
            } else if (target != null && target instanceof EntityPlayer
                && ((EntityPlayer) target).capabilities.disableDamage) {
                    setAttackTarget(null);
                    target = null;
                }
        }
        return target;
    }

    private void faceTarget(EntityLivingBase target) {
        float yaw = AimAngles.yaw(target.posX - posX, 0.0, target.posZ - posZ);
        rotationYaw = yaw;
        renderYawOffset = yaw;
        rotationYawHead = yaw;
    }

    private void act(HelicopterRules.Action action, EntityLivingBase target) {
        switch (action) {
            case BURST_SOUND:
                worldObj.playSoundAtEntity(this, "random.explode", 2.0F, 2.0F);
                break;
            case MACHINE_GUN:
                fireAt(target, MACHINE_GUN, new GenericProjectile(worldObj), MG_MUZZLE_Y);
                break;
            case ROCKET:
                worldObj.playAuxSFXAtEntity(null, 1008, (int) posX, (int) posY, (int) posZ, 0);
                fireAt(target, ROCKET, new RocketProjectile(worldObj), ROCKET_MUZZLE_Y);
                break;
            default:
                break;
        }
    }

    /**
     * Launches one projectile from the muzzle at the target. The shooter is the helicopter (NPC-11) and the pitch is
     * signed (NPC-10). The muzzle is reached by moving the entity for the length of the call, so the projectile's own
     * spawn geometry (and drop-off start) uses it.
     */
    private void fireAt(EntityLivingBase target, GunStats stats, GenericProjectile shot, double muzzleY) {
        double yawRad = Math.toRadians(rotationYawHead);
        double mx = posX - Math.sin(yawRad) * MUZZLE_FORWARD;
        double my = posY + muzzleY - getEyeHeight();
        double mz = posZ + Math.cos(yawRad) * MUZZLE_FORWARD;
        double aimY = target.posY + target.height * AIM_HEIGHT_FRACTION;
        float pitch = AimAngles.pitch(target.posX - mx, aimY - (my + getEyeHeight()), target.posZ - mz);
        float yaw = AimAngles.yaw(target.posX - mx, 0.0, target.posZ - mz);
        NpcFiring.Scaling scaling = NpcFiring
            .scaling(NpcFiring.Difficulty.fromVanillaId(worldObj.difficultySetting.getDifficultyId()), false);
        ShotOffset offset = NpcFiring.roll(getRNG(), stats, scaling)[0];
        double ox = posX;
        double oy = posY;
        double oz = posZ;
        float oyaw = rotationYawHead;
        float opitch = rotationPitch;
        try {
            posX = mx;
            posY = my;
            posZ = mz;
            rotationYawHead = yaw;
            rotationPitch = pitch;
            shot.fire(this, stats, offset, (float) scaling.damageFactor());
        } finally {
            posX = ox;
            posY = oy;
            posZ = oz;
            rotationYawHead = oyaw;
            rotationPitch = opitch;
        }
        worldObj.spawnEntityInWorld(shot);
    }

    /** A flying machine takes no fall damage (public so a scenario can prove it). */
    @Override
    public void fall(float distance) {}

    // --- crash ----------------------------------------------------------------------------------------------------

    /** The crash: sink and spin faster as it goes, then explode and vanish after 100 ticks. */
    @Override
    protected void onDeathUpdate() {
        crashTicks++;
        float progress = HelicopterRules.deathProgress(crashTicks, 0.0F);
        motionY -= CRASH_SINK * (0.25 + progress);
        float spin = 4.0F + 36.0F * progress;
        rotationYaw += spin;
        renderYawOffset = rotationYaw;
        rotationYawHead = rotationYaw;
        if (crashTicks >= HelicopterRules.CRASH_TICKS) {
            if (!worldObj.isRemote) {
                dropExperience();
                worldObj.playSoundAtEntity(this, "random.explode", 6.0F, 0.8F);
            }
            setDead();
        }
    }

    private void dropExperience() {
        if (recentlyHit > 0 && worldObj.getGameRules()
            .getGameRuleBooleanValue("doMobLoot")) {
            int xp = getExperiencePoints(attackingPlayer);
            while (xp > 0) {
                int part = EntityXPOrb.getXPSplit(xp);
                xp -= part;
                worldObj.spawnEntityInWorld(new EntityXPOrb(worldObj, posX, posY, posZ, part));
            }
        }
    }

    /** @return ticks since the health reached zero, 0 while alive */
    public int crashTicks() {
        return crashTicks;
    }

    @Override
    public float deathProgress(float partialTicks) {
        return getHealth() > 0.0F ? 0.0F : HelicopterRules.deathProgress(crashTicks, partialTicks);
    }

    // --- pose --------------------------------------------------------------------------------------------------------

    /** Syncs the engaged flag to tracking clients on change and every 40 ticks (no DataWatcher slot, GGS-01-11). */
    private void setAimingFlag(boolean on) {
        if (on != aiming || on && ticksExisted % AIM_RESEND_TICKS == 0) {
            aiming = on;
            GregGunsNetwork.channel()
                .sendToAllTracking(new PacketNpcAim(this, on), this);
        }
    }

    /**
     * Sets the engaged flag. The server sets it itself; the client sets it from {@link PacketNpcAim}.
     *
     * @param value true while the helicopter engages a target
     */
    public void setAiming(boolean value) {
        aiming = value;
    }

    @Override
    public boolean isAiming() {
        return aiming;
    }

    @Override
    public float attackProgress(float partialTicks) {
        return -1.0F;
    }

    /** @return the attack counter, for tests: 16 to 30 is the burst, negative the wind-up after the rocket */
    public int attackCounter() {
        return attackCounter;
    }

    /** @return the current waypoint, {x, y, z}, for tests */
    public double[] waypoint() {
        return new double[] { waypointX, waypointY, waypointZ };
    }

    // --- sounds ------------------------------------------------------------------------------------------------------

    @Override
    protected String getLivingSound() {
        return null;
    }

    @Override
    protected String getHurtSound() {
        return "mob.irongolem.hit";
    }

    @Override
    protected String getDeathSound() {
        return "mob.irongolem.death";
    }

    @Override
    protected float getSoundVolume() {
        return 4.0F;
    }

    // --- loot --------------------------------------------------------------------------------------------------------

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        NpcProfile profile = NpcProfiles.get(PROFILE_ID);
        if (profile == null) {
            return;
        }
        List<LootDrop> fixed = profile.fixedDrops();
        for (LootDrop drop : fixed) {
            ItemStack stack = NpcItems.stack(drop, getRNG());
            if (stack != null && stack.stackSize > 0) {
                entityDropItem(stack, 0.0F);
            }
        }
    }
}
