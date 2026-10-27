package com.gregguns.icbm.missile;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.gregguns.core.testing.ScenarioContext;

/**
 * A context that always uses the same origin, so scenarios with different names (each has its own grid slot) share one
 * arena in the I4 client session. Dev only; it does no cleanup bookkeeping.
 */
public final class I4Anchor implements ScenarioContext {

    private static int[] base;

    private final ScenarioContext ctx;
    private final int ox;
    private final int oz;

    private I4Anchor(ScenarioContext ctx, int ox, int oz) {
        this.ctx = ctx;
        this.ox = ox;
        this.oz = oz;
    }

    /**
     * @param ctx    the harness context of the running scenario
     * @param shiftX offset of this arena from the shared base, in X
     * @param shiftZ offset in Z
     * @return a context whose origin is the shared base plus the shift
     */
    public static synchronized ScenarioContext of(ScenarioContext ctx, int shiftX, int shiftZ) {
        if (base == null) {
            base = new int[] { ctx.originX(), ctx.originZ() };
        }
        return new I4Anchor(ctx, base[0] + shiftX, base[1] + shiftZ);
    }

    @Override
    public World world() {
        return ctx.world();
    }

    @Override
    public int originX() {
        return ox;
    }

    @Override
    public int originY() {
        return ctx.originY();
    }

    @Override
    public int originZ() {
        return oz;
    }

    @Override
    public EntityPlayer player() {
        return ctx.player();
    }

    @Override
    public Random random() {
        return ctx.random();
    }

    @Override
    public <T extends Entity> T spawn(T entity) {
        return ctx.spawn(entity);
    }

    @Override
    public void despawn(Entity entity) {
        ctx.despawn(entity);
    }

    @Override
    public void clearArea(int radius, int height) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    setBlock(dx, dy, dz, Blocks.air, 0);
                }
            }
        }
    }

    @Override
    public void setBlock(int dx, int dy, int dz, Block block, int meta) {
        int x = ox + dx;
        int z = oz + dz;
        ctx.world()
            .getChunkProvider()
            .loadChunk(x >> 4, z >> 4);
        ctx.world()
            .setBlock(x, originY() + dy, z, block, meta, 2);
    }

    @Override
    public void cleanup() {}
}
