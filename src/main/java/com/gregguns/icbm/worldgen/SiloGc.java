package com.gregguns.icbm.worldgen;

import net.minecraft.block.Block;

/**
 * The one place the complex filler touches Galacticraft, by reflection so a pack without it, or with the module off,
 * never loads a Galacticraft class. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Machines)
final class SiloGc {

    private static boolean tried;
    private static Block nasa;

    private SiloGc() {}

    /** @return Galacticraft's NASA Workbench block, or null when Galacticraft is absent */
    static synchronized Block nasaWorkbench() {
        if (!tried) {
            tried = true;
            try {
                // Source:
                // https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/blocks/GCBlocks.java#L51
                nasa = (Block) Class.forName("micdoodle8.mods.galacticraft.core.blocks.GCBlocks")
                    .getField("nasaWorkbench")
                    .get(null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                nasa = null;
            }
        }
        return nasa;
    }
}
