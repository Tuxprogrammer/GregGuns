package com.gregguns.icbm.compat.oc;

import java.util.function.Function;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import li.cil.oc.api.driver.SidedBlock;
import li.cil.oc.api.network.ManagedEnvironment;

/**
 * The OpenComputers driver that turns a GT machine next to an Adapter into a component. GT's tile entity cannot be an
 * OpenComputers environment itself, so the driver looks at the machine's meta tile entity: any machine that implements
 * the view interface {@code V} becomes the component the factory builds for it. One class serves every ICBM machine,
 * so a new component is a view interface, a logic class and an environment, never a new driver. Common side.
 *
 * @param <V> the view interface the machine implements
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (Integration points)
final class ViewDriver<V> implements SidedBlock {

    private final Class<V> view;
    private final Function<V, ManagedEnvironment> factory;

    ViewDriver(Class<V> view, Function<V, ManagedEnvironment> factory) {
        this.view = view;
        this.factory = factory;
    }

    // Source:
    // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/driver/SidedBlock.java
    @Override
    public boolean worksWith(World world, int x, int y, int z, ForgeDirection side) {
        return viewAt(world, x, y, z) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z, ForgeDirection side) {
        V v = viewAt(world, x, y, z);
        return v == null ? null : factory.apply(v);
    }

    private V viewAt(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IGregTechTileEntity) {
            Object meta = ((IGregTechTileEntity) tile).getMetaTileEntity();
            if (view.isInstance(meta)) {
                return view.cast(meta);
            }
        }
        return null;
    }
}
