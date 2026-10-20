package com.gregguns.machine.reaction;

import java.util.Arrays;
import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.gregguns.GregGuns;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTRecipe;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;

/**
 * The Reaction Chamber single block (EV 20106, IV 20107): a GT basic machine that runs
 * {@code gregguns.recipe.reactionchamber} one recipe at a time, with the Techguns intensity, focus and risk gameplay.
 *
 * <p>
 * GT's basic-machine loop finds and consumes the recipe (the Heat Ray Focus is its stack-size-0, non-consumed input).
 * This class adds three rules on top: a recipe starts only if the intensity setting equals its preferred intensity;
 * the run is not overclocked and ticks a {@link ReactionState} once per running tick; and on a failed run the outputs
 * are dropped and the {@link ReactionRisk} is applied once. The single block needs no Muffler and does not pollute
 * (spec asymmetry with the Retort). Server side logic; the GUI is built on both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/reaction-chamber-multiblock.md (Single block and multiblock forms,
// GGS-08-18 to GGS-08-23, GGS-08-46)
public final class MTEReactionChamber extends MTEBasicMachineWithRecipe implements ReactionControlled {

    static final int INPUT_SLOTS = 2;
    static final int OUTPUT_SLOTS = 4;
    private static final String OVERLAY_DIR = "machines/reaction_chamber/";
    private static final String NBT_STATE = "gregguns.rc.state";
    private static final String NBT_INTENSITY = "gregguns.rc.intensity";

    private final ReactionState state = new ReactionState();
    private final Random random = new Random();
    private int intensity;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id, from {@link GregGunsMTEIDs}
     * @param name            GT's internal name
     * @param nameRegional    the English display name
     * @param tier            GT voltage index (4 EV, 5 IV)
     * @param descriptionKeys lang keys of the tooltip lines
     * @param craftingRecipe  GT machine-recipe grid, or {@code null}
     */
    public MTEReactionChamber(int id, String name, String nameRegional, int tier, String[] descriptionKeys,
        Object[] craftingRecipe) {
        super(
            id,
            name,
            nameRegional,
            tier,
            descriptionKeys,
            ReactionRecipeMaps.REACTION_CHAMBER,
            INPUT_SLOTS,
            OUTPUT_SLOTS,
            true,
            false,
            getCapacityForTier(tier),
            SoundResource.GTCEU_LOOP_CHEMICAL,
            SpecialEffects.NONE,
            overlays(),
            craftingRecipe);
    }

    private MTEReactionChamber(String name, int tier, String[] descriptionKeys, RecipeMap<?> recipes, int tankCapacity,
        int amperage, ITexture[][][] textures, SoundResource sound) {
        super(
            name,
            tier,
            descriptionKeys,
            recipes,
            INPUT_SLOTS,
            OUTPUT_SLOTS,
            true,
            false,
            tankCapacity,
            amperage,
            textures,
            sound,
            SpecialEffects.NONE);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        MTEReactionChamber copy = new MTEReactionChamber(
            mName,
            mTier,
            mDescriptionArray,
            mRecipes,
            mTankCapacity,
            mAmperage,
            mTextures,
            mSoundResource);
        copy.setProgressBarTexture(progressBarTexture);
        copy.setRecipeCatalystPriority(recipeCatalystPriority);
        return copy;
    }

    @Override
    public String[] getDescription() {
        String[] lines = new String[mDescriptionArray.length];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = StatCollector.translateToLocal(mDescriptionArray[i]);
        }
        return com.gregguns.core.text.AddedBy.append(lines);
    }

    // ---- ReactionControlled ----

    @Override
    public int getIntensity() {
        return intensity;
    }

    @Override
    public void setIntensity(int value) {
        // GGS-08-28: the server clamps whatever a client wrote.
        int clamped = ReactionState.clampIntensity(value);
        if (clamped != intensity && getBaseMetaTileEntity() != null) {
            // Wake GT's recipe check so an idle machine starts the recipe that now matches.
            getBaseMetaTileEntity().markInventoryBeenModified();
        }
        intensity = clamped;
    }

    @Override
    public ReactionState getReactionState() {
        return state;
    }

    // ---- recipe start ----

    @Override
    public int checkRecipe(boolean skipOC) {
        RecipeMap<?> map = getRecipeMap();
        if (map == null) {
            return DID_NOT_FIND_RECIPE;
        }
        // Look the recipe up first, without consuming, so a wrong intensity costs nothing (GGS-08-19).
        GTRecipe recipe = map.findRecipeQuery()
            .items(getAllInputs())
            .fluids(getFillableStack())
            .voltage(gregtech.api.enums.GTValues.V[mTier])
            .find();
        if (recipe == null) {
            return DID_NOT_FIND_RECIPE;
        }
        int preferred = recipe.getMetadataOrDefault(ReactionRecipeMaps.PREFERRED_INTENSITY, 0);
        if (preferred != intensity) {
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
        int result = super.checkRecipe(skipOC);
        if (result == FOUND_AND_SUCCESSFULLY_USED_RECIPE) {
            state.start(
                preferred,
                recipe.getMetadataOrDefault(ReactionRecipeMaps.INTENSITY_MARGIN, 0),
                recipe.getMetadataOrDefault(ReactionRecipeMaps.REQUIRED_COMPLETION, 1),
                recipe.mDuration,
                ReactionRisk.byName(recipe.getMetadataOrDefault(ReactionRecipeMaps.RISK, ReactionRisk.NONE.name())));
        }
        return result;
    }

    @Override
    protected void calculateCustomOverclock(GTRecipe recipe) {
        // A reaction runs at its own pace: the duration is reaction ticks times 60, whatever the hull tier.
        mEUt = (int) recipe.mEUt;
        mMaxProgresstime = recipe.mDuration;
    }

    // ---- per tick ----

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        if (base.isServerSide() && state.isActive()
            && mMaxProgresstime > 0
            && mProgresstime >= 0
            && base.isAllowedToWork()
            && base.getStoredEU() >= mEUt) {
            // The base class draws power and advances progress after this; the step assumes the draw succeeds.
            ReactionState.Outcome outcome = state.tick(intensity, random);
            if (outcome == ReactionState.Outcome.SUCCESS) {
                // Early finish: GT delivers the outputs on this tick's increment.
                mProgresstime = Math.max(mProgresstime, mMaxProgresstime - 1);
            } else if (outcome == ReactionState.Outcome.FAILURE) {
                failRun(base);
                return;
            }
        }
        super.onPostTick(base, tick);
    }

    private void failRun(IGregTechTileEntity base) {
        ReactionRisk risk = state.risk();
        // Inputs are already consumed; the outputs are dropped (GGS-08-21).
        Arrays.fill(mOutputItems, null);
        mOutputFluid = null;
        mEUt = 0;
        mProgresstime = 0;
        mMaxProgresstime = 0;
        base.setActive(false);
        ReactionEffects.applyFailure(base, risk, false);
    }

    // ---- save ----

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setIntArray(NBT_STATE, state.toArray());
        nbt.setInteger(NBT_INTENSITY, intensity);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        state.fromArray(nbt.getIntArray(NBT_STATE));
        intensity = ReactionState.clampIntensity(nbt.getInteger(NBT_INTENSITY));
    }

    // ---- GUI ----

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new Gui(this).build(data, syncManager, uiSettings);
    }

    /** The basic-machine panel plus the intensity control. */
    private static final class Gui extends MTEBasicMachineBaseGui<MTEReactionChamber> {

        Gui(MTEReactionChamber machine) {
            super(machine, machine.getUIProperties());
        }

        @Override
        protected void registerSyncValues(PanelSyncManager syncManager) {
            super.registerSyncValues(syncManager);
            ReactionGuiWidgets.register(syncManager, machine);
        }

        @Override
        protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
            return super.createContentSection(panel, syncManager).child(
                ReactionGuiWidgets.row(syncManager)
                    .horizontalCenter()
                    .bottomRel(0));
        }
    }

    // ---- textures ----

    // Source: same eight-overlay order as MTEAmmoPress#overlays, see
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachineWithRecipe.java#L101-L218
    private static ITexture[] overlays() {
        return new ITexture[] { overlay("OVERLAY_SIDE_ACTIVE"), overlay("OVERLAY_SIDE"),
            overlay("OVERLAY_FRONT_ACTIVE"), overlay("OVERLAY_FRONT"), overlay("OVERLAY_TOP_ACTIVE"),
            overlay("OVERLAY_TOP"), overlay("OVERLAY_BOTTOM_ACTIVE"), overlay("OVERLAY_BOTTOM") };
    }

    private static ITexture overlay(String file) {
        return TextureFactory.of(
            TextureFactory.of(Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file)),
            TextureFactory.builder()
                .addIcon(Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file + "_GLOW"))
                .glow()
                .build());
    }
}
