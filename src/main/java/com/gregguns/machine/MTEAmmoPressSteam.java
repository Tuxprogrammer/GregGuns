package com.gregguns.machine;

import net.minecraft.util.StatCollector;

import com.gregguns.GregGuns;
import com.gregguns.recipe.GregGunsRecipeMaps;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineBronze;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTRecipe;

/**
 * The Steam (Bronze) Ammo Press: a GT bronze steam machine on the {@code gregguns.ammoPress} map. It follows GT's
 * Bronze Compressor: steam in through any side but the front, and after each recipe it must vent steam through the
 * front face (the base class handles both). It runs ULV recipes only.
 *
 * <p>
 * It has no TESR: the animation is the vanilla animated overlay strips in {@code textures/blocks/machines/
 * ammo_press_steam/}, as for every GT steam machine. Both sides; textures are client only.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md (Tiers, "3. Steam tier")
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/steam/MTESteamCompressorBronze.java#L31
// Deviation: GT pairs each bronze machine with a steel High Pressure variant. The roster has no MTE ID for one, so none
// is built; the gap is recorded in ammo-press.md.
public final class MTEAmmoPressSteam extends MTEBasicMachineBronze {

    /** Item inputs: four, as the Bronze GUI lays out. Stone Bullet recipes need fewer. */
    static final int INPUT_SLOTS = 4;
    /** Item outputs: one, because the bronze recipe check fills a single output. */
    static final int OUTPUT_SLOTS = 1;

    private static final String DIR = "machines/ammo_press_steam/";

    /**
     * The prototype that registers the machine.
     *
     * @param id           the MTE id
     * @param name         GT's internal name
     * @param nameRegional the English display name
     * @param tooltipKey   lang key of the tooltip line
     */
    public MTEAmmoPressSteam(int id, String name, String nameRegional, String tooltipKey) {
        super(id, name, nameRegional, tooltipKey, INPUT_SLOTS, OUTPUT_SLOTS, false);
    }

    private MTEAmmoPressSteam(String name, String[] description, ITexture[][][] textures) {
        super(name, description, textures, INPUT_SLOTS, OUTPUT_SLOTS, false);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTEAmmoPressSteam(mName, mDescriptionArray, mTextures);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GregGunsRecipeMaps.AMMO_PRESS;
    }

    @Override
    public int checkRecipe() {
        // Deviation: GT's bronze check accepts recipes up to LV voltage. This press runs ULV recipes only (spec), so a
        // 30 EU/t LV recipe, the 9mm bullet for one, is not available to it.
        GTRecipe recipe = getRecipeMap().findRecipeQuery()
            .items(getAllInputs())
            .voltage(TierEU.ULV)
            .find();
        if (recipe != null && canOutput(recipe.mOutputs) && recipe.isRecipeInputEqual(true, null, getAllInputs())) {
            mOutputItems[0] = recipe.rollOutput(getBaseMetaTileEntity(), 0);
            calculateCustomOverclock(recipe);
            return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
        }
        return DID_NOT_FIND_RECIPE;
    }

    @Override
    public String[] getDescription() {
        // The lang file is not loaded at construction, so the tooltip key is translated here.
        String[] lines = super.getDescription();
        lines[0] = StatCollector.translateToLocal(mDescriptionArray[0]);
        return lines;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GTCEU_LOOP_COMPRESSOR;
    }

    private static ITexture overlay(String file) {
        return TextureFactory.of(Textures.BlockIcons.customOptional(GregGuns.MODID, DIR + file));
    }

    @Override
    public ITexture[] getSideFacingActive(byte color) {
        return new ITexture[] { super.getSideFacingActive(color)[0], overlay("OVERLAY_SIDE_ACTIVE") };
    }

    @Override
    public ITexture[] getSideFacingInactive(byte color) {
        return new ITexture[] { super.getSideFacingInactive(color)[0], overlay("OVERLAY_SIDE") };
    }

    @Override
    public ITexture[] getFrontFacingActive(byte color) {
        return new ITexture[] { super.getFrontFacingActive(color)[0], overlay("OVERLAY_FRONT_ACTIVE") };
    }

    @Override
    public ITexture[] getFrontFacingInactive(byte color) {
        return new ITexture[] { super.getFrontFacingInactive(color)[0], overlay("OVERLAY_FRONT") };
    }

    @Override
    public ITexture[] getTopFacingActive(byte color) {
        return new ITexture[] { super.getTopFacingActive(color)[0], overlay("OVERLAY_TOP_ACTIVE") };
    }

    @Override
    public ITexture[] getTopFacingInactive(byte color) {
        return new ITexture[] { super.getTopFacingInactive(color)[0], overlay("OVERLAY_TOP") };
    }

    @Override
    public ITexture[] getBottomFacingActive(byte color) {
        return new ITexture[] { super.getBottomFacingActive(color)[0], overlay("OVERLAY_BOTTOM") };
    }

    @Override
    public ITexture[] getBottomFacingInactive(byte color) {
        return new ITexture[] { super.getBottomFacingInactive(color)[0], overlay("OVERLAY_BOTTOM") };
    }
}
