package com.gregguns.icbm.complex.gen;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.worldgen.WindowedSink;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * BiblioCraft's furniture for the small rooms of a silo complex (D259, D262), an optional integration: blocks are
 * looked up by registry name at write time and their facing set through their tile entity's NBT. No BiblioCraft class
 * is linked and none of its assets ship with GregGuns. Without BiblioCraft, {@link #place} declines and the room keeps
 * its cube furniture. Server side.
 *
 * <p>
 * Every piece is set as if a player standing in front of it had placed it: BiblioCraft turns a piece to face the
 * player who places it, storing {@code (playerFacing + 1) % 4} with the player's facing counted 0 south, 1 west,
 * 2 north, 3 east. A piece whose front faces {@code f} is placed by a player facing {@code f + 2}, so its angle is
 * {@code (f + 3) % 4}.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Furniture); D259; D262
// Decompiled: BiblioCraft[v1.11.7][MC1.7.10].jar (GTNH 2.9.0-RC-2; no 1.7.10 source exists, and its copyright.txt
// allows decompiling for private use). Inferred from decompiled bytecode, unverified, and may change when the pack
// updates BiblioCraft.
// Facts only, no code: the registry names in jds.bibliocraft.blocks.BlockLoader, the metas in the BlockItem*.subName
// arrays (0 oak, 1 spruce, 2 birch, 3 jungle, 4 acacia, 5 dark oak;
// typewriter metas are dye colors, 3 black), the angle rule in each block's onBlockPlacedBy, the armor stand's meta
// (angle below, angle + 4 above), and these tile-entity NBT keys: seat "Angle", "hasBack", "backWoodType"; table
// "leg1".."leg4", "top1".."top4", "monoleg" (a lone table: no corner legs, one center leg, all four tops); desk
// "deskAngle"; typewriter and clipboard "angle"; bookcase "bookcaseAngle"; shelf "genericShelfAngle"; potion shelf
// "potionshelfAngle"; tool rack "rackAngle".
// Source: the same structure in the author's MIT-licensed 1.12 code (not the 1.7.10 code; names and keys there can
// differ, so the jar above is the authority):
// https://github.com/Nuchaz/BiblioCraft-Source/blob/ec4b1dc1d009b0640be225fae5e226035f205506/src/main/java/jds/bibliocraft/BlockLoader.java
// https://github.com/Nuchaz/BiblioCraft-Source/blob/ec4b1dc1d009b0640be225fae5e226035f205506/src/main/java/jds/bibliocraft/tileentities/TileEntitySeat.java
public final class BiblioFurniture {

    /** BiblioCraft's mod id. */
    public static final String MOD_ID = "BiblioCraft";

    private static final ConcurrentMap<String, Object> BLOCKS = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    private BiblioFurniture() {}

    /** @return the BiblioCraft registry name of a piece, or null for a piece BiblioCraft does not supply */
    static String registryName(FurniturePiece p) {
        switch (p) {
            case CHAIR:
            case STOOL:
                return "BiblioSeats";
            case WOOD_TABLE:
                return "BiblioTable";
            case DESK:
                return "BiblioDesk";
            case TYPEWRITER:
                return "BiblioTypewriter";
            case CLIPBOARD:
                return "BiblioClipboard";
            case BOOKCASE:
                return "Bibliotheca";
            case WALL_SHELF:
                return "BiblioShelf";
            case MEDICINE_SHELF:
                return "BiblioPotionShelf";
            case TOOL_RACK:
                return "BiblioRack";
            case ARMOR_STAND:
            case ARMOR_STAND_TOP:
                return "Armor Stand";
            default:
                return null; // the locker is GregTech's, the server rack the style's console
        }
    }

    /** @return BiblioCraft's stored angle for a piece whose front faces {@code facing} (0 south ... 3 east) */
    static int angle(int facing) {
        return (facing + 3) & 3;
    }

    /** The typewriter's color: black. */
    static final int TYPEWRITER_BLACK = 3;

    /** @return the block meta of a piece */
    static int meta(FurniturePiece p, int wood, int facing) {
        switch (p) {
            case TYPEWRITER:
                return TYPEWRITER_BLACK;
            case CLIPBOARD:
                return 0;
            case ARMOR_STAND:
                return angle(facing);
            case ARMOR_STAND_TOP:
                return angle(facing) + 4;
            default:
                return wood;
        }
    }

    /**
     * Writes the facing (and a chair's back, a lone table's legs) into a piece's NBT, which starts as the tile's own
     * defaults.
     */
    static void applyNbt(FurniturePiece p, int wood, int facing, NBTTagCompound t) {
        int angle = angle(facing);
        switch (p) {
            case CHAIR:
            case STOOL:
                t.setInteger("Angle", angle);
                t.setInteger("hasBack", p == FurniturePiece.CHAIR ? 1 : 0);
                t.setInteger("backWoodType", wood);
                break;
            case WOOD_TABLE:
                for (int i = 1; i <= 4; i++) {
                    t.setBoolean("leg" + i, false);
                    t.setBoolean("top" + i, true);
                }
                t.setBoolean("monoleg", true);
                break;
            case DESK:
                t.setInteger("deskAngle", angle);
                break;
            case TYPEWRITER:
            case CLIPBOARD:
                t.setInteger("angle", angle);
                break;
            case BOOKCASE:
                t.setInteger("bookcaseAngle", angle);
                break;
            case WALL_SHELF:
                t.setInteger("genericShelfAngle", angle);
                break;
            case MEDICINE_SHELF:
                t.setInteger("potionshelfAngle", angle);
                break;
            case TOOL_RACK:
                t.setInteger("rackAngle", angle);
                break;
            default:
                break; // the armor stand keeps its facing in its meta
        }
    }

    /** @return whether BiblioCraft is loaded */
    public static boolean loaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /** @return the block registered under {@code name}, or null (cached) */
    static Block block(String name) {
        Object o = BLOCKS.get(name);
        if (o == null) {
            Block b = GameRegistry.findBlock(MOD_ID, name);
            BLOCKS.put(name, b == null ? NONE : b);
            return b;
        }
        return o == NONE ? null : (Block) o;
    }

    /**
     * Sets a BiblioCraft piece through the sink and turns it.
     *
     * @return false when BiblioCraft does not supply this piece in this pack (the caller falls back); true otherwise,
     *         also when the cell lies outside the window and nothing was written
     */
    public static boolean place(World world, WindowedSink sink, FurniturePiece p, int wood, int facing, int x, int y,
        int z) {
        if (!loaded()) return false;
        String name = registryName(p);
        Block b = name == null ? null : block(name);
        if (b == null) return false;
        if (!sink.setBlock(x, y, z, b, meta(p, wood, facing)) || world == null) return true;
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) return true;
        NBTTagCompound t = new NBTTagCompound();
        te.writeToNBT(t);
        applyNbt(p, wood, facing, t);
        te.readFromNBT(t);
        te.markDirty();
        return true;
    }
}
