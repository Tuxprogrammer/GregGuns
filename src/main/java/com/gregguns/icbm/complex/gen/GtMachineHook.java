package com.gregguns.icbm.complex.gen;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.threads.RunnableMachineUpdate;

/**
 * Places the silo's GT machines: the Missile Silo controller and the hatches around it, facing the silo's front (toward
 * its gallery), the same way {@code DevStructureBuilder} builds a silo.
 */
// Source: DevStructureBuilder.place (this repo) for the GT calls; GT5-Unofficial 5.09.54.205 BaseMetaTileEntity API.
public final class GtMachineHook implements MachineHook {

    /** The Energy Hatch of a stamped silo; the chests' missing-part hatch is this item too, so the part always fits. */
    public static final ItemList ENERGY_HATCH = ItemList.Hatch_Energy_EV;

    @Override
    public void place(World world, byte cell, int x, int y, int z) {
        place(world, cell, x, y, z, 0);
    }

    @Override
    public void place(World world, byte cell, int x, int y, int z, int facing) {
        int id = idOf(cell);
        if (id < 0) return;
        placeMte(world, id, x, y, z, FACINGS[facing & 3]);
    }

    /** Our facing index (0 north, 1 east, 2 south, 3 west) as a Forge direction. */
    private static final ForgeDirection[] FACINGS = { ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH,
        ForgeDirection.WEST };

    /**
     * Sets a GT machine block with the meta tile entity {@code id} at the position, facing north (a silo's front).
     *
     * @param world the world
     * @param id    the meta tile entity id
     * @param x     X
     * @param y     Y
     * @param z     Z
     */
    public static void placeMte(World world, int id, int x, int y, int z) {
        placeMte(world, id, x, y, z, ForgeDirection.NORTH);
    }

    /**
     * As {@link #placeMte(World, int, int, int, int)}, facing {@code front}.
     *
     * @param front the way the machine's front faces
     */
    /** The locker of the small rooms (D263): GregTech's ULV Locker, which holds a suit of armor. */
    public static final ItemList LOCKER = ItemList.Locker_ULV;

    /**
     * Places a GT Locker whose front faces {@code facing} (0 south, 1 west, 2 north, 3 east: a hanging direction).
     *
     * @param world the world; the cell is already air and inside the window
     */
    public static void placeLocker(World world, int x, int y, int z, int facing) {
        ForgeDirection[] byHanging = { ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH,
            ForgeDirection.EAST };
        placeMte(
            world,
            LOCKER.get(1)
                .getItemDamage(),
            x,
            y,
            z,
            byHanging[facing & 3]);
    }

    public static void placeMte(World world, int id, int x, int y, int z, ForgeDirection front) {
        String cell = x + "," + y + "," + z;
        IMetaTileEntity proto = GregTechAPI.METATILEENTITIES[id];
        if (proto == null) {
            GregGuns.LOG.warn("Silo complex: no meta tile entity {} for cell {}", id, cell);
            return;
        }
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, proto.getTileEntityBaseType(), 2);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity)) {
            GregGuns.LOG.warn("Silo complex: no GT tile entity at {} {} {}", x, y, z);
            return;
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        claim(base);
        base.setFrontFacing(front);
    }

    /** The owner every generated machine carries (D239). */
    public static final String OWNER_NAME = "[Silo Complex]";
    public static final java.util.UUID OWNER_UUID = java.util.UUID
        .nameUUIDFromBytes("gregguns:silo_complex".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /**
     * Gives a generated GT machine an owner. A multiblock that stops (it is broken, or it loses its structure or power)
     * files a power-fail event under its owner, and GT's tracker throws on a machine with none: a generated radar or
     * silo controller then errors every tick or when it is removed (D239).
     *
     * @param base the machine's tile
     */
    public static void claim(IGregTechTileEntity base) {
        base.setOwnerName(OWNER_NAME);
        base.setOwnerUuid(OWNER_UUID);
    }

    /**
     * Hands a generated machine to the first real player who uses it: a captured silo, Launch Control or radar then
     * files its power-fail notices, wireless energy and launches under that player instead of the complex. GT sets an
     * owner only while none is set ({@code BaseMetaTileEntity.playerOwnsThis}), so the complex's owner would stick
     * otherwise (D250). A fake player (a turtle, a deployer) never takes a machine over.
     *
     * @param base   the machine's tile
     * @param player the player who right-clicked it
     * @return true when the machine changed hands
     */
    // Source: GT sets the owner only when it is empty, at 5.09.54.205:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L1305-L1314
    public static boolean handOver(IGregTechTileEntity base, EntityPlayer player) {
        if (base == null || player == null
            || !takesOver(base.getOwnerUuid(), player instanceof FakePlayer, base.isClientSide())) {
            return false;
        }
        base.setOwnerName(player.getDisplayName());
        base.setOwnerUuid(player.getUniqueID());
        return true;
    }

    /**
     * @param owner      the machine's owner UUID (null when it has none)
     * @param fakePlayer whether the user is a fake player
     * @param clientSide whether this is the client's copy
     * @return whether the user takes the machine over: only a real player, on the server, from the complex
     */
    static boolean takesOver(java.util.UUID owner, boolean fakePlayer, boolean clientSide) {
        return !clientSide && !fakePlayer && OWNER_UUID.equals(owner);
    }

    @Override
    public boolean suspendUpdates() {
        boolean before = RunnableMachineUpdate.isCurrentThreadEnabled();
        RunnableMachineUpdate.setCurrentThreadEnabled(false);
        return before;
    }

    @Override
    public void resumeUpdates(boolean before) {
        RunnableMachineUpdate.setCurrentThreadEnabled(before);
    }

    private static int idOf(byte cell) {
        switch (cell) {
            case Cell.CONTROLLER:
                return GregGunsMTEIDs.MISSILE_SILO;
            case Cell.RADAR_CONTROLLER:
                return GregGunsMTEIDs.RADAR_HV;
            case Cell.HATCH_BUS:
                return ItemList.Hatch_Input_Bus_HV.get(1)
                    .getItemDamage();
            case Cell.HATCH_MAINTENANCE:
                return ItemList.Hatch_Maintenance.get(1)
                    .getItemDamage();
            case Cell.HATCH_ENERGY:
                return ENERGY_HATCH.get(1)
                    .getItemDamage();
            case Cell.HATCH_POWER:
                return ItemList.Hatch_Input_HV.get(1)
                    .getItemDamage();
            default:
                return -1;
        }
    }
}
