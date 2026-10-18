package com.gregguns.machine;

import net.minecraft.util.StatCollector;

import com.gregguns.GregGuns;
import com.gregguns.recipe.GregGunsRecipeMaps;

import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;

/**
 * The Ammo Press: a GT basic machine that runs the {@code gregguns.ammoPress} recipe map. One class serves every tier;
 * the tier is a constructor argument, so the other tiers in the roster are one more instance each.
 *
 * <p>
 * M1 builds only the LV press (docs/gregguns-spec/08-machines-production/machine-roster.md). The recipe-progress
 * animation, the other tiers and the Ammunition Works multiblock stay in M4. Until then the press shows the static and
 * active-state overlays from {@code textures/blocks/machines/ammo_press/}, using GT's overlay mechanism.
 *
 * <p>
 * Construct only inside GT's MTE window, after GT's preload and before its postload
 * ({@link AmmoPressMachines#register()}). Both sides; the textures exist on the client only, as for every GT machine.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md, docs/design/0011-machine-policy.md
public final class MTEAmmoPress extends MTEBasicMachineWithRecipe {

    /** Item input slots: the map's six. */
    static final int INPUT_SLOTS = 6;
    /** Item output slots: the map's three. */
    static final int OUTPUT_SLOTS = 3;

    private static final String OVERLAY_DIR = "machines/ammo_press/";

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id, from {@link GregGunsMTEIDs}
     * @param name            GT's internal name, lower case with dots
     * @param nameRegional    the English display name GT registers for the block
     * @param tier            GT voltage index (1 is LV)
     * @param descriptionKeys lang keys of the tooltip lines; translated when the tooltip is shown
     * @param craftingRecipe  GT machine-recipe grid for the hand-crafted press, or {@code null}
     */
    public MTEAmmoPress(int id, String name, String nameRegional, int tier, String[] descriptionKeys,
        Object[] craftingRecipe) {
        super(
            id,
            name,
            nameRegional,
            tier,
            descriptionKeys,
            GregGunsRecipeMaps.AMMO_PRESS,
            INPUT_SLOTS,
            OUTPUT_SLOTS,
            true,
            false,
            getCapacityForTier(tier),
            SoundResource.GTCEU_LOOP_COMPRESSOR,
            SpecialEffects.NONE,
            overlays(),
            craftingRecipe);
    }

    private MTEAmmoPress(String name, int tier, String[] descriptionKeys, RecipeMap<?> recipes, int tankCapacity,
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
        MTEAmmoPress copy = new MTEAmmoPress(
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
        // The lang file is not loaded when the machine is constructed, so the keys are translated per tooltip.
        String[] lines = new String[mDescriptionArray.length];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = StatCollector.translateToLocal(mDescriptionArray[i]);
        }
        return lines;
    }

    /**
     * The eight overlays {@code MTEBasicMachine} expects, in its order: side active, side, front active, front, top
     * active, top, bottom active, bottom. Each is the PNG plus an optional glow layer; a PNG that does not exist is
     * skipped by GT's optional icon, so the set may be partial.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachineWithRecipe.java#L101-L218
    // (the string-overlay constructor builds the same eight in this order, but only under GT's own resource domain);
    // Textures.BlockIcons.customOptional(domain, path) at
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Textures.java#L2721
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
