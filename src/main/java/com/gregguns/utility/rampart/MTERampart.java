package com.gregguns.utility.rampart;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.core.registry.Naming;
import com.gregguns.utility.security.FriendProvider;
import com.gregguns.utility.security.FriendProviders;
import com.gregguns.utility.security.OwnerAccess;
import com.gregguns.utility.security.SecurityLevel;
import com.gregguns.utility.turret.TurretAmmo;
import com.gregguns.utility.turret.TurretTargetSelector;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.GTStructureChannels;

/**
 * The Rampart: a crenellated wall of up to ten turret mounts on one ammunition feed and one power feed. A gatehouse
 * tower holds the controller, the buses, the Energy Hatch and the Maintenance Hatch; two to ten bays follow, each with
 * one Gun Mount Hatch on its sill; a one-block cap closes the wall. Each mount keeps one 30 HP turret entity, the same
 * one the single Turret uses.
 *
 * <p>
 * There is no recipe. The controller runs rolling {@value RampartPower#CYCLE_TICKS}-tick cycles and sets its own draw
 * to the mounts' upkeep ({@link RampartPower#upkeepPerTick}); healing and repair are paid on top by the mounts, from
 * the
 * same hatches. If the hatches cannot pay, GT stops the machine and every mount goes inactive. Ammunition is one shared
 * store: a mount's reload takes magazines from the Input Buses and puts the empty ones on an Output Bus. Every mount's
 * armor is that of the lowest Bastion Plating in the structure ({@link BastionPlating}). Security (owner, friends) and
 * the animal switch are set here once and apply to every mount: sneak-use a Screwdriver on the controller to toggle
 * the animal switch, use it to cycle the security level (owner only).
 *
 * <p>
 * The shape is in {@link RampartShape}. Construct only inside GT's MTE window ({@link RampartMachines#register()}),
 * after {@link GregGunsCasings#create}. Both sides; the behavior runs on the server.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Rampart, GGS-08-74 to GGS-08-81, GGS-08-82)
// Source (the pattern: a non-recipe multiblock that sets its own EU/t and cycle in checkProcessing):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTECleanroom.java#L213-L233
// Source (the pattern: a repeated bay checked in a loop, hologram sized by a channel):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTENeutronActivator.java#L279-L297
// Deviation: security and the animal switch are set with the Screwdriver, not on a controller GUI; the redstone mode of
// the single Turret is replaced by the controller's own on/off (Soft Mallet).
// Deviation: an ME Input Bus is not proven; the shared store reads the buses' item slots.
public final class MTERampart extends MTEExtendedPowerMultiBlockBase<MTERampart>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String PIECE_GATE = "gate";
    private static final String PIECE_BAY = "bay";
    private static final String PIECE_CAP = "cap";

    private static final String OVERLAY_DIR = "machines/rampart/";
    private static final String KEY_SECURITY = "security";
    private static final String KEY_ANIMALS = "attackAnimals";

    private final RampartMounts mounts = new RampartMounts();
    private final List<MTEGunMountHatch> pending = new ArrayList<>();
    private int lowestPlatingMeta = Integer.MAX_VALUE;
    private boolean formed;
    private SecurityLevel security = SecurityLevel.PUBLIC;
    private boolean attackAnimals;
    private FriendProvider friendOverride;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id           the MTE id, from {@link com.gregguns.machine.GregGunsMTEIDs}
     * @param name         GT's internal name
     * @param nameRegional the English display name GT registers for the block
     */
    public MTERampart(int id, String name, String nameRegional) {
        super(id, name, nameRegional);
        Overlays.load();
    }

    private MTERampart(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTERampart(mName);
    }

    // --- structure ---------------------------------------------------------------------------------------------

    /** Lazily built, because the casing block must exist before the elements are resolved. */
    private static final class Structure {

        static final IStructureDefinition<MTERampart> DEFINITION = build();

        private Structure() {}

        /** Any Bastion Plating; the lowest meta found sets the armor. */
        @SuppressWarnings("unchecked")
        private static IStructureElement<MTERampart> plating() {
            List<IStructureElement<MTERampart>> options = new ArrayList<>();
            for (BastionPlating p : BastionPlating.values()) {
                final int meta = p.meta();
                options.add(
                    onElementPass(
                        t -> t.lowestPlatingMeta = Math.min(t.lowestPlatingMeta, meta),
                        lazy(() -> ofBlock(GregGunsCasings.INSTANCE, meta))));
            }
            return ofChain(options.toArray(new IStructureElement[0]));
        }

        private static IStructureDefinition<MTERampart> build() {
            int casingIndex = GregGunsCasings.textureIndex(GregGunsCasings.BASTION_IRON);
            return StructureDefinition.<MTERampart>builder()
                .addShape(PIECE_GATE, RampartShape.GATE)
                .addShape(PIECE_BAY, RampartShape.BAY)
                .addShape(PIECE_CAP, RampartShape.CAP)
                .addElement(
                    'T',
                    buildHatchAdder(MTERampart.class).atLeast(InputBus, OutputBus, Energy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(1)
                        .buildAndChain(plating()))
                .addElement('P', plating())
                .addElement(
                    'H',
                    buildHatchAdder(MTERampart.class).adder(MTERampart::addMountHatch)
                        .hatchClass(MTEGunMountHatch.class)
                        .casingIndex(casingIndex)
                        .hint(2)
                        .build())
                .addElement('G', chainAllGlasses())
                .build();
        }
    }

    @Override
    public IStructureDefinition<MTERampart> getStructureDefinition() {
        return Structure.DEFINITION;
    }

    /** The hatch adder of the {@code H} position: accepts only a Gun Mount Hatch. */
    private boolean addMountHatch(IGregTechTileEntity tile, short casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof MTEGunMountHatch)) {
            return false;
        }
        MTEGunMountHatch hatch = (MTEGunMountHatch) tile.getMetaTileEntity();
        hatch.updateTexture(casingIndex);
        return pending.add(hatch);
    }

    @Override
    public void checkMachine(IGregTechTileEntity baseMetaTileEntity, ItemStack stack, List<StructureError> errors) {
        pending.clear();
        lowestPlatingMeta = Integer.MAX_VALUE;
        checkShape(errors);
        boolean ok = errors.isEmpty();
        formed = ok;
        // The hatches of a good check are bound; a bad check releases them all, which kills their turrets.
        mounts.commit(ok ? new ArrayList<>(pending) : new ArrayList<MTEGunMountHatch>(), this);
        if (!ok) {
            formed = false;
        }
    }

    private void checkShape(List<StructureError> errors) {
        if (!checkPiece(PIECE_GATE, RampartShape.CONTROLLER_COLUMN, RampartShape.GATE_CONTROLLER_ROW, 0, errors)) {
            return;
        }
        int bays = 0;
        // Bays are taken while they match; a failed probe's errors are discarded, and so are the hatch and the plating
        // it saw, because the cap check reports a real mismatch.
        while (bays < RampartShape.MAX_MOUNTS) {
            int hatchMark = pending.size();
            int platingMark = lowestPlatingMeta;
            if (checkPiece(
                PIECE_BAY,
                RampartShape.bayOffset(bays),
                RampartShape.WALL_CONTROLLER_ROW,
                0,
                new ArrayList<>())) {
                bays++;
            } else {
                while (pending.size() > hatchMark) {
                    pending.remove(pending.size() - 1);
                }
                lowestPlatingMeta = platingMark;
                break;
            }
        }
        if (!checkPiece(PIECE_CAP, RampartShape.capOffset(bays), RampartShape.WALL_CONTROLLER_ROW, 0, errors)) {
            return;
        }
        if (bays < RampartShape.MIN_MOUNTS) {
            errors.add(StructureErrors.of("gregguns.machine.rampart.error.too_few_mounts"));
            return;
        }
        if (pending.size() != bays) {
            errors.add(StructureErrors.of("gregguns.machine.rampart.error.missing_hatch"));
            return;
        }
        checkHasAnyEnergy(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
    }

    /** @return the number of mounts of the formed Rampart; 0 when unformed */
    public int mountCount() {
        return formed ? mounts.size() : 0;
    }

    /** @return the Gun Mount Hatches of the formed Rampart, in bay order; empty when unformed */
    public List<MTEGunMountHatch> mountHatches() {
        return mounts.hatches();
    }

    /** @return true while the last structure check passed */
    public boolean isFormed() {
        return formed && mMachine;
    }

    /** @return the armor value every mount has: that of the lowest Bastion Plating found; 0 when unformed */
    public int armorValue() {
        return isFormed() ? BastionPlating.lowestArmor(lowestPlatingMeta) : 0;
    }

    /** @return true while the controller is running a cycle, which is when the mounts are active */
    public boolean mountsRunning() {
        return isFormed() && mMaxProgresstime > 0;
    }

    // --- power -------------------------------------------------------------------------------------------------

    @Override
    public @Nonnull CheckRecipeResult checkProcessing() {
        int count = mountCount();
        if (count < RampartShape.MIN_MOUNTS) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        lEUt = -RampartPower.upkeepPerTick(count, UtilityConfig.turretUpkeepEuPerTick);
        mMaxProgresstime = RampartPower.CYCLE_TICKS;
        mEfficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
        mEfficiencyIncrease = 10000;
        mOutputItems = new ItemStack[0];
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /**
     * Pays a one-off charge (healing, repair) from the Energy Hatches. All or nothing: nothing is taken when the
     * hatches together hold less.
     *
     * @param eu the charge
     * @return true when it was paid
     */
    public boolean payExtra(long eu) {
        if (eu <= 0L) {
            return true;
        }
        List<MTEHatchEnergy> hatches = new ArrayList<>();
        for (MTEHatchEnergy h : validMTEList(mEnergyHatches)) {
            hatches.add(h);
        }
        long[] stored = new long[hatches.size()];
        for (int i = 0; i < stored.length; i++) {
            stored[i] = hatches.get(i)
                .getBaseMetaTileEntity()
                .getStoredEU();
        }
        return RampartPower.canPay(stored, eu) && drainEnergyInput(eu);
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean willExplodeInRain() {
        // GGS-08-81: the Rampart stands outside.
        return false;
    }

    // --- ammunition --------------------------------------------------------------------------------------------

    /** @return the shared ammunition store: the Input Buses in, the Output Buses for the empties (GGS-08-77) */
    public TurretAmmo.Store ammoStore() {
        return new BusStore();
    }

    private static boolean isItem(ItemStack stack, String registryId) {
        return stack != null && stack.stackSize > 0
            && Naming.qualifiedName(registryId)
                .equals(Item.itemRegistry.getNameForObject(stack.getItem()));
    }

    /** The buses as the pure ammo rules see them. */
    private final class BusStore implements TurretAmmo.Store {

        @Override
        public int count(String ammoId) {
            int found = 0;
            for (MTEHatchInputBus bus : validMTEList(mInputBusses)) {
                for (int i = 0; i < bus.getSizeInventory(); i++) {
                    ItemStack s = bus.getStackInSlot(i);
                    if (isItem(s, ammoId)) {
                        found += s.stackSize;
                    }
                }
            }
            return found;
        }

        @Override
        public int take(String ammoId, int max) {
            int taken = 0;
            for (MTEHatchInputBus bus : validMTEList(mInputBusses)) {
                for (int i = 0; i < bus.getSizeInventory() && taken < max; i++) {
                    ItemStack s = bus.getStackInSlot(i);
                    if (isItem(s, ammoId)) {
                        int n = Math.min(max - taken, s.stackSize);
                        s.stackSize -= n;
                        taken += n;
                        if (s.stackSize <= 0) {
                            bus.setInventorySlotContents(i, null);
                        }
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
            ItemStack rest = new ItemStack(item, count);
            for (MTEHatchOutputBus bus : validMTEList(mOutputBusses)) {
                if (rest.stackSize > 0) {
                    bus.storePartial(rest, false);
                }
            }
            if (TurretAmmo.shouldDrop(rest.stackSize)) {
                // MUTL-24: only a real overflow is dropped, never a zero-size stack.
                IGregTechTileEntity base = getBaseMetaTileEntity();
                base.getWorld()
                    .spawnEntityInWorld(
                        new EntityItem(
                            base.getWorld(),
                            base.getXCoord() + 0.5,
                            base.getYCoord() + 1.2,
                            base.getZCoord() + 0.5,
                            rest));
            }
        }
    }

    // --- security ----------------------------------------------------------------------------------------------

    /** @return the owner's UUID; null for an ownerless controller */
    public UUID ownerId() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base == null ? null : base.getOwnerUuid();
    }

    /** @return who the mounts leave alone; set once here for every mount (GGS-08-79) */
    public SecurityLevel security() {
        return security;
    }

    /** @return the Target Animals switch, for every mount */
    public boolean targetsAnimals() {
        return attackAnimals;
    }

    /**
     * Replaces the friend source of this Rampart, for the scenarios that need a friend without ServerUtilities.
     *
     * @param provider the stub; null restores the pack's provider
     */
    public void setFriendProvider(FriendProvider provider) {
        friendOverride = provider;
    }

    private FriendProvider friends() {
        return friendOverride != null ? friendOverride : FriendProviders.current();
    }

    /** @return the settings the target rules read for every mount */
    public TurretTargetSelector.Settings targetSettings() {
        return new TurretTargetSelector.Settings(security, ownerId(), attackAnimals, friends());
    }

    /**
     * @param player a player
     * @return true when the security level lets this player use the mount hatches
     */
    public boolean mayUse(EntityPlayer player) {
        return player != null && OwnerAccess.mayUse(security, ownerId(), player.getUniqueID(), friends());
    }

    /**
     * @param player a player
     * @return true when this player may change the settings: the owner only (any player for an ownerless block)
     */
    public boolean mayChangeSecurity(EntityPlayer player) {
        return player != null && OwnerAccess.mayUse(SecurityLevel.OWNER, ownerId(), player.getUniqueID(), friends());
    }

    /**
     * Cycles the security level if the player may.
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
     * Toggles Target Animals if the player may.
     *
     * @param player the pressing player
     * @return true when it changed
     */
    public boolean toggleAnimals(EntityPlayer player) {
        if (!mayChangeSecurity(player)) {
            return false;
        }
        attackAnimals = !attackAnimals;
        return true;
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (getBaseMetaTileEntity().isClientSide()) {
            return;
        }
        if (player.isSneaking()) {
            if (toggleAnimals(player)) {
                player.addChatMessage(
                    new ChatComponentTranslation(
                        attackAnimals ? "gregguns.rampart.animals_on" : "gregguns.rampart.animals_off"));
            } else {
                player.addChatMessage(new ChatComponentTranslation("gregguns.rampart.not_owner"));
            }
        } else if (cycleSecurity(player)) {
            player.addChatMessage(new ChatComponentTranslation("gregguns.rampart.security." + security.id()));
        } else {
            player.addChatMessage(new ChatComponentTranslation("gregguns.rampart.not_owner"));
        }
    }

    @Override
    public void onBlockDestroyed() {
        // GGS-08-80: breaking the structure's controller kills every mount.
        formed = false;
        mounts.releaseAll();
        super.onBlockDestroyed();
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setByte(KEY_SECURITY, (byte) security.id());
        nbt.setBoolean(KEY_ANIMALS, attackAnimals);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        security = SecurityLevel.fromId(nbt.getByte(KEY_SECURITY));
        attackAnimals = nbt.getBoolean(KEY_ANIMALS);
    }

    // --- tooltip -----------------------------------------------------------------------------------------------

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(lang("type"))
            .addInfo(lang("info.mounts"))
            .addInfo(lang("info.ammo"))
            .addInfo(lang("info.armor"))
            .addInfo(lang("info.security"))
            .addInfo(lang("info.power"))
            .beginVariableStructureBlock(
                RampartShape.totalLength(RampartShape.MIN_MOUNTS),
                RampartShape.totalLength(RampartShape.MAX_MOUNTS),
                RampartShape.GATE_HEIGHT,
                RampartShape.GATE_HEIGHT,
                RampartShape.DEPTH,
                RampartShape.DEPTH,
                false)
            .addController(lang("structure.controller"))
            .addCasingInfoRange(StatCollector.translateToLocal("gregguns.casings.7.name"), 0, 999, false)
            .addOtherStructurePart(lang("structure.hatch"), lang("structure.hatch.info"), 2)
            .addInputBus(lang("structure.gate"), 1)
            .addOutputBus(lang("structure.gate"), 1)
            .addEnergyHatch(lang("structure.gate"), 1)
            .addMaintenanceHatch(lang("structure.gate"), 1)
            .addAir(lang("structure.air"))
            .addStructureInfo(lang("structure.footer"))
            .addSubChannel(GTStructureChannels.STRUCTURE_LENGTH)
            .toolTipFinisher(com.gregguns.core.text.AddedBy.NAME);
        return tt;
    }

    private static String lang(String key) {
        return StatCollector.translateToLocal("gregguns.machine.rampart." + key);
    }

    // --- textures ----------------------------------------------------------------------------------------------

    /** The overlay icons, requested when the first controller is constructed (before GT stitches textures). */
    private static final class Overlays {

        static final IIconContainer FRONT = icon("OVERLAY_FRONT");
        static final IIconContainer FRONT_ACTIVE = icon("OVERLAY_FRONT_ACTIVE");
        static final IIconContainer FRONT_ACTIVE_GLOW = icon("OVERLAY_FRONT_ACTIVE_GLOW");

        private Overlays() {}

        static void load() {
            // Loading the class runs the initializers above.
        }

        private static IIconContainer icon(String file) {
            return Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file);
        }
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        ITexture casing = getCasingTexture();
        if (side != facing) {
            return new ITexture[] { casing };
        }
        if (!active) {
            return new ITexture[] { casing, TextureFactory.of(Overlays.FRONT) };
        }
        return new ITexture[] { casing, TextureFactory.of(Overlays.FRONT_ACTIVE), TextureFactory.builder()
            .addIcon(Overlays.FRONT_ACTIVE_GLOW)
            .glow()
            .build() };
    }

    @Override
    public ITexture getCasingTexture() {
        return Textures.BlockIcons.getCasingTextureForId(GregGunsCasings.textureIndex(GregGunsCasings.BASTION_IRON));
    }

    // --- construction ------------------------------------------------------------------------------------------

    private static int mountsFor(ItemStack trigger) {
        if (GTStructureChannels.STRUCTURE_LENGTH.hasValue(trigger)) {
            return GTStructureChannels.STRUCTURE_LENGTH
                .getValueClamped(trigger, RampartShape.MIN_MOUNTS, RampartShape.MAX_MOUNTS);
        }
        return RampartShape.clampMounts(trigger.stackSize);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        int count = mountsFor(trigger);
        buildPiece(PIECE_GATE, trigger, hintsOnly, RampartShape.CONTROLLER_COLUMN, RampartShape.GATE_CONTROLLER_ROW, 0);
        for (int i = 0; i < count; i++) {
            buildPiece(PIECE_BAY, trigger, hintsOnly, RampartShape.bayOffset(i), RampartShape.WALL_CONTROLLER_ROW, 0);
        }
        buildPiece(PIECE_CAP, trigger, hintsOnly, RampartShape.capOffset(count), RampartShape.WALL_CONTROLLER_ROW, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) {
            return -1;
        }
        int count = mountsFor(trigger);
        int built = survivalBuildPiece(
            PIECE_GATE,
            trigger,
            RampartShape.CONTROLLER_COLUMN,
            RampartShape.GATE_CONTROLLER_ROW,
            0,
            elementBudget,
            env,
            false,
            true);
        if (built >= 0) {
            return built;
        }
        for (int i = 0; i < count; i++) {
            built = survivalBuildPiece(
                PIECE_BAY,
                trigger,
                RampartShape.bayOffset(i),
                RampartShape.WALL_CONTROLLER_ROW,
                0,
                elementBudget,
                env,
                false,
                true);
            if (built >= 0) {
                return built;
            }
        }
        return survivalBuildPiece(
            PIECE_CAP,
            trigger,
            RampartShape.capOffset(count),
            RampartShape.WALL_CONTROLLER_ROW,
            0,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        // The wall stands upright: no pointing up or down, no roll, no upside-down.
        return (d, r, f) -> (d.flag & (ForgeDirection.UP.flag | ForgeDirection.DOWN.flag)) == 0 && r.isNotRotated()
            && !f.isVerticallyFliped();
    }

    @Override
    public boolean isRotationChangeAllowed() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GTCEU_LOOP_COMPRESSOR;
    }
}
