package com.gregguns.icbm.compat;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

/**
 * The Galacticraft launch pad as a StructureLib element, for the silo's shaft floor. Blocks are found by registry name
 * through {@link GalacticraftParts}, so no Galacticraft class is loaded. Without Galacticraft (the module is then off
 * and the silo inert) the element accepts any block. Common side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The pad in the floor)
// Source: Galacticraft registers the pad as "tile.landingPad" (eight blocks) and "tile.landingPadFull" (the formed
// center, with the pad's tile entity) and "tile.dummyblock" meta 2 (the other eight, TileEntityLandingPad#onCreate);
// meta 0 is the rocket pad, meta 1 the buggy fueling pad:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/blocks/GCBlocks.java#L315-L323
public final class GalacticraftPads {

    private GalacticraftPads() {}

    /**
     * @param block a block found at a pad position
     * @param meta  its metadata (StructureLib passes the damage value, which is 0 for the dummy blocks)
     * @return true for either state of a rocket pad block: the unformed pad block or the formed center
     */
    public static boolean accepts(Block block, int meta) {
        Block pad = GalacticraftParts.findBlock(GalacticraftParts.PAD);
        Block full = GalacticraftParts.findBlock(GalacticraftParts.PAD_FULL);
        Block multi = GalacticraftParts.findBlock(GalacticraftParts.PAD_MULTI);
        if (pad == null || full == null || multi == null) {
            return true;
        }
        // Formed: the full block at the center and the dummy block (meta 2) on the other eight, as
        // TileEntityLandingPad.onCreate makes them. Unformed: the pad block, which Galacticraft forms itself.
        return (block == pad || block == full) && meta == 0 || block == multi;
    }

    /**
     * @param <T> the machine class
     * @return the element for the pad cells: accepts the formed pad and the nine unformed blocks (Galacticraft forms
     *         them itself); the hologram and auto-build place the unformed block
     */
    // Why the dummy block's metadata is not checked: StructureLib reads a block's "meta" with getDamageValue(), which
    // is damageDropped(meta). Galacticraft's BlockMulti does not override damageDropped, so the formed pad's dummy
    // blocks (placed with meta 2) read as 0 and a meta 2 test refused the pad one tick after Galacticraft formed it
    // (observed in the icbm.silo.structure scenario). Any dummy block under a rocket pad cell is accepted.
    public static <T> IStructureElement<T> element() {
        return StructureUtility.<T>lazy(() -> {
            Block hint = GalacticraftParts.findBlock(GalacticraftParts.PAD);
            return StructureUtility
                .<T>ofBlockAdder((t, block, meta) -> accepts(block, meta), hint == null ? Blocks.iron_block : hint, 0);
        });
    }
}
