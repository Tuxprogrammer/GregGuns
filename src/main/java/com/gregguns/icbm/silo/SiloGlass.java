package com.gregguns.icbm.silo;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.gregguns.GregGuns;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The glass the Missile Silo's rings accept (D115): Tinkers' Construct Clear Glass and EnderIO's Quite Clear Glass (and
 * its Enlightened variant), mixed freely, and nothing else, because GT's borosilicate frames and tinted glass made a
 * Tier 3 missile hard to see. Blocks are found by registry name, so no Tinkers' or EnderIO class is loaded. When
 * neither
 * mod is loaded (the GregGuns dev environment) the rings accept vanilla glass instead, with one logged warning. Common
 * side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (Structure)
// Source: Tinkers' Construct registers its clear glass as block "GlassBlock" in mod "TConstruct", with no metadata
// (its stained glass is a different block):
// https://github.com/GTNewHorizons/TinkersConstruct/blob/1.14.118-GTNH/src/main/java/tconstruct/smeltery/TinkerSmeltery.java#L513
// https://github.com/GTNewHorizons/TinkersConstruct/blob/1.14.118-GTNH/src/main/java/tconstruct/smeltery/TinkerSmeltery.java#L584
// Source: EnderIO's glasses are one block, "blockFusedQuartz" in mod "EnderIO", whose metadata is the Type ordinal:
// 0 Fused Quartz, 1 Quite Clear Glass (fusedGlass), 2 Enlightened Fused Quartz, 3 Enlightened Clear Glass, 4 Dark
// Fused Quartz, 5 Dark Clear Glass. The Dark variants have light opacity 255, so they are not see-through. Its
// damageDropped returns the metadata, so StructureLib's getDamageValue read is the real meta:
// https://github.com/GTNewHorizons/EnderIO/blob/2.10.48/src/main/java/crazypants/enderio/material/BlockFusedQuartz.java#L35-L51
// https://github.com/GTNewHorizons/EnderIO/blob/2.10.48/src/main/java/crazypants/enderio/material/BlockFusedQuartz.java#L183-L187
public final class SiloGlass {

    /** Tinkers' Construct's mod id. */
    public static final String TCONSTRUCT = "TConstruct";
    /** Tinkers' Construct's Clear Glass registry name. */
    public static final String TC_CLEAR = "GlassBlock";
    /** EnderIO's mod id. */
    public static final String ENDERIO = "EnderIO";
    /** EnderIO's fused quartz and glass block registry name. */
    public static final String EIO_BLOCK = "blockFusedQuartz";
    /** EnderIO's Quite Clear Glass metadata. */
    public static final int EIO_CLEAR_META = 1;
    /** EnderIO's Enlightened Clear Glass metadata: the same glass, giving off light. */
    public static final int EIO_ENLIGHTENED_META = 3;
    /** The fallback glass when neither mod is loaded. */
    public static final String VANILLA = "minecraft:glass";

    private static boolean warned;

    private SiloGlass() {}

    /**
     * @param registryName a block's registry name, {@code modid:name}
     * @param meta         its damage value
     * @param fallback     whether neither mod is loaded, so vanilla glass stands in
     * @return whether the block is accepted in a glass ring
     */
    public static boolean accepts(String registryName, int meta, boolean fallback) {
        if (registryName == null) {
            return false;
        }
        if (fallback) {
            return registryName.equals(VANILLA);
        }
        if (registryName.equals(TCONSTRUCT + ":" + TC_CLEAR)) {
            return true;
        }
        return registryName.equals(ENDERIO + ":" + EIO_BLOCK)
            && (meta == EIO_CLEAR_META || meta == EIO_ENLIGHTENED_META);
    }

    /**
     * @return true when neither Tinkers' Construct nor EnderIO is loaded
     */
    public static boolean fallbackActive() {
        try {
            return !Loader.isModLoaded(TCONSTRUCT) && !Loader.isModLoaded(ENDERIO);
        } catch (RuntimeException | LinkageError e) {
            return false; // outside a running game (unit tests)
        }
    }

    /**
     * @param block a block found at a glass cell
     * @param meta  its damage value
     * @return whether the ring accepts it in this installation
     */
    public static boolean accepts(Block block, int meta) {
        String name = Block.blockRegistry.getNameForObject(block);
        return accepts(name, meta, fallbackActive());
    }

    /**
     * @param <T> the machine class
     * @return the element for the ring cells: the accepted glasses; the hologram and auto-build place Tinkers' Clear
     *         Glass, or EnderIO's when Tinkers' is absent, or vanilla glass when neither is loaded
     */
    public static <T> IStructureElement<T> element() {
        return StructureUtility.<T>lazy(() -> {
            Block hint = Blocks.glass;
            int hintMeta = 0;
            if (fallbackActive()) {
                if (!warned) {
                    warned = true;
                    GregGuns.LOG.warn(
                        "Neither Tinkers' Construct nor EnderIO is loaded: the Missile Silo's glass rings accept vanilla glass");
                }
            } else {
                Block tc = Loader.isModLoaded(TCONSTRUCT) ? GameRegistry.findBlock(TCONSTRUCT, TC_CLEAR) : null;
                Block eio = Loader.isModLoaded(ENDERIO) ? GameRegistry.findBlock(ENDERIO, EIO_BLOCK) : null;
                if (tc != null) {
                    hint = tc;
                } else if (eio != null) {
                    hint = eio;
                    hintMeta = EIO_CLEAR_META;
                }
            }
            return StructureUtility.<T>ofBlockAdder((t, block, meta) -> accepts(block, meta), hint, hintMeta);
        });
    }
}
