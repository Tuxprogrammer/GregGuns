package com.gregguns.icbm.defense;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.icbm.detect.Contacts;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * The part of a radar the small radar and the tower share: the protected radius, the latest sweep and state, the
 * redstone and active outputs, and the data stick link. The machine owns the power and the structure; it calls
 * {@link #scan} every {@link RadarRules#SCAN_TICKS} ticks and {@link #publish} after. Signals change only on a
 * transition, so a quiet radar does not spam neighbor updates. Server side, except the NBT.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Sweep, Outputs)
public final class RadarUnit {

    private static final String K_RADIUS = "radius";

    private int radius = RadarRules.DEFAULT_RADIUS;
    private Sweep sweep = Sweep.EMPTY;
    private RadarRules.State state = RadarRules.State.OFF;
    private int signal = -1;
    private boolean lastActive;

    /** @return the protected radius, blocks */
    public int radius() {
        return radius;
    }

    /**
     * @param wanted the radius a player typed
     * @param range  the radar's range, for the clamp
     */
    public void setRadius(int wanted, int range) {
        radius = RadarRules.clampRadius(wanted, range);
    }

    /** @return the latest sweep */
    public Sweep sweep() {
        return sweep;
    }

    /** @return what the radar reports now */
    public RadarRules.State state() {
        return state;
    }

    /** @return the redstone strength last published, 0 to 15 (0 before the first publish) */
    public int signal() {
        return Math.max(signal, 0);
    }

    /**
     * Sweeps the tracker.
     *
     * @param world   the radar's world
     * @param x       the radar's X (block center)
     * @param z       the radar's Z
     * @param range   the detection range
     * @param powered whether the radar paid for this scan; false clears the sweep
     */
    public void scan(World world, double x, double z, int range, boolean powered) {
        if (!powered || range <= 0) {
            sweep = Sweep.EMPTY;
            state = RadarRules.State.OFF;
            return;
        }
        sweep = Sweep.of(Contacts.within(world, x, z, range), x, z, radius);
        state = RadarRules.state(
            true,
            sweep.detected()
                .size(),
            sweep.incoming()
                .size());
    }

    /**
     * Sets the redstone strength on every side and the active flag (what a GT machine-state cover reads), only when
     * they changed.
     *
     * @param base the tile
     */
    public void publish(IGregTechTileEntity base) {
        int want = RadarRules.redstone(
            sweep.incoming()
                .size());
        if (want != signal) {
            signal = want;
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                base.setInternalOutputRedstoneSignal(d, (byte) want);
            }
        }
        boolean threat = state == RadarRules.State.THREAT;
        if (threat != lastActive) {
            lastActive = threat;
            base.setActive(threat);
        }
    }

    /**
     * Sets the redstone strength on every side, only when it changed. A multiblock's active flag is its own running
     * state, so it is left alone.
     *
     * @param base the tile
     */
    public void publishRedstone(IGregTechTileEntity base) {
        int want = RadarRules.redstone(
            sweep.incoming()
                .size());
        if (want != signal) {
            signal = want;
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                base.setInternalOutputRedstoneSignal(d, (byte) want);
            }
        }
    }

    /** Clears the outputs when the radar goes away or switches off. */
    public void reset() {
        sweep = Sweep.EMPTY;
        state = RadarRules.State.OFF;
    }

    /**
     * Writes this radar's address to a data stick the player holds, for a battery to read.
     *
     * @param base   the radar tile
     * @param stick  a GT data stick
     * @param player who is told
     * @return true when the stick was written
     */
    public static boolean writeLink(IGregTechTileEntity base, ItemStack stick, EntityPlayer player) {
        if (!isStick(stick) || base.isClientSide()) {
            return false;
        }
        if (!stick.hasTagCompound()) {
            stick.setTagCompound(new NBTTagCompound());
        }
        RadarLink link = new RadarLink(
            base.getWorld().provider.dimensionId,
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord());
        link.write(stick.getTagCompound());
        if (player != null) {
            player.addChatMessage(new ChatComponentTranslation("gregguns.icbm.radar.link_saved", link.toString()));
        }
        return true;
    }

    /**
     * @param stack any stack
     * @return true for a GT data stick
     */
    public static boolean isStick(ItemStack stack) {
        return stack != null && ItemList.Tool_DataStick.isStackEqual(stack, false, true);
    }

    /** @param tag the machine's NBT */
    public void save(NBTTagCompound tag) {
        tag.setInteger(K_RADIUS, radius);
    }

    /** @param tag the machine's NBT */
    public void load(NBTTagCompound tag) {
        radius = tag.hasKey(K_RADIUS) ? Math.max(RadarRules.MIN_RADIUS, tag.getInteger(K_RADIUS))
            : RadarRules.DEFAULT_RADIUS;
    }
}
