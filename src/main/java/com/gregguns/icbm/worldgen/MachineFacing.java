package com.gregguns.icbm.worldgen;

import java.util.function.Predicate;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;

import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/**
 * Gives a generated GT machine a horizontal front (D129). A fresh GT tile faces DOWN, and a basic machine (a pump, a
 * hull, a generator) has no main facing yet: GT sets both when a player places the item, which a generated machine
 * never goes through, so every pump stood with its output down and its front unset. This picks a valid horizontal side
 * (the side the room's door is on, toward the walkway, when that side is allowed) and writes it into the tile the way
 * GT's own world load would read it back.
 *
 * <p>
 * The tile has no world while a complex is written (the writer hands it to a sink), so the usual
 * {@code setFrontFacing} cannot run: it schedules texture packets and machine updates on the world. The facing goes
 * through the NBT instead, the path GT itself uses for a machine loaded from disk. Server side.
 */
// Source: GT5-Unofficial 5.09.54.205, the player-placement order in ItemMachines.placeBlockAt (setFrontFacing, then
// initDefaultModes) and MTEBasicMachine.setMainFacing:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/ItemMachines.java#L286-L303
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L244-L252
public final class MachineFacing {

    /** Our facing index (0 north, 1 east, 2 south, 3 west) as a Forge direction. */
    private static final ForgeDirection[] COMPASS = { ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH,
        ForgeDirection.WEST };

    private MachineFacing() {}

    /** @return the direction of a facing index (0 north, 1 east, 2 south, 3 west; any int wraps) */
    public static ForgeDirection compass(int index) {
        return COMPASS[index & 3];
    }

    /**
     * Picks a front: the preferred side when allowed, else the next horizontal sides clockwise, else UP or DOWN when
     * the machine allows only those.
     *
     * @param preferred the preferred facing index (0 north, 1 east, 2 south, 3 west)
     * @param valid     whether the machine allows a facing
     * @return the front, or null when the machine allows none
     */
    public static ForgeDirection choose(int preferred, Predicate<ForgeDirection> valid) {
        for (int i = 0; i < 4; i++) {
            ForgeDirection d = compass(preferred + i);
            if (valid.test(d)) return d;
        }
        if (valid.test(ForgeDirection.UP)) return ForgeDirection.UP;
        if (valid.test(ForgeDirection.DOWN)) return ForgeDirection.DOWN;
        return null;
    }

    /**
     * Builds the tile of a machine with its front set. A basic machine's main facing is the chosen side and its output
     * side is the opposite one, the order GT leaves a machine in after a player places it.
     *
     * @param id        the meta tile entity id
     * @param preferred the preferred facing index (0 north, 1 east, 2 south, 3 west)
     * @param owner     called on the tile before it is saved, to give it an owner; may be null
     * @return the tile, ready for a sink, or null when the id has no machine
     */
    public static BaseMetaTileEntity build(int id, int preferred,
        java.util.function.Consumer<BaseMetaTileEntity> owner) {
        BaseMetaTileEntity fresh = new BaseMetaTileEntity();
        fresh.setInitialValuesAsNBT(null, (short) id);
        if (fresh.getMetaTileEntity() == null) return null;
        if (owner != null) owner.accept(fresh);
        boolean basic = fresh.getMetaTileEntity() instanceof MTEBasicMachine;
        // A basic machine with no main facing yet answers isFacingValid with its horizontal-only main-facing rule.
        ForgeDirection front = choose(
            preferred,
            d -> fresh.getMetaTileEntity()
                .isFacingValid(d));
        if (front == null) {
            GregGuns.LOG.warn("Silo complex: machine {} allows no front facing", id);
            return fresh;
        }
        NBTTagCompound tag = new NBTTagCompound();
        fresh.writeToNBT(tag);
        if (basic) {
            tag.setInteger("mMainFacing", front.ordinal());
            tag.setShort(
                "mFacing",
                (short) front.getOpposite()
                    .ordinal());
        } else {
            tag.setShort("mFacing", (short) front.ordinal());
        }
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.readFromNBT(tag);
        return tile;
    }
}
