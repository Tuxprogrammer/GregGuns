package com.gregguns.icbm.fx;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.gregguns.network.GregGunsNetwork;

/** Sends {@link PacketBlastOrb} to the players within {@link OrbPlan#BROADCAST_RANGE}. Server side. */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Orbs)
public final class OrbBroadcast {

    private OrbBroadcast() {}

    /**
     * @param world  the server world
     * @param kind   the orb
     * @param x      center X
     * @param y      center Y
     * @param z      center Z
     * @param radius blocks (clamped to 1..255)
     * @param life   ticks the orb still has (clamped to 1..32767)
     * @return how many players were sent the packet
     */
    public static int send(World world, OrbKind kind, double x, double y, double z, int radius, int life) {
        if (world == null || world.isRemote || life <= 0) {
            return 0;
        }
        int r = Math.max(1, Math.min(255, radius));
        int l = Math.min(Short.MAX_VALUE, life);
        double limit = OrbPlan.BROADCAST_RANGE * OrbPlan.BROADCAST_RANGE;
        int sent = 0;
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
                    .sendTo(new PacketBlastOrb(kind, x, y, z, r, l), p);
                sent++;
            }
        }
        return sent;
    }
}
