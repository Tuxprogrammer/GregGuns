package com.gregguns.icbm.defense;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.icbm.component.AirDefenseView;
import com.gregguns.icbm.component.Reply;
import com.gregguns.icbm.detect.Contact;
import com.gregguns.icbm.detect.Contacts;
import com.gregguns.icbm.missile.EntityMissile;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * The air-defense battery (D15): an HV single block that holds {@link SamRules#CAPACITY} SAMs and listens to one radar
 * through a data stick link. Every few ticks it reads the radar's incoming list; for the nearest threat within
 * {@link SamRules#ENGAGE_RANGE} that the SAMs already in the air will not kill, it launches one SAM, at most one per
 * {@link SamRules#FIRE_INTERVAL} ticks, paying {@link SamRules#EU_PER_SHOT} EU when the SAM leaves. It is independent
 * of the silo and the Launch Control. It reports a redstone signal and its active flag while it engages. Construct only
 * inside GT's MTE window. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md
public final class MTEAirDefense extends DefenseBlock implements AirDefenseView {

    /** First SAM slot; the SAMs take slots 0 to {@link SamRules#CAPACITY} - 1. */
    public static final int SLOT_SAM = 0;
    /** The data stick slot, after the SAMs. */
    public static final int SLOT_STICK = SamRules.CAPACITY;
    private static final String K_COOL = "cool";
    private static final String K_AUTO = "auto";
    private static final String K_FIRED = "fired";
    private static final String K_HITS = "hits";
    private static final int HV = 3;

    private int cooldown;
    private final java.util.List<EntitySam> flying = new java.util.ArrayList<EntitySam>();
    private SamRules.State state = SamRules.State.NO_LINK;
    private int threats;
    private int signal = -1;
    private boolean lastActive;
    private boolean auto = true;
    private int fired;
    private int hits;

    /**
     * The prototype that registers the machine under {@code id}.
     *
     * @param id              the MTE id
     * @param name            GT's internal name
     * @param nameRegional    the English display name
     * @param descriptionKeys lang keys of the tooltip lines
     * @param iconDir         the overlay folder under {@code textures/blocks/}
     */
    public MTEAirDefense(int id, String name, String nameRegional, String[] descriptionKeys, String iconDir) {
        super(id, name, nameRegional, HV, SamRules.CAPACITY + 1, descriptionKeys, iconDir);
    }

    private MTEAirDefense(String name, String[] descriptionKeys, Icons icons) {
        super(name, HV, SamRules.CAPACITY + 1, descriptionKeys, icons);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTEAirDefense(mName, mDescriptionArray, icons());
    }

    // --- inventory ---------------------------------------------------------------------------------------------

    static boolean isSam(ItemStack s) {
        return s != null && s.getItem() instanceof ItemSam;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == SLOT_STICK ? isStick(stack) : index >= SLOT_SAM && index < SLOT_STICK && isSam(stack);
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return isItemValidForSlot(index, stack);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public int samCount() {
        int n = 0;
        for (int i = SLOT_SAM; i < SLOT_STICK; i++) {
            ItemStack s = getStackInSlot(i);
            if (isSam(s)) {
                n += s.stackSize;
            }
        }
        return n;
    }

    private boolean takeSam() {
        for (int i = SLOT_SAM; i < SLOT_STICK; i++) {
            ItemStack s = getStackInSlot(i);
            if (isSam(s)) {
                s.stackSize--;
                if (s.stackSize <= 0) {
                    setInventorySlotContents(i, null);
                }
                return true;
            }
        }
        return false;
    }

    // --- state -------------------------------------------------------------------------------------------------

    /** @return what the battery reports now */
    public SamRules.State state() {
        return state;
    }

    /** @return how many threats the linked radar reports within the engage range */
    public int threats() {
        return threats;
    }

    /** @return the redstone strength last published, 0 or 15 */
    public int signal() {
        return Math.max(signal, 0);
    }

    /** @return the link on the stick in the slot, or null */
    public RadarLink link() {
        ItemStack stick = getStackInSlot(SLOT_STICK);
        return isStick(stick) ? RadarLink.read(stick.getTagCompound()) : null;
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (base.isClientSide() || !ModuleState.isActive(ModuleId.ICBM)) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        driveSams();
        if (tick % SamRules.THINK_TICKS == 0) {
            think();
        }
    }

    /**
     * Flies the SAMs this battery launched. The world does not tick an entity unless the chunks around it are loaded,
     * and a battery tends to stand where none are (no player near), so it drives its own SAMs
     * ({@link EntitySam#advance} runs once per tick whoever calls first). A SAM that survives a reload has no
     * driver here and fizzles on its own.
     */
    private void driveSams() {
        for (java.util.Iterator<EntitySam> it = flying.iterator(); it.hasNext();) {
            EntitySam sam = it.next();
            if (sam.isDead) {
                it.remove();
            } else {
                sam.advance();
            }
        }
    }

    /** Looks at the radar and fires if the rules say so. Server side. */
    void think() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        World w = base.getWorld();
        threats = 0;
        RadarLink link = link();
        RadarSource radar = null;
        if (link == null) {
            state = SamRules.State.NO_LINK;
        } else {
            radar = RadarLinks.find(w, link);
            state = radar == null || !radar.online() ? SamRules.State.LINK_LOST : SamRules.State.STANDBY;
        }
        if (state == SamRules.State.STANDBY) {
            if (samCount() == 0) {
                state = SamRules.State.NO_SAMS;
            } else if (base.getStoredEU() < SamRules.EU_PER_SHOT) {
                state = SamRules.State.NO_POWER;
            }
        }
        if (radar != null && radar.online()) {
            engage(base, w, radar, auto);
        }
        publish(base);
    }

    private void engage(IGregTechTileEntity base, World w, RadarSource radar, boolean mayFire) {
        double bx = base.getXCoord() + 0.5;
        double by = base.getYCoord() + 0.5;
        double bz = base.getZCoord() + 0.5;
        boolean fired = false; // this pass
        for (Contact c : radar.sweep()
            .incoming()) {
            double dx = c.x() - bx;
            double dy = c.y() - by;
            double dz = c.z() - bz;
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) > SamRules.ENGAGE_RANGE) {
                continue;
            }
            EntityMissile m = Contacts.missile(w, c.id());
            if (m == null) {
                continue;
            }
            threats++;
            if (!mayFire || fired
                || cooldown > 0
                || state == SamRules.State.NO_SAMS
                || state == SamRules.State.NO_POWER) {
                continue;
            }
            if (SamRules.stillNeeded(m.health(), samsAt(w, c.id())) > 0 && launch(base, w, c.id())) {
                fired = true;
            }
        }
        if (threats > 0 && state == SamRules.State.STANDBY) {
            state = SamRules.State.ENGAGING;
        } else if (threats > 0 && fired) {
            state = SamRules.State.ENGAGING;
        }
        if (samCount() == 0 && state == SamRules.State.ENGAGING) {
            state = SamRules.State.NO_SAMS;
        }
    }

    /** Takes a SAM and the shot's EU and sends it at the missile; false when either is missing. */
    private boolean launch(IGregTechTileEntity base, World w, int missileId) {
        if (!takeSam()) {
            return false;
        }
        if (!base.decreaseStoredEnergyUnits(SamRules.EU_PER_SHOT, false)) {
            // Put the SAM back: the shot was refused for want of EU.
            restoreSam();
            return false;
        }
        double bx = base.getXCoord() + 0.5;
        double bz = base.getZCoord() + 0.5;
        EntitySam sam = EntitySam.launch(w, bx, base.getYCoord() + 1.2, bz, missileId);
        sam.setOwner(this);
        w.spawnEntityInWorld(sam);
        flying.add(sam);
        w.playSoundEffect(bx, base.getYCoord() + 0.5, bz, "fireworks.launch", 3.0F, 1.2F);
        cooldown = SamRules.FIRE_INTERVAL;
        this.fired++;
        return true;
    }

    private void restoreSam() {
        for (int i = SLOT_SAM; i < SLOT_STICK; i++) {
            ItemStack s = getStackInSlot(i);
            if (s == null) {
                setInventorySlotContents(i, new ItemStack(DefenseMachines.samItem()));
                return;
            }
            if (isSam(s) && s.stackSize < s.getMaxStackSize()) {
                s.stackSize++;
                return;
            }
        }
    }

    // --- AirDefenseView (OpenComputers) -------------------------------------------------------------------------

    @Override
    public boolean linked() {
        RadarLink link = link();
        if (link == null) {
            return false;
        }
        RadarSource radar = RadarLinks.find(getBaseMetaTileEntity().getWorld(), link);
        return radar != null && radar.online();
    }

    @Override
    public int capacity() {
        return SamRules.CAPACITY * ItemSam.MAX_STACK;
    }

    @Override
    public boolean auto() {
        return auto;
    }

    @Override
    public void setAuto(boolean on) {
        auto = on;
        getBaseMetaTileEntity().markDirty();
    }

    @Override
    public Reply fireAt(int contactId) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.isClientSide()) {
            return Reply.refused("module_off");
        }
        World w = base.getWorld();
        RadarLink link = link();
        RadarSource radar = link == null ? null : RadarLinks.find(w, link);
        Contact threat = null;
        if (radar != null && radar.online()) {
            for (Contact c : radar.sweep()
                .incoming()) {
                if (c.id() == contactId) {
                    threat = c;
                    break;
                }
            }
        }
        EntityMissile m = threat == null ? null : Contacts.missile(w, contactId);
        boolean inRange = false;
        if (threat != null) {
            double dx = threat.x() - (base.getXCoord() + 0.5);
            double dy = threat.y() - (base.getYCoord() + 0.5);
            double dz = threat.z() - (base.getZCoord() + 0.5);
            inRange = Math.sqrt(dx * dx + dy * dy + dz * dz) <= SamRules.ENGAGE_RANGE;
        }
        String why = SamRules.fireRefusal(
            ModuleState.isActive(ModuleId.ICBM),
            link != null,
            radar != null && radar.online(),
            samCount(),
            base.getStoredEU() >= SamRules.EU_PER_SHOT,
            cooldown,
            m != null,
            inRange);
        if (why != null) {
            return Reply.refused(why);
        }
        return launch(base, w, contactId) ? Reply.ok() : Reply.refused("no_power");
    }

    @Override
    public int fired() {
        return fired;
    }

    @Override
    public int hits() {
        return hits;
    }

    /** A SAM of this battery destroyed its missile. */
    void recordHit() {
        hits++;
    }

    /** @return how many SAMs are in the air at that missile */
    static int samsAt(World w, int missileId) {
        int n = 0;
        for (Object o : w.loadedEntityList) {
            if (o instanceof EntitySam && !((EntitySam) o).isDead && ((EntitySam) o).targetId() == missileId) {
                n++;
            }
        }
        return n;
    }

    private void publish(IGregTechTileEntity base) {
        boolean engaged = threats > 0;
        int want = engaged ? 15 : 0;
        if (want != signal) {
            signal = want;
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                base.setInternalOutputRedstoneSignal(d, (byte) want);
            }
        }
        if (engaged != lastActive) {
            lastActive = engaged;
            base.setActive(engaged);
        }
    }

    // --- GUI and NBT -------------------------------------------------------------------------------------------

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player, ForgeDirection side, float x, float y,
        float z) {
        if (!base.isClientSide()) {
            openGui(player);
        }
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return new BatteryGui(this).build(guiData, syncManager, uiSettings);
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        nbt.setInteger(K_COOL, cooldown);
        nbt.setBoolean(K_AUTO, auto);
        nbt.setInteger(K_FIRED, fired);
        nbt.setInteger(K_HITS, hits);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        cooldown = Math.max(0, nbt.getInteger(K_COOL));
        auto = !nbt.hasKey(K_AUTO) || nbt.getBoolean(K_AUTO);
        fired = Math.max(0, nbt.getInteger(K_FIRED));
        hits = Math.max(0, nbt.getInteger(K_HITS));
    }
}
