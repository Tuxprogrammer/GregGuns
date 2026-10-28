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
}
