package com.gregguns.machine.reaction;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.gregguns.GregGuns;
import com.gregguns.core.config.MachinesConfig;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrorRegistry;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * The Retort (MTE 20108): the Reaction Chamber as a flask-shaped StructureLib multiblock. A 5 x (7 + n) x 5 vessel of
 * Reaction Casing and borosilicate glass on steel feet, grown by 2 to 6 bulb layers. It runs
 * {@code gregguns.recipe.reactionchamber} with {@code 2 x n} parallels that share one intensity, so a batch succeeds
 * or fails together and a failure applies its risk once. The lowest glass tier is the pressure rating: a recipe whose
 * voltage tier is higher does not start.
 *
 * <p>
 * Hatches: input bus, output bus, input hatch, energy hatch and maintenance hatch in the floor and low ring; the
 * Muffler Hatch on the cap. Server logic; the structure definition and tooltip exist on both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Retort, GGS-08-64 to GGS-08-68),
// reaction-chamber-multiblock.md (Integration points)
// Source for the base pattern:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEDistillationTower.java#L63-L120
// (pieces, hatch adders) and
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTENeutronActivator.java#L279-L297
// (probing a repeated piece, then the top)
public final class MTERetort extends MTEExtendedPowerMultiBlockBase<MTERetort>
    implements ISurvivalConstructable, ICasingTextureProvider, ReactionControlled {

    private static final String PIECE_BASE = "base";
    private static final String PIECE_BULB = "bulb";
    private static final String PIECE_TOP = "top";
    private static final String OVERLAY_DIR = "machines/retort/";
    private static final String NBT_STATE = "gregguns.rc.state";
    private static final String NBT_INTENSITY = "gregguns.rc.intensity";
    private static final String NBT_BULBS = "gregguns.rc.bulbs";
    private static final int EV = 4;

    private static final CheckRecipeResult WRONG_INTENSITY = SimpleCheckRecipeResult.ofFailure("gregguns_rc_intensity");
    private static final ShutDownReason REACTION_FAILED = SimpleShutDownReason.ofNormal("gregguns_rc_failed");

    private static final IStructureDefinition<MTERetort> STRUCTURE = StructureDefinition.<MTERetort>builder()
        .addShape(PIECE_BASE, transpose(blank(RetortShape.BASE)))
        .addShape(PIECE_BULB, transpose(blank(RetortShape.BULB)))
        .addShape(PIECE_TOP, transpose(blank(RetortShape.TOP)))
        .addElement(
            'B',
            buildHatchAdder(MTERetort.class).atLeast(InputBus, OutputBus, InputHatch, Energy, Maintenance)
                .casingIndex(ReactionCasingBlock.TEXTURE_ID)
                .hint(1)
                .buildAndChain(onElementPass(MTERetort::countCasing, ofBlock(ReactionCasingBlock.get(), 0))))
        .addElement(
            'C',
            buildHatchAdder(MTERetort.class).atLeast(Muffler)
                .casingIndex(ReactionCasingBlock.TEXTURE_ID)
                .hint(2)
                .buildAndChain(onElementPass(MTERetort::countCasing, ofBlock(ReactionCasingBlock.get(), 0))))
        .addElement('S', onElementPass(MTERetort::countCasing, ofBlock(ReactionCasingBlock.get(), 0)))
        .addElement('F', ofFrame(Materials.Steel))
        // Deviation: GT's own ofBlocksTiered insists every glass block is the same tier. The getter always answers
        // "not set", so mixed tiers pass and the setter keeps the lowest, which is the pressure rating (spec).
        .addElement('G', chainAllGlasses(-1, MTERetort::noteGlass, retort -> -1))
        .build();

    private final ReactionState state = new ReactionState();
    private final Random random = new Random();
    private int intensity;
    private int casings;
    private int glassTier = Integer.MAX_VALUE;
    private int bulbs;

    /**
     * The prototype that registers the controller under {@code id}.
     *
     * @param id           the MTE id
     * @param name         GT's internal name
     * @param nameRegional the English display name
     */
    public MTERetort(int id, String name, String nameRegional) {
        super(id, name, nameRegional);
    }

    private MTERetort(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tileEntity) {
        return new MTERetort(mName);
    }

    /** Shapes are written with {@code .} for "anything"; StructureLib's blank is a space. */
    private static String[][] blank(String[][] piece) {
        String[][] out = new String[piece.length][];
        for (int i = 0; i < piece.length; i++) {
            out[i] = new String[piece[i].length];
            for (int j = 0; j < piece[i].length; j++) {
                out[i][j] = piece[i][j].replace('.', ' ');
            }
        }
        return out;
    }

    private void countCasing() {
        casings++;
    }

    private void noteGlass(int tier) {
        glassTier = Math.min(glassTier, tier);
    }

    // ---- ReactionControlled ----

    @Override
    public int getIntensity() {
        return intensity;
    }

    @Override
    public void setIntensity(int value) {
        intensity = ReactionState.clampIntensity(value);
    }

    @Override
    public ReactionState getReactionState() {
        return state;
    }

    // ---- structure ----

    @Override
    public IStructureDefinition<MTERetort> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity base, ItemStack stack, List<StructureError> errors) {
        casings = 0;
        glassTier = Integer.MAX_VALUE;
        if (!checkPiece(PIECE_BASE, RetortShape.CONTROLLER_X, RetortShape.BASE_ROWS_ABOVE, 0, errors)) {
            return;
        }
        // Count the bulb layers: probe upward while the next layer is a bulb, then check the top on top of them.
        if (!mMachine) {
            bulbs = 0;
            while (bulbs < RetortShape.MAX_BULBS
                && checkPiece(PIECE_BULB, RetortShape.CONTROLLER_X, 2 + bulbs, 0, errors)) {
                bulbs++;
            }
            errors.clear();
            casings = 0;
            glassTier = Integer.MAX_VALUE;
            if (!checkPiece(PIECE_BASE, RetortShape.CONTROLLER_X, RetortShape.BASE_ROWS_ABOVE, 0, errors)) {
                return;
            }
        }
        for (int i = 0; i < bulbs; i++) {
            if (!checkPiece(PIECE_BULB, RetortShape.CONTROLLER_X, 2 + i, 0, errors)) {
                return;
            }
        }
        if (bulbs < RetortShape.MIN_BULBS) {
            errors.add(StructureErrorRegistry.TOO_SHORT_HEIGHT);
            return;
        }
        if (!checkPiece(PIECE_TOP, RetortShape.CONTROLLER_X, 2 + bulbs + RetortShape.TOP_LAYERS - 1, 0, errors)) {
            return;
        }
        checkCasingMin(errors, casings, RetortShape.casingMinimum());
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasMufflerHatch(errors);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasInputHatch(errors);
        for (MTEHatchEnergy hatch : mEnergyHatches) {
            if (hatch.mTier < EV) {
                errors.add(StructureErrors.of("gregguns.retort.error.energy_tier"));
                break;
            }
        }
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        int n = RetortShape.clampBulbs(stackSize.stackSize);
        buildPiece(PIECE_BASE, stackSize, hintsOnly, RetortShape.CONTROLLER_X, RetortShape.BASE_ROWS_ABOVE, 0);
        for (int i = 0; i < n; i++) {
            buildPiece(PIECE_BULB, stackSize, hintsOnly, RetortShape.CONTROLLER_X, 2 + i, 0);
        }
        buildPiece(PIECE_TOP, stackSize, hintsOnly, RetortShape.CONTROLLER_X, 2 + n + RetortShape.TOP_LAYERS - 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) {
            return -1;
        }
        int n = RetortShape.clampBulbs(stackSize.stackSize);
        int built = survivalBuildPiece(
            PIECE_BASE,
            stackSize,
            RetortShape.CONTROLLER_X,
            RetortShape.BASE_ROWS_ABOVE,
            0,
            elementBudget,
            env,
            false,
            true);
        if (built >= 0) {
            return built;
        }
        for (int i = 0; i < n; i++) {
            built = survivalBuildPiece(
                PIECE_BULB,
                stackSize,
                RetortShape.CONTROLLER_X,
                2 + i,
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
            PIECE_TOP,
            stackSize,
            RetortShape.CONTROLLER_X,
            2 + n + RetortShape.TOP_LAYERS,
            0,
            elementBudget,
            env,
            false,
            true);
    }

    /** @return the bulb layers found by the last structure check */
    public int getBulbs() {
        return bulbs;
    }

    /** @return the lowest glass tier found by the last structure check; {@code Integer.MAX_VALUE} if none */
    public int getGlassTier() {
        return glassTier;
    }

    // ---- recipes ----

    @Override
    public RecipeMap<?> getRecipeMap() {
        return ReactionRecipeMaps.REACTION_CHAMBER;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Nonnull
            @Override
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                int preferred = recipe.getMetadataOrDefault(ReactionRecipeMaps.PREFERRED_INTENSITY, 0);
                if (preferred != intensity) {
                    return WRONG_INTENSITY;
                }
                int needed = RetortShape.voltageIndexFor(recipe.mEUt);
                if (!RetortShape.glassAllows(glassTier, needed)) {
                    return CheckRecipeResultRegistry.insufficientMachineTier(needed);
                }
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }

            @Nonnull
            @Override
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                // A reaction runs at its own pace (reaction ticks times 60), whatever the hatch voltage.
                return super.createOverclockCalculator(recipe).setNoOverclock(true);
            }

            @Nonnull
            @Override
            protected CheckRecipeResult onRecipeStart(@Nonnull GTRecipe recipe) {
                state.start(
                    recipe.getMetadataOrDefault(ReactionRecipeMaps.PREFERRED_INTENSITY, 0),
                    recipe.getMetadataOrDefault(ReactionRecipeMaps.INTENSITY_MARGIN, 0),
                    recipe.getMetadataOrDefault(ReactionRecipeMaps.REQUIRED_COMPLETION, 1),
                    recipe.mDuration,
                    ReactionRisk
                        .byName(recipe.getMetadataOrDefault(ReactionRecipeMaps.RISK, ReactionRisk.NONE.name())));
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        }.setMaxParallelSupplier(this::getMaxParallelRecipes);
    }

    @Override
    public int getMaxParallelRecipes() {
        return RetortShape.parallels(Math.max(bulbs, 0));
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (!super.onRunningTick(stack)) {
            return false;
        }
        if (state.isActive()) {
            ReactionState.Outcome outcome = state.tick(intensity, random);
            if (outcome == ReactionState.Outcome.SUCCESS) {
                // Early finish: runMachine increments the progress right after this and delivers the outputs.
                mProgresstime = Math.max(mProgresstime, mMaxProgresstime - 1);
            } else if (outcome == ReactionState.Outcome.FAILURE) {
                ReactionRisk risk = state.risk();
                // stopMachine drops the outputs (GGS-08-21); the risk is applied once for the whole batch (GGS-08-67).
                stopMachine(REACTION_FAILED);
                ReactionEffects.applyFailure(getBaseMetaTileEntity(), risk, true);
                return false;
            }
        }
        return true;
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return MachinesConfig.retortPollutionPerSecond;
    }

    // ---- save ----

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setIntArray(NBT_STATE, state.toArray());
        nbt.setInteger(NBT_INTENSITY, intensity);
        nbt.setInteger(NBT_BULBS, bulbs);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        state.fromArray(nbt.getIntArray(NBT_STATE));
        intensity = ReactionState.clampIntensity(nbt.getInteger(NBT_INTENSITY));
        bulbs = nbt.getInteger(NBT_BULBS);
    }

    // ---- GUI ----

    @NotNull
    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new Gui(this);
    }

    /** The multiblock panel plus the intensity control. */
    private static final class Gui extends MTEMultiBlockBaseGui<MTERetort> {

        Gui(MTERetort machine) {
            super(machine);
        }

        @Override
        protected void registerSyncValues(PanelSyncManager syncManager) {
            super.registerSyncValues(syncManager);
            ReactionGuiWidgets.register(syncManager, multiblock);
        }

        @Override
        protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
            return super.createLeftPanelGapRow(parent, syncManager).child(ReactionGuiWidgets.row(syncManager));
        }
    }

    // ---- tooltip and textures ----

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(StatCollector.translateToLocal("gregguns.retort.tt.type"))
            .addInfo(StatCollector.translateToLocal("gregguns.retort.tt.info1"))
            .addInfo(StatCollector.translateToLocal("gregguns.retort.tt.info2"))
            .addInfo(StatCollector.translateToLocal("gregguns.retort.tt.info3"))
            .addPollutionAmount(MachinesConfig.retortPollutionPerSecond)
            .beginVariableStructureBlock(
                5,
                5,
                RetortShape.height(RetortShape.MIN_BULBS),
                RetortShape.height(RetortShape.MAX_BULBS),
                5,
                5,
                false)
            .addController(StatCollector.translateToLocal("gregguns.retort.tt.controller"))
            .addCasing(
                String.valueOf(RetortShape.casingMinimum()) + "+",
                StatCollector.translateToLocal("tile.gregguns.casing_reaction.name"),
                false)
            .addOtherStructurePart(
                StatCollector.translateToLocal("gregguns.retort.tt.glass"),
                StatCollector.translateToLocal("gregguns.retort.tt.glass.info"))
            .addOtherStructurePart(
                StatCollector.translateToLocal("gregguns.retort.tt.frame"),
                StatCollector.translateToLocal("gregguns.retort.tt.frame.info"))
            .addEnergyHatch("1+", StatCollector.translateToLocal("gregguns.retort.tt.low_ring"), 1)
            .addMaintenanceHatch("1", StatCollector.translateToLocal("gregguns.retort.tt.low_ring"), 1)
            .addInputBus("1+", StatCollector.translateToLocal("gregguns.retort.tt.low_ring"), 1)
            .addOutputBus("1+", StatCollector.translateToLocal("gregguns.retort.tt.low_ring"), 1)
            .addInputHatch("1+", StatCollector.translateToLocal("gregguns.retort.tt.low_ring"), 1)
            .addMufflerHatch("1", StatCollector.translateToLocal("gregguns.retort.tt.cap"), 2)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity base, ForgeDirection side, ForgeDirection facing, int colorIndex,
        boolean active, boolean redstoneLevel) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(ReactionCasingBlock.TEXTURE_ID);
        if (side == facing) {
            String file = active ? "OVERLAY_FRONT_ACTIVE" : "OVERLAY_FRONT";
            ITexture overlay = TextureFactory.builder()
                .addIcon(Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file))
                .extFacing()
                .build();
            ITexture glow = TextureFactory.builder()
                .addIcon(Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file + "_GLOW"))
                .extFacing()
                .glow()
                .build();
            return new ITexture[] { casing, overlay, glow };
        }
        return new ITexture[] { casing };
    }

    @Override
    public ITexture getCasingTexture() {
        return Textures.BlockIcons.getCasingTextureForId(ReactionCasingBlock.TEXTURE_ID);
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }
}
