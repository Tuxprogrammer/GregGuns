package com.gregguns.machine.repair;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gregguns.GregGuns;
import com.gregguns.api.CamoChangeable;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;

/**
 * The Camo Bench: a GT single block that cycles the camo of the item in its slot or of any armor piece the player
 * wears. Cosmetic only, so it draws no power (LV only because that is the lowest tier that has hulls). The click
 * arrives as {@link PacketCamoCycle}, which the server validates before {@link #cycle} runs.
 *
 * <p>
 * Items opt in through {@link CamoChangeable}; armor does today. Automation sees no slots (insert and extract are both
 * refused). Construct only inside GT's MTE window ({@link RepairMachines#register()}). Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/camo-bench.md,
// docs/gregguns-spec/08-machines-production/machine-roster.md (Camo Bench: LV, 20115, no multiblock)
// Deviation: no EU draw. The sheet gives an LV machine but no process, and a click needs no time or power; a power
// requirement would only block a cosmetic. The recipe still uses Hull_LV.
public final class MTECamoBench extends MTETieredMachineBlock {

    /** The single item slot. */
    public static final int SLOT = 0;

    private static final String OVERLAY_DIR = "machines/camo_bench/";

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id, from {@link com.gregguns.machine.GregGunsMTEIDs}
     * @param name            GT's internal name, lower case with dots
     * @param nameRegional    the English display name GT registers for the block
     * @param descriptionKeys lang keys of the tooltip lines, translated when the tooltip is shown
     */
    public MTECamoBench(int id, String name, String nameRegional, String[] descriptionKeys) {
        super(id, name, nameRegional, 1, 1, descriptionKeys);
        Overlays.load();
    }

    private MTECamoBench(String name, String[] descriptionKeys) {
        super(name, 1, 1, descriptionKeys, null);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTECamoBench(mName, mDescriptionArray);
    }

    @Override
    public String[] getDescription() {
        String[] lines = new String[mDescriptionArray.length];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = StatCollector.translateToLocal(mDescriptionArray[i]);
        }
        return com.gregguns.core.text.AddedBy.append(lines);
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return facing != ForgeDirection.UP && facing != ForgeDirection.DOWN && facing != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity baseMetaTileEntity, int index, ForgeDirection side,
        ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity baseMetaTileEntity, int index, ForgeDirection side,
        ItemStack stack) {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == SLOT && stack != null && stack.getItem() instanceof CamoChangeable;
    }

    /**
     * Steps the camo of one target. Runs on the server, after {@link PacketCamoCycle} validated the click. Items that
     * are not {@link CamoChangeable}, or have one camo, are left alone.
     *
     * @param player the player
     * @param target what to cycle
     * @param back   true to step backward
     * @return true when a camo changed
     */
    public boolean cycle(EntityPlayer player, CamoCycle.Target target, boolean back) {
        ItemStack stack = target == CamoCycle.Target.BENCH_SLOT ? getStackInSlot(SLOT)
            : player.inventory.armorInventory[3 - target.armorType()];
        return cycleStack(stack, back);
    }

    /**
     * Steps the camo of a stack in place; every other tag is left as it was.
     *
     * @param stack a stack, may be null
     * @param back  true to step backward
     * @return true when the camo index changed
     */
    public static boolean cycleStack(@Nullable ItemStack stack, boolean back) {
        if (stack == null || !(stack.getItem() instanceof CamoChangeable)) {
            return false;
        }
        CamoChangeable item = (CamoChangeable) stack.getItem();
        int count = item.camoCount(stack);
        if (count <= 1) {
            return false;
        }
        int current = item.getCamo(stack);
        int next = CamoCycle.next(current, count, back);
        item.setCamo(stack, next);
        return next != current;
    }

    // --- GUI ------------------------------------------------------------------------------------------------------

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return new CamoBenchGui(this).build(guiData, syncManager, uiSettings);
    }

    // --- textures -------------------------------------------------------------------------------------------------

    @Override
    public ITexture[][][] getTextureSet(ITexture[] textures) {
        return null;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        ITexture casing = Textures.BlockIcons.MACHINE_CASINGS[mTier][colorIndex + 1];
        IIconContainer overlay;
        if (side == facing) {
            overlay = active ? Overlays.FRONT_ACTIVE : Overlays.FRONT;
        } else if (side == ForgeDirection.UP) {
            overlay = active ? Overlays.TOP_ACTIVE : Overlays.TOP;
        } else if (side == ForgeDirection.DOWN) {
            overlay = Overlays.BOTTOM;
        } else {
            overlay = Overlays.SIDE;
        }
        return new ITexture[] { casing, TextureFactory.of(overlay) };
    }

    /** The overlay icons, requested when the first bench is constructed (before GT stitches textures). */
    private static final class Overlays {

        static final IIconContainer FRONT = icon("OVERLAY_FRONT");
        static final IIconContainer FRONT_ACTIVE = icon("OVERLAY_FRONT_ACTIVE");
        static final IIconContainer SIDE = icon("OVERLAY_SIDE");
        static final IIconContainer TOP = icon("OVERLAY_TOP");
        static final IIconContainer TOP_ACTIVE = icon("OVERLAY_TOP_ACTIVE");
        static final IIconContainer BOTTOM = icon("OVERLAY_BOTTOM");

        private Overlays() {}

        static void load() {
            // Loading the class runs the initializers above.
        }

        private static IIconContainer icon(String file) {
            return Textures.BlockIcons.customOptional(GregGuns.MODID, OVERLAY_DIR + file);
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        // The inventory is saved by GT; the bench keeps no other state.
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        // Nothing beyond the inventory.
    }
}
