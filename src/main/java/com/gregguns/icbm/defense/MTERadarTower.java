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
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * The radar tower (D14): an IV-or-better multiblock whose weakest Energy Hatch sets the range ({@link RadarRules#range}
 * from IV 512 up to UHV 1536), the way the Missile Silo's hatch sets the missile tier it handles. There is no recipe
 * map: while it is formed and paid for, each one-second cycle repeats and the controller sweeps every
 * {@link RadarRules#SCAN_TICKS} ticks. It reports through the same outputs as the single blocks. Construct only inside
 * GT's MTE window, after {@link GregGunsCasings#create} has run. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Tower)
// Source (the pattern: a non-recipe multiblock whose checkProcessing sets power and time):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L877
public final class MTERadarTower extends MTEExtendedPowerMultiBlockBase<MTERadarTower>
    implements ISurvivalConstructable, ICasingTextureProvider, RadarSource, RadarView {

    private static final String PIECE = "tower";
    private static final String OVERLAY_DIR = "machines/thirdparty/ntm/radar_tower/";
    private static final int CYCLE_TICKS = 20;

    private final RadarUnit unit = new RadarUnit();
    private int casingCount;
    private boolean online;

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

    /** @return the protected radius text for the GUI field */
    String radiusText() {
        return Integer.toString(unit.radius());
    }

    /** @param text what the player typed; anything that is not a number is ignored, the rest is clamped */
    void setRadiusText(String text) {
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

    /** @return how many missiles the last scan detected */
    int detectedCount() {
        return unit.sweep()
            .detected()
            .size();
    }

    /** @return how many of them are aimed inside the protected radius */
    int incomingCount() {
        return unit.sweep()
            .incoming()
            .size();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new RadarTowerGui(this);
    }

    /** @return what the tower reports now */
    public RadarRules.State state() {
        return unit.state();
    }

    /** @return the redstone strength last published */
    public int signal() {
        return Math.max(lastSignal, 0);
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
        online = mMachine && base.isActive() && rangeIfOn() > 0;
        unit.scan(base.getWorld(), base.getXCoord() + 0.5, base.getZCoord() + 0.5, rangeIfOn(), online);
        publishOutputs(base);
    }

    private void publishOutputs(IGregTechTileEntity base) {
        // The active flag is the machine's own "running" state here, so only the redstone strength is driven.
        int want = RadarRules.redstone(
            unit.sweep()
                .incoming()
                .size());
        if (want != lastSignal) {
            lastSignal = want;
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                base.setInternalOutputRedstoneSignal(d, (byte) want);
            }
        }
    }

    private int lastSignal = -1;

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
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        unit.load(tag);
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
