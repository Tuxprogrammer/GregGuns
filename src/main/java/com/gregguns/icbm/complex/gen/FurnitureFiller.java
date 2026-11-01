package com.gregguns.icbm.complex.gen;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.WindowedSink;

/**
 * Sets the rooms' pieces over what the shell wrote (D259, D305): the BiblioCraft piece when BiblioCraft is loaded, a
 * GT Locker for a locker, the Vending Machine's casing (D355), a vanilla stair for a chair or a stool without
 * BiblioCraft, and every prop's block from {@link PropBlocks} (beds, crates, containers, furnaces, inlays). It writes
 * the state-free look: a cold furnace, an empty cauldron, bare wet farmland; the contents pass sets what the room's
 * state changes, and places the metal props, whose rung follows the ceiling. Each mark is one cell, so a bed's halves
 * or a door's are written by the window that owns each one. Writes only inside the window. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D259; D262; D305; D306
public final class FurnitureFiller implements MarkFiller {

    @Override
    public void fill(StructurePlan plan, Mark mark, int x, int y, int z, WindowedSink sink) {
        fill(null, plan, mark, x, y, z, sink);
    }

    @Override
    public void fill(World world, StructurePlan plan, Mark mark, int x, int y, int z, WindowedSink sink) {
        if (mark.kind() != MarkKind.FURNITURE) return;
        FurniturePiece p = FurniturePiece.of(mark.key());
        if (p == null || p.role() == Cell.FURN_CONSOLE
            || p == FurniturePiece.COUNTER
            || p == FurniturePiece.PARTITION) {
            return; // the style's cubes and the wall: the shell wrote them
        }
        int facing = mark.facing() & 3;
        PropBlocks.Ref prop = PropBlocks.block(mark.key(), facing);
        if (prop != null) {
            if (sink.setBlock(x, y, z, prop.block, prop.meta) && world != null && aimsItself(prop.block)) {
                // The block turned itself by its neighbors as it was placed; the piece faces the way its mark does.
                world.setBlockMetadataWithNotify(x, y, z, prop.meta, 2);
            }
            if (p == FurniturePiece.MYCELIUM_BED) {
                PropBlocks.Ref m = PropBlocks.mushroom(mark.key());
                sink.setBlock(x, y + 1, z, m.block, m.meta);
            }
            return;
        }
        if (p.role() == Cell.PROP) return; // a metal prop: the contents pass places it
        if (p == FurniturePiece.LOCKER) {
            // D263: GregTech's Locker, the pack's one real locker block; the shell's shelf stays without a world.
            if (world != null && GtMachineHook.lockerAvailable() && sink.setBlock(x, y, z, Blocks.air, 0)) {
                GtMachineHook.placeLocker(world, x, y, z, facing);
            }
            return;
        }
        if (p == FurniturePiece.VENDING_MACHINE) {
            // D355: the Vending Machine's casing in all six cells; the contents pass sets its controller. Without the
            // mod (or without a world) the style's cabinet stays.
            if (world != null && VendingMachinePiece.present()) sink.setBlock(x, y, z, VendingMachinePiece.casing(), 0);
            return;
        }
        int wood = FurniturePiece.woodOf(mark.key());
        if (BiblioFurniture.place(world, sink, p, wood, facing, x, y, z)) return;
        if (p.seat()) sink.setBlock(x, y, z, stairs(wood), stairMeta(facing));
    }

    /**
     * @param b a block
     * @return whether the block picks its own facing from its neighbors when it is placed (a vanilla furnace, chest,
     *         dispenser or dropper), so the meta it was written with must be written again (D345)
     */
    public static boolean aimsItself(Block b) {
        return b == Blocks.furnace || b == Blocks.lit_furnace
            || b == Blocks.chest
            || b == Blocks.trapped_chest
            || b == Blocks.dispenser
            || b == Blocks.dropper;
    }

    /**
     * Whether a cell holds the block this filler (or the contents pass, for a lit furnace) writes for a piece, so its
     * stock goes into the piece and not into a container that was there before (D345).
     *
     * @param b     the block standing in the cell
     * @param piece the piece's name ({@link FurniturePiece#name()}) or mark key
     * @return whether {@code b} is one of the piece's blocks: its BiblioCraft block, its prop block, or a furnace of
     *         either state
     */
    public static boolean placed(Block b, String piece) {
        FurniturePiece p = FurniturePiece.of(piece);
        if (b == null || p == null) return false;
        String name = BiblioFurniture.registryName(p);
        if (name != null && BiblioFurniture.loaded() && b == BiblioFurniture.block(name)) return true;
        if (p == FurniturePiece.FURNACE) return b == Blocks.furnace || b == Blocks.lit_furnace;
        PropBlocks.Ref prop = PropBlocks.block(piece, 0);
        return prop != null && b == prop.block;
    }

    /** @return the vanilla stair of a wood: oak, spruce, birch, jungle, acacia, dark oak */
    static Block stairs(int wood) {
        switch (wood) {
            case 1:
                return Blocks.spruce_stairs;
            case 2:
                return Blocks.birch_stairs;
            case 3:
                return Blocks.jungle_stairs;
            case 4:
                return Blocks.acacia_stairs;
            case 5:
                return Blocks.dark_oak_stairs;
            default:
                return Blocks.oak_stairs;
        }
    }

    /**
     * A stair as a chair: its tall back on the side opposite the way the sitter faces. Stair metas 0 to 3 put the tall
     * half on the east, west, south and north side.
     *
     * @param facing the way the chair faces (0 south, 1 west, 2 north, 3 east)
     */
    static int stairMeta(int facing) {
        switch (facing & 3) {
            case 0:
                return 3; // faces south: back to the north
            case 1:
                return 0; // faces west: back to the east
            case 2:
                return 2; // faces north: back to the south
            default:
                return 1; // faces east: back to the west
        }
    }
}
