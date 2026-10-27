package com.gregguns.icbm.silo;

import net.minecraft.block.Block;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

import gregtech.api.GregTechAPI;

/**
 * The wall and floor-ring blocks of the Missile Silo (D116): GT's Light Concrete or Dark Concrete in any finished
 * finish, mixed freely. The hologram and auto-build place plain Light Concrete. Common side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (Structure)
// Source: GT registers one block, "gt.blockconcretes" (GregTechAPI.sBlockConcretes), whose metadata 0 to 7 is Dark
// Concrete (stone, cobblestone, mossy cobblestone, bricks, cracked bricks, mossy bricks, chiseled, smooth) and 8 to 15
// the same eight in Light Concrete; getDamageValue returns the world metadata:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockConcretes.java#L30-L36
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Textures.java#L2282-L2286
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockStonesAbstract.java#L237-L240
public final class SiloConcrete {

    /** Plain Light Concrete: what the hologram and auto-build place. */
    public static final int LIGHT = 8;
    /** Metadata bits of the finish within a color: 1 and 2 (cobblestone) are the rough forms and are refused. */
    private static final int ROUGH_MASK = (1 << 1) | (1 << 2);

    private SiloConcrete() {}

    /**
     * Whether a concrete metadata is a finished wall block. Both colors are accepted in the stone, bricks, cracked
     * bricks, mossy bricks, chiseled and smooth finishes. The cobblestone forms (plain and mossy) are rubble, the form
     * the plain stone drops as when mined, so a wall of them is refused.
     *
     * @param meta a GT concrete block's metadata
     * @return true for a finished variant
     */
    public static boolean acceptsMeta(int meta) {
        return meta >= 0 && meta < 16 && (ROUGH_MASK & (1 << (meta & 7))) == 0;
    }

    /**
     * @param block a block found at a wall cell
     * @param meta  its damage value
     * @return whether the wall accepts it
     */
    public static boolean accepts(Block block, int meta) {
        return block != null && block == GregTechAPI.sBlockConcretes && acceptsMeta(meta);
    }

    /**
     * @param <T> the machine class
     * @return the element for the wall cells; the hologram shows Light Concrete
     */
    public static <T> IStructureElement<T> element() {
        return StructureUtility.<T>lazy(
            () -> StructureUtility
                .<T>ofBlockAdder((t, block, meta) -> accepts(block, meta), GregTechAPI.sBlockConcretes, LIGHT));
    }
}
