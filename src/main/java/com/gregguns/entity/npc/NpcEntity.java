package com.gregguns.entity.npc;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gregguns.entity.npc.ai.EntityAIGunAttack;
import com.gregguns.entity.npc.ai.EntityAIHurtByFaction;
import com.gregguns.entity.npc.ai.EntityAINpcMelee;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.NpcFiring;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.sound.GunSoundPlayer;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.mob.model.MobAnimSource;
import com.gregguns.network.GregGunsNetwork;
import com.gregguns.projectile.Projectiles;

/**
 * The base of every GregGuns hostile NPC: a vanilla {@link EntityMob} with the Techguns task list, a faction, and a
 * gun-or-melee combat task chosen from the held item. Concrete mobs subclass it and set stats, gear and the faction.
 *
 * <p>
 * Tasks: swim (1), combat (4), wander (5), watch the nearest player and look idle (6). Targets: retaliate against
 * whoever hurt it, subject to the faction table (1), then the nearest player who can take damage (2).
 *
 * <p>
 * Gun use: an NPC holding an {@link ItemGun} fires through {@link Projectiles#launcher()}. It never uses ammo or
 * reloads, and scales only the gun's damage and accuracy by world difficulty, once ({@link NpcFiring#scaling}); its
 * health and melee damage are never touched by difficulty (GGS-10-29). The held gun lives in the vanilla held-item
 * slot, so it persists in the entity's save data, and the combat task is rebuilt on load.
 *
 * <p>
 * Deviations: the player scan runs on a one in {@link #SCAN_CHANCE} chance per tick instead of every tick, as a TPS
 * guard; the eye height (the muzzle) is fixed at 1.6 blocks. Both sides; the AI runs on the server.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (sections 2 and 3), docs/techguns-spec/10-entities-npcs/per-mob.md
// (0. shared GenericNPC mechanics), docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-28, GGS-10-29)
public class NpcEntity extends EntityMob implements IRangedAttackMob, HasNpcFaction, MobAnimSource {

    /** One in this many ticks the player scan runs (a TPS guard; Techguns scanned every tick). */
    public static final int SCAN_CHANCE = 10;
    /** Entity id of the dev-only test registration of this base class (the top of the range N7 reserves, 61 to 80). */
    public static final int TEST_ENTITY_ID = 80;
    /** Muzzle height above the feet, in blocks. */
    public static final float MUZZLE_HEIGHT = 1.6F;

    private static final int AIM_RESEND_TICKS = 40;
    private static final float WATCH_RANGE = 8.0F;
    private static final double MELEE_SPEED = 1.2;
    private static final double AIM_HEIGHT_FRACTION = 0.6;

    private EntityAIBase combatTask;
    private double lastShotFactor = 1.0;
    private boolean aiming;

    /**
     * @param world the world; the entity registry calls this when it loads an NPC
     */
    public NpcEntity(World world) {
        super(world);
        tasks.addTask(1, new EntityAISwimming(this));
        tasks.addTask(5, new EntityAIWander(this, 1.0));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, WATCH_RANGE));
        tasks.addTask(6, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByFaction(this, false));
        targetTasks.addTask(
            2,
            new EntityAINearestAttackableTarget(
                this,
                EntityPlayer.class,
                SCAN_CHANCE,
                true,
                false,
                e -> e instanceof EntityPlayer
                    && NpcTargetRules.canSeekPlayer(((EntityPlayer) e).capabilities.disableDamage)));
        // The held item is empty here, so this installs the melee task; setting a gun re-runs it.
        setCombatTask();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        // Base values only: difficulty never scales these (GGS-10-29).
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20.0);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.3);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(40.0);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(3.0);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public float getEyeHeight() {
        return MUZZLE_HEIGHT;
    }

    @Override
    public NpcFaction npcFaction() {
        return NpcFaction.HOSTILE;
    }

    // --- persistence --------------------------------------------------------------------------------------------

    /**
     * Hook for despawn rules: a persistent NPC (a mini-boss) never despawns.
     *
     * @return true to keep this NPC loaded; by default true for the kinds {@link DespawnRules} lists
     */
    protected boolean isPersistentNpc() {
        return DespawnRules.isPersistent(NpcMob.byName(EntityList.getEntityString(this)));
    }

    @Override
    protected boolean canDespawn() {
        return !isPersistentNpc();
    }

    // --- equipment and loot (N5) ---------------------------------------------------------------------------------

    /** Spawn danger when none was set: an egg or command spawn, which rolls 0 to 3 uniformly. */
    public static final int DANGER_UNSET = -1;

    private static final String DANGER_TAG = "GGDanger";
    private static final int EGG_DANGER_RANGE = 4;

    private int danger = DANGER_UNSET;

    /**
     * Hook: the loot and equipment data of this mob. Concrete mobs return their {@code NpcProfiles} entry; the base
     * returns null, so it drops nothing and carries nothing.
     *
     * @return the profile, or null
     */
    protected NpcProfile npcProfile() {
        return null;
    }

    /** @return the spawn danger 0 to 5 the equipment was rolled with, or {@link #DANGER_UNSET} */
    public int danger() {
        return danger;
    }

    /**
     * Sets the spawn danger. The spawn handler (N6) calls it before {@link #rollEquipment()}.
     *
     * @param value danger 0 to 5
     */
    public void setDanger(int value) {
        danger = value;
    }

    /** Rolls and applies the mob's equipment with its current danger. Server side. */
    public final void rollEquipment() {
        addRandomArmor();
    }

    /**
     * The single equipment entry point (GGS-10-17): vanilla's no-argument roll, so spawn eggs call it and Mobs Info can
     * see it. Uses the danger set by the spawner, or 0 to 3 from the entity's own random source when none was set.
     */
    @Override
    protected void addRandomArmor() {
        NpcProfile profile = npcProfile();
        if (profile != null && !worldObj.isRemote) {
            int level = danger >= 0 ? danger : getRNG().nextInt(EGG_DANGER_RANGE);
            NpcEquipment.apply(this, profile.equipment(), level);
        }
    }

    /**
     * Egg and command spawns roll gear here: vanilla's {@code EntityLiving} version does not call
     * {@code addRandomArmor} (only zombies and skeletons override it), so without this an egg NPC is unarmed.
     */
    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        IEntityLivingData out = super.onSpawnWithEgg(data);
        addRandomArmor();
        return out;
    }

    /** NPC gear is never enchanted. */
    @Override
    protected void enchantEquipment() {}

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        NpcProfile profile = npcProfile();
        if (profile != null) {
            NpcDrops.dropFewItems(this, profile, looting);
        }
    }

    @Override
    protected void dropRareDrop(int stage) {
        NpcProfile profile = npcProfile();
        if (profile != null) {
            NpcDrops.dropRareDrop(this, profile);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger(DANGER_TAG, danger);
    }

    // --- combat task ---------------------------------------------------------------------------------------------

    /**
     * Rebuilds the combat task from the held item: the gun attack for an {@link ItemGun}, melee for anything else.
     * Called from the constructor, when the held item changes and after loading. Server only; the client has no AI.
     */
    protected final void setCombatTask() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        if (combatTask != null) {
            tasks.removeTask(combatTask);
        }
        ItemStack held = getHeldItem();
        if (held != null && held.getItem() instanceof ItemGun) {
            String name = ((ItemGun) held.getItem()).contentName();
            combatTask = new EntityAIGunAttack(this, NpcGunProfile.forGunOrDefault(name));
        } else {
            combatTask = new EntityAINpcMelee(this, MELEE_SPEED, false);
        }
        tasks.addTask(4, combatTask);
    }

    /** @return true when the held item is an {@link ItemGun} */
    public boolean holdsGun() {
        ItemStack held = getHeldItem();
        return held != null && held.getItem() instanceof ItemGun;
    }

    /** @return the current combat task, for tests */
    public EntityAIBase combatTask() {
        return combatTask;
    }

    @Override
    public void setCurrentItemOrArmor(int slot, ItemStack stack) {
        super.setCurrentItemOrArmor(slot, stack);
        if (slot == 0) {
            setCombatTask();
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        danger = tag.hasKey(DANGER_TAG) ? tag.getInteger(DANGER_TAG) : DANGER_UNSET;
        setCombatTask();
    }

    // --- firing ---------------------------------------------------------------------------------------------------

    /**
     * Fires one shot of the held gun at the target. Called by {@link EntityAIGunAttack}.
     *
     * @param target the target
     * @param factor the distance factor of the shot, distance over range clamped to [0.1, 1]; kept for
     *               {@link #lastShotFactor()}
     */
    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float factor) {
        ItemStack held = getHeldItem();
        if (worldObj.isRemote || held == null || !(held.getItem() instanceof ItemGun)) {
            return;
        }
        lastShotFactor = factor;
        ItemGun gun = (ItemGun) held.getItem();
        GunStats stats = gun.stats();
        aimAt(target);
        NpcFiring.Scaling scaling = NpcFiring
            .scaling(NpcFiring.Difficulty.fromVanillaId(worldObj.difficultySetting.getDifficultyId()), false);
        ShotOffset[] offsets = NpcFiring.roll(getRNG(), stats, scaling);
        // Difficulty damage once, here; the NPC-to-player factor is applied at the hit by DamageSources.hit.
        for (ShotOffset offset : offsets) {
            Projectiles.launcher()
                .launch(worldObj, this, stats, offset, (float) scaling.damageFactor());
        }
        GunSoundPlayer.playFire(this, gun.fireEvent(), null);
        swingItem();
    }

    private void aimAt(EntityLivingBase target) {
        double dx = target.posX - posX;
        double dy = target.posY + target.height * AIM_HEIGHT_FRACTION - (posY + MUZZLE_HEIGHT);
        double dz = target.posZ - posZ;
        float yaw = AimAngles.yaw(dx, dy, dz);
        rotationYaw = yaw;
        rotationYawHead = yaw;
        renderYawOffset = yaw;
        rotationPitch = AimAngles.pitch(dx, dy, dz);
    }

    /** @return the distance factor passed with the last shot, 1 before any */
    public double lastShotFactor() {
        return lastShotFactor;
    }

    // --- model pose -------------------------------------------------------------------------------------------------

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!worldObj.isRemote) {
            boolean now = holdsGun() && getAttackTarget() != null;
            if (now != aiming || now && ticksExisted % AIM_RESEND_TICKS == 0) {
                aiming = now;
                GregGunsNetwork.channel()
                    .sendToAllTracking(new PacketNpcAim(this, now), this);
            }
        }
    }

    /**
     * Sets the aiming flag. The server sets it itself; the client sets it from {@link PacketNpcAim}.
     *
     * @param value true while the NPC holds its gun up to fire
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
        // No attack clock of its own: the model plays the vanilla swing.
        return -1.0F;
    }
}
