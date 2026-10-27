package com.gregguns.icbm.compat.oc;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.icbm.emp.MTEEmpTower;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import li.cil.oc.api.driver.SidedBlock;
import li.cil.oc.api.network.ManagedEnvironment;

/**
 * The OpenComputers driver that turns an EMP Tower next to an Adapter into the {@code gg_emp_tower} component. Common
 * side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_emp_tower)
final class EmpTowerDriver implements SidedBlock {

    // Source:
    // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/driver/SidedBlock.java
    @Override
    public boolean worksWith(World world, int x, int y, int z, ForgeDirection side) {
        return tower(world, x, y, z) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z, ForgeDirection side) {
        MTEEmpTower tower = tower(world, x, y, z);
        return tower == null ? null : new EmpTowerEnvironment(tower);
    }

    private static MTEEmpTower tower(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) tile).getMetaTileEntity() instanceof MTEEmpTower) {
            return (MTEEmpTower) ((IGregTechTileEntity) tile).getMetaTileEntity();
        }
        return null;
    }
}
