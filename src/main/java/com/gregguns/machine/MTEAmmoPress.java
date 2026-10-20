package com.gregguns.machine;

import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;

import com.google.common.io.ByteArrayDataInput;
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
import gregtech.common.render.IMTERenderer;
import io.netty.buffer.ByteBuf;

/**
 * The Ammo Press: a GT basic machine that runs the {@code gregguns.ammoPress} recipe map. One class serves every tier;
 * the tier is a constructor argument, so the other tiers in the roster are one more instance each.
 *
 * <p>
 * Serves LV, MV, HV and EV (the Steam press is {@link MTEAmmoPressSteam}). The front overlay animation is synced to
 * recipe progress: the server sends progress samples through {@code IMTERenderer} and the client renderer picks the
 * strip frame ({@link AmmoPressAnimation}). Other faces use GT's active-state overlays from
 * {@code textures/blocks/machines/ammo_press/}.
 *
 * <p>
 * Construct only inside GT's MTE window, after GT's preload and before its postload
 * ({@link AmmoPressMachines#register()}). Both sides; the textures exist on the client only, as for every GT machine.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md, docs/design/0011-machine-policy.md
public final class MTEAmmoPress extends MTEBasicMachineWithRecipe implements IMTERenderer {

    /** Ticks between progress samples sent to nearby clients while the press runs. */
    private static final int SYNC_INTERVAL = 10;

    /** Client side: the estimate of the recipe progress, fed by {@link #decodeRenderData}. */
    private final AmmoPressAnimation.ProgressTracker tracker = new AmmoPressAnimation.ProgressTracker();
    /** Server side: whether the last render-data packet said running. */
    private boolean lastSentRunning;

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
    public void onPostTick(IGregTechTileEntity baseMetaTileEntity, long tick) {
        super.onPostTick(baseMetaTileEntity, tick);
        if (!baseMetaTileEntity.isServerSide()) {
            return;
        }
        // Spec: ammo-press.md "Synced to recipe progress". GT does not send progress to the client outside an open GUI,
        // so the server sends it on a start, a stop and every SYNC_INTERVAL ticks, as the Cutting Machine's renderer
        // data does.
        boolean running = baseMetaTileEntity.isActive();
        if (running != lastSentRunning || running && tick % SYNC_INTERVAL == 0) {
            lastSentRunning = running;
            sendRenderDataToClient(this);
        }
    }

    @Override
    public void encodeRenderData(ByteBuf buffer) {
        buffer.writeInt(getProgresstime());
        buffer.writeInt(maxProgresstime());
        buffer.writeBoolean(getBaseMetaTileEntity().isActive());
    }

    @Override
    public void decodeRenderData(ByteArrayDataInput buffer) {
        tracker.receive(buffer.readInt(), buffer.readInt(), buffer.readBoolean());
    }

    /** @return the client's progress estimate; used by the renderer only */
    public AmmoPressAnimation.ProgressTracker progressTracker() {
        return tracker;
    }

    @Override
    public void renderTESR(double x, double y, double z, float timeSinceLastTick) {
        // Client only: GT calls this from its tile-entity renderer, never on a dedicated server.
        com.gregguns.client.machine.AmmoPressRenderer.render(this, x, y, z, timeSinceLastTick);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
    }

    // Source: MTEBasicMachine#canOutput compares output i with slot i only; see OutputFit.
    @Override
    protected boolean canOutput(ItemStack... outputs) {
        if (OutputFit.fits(getAllOutputs(), outputs)) {
            return true;
        }
        mOutputBlocked++;
        return false;
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
        // Deviation: the active front is the static front. The animated strip is drawn by AmmoPressRenderer, which
        // picks the frame from recipe progress; a vanilla animated sprite would free-run on a timer.
        return new ITexture[] { overlay("OVERLAY_SIDE_ACTIVE"), overlay("OVERLAY_SIDE"), overlay("OVERLAY_FRONT"),
            overlay("OVERLAY_FRONT"), overlay("OVERLAY_TOP_ACTIVE"), overlay("OVERLAY_TOP"),
            overlay("OVERLAY_BOTTOM_ACTIVE"), overlay("OVERLAY_BOTTOM") };
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
