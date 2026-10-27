package com.gregguns.icbm.warhead.effects;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.explosive.BlastContext;
import com.gregguns.icbm.explosive.BlastEffect;

/**
 * The ender warhead (D80, D10): for {@link EnderRules#DURATION_TICKS} ticks it pulls living things toward the center,
 * and what comes within {@link EnderRules#TELEPORT_RANGE} blocks is sent to one landing spot chosen when it goes off.
 * The rules, all of them bounds:
 * <ul>
 * <li>Same dimension only. The landing spot is chosen at detonation, 40 to 128 blocks from the center on each axis,
 * on a column that is <b>already loaded</b> and has solid dry ground under two free blocks; no chunk is ever loaded
 * for it, and the pull stops if its chunk is unloaded later. With no safe spot after 16 attempts the blast only
 * pulls.</li>
 * <li>Players and mobs both go; bosses (anything with a boss bar) are pulled but never teleported; riders are
 * dismounted first; the landing resets fall distance; {@code EnderTeleportEvent} may cancel each teleport.</li>
 * <li>At most {@code icbm.enderMaxTeleports} entities per blast, and at most {@link EnderRules#MAX_PULLED} pulled in
 * a tick.</li>
 * </ul>
 * The target and the count live in the blast's saved state, so a reload keeps the same landing spot. It changes no
 * block and so ignores the terrain switch. Server side. ICBM's eight endermen at the end are dropped.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md (Ender)
final class EnderEffect implements BlastEffect {

    /** Test hook: the next blasts land here instead of at a random spot; null for normal play. */
    static volatile int[] forced;
    /** Entity data key: the world time until which a sent entity is left alone. */
    private static final String SENT_KEY = "ggEnderSentUntil";
    /** Ticks a sent entity is immune to the pull (a blast lasts 160). */
    private static final int SENT_IMMUNITY = 200;

    @Override
    public void onDetonate(BlastContext ctx) {
        World world = ctx.world();
        if (world.isRemote) {
            return;
        }
        NBTTagCompound state = ctx.state();
        int cx = (int) Math.floor(ctx.x());
        int cz = (int) Math.floor(ctx.z());
        state.setBoolean("has", false);
        int[] force = forced;
        if (force != null) {
            state.setBoolean("has", true);
            state.setInteger("tx", force[0]);
            state.setInteger("ty", force[1]);
            state.setInteger("tz", force[2]);
        }
        for (int i = 0; force == null && i < EnderRules.TARGET_ATTEMPTS; i++) {
            int tx = cx + EnderRules.offset(world.rand.nextDouble());
            int tz = cz + EnderRules.offset(world.rand.nextDouble());
            int ty = safeFloor(world, tx, tz);
            if (ty > 0) {
                state.setBoolean("has", true);
                state.setInteger("tx", tx);
                state.setInteger("ty", ty);
                state.setInteger("tz", tz);
                break;
            }
        }
        world.playSoundEffect(ctx.x(), ctx.y(), ctx.z(), "portal.portal", 2.0F, 0.8F);
    }

    /**
     * @return the Y of the first free cell above a safe floor in the column, or -1 when the chunk is not loaded or the
     *         column has no safe spot
     */
    static int safeFloor(World world, int x, int z) {
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) {
            return -1;
        }
        int top = Math.min(250, world.getHeightValue(x, z));
        for (int y = top; y > 1; y--) {
            Block floor = world.getBlock(x, y - 1, z);
            Material m = floor.getMaterial();
            if (m.isSolid() && !m.isLiquid()
                && m != Material.lava
                && m != Material.cactus
                && m != Material.fire
                && world.isAirBlock(x, y, z)
                && world.isAirBlock(x, y + 1, z)) {
                return y;
            }
        }
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onTick(BlastContext ctx, int age) {
        World world = ctx.world();
        NBTTagCompound state = ctx.state();
        if (world.isRemote || age >= EnderRules.DURATION_TICKS || !state.getBoolean("has")) {
            return false;
        }
        int tx = state.getInteger("tx");
        int tz = state.getInteger("tz");
        if (!world.getChunkProvider()
            .chunkExists(tx >> 4, tz >> 4)) {
            return false; // the landing chunk went away: never load it, stop here
        }
        if (age % 20 == 0) {
            world.playSoundEffect(
                ctx.x(),
                ctx.y(),
                ctx.z(),
                "portal.portal",
                2.0F,
                0.8F + world.rand.nextFloat() * 0.4F);
        }
        double r = ctx.radius();
        List<EntityLivingBase> near = world.getEntitiesWithinAABB(
            EntityLivingBase.class,
            net.minecraft.util.AxisAlignedBB
                .getBoundingBox(ctx.x() - r, ctx.y() - r, ctx.z() - r, ctx.x() + r, ctx.y() + r, ctx.z() + r));
        int handled = 0;
        for (EntityLivingBase e : near) {
            if (e.isDead || e.getHealth() <= 0.0F
                || e.getEntityData()
                    .getLong(SENT_KEY) > world.getTotalWorldTime()) {
                continue; // just sent by a blast: never pulled back in by the same or a neighboring one
            }
            if (++handled > EnderRules.MAX_PULLED) {
                break;
            }
            double dx = e.posX - ctx.x();
            double dy = e.posY + e.height / 2.0 - ctx.y();
            double dz = e.posZ - ctx.z();
            if (EnderRules.inTeleportRange(dx, dy, dz) && !(e instanceof IBossDisplayData)
                && state.getInteger("sent") < IcbmConfig.enderMaxTeleports) {
                if (teleport(e, tx + 0.5, state.getInteger("ty"), tz + 0.5)) {
                    state.setInteger("sent", state.getInteger("sent") + 1);
                    continue;
                }
            }
            e.motionX += EnderRules.pull(dx, r);
            e.motionY += EnderRules.pull(dy, r) * EnderRules.VERTICAL_FACTOR;
            e.motionZ += EnderRules.pull(dz, r);
            e.velocityChanged = true;
        }
        return true;
    }

    private static boolean teleport(EntityLivingBase e, double x, double y, double z) {
        EnderTeleportEvent event = new EnderTeleportEvent(e, x, y, z, 0.0F);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return false;
        }
        if (e.ridingEntity != null) {
            e.mountEntity(null);
        }
        if (e.riddenByEntity != null) {
            e.riddenByEntity.mountEntity(null);
        }
        if (e instanceof EntityPlayerMP) {
            ((EntityPlayerMP) e).playerNetServerHandler
                .setPlayerLocation(event.targetX, event.targetY, event.targetZ, e.rotationYaw, e.rotationPitch);
        } else {
            e.setPositionAndUpdate(event.targetX, event.targetY, event.targetZ);
        }
        e.getEntityData()
            .setLong(SENT_KEY, e.worldObj.getTotalWorldTime() + SENT_IMMUNITY);
        e.fallDistance = 0.0F;
        e.motionX = 0.0;
        e.motionY = 0.0;
        e.motionZ = 0.0;
        e.worldObj.playSoundEffect(event.targetX, event.targetY, event.targetZ, "mob.endermen.portal", 1.0F, 1.0F);
        return true;
    }
}
