package com.gregguns.icbm.control;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gregguns.GregGuns;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.text.AddedBy;
import com.gregguns.icbm.missile.LaunchResult;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.MissileStatus;

import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;

/**
 * The Launch Control: an HV GT single block that stands within reach of a Galacticraft launch pad and fires the missile
 * on it (D13). It stores a target (typed coordinates, the player's position, or a GT data stick), checks the pad,
 * the arming, the fuel and the range, and asks the launch site ({@link LaunchSite}: a pad or a silo) to launch. Three
 * things can fire it:
 * the Launch button, a rising redstone edge, and a rising edge of GT's work-allowed flag (what a machine controller
 * cover drives). Every shot costs {@link #EU_PER_SHOT} EU, paid only when the pad accepted the launch.
 *
 * <p>
 * Server-side rules: a button press is re-checked against the pressing player (alive, near), the typed coordinates are
 * clamped by {@link LaunchTarget#of}, and nothing the client sends can set energy or fire past the pre-check
 * (ICBM-04-12, 04-13). Construct only inside GT's MTE window ({@link ControlMachines#register()}). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md,
// docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md
public final class MTELaunchControl extends MTETieredMachineBlock
    implements ISecondaryDescribable, com.gregguns.icbm.component.LaunchView {

    /** EU one shot costs (1,200 FE in ICBM's units; 4 FE to the EU). */
    public static final int EU_PER_SHOT = 300;
    /** Ticks the block refuses another shot after one fired. */
    public static final int COOLDOWN_TICKS = 40;
    /** The data stick slot. */
    public static final int SLOT_STICK = 0;
    /** How far a pressing player may stand from the block, blocks. */
    static final double USE_DISTANCE = 8.0;
    private static final int HV = 3;
    private static final int SNAPSHOT_TICKS = 5;
    private static final String OVERLAY_DIR = "machines/thirdparty/ntm/launch_control/";
    private static final String K_X = "tx";
    private static final String K_Z = "tz";
    private static final String K_HAS_X = "hasX";
    private static final String K_HAS_Z = "hasZ";
    private static final String K_COOL = "cool";

    /** What the block knows about the pad and the missile right now; recomputed every few ticks on the server. */
    static final class Snapshot {

        final ControlStatus status;
        final boolean padFound;
        final int minRange;
        final int maxRange;
        final int distance;
        /** Expected blast radius at the target, tenths of a block; 0 when unknown. */
        final int radiusTenths;
        final int tier;
        /** Ticks left on a running countdown (D88), -1 when none. */
        final int countdown;

        Snapshot(ControlStatus status, boolean padFound, int minRange, int maxRange, int distance, int radiusTenths,
            int tier) {
            this(status, padFound, minRange, maxRange, distance, radiusTenths, tier, -1);
        }

        Snapshot(ControlStatus status, boolean padFound, int minRange, int maxRange, int distance, int radiusTenths,
            int tier, int countdown) {
            this.countdown = countdown;
            this.status = status;
            this.padFound = padFound;
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.distance = distance;
            this.radiusTenths = radiusTenths;
            this.tier = tier;
        }
    }

    private boolean hasX;
    private boolean hasZ;
    private int tx;
    private int tz;
    private final FireTrigger shots = new FireTrigger();
    private final FireTrigger redstoneEdge = new FireTrigger();
    private final FireTrigger coverEdge = new FireTrigger();
    private Snapshot snapshot;
    private long snapshotTick;
    private ControlStatus lastShot;
    private TargetStick.Outcome lastStick;
    private int attempts;
    private int launches;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id
     * @param name            GT's internal name
     * @param nameRegional    the English display name
     * @param descriptionKeys lang keys of the tooltip lines
     */
    public MTELaunchControl(int id, String name, String nameRegional, String[] descriptionKeys) {
        super(id, name, nameRegional, HV, 1, descriptionKeys);
        Overlays.load();
    }

    private MTELaunchControl(String name, String[] descriptionKeys) {
        super(name, HV, 1, descriptionKeys, null);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTELaunchControl(mName, mDescriptionArray);
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
        // The Shift details are full sentences; wrap them at 200 px like the warhead tooltip, or they clip at the
        // screen's left edge in a small window. Only the client asks, so the font is safe to touch.
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

        /** Pixels a tooltip line may take before it wraps (same as the warhead tooltip). */
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
        return StatCollector.translateToLocal(mDescriptionArray[i]);
    }

    // --- energy and inventory -------------------------------------------------------------------------------------

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

    /** @return true for a GT data stick */
    static boolean isStick(ItemStack stack) {
        return stack != null && ItemList.Tool_DataStick.isStackEqual(stack, false, true);
    }

    // --- target ---------------------------------------------------------------------------------------------------

    /** @return the stored target, or {@link LaunchTarget#NONE} while either coordinate is missing */
    public LaunchTarget target() {
        return hasX && hasZ ? LaunchTarget.of(tx, tz) : LaunchTarget.NONE;
    }

    /** @return the X field text; empty while unset */
    public String xText() {
        return hasX ? Integer.toString(tx) : "";
    }

    /** @return the Z field text; empty while unset */
    public String zText() {
        return hasZ ? Integer.toString(tz) : "";
    }

    /** @param text the X field; empty clears it, anything that is not a legal coordinate is ignored */
    public void setXText(String text) {
        int v = LaunchTarget.parseCoordinate(text);
        if (v == LaunchTarget.INVALID) {
            if (text == null || text.trim()
                .isEmpty()) {
                hasX = false;
            }
            return;
        }
        tx = v;
        hasX = true;
        changed();
    }

    /** @param text the Z field; empty clears it, anything that is not a legal coordinate is ignored */
    public void setZText(String text) {
        int v = LaunchTarget.parseCoordinate(text);
        if (v == LaunchTarget.INVALID) {
            if (text == null || text.trim()
                .isEmpty()) {
                hasZ = false;
            }
            return;
        }
        tz = v;
        hasZ = true;
        changed();
    }

    private void setTarget(LaunchTarget t) {
        tx = t.x();
        tz = t.z();
        hasX = t.isSet();
        hasZ = t.isSet();
        changed();
    }

    private void changed() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) {
            base.markDirty();
        }
        snapshot = null;
    }

    /** @param player the pressing player; the target becomes the block under their feet */
    public void useMyPosition(EntityPlayer player) {
        if (!mayUse(player)) {
            return;
        }
        setTarget(LaunchTarget.of((long) Math.floor(player.posX), (long) Math.floor(player.posZ)));
    }

    /**
     * Copies the stored target to the stick in the slot.
     *
     * @param player the pressing player
     */
    public void saveToStick(EntityPlayer player) {
        if (!mayUse(player)) {
            return;
        }
        ItemStack stick = getStackInSlot(SLOT_STICK);
        if (!isStick(stick)) {
            lastStick = TargetStick.Outcome.NO_STICK;
            return;
        }
        LaunchTarget t = target();
        if (!t.isSet()) {
            lastStick = TargetStick.Outcome.NO_TARGET;
            return;
        }
        if (!stick.hasTagCompound()) {
            stick.setTagCompound(new NBTTagCompound());
        }
        TargetStick.write(stick.getTagCompound(), t, getBaseMetaTileEntity().getWorld().provider.dimensionId);
        lastStick = TargetStick.Outcome.SAVED;
        changed();
    }

    /**
     * Copies the target on the stick in the slot to this block.
     *
     * @param player the pressing player
     */
    public void loadFromStick(EntityPlayer player) {
        if (!mayUse(player)) {
            return;
        }
        loadStick();
    }

    /** Copies the target on the stick in the slot to this block; the outcome is also kept for the GUI line. */
    private TargetStick.Outcome loadStick() {
        ItemStack stick = getStackInSlot(SLOT_STICK);
        if (!isStick(stick)) {
            lastStick = TargetStick.Outcome.NO_STICK;
            return lastStick;
        }
        NBTTagCompound tag = stick.getTagCompound();
        LaunchTarget t = TargetStick.read(tag);
        if (!t.isSet()) {
            lastStick = TargetStick.Outcome.EMPTY_STICK;
            return lastStick;
        }
        int dim = getBaseMetaTileEntity().getWorld().provider.dimensionId;
        if (TargetStick.dimension(tag, dim) != dim) {
            lastStick = TargetStick.Outcome.WRONG_DIMENSION;
            return lastStick;
        }
        setTarget(t);
        lastStick = TargetStick.Outcome.LOADED;
        return lastStick;
    }

    /** @return the pre-check status right now, recomputed (tests and the GUI line); server side */
    public ControlStatus currentStatus() {
        snapshot = null;
        return snapshot().status;
    }

    /** @return how many times a shot was attempted since the block loaded (tests and the GUI) */
    public int attempts() {
        return attempts;
    }

    /** @return the last data stick outcome, for the GUI line; null before any */
    public TargetStick.Outcome lastStick() {
        return lastStick;
    }

    /** @return the status of the last shot attempt, for the GUI line; null before any */
    public ControlStatus lastShot() {
        return lastShot;
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

    // --- the shot -------------------------------------------------------------------------------------------------

    /** @return the current pre-check and pad facts, recomputed at most every few ticks; server side only */
    Snapshot snapshot() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        long now = base.getTimer();
        if (snapshot != null && now - snapshotTick < SNAPSHOT_TICKS && now >= snapshotTick) {
            return snapshot;
        }
        snapshot = compute(base);
        snapshotTick = now;
        return snapshot;
    }

    private Snapshot compute(IGregTechTileEntity base) {
        LaunchTarget t = target();
        boolean energy = base.getStoredEU() >= EU_PER_SHOT;
        World w = base.getWorld();
        LaunchSite site = ModuleState.isActive(ModuleId.ICBM)
            ? LaunchSites.find(w, base.getXCoord(), base.getYCoord(), base.getZCoord())
            : null;
        if (site == null) {
            return new Snapshot(
                LaunchGate.check(t, false, energy, shots.coolingDown(), false, false, false, 0, 0, 0),
                false,
                0,
                0,
                0,
                0,
                0);
        }
        int cd = site.countdownTicks();
        if (cd >= 0 || site.ignited()) {
            // A launch is in progress: the missile has left the pad's "armed" state, so the gate has nothing to say
            // (D88).
            return new Snapshot(cd >= 0 ? ControlStatus.COUNTDOWN : ControlStatus.IGNITED, true, 0, 0, 0, 0, 0, cd);
        }
        MissileStatus ms = site.status();
        int min = ms.present() ? ms.tier()
            .minRange() : 0;
        int max = ms.present() ? site.maxRange() : 0;
        double dist = t.distanceFrom(site.centerX(), site.centerZ());
        ControlStatus st = LaunchGate
            .check(t, true, energy, shots.coolingDown(), ms.present(), ms.armed(), ms.fueled(), dist, min, max);
        if (st == ControlStatus.READY && site.hold() != com.gregguns.icbm.missile.LaunchResult.OK) {
            st = LaunchGate.fromResult(
                site.hold()
                    .name());
        }
        int radius = t.isSet() && ms.present() ? (int) Math.round(site.expectedRadius(t.aimX(), t.aimZ()) * 10.0) : 0;
        return new Snapshot(
            st,
            true,
            min,
            max,
            (int) Math.round(dist),
            radius,
            ms.present() ? ms.tier()
                .number() : 0);
    }

    /**
     * Fires the missile on the pad at the stored target, when every check passes. The EU is taken only after the pad
     * accepted the launch (ICBM-04-07). Server side.
     *
     * @param source who fired it, null for redstone or a cover
     * @return what came of it
     */
    public ControlStatus fire(EntityPlayer source) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM)) {
            return ControlStatus.NO_PAD;
        }
        attempts++;
        snapshot = null;
        Snapshot s = snapshot();
        if (s.status != ControlStatus.READY) {
            lastShot = s.status;
            return s.status;
        }
        LaunchTarget t = target();
        LaunchSite site = LaunchSites.find(base.getWorld(), base.getXCoord(), base.getYCoord(), base.getZCoord());
        if (site == null) {
            lastShot = ControlStatus.NO_PAD;
            return lastShot;
        }
        LaunchResult r = site.launch(t.aimX(), t.aimZ(), source);
        ControlStatus st = LaunchGate.fromResult(r.name());
        if (st == ControlStatus.FIRED) {
            base.decreaseStoredEnergyUnits(EU_PER_SHOT, true);
            shots.startCooldown(COOLDOWN_TICKS);
            launches++;
        }
        lastShot = st;
        snapshot = null;
        return st;
    }

    // --- OpenComputers view (gg_launch_control) -------------------------------------------------------------------

    @Override
    public LaunchTarget launchTarget() {
        return target();
    }

    @Override
    public void aimAt(int x, int z) {
        if (!isServerLive()) {
            return;
        }
        setTarget(LaunchTarget.of(x, z));
    }

    @Override
    public TargetStick.Outcome loadTargetFromStick() {
        return isServerLive() ? loadStick() : TargetStick.Outcome.NO_STICK;
    }

    @Override
    public MissileStatus missileStatus() {
        LaunchSite site = siteOrNull();
        return site == null ? MissileStatus.NONE : site.status();
    }

    @Override
    public String fuelFluid() {
        MissileStatus ms = missileStatus();
        return ms.present() && ms.tier()
            .fuels().length > 0 ? ms.tier()
                .fuels()[0] : "";
    }

    @Override
    public ControlStatus launchStatus() {
        return isServerLive() ? currentStatus() : ControlStatus.NO_PAD;
    }

    @Override
    public ControlStatus launchNow() {
        return fire(null);
    }

    @Override
    public int launches() {
        return launches;
    }

    @Override
    public java.util.Map<String, Object> launchInfo() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("euPerShot", EU_PER_SHOT);
        m.put("cooldownTicks", COOLDOWN_TICKS);
        m.put("padFound", siteOrNull() != null);
        return m;
    }

    private boolean isServerLive() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && !base.isClientSide() && ModuleState.isActive(ModuleId.ICBM);
    }

    private LaunchSite siteOrNull() {
        if (!isServerLive()) {
            return null;
        }
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return LaunchSites.find(base.getWorld(), base.getXCoord(), base.getYCoord(), base.getZCoord());
    }

    /**
     * Fired by the Launch button; the button re-checks the player first.
     *
     * @param player the pressing player
     */
    public void fireFromButton(EntityPlayer player) {
        if (mayUse(player)) {
            fire(player);
        }
    }

    /**
     * Stops the countdown in progress (D88), when the pressing player may use the block. Refused after ignition.
     *
     * @param player the pressing player
     * @return true when a launch was aborted
     */
    public boolean abortFromButton(EntityPlayer player) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM) || !mayUse(player)) {
            return false;
        }
        LaunchSite site = LaunchSites.find(base.getWorld(), base.getXCoord(), base.getYCoord(), base.getZCoord());
        snapshot = null;
        return site != null && site.abort();
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM)) {
            return;
        }
        shots.tick();
        // Both edges are sampled every tick; neither fires on its first sample, so a load with the signal high is
        // quiet.
        boolean rs = redstoneEdge.rising(base.getStrongestRedstone() > 0);
        boolean cover = coverEdge.rising(base.isAllowedToWork());
        if (rs || cover) {
            fire(null);
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
        if (loadFromRadarGun(player)) {
            return true;
        }
        openGui(player);
        return true;
    }

    /**
     * A radar gun in hand with a target copies it here instead of opening the screen (D93).
     *
     * @param player the player
     * @return true when the gun held a target in this dimension and it was copied
     */
    private boolean loadFromRadarGun(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        LaunchTarget t = com.gregguns.icbm.defense.ItemRadarGun.target(held);
        if (!t.isSet() || !mayUse(player)) {
            return false;
        }
        int dim = getBaseMetaTileEntity().getWorld().provider.dimensionId;
        if (com.gregguns.icbm.defense.ItemRadarGun.dimension(held, dim) != dim) {
            player.addChatMessage(
                new net.minecraft.util.ChatComponentTranslation("gregguns.icbm.radargun.wrong_dimension"));
            return true;
        }
        setTarget(t);
        player.addChatMessage(
            new net.minecraft.util.ChatComponentTranslation("gregguns.icbm.radargun.loaded", t.x(), t.z()));
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return new LaunchControlGui(this).build(guiData, syncManager, uiSettings);
    }

    // --- NBT ------------------------------------------------------------------------------------------------------

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        nbt.setBoolean(K_HAS_X, hasX);
        nbt.setBoolean(K_HAS_Z, hasZ);
        nbt.setInteger(K_X, tx);
        nbt.setInteger(K_Z, tz);
        nbt.setInteger(K_COOL, shots.cooldownLeft());
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        hasX = nbt.getBoolean(K_HAS_X);
        hasZ = nbt.getBoolean(K_HAS_Z);
        tx = nbt.getInteger(K_X);
        tz = nbt.getInteger(K_Z);
        shots.restoreCooldown(nbt.getInteger(K_COOL));
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
