package com.gregguns.icbm.emp;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gregguns.GregGuns;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.text.AddedBy;
import com.gregguns.icbm.control.FireTrigger;
import com.gregguns.icbm.control.LaunchControlTooltipFormat;

import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;

/**
 * The EMP Tower (D16): an EV GT single block that, when it fires, drains the stored EU of the nearest devices around
 * it, gives each multiblock among them one maintenance issue, and rolls every missile in flight in range against the
 * kill-chance table ({@link EmpPulse}). It stores EU for one pulse at the largest radius; a pulse costs
 * {@link EmpRules#euCost} of the set radius, taken only after the pulse ran. Three things can fire it, all through the
 * same {@link #fire}: the Fire button, a rising redstone edge, and a rising edge of GT's work-allowed flag (a machine
 * controller cover). A computer reaches it through the OpenComputers component, which calls the same methods.
 *
 * <p>
 * Server-side rules: a button press is re-checked against the pressing player (alive, near), and the radius and mode
 * are clamped (ICBM-05-11). Construct only inside GT's MTE window ({@link EmpMachines#register()}). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md
public final class MTEEmpTower extends MTETieredMachineBlock
    implements ISecondaryDescribable, com.gregguns.icbm.component.EmpTowerView {

    /** How far a pressing player may stand from the block, blocks. */
    static final double USE_DISTANCE = 8.0;
    /** Radius a new tower starts with, blocks (ICBM's 60, kept inside the default maximum). */
    public static final int DEFAULT_RADIUS = 60;
    private static final int EV = 4;
    private static final String OVERLAY_DIR = "machines/thirdparty/ntm/emp_tower/";
    private static final String K_RADIUS = "radius";
    private static final String K_MODE = "mode";
    private static final String K_COOL = "cool";

    private int radius = DEFAULT_RADIUS;
    private EmpMode mode = EmpMode.ALL;
    private final FireTrigger cooldown = new FireTrigger();
    private final FireTrigger redstoneEdge = new FireTrigger();
    private final FireTrigger coverEdge = new FireTrigger();
    private EmpRules.Gate lastGate;
    private EmpPulse.Result lastResult;
    private int pulses;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id
     * @param name            GT's internal name
     * @param nameRegional    the English display name
     * @param descriptionKeys lang keys of the tooltip lines
     */
    public MTEEmpTower(int id, String name, String nameRegional, String[] descriptionKeys) {
        super(id, name, nameRegional, EV, 0, descriptionKeys);
        Overlays.load();
    }

    private MTEEmpTower(String name, String[] descriptionKeys) {
        super(name, EV, 0, descriptionKeys, null);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTEEmpTower(mName, mDescriptionArray);
    }

    @Override
    public String[] getDescription() {
        return getCurrentDescription();
    }

    @Override
    public String[] getPrimaryDescription() {
        return LaunchControlTooltipFormat
            .primary(local(0), StatCollector.translateToLocal("gregguns.icbm.emp.tooltip.shift"), AddedBy.line())
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

    /** Font-backed wrapper, in its own class so the dedicated server never loads the client font types. */
    private static final class ClientWrap implements LaunchControlTooltipFormat.Wrapper {

        static final ClientWrap INSTANCE = new ClientWrap();
        private static final int WIDTH = 200;

        @Override
        @SuppressWarnings("unchecked")
        public List<String> wrap(String line) {
            return net.minecraft.client.Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(line, WIDTH);
        }
    }

    /** Only the client ever asks; the dedicated server has no keyboard. */
    @Override
    public boolean isDisplaySecondaryDescription() {
        return FMLCommonHandler.instance()
            .getSide()
            .isClient() && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
    }

    private String local(int i) {
        String text = StatCollector.translateToLocal(mDescriptionArray[i]);
        // The pulse line carries the configured device cap (%s), as the lang file cannot know it.
        return text.contains("%s") ? String.format(text, IcbmConfig.empMaxDevices) : text;
    }

    // --- energy ---------------------------------------------------------------------------------------------------

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
        return 2;
    }

    /** Room for one pulse at the largest radius the config allows, and a little more. */
    @Override
    public long maxEUStore() {
        return EmpRules.euCost(IcbmConfig.empMaxRadius);
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return facing != ForgeDirection.UP && facing != ForgeDirection.DOWN && facing != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    // --- state ----------------------------------------------------------------------------------------------------

    /** @return the pulse radius, blocks, from {@link EmpRules#MIN_RADIUS} to the configured maximum */
    @Override
    public int radius() {
        return EmpRules.clampRadius(radius, IcbmConfig.empMaxRadius);
    }

    /**
     * Sets the radius, clamped both ways (ICBM-05-11).
     *
     * @param value the requested radius, blocks
     */
    @Override
    public void setRadius(int value) {
        radius = EmpRules.clampRadius(value, IcbmConfig.empMaxRadius);
        changed();
    }

    /** @return what a pulse reaches */
    @Override
    public EmpMode mode() {
        return mode;
    }

    /** @param next what a pulse reaches; null is ignored */
    @Override
    public void setMode(EmpMode next) {
        if (next != null) {
            mode = next;
            changed();
        }
    }

    @Override
    public long stored() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base == null ? 0L : base.getStoredEU();
    }

    /** @return the EU one pulse costs at the current radius */
    @Override
    public long cost() {
        return EmpRules.euCost(radius());
    }

    /** @return ticks until the tower may fire again; 0 when ready by the clock */
    @Override
    public int cooldownLeft() {
        return cooldown.cooldownLeft();
    }

    /** @return the first failing check right now, or READY; the same answer for every way of firing */
    @Override
    public EmpRules.Gate gate() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        long stored = base == null ? 0L : base.getStoredEU();
        return EmpRules.gate(ModuleState.isActive(ModuleId.ICBM), cooldown.coolingDown(), stored, cost());
    }

    /** @return the gate of the last firing attempt, or null before any (the GUI line) */
    public EmpRules.Gate lastGate() {
        return lastGate;
    }

    /** @return what the last pulse did, or null before any */
    public EmpPulse.Result lastResult() {
        return lastResult;
    }

    /** @return how many pulses this tower has fired since it loaded (tests and the GUI) */
    @Override
    public int pulses() {
        return pulses;
    }

    private void changed() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) {
            base.markDirty();
        }
    }

    // --- the pulse ------------------------------------------------------------------------------------------------

    /**
     * Fires a pulse when every check passes. The EU is taken after the pulse ran, so a pulse that could not run costs
     * nothing. Server side; every way of firing comes here (GGI-01-04).
     *
     * @return the gate that decided: {@link EmpRules.Gate#READY} means the pulse went
     */
    @Override
    public EmpRules.Gate fire() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.isClientSide()) {
            return EmpRules.Gate.MODULE_OFF;
        }
        EmpRules.Gate gate = gate();
        lastGate = gate;
        if (!gate.isGood()) {
            return gate;
        }
        long cost = cost();
        try {
            lastResult = EmpPulse.fire(
                base.getWorld(),
                base.getXCoord() + 0.5,
                base.getYCoord() + 0.5,
                base.getZCoord() + 0.5,
                radius(),
                mode,
                (net.minecraft.tileentity.TileEntity) base);
        } finally {
            // A pulse that failed midway still went out: the cost and the cooldown stand, or the next edge is free.
            base.decreaseStoredEnergyUnits(cost, true);
            cooldown.startCooldown(EmpRules.COOLDOWN_TICKS);
            pulses++;
            changed();
        }
        return gate;
    }

    /**
     * @param player a player
     * @return true when the player may press this block's buttons: alive and within {@link #USE_DISTANCE}
     */
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
     * The Fire button.
     *
     * @param player the pressing player
     */
    void fireFromButton(EntityPlayer player) {
        if (mayUse(player)) {
            fire();
        }
    }

    /**
     * The radius field.
     *
     * @param player the editing player
     * @param value  the requested radius
     */
    void setRadiusFromGui(EntityPlayer player, int value) {
        if (mayUse(player)) {
            setRadius(value);
        }
    }

    /**
     * The mode button.
     *
     * @param player the pressing player
     */
    void cycleModeFromButton(EntityPlayer player) {
        if (mayUse(player)) {
            setMode(mode.next());
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM)) {
            return;
        }
        cooldown.tick();
        boolean rs = redstoneEdge.rising(base.getStrongestRedstone() > 0);
        boolean cover = coverEdge.rising(base.isAllowedToWork());
        if (rs || cover) {
            fire();
        }
        if (tick % 20 == 0) {
            base.setActive(base.getStoredEU() >= cost() && !cooldown.coolingDown());
        }
    }

    @Override
    public void onFirstTick(IGregTechTileEntity base) {
        super.onFirstTick(base);
        redstoneEdge.reset();
        coverEdge.reset();
    }

    // --- GUI ------------------------------------------------------------------------------------------------------

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player, ForgeDirection side, float x, float y,
        float z) {
        if (base.isClientSide()) {
            return true;
        }
        openGui(player);
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return new EmpTowerGui(this).build(guiData, syncManager, uiSettings);
    }

    // --- NBT ------------------------------------------------------------------------------------------------------

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        nbt.setInteger(K_RADIUS, radius);
        nbt.setString(K_MODE, mode.id());
        nbt.setInteger(K_COOL, cooldown.cooldownLeft());
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        radius = nbt.hasKey(K_RADIUS) ? EmpRules.clampRadius(nbt.getInteger(K_RADIUS), IcbmConfig.empMaxRadius)
            : DEFAULT_RADIUS;
        EmpMode saved = EmpMode.byId(nbt.getString(K_MODE));
        mode = saved == null ? EmpMode.ALL : saved;
        cooldown.restoreCooldown(nbt.getInteger(K_COOL));
    }

    // --- textures -------------------------------------------------------------------------------------------------

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

    /** The overlay icons, requested when the first block is constructed (before GT stitches textures). */
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
}
