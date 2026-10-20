package com.gregguns.worldgen.camp;

import net.minecraft.block.Block;

import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StructureBiomes.CampColor;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.MarkListener;

/**
 * What the generator (G9) needs from a camp placer: write the slice of a camp inside one populate window, then run the
 * flag post-step in the same window. Implemented by {@link CampPlacer}; adopt it so the generator can be tested with a
 * stand-in.
 */
// Spec: camp_parts.md; PLAN-M8 section 1 (G9 calls G10)
public interface CampWriter {

    /** Stamps the slice of the camp inside the window; returns the number of blocks the sink accepted. */
    int write(WindowedSink sink, PlacedCamp camp, PopulateWindow window, CampColor color, MarkListener marks);

    /** The flag post-step for this camp; run it after {@code write} in the same window. */
    CampFlags.FlagPostStep flagStep(PlacedCamp camp, Block flagBlock, float capFactor, CampFlags.Recorder recorder);
}
