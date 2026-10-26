package com.gregguns.icbm.fx;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.gregguns.network.GregGunsNetwork;

/**
 * Sends {@link PacketBlastCloud} to the players near a blast, and only to them ({@link CloudPlan#BROADCAST_RANGE}).
 * Server side.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Cost bounds)
public final class CloudBroadcast {

    private CloudBroadcast() {}

    /**
     * Announces a cloud to every player of the world within range.
     *
     * @param world  the server world
     * @param kind   the cloud kind
     * @param x      center X
     * @param y      center Y
     * @param z      center Z
     * @param radius blocks (clamped to 1..255)
     * @param life   the whole length, ticks
     * @param left   ticks still to run
     * @return how many players were sent the packet
     */
    public static int send(World world, CloudKind kind, double x, double y, double z, int radius, int life, int left) {
        if (world == null || world.isRemote || left <= 0) {
            return 0;
        }
        int r = Math.max(1, Math.min(255, radius));
        int sent = 0;
        double limit = CloudPlan.BROADCAST_RANGE * CloudPlan.BROADCAST_RANGE;
        for (Object o : world.playerEntities) {
            if (!(o instanceof EntityPlayerMP)) {
                continue;
            }
            EntityPlayerMP p = (EntityPlayerMP) o;
            double dx = p.posX - x;
            double dy = p.posY - y;
            double dz = p.posZ - z;
            if (p.playerNetServerHandler != null && dx * dx + dy * dy + dz * dz <= limit) {
                GregGunsNetwork.channel()
                    .sendTo(
                        new PacketBlastCloud(kind, x, y, z, r, Math.min(life, Short.MAX_VALUE), Math.min(left, life)),
                        p);
                sent++;
            }
        }
        return sent;
    }
}
