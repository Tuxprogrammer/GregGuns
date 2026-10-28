package com.gregguns.icbm.complex.gen;

/** Places the real machines of a silo (controller and hatches). Needs GT, so it is a seam for unit tests. */
public interface MachineHook {

    MachineHook NONE = (world, cell, x, y, z) -> {};

    /**
     * @param world the world being populated; the position is inside the window and its block is already the air
     *              placeholder
     * @param cell  a {@link com.gregguns.icbm.complex.Cell} machine class; the position is inside the window
     */
    void place(net.minecraft.world.World world, byte cell, int x, int y, int z);
}
