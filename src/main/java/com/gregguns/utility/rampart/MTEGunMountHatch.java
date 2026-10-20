package com.gregguns.utility.rampart;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.utility.security.OwnerAccess;
import com.gregguns.utility.security.SecurityLevel;
import com.gregguns.utility.turret.MTETurret;
import com.gregguns.utility.turret.TurretAmmo;
import com.gregguns.utility.turret.TurretHost;
import com.gregguns.utility.turret.TurretLink;
import com.gregguns.utility.turret.TurretLogic;
import com.gregguns.utility.turret.TurretTargetSelector;
import com.gregguns.utility.turret.entity.TurretEntity;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/**
 * The Gun Mount Hatch (MV to IV): the sill of one Rampart bay. It holds one gun and keeps one 30 HP
 * {@link TurretEntity} standing in the crenel above it. The hatch is the entity's {@link TurretHost}: owner, security,
 * the animal switch, armor, ammunition and power all come from the Rampart controller it is part of, so the entity is
 * the same one the single Turret uses. When the hatch is broken, or the structure stops being a Rampart, the turret is
 * killed.
 *
 * <p>
 * Using it: right-click with a gun to mount it (the gun's tier must not exceed the hatch's); right-click with an empty
 * hand to take it back. Both need the controller's security level to allow the player. Pipes can insert a gun but not
 * pull it. Construct only inside GT's MTE window ({@link RampartMachines#register()}). Both sides; the behavior runs
 * on the server.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Rampart: Hatches, controller, mounts; GGS-08-75,
// GGS-08-76, GGS-08-80), docs/gregguns-spec/09-machines-utility/turret.md
// Deviation: the gun goes in and out by right-click, not through a GUI; the hatch has one slot and a GUI would only
// show it.
// Deviation: the respawn timer and healing are per mount (as the sheet says) but the upkeep is the controller's draw,
// so the per-mount TurretLogic is built with an upkeep of 0.
public final class MTEGunMountHatch extends MTEHatch implements TurretHost {

    /** The one inventory slot. */
    public static final int GUN_SLOT = 0;

    private static final int LINK_CHECK_TICKS = 10;
    private static final String OVERLAY_DIR = "machines/gun_mount_hatch/";

    private static final String KEY_TURRET = "turretUuid";
    private static final String KEY_DEAD = "turretDeath";
    private static final String KEY_REPAIR = "repairTime";
    private static final String KEY_HEAL = "turretHealTime";
    private static final String KEY_ACTIVE = "turretActive";
    private static final String KEY_WEAK = "respawnWeak";

    private final TurretLogic logic = new TurretLogic(
        0,
        UtilityConfig.turretEuPerHealedHp,
        UtilityConfig.turretRepairEuPerTick,
        UtilityConfig.turretRepairTicks);

    private MTERampart controller;
    private UUID turretId;
    private TurretEntity turret;
    private boolean respawnWeak;

    /**
     * The prototype that registers the hatch under {@code id}.
     *
     * @param id              the MTE id, from {@link com.gregguns.machine.GregGunsMTEIDs}
     * @param name            GT's internal name
     * @param nameRegional    the English display name GT registers for the block
     * @param tier            GT voltage index (2 is MV)
     * @param descriptionKeys lang keys of the tooltip lines, translated when the tooltip is shown
     */
    public MTEGunMountHatch(int id, String name, String nameRegional, int tier, String[] descriptionKeys) {
        super(id, name, nameRegional, tier, 1, descriptionKeys);
        Overlays.load();
    }

    private MTEGunMountHatch(String name, int tier, String[] descriptionKeys, ITexture[][][] textures) {
        super(name, tier, 1, descriptionKeys, textures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTEGunMountHatch(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public String[] getDescription() {
        String[] lines = new String[mDescriptionArray.length];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = StatCollector.translateToLocal(mDescriptionArray[i]);
        }
        return com.gregguns.core.text.AddedBy.append(lines);
    }

    /** @return the GT voltage index of this hatch, which is the highest gun tier it mounts */
    public int hatchTierIndex() {
        return mTier;
    }

    @Override
    public boolean willExplodeInRain() {
        // The Rampart stands outside (GGS-08-81); the single Turret does the same.
        return false;
    }

    // --- binding to the controller -----------------------------------------------------------------------------

    /**
     * Joins a controller. Called by the controller's structure check.
     *
     * @param owner the controller; null unbinds without killing anything
     */
    void bind(MTERampart owner) {
        controller = owner;
    }

    /** Leaves the controller and kills the turret (GGS-08-80): the structure no longer is a Rampart. */
    void release() {
        controller = null;
        killTurret();
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.isActive()) {
            base.setActive(false);
        }
    }

    /** @return the controller this hatch is part of, or null when it is not part of a formed Rampart */
    public MTERampart controller() {
        return controller;
    }

    private void killTurret() {
        if (turret != null) {
            turret.unbind();
            turret.setDead();
            turret = null;
        }
        turretId = null;
    }

    // --- inventory ---------------------------------------------------------------------------------------------

    /**
     * @param stack a stack
     * @return true when this hatch may hold the gun: a GregGuns gun that is not akimbo and whose tier is at most the
     *         hatch's (GGS-08-76)
     */
    public boolean mayMount(ItemStack stack) {
        return MTETurret.isGun(stack) && TurretLogic.gunAllowed(
            ((ItemGun) stack.getItem()).getTier()
                .voltageIndex(),
            mTier);
    }

    @Override
    public boolean isValidSlot(int index) {
        return index == GUN_SLOT;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return index == GUN_SLOT && mInventory[GUN_SLOT] == null && mayMount(stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player) {
        if (base.isClientSide()) {
            return true;
        }
        if (!mayUse(player)) {
            player.addChatMessage(new ChatComponentTranslation("gregguns.mount.locked"));
            return true;
        }
        ItemStack held = player.getHeldItem();
        if (mInventory[GUN_SLOT] == null) {
            if (held == null) {
                player.addChatMessage(new ChatComponentTranslation("gregguns.mount.empty"));
            } else if (!MTETurret.isGun(held)) {
                player.addChatMessage(new ChatComponentTranslation("gregguns.mount.not_gun"));
            } else if (!mayMount(held)) {
                player.addChatMessage(new ChatComponentTranslation("gregguns.mount.tier"));
            } else {
                ItemStack one = held.splitStack(1);
                base.setInventorySlotContents(GUN_SLOT, one);
                if (held.stackSize <= 0) {
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                }
            }
        } else if (held == null) {
            ItemStack gun = mInventory[GUN_SLOT];
            base.setInventorySlotContents(GUN_SLOT, null);
            if (!player.inventory.addItemStackToInventory(gun)) {
                player.dropPlayerItemWithRandomChoice(gun, false);
            }
        } else {
            player.addChatMessage(new ChatComponentTranslation("gregguns.mount.full"));
        }
        return true;
    }

    // --- TurretHost -------------------------------------------------------------------------------------------

    private boolean mayUse(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        if (controller != null) {
            return controller.mayUse(player);
        }
        return OwnerAccess.mayUse(
            SecurityLevel.OWNER,
            ownerId(),
            player.getUniqueID(),
            com.gregguns.utility.security.FriendProviders.current());
    }

    @Override
    public UUID ownerId() {
        if (controller != null) {
            return controller.ownerId();
        }
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base == null ? null : base.getOwnerUuid();
    }

    @Override
    public SecurityLevel security() {
        return controller == null ? SecurityLevel.OWNER : controller.security();
    }

    @Override
    public boolean targetsAnimals() {
        return controller != null && controller.targetsAnimals();
    }

    @Override
    public TurretTargetSelector.Settings targetSettings() {
        return controller != null ? controller.targetSettings() : TurretHost.super.targetSettings();
    }

    @Override
    public ItemStack mountedGun() {
        ItemStack gun = mInventory[GUN_SLOT];
        return mayMount(gun) ? gun : null;
    }

    @Override
    public int armorValue() {
        return controller == null ? 0 : controller.armorValue();
    }

    @Override
    public boolean isHostValid() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && !base.isDead()
            && !base.isInvalidTileEntity()
            && controller != null
            && controller.isFormed();
    }

    @Override
    public boolean isTurretActive() {
        return controller != null && controller.mountsRunning() && logic.isActive();
    }

    @Override
    public boolean adopt(TurretEntity candidate) {
        if (candidate == null || candidate.isDead || !isHostValid()) {
            return false;
        }
        if (turret == candidate) {
            return true;
        }
        if (turret != null && !turret.isDead) {
            return false;
        }
        if (logic.isDead()) {
            return false;
        }
        turret = candidate;
        turretId = candidate.getUniqueID();
        IGregTechTileEntity base = getBaseMetaTileEntity();
        candidate.bind(this, base.getXCoord(), base.getYCoord(), base.getZCoord());
        return true;
    }

    @Override
    public void onTurretDeath() {
        turret = null;
        turretId = null;
        logic.onTurretDeath();
        respawnWeak = true;
    }

    @Override
    public boolean consumeAmmo() {
        ItemStack gunStack = mountedGun();
        if (gunStack == null || controller == null) {
            return false;
        }
        GunStats stats = ((ItemGun) gunStack.getItem()).stats();
        if (gunStack.getTagCompound() == null) {
            gunStack.setTagCompound(new NBTTagCompound());
        }
        GunState state = GunNbt.read(gunStack.getTagCompound(), stats);
        TurretAmmo.Result result = TurretAmmo.consume(stats, state.loaded(), controller.ammoStore());
        state.setLoaded(stats, result.loaded());
        GunNbt.write(gunStack.getTagCompound(), state);
        return result.fired();
    }

    /** @return the linked turret entity, or null when none is linked */
    public TurretEntity linkedTurret() {
        return turret;
    }

    /** @return the base's state machine (read-only use by scenarios and tests) */
    public TurretLogic logic() {
        return logic;
    }

    // --- tick -------------------------------------------------------------------------------------------------

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide()) {
            return;
        }
        MTERampart rampart = controller;
        if (rampart == null) {
            // Not part of a formed Rampart (yet): after a reload the controller's first check binds this hatch again.
            return;
        }
        if (turret != null && turret.isDead) {
            turret = null;
        }
        if (turret == null && !logic.isDead() && tick % LINK_CHECK_TICKS == 0) {
            linkOrSpawn(base);
        }
        boolean hurt = turret != null && turret.getHealth() < turret.getMaxHealth();
        TurretLogic.Result result = logic.tick(rampart::payExtra, rampart.mountsRunning(), turret != null, hurt);
        if (result.heal && turret != null) {
            turret.heal(1.0F);
        }
        if (result.respawn) {
            respawnWeak = true;
            spawn(base);
        }
        boolean fighting = turret != null && isTurretActive();
        if (fighting != base.isActive()) {
            base.setActive(fighting);
        }
    }

    private void linkOrSpawn(IGregTechTileEntity base) {
        List<?> nearby = base.getWorld()
            .getEntitiesWithinAABB(
                TurretEntity.class,
                AxisAlignedBB.getBoundingBox(
                    base.getXCoord() - 1,
                    base.getYCoord(),
                    base.getZCoord() - 1,
                    base.getXCoord() + 2,
                    base.getYCoord() + 3,
                    base.getZCoord() + 2));
        @SuppressWarnings("unchecked")
        TurretEntity found = TurretLink
            .find(turretId, (List<TurretEntity>) nearby, TurretEntity::getUniqueID, e -> !e.isDead);
        if (found != null) {
            turret = found;
            found.bind(this, base.getXCoord(), base.getYCoord(), base.getZCoord());
            return;
        }
        // MUTL-25: a missing, stale or wrong-type link means unlinked, and the hatch makes a new turret.
        spawn(base);
    }

    private void spawn(IGregTechTileEntity base) {
        if (base.getWorld()
            .getBlock(base.getXCoord(), base.getYCoord() + 1, base.getZCoord())
            .isOpaqueCube()) {
            return;
        }
        TurretEntity fresh = new TurretEntity(base.getWorld());
        fresh.setLocationAndAngles(base.getXCoord() + 0.5, base.getYCoord() + 1.0, base.getZCoord() + 0.5, 0.0F, 0.0F);
        fresh.setHealth(respawnWeak ? TurretLogic.RESPAWN_HEALTH : TurretLogic.FULL_HEALTH);
        fresh.bind(this, base.getXCoord(), base.getYCoord(), base.getZCoord());
        if (base.getWorld()
            .spawnEntityInWorld(fresh)) {
            turret = fresh;
            turretId = fresh.getUniqueID();
            respawnWeak = false;
            logic.onTurretSpawned();
        }
    }

    @Override
    public void onBlockDestroyed() {
        // GGS-08-80: breaking a hatch kills that mount's turret.
        controller = null;
        killTurret();
        super.onBlockDestroyed();
    }

    // --- textures ---------------------------------------------------------------------------------------------

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(Overlays.FRONT_ACTIVE) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(Overlays.FRONT) };
    }

    /** The overlay icons, requested when the first hatch is constructed (before GT stitches textures). */
    private static final class Overlays {

        static final IIconContainer FRONT = icon("OVERLAY_FRONT");
        static final IIconContainer FRONT_ACTIVE = icon("OVERLAY_FRONT_ACTIVE");

        private Overlays() {}

        static void load() {
            // Loading the class runs the initializers above.
        }

        private static IIconContainer icon(String file) {
            return Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file);
        }
    }

    // --- NBT --------------------------------------------------------------------------------------------------

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        if (turretId != null) {
            nbt.setString(KEY_TURRET, turretId.toString());
        }
        nbt.setBoolean(KEY_DEAD, logic.isDead());
        nbt.setInteger(KEY_REPAIR, logic.repairLeft());
        nbt.setInteger(KEY_HEAL, logic.healCooldown());
        nbt.setBoolean(KEY_ACTIVE, logic.isActive());
        nbt.setBoolean(KEY_WEAK, respawnWeak);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        turretId = null;
        if (nbt.hasKey(KEY_TURRET)) {
            try {
                turretId = UUID.fromString(nbt.getString(KEY_TURRET));
            } catch (IllegalArgumentException e) {
                // A damaged tag means "unlinked": the hatch makes a new turret (MUTL-25).
                GregGuns.LOG.warn("Gun Mount Hatch has a malformed turret UUID; it will respawn the turret", e);
            }
        }
        boolean active = !nbt.hasKey(KEY_ACTIVE) || nbt.getBoolean(KEY_ACTIVE);
        logic.restore(nbt.getBoolean(KEY_DEAD), nbt.getInteger(KEY_REPAIR), nbt.getInteger(KEY_HEAL), active);
        respawnWeak = nbt.getBoolean(KEY_WEAK);
    }
}
