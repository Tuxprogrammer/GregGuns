package com.gregguns.icbm.defense;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.icbm.component.RadarView;
import com.gregguns.icbm.detect.ContactSource;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * The small radar (D125, D190): a 3x3 Radar Casing base, two high, with a pedestal
 * and a spinning dish on top ({@link EntityRadarDish}). The controller's voltage tier sets the range
 * ({@link RadarRules#range}: HV 128, EV 256) and the power (a quarter amp of that tier while it runs); an Energy Hatch
 * of that tier or better pays for it. Contacts, the protected radius, the redstone outputs and the data stick link are
 * the tower's ({@link RadarUnit}). A controller with no structure does nothing: no scan, no outputs, no dish. Construct
 * only inside GT's MTE window, after {@link GregGunsCasings#create} has run. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The small radar)
// Source (the pattern: a non-recipe multiblock whose checkProcessing sets power and time):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L877
public final class MTERadar extends MTEExtendedPowerMultiBlockBase<MTERadar>
    implements ISurvivalConstructable, ICasingTextureProvider, RadarSource, RadarView, RadarPanel, RadarDishHost {

    private static final String PIECE = "radar";
    private static final int CYCLE_TICKS = 20;
    /** Scan steps a fresh load may stay unformed before the dish is taken down. */
    private static final int UNFORMED_GRACE_STEPS = 20;

    private final RadarUnit unit = new RadarUnit();
    private final RadarDishKeeper dish = new RadarDishKeeper(RadarShape.DISH_HEIGHT, 1.0F);
    private final int tier;
    private final IIconContainer front;
    private final IIconContainer frontActive;
    private int casingCount;
    private boolean online;
    private int unformedSteps;
    private int steps;
    /** Set when the machine stopped itself (power loss, a broken structure, repairs); cleared once it restarts. */
    private boolean resumeWhenPowered;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id           the MTE id
     * @param name         GT's internal name
     * @param nameRegional the English display name
     * @param tier         the controller's voltage index (HV 3, EV 4)
     * @param iconDir      the overlay folder under {@code textures/blocks/}, with a trailing slash
     */
    public MTERadar(int id, String name, String nameRegional, int tier, String iconDir) {
        super(id, name, nameRegional);
        this.tier = tier;
        this.front = Textures.BlockIcons.customOptional(GregGuns.MODID, iconDir + "OVERLAY_FRONT");
        this.frontActive = Textures.BlockIcons.customOptional(GregGuns.MODID, iconDir + "OVERLAY_FRONT_ACTIVE");
    }

    private MTERadar(String name, int tier, IIconContainer front, IIconContainer frontActive) {
        super(name);
        this.tier = tier;
        this.front = front;
        this.frontActive = frontActive;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTERadar(mName, tier, front, frontActive);
    }

    private static final class Structure {

        static final IStructureDefinition<MTERadar> DEFINITION = build();

        private Structure() {}

        private static IStructureElement<MTERadar> casing() {
            return lazy(() -> ofBlock(GregGunsCasings.INSTANCE, GregGunsCasings.RADAR_CASING));
        }

        private static IStructureDefinition<MTERadar> build() {
            int casingIndex = GregGunsCasings.textureIndex(GregGunsCasings.RADAR_CASING);
            return StructureDefinition.<MTERadar>builder()
                .addShape(PIECE, RadarShape.PIECES[0])
                .addElement('C', onElementPass(MTERadar::onCasingAdded, casing()))
                .addElement(
                    'H',
                    buildHatchAdder(MTERadar.class).atLeast(Energy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(1)
                        .buildAndChain(onElementPass(MTERadar::onCasingAdded, casing())))
                .build();
        }
    }

    private void onCasingAdded() {
        casingCount++;
    }

    @Override
    public IStructureDefinition<MTERadar> getStructureDefinition() {
        return Structure.DEFINITION;
    }

    @Override
    public void checkMachine(IGregTechTileEntity baseMetaTileEntity, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        if (!checkPiece(PIECE, RadarShape.CONTROLLER_COLUMN, RadarShape.CONTROLLER_ROW, 0, errors)) {
            return;
        }
        checkCasingMin(errors, casingCount, RadarShape.CASING_MINIMUM);
        checkHasAnyEnergy(errors);
        checkHasMaintenanceHatch(errors);
    }

    // --- RadarSource ------------------------------------------------------------------------------------------

    /** @return the voltage index of the weakest Energy Hatch, or -1 when there is none */
    int hatchTier() {
        int lowest = Integer.MAX_VALUE;
        for (MTEHatch hatch : validMTEList(mEnergyHatches)) {
            lowest = Math.min(lowest, hatch.mTier);
        }
        return lowest == Integer.MAX_VALUE ? -1 : lowest;
    }

    /** @return whether the last structure check found a complete radar */
    @Override
    public boolean isFormed() {
        return mMachine;
    }

    @Override
    public boolean online() {
        return online && mMachine;
    }

    @Override
    public Sweep sweep() {
        return online() ? unit.sweep() : Sweep.EMPTY;
    }

    @Override
    public int range() {
        return online() ? RadarRules.range(tier) : 0;
    }

    @Override
    public int protectedRadius() {
        return unit.radius();
    }

    /** @return the detection range this controller gives, online or not */
    @Override
    public int rangeIfOn() {
        return RadarRules.range(tier);
    }

    /** @return the controller's voltage index (HV 3, EV 4) */
    public int tier() {
        return tier;
    }

    /**
     * Sets the protected radius, clamped to the range; for scenarios and the GUI field.
     *
     * @param radius blocks
     */
    public void setRadius(int radius) {
        unit.setRadius(radius, rangeIfOn());
        getBaseMetaTileEntity().markDirty();
    }

    @Override
    public String radiusText() {
        return unit.radiusText(getBaseMetaTileEntity().getTimer());
    }

    @Override
    public void setRadiusText(String text) {
        unit.typeRadius(text, rangeIfOn(), getBaseMetaTileEntity().getTimer());
        getBaseMetaTileEntity().markDirty();
    }

    // --- RadarView (OpenComputers) -------------------------------------------------------------------------------

    @Override
    public int setProtectedRadius(int blocks) {
        setRadius(blocks);
        return unit.radius();
    }

    @Override
    public double centerX() {
        return getBaseMetaTileEntity().getXCoord() + 0.5;
    }

    @Override
    public double centerZ() {
        return getBaseMetaTileEntity().getZCoord() + 0.5;
    }

    @Override
    public List<? extends ContactSource> contacts() {
        return sweep().detected();
    }

    @Override
    public int detectedCount() {
        return unit.sweep()
            .detected()
            .size();
    }

    @Override
    public int incomingCount() {
        return unit.sweep()
            .incoming()
            .size();
    }

    @Override
    public String panelTitleKey() {
        return "gregguns.icbm.radar.gui.title";
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new RadarMultiGui<>(this);
    }

    /** @return what the radar reports now */
    public RadarRules.State state() {
        return unit.state();
    }

    /** @return the redstone strength last published */
    public int signal() {
        return unit.signal();
    }

    // --- running ----------------------------------------------------------------------------------------------

    @Override
    public @Nonnull CheckRecipeResult checkProcessing() {
        if (!ModuleState.isActive(ModuleId.ICBM)) {
            return CheckRecipeResultRegistry.NO_RECIPE; // the module is off: registered but inert
        }
        int t = hatchTier();
        if (t < 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        if (t < tier) {
            return SimpleCheckRecipeResult.ofFailure("gregguns_radar_tier");
        }
        mMaxProgresstime = CYCLE_TICKS;
        lEUt = -RadarRules.euPerTick(GTValues.V[tier]);
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM)) {
            return;
        }
        if (tick % RadarRules.SCAN_TICKS != 0) {
            return;
        }
        scanNow();
    }

    /** One scan now: it runs while the structure is formed and a cycle is paid for. */
    void scanNow() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        resumeIfPowered(base);
        online = mMachine && base.isActive() && hatchTier() >= tier;
        unit.scan(base.getWorld(), base.getXCoord() + 0.5, base.getZCoord() + 0.5, rangeIfOn(), online);
        // The active flag is the machine's own "running" state here, so only the redstone strength is driven.
        unit.publishRedstone(base);
        tendDish(base);
    }

    /**
     * The machine's own stops (not a player's soft mallet) are not final here: the I4 radar resumed by itself when
     * power returned, and a radar that stays off without saying why is a trap, so a restart follows (D195).
     */
    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        super.stopMachine(reason);
        if (reason == ShutDownReasonRegistry.POWER_LOSS || reason == ShutDownReasonRegistry.STRUCTURE_INCOMPLETE
            || reason == ShutDownReasonRegistry.NO_REPAIR) {
            resumeWhenPowered = true;
        }
    }

    /**
     * Gets a radar that is formed, allowed to work and paid for but not running going again. GT's controller only
     * looks for a recipe when something pushes it to (no periodic polling), so a radar that found no power at its
     * first check, or was switched back on after a stop, would otherwise sit idle with power in its hatch. After a
     * stop of its own (D195) it is also switched back on first.
     */
    private void resumeIfPowered(IGregTechTileEntity base) {
        if (!mMachine) {
            return;
        }
        long stored = 0;
        for (MTEHatch hatch : validMTEList(mEnergyHatches)) {
            stored += hatch.getBaseMetaTileEntity()
                .getStoredEU();
        }
        if (hatchTier() < tier || stored < 2 * RadarRules.euPerTick(GTValues.V[tier])) {
            return;
        }
        if (resumeWhenPowered && !base.isAllowedToWork()) {
            base.enableWorking();
        }
        resumeWhenPowered = false;
        if (base.isAllowedToWork() && !base.isActive()) {
            scheduleRecipeCheckImmediate();
        }
    }

    /** Spawns the dish when formed, spins it while the radar runs, and takes it down once the structure is gone. */
    private void tendDish(IGregTechTileEntity base) {
        if (mMachine) {
            unformedSteps = 0;
            dish.update(base, online(), ++steps % RadarDishKeeper.CHECK_STEPS == 0);
        } else if (++unformedSteps >= UNFORMED_GRACE_STEPS) {
            dish.remove();
        }
    }

    /** @return the dish entity now standing on the radar, or null (scenarios read it) */
    public EntityRadarDish dish() {
        return dish.dish();
    }

    /**
     * A tile read from a world saved with the I4 single-block radar may hold a data stick in slot 0, where that block
     * kept it; this multiblock has no use for that slot, so the stick is handed back instead of being lost.
     */
    @Override
    public void onFirstTick(IGregTechTileEntity base) {
        super.onFirstTick(base);
        if (base.isClientSide() || mInventory == null || mInventory.length == 0 || mInventory[0] == null) {
            return;
        }
        ItemStack stick = mInventory[0];
        mInventory[0] = null;
        EntityItem drop = new EntityItem(
            base.getWorld(),
            base.getXCoord() + 0.5,
            base.getYCoord() + 1.0,
            base.getZCoord() + 0.5,
            stick);
        base.getWorld()
            .spawnEntityInWorld(drop);
        base.markDirty();
    }

    @Override
    public void onRemoval() {
        dish.remove();
        super.onRemoval();
    }

    /**
     * GT only exposes a machine's per-side redstone strength to the world when the machine says so (default false), so
     * without this the strength set by the scan never reaches a neighboring lamp.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L1726-L1729
    // (getGeneralRS returns 0 unless allowGeneralRedstoneOutput(); default false in CommonMetaTileEntity#L610)
    @Override
    public boolean allowGeneralRedstoneOutput() {
        return true;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player, ForgeDirection side, float x, float y,
        float z) {
        com.gregguns.icbm.complex.gen.GtMachineHook.handOver(base, player); // a generated one becomes the player's
        if (!base.isClientSide() && RadarUnit.isStick(player.getHeldItem())) {
            RadarUnit.writeLink(base, player.getHeldItem(), player);
            return true;
        }
        return super.onRightclick(base, player, side, x, y, z);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        unit.save(tag);
        tag.setBoolean("resumeWhenPowered", resumeWhenPowered);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        unit.load(tag);
        resumeWhenPowered = tag.getBoolean("resumeWhenPowered");
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

    // --- tooltip ----------------------------------------------------------------------------------------------

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(lang("type"))
            .addInfo(lang("info.scan"))
            .addInfo(lang("info.range"))
            .addInfo(lang("info.link"))
            .addInfo(lang("info.outputs"))
            .addInfo(lang("info.dish"))
            .beginStructureBlock(RadarShape.WIDTH, RadarShape.HEIGHT, RadarShape.DEPTH, false)
            .addController(lang("structure.controller"))
            .addCasingInfoRange(
                StatCollector.translateToLocal("gregguns.casings.11.name"),
                RadarShape.CASING_MINIMUM,
                RadarShape.casingPositions(),
                false)
            .addEnergyHatch(lang("structure.hatch"), 1)
            .addMaintenanceHatch(lang("structure.hatch"), 1)
            .addStructureInfo(lang("structure.pedestal"))
            .toolTipFinisher(com.gregguns.core.text.AddedBy.NAME);
        return tt;
    }

    private static String lang(String key) {
        return StatCollector.translateToLocal("gregguns.machine.radar." + key);
    }

    // --- textures ---------------------------------------------------------------------------------------------

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        ITexture casing = getCasingTexture();
        if (side != facing) {
            return new ITexture[] { casing };
        }
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? frontActive : front)
            .extFacing()
            .build() };
    }

    @Override
    public ITexture getCasingTexture() {
        return Textures.BlockIcons.getCasingTextureForId(GregGunsCasings.textureIndex(GregGunsCasings.RADAR_CASING));
    }

    // --- construction -----------------------------------------------------------------------------------------

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(PIECE, trigger, hintsOnly, RadarShape.CONTROLLER_COLUMN, RadarShape.CONTROLLER_ROW, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) {
            return -1;
        }
        return survivalBuildPiece(
            PIECE,
            trigger,
            RadarShape.CONTROLLER_COLUMN,
            RadarShape.CONTROLLER_ROW,
            0,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> (d.flag & (ForgeDirection.UP.flag | ForgeDirection.DOWN.flag)) == 0 && r.isNotRotated()
            && !f.isVerticallyFliped();
    }

    @Override
    public boolean isRotationChangeAllowed() {
        return false;
    }
}
