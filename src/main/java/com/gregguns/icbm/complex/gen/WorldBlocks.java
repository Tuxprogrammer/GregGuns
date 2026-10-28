package com.gregguns.icbm.complex.gen;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.gregguns.block.GregGunsCasings;
import com.gregguns.icbm.compat.GalacticraftParts;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.wall.BlockComplexWall;
import com.gregguns.worldgen.layout.ResolvedBlock;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;

/**
 * The real blocks of the complex: GT Light and Dark Concrete for walls and the silo (the silo's own accepted finishes),
 * clear glass, Gantry Beam, a Galacticraft launch pad block for the shaft floor. Variants are chosen from the position,
 * so they never depend on window order.
 */
// Source: block choices follow SiloShape and the structure definition in MTEMissileSilo (concrete 'C'/'F', glass 'G',
// Gantry Beam 'B', pad 'P').
public final class WorldBlocks implements BlockResolver {

    private static final int[] SILO_METAS = { 8, 11, 8, 12 };

    private static ResolvedBlock concrete(int[] metas, int x, int y, int z) {
        int h = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791);
        return new ResolvedBlock(GregTechAPI.sBlockConcretes, metas[Math.floorMod(h, metas.length)], false);
    }

    private static ResolvedBlock glass() {
        Block b = GameRegistry.findBlock("TConstruct", "GlassBlock");
        if (b != null) return new ResolvedBlock(b, 0, false);
        b = GameRegistry.findBlock("EnderIO", "blockFusedQuartz");
        if (b != null) return new ResolvedBlock(b, 1, false);
        return new ResolvedBlock(Blocks.glass, 0, true);
    }

    @Override
    public ResolvedBlock resolve(byte cell, int x, int y, int z) {
        switch (cell) {
            case Cell.WALL:
                // The complex wall block (D205): Bastion Plating's look, joined by the same connected textures, but
                // not a GT casing, so placing tens of thousands of it starts no machine-update search. The silo and
                // its shaft stay GT concrete, the silo's own accepted finish (SiloConcrete.acceptsMeta).
                return new ResolvedBlock(BlockComplexWall.instance(), 0, false);
            case Cell.AIR:
                return new ResolvedBlock(Blocks.air, 0, false);
            case Cell.RAIL_NS:
                return new ResolvedBlock(Blocks.rail, 0, false);
            case Cell.RAIL_EW:
                return new ResolvedBlock(Blocks.rail, 1, false);
            case Cell.SILO_CONCRETE:
                return concrete(SILO_METAS, x, y, z);
            case Cell.SILO_GLASS:
                return glass();
            case Cell.SILO_BEAM:
                return new ResolvedBlock(GregGunsCasings.INSTANCE, GregGunsCasings.GANTRY_BEAM, false);
            case Cell.SILO_PAD: {
                Block pad = GalacticraftParts.findBlock(GalacticraftParts.PAD);
                return pad == null ? concrete(SILO_METAS, x, y, z) : new ResolvedBlock(pad, 0, false);
            }
            case Cell.LADDER:
                return new ResolvedBlock(Blocks.ladder, 2, false);
            default:
                return null;
        }
    }

    @Override
    public ResolvedBlock ladder(int meta, int x, int y, int z) {
        return new ResolvedBlock(Blocks.ladder, meta, false);
    }

    @Override
    public ResolvedBlock named(String what, int x, int y, int z) {
        switch (what) {
            case "ring":
            case "cap":
                return concrete(SILO_METAS, x, y, z);
            case "ladder":
                return new ResolvedBlock(Blocks.ladder, 2, false);
            case "backing":
                // Plain stone behind the escape ladder, so it still has something to cling to where the shaft
                // crosses a cave.
                return new ResolvedBlock(Blocks.stone, 0, false);
            case "trapdoor":
                // Bit 3 is the top half: a closed trapdoor in the top half of the surface block lies flush with the
                // terrain around it.
                return new ResolvedBlock(Blocks.trapdoor, 8, false);
            case "hutWall":
                return new ResolvedBlock(GregGunsCasings.INSTANCE, GregGunsCasings.WORKS_CASING, false);
            case "hutRoof":
                return new ResolvedBlock(GregGunsCasings.INSTANCE, GregGunsCasings.GANTRY_CASING, false);
            case "mast":
                return new ResolvedBlock(Blocks.iron_bars, 0, false);
            default:
                return null;
        }
    }

    @Override
    public Block air() {
        return Blocks.air;
    }
}
