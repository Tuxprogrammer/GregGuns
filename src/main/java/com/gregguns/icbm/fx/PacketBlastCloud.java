package com.gregguns.icbm.fx;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to client: a blast cloud begins or is still running at a point. The client animates it with cheap vanilla
 * particles ({@link CloudPlan}). Wire: kind (byte), x, y, z (floats), radius (short), life (short, whole length in
 * ticks), ticks left (short): 19 bytes. The handler calls {@link CloudFxHooks#sink()}, a no-op until the client
 * installs its sink, so a dedicated server is safe. Side: sent by the server, handled by the client.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Packet)
public final class PacketBlastCloud extends ClientboundPacket {

    /** Wire size of one packet's payload, bytes. */
    public static final int WIRE_BYTES = 1 + 3 * 4 + 3 * 2;

    private int kind;
    private float x;
    private float y;
    private float z;
    private int radius;
    private int life;
    private int left;

    /** Required by the channel. */
    public PacketBlastCloud() {}

    /**
     * @param kind   the cloud kind
     * @param x      center X
     * @param y      center Y
     * @param z      center Z
     * @param radius blocks, 1 to 255
     * @param life   the cloud's whole length, ticks, 1 to 32767
     * @param left   ticks still to run, 1 to {@code life}
     */
    public PacketBlastCloud(CloudKind kind, double x, double y, double z, int radius, int life, int left) {
        this.kind = kind.ordinal();
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.radius = radius;
        this.life = life;
        this.left = left;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeByte(kind);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeShort(radius);
        buf.writeShort(life);
        buf.writeShort(left);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        kind = buf.readByte();
        x = buf.readFloat();
        y = buf.readFloat();
        z = buf.readFloat();
        radius = buf.readShort();
        life = buf.readShort();
        left = buf.readShort();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("kind", kind, 0, CloudKind.values().length - 1)
            .finite("x", x)
            .finite("y", y)
            .finite("z", z)
            .inRange("radius", radius, 1, 255)
            .inRange("life", life, 1, Short.MAX_VALUE)
            .inRange("left", left, 1, Short.MAX_VALUE);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        CloudKind k = CloudKind.byId(kind);
        if (k != null) {
            CloudFxHooks.sink()
                .cloud(k, x, y, z, radius, life, Math.min(left, life));
        }
    }

    /** @return the kind id, for tests */
    int kindId() {
        return kind;
    }

    /** @return the radius, for tests */
    int radius() {
        return radius;
    }

    /** @return the ticks left, for tests */
    int left() {
        return left;
    }
}
