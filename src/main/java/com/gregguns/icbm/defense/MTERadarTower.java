package com.gregguns.icbm.defense;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.MultiAmpEnergy;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.List;

import javax.annotation.Nonnull;

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
import gregtech.api.structure.error.StructureErrorRegistry;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * The radar tower (D14): an IV-or-better multiblock whose weakest Energy Hatch sets the range ({@link RadarRules#range}
 * from IV 512 up to UHV 1536), the way the Missile Silo's hatch sets the missile tier it handles. There is no recipe
 * map: while it is formed and paid for, each one-second cycle repeats and the controller sweeps every
 * {@link RadarRules#SCAN_TICKS} ticks. It reports through the same outputs as the small radar. Construct only inside
 * GT's MTE window, after {@link GregGunsCasings#create} has run. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Tower)
// Source (the pattern: a non-recipe multiblock whose checkProcessing sets power and time):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L877
public final class MTERadarTower extends MTEExtendedPowerMultiBlockBase<MTERadarTower>
    implements ISurvivalConstructable, ICasingTextureProvider, RadarSource, RadarView, RadarPanel, RadarDishHost {

    private static final String PIECE = "tower";
    private static final String OVERLAY_DIR = "machines/thirdparty/ntm/radar_tower/";
    private static final int CYCLE_TICKS = 20;

    private final RadarUnit unit = new RadarUnit();
    /** Ticks (in scan steps) a fresh load may stay unformed before the dish is taken down. */
    private static final int UNFORMED_GRACE_STEPS = 20;
    /** The tower's dish is half again the small radar's. */
    private static final float DISH_SCALE = 1.5F;

    private final RadarDishKeeper dish = new RadarDishKeeper(RadarTowerShape.DISH_HEIGHT, DISH_SCALE);
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
     */
    public MTERadarTower(int id, String name, String nameRegional) {
        super(id, name, nameRegional);
        Overlays.load();
    }

    private MTERadarTower(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTERadarTower(mName);
    }

    private static final class Structure {

        static final IStructureDefinition<MTERadarTower> DEFINITION = build();

        private Structure() {}

        private static IStructureElement<MTERadarTower> casing(int meta) {
            return lazy(() -> ofBlock(GregGunsCasings.INSTANCE, meta));
        }

        private static IStructureDefinition<MTERadarTower> build() {
            int casingIndex = GregGunsCasings.textureIndex(GregGunsCasings.RADAR_CASING);
            return StructureDefinition.<MTERadarTower>builder()
                .addShape(PIECE, RadarTowerShape.PIECES[0])
                .addElement('C', onElementPass(MTERadarTower::onCasingAdded, casing(GregGunsCasings.RADAR_CASING)))
                .addElement(
                    'H',
                    buildHatchAdder(MTERadarTower.class).atLeast(Energy, MultiAmpEnergy, Maintenance)
                        .casingIndex(casingIndex)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(MTERadarTower::onCasingAdded, casing(GregGunsCasings.RADAR_CASING))))
                .addElement('B', casing(GregGunsCasings.GANTRY_BEAM))
                .build();
        }
    }

    private void onCasingAdded() {
        casingCount++;
    }

    @Override
    public IStructureDefinition<MTERadarTower> getStructureDefinition() {
        return Structure.DEFINITION;
    }

    @Override
    public void checkMachine(IGregTechTileEntity baseMetaTileEntity, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        if (!checkPiece(PIECE, RadarTowerShape.CONTROLLER_COLUMN, RadarTowerShape.CONTROLLER_ROW, 0, errors)) {
            return;
        }
        checkCasingMin(errors, casingCount, RadarTowerShape.CASING_MINIMUM);
        checkHasAnyEnergy(errors);
        checkHasMaintenanceHatch(errors);
        if (!mExoticEnergyHatches.isEmpty() && !mEnergyHatches.isEmpty()) {
            errors.add(StructureErrorRegistry.ONE_ENERGY_HATCH_ON_MULTI_OR_LASER);
        }
    }

    // --- RadarSource ------------------------------------------------------------------------------------------

    /** @return the voltage index of the weakest Energy Hatch, or -1 when there is none */
    int hatchTier() {
        int lowest = Integer.MAX_VALUE;
        for (MTEHatch hatch : validMTEList(mEnergyHatches)) {
            lowest = Math.min(lowest, hatch.mTier);
        }
        for (MTEHatch hatch : validMTEList(mExoticEnergyHatches)) {
            lowest = Math.min(lowest, hatch.mTier);
        }
        return lowest == Integer.MAX_VALUE ? -1 : lowest;
    }

    /** @return whether the last structure check found a complete tower */
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
        return online() ? RadarRules.range(hatchTier()) : 0;
    }

    @Override
    public int protectedRadius() {
        return unit.radius();
    }

    /** @return the range the hatches give, whether or not it runs now; 0 below IV */
    @Override
    public int rangeIfOn() {
        int t = hatchTier();
        return t < RadarRules.TOWER_MIN_TIER ? 0 : RadarRules.range(t);
    }

    /**
     * Sets the protected radius, clamped to the hatch range (or to the IV range
     * when there is no hatch yet); for scenarios and the GUI field.
     *
     * @param radius blocks
     */
    public void setRadius(int radius) {
        unit.setRadius(radius, radiusCeiling());
        getBaseMetaTileEntity().markDirty();
    }

    private int radiusCeiling() {
        int range = rangeIfOn();
        return range > 0 ? range : RadarRules.range(RadarRules.TOWER_MIN_TIER);
    }

    @Override
    public String radiusText() {
        return Integer.toString(unit.radius());
    }

    @Override
    public void setRadiusText(String text) {
        try {
            setRadius(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            // Ignored: the field keeps its value.
        }
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
        return "gregguns.icbm.radar_tower.gui.title";
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new RadarMultiGui<>(this);
    }

    /** @return what the tower reports now */
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
        if (t < RadarRules.TOWER_MIN_TIER) {
            return SimpleCheckRecipeResult.ofFailure("gregguns_radar_tower_tier");
        }
        mMaxProgresstime = CYCLE_TICKS;
        lEUt = -RadarRules.euPerTick(GTValues.V[t]);
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
        online = mMachine && base.isActive() && rangeIfOn() > 0;
        unit.scan(base.getWorld(), base.getXCoord() + 0.5, base.getZCoord() + 0.5, rangeIfOn(), online);
        // The active flag is the machine's own "running" state here, so only the redstone strength is driven.
        unit.publishRedstone(base);
        tendDish(base);
    }

    /** The machine's own stops are not final here, as on the small radar (D195). */
    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        super.stopMachine(reason);
        if (reason == ShutDownReasonRegistry.POWER_LOSS || reason == ShutDownReasonRegistry.STRUCTURE_INCOMPLETE
            || reason == ShutDownReasonRegistry.NO_REPAIR) {
            resumeWhenPowered = true;
        }
    }

    /**
     * Gets a tower that is formed, allowed to work and paid for but not running going again: GT's controller only
     * looks for a recipe when pushed, and after a stop of its own (D195) it is switched back on first.
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
        int t = hatchTier();
        if (t < RadarRules.TOWER_MIN_TIER || stored < 2 * RadarRules.euPerTick(GTValues.V[t])) {
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

    /** Spawns the dish when formed, spins it while the tower runs, and takes it down once the structure is gone. */
    private void tendDish(IGregTechTileEntity base) {
        if (mMachine) {
            unformedSteps = 0;
            dish.update(base, online(), ++steps % RadarDishKeeper.CHECK_STEPS == 0);
        } else if (++unformedSteps >= UNFORMED_GRACE_STEPS) {
            dish.remove();
        }
    }

    /** @return the dish entity now standing on the tower, or null (scenarios read it) */
    public EntityRadarDish dish() {
        return dish.dish();
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
            .beginStructureBlock(RadarTowerShape.WIDTH, RadarTowerShape.HEIGHT, RadarTowerShape.DEPTH, false)
            .addController(lang("structure.controller"))
            .addCasingInfoRange(
                StatCollector.translateToLocal("gregguns.casings.11.name"),
                RadarTowerShape.CASING_MINIMUM,
                RadarTowerShape.casingPositions(),
                false)
            .addEnergyHatch(lang("structure.hatch"), 1)
            .addMaintenanceHatch(lang("structure.hatch"), 1)
            .addCasing(Integer.toString(RadarTowerShape.count('B')), lang("structure.mast"), false)
            .toolTipFinisher(com.gregguns.core.text.AddedBy.NAME);
        return tt;
    }

    private static String lang(String key) {
        return StatCollector.translateToLocal("gregguns.machine.radar_tower." + key);
    }

    // --- textures ---------------------------------------------------------------------------------------------

    /** The overlay icons, requested when the first controller is constructed (before GT stitches textures). */
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

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        ITexture casing = getCasingTexture();
        if (side != facing) {
            return new ITexture[] { casing };
        }
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? Overlays.FRONT_ACTIVE : Overlays.FRONT)
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
        buildPiece(PIECE, trigger, hintsOnly, RadarTowerShape.CONTROLLER_COLUMN, RadarTowerShape.CONTROLLER_ROW, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) {
            return -1;
        }
        return survivalBuildPiece(
            PIECE,
            trigger,
            RadarTowerShape.CONTROLLER_COLUMN,
            RadarTowerShape.CONTROLLER_ROW,
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
