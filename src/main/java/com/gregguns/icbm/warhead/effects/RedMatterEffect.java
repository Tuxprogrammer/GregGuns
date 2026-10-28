package com.gregguns.icbm.warhead.effects;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gregguns.combat.DamageSources;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.explosive.BlastContext;
import com.gregguns.icbm.explosive.BlastEffect;
import com.gregguns.icbm.fx.OrbBroadcast;
import com.gregguns.icbm.fx.OrbKind;
import com.gregguns.icbm.fx.OrbPlan;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * The red matter warhead (UHV, D10, D132): a ball of annihilation that lives for a bounded time. It is a blast effect,
 * not an entity, so the blast engine already saves it with the world and resumes it after a reload.
 *
 * <ul>
 * <li><b>Eat.</b> The ball starts at {@link RedMatterModel#START_RADIUS} and grows one block per pass up to the blast
 * radius. A pass walks the cells of its new shell with a cursor and removes the breakable, non-liquid blocks (claims
 * and the break guard decide first, through {@code ExplosionEvent.Detonate}). The walk is offered to the scheduler as
 * {@link #work}, so it shares the blast tick budget with every crater instead of running on its own.</li>
 * <li><b>Idle.</b> When the ball is full size and its last shell is done there is nothing left to trace; the body goes
 * idle and costs no scan work until it ends. Terrain damage off, or an unloaded neighborhood, therefore idles too.</li>
 * <li><b>Pull and kill.</b> Every tick, living entities inside the ball are annihilated (bosses included) and other
 * entities are removed; nearby entities are pulled in, with a speed cap and no division by a zero distance.</li>
 * <li><b>End.</b> After {@code icbm.redMatterLifetimeSeconds}, or after {@link RedMatterModel#IDLE_LINGER_TICKS}
 * idle, or when antimatter goes off within reach.</li>
 * </ul>
 *
 * It leaves no fallout (D21). The whole state is in {@link BlastContext#state()}.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/red-matter.md
final class RedMatterEffect implements BlastEffect {

    static final String INIT = "rmInit";
    static final String RADIUS = "rmRadius";
    static final String INNER = "rmInner";
    static final String CURSOR = "rmCursor";
    static final String MODE = "rmMode";
    static final String IDLE_SINCE = "rmIdleSince";
    static final String AGE = "rmAge";
    static final String EDITS = "rmEdits";
    static final String EATEN = "rmEaten";

    @Override
    public void onDetonate(BlastContext ctx) {
        World world = ctx.world();
        if (world.isRemote) {
            return;
        }
        init(ctx.state(), ctx);
        world.playSoundEffect(ctx.x(), ctx.y(), ctx.z(), "mob.wither.spawn", 4.0F, 0.5F);
    }

    /** Sets the first state once; a resumed blast keeps what it saved. */
    static void init(NBTTagCompound s, BlastContext ctx) {
        if (s.getBoolean(INIT)) {
            return;
        }
        int max = RedMatterModel.maxRadius(ctx.radius());
        s.setBoolean(INIT, true);
        s.setInteger(RADIUS, Math.min(RedMatterModel.START_RADIUS, max));
        s.setInteger(INNER, 0);
        s.setInteger(CURSOR, 0);
        s.setInteger(
            MODE,
            IcbmConfig.terrainDamage(
                ctx.explosive()
                    .tier()
                    .number()) ? RedMatterModel.Mode.SCAN.ordinal() : RedMatterModel.Mode.IDLE.ordinal());
        s.setInteger(IDLE_SINCE, 0);
        s.setInteger(AGE, 0);
        s.setInteger(EDITS, 0);
        s.setInteger(EATEN, 0);
    }

    @Override
    public boolean onTick(BlastContext ctx, int age) {
        World world = ctx.world();
        if (world.isRemote) {
            return false;
        }
        NBTTagCompound s = ctx.state();
        init(s, ctx);
        s.setInteger(AGE, age);
        s.setInteger(EDITS, 0);
        int lifetime = RedMatterModel.lifetimeTicks(IcbmConfig.redMatterLifetimeSeconds);
        if (age >= lifetime) {
            return false;
        }
        int radius = s.getInteger(RADIUS);
        if (isIdle(s) && s.getInteger(IDLE_SINCE) == 0) {
            s.setInteger(IDLE_SINCE, Math.max(1, age));
        }
        if (isIdle(s) && age - s.getInteger(IDLE_SINCE) >= RedMatterModel.IDLE_LINGER_TICKS) {
            return false;
        }
        reach(ctx, radius);
        if (age % RedMatterModel.ANNOUNCE_PERIOD == 0) {
            OrbBroadcast.send(
                world,
                OrbKind.RED_MATTER,
                ctx.x(),
                ctx.y(),
                ctx.z(),
                radius,
                OrbPlan.lease(lifetime - age, RedMatterModel.ANNOUNCE_PERIOD));
        }
        return true;
    }

    @Override
    public boolean wantsWork(BlastContext ctx) {
        NBTTagCompound s = ctx.state();
        return s.getBoolean(INIT) && !isIdle(s) && s.getInteger(EDITS) < RedMatterModel.EDITS_PER_TICK;
    }

    @Override
    public void work(BlastContext ctx) {
        NBTTagCompound s = ctx.state();
        World world = ctx.world();
        int radius = s.getInteger(RADIUS);
        int inner = s.getInteger(INNER);
        int cursor = s.getInteger(CURSOR);
        int total = RedMatterModel.cubeCells(radius);
        int cx = (int) Math.floor(ctx.x());
        int cy = (int) Math.floor(ctx.y());
        int cz = (int) Math.floor(ctx.z());
        int room = Math.min(RedMatterModel.EDITS_PER_SLICE, RedMatterModel.EDITS_PER_TICK - s.getInteger(EDITS));
        List<int[]> batch = new ArrayList<>();
        int[] d = new int[3];
        int weight = 0;
        int visited = 0;
        while (visited < RedMatterModel.CELLS_PER_SLICE && cursor < total && weight < room) {
            RedMatterModel.decodeInto(cursor, radius, d);
            cursor++;
            visited++;
            if (!RedMatterModel.inShell(d[0], d[1], d[2], inner, radius)) {
                continue;
            }
            int x = cx + d[0];
            int y = cy + d[1];
            int z = cz + d[2];
            // Never load a chunk to eat a block in it (ICBM-02-95).
            if (y < 1 || y > 255 || !world.blockExists(x, y, z)) {
                continue;
            }
            Block b = world.getBlock(x, y, z);
            if (b.isAir(world, x, y, z) || b.getMaterial()
                .isLiquid() || b.getBlockHardness(world, x, y, z) < 0.0F || !Effects.mayChange(ctx, x, y, z)) {
                continue;
            }
            batch.add(new int[] { x, y, z });
            // A GT machine can explode in a chain when it breaks (BlockMachines.onBlockExploded), so it weighs more
            // against the slice's edit room; the walk then stops sooner and the scheduler checks the budget again.
            weight += world.getTileEntity(x, y, z) instanceof IGregTechTileEntity ? RedMatterModel.MACHINE_EDIT_WEIGHT
                : 1;
        }
        if (!batch.isEmpty()) {
            List<int[]> kept = Effects.vetted(ctx, batch);
            Effects.breakVetted(ctx, kept);
            s.setInteger(EDITS, s.getInteger(EDITS) + weight);
            s.setInteger(EATEN, s.getInteger(EATEN) + kept.size());
        }
        s.setInteger(CURSOR, cursor);
        if (cursor >= total) {
            endPass(s, radius, RedMatterModel.maxRadius(ctx.radius()));
        }
    }

    /** The shell is done: the next pass is one block wider, or the full-size body idles. */
    static void endPass(NBTTagCompound s, int radius, int max) {
        int next = RedMatterModel.nextRadius(radius, max);
        s.setInteger(INNER, radius);
        s.setInteger(CURSOR, 0);
        if (next < 0) {
            s.setInteger(MODE, RedMatterModel.Mode.IDLE.ordinal());
        } else {
            s.setInteger(RADIUS, next);
        }
    }

    static boolean isIdle(NBTTagCompound s) {
        return s.getInteger(MODE) == RedMatterModel.Mode.IDLE.ordinal();
    }

    /** Annihilates what the ball holds and pulls what is near, a bounded number of entities a tick. */
    @SuppressWarnings("unchecked")
    private static void reach(BlastContext ctx, int radius) {
        World world = ctx.world();
        double range = RedMatterModel.pullRange(radius);
        double kill = RedMatterModel.killRadius(radius);
        List<Entity> near = world.getEntitiesWithinAABB(
            Entity.class,
            AxisAlignedBB.getBoundingBox(
                ctx.x() - range,
                ctx.y() - range,
                ctx.z() - range,
                ctx.x() + range,
                ctx.y() + range,
                ctx.z() + range));
        int seen = 0;
        for (Entity e : near) {
            if (e.isDead) {
                continue;
            }
            if (++seen > RedMatterModel.ENTITY_CAP) {
                break;
            }
            if (e instanceof EntityPlayer && ((EntityPlayer) e).capabilities.disableDamage) {
                continue;
            }
            double dx = ctx.x() - e.posX;
            double dy = ctx.y() - (e.posY + e.height / 2.0);
            double dz = ctx.z() - e.posZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= kill) {
                if (e instanceof EntityLivingBase) {
                    DamageSources.annihilateLiving((EntityLivingBase) e, ctx.source(), true);
                } else if (!(e instanceof EntityPlayer)) {
                    e.setDead();
                }
                continue;
            }
            double speed = RedMatterModel.pullSpeed(dist, radius);
            if (speed > 0.0) {
                e.motionX += dx / dist * speed;
                e.motionY += dy / dist * speed;
                e.motionZ += dz / dist * speed;
                double k = RedMatterModel.speedClamp(e.motionX, e.motionY, e.motionZ);
                e.motionX *= k;
                e.motionY *= k;
                e.motionZ *= k;
                e.velocityChanged = true;
            }
        }
    }

    @Override
    public void onFinish(BlastContext ctx) {
        // The sound is the body's own end; a body that antimatter cancels ends under the antimatter flash instead.
        if (ctx.completed() && !ctx.world().isRemote) {
            ctx.world()
                .playSoundEffect(ctx.x(), ctx.y(), ctx.z(), "random.explode", 3.0F, 0.4F);
        }
    }
}
