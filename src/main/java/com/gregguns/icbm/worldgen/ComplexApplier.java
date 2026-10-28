package com.gregguns.icbm.worldgen;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.GregGuns;
import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.control.ControlMachines;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.spawner.HoleKind;
import com.gregguns.worldgen.spawner.HoleSpawnerRegistration;
import com.gregguns.worldgen.spawner.TileHoleSpawner;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

/**
 * Applies {@link Placement}s to the world through a {@link WindowedSink}, so every write is clipped to the populate
 * window (GGS-11-08). Complex-local (x, y, z) maps to the world by adding the origin of the complex's level floor.
 * A placement whose block or item does not exist this run (a GT machine name GT lacks, Galacticraft absent) is skipped
 * and logged once, never replaced by a guess. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Placements)
public final class ComplexApplier {

    private final WindowedSink sink;
    private final int ox;
    private final int oy;
    private final int oz;
    private final long worldSeed;
    private final int cellX;
    private final int cellZ;
    private final int ceiling;
    private final net.minecraft.world.World world;

    /**
     * @param sink      the clipped sink
     * @param ox        world X of complex-local x 0
     * @param oy        world Y of complex-local y 0 (the level floor)
     * @param oz        world Z of complex-local z 0
     * @param worldSeed the world seed
     * @param cellX     the complex's grid cell X
     * @param cellZ     the complex's grid cell Z
     * @param ceiling   the complex's ceiling, handed to the spawners
     */
    public ComplexApplier(WindowedSink sink, int ox, int oy, int oz, long worldSeed, int cellX, int cellZ,
        int ceiling) {
        this(sink, null, ox, oy, oz, worldSeed, cellX, cellZ, ceiling);
    }

    /**
     * As above, with the world the item frames of a {@link Placement.Kind#FRAME_WALL} are created in (an entity needs
     * one). Without a world the frame walls are skipped.
     *
     * @param world the world being populated, or null
     */
    public ComplexApplier(WindowedSink sink, net.minecraft.world.World world, int ox, int oy, int oz, long worldSeed,
        int cellX, int cellZ, int ceiling) {
        this.world = world;
        this.sink = sink;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.worldSeed = worldSeed;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.ceiling = ceiling;
    }

    /** @param p a placement */
    public void apply(Placement p) {
        int x = ox + p.x();
        int y = oy + p.y();
        int z = oz + p.z();
        switch (p.kind()) {
            case AIR:
                sink.setBlock(x, y, z, Blocks.air, 0);
                break;
            case RUBBLE:
                sink.setBlock(x, y, z, rubble(p.x(), p.y(), p.z()), 0);
                break;
            case WATER:
                sink.setBlock(x, y, z, Blocks.water, 0);
                break;
            case LIGHT:
                sink.setBlock(x, y, z, Blocks.glowstone, 0);
                break;
            case CHEST:
                if (sink.setBlock(x, y, z, Blocks.chest, 2)) {
                    sink.setTile(x, y, z, SiloChests.filledChest(p, worldSeed, cellX, cellZ));
                }
                break;
            case SPAWNER:
                spawner(p.spawner(), x, y, z);
                break;
            case MACHINE:
                machine(p, x, y, z);
                break;
            case COLLAPSE:
                collapse(p.box());
                break;
            case FRAME_WALL:
                frameWall(p);
                break;
            default:
                break;
        }
    }

    /**
     * Hangs the frames of an armory wall: one every second cell from the mark's cell, along +X or +Z (the wall's
     * axis), each holding a gun from {@link SiloFrames}. A frame whose gun does not resolve this run stays empty
     * rather than showing a guess.
     */
    private void frameWall(Placement p) {
        if (world == null) {
            return;
        }
        boolean alongZ = p.facing() == 1 || p.facing() == 3;
        for (int i = 0; i < p.extent(); i++) {
            int lx = p.x() + (alongZ ? 0 : 2 * i);
            int lz = p.z() + (alongZ ? 2 * i : 0);
            int x = ox + lx;
            int z = oz + lz;
            if (!sink.window()
                .contains(x, z)) {
                continue;
            }
            int y = oy + p.y();
            net.minecraft.entity.item.EntityItemFrame frame = new net.minecraft.entity.item.EntityItemFrame(
                world,
                x,
                y,
                z,
                p.facing());
            String ref = SiloFrames.gunFor(ceiling, worldSeed ^ (cellX * 31L + cellZ), lx, p.y(), lz, GUN);
            ItemStack gun = ref == null ? null : com.gregguns.worldgen.loot.LootCategories.stack(ref, 1);
            if (gun != null) {
                frame.setDisplayedItem(gun);
            }
            sink.spawn(frame, frame.posX, frame.posY, frame.posZ);
        }
    }

    private static final java.util.function.Predicate<String> GUN = ref -> {
        net.minecraft.item.Item it = com.gregguns.entity.npc.NpcItems.item(ref);
        return it instanceof com.gregguns.item.gun.ItemGun;
    };

    private static Block rubble(int x, int y, int z) {
        return ((x * 31 + y * 17 + z * 13) & 3) == 0 ? Blocks.gravel : Blocks.cobblestone;
    }

    private void spawner(HoleKind kind, int x, int y, int z) {
        Block b = HoleSpawnerRegistration.block(kind);
        if (b != null && sink.setBlock(x, y, z, b, 0)) {
            sink.setTile(x, y, z, new TileHoleSpawner(kind, ceiling));
        }
    }

    private void collapse(Box box) {
        for (int x = box.minX + 1; x < box.maxX - 1; x++) {
            for (int z = box.minZ + 1; z < box.maxZ - 1; z++) {
                for (int y = box.minY + 1; y < box.minY + 4 && y < box.maxY - 1; y++) {
                    long h = Mix.of(worldSeed ^ cellX * 31L + cellZ, 0xC011A, x * 4096L + z, y);
                    if (Mix.unit(h) < 0.55 - 0.12 * (y - box.minY - 1)) {
                        sink.setBlock(ox + x, oy + y, oz + z, rubble(x, y, z), 0);
                    }
                }
            }
        }
    }

    private void machine(Placement p, int x, int y, int z) {
        MachineRole role = p.role();
        if (role == MachineRole.NASA_WORKBENCH && !p.dead()) {
            placeNasaWorkbench(x, y, z);
            return;
        }
        int mteId;
        if (role == MachineRole.LAUNCH_CONTROL && !p.dead()) {
            mteId = ControlMachines.stack(1)
                .getItemDamage();
        } else if (role == MachineRole.MAGAZINE_CASING) {
            placeCasing(x, y, z);
            return;
        } else {
            MachinePicker.Pick pick = MachinePicker.pick(role, Math.max(1, p.tier()), p.dead());
            if (pick == null) {
                GregGuns.LOG.warn("No GT machine for {} at tier {}", role, p.tier());
                return;
            }
            mteId = pick.stack()
                .getItemDamage();
        }
        placeMte(x, y, z, mteId);
    }

    private void placeMte(int x, int y, int z, int mteId) {
        IMetaTileEntity proto = mteId >= 0 && mteId < GregTechAPI.METATILEENTITIES.length
            ? GregTechAPI.METATILEENTITIES[mteId]
            : null;
        if (proto == null) {
            return;
        }
        if (sink.setBlock(x, y, z, GregTechAPI.sBlockMachines, proto.getTileEntityBaseType())) {
            gregtech.api.metatileentity.BaseMetaTileEntity tile = new gregtech.api.metatileentity.BaseMetaTileEntity();
            tile.setInitialValuesAsNBT(null, (short) mteId);
            sink.setTile(x, y, z, tile);
        }
    }

    private void placeCasing(int x, int y, int z) {
        Block b = com.gregguns.icbm.warhead.WarheadBlocks.of(com.gregguns.icbm.warhead.WarheadCasing.T1);
        if (b != null && sink.setBlock(x, y, z, b, 0)) {
            sink.setTile(x, y, z, b.createTileEntity(null, 0));
        }
    }

    private void placeNasaWorkbench(int x, int y, int z) {
        Block b = SiloGc.nasaWorkbench();
        if (b == null) {
            return;
        }
        if (sink.setBlock(x, y, z, b, 0)) {
            TileEntity t = b.createTileEntity(null, 0);
            if (t != null) {
                sink.setTile(x, y, z, t);
            }
        }
    }

    /** @return one machine item for a placement, for scenarios that check what would be placed */
    public static ItemStack stackFor(Placement p) {
        MachinePicker.Pick pick = MachinePicker.pick(p.role(), Math.max(1, p.tier()), p.dead());
        return pick == null ? null : pick.stack();
    }
}
