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
public final class MTEAirDefense extends DefenseBlock {

    /** First SAM slot; the SAMs take slots 0 to {@link SamRules#CAPACITY} - 1. */
    public static final int SLOT_SAM = 0;
    /** The data stick slot, after the SAMs. */
    public static final int SLOT_STICK = SamRules.CAPACITY;
    private static final String K_COOL = "cool";
    private static final int HV = 3;

    private int cooldown;
    private final java.util.List<EntitySam> flying = new java.util.ArrayList<EntitySam>();
    private SamRules.State state = SamRules.State.NO_LINK;
    private int threats;
    private int signal = -1;
    private boolean lastActive;

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

    /** @return how many SAMs the battery holds */
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
            engage(base, w, radar);
        }
        publish(base);
    }

    private void engage(IGregTechTileEntity base, World w, RadarSource radar) {
        double bx = base.getXCoord() + 0.5;
        double by = base.getYCoord() + 0.5;
        double bz = base.getZCoord() + 0.5;
        boolean fired = false;
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
            if (fired || cooldown > 0 || state == SamRules.State.NO_SAMS || state == SamRules.State.NO_POWER) {
                continue;
            }
            if (SamRules.stillNeeded(m.health(), samsAt(w, c.id())) > 0 && takeSam()
                && base.decreaseStoredEnergyUnits(SamRules.EU_PER_SHOT, false)) {
                EntitySam sam = EntitySam.launch(w, bx, base.getYCoord() + 1.2, bz, c.id());
                w.spawnEntityInWorld(sam);
                flying.add(sam);
                w.playSoundEffect(bx, by, bz, "fireworks.launch", 3.0F, 1.2F);
                cooldown = SamRules.FIRE_INTERVAL;
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
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        cooldown = Math.max(0, nbt.getInteger(K_COOL));
    }
}
