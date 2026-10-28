package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.FurniturePiece;

/** D259, D262: the BiblioCraft facts the furniture is placed with, and the vanilla fallback of a seat. */
class BiblioFurnitureTest {

    @Test
    void everyBiblioPieceHasARegistryName() {
        for (FurniturePiece p : FurniturePiece.values()) {
            String n = BiblioFurniture.registryName(p);
            boolean ours = p == FurniturePiece.LOCKER || p == FurniturePiece.SERVER_RACK;
            assertEquals(ours, n == null, p.name());
        }
        assertEquals("BiblioSeats", BiblioFurniture.registryName(FurniturePiece.CHAIR));
        assertEquals("Armor Stand", BiblioFurniture.registryName(FurniturePiece.ARMOR_STAND_TOP));
    }

    @Test
    void anglesTurnWithTheFacingAndStayInRange() {
        Set<Integer> seen = new HashSet<>();
        for (int f = 0; f < 4; f++) {
            int a = BiblioFurniture.angle(f);
            assertTrue(a >= 0 && a < 4);
            seen.add(a);
            assertEquals(a, BiblioFurniture.meta(FurniturePiece.ARMOR_STAND, 1, f));
            assertEquals(a + 4, BiblioFurniture.meta(FurniturePiece.ARMOR_STAND_TOP, 1, f));
        }
        assertEquals(4, seen.size());
        // Wood pieces carry the wood in their meta.
        assertEquals(5, BiblioFurniture.meta(FurniturePiece.CHAIR, 5, 2));
        assertEquals(BiblioFurniture.TYPEWRITER_BLACK, BiblioFurniture.meta(FurniturePiece.TYPEWRITER, 5, 2));
    }

    @Test
    void nbtSetsTheFacingKeyOfEachPiece() {
        NBTTagCompound t = new NBTTagCompound();
        BiblioFurniture.applyNbt(FurniturePiece.CHAIR, 2, 1, t);
        assertEquals(BiblioFurniture.angle(1), t.getInteger("Angle"));
        assertEquals(1, t.getInteger("hasBack"));
        assertEquals(2, t.getInteger("backWoodType"));
        t = new NBTTagCompound();
        BiblioFurniture.applyNbt(FurniturePiece.STOOL, 2, 1, t);
        assertEquals(0, t.getInteger("hasBack"));
        t = new NBTTagCompound();
        BiblioFurniture.applyNbt(FurniturePiece.WOOD_TABLE, 1, 0, t);
        assertTrue(t.getBoolean("monoleg") && t.getBoolean("top3") && !t.getBoolean("leg2"));
        String[][] keys = { { "DESK", "deskAngle" }, { "TYPEWRITER", "angle" }, { "CLIPBOARD", "angle" },
            { "BOOKCASE", "bookcaseAngle" }, { "WALL_SHELF", "genericShelfAngle" },
            { "MEDICINE_SHELF", "potionshelfAngle" }, { "TOOL_RACK", "rackAngle" } };
        for (String[] k : keys) {
            t = new NBTTagCompound();
            BiblioFurniture.applyNbt(FurniturePiece.valueOf(k[0]), 1, 3, t);
            assertTrue(t.hasKey(k[1]), k[0]);
            assertEquals(BiblioFurniture.angle(3), t.getInteger(k[1]), k[0]);
        }
    }

    @Test
    void aStairChairHasItsBackOppositeItsFacing() {
        // Stair metas: the tall half on the east (0), west (1), south (2), north (3).
        assertEquals(3, FurnitureFiller.stairMeta(0)); // faces south, back north
        assertEquals(0, FurnitureFiller.stairMeta(1)); // faces west, back east
        assertEquals(2, FurnitureFiller.stairMeta(2)); // faces north, back south
        assertEquals(1, FurnitureFiller.stairMeta(3)); // faces east, back west
    }
}
