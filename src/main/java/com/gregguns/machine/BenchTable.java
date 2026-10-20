package com.gregguns.machine;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.gregguns.block.GregGunsCasings;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

/**
 * The table block of the Ammunition Works' filling bench. The pack's Chisel has no table or workbench (its carving
 * groups are stone, metal, glass, wood planks and decorative blocks), so the bench takes the best full-size
 * workbench the pack does have: Natura's wooden Workbench (any of its 13 wood metas), then the vanilla Crafting
 * Table, then Avaritia's Dire Crafting Table. All three are placeable cubes with a crafting-table look. If none of
 * them is registered (a pack without Natura and, impossibly, without vanilla), the position accepts a Works Casing so
 * the structure still builds.
 *
 * <p>
 * Blocks are matched by registry name and meta, never by class. The hologram and survival build place Natura's
 * redwood workbench when Natura is loaded, otherwise the vanilla Crafting Table. Both sides.
 */
// Source (Natura registers one block, "Natura.workbench", metas 0 to 12 = woodTextureNames; BlockWorkbench subclass,
// no tile entity):
// https://github.com/GTNewHorizons/Natura/blob/2.8.24/src/main/java/mods/natura/common/NContent.java#L325-L329
// https://github.com/GTNewHorizons/Natura/blob/2.8.24/src/main/java/mods/natura/common/NContent.java#L2017-L2018
// https://github.com/GTNewHorizons/Natura/blob/2.8.24/src/main/java/mods/natura/blocks/overrides/AlternateWorkbench.java
// (Natura.cfg in the pack: "Enable Wooden Workbenches" = true.)
// Source (Avaritia registers it as "Dire_Crafting"):
// https://github.com/GTNewHorizons/Avaritia/blob/1.99/src/main/java/fox/spiteful/avaritia/blocks/LudicrousBlocks.java#L33
// Decision (textures read from the pack's jars, 16x16): Natura's workbench has a round wicker-style top grid in the
// wood's color and tool-covered sides in the vanilla crafting-table layout, in 13 woods; the vanilla table is the
// familiar grid-and-tools cube; Avaritia's Dire table is a cyan lattice top with blue sides (a late-game look).
public final class BenchTable {

    /** The shape character the bench piece uses for a table. */
    public static final char SYMBOL = 'T';

    /** Natura's wooden workbench, any meta. */
    public static final String NATURA = "Natura:Natura.workbench";
    /** The vanilla Crafting Table. */
    public static final String VANILLA = "minecraft:crafting_table";
    /** Avaritia's Dire Crafting Table. */
    public static final String AVARITIA = "Avaritia:Dire_Crafting";

    /** Natura workbench metas (one per wood). */
    public static final int NATURA_METAS = 13;
    /** The Natura meta the hologram places: redwood. */
    public static final int NATURA_HOLOGRAM_META = 3;

    private BenchTable() {}

    /**
     * @param registryName a block's registry name, for example {@code "minecraft:crafting_table"}
     * @param meta         its meta
     * @return whether it is an accepted table
     */
    public static boolean accepts(String registryName, int meta) {
        if (registryName == null) {
            return false;
        }
        switch (registryName) {
            case NATURA:
                return meta >= 0 && meta < NATURA_METAS;
            case VANILLA:
            case AVARITIA:
                return true;
            default:
                return false;
        }
    }

    /** @return whether any accepted table block is registered in this game */
    static boolean anyTableRegistered() {
        return Block.blockRegistry.containsKey(NATURA) || Block.blockRegistry.containsKey(VANILLA)
            || Block.blockRegistry.containsKey(AVARITIA);
    }

    private static boolean acceptsBlock(Block block, int meta) {
        if (block == null) {
            return false;
        }
        if (accepts(Block.blockRegistry.getNameForObject(block), meta)) {
            return true;
        }
        // The defensive fallback: only when no table block exists at all.
        return block == GregGunsCasings.INSTANCE && meta == GregGunsCasings.WORKS_CASING && !anyTableRegistered();
    }

    /**
     * @param <T> the machine class
     * @return the structure element for {@link #SYMBOL}; the hologram shows a Natura workbench, else the vanilla table
     */
    public static <T> IStructureElement<T> element() {
        return StructureUtility.lazy(() -> {
            Block natura = (Block) Block.blockRegistry.getObject(NATURA);
            boolean haveNatura = natura != null && natura != Blocks.air;
            Block hint = haveNatura ? natura : Blocks.crafting_table;
            int hintMeta = haveNatura ? NATURA_HOLOGRAM_META : 0;
            return StructureUtility.<T>ofBlockAdder((t, block, meta) -> acceptsBlock(block, meta), hint, hintMeta);
        });
    }
}
