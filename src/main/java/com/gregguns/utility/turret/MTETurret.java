package com.gregguns.utility.turret;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.core.registry.Naming;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.utility.ItemTurretArmor;
import com.gregguns.utility.security.FriendProvider;
import com.gregguns.utility.security.FriendProviders;
import com.gregguns.utility.security.OwnerAccess;
import com.gregguns.utility.security.SecurityLevel;
import com.gregguns.utility.turret.entity.TurretEntity;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;

/**
 * The Turret: a powered GT single block (MV to IV) that holds one GregGuns gun, nine stacks of ammo and an armor
 * plate, and keeps a 30 HP {@link TurretEntity} standing on top of it. The base pays the upkeep and the healing in EU,
 * waits {@code turretRepairTicks} to repair after the entity dies, and respawns it. A Turret fires guns up to its own
 * tier.
 *
 * <p>
 * Using it: place the block, open its GUI, put a gun in the gun slot, magazines or rounds in the nine ammo slots, and
 * power it. Empty magazines appear in the nine output slots. Insertion by pipes or hoppers works on every side but the
 * bottom (ammo only); extraction takes the output slots only. The security button (0 public, 1 friends, 2 owner) is the
 * owner's; the redstone and target-animals buttons follow the security level.
 *
 * <p>
 * Construct only inside GT's MTE window ({@link TurretMachines#register()}). Both sides; the behavior runs on the
 * server.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md, docs/techguns-spec/09-machines-utility/turret.md,
// docs/gregguns-spec/08-machines-production/machine-roster.md (Turret, GGS-08-50)
// Deviation: EU instead of RF (turret.md); the entity link is its UUID (MUTL-25); the wrench-dismantle NBT carry and
// the turret-death-at-placement flag are not built.
public final class MTETurret extends MTETieredMachineBlock implements TurretHost {

    /** Ammo input slots 0 to 8. */
    public static final int AMMO_FIRST = 0;
    /** One past the last ammo slot. */
    public static final int AMMO_END = 9;
    /** Empty-magazine output slots 9 to 17. */
    public static final int OUT_FIRST = 9;
    /** One past the last output slot. */
    public static final int OUT_END = 18;
    /** The gun slot. */
    public static final int GUN_SLOT = 18;
    /** The armor plate slot. */
    public static final int ARMOR_SLOT = 19;
    /** Total slots. */
    public static final int SLOTS = 20;

    private static final int LINK_CHECK_TICKS = 10;
    private static final String OVERLAY_DIR = "machines/gun_mount_hatch/";

    private static final String KEY_SECURITY = "security";
    private static final String KEY_REDSTONE = "redstoneMode";
    private static final String KEY_ANIMALS = "attackAnimals";
    private static final String KEY_TURRET = "turretUuid";
    private static final String KEY_DEAD = "turretDeath";
    private static final String KEY_REPAIR = "repairTime";
    private static final String KEY_HEAL = "turretHealTime";
    private static final String KEY_ACTIVE = "turretActive";

    private final TurretLogic logic = new TurretLogic(
        UtilityConfig.turretUpkeepEuPerTick,
        UtilityConfig.turretEuPerHealedHp,
        UtilityConfig.turretRepairEuPerTick,
        UtilityConfig.turretRepairTicks);

    private SecurityLevel security = SecurityLevel.PUBLIC;
    private RedstoneMode redstone = RedstoneMode.IGNORE;
    private boolean attackAnimals;
    private UUID turretId;
    private TurretEntity turret;
    private boolean enabled = true;
    private boolean respawnWeak;
    private FriendProvider friendOverride;
    private int syncedRepairLeft;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id, from {@link com.gregguns.machine.GregGunsMTEIDs}
     * @param name            GT's internal name, lower case with dots
     * @param nameRegional    the English display name GT registers for the block
     * @param tier            GT voltage index (2 is MV)
     * @param descriptionKeys lang keys of the tooltip lines, translated when the tooltip is shown
     */
    public MTETurret(int id, String name, String nameRegional, int tier, String[] descriptionKeys) {
        super(id, name, nameRegional, tier, SLOTS, descriptionKeys);
        Overlays.load();
    }

    private MTETurret(String name, int tier, String[] descriptionKeys) {
        super(name, tier, SLOTS, descriptionKeys, null);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTETurret(mName, mTier, mDescriptionArray);
    }

    /** @return this Turret's tier as a GregGuns tier */
    public Tier turretTier() {
        for (Tier t : Tier.values()) {
            if (t.voltageIndex() == mTier) {
                return t;
            }
        }
        throw new IllegalStateException("no GregGuns tier for GT tier " + mTier);
    }

    @Override
    public String[] getDescription() {
        String[] lines = new String[mDescriptionArray.length];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = StatCollector.translateToLocal(mDescriptionArray[i]);
        }
        return lines;
    }

    // --- energy -----------------------------------------------------------------------------------------------

    @Override
    public boolean isEnetInput() {
        return true;
    }

    @Override
    public boolean isInputFacing(ForgeDirection side) {
        // Techguns: energy connects on every side except the top.
        return side != ForgeDirection.UP;
    }

    @Override
    public long maxEUInput() {
        return GTValues.V[mTier];
    }

    @Override
    public long maxAmperesIn() {
        return 1;
    }

    @Override
    public long maxEUStore() {
        return GTValues.V[mTier] * 64L;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return facing != ForgeDirection.UP && facing != ForgeDirection.DOWN && facing != ForgeDirection.UNKNOWN;
    }

    private TurretLogic.Power power() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return eu -> eu <= 0 || base.getStoredEU() >= eu && base.decreaseStoredEnergyUnits(eu, true);
    }

    // --- inventory --------------------------------------------------------------------------------------------

    /**
     * @param stack a stack
     * @return true when it may sit in the gun slot: a GregGuns gun that is not akimbo
     */
    public static boolean isGun(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemGun
            && !((ItemGun) stack.getItem()).stats()
                .akimbo();
    }

    /**
     * @param stack a stack
     * @return true when it may sit in the armor slot: a turret armor plate
     */
    public static boolean isPlate(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemTurretArmor;
    }

    /**
     * @param stack a stack
     * @return true when this Turret may hold the gun: a gun slot item whose tier is at most the Turret's
     */
    public boolean mayMount(ItemStack stack) {
        return isGun(stack) && TurretLogic.gunAllowed(
            ((ItemGun) stack.getItem()).getTier()
                .voltageIndex(),
            mTier);
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity baseMetaTileEntity, int index, ForgeDirection side,
        ItemStack stack) {
        if (side == ForgeDirection.DOWN) {
            return false;
        }
        if (index >= AMMO_FIRST && index < AMMO_END) {
            return true;
        }
        if (index == GUN_SLOT) {
            return mInventory[GUN_SLOT] == null && mayMount(stack);
        }
        return index == ARMOR_SLOT && mInventory[ARMOR_SLOT] == null && isPlate(stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity baseMetaTileEntity, int index, ForgeDirection side,
        ItemStack stack) {
        return side != ForgeDirection.DOWN && index >= OUT_FIRST && index < OUT_END;
    }

    // --- TurretHost -------------------------------------------------------------------------------------------

    @Override
    public UUID ownerId() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base == null ? null : base.getOwnerUuid();
    }

    @Override
    public SecurityLevel security() {
        return security;
    }

    @Override
    public boolean targetsAnimals() {
        return attackAnimals;
    }

    @Override
    public ItemStack mountedGun() {
        ItemStack gun = mInventory[GUN_SLOT];
        return mayMount(gun) ? gun : null;
    }

    @Override
    public int armorValue() {
        ItemStack plate = mInventory[ARMOR_SLOT];
        return isPlate(plate) ? ((ItemTurretArmor) plate.getItem()).armorValue() : 0;
    }

    @Override
    public boolean isTurretActive() {
        return enabled && logic.isActive();
    }

    @Override
    public boolean isHostValid() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && !base.isDead() && !base.isInvalidTileEntity();
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
            // The base is repairing: any old entity is a leftover.
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
        if (gunStack == null) {
            return false;
        }
        GunStats stats = ((ItemGun) gunStack.getItem()).stats();
        if (gunStack.getTagCompound() == null) {
            gunStack.setTagCompound(new NBTTagCompound());
        }
        GunState state = GunNbt.read(gunStack.getTagCompound(), stats);
        TurretAmmo.Result result = TurretAmmo.consume(stats, state.loaded(), new SlotStore());
        state.setLoaded(stats, result.loaded());
        GunNbt.write(gunStack.getTagCompound(), state);
        return result.fired();
    }

    /** The ammo slots as the pure ammo rules see them. */
    private final class SlotStore implements TurretAmmo.Store {

        @Override
        public int count(String ammoId) {
            int found = 0;
            for (int i = AMMO_FIRST; i < AMMO_END; i++) {
                if (isItem(mInventory[i], ammoId)) {
                    found += mInventory[i].stackSize;
                }
            }
            return found;
        }

        @Override
        public int take(String ammoId, int max) {
            int taken = 0;
            for (int i = AMMO_FIRST; i < AMMO_END && taken < max; i++) {
                ItemStack s = mInventory[i];
                if (isItem(s, ammoId)) {
                    int n = Math.min(max - taken, s.stackSize);
                    s.stackSize -= n;
                    taken += n;
                    if (s.stackSize <= 0) {
                        getBaseMetaTileEntity().setInventorySlotContents(i, null);
                    }
                }
            }
            return taken;
        }

        @Override
        public void putEmpty(String emptyId, int count) {
            Item item = GameRegistry.findItem(GregGuns.MODID, emptyId);
            if (item == null) {
                return;
            }
            int left = count;
            for (int i = OUT_FIRST; i < OUT_END && left > 0; i++) {
                ItemStack s = mInventory[i];
                if (s != null && s.getItem() == item && s.getItemDamage() == 0) {
                    int n = Math.min(left, s.getMaxStackSize() - s.stackSize);
                    s.stackSize += n;
                    left -= n;
                }
            }
            for (int i = OUT_FIRST; i < OUT_END && left > 0; i++) {
                if (mInventory[i] == null) {
                    int n = Math.min(left, new ItemStack(item).getMaxStackSize());
                    getBaseMetaTileEntity().setInventorySlotContents(i, new ItemStack(item, n));
                    left -= n;
                }
            }
            if (TurretAmmo.shouldDrop(left)) {
                // MUTL-24: only a real overflow is dropped, never a zero-size stack.
                IGregTechTileEntity base = getBaseMetaTileEntity();
                EntityItem drop = new EntityItem(
                    base.getWorld(),
                    base.getXCoord() + 0.5,
                    base.getYCoord() + 1.2,
                    base.getZCoord() + 0.5,
                    new ItemStack(item, left));
                base.getWorld()
                    .spawnEntityInWorld(drop);
            }
        }
    }

    private static boolean isItem(ItemStack stack, String registryId) {
        return stack != null && stack.stackSize > 0
            && Naming.qualifiedName(registryId)
                .equals(Item.itemRegistry.getNameForObject(stack.getItem()));
    }

    // --- tick -------------------------------------------------------------------------------------------------

    /**
     * Replaces the friend source of this Turret. For the scenarios, which need a friend without ServerUtilities.
     *
     * @param provider the stub; null restores the pack's provider
     */
    public void setFriendProvider(FriendProvider provider) {
        friendOverride = provider;
    }

    private FriendProvider friends() {
        return friendOverride != null ? friendOverride : FriendProviders.current();
    }

    @Override
    public TurretTargetSelector.Settings targetSettings() {
        return new TurretTargetSelector.Settings(security, ownerId(), attackAnimals, friends());
    }

    /** @return the linked turret entity, or null when none is linked */
    public TurretEntity linkedTurret() {
        return turret;
    }

    /** @return the saved link UUID, or null */
    public UUID linkedId() {
        return turretId;
    }

    /**
     * Replaces the saved link. For the scenarios and tests that need to plant a stale or wrong-type link.
     *
     * @param id the UUID to hold; null clears
     */
    public void setLinkedId(UUID id) {
        turretId = id;
        turret = null;
    }

    /** @return the base's state machine (read-only use by GUIs and tests) */
    public TurretLogic logic() {
        return logic;
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide()) {
            return;
        }
        enabled = redstone.allows(base.getRedstone());
        if (turret != null && turret.isDead) {
            turret = null;
        }
        if (turret == null && !logic.isDead() && tick % LINK_CHECK_TICKS == 0) {
            linkOrSpawn(base);
        }
        boolean hurt = turret != null && turret.getHealth() < turret.getMaxHealth();
        TurretLogic.Result result = logic.tick(power(), enabled, turret != null, hurt);
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
        syncedRepairLeft = logic.repairLeft();
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
        // MUTL-25: a missing, stale or wrong-type link means unlinked, and the base makes a new turret.
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
        // Breaking the base kills the mounted turret (Techguns: the turret is killed and the inventory spills).
        if (turret != null) {
            turret.unbind();
            turret.setDead();
            turret = null;
        }
        turretId = null;
        super.onBlockDestroyed();
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player, ForgeDirection side, float x, float y,
        float z) {
        if (base.isServerSide() && !mayUse(player)) {
            player.addChatMessage(new ChatComponentTranslation("gregguns.turret.locked"));
            return true;
        }
        return super.onRightclick(base, player, side, x, y, z);
    }

    // --- settings (server) ------------------------------------------------------------------------------------

    /**
     * @param player a player
     * @return true when the security level lets this player open the GUI and change the redstone and animal buttons
     */
    public boolean mayUse(EntityPlayer player) {
        return player != null && OwnerAccess.mayUse(security, ownerId(), player.getUniqueID(), friends());
    }

    /**
     * @param player a player
     * @return true when this player may change the security level: the owner only (any player for an ownerless block)
     */
    public boolean mayChangeSecurity(EntityPlayer player) {
        return player != null && OwnerAccess.mayUse(SecurityLevel.OWNER, ownerId(), player.getUniqueID(), friends());
    }

    /**
     * Cycles the security level if the player may (GGS-09-04: level 1 equals 2 without ServerUtilities).
     *
     * @param player the pressing player
     * @return true when it changed
     */
    public boolean cycleSecurity(EntityPlayer player) {
        if (!mayChangeSecurity(player)) {
            return false;
        }
        security = security.next();
        return true;
    }

    /**
     * Cycles the redstone mode if the player may use the Turret.
     *
     * @param player the pressing player
     * @return true when it changed
     */
    public boolean cycleRedstone(EntityPlayer player) {
        if (!mayUse(player)) {
            return false;
        }
        redstone = redstone.next();
        return true;
    }

    /**
     * Toggles Target Animals if the player may use the Turret.
     *
     * @param player the pressing player
     * @return true when it changed
     */
    public boolean toggleAnimals(EntityPlayer player) {
        if (!mayUse(player)) {
            return false;
        }
        attackAnimals = !attackAnimals;
        return true;
    }

    /** @return the security level id for the GUI sync */
    public int securityId() {
        return security.id();
    }

    /** @return the redstone mode id for the GUI sync */
    public int redstoneId() {
        return redstone.id();
    }

    /** @return 1 when Target Animals is on, for the GUI sync */
    public int animalsFlag() {
        return attackAnimals ? 1 : 0;
    }

    /** @return seconds of repair left, for the GUI */
    public int repairSecondsLeft() {
        return (syncedRepairLeft + 19) / 20;
    }

    private boolean isClientCopy() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && base.isClientSide();
    }

    /**
     * Applies a security level the server pushed to this client copy. Ignored on the server, where a client must never
     * set a setting through the sync channel.
     *
     * @param id the level id
     */
    public void syncSecurity(int id) {
        if (isClientCopy()) {
            security = SecurityLevel.fromId(id);
        }
    }

    /**
     * Applies a redstone mode the server pushed; ignored on the server.
     *
     * @param id the mode id
     */
    public void syncRedstone(int id) {
        if (isClientCopy()) {
            redstone = RedstoneMode.fromId(id);
        }
    }

    /**
     * Applies the Target Animals flag the server pushed; ignored on the server.
     *
     * @param flag 1 for on
     */
    public void syncAnimals(int flag) {
        if (isClientCopy()) {
            attackAnimals = flag != 0;
        }
    }

    /**
     * Applies the repair ticks the server pushed; ignored on the server.
     *
     * @param ticks ticks of repair left
     */
    public void syncRepair(int ticks) {
        if (isClientCopy()) {
            syncedRepairLeft = ticks;
        }
    }

    /** @return ticks of repair left, for the GUI sync */
    public int repairTicksForSync() {
        return logic.repairLeft();
    }

    // --- GUI --------------------------------------------------------------------------------------------------

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return new TurretGui(this).build(guiData, syncManager, uiSettings);
    }

    // --- textures ---------------------------------------------------------------------------------------------

    @Override
    public ITexture[][][] getTextureSet(ITexture[] textures) {
        return null;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        // Placeholder: the machine casing with the Gun Mount Hatch front on the facing side; the closest existing art.
        ITexture casing = Textures.BlockIcons.MACHINE_CASINGS[mTier][colorIndex + 1];
        if (side == facing) {
            return new ITexture[] { casing, TextureFactory.of(active ? Overlays.FRONT_ACTIVE : Overlays.FRONT) };
        }
        return new ITexture[] { casing };
    }

    /** The overlay icons, requested when the first Turret is constructed (before GT stitches textures). */
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
        nbt.setByte(KEY_SECURITY, (byte) security.id());
        nbt.setByte(KEY_REDSTONE, (byte) redstone.id());
        nbt.setBoolean(KEY_ANIMALS, attackAnimals);
        if (turretId != null) {
            nbt.setString(KEY_TURRET, turretId.toString());
        }
        nbt.setBoolean(KEY_DEAD, logic.isDead());
        nbt.setInteger(KEY_REPAIR, logic.repairLeft());
        nbt.setInteger(KEY_HEAL, logic.healCooldown());
        nbt.setBoolean(KEY_ACTIVE, logic.isActive());
        nbt.setBoolean("respawnWeak", respawnWeak);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        security = SecurityLevel.fromId(nbt.getByte(KEY_SECURITY));
        redstone = RedstoneMode.fromId(nbt.getByte(KEY_REDSTONE));
        attackAnimals = nbt.getBoolean(KEY_ANIMALS);
        turretId = null;
        if (nbt.hasKey(KEY_TURRET)) {
            try {
                turretId = UUID.fromString(nbt.getString(KEY_TURRET));
            } catch (IllegalArgumentException e) {
                // A damaged tag means "unlinked": the base makes a new turret (MUTL-25).
                GregGuns.LOG.warn("Turret base has a malformed turret UUID; it will respawn the turret", e);
            }
        }
        boolean active = !nbt.hasKey(KEY_ACTIVE) || nbt.getBoolean(KEY_ACTIVE);
        logic.restore(nbt.getBoolean(KEY_DEAD), nbt.getInteger(KEY_REPAIR), nbt.getInteger(KEY_HEAL), active);
        respawnWeak = nbt.getBoolean("respawnWeak");
    }
}
