package com.gregguns.icbm.fx;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to client: an annihilation orb (the antimatter flash or the red matter body) exists at a point. Wire: kind
 * (byte), x, y, z (floats), radius (short), ticks left (short): 17 bytes. The handler calls {@link OrbFxHooks#sink()},
 * a no-op until the client installs its sink. Side: sent by the server, handled by the client.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Orbs)
public final class PacketBlastOrb extends ClientboundPacket {

    private int kind;
    private float x;
    private float y;
    private float z;
    private int radius;
    private int life;

    /** Required by the channel. */
    public PacketBlastOrb() {}

    /**
     * @param kind   the orb
     * @param x      center X
     * @param y      center Y
     * @param z      center Z
     * @param radius blocks, 1 to 255
     * @param life   ticks left, 1 to 32767
     */
    public PacketBlastOrb(OrbKind kind, double x, double y, double z, int radius, int life) {
        this.kind = kind.ordinal();
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.radius = radius;
        this.life = life;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeByte(kind);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeShort(radius);
        buf.writeShort(life);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        kind = buf.readByte();
        x = buf.readFloat();
        y = buf.readFloat();
        z = buf.readFloat();
        radius = buf.readShort();
        life = buf.readShort();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("kind", kind, 0, OrbKind.values().length - 1)
            .finite("x", x)
            .finite("y", y)
            .finite("z", z)
            .inRange("radius", radius, 1, 255)
            .inRange("life", life, 1, Short.MAX_VALUE);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        OrbKind k = OrbKind.byId(kind);
        if (k != null) {
            OrbFxHooks.sink()
                .orb(k, x, y, z, radius, life);
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
    int life() {
        return life;
    }
}
