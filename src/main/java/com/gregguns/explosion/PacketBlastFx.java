package com.gregguns.explosion;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to every client within {@link ExplosionRules#NUKE_FX_RANGE} blocks of a nuke-size blast: draw the blast.
 * Vanilla particles only reach about 64 blocks; this carries the center and radius so the FX system can draw a flash
 * that is visible from 150.
 *
 * <p>
 * Wire: x, y, z (double), radius (float). Side: sent by the server, handled through {@link BlastFxHooks}.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/m5-exit-gate.md (nuke scale)
public final class PacketBlastFx extends ClientboundPacket {

    private double x;
    private double y;
    private double z;
    private float radius;

    /** Required by the channel. */
    public PacketBlastFx() {}

    /**
     * @param x      centre X
     * @param y      centre Y
     * @param z      centre Z
     * @param radius blast radius, blocks
     */
    public PacketBlastFx(double x, double y, double z, float radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(radius);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        radius = buf.readFloat();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("radius", Math.round(radius), 1, 64);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        BlastFxHooks.sink()
            .blast(x, y, z, radius);
    }
}
