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
import com.gregguns.icbm.IcbmSync;
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
 * The Launch Control: an HV GT single block that fires a missile (D13). Built into a Missile Silo's structure (any
 * hatch position) it is that silo's Launch Control and fires that silo only; standing free, it fires a Galacticraft
 * launch pad it touches (D288, {@link LaunchSites}). It stores a target (typed coordinates, the player's position, or a
 * GT data stick), checks the pad,
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
    private static final String K_HAS_SILO = "hasSilo";
    private static final String K_SILO_X = "siloX";
    private static final String K_SILO_Y = "siloY";
    private static final String K_SILO_Z = "siloZ";
    /** Ticks after the block's first tick at which it asks silos around it to check their structure once. */
    private static final int NUDGE_TICKS = 20;

    private boolean hasX;
    private boolean hasZ;
    private int tx;
    private int tz;
    private final FireTrigger shots = new FireTrigger();
    private final FireTrigger redstoneEdge = new FireTrigger();
    private final FireTrigger coverEdge = new FireTrigger();
    private static final LaunchSnapshot GONE = new LaunchSnapshot(ControlStatus.NO_PAD, false, 0, 0, 0, 0, 0);

    /** The silo whose structure holds this block (D288), set by the silo's structure check; saved. */
    private boolean hasSilo;
    private int siloX;
    private int siloY;
    private int siloZ;
    private int nudgeIn = -1;

    private LaunchSnapshot snapshot;
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
        return index == SLOT_STICK && isTargetCarrier(stack);
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return index == SLOT_STICK && isTargetCarrier(stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    /** @return true for a GT data stick (accepted quietly, for admins and automation) */
    static boolean isStick(ItemStack stack) {
        return stack != null && ItemList.Tool_DataStick.isStackEqual(stack, false, true);
    }

    /** @return true for a Radar Gun, the handheld that captures targets */
    static boolean isRadarGun(ItemStack stack) {
        return stack != null && stack.getItem() instanceof com.gregguns.icbm.defense.ItemRadarGun;
    }

    /**
     * What the target slot takes: a Radar Gun, or a data stick. Both keep the target in the same tag
     * ({@link TargetStick}); the item stays in the slot, so the player can take it back.
     *
     * @param stack any stack
     * @return true for a Radar Gun or a GT data stick
     */
    static boolean isTargetCarrier(ItemStack stack) {
        return isRadarGun(stack) || isStick(stack);
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
        if (!isTargetCarrier(stick)) {
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
        ControlStatus now = snapshot().status;
        if (now == ControlStatus.COUNTDOWN || now == ControlStatus.IGNITED) {
            return; // the target is frozen while a launch is under way
        }
        loadStick();
    }

    /** Copies the target on the stick in the slot to this block; the outcome is also kept for the GUI line. */
    private TargetStick.Outcome loadStick() {
        ItemStack stick = getStackInSlot(SLOT_STICK);
        if (!isTargetCarrier(stick)) {
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

    /**
     * @return the pre-check without the target right now, recomputed: what the screen's status line reports while no
     *         target is set ({@link LaunchGate#statusLineKey}); server side
     */
    public ControlStatus currentSiteStatus() {
        snapshot = null;
        return snapshot().siteStatus;
    }

    /** @return the silo or pad the block is bound to right now, recomputed (tests, the GUI line); server side */
    public SiteLine siteLine() {
        snapshot = null;
        return snapshot().site;
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
    LaunchSnapshot snapshot() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !IcbmSync.live(this)) {
            // The block vanished under an open screen (broken, exploded, chunk unloaded): show an empty pad.
            return GONE;
        }
        long now = base.getTimer();
        if (snapshot != null && now - snapshotTick < SNAPSHOT_TICKS && now >= snapshotTick) {
            return snapshot;
        }
        snapshot = compute(base);
        snapshotTick = now;
        return snapshot;
    }

    private LaunchSnapshot compute(IGregTechTileEntity base) {
        LaunchTarget t = target();
        com.gregguns.icbm.silo.MTEMissileSilo silo = boundSilo();
        boolean siloPowered = silo != null;
        boolean energy = shotEu(silo) >= EU_PER_SHOT;
        World w = base.getWorld();
        LaunchSites.Found found = ModuleState.isActive(ModuleId.ICBM)
            ? LaunchSites.find(this, w, base.getXCoord(), base.getYCoord(), base.getZCoord())
            : LaunchSites.Found.NOTHING;
        LaunchSite site = found.site;
        if (site == null) {
            ControlStatus st = LaunchGate
                .check(t, false, energy, siloPowered, shots.coolingDown(), false, false, false, 0, 0, 0);
            ControlStatus siteSt = found.siloUnformed ? ControlStatus.SILO_UNFORMED : ControlStatus.NO_PAD;
            if (st == ControlStatus.NO_PAD) {
                st = siteSt;
            }
            return new LaunchSnapshot(st, false, 0, 0, 0, 0, 0).at(found.line)
                .withSiteStatus(siteSt);
        }
        int cd = site.countdownTicks();
        MissileStatus ms = site.status();
        int min = ms.present() ? ms.tier()
            .minRange() : 0;
        int max = ms.present() ? site.maxRange() : 0;
        double dist = t.distanceFrom(site.centerX(), site.centerZ());
        ControlStatus st = LaunchGate.check(
            t,
            true,
            energy,
            siloPowered,
            shots.coolingDown(),
            ms.present(),
            ms.armed(),
            ms.fueled(),
            dist,
            min,
            max);
        ControlStatus siteSt = LaunchGate
            .checkSite(true, energy, siloPowered, shots.coolingDown(), ms.present(), ms.armed(), ms.fueled());
        com.gregguns.icbm.missile.LaunchResult hold = site.hold();
        if (hold != com.gregguns.icbm.missile.LaunchResult.OK) {
            ControlStatus held = LaunchGate.fromResult(hold.name());
            if (st == ControlStatus.READY) {
                st = held;
            }
            if (siteSt == ControlStatus.READY) {
                siteSt = held;
            }
        }
        int radius = t.isSet() && ms.present() ? (int) Math.round(site.expectedRadius(t.aimX(), t.aimZ()) * 10.0) : 0;
        if (cd >= 0 || site.ignited()) {
            // A launch is in progress (D88): the gate has nothing to say, but the screen keeps the locked target's
            // real distance, range and blast radius (D279).
            return LaunchSnapshot.inProgress(
                cd,
                min,
                max,
                dist,
                radius,
                ms.present() ? ms.tier()
                    .number() : 0)
                .at(found.line);
        }
        return new LaunchSnapshot(
            st,
            true,
            min,
            max,
            (int) Math.round(dist),
            radius,
            ms.present() ? ms.tier()
                .number() : 0).at(found.line)
                    .withSiteStatus(siteSt);
    }

    /**
     * The EU a shot is paid from (D300): a silo's own Launch Control uses the silo's Energy Hatches and ignores its own
     * buffer while it is bound; a free one uses its own buffer. Server side.
     *
     * @return the EU available for a shot
     */
    public long shotEu() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.isClientSide()) {
            return 0;
        }
        return shotEu(boundSilo());
    }

    private long shotEu(com.gregguns.icbm.silo.MTEMissileSilo silo) {
        return silo != null ? silo.launchControlEu() : getBaseMetaTileEntity().getStoredEU();
    }

    /** @return whether a shot is paid from a bound silo's Energy Hatches (D300); server side */
    public boolean siloPowered() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && !base.isClientSide() && boundSilo() != null;
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
        LaunchSnapshot s = snapshot();
        if (s.status != ControlStatus.READY) {
            lastShot = s.status;
            return s.status;
        }
        LaunchTarget t = target();
        LaunchSite site = LaunchSites
            .find(this, base.getWorld(), base.getXCoord(), base.getYCoord(), base.getZCoord()).site;
        if (site == null) {
            lastShot = ControlStatus.NO_PAD;
            return lastShot;
        }
        LaunchResult r = site.launch(t.aimX(), t.aimZ(), source);
        ControlStatus st = LaunchGate.fromResult(r.name());
        if (st == ControlStatus.FIRED) {
            // A silo's own Launch Control pays from the silo's Energy Hatches (D300). The launch is already accepted:
            // if the hatches were drained in this same tick since the check, the shot stands and nothing goes below
            // zero.
            com.gregguns.icbm.silo.MTEMissileSilo silo = boundSilo();
            if (silo != null) {
                silo.payLaunchControl(EU_PER_SHOT);
            } else {
                base.decreaseStoredEnergyUnits(EU_PER_SHOT, true);
            }
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
        if (isServerLive()) {
            // A silo's own Launch Control runs on the silo's EU (D300).
            m.put("power", siloPowered() ? "silo" : "own");
            m.put("eu", shotEu());
            SiteLine line = snapshot().site;
            m.put(
                "site",
                line.kind()
                    .name()
                    .toLowerCase(java.util.Locale.ROOT));
            m.put("siteName", line.name());
        }
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
        return LaunchSites.find(this, base.getWorld(), base.getXCoord(), base.getYCoord(), base.getZCoord()).site;
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
        LaunchSite site = LaunchSites
            .find(this, base.getWorld(), base.getXCoord(), base.getYCoord(), base.getZCoord()).site;
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
        if (nudgeIn > 0 && --nudgeIn == 0) {
            nudgeSilos(base);
        }
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
        nudgeIn = NUDGE_TICKS;
    }

    // --- the silo (D288) ------------------------------------------------------------------------------------------

    /**
     * Called by a Missile Silo's structure check that found this block in one of its hatch positions: this block is
     * now that silo's Launch Control. Server side.
     *
     * @param x X of the silo controller
     * @param y Y
     * @param z Z
     */
    public void bindSilo(int x, int y, int z) {
        if (!hasSilo || siloX != x || siloY != y || siloZ != z) {
            hasSilo = true;
            siloX = x;
            siloY = y;
            siloZ = z;
            changed();
        }
    }

    private void unbindSilo() {
        hasSilo = false;
        changed();
    }

    /** @return whether a silo's structure check bound this block and it has not been released since */
    boolean hasSilo() {
        return hasSilo;
    }

    /** @return {x, y, z} of the bound silo's controller; meaningful only while {@link #hasSilo()} */
    int[] siloPos() {
        return new int[] { siloX, siloY, siloZ };
    }

    /**
     * The silo this block belongs to. The binding holds while the controller stands there and is either unformed (it
     * may re-form with this block) or formed with a Launch Control at this block's position; a silo that formed
     * without it, or a controller that is gone, releases it and the block stands free. Both sides keep positions, not
     * references, so a reload of one chunk does not break the pairing. Server side.
     *
     * @return the silo, or null when the block is not part of one or the silo's chunk is not loaded
     */
    public com.gregguns.icbm.silo.MTEMissileSilo boundSilo() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (!hasSilo || base == null || base.isClientSide()) {
            return null;
        }
        World w = base.getWorld();
        if (!w.blockExists(siloX, siloY, siloZ)) {
            return null; // keep the binding until the silo's chunk is back
        }
        com.gregguns.icbm.silo.MTEMissileSilo silo = LaunchSites.siloAt(w, siloX, siloY, siloZ);
        if (silo == null
            || silo.isFormed() && !silo.hasLaunchControlAt(base.getXCoord(), base.getYCoord(), base.getZCoord())) {
            unbindSilo();
            return null;
        }
        return silo;
    }

    /**
     * A block placed into a silo that is already formed: GT rechecks a structure when a machine block next to its
     * parts changes, which a Launch Control in a plain concrete wall may not reach. So once, shortly after it loads or
     * is placed, a free block asks every formed silo whose structure could hold it, and that has no Launch Control, to
     * check its structure again; the check binds it ({@link #bindSilo}).
     */
    private void nudgeSilos(IGregTechTileEntity base) {
        if (hasSilo) {
            return;
        }
        World w = base.getWorld();
        int x = base.getXCoord();
        int y = base.getYCoord();
        int z = base.getZCoord();
        int reach = com.gregguns.icbm.silo.SiloShape.DEPTH - 1;
        int above = com.gregguns.icbm.silo.SiloShape.HEIGHT - 1 - com.gregguns.icbm.silo.SiloShape.CONTROLLER_ROW;
        int below = com.gregguns.icbm.silo.SiloShape.CONTROLLER_ROW;
        for (int dy = -below; dy <= above; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (y + dy < 0 || y + dy > 255
                        || !w.blockExists(x + dx, y + dy, z + dz)
                        || w.getBlock(x + dx, y + dy, z + dz) != gregtech.api.GregTechAPI.sBlockMachines) {
                        continue;
                    }
                    com.gregguns.icbm.silo.MTEMissileSilo silo = LaunchSites.siloAt(w, x + dx, y + dy, z + dz);
                    if (silo != null && silo.isFormed() && !silo.hasLaunchControl()) {
                        silo.onMachineBlockUpdate();
                    }
                }
            }
        }
    }

    // --- GUI ------------------------------------------------------------------------------------------------------

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player, ForgeDirection side, float x, float y,
        float z) {
        if (base.isClientSide()) {
            return true;
        }
        com.gregguns.icbm.complex.gen.GtMachineHook.handOver(base, player); // a generated one becomes the player's
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
        nbt.setBoolean(K_HAS_SILO, hasSilo);
        nbt.setInteger(K_SILO_X, siloX);
        nbt.setInteger(K_SILO_Y, siloY);
        nbt.setInteger(K_SILO_Z, siloZ);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        hasX = nbt.getBoolean(K_HAS_X);
        hasZ = nbt.getBoolean(K_HAS_Z);
        tx = nbt.getInteger(K_X);
        tz = nbt.getInteger(K_Z);
        shots.restoreCooldown(nbt.getInteger(K_COOL));
        hasSilo = nbt.getBoolean(K_HAS_SILO);
        siloX = nbt.getInteger(K_SILO_X);
        siloY = nbt.getInteger(K_SILO_Y);
        siloZ = nbt.getInteger(K_SILO_Z);
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
