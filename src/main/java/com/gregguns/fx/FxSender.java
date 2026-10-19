package com.gregguns.fx;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.gregguns.network.GregGunsNetwork;

import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * Server-side entry point for FX: sends the packet that makes clients spawn a named effect. Does nothing on a client
 * world, so a call from shared code never spawns from the wrong thread (fixes PFX-04).
 *
 * <p>
 * Usage: {@code FxSender.at(world, "RocketExplosion", x, y, z, 50);} and
 * {@code FxSender.onEntity(projectile, "BioGunTrail");}. Effect names live in the data files, see {@link FxResources}.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/fx-catalog.md (Triggers: packet, range); Fixes PFX-04
public final class FxSender {

    /** Range of most point effects, blocks. */
    public static final double RANGE_NEAR = 50;
    /** Range of projectile trails, blocks. */
    public static final double RANGE_TRAIL = 200;
    /** Range of the nuke blast, blocks. */
    public static final double RANGE_NUKE = 150;

    private FxSender() {}

    /**
     * Spawns an effect at a point for every player within range.
     *
     * @param world the server world; a client world is ignored
     * @param name  FX name, 1 to {@value PacketSpawnParticle#MAX_NAME} characters
     * @param x     position
     * @param y     position
     * @param z     position
     * @param range how far away players still see it, blocks
     */
    public static void at(World world, String name, double x, double y, double z, double range) {
        at(world, name, x, y, z, 0, 0, 0, range);
    }

    /**
     * Spawns a moving effect at a point.
     *
     * @param world the server world; a client world is ignored
     * @param name  FX name
     * @param x     position
     * @param y     position
     * @param z     position
     * @param mx    the effect's motion, blocks per tick
     * @param my    motion
     * @param mz    motion
     * @param range how far away players still see it, blocks
     */
    public static void at(World world, String name, double x, double y, double z, double mx, double my, double mz,
        double range) {
        if (world.isRemote || !valid(name)) {
            return;
        }
        GregGunsNetwork.channel()
            .sendToAllAround(
                new PacketSpawnParticle(name, x, y, z, mx, my, mz),
                new NetworkRegistry.TargetPoint(world.provider.dimensionId, x, y, z, range));
    }

    /**
     * Spawns an effect attached to an entity, for the players tracking it (and the entity itself when it is a
     * player). A client that gets the packet before the entity waits for it (PFX-01).
     *
     * @param entity the entity to follow; ignored when it is on a client world
     * @param name   FX name
     */
    public static void onEntity(Entity entity, String name) {
        if (entity.worldObj.isRemote || !valid(name)) {
            return;
        }
        PacketSpawnParticleOnEntity packet = new PacketSpawnParticleOnEntity(name, entity.getEntityId());
        if (entity instanceof EntityPlayerMP) {
            GregGunsNetwork.channel()
                .sendToAllTrackingAndSelf(packet, entity);
        } else {
            GregGunsNetwork.channel()
                .sendToAllTracking(packet, entity);
        }
    }

    private static boolean valid(String name) {
        return name != null && !name.isEmpty() && name.length() <= PacketSpawnParticle.MAX_NAME;
    }
}
