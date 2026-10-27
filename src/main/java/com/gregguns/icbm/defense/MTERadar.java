package com.gregguns.icbm.defense;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * A single-block radar (HV or EV, D14): scans the missile tracker for missiles in flight within its range, flags the
 * ones aimed inside its protected radius, and reports them by redstone, by the active flag a GT machine-state cover
 * reads, by its GUI and by the data stick link a battery listens to. The hull's voltage sets the range
 * ({@link RadarRules#range}); it draws a quarter amp while scanning. Construct only inside GT's MTE window. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md
public final class MTERadar extends DefenseBlock implements RadarSource {

    /** The data stick slot. */
    public static final int SLOT_STICK = 0;
    /** How far a pressing player may stand from the block, blocks. */
    static final double USE_DISTANCE = 8.0;

    private final RadarUnit unit = new RadarUnit();
    private boolean online;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id
     * @param name            GT's internal name
     * @param nameRegional    the English display name
     * @param tier            the hull's voltage index (HV 3, EV 4)
     * @param descriptionKeys lang keys of the tooltip lines
     * @param iconDir         the overlay folder under {@code textures/blocks/}
     */
    public MTERadar(int id, String name, String nameRegional, int tier, String[] descriptionKeys, String iconDir) {
        super(id, name, nameRegional, tier, 1, descriptionKeys, iconDir);
    }

    private MTERadar(String name, int tier, String[] descriptionKeys, Icons icons) {
        super(name, tier, 1, descriptionKeys, icons);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTERadar(mName, mTier, mDescriptionArray, icons());
    }

    // --- RadarSource -------------------------------------------------------------------------------------------

    @Override
    public boolean online() {
        return online;
    }

    @Override
    public Sweep sweep() {
        return online ? unit.sweep() : Sweep.EMPTY;
    }

    @Override
    public int range() {
        return online ? RadarRules.range(mTier) : 0;
    }

    @Override
    public int protectedRadius() {
        return unit.radius();
    }

    /** @return the shared radar state (the GUI and the scenarios read it) */
    RadarUnit unit() {
        return unit;
    }

    /** @return the detection range this hull gives, online or not */
    public int rangeIfOn() {
        return RadarRules.range(mTier);
    }

    /** @return what the radar reports now */
    public RadarRules.State state() {
        return unit.state();
    }

    /** @return the redstone strength last published */
    public int signal() {
        return unit.signal();
    }

    // --- tick --------------------------------------------------------------------------------------------------

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM)) {
            return;
        }
        if (tick % RadarRules.SCAN_TICKS != 0) {
            return;
        }
        long cost = RadarRules.euPerTick(GTValues.V[mTier]) * RadarRules.SCAN_TICKS;
        online = base.decreaseStoredEnergyUnits(cost, false);
        unit.scan(base.getWorld(), base.getXCoord() + 0.5, base.getZCoord() + 0.5, rangeIfOn(), online);
        unit.publish(base);
    }

    /** Runs one scan now, free of charge when the block holds the energy (scenarios and the GUI use this). */
    void scanNow() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        long cost = RadarRules.euPerTick(GTValues.V[mTier]) * RadarRules.SCAN_TICKS;
        online = base.decreaseStoredEnergyUnits(cost, false);
        unit.scan(base.getWorld(), base.getXCoord() + 0.5, base.getZCoord() + 0.5, rangeIfOn(), online);
        unit.publish(base);
    }

    // --- stick and radius --------------------------------------------------------------------------------------

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == SLOT_STICK && isStick(stack);
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return index == SLOT_STICK && isStick(stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    boolean mayUse(EntityPlayer player) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (player == null || base == null || base.isClientSide() || player.isDead) {
            return false;
        }
        double dx = player.posX - (base.getXCoord() + 0.5);
        double dy = player.posY - (base.getYCoord() + 0.5);
        double dz = player.posZ - (base.getZCoord() + 0.5);
        return player.worldObj == base.getWorld() && dx * dx + dy * dy + dz * dz <= USE_DISTANCE * USE_DISTANCE;
    }

    /**
     * Writes this radar's link to the stick in the slot.
     *
     * @param player the pressing player
     * @return true when the stick was written
     */
    public boolean saveLink(EntityPlayer player) {
        return mayUse(player) && RadarUnit.writeLink(getBaseMetaTileEntity(), getStackInSlot(SLOT_STICK), player);
    }

    /** @return the protected radius text for the GUI field */
    String radiusText() {
        return Integer.toString(unit.radius());
    }

    /** @param text what the player typed; anything that is not a number is ignored, the rest is clamped */
    void setRadiusText(String text) {
        try {
            unit.setRadius(Integer.parseInt(text.trim()), rangeIfOn());
            getBaseMetaTileEntity().markDirty();
        } catch (NumberFormatException e) {
            // Ignored: the field keeps its value.
        }
    }

    // --- GUI and NBT -------------------------------------------------------------------------------------------

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player, ForgeDirection side, float x, float y,
        float z) {
        if (base.isClientSide()) {
            return true;
        }
        ItemStack held = player.getHeldItem();
        if (isStick(held)) {
            RadarUnit.writeLink(base, held, player);
            return true;
        }
        openGui(player);
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return new RadarGui(this).build(guiData, syncManager, uiSettings);
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        unit.save(nbt);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        unit.load(nbt);
    }
}
