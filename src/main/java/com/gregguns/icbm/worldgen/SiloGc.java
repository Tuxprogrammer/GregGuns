package com.gregguns.icbm.worldgen;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.GregGuns;
import com.gregguns.worldgen.WindowedSink;

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

    /**
     * Places a NASA Workbench the way Galacticraft's own placement does: the main block with its tile, and the
     * dummy blocks of its footprint (the three cells above it and the four arms beside the two cells over it), each
     * tied back to the main block. Breaking any part then takes the whole workbench down and nothing else. Each
     * window writes the cells of its own columns, so a workbench on a window edge is still whole.
     *
     * @return true when the workbench's main cell was written
     */
    // Source:
    // https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/tile/TileEntityNasaWorkbench.java#L35-L66
    // (onCreate: layers 1 and 2 hold the centre and the four arms, layer 3 the centre, all fake blocks of meta 3)
    static boolean placeNasaWorkbench(WindowedSink sink, int x, int y, int z) {
        Block main = nasaWorkbench();
        if (main == null) {
            return false;
        }
        try {
            Block fake = fakeBlock();
            if (fake == null) {
                return false;
            }
            Class<?> vec = Class.forName("micdoodle8.mods.galacticraft.api.vector.BlockVec3");
            Object mainPos = vec.getConstructor(int.class, int.class, int.class)
                .newInstance(x, y, z);
            java.lang.reflect.Field field = Class.forName("micdoodle8.mods.galacticraft.core.tile.TileEntityMulti")
                .getField("mainBlockPosition");
            boolean wrote = false;
            for (int[] c : footprint()) {
                boolean isMain = c[1] == 0 && c[0] == 0 && c[2] == 0;
                Block b = isMain ? main : fake;
                int meta = isMain ? 0 : 3;
                if (sink.setBlock(x + c[0], y + c[1], z + c[2], b, meta)) {
                    TileEntity t = b.createTileEntity(null, meta);
                    if (t != null) {
                        field.set(t, mainPos);
                        sink.setTile(x + c[0], y + c[1], z + c[2], t);
                    }
                    wrote |= isMain;
                }
            }
            return wrote;
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.warn("NASA Workbench not placed: {}", e.toString());
            return false;
        }
    }

    /** @return Galacticraft's dummy block (the parts of a multiblock), or null when it is absent */
    static Block fakeBlock() {
        try {
            return (Block) Class.forName("micdoodle8.mods.galacticraft.core.blocks.GCBlocks")
                .getField("fakeBlock")
                .get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** @return the {dx, dy, dz} of every cell of a workbench: the main block, then the dummy blocks */
    static int[][] footprint() {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        out.add(new int[] { 0, 0, 0 });
        for (int dy = 1; dy <= 2; dy++) {
            out.add(new int[] { 0, dy, 0 });
            out.add(new int[] { 1, dy, 0 });
            out.add(new int[] { -1, dy, 0 });
            out.add(new int[] { 0, dy, 1 });
            out.add(new int[] { 0, dy, -1 });
        }
        out.add(new int[] { 0, 3, 0 });
        return out.toArray(new int[0][]);
    }
}
