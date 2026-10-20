package com.gregguns.utility.turret.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.gregguns.combat.TurretAttacker;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.NpcFiring;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.sound.GunSoundPlayer;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.projectile.Projectiles;
import com.gregguns.utility.turret.TurretGuiModel;
import com.gregguns.utility.turret.TurretHost;
import com.gregguns.utility.turret.TurretLink;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import io.netty.buffer.ByteBuf;

/**
 * The mounted gun turret: a 30 HP creature that stands still, picks the nearest valid target and fires the gun of its
 * host through the projectile framework. It is never a hostile mob ({@code IMob}), never an {@code EntityAnimal} or
 * {@code EntityAmbientCreature}, and is {@link IEntityOwnable} with the base owner, so Infernal Mobs and mob-targeting
 * mods leave it alone (GGS-09-01, GGS-09-02). It is a {@link TurretAttacker}, so its hits on players use the turret
 * damage factor.
 *
 * <p>
 * It talks to its base only through {@link TurretHost}; the host is found again after a reload from the saved block
 * position and re-adopted. A turret that finds no host for {@link #ORPHAN_TICKS} ticks removes itself, and one that
 * dies with no host tells nobody and throws nothing (NPC-12). Both sides; the behavior runs on the server.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (5. Turret), docs/techguns-spec/09-machines-utility/turret.md,
// docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-01, GGS-09-02), docs/techguns-spec/BUGS.md (NPC-12)
// Deviation: retaliation against whoever hurt the turret waits for the M7 faction rules; the turret only fires at
// what the target rules choose.
public class TurretEntity extends EntityCreature
    implements IEntityOwnable, TurretAttacker, TurretDisplay, IEntityAdditionalSpawnData {

    /** Mod-local entity id (the registry id; 11, 24 and 25 are projectiles). */
    public static final int ENTITY_ID = 60;
    /** Blocks within which clients track a turret. */
    public static final int TRACKING_RANGE = 64;
    /** Ticks between position updates. */
    public static final int UPDATE_FREQUENCY = 3;
    /** Ticks without a host before a turret removes itself. */
    public static final int ORPHAN_TICKS = 100;
    /** Vertical half-range of the target search, in blocks. */
    private static final double VERTICAL_REACH = 20.0;
    private static final int SCAN_INTERVAL = 10;
    private static final double EYE_HEIGHT = 0.8;

    private static final String KEY_HAS_BASE = "hasBase";
    private static final String KEY_BASE_X = "baseX";
    private static final String KEY_BASE_Y = "baseY";
    private static final String KEY_BASE_Z = "baseZ";

    private int spawnArmor;
    private int spawnGunId = -1;
    private TurretHost host;
    private boolean hasBase;
    private int baseX;
    private int baseY;
    private int baseZ;
    private int hostlessTicks;
    private int scanCountdown;
    private int hostCountdown;
    private int attackCooldown;
    private int burstLeft;
    private EntityLivingBase target;

    /**
     * @param world the world; the entity registry calls this when it loads a turret
     */
    public TurretEntity(World world) {
        super(world);
        setSize(0.6F, 1.0F);
        isImmuneToFire = false;
        getNavigator().setCanSwim(false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(30.0);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.0);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1.0);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(UtilityConfig.turretRange);
    }

    // --- link to the host -------------------------------------------------------------------------------------

    /**
     * Binds this turret to a host and the block it stands on.
     *
     * @param newHost the host; null unlinks
     * @param x       block x of the base
     * @param y       block y of the base
     * @param z       block z of the base
     */
    public void bind(TurretHost newHost, int x, int y, int z) {
        host = newHost;
        hasBase = newHost != null;
        baseX = x;
        baseY = y;
        baseZ = z;
        hostlessTicks = 0;
    }

    /** Drops the link without telling the host (the host is removing or replacing this turret). */
    public void unbind() {
        host = null;
        hasBase = false;
        target = null;
    }

    /** @return the host, or null when unlinked */
    public TurretHost host() {
        return host;
    }

    private void seekHost() {
        if (!hasBase) {
            return;
        }
        if (!worldObj.blockExists(baseX, baseY, baseZ)) {
            return;
        }
        TileEntity tile = worldObj.getTileEntity(baseX, baseY, baseZ);
        if (tile instanceof IGregTechTileEntity) {
            Object mte = ((IGregTechTileEntity) tile).getMetaTileEntity();
            if (mte instanceof TurretHost && ((TurretHost) mte).adopt(this)) {
                host = (TurretHost) mte;
                hostlessTicks = 0;
                return;
            }
        }
        if (++hostlessTicks >= ORPHAN_TICKS / SCAN_INTERVAL) {
            setDead();
        }
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
        if (host == null) {
            target = null;
            if (++hostCountdown >= SCAN_INTERVAL) {
                hostCountdown = 0;
                seekHost();
            }
            return;
        }
        if (!host.isHostValid()) {
            host = null;
            target = null;
            return;
        }
        if (!host.isTurretActive()) {
            target = null;
            return;
        }
        ItemStack gunStack = host.mountedGun();
        if (gunStack == null || !(gunStack.getItem() instanceof ItemGun)) {
            target = null;
            return;
        }
        ItemGun gun = (ItemGun) gunStack.getItem();
        GunStats stats = gun.stats();
        double range = Math.min(stats.aiRange(), UtilityConfig.turretRange);
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (target != null && (target.isDead || target.getHealth() <= 0
            || getDistanceSqToEntity(target) > range * range
            || !TurretCandidate.shouldShoot(host, this, target)
            || !canEntityBeSeen(target))) {
            target = null;
        }
        if (target == null && ++scanCountdown >= SCAN_INTERVAL) {
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
        List<EntityLivingBase> nearby = worldObj
            .getEntitiesWithinAABB(EntityLivingBase.class, boundingBox.expand(range, VERTICAL_REACH, range));
        EntityLivingBase best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityLivingBase candidate : nearby) {
            if (candidate == this || candidate.isDead || candidate.getHealth() <= 0) {
                continue;
            }
            double dist = getDistanceSqToEntity(candidate);
            if (dist > range * range || dist >= bestDist) {
                continue;
            }
            if (TurretCandidate.shouldShoot(host, this, candidate) && canEntityBeSeen(candidate)) {
                best = candidate;
                bestDist = dist;
            }
        }
        return best;
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
        if (!host.consumeAmmo()) {
            attackCooldown = 20;
            return;
        }
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

    /**
     * @param entity a possible target
     * @return true when this turret's host rules choose the entity; false when unlinked or for itself. Used by the
     *         scenarios to prove the owner and friends are spared.
     */
    public boolean wouldTarget(EntityLivingBase entity) {
        return TurretCandidate.shouldShoot(host, this, entity);
    }

    // --- death, armor, ownership ------------------------------------------------------------------------------

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!worldObj.isRemote) {
            TurretHost lost = host;
            host = null;
            // NPC-12: a turret with no base dies quietly.
            TurretLink.notifyDeath(lost);
        }
    }

    /**
     * @return the plate's armor value for choosing a texture: the live value on the server, the value sent at spawn
     *         on a client (GGS-01-11 forbids a DataWatcher slot, so a plate changed later shows after a re-track)
     */
    /**
     * @return what the turret is aiming at, for the Turret screen: empty when idle, {@code @name} for a player, else
     *         the entity's lang key; server side only (the target is not sent to clients)
     */
    public String targetForGui() {
        EntityLivingBase t = target;
        if (t == null || t.isDead) {
            return "";
        }
        if (t instanceof net.minecraft.entity.player.EntityPlayer) {
            return TurretGuiModel.PLAYER_PREFIX + t.getCommandSenderName();
        }
        String name = net.minecraft.entity.EntityList.getEntityString(t);
        return name == null ? "" : "entity." + name + ".name";
    }

    @Override
    public int displayArmorValue() {
        return worldObj.isRemote ? spawnArmor : getTotalArmorValue();
    }

    /**
     * @return the gun the head shows: the live gun on the server, the gun sent at spawn on a client (same limit as
     *         {@link #displayArmorValue()}: a gun swapped later shows after a re-track); null for none
     */
    @Override
    public ItemStack displayGun() {
        if (!worldObj.isRemote) {
            return host == null ? null : host.mountedGun();
        }
        Item item = spawnGunId < 0 ? null : Item.getItemById(spawnGunId);
        return item instanceof ItemGun ? new ItemStack(item) : null;
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeByte(getTotalArmorValue());
        ItemStack gun = host == null ? null : host.mountedGun();
        buffer.writeShort(gun == null ? -1 : Item.getIdFromItem(gun.getItem()));
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        spawnArmor = additionalData.readByte();
        spawnGunId = additionalData.readShort();
    }

    @Override
    public int getTotalArmorValue() {
        return host == null ? 0 : host.armorValue();
    }

    @Override
    public String func_152113_b() {
        if (host == null || host.ownerId() == null) {
            return "";
        }
        return host.ownerId()
            .toString();
    }

    @Override
    public Entity getOwner() {
        if (host == null || host.ownerId() == null) {
            return null;
        }
        for (Object o : worldObj.playerEntities) {
            if (host.ownerId()
                .equals(((Entity) o).getUniqueID())) {
                return (Entity) o;
            }
        }
        return null;
    }

    @Override
    public float getEyeHeight() {
        return (float) EYE_HEIGHT;
    }

    @Override
    protected boolean canDespawn() {
        return false;
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

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {}

    // --- NBT --------------------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean(KEY_HAS_BASE, hasBase);
        tag.setInteger(KEY_BASE_X, baseX);
        tag.setInteger(KEY_BASE_Y, baseY);
        tag.setInteger(KEY_BASE_Z, baseZ);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        hasBase = tag.getBoolean(KEY_HAS_BASE);
        baseX = tag.getInteger(KEY_BASE_X);
        baseY = tag.getInteger(KEY_BASE_Y);
        baseZ = tag.getInteger(KEY_BASE_Z);
    }
}
