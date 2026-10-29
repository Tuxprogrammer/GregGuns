package com.gregguns.icbm.complex.gen;

import com.gregguns.icbm.complex.Mark;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.WindowedSink;

/**
 * The seam to the activity, guard and loot code (coder B): called once per mark whose world position lies in the
 * window being populated, with the activity-independent plan. The filler writes only inside the window through
 * {@code sink}.
 */
public interface MarkFiller {

    MarkFiller NONE = (plan, mark, x, y, z, sink) -> {};

    /**
     * @param mark the mark; its x, y, z are complex-local
     * @param x    world x
     * @param y    world y
     * @param z    world z
     */
    void fill(StructurePlan plan, Mark mark, int x, int y, int z, WindowedSink sink);

    /**
     * As {@link #fill(StructurePlan, Mark, int, int, int, WindowedSink)}, with the world being populated, for a filler
     * that sets a tile entity's state after placing its block (a piece of furniture, D259). The placer calls this one.
     *
     * @param world the world, or null when the writer has none (a unit test)
     */
    default void fill(net.minecraft.world.World world, StructurePlan plan, Mark mark, int x, int y, int z,
        WindowedSink sink) {
        fill(plan, mark, x, y, z, sink);
    }
}
