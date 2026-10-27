package com.gregguns.icbm.warhead.effects;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.explosive.BlastContext;
import com.gregguns.icbm.explosive.BlastEffect;
import com.gregguns.icbm.warhead.EntityFlyingBlock;

/**
 * The anti-gravitational warhead (D80, D10). For {@link #LIFT_TICKS} ticks it lifts living things near the center, and,
 * when terrain damage is allowed for T3, it turns solid blocks of the blast sphere into {@link EntityFlyingBlock}s,
 * top layer first. The bounds:
 * <ul>
 * <li>At most {@code icbm.antigravMaxBlocks} blocks per blast (default 512) and {@link #PER_TICK} per tick; the layer
 * scan stops when the shared tick budget is spent.</li>
 * <li>Never a block with a tile entity (no machine or inventory is lifted, copied or lost), never fluid, bedrock or
 * anything harder than {@link #MAX_HARDNESS}.</li>
 * <li>Each layer's candidates go through the claim and protection veto ({@code Detonate}, D52) once, and the survivors
 * wait in a saved queue; the layer cursor, the queue and the count are in the blast's state, so a reload resumes
 * exactly.</li>
 * </ul>
 * Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md (Anti-gravitational)
final class AntiGravEffect implements BlastEffect {

    /** The lifting phase, ticks (30 s). The blocks then hover on their own and fall. */
    static final int LIFT_TICKS = 600;
    /** Most blocks turned into entities in one tick. */
    static final int PER_TICK = 8;
    /** Hardest block that is lifted; obsidian-hard blocks stay. */
    static final float MAX_HARDNESS = 30.0F;
    /** Most living entities lifted in one pass. */
    static final int MAX_LIFTED = 128;
    /** Entities are lifted only below this many blocks above the center. */
    static final int ENTITY_CEILING = 20;

    @Override
    public void onDetonate(BlastContext ctx) {
        ctx.state()
            .setInteger("layer", (int) Math.floor(ctx.radius()));
        ctx.state()
            .setInteger("lifted", 0);
        ctx.state()
            .setIntArray("queue", new int[0]);
        ctx.state()
            .setInteger("next", 0);
    }

    @Override
    public boolean onTick(BlastContext ctx, int age) {
        World world = ctx.world();
        if (world.isRemote || age >= LIFT_TICKS) {
            return false;
        }
        if (ctx.breaksTerrain()) {
            liftBlocks(ctx);
        }
        if (age % 2 == 0) {
            liftEntities(ctx);
        }
        return true;
    }

    private void liftBlocks(BlastContext ctx) {
        NBTTagCompound state = ctx.state();
        World world = ctx.world();
        int cap = IcbmConfig.antigravMaxBlocks;
        int lifted = state.getInteger("lifted");
        int spawned = 0;
        int[] queue = state.getIntArray("queue");
        int next = state.getInteger("next");
        int layer = state.getInteger("layer");
        double radius = ctx.radius();
        while (spawned < PER_TICK && lifted < cap) {
            if (next * 3 >= queue.length) {
                // The queue is spent: gather the next layer, once, through the veto.
                if (layer < -(int) Math.floor(radius) || ctx.budget()
                    .exhausted()) {
                    break;
                }
                queue = gatherLayer(ctx, layer);
                layer--;
                next = 0;
                continue;
            }
            int x = queue[next * 3];
            int y = queue[next * 3 + 1];
            int z = queue[next * 3 + 2];
            next++;
            if (!world.blockExists(x, y, z) || !liftable(world, x, y, z)) {
                continue;
            }
            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            if (!world.setBlock(x, y, z, Blocks.air, 0, 3)) {
                continue;
            }
            double angle = world.rand.nextDouble() * Math.PI * 2.0;
            double drift = 0.02 + world.rand.nextDouble() * 0.06;
            double riseTo = Math.min(250.0, y + 12 + world.rand.nextInt(28));
            world.spawnEntityInWorld(
                EntityFlyingBlock.lift(
                    world,
                    x + 0.5,
                    y,
                    z + 0.5,
                    block,
                    meta,
                    riseTo,
                    400 + world.rand.nextInt(200),
                    Math.cos(angle) * drift,
                    Math.sin(angle) * drift));
            lifted++;
            spawned++;
        }
        state.setInteger("lifted", lifted);
        state.setInteger("layer", layer);
        state.setInteger("next", next);
        state.setIntArray("queue", queue);
    }

    /** The vetted, liftable cells of one horizontal layer of the sphere, as x y z triples. */
    private int[] gatherLayer(BlastContext ctx, int dy) {
        World world = ctx.world();
        int cx = (int) Math.floor(ctx.x());
        int cy = (int) Math.floor(ctx.y());
        int cz = (int) Math.floor(ctx.z());
        int y = cy + dy;
        double r2 = ctx.radius() * ctx.radius() - (double) dy * dy;
        List<int[]> cells = new ArrayList<>();
        if (r2 >= 0.0 && y > 0 && y < 255) {
            int w = (int) Math.floor(Math.sqrt(r2));
            for (int dx = -w; dx <= w; dx++) {
                for (int dz = -w; dz <= w; dz++) {
                    if ((double) dx * dx + (double) dz * dz > r2) {
                        continue;
                    }
                    int x = cx + dx;
                    int z = cz + dz;
                    if (world.blockExists(x, y, z) && liftable(world, x, y, z) && Effects.mayChange(ctx, x, y, z)) {
                        cells.add(new int[] { x, y, z });
                    }
                }
            }
        }
        List<int[]> kept = Effects.vetted(ctx, cells);
        int[] out = new int[kept.size() * 3];
        for (int i = 0; i < kept.size(); i++) {
            System.arraycopy(kept.get(i), 0, out, i * 3, 3);
        }
        return out;
    }

    /** A plain solid block: not air, fluid or unbreakable, not too hard, and with no tile entity. */
    static boolean liftable(World world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        if (b.isAir(world, x, y, z)) {
            return false;
        }
        Material m = b.getMaterial();
        if (m.isLiquid() || b == Blocks.bedrock) {
            return false;
        }
        int meta = world.getBlockMetadata(x, y, z);
        if (b.hasTileEntity(meta) || world.getTileEntity(x, y, z) != null) {
            return false;
        }
        float h = b.getBlockHardness(world, x, y, z);
        return h >= 0.0F && h <= MAX_HARDNESS;
    }

    @SuppressWarnings("unchecked")
    private void liftEntities(BlastContext ctx) {
        double r = ctx.radius();
        List<EntityLivingBase> near = Effects.living(ctx.world(), ctx.x(), ctx.y(), ctx.z(), r);
        int n = 0;
        for (EntityLivingBase e : near) {
            if (++n > MAX_LIFTED) {
                break;
            }
            double dx = e.posX - ctx.x();
            double dz = e.posZ - ctx.z();
            if (dx * dx + dz * dz > r * r || e.posY > ctx.y() + ENTITY_CEILING) {
                continue;
            }
            if (e instanceof EntityPlayer && ((EntityPlayer) e).capabilities.isFlying) {
                continue;
            }
            if (e.motionY < 0.4) {
                e.motionY += 0.3; // applied every other tick: ICBM's +0.15 a tick, below 0.4
                e.fallDistance = 0.0F;
                e.velocityChanged = true;
            }
        }
    }
}
