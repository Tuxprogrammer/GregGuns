package com.gregguns.worldgen;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Real-world destination for a {@link ClippingSink}. Flag 2 sends the client update without neighbor updates, which
 * keeps a structure write from triggering block updates that read (and so may load) neighboring chunks.
 */
public final class WorldTarget implements WindowedSink.Target {

    private final World world;

    public WorldTarget(World world) {
        this.world = world;
    }

    @Override
    public void setBlock(int x, int y, int z, Block block, int meta) {
        world.setBlock(x, y, z, block, meta, 2);
    }

    @Override
    public void setTile(int x, int y, int z, TileEntity tile) {
        world.setTileEntity(x, y, z, tile);
    }

    @Override
    public void spawn(Entity entity, double x, double y, double z) {
        // A hanging entity (item frame) sets its own position and thin bounding box from its wall and direction;
        // setPosition would replace the box with a cube, and the frame would pop off at its first surface check.
        if (!(entity instanceof net.minecraft.entity.EntityHanging)) {
            entity.setPosition(x, y, z);
        }
        world.spawnEntityInWorld(entity);
    }
}
