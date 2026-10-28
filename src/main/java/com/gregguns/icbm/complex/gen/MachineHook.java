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

    /**
     * As {@link #place}, turned: a silo's controller and hatches face its gallery.
     *
     * @param facing the way the machine's front faces: 0 north, 1 east, 2 south, 3 west
     */
    default void place(net.minecraft.world.World world, byte cell, int x, int y, int z, int facing) {
        place(world, cell, x, y, z);
    }

    /**
     * Called before a window's whole write: GregTech blocks the complex still places (silo concrete, hatches) must not
     * each start a machine-update search, so the real hook switches the update off for this thread.
     *
     * @return what to hand back to {@link #resumeUpdates}
     */
    default boolean suspendUpdates() {
        return true;
    }

    /** @param before the value {@link #suspendUpdates} returned */
    default void resumeUpdates(boolean before) {}
}
