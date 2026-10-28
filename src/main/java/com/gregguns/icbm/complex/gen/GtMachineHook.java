package com.gregguns.icbm.complex.gen;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
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
 * Places the silo's GT machines: the Missile Silo controller and the hatches around it, facing north (the silo's
 * front),
 * the same way {@code DevStructureBuilder} builds a silo.
 */
// Source: DevStructureBuilder.place (this repo) for the GT calls; GT5-Unofficial 5.09.54.205 BaseMetaTileEntity API.
public final class GtMachineHook implements MachineHook {

    /** The Energy Hatch of a stamped silo; the chests' missing-part hatch is this item too, so the part always fits. */
    public static final ItemList ENERGY_HATCH = ItemList.Hatch_Energy_EV;

    @Override
    public void place(World world, byte cell, int x, int y, int z) {
        int id = idOf(cell);
        if (id < 0) return;
        placeMte(world, id, x, y, z);
    }

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
        base.setFrontFacing(ForgeDirection.NORTH);
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
