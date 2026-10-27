package com.gregguns.icbm.defense;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Keyboard;

import com.gregguns.GregGuns;
import com.gregguns.core.text.AddedBy;
import com.gregguns.icbm.control.LaunchControlTooltipFormat;

import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;

/**
 * What the single-block defense machines (the HV and EV radars and the air-defense battery) share: GT's hull plus a
 * front overlay, the energy input of their tier, and the two-step D41 tooltip. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md, air-defense-battery.md
abstract class DefenseBlock extends MTETieredMachineBlock implements ISecondaryDescribable {

    /** The overlay icons of one machine: front (idle and active), side, top and bottom. */
    static final class Icons {

        final IIconContainer front;
        final IIconContainer frontActive;
        final IIconContainer side;
        final IIconContainer top;
        final IIconContainer topActive;
        final IIconContainer bottom;

        private Icons(String dir) {
            front = icon(dir, "OVERLAY_FRONT");
            frontActive = icon(dir, "OVERLAY_FRONT_ACTIVE");
            side = icon(dir, "OVERLAY_SIDE");
            top = icon(dir, "OVERLAY_TOP");
            topActive = icon(dir, "OVERLAY_TOP_ACTIVE");
            bottom = icon(dir, "OVERLAY_BOTTOM");
        }

        /**
         * Requests the icons; call only while the first block is constructed (before GT stitches textures).
         *
         * @param dir the texture folder under {@code textures/blocks/}, with a trailing slash
         * @return the icons
         */
        static Icons of(String dir) {
            return new Icons(dir);
        }

        private static IIconContainer icon(String dir, String file) {
            return Textures.BlockIcons.customOptional(GregGuns.MODID, dir + file);
        }
    }

    private final Icons icons;

    /** The prototype that registers the machine. */
    DefenseBlock(int id, String name, String regional, int tier, int slots, String[] descriptionKeys, String iconDir) {
        super(id, name, regional, tier, slots, descriptionKeys);
        this.icons = Icons.of(iconDir);
    }

    /** A placed clone. */
    DefenseBlock(String name, int tier, int slots, String[] descriptionKeys, Icons icons) {
        super(name, tier, slots, descriptionKeys, null);
        this.icons = icons;
    }

    final Icons icons() {
        return icons;
    }

    @Override
    public String[] getDescription() {
        return getCurrentDescription();
    }

    @Override
    public String[] getPrimaryDescription() {
        return LaunchControlTooltipFormat
            .primary(local(0), StatCollector.translateToLocal("gregguns.icbm.control.tooltip.shift"), AddedBy.line())
            .toArray(new String[0]);
    }

    @Override
    public String[] getSecondaryDescription() {
        List<String> details = new ArrayList<>();
        for (int i = 1; i < mDescriptionArray.length; i++) {
            details.add(local(i));
        }
        List<String> lines = LaunchControlTooltipFormat.secondary(local(0), details, AddedBy.line());
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            lines = LaunchControlTooltipFormat.wrapAll(lines, ClientWrap.INSTANCE);
        }
        return lines.toArray(new String[0]);
    }

    /** Font-backed wrapper in its own class so the dedicated server never loads the client font types. */
    private static final class ClientWrap implements LaunchControlTooltipFormat.Wrapper {

        static final ClientWrap INSTANCE = new ClientWrap();
        private static final int WIDTH = 200;

        @Override
        @SuppressWarnings("unchecked")
        public List<String> wrap(String line) {
            return net.minecraft.client.Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(line, WIDTH);
        }
    }

    @Override
    public boolean isDisplaySecondaryDescription() {
        return FMLCommonHandler.instance()
            .getSide()
            .isClient() && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
    }

    private String local(int i) {
        return StatCollector.translateToLocal(mDescriptionArray[i]);
    }

    @Override
    public boolean isEnetInput() {
        return true;
    }

    @Override
    public boolean isInputFacing(ForgeDirection side) {
        return true;
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
        return GTValues.V[mTier] * 16L;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return facing != ForgeDirection.UP && facing != ForgeDirection.DOWN && facing != ForgeDirection.UNKNOWN;
    }

    /** @return true for a GT data stick */
    static boolean isStick(ItemStack stack) {
        return RadarUnit.isStick(stack);
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] textures) {
        return null;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity base, ForgeDirection side, ForgeDirection facing, int colorIndex,
        boolean active, boolean redstoneLevel) {
        ITexture casing = Textures.BlockIcons.MACHINE_CASINGS[mTier][colorIndex + 1];
        IIconContainer overlay;
        if (side == facing) {
            overlay = active ? icons.frontActive : icons.front;
        } else if (side == ForgeDirection.UP) {
            overlay = active ? icons.topActive : icons.top;
        } else if (side == ForgeDirection.DOWN) {
            overlay = icons.bottom;
        } else {
            overlay = icons.side;
        }
        return new ITexture[] { casing, TextureFactory.of(overlay) };
    }
}
