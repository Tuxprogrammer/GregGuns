package com.gregguns.fx;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.PacketStrings;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to client: spawn the named FX at a point. The handler runs on the client thread (GGS-04-09) and calls
 * {@link FxHooks#sink()}, the same path every other FX spawn uses (fixes PFX-04).
 *
 * <p>
 * Wire: name (UTF-8 string, at most {@value #MAX_NAME} characters), x, y, z (doubles), mx, my, mz (floats, blocks per
 * tick). Techguns sends the name in the platform charset (CORE-11); this uses UTF-8.
 *
 * <p>
 * Side: sent by the server through {@link FxSender}, handled by the client.
 */
// Spec: docs/techguns-spec/01-core/networking.md (PacketSpawnParticle); GGS-04-09
public final class PacketSpawnParticle extends ClientboundPacket {

    /** Longest FX name accepted, characters. */
    public static final int MAX_NAME = 64;

    private String name = "";
    private double x;
    private double y;
    private double z;
    private float mx;
    private float my;
    private float mz;

    /** Required by the channel. */
    public PacketSpawnParticle() {}

    /**
     * @param name FX name, 1 to {@value #MAX_NAME} characters
     * @param x    position
     * @param y    position
     * @param z    position
     * @param mx   the effect's motion, blocks per tick
     * @param my   motion
     * @param mz   motion
     */
    public PacketSpawnParticle(String name, double x, double y, double z, double mx, double my, double mz) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.mx = (float) mx;
        this.my = (float) my;
        this.mz = (float) mz;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        PacketStrings.write(buf, name);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(mx);
        buf.writeFloat(my);
        buf.writeFloat(mz);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        name = PacketStrings.read(buf, MAX_NAME);
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        mx = buf.readFloat();
        my = buf.readFloat();
        mz = buf.readFloat();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("name length", name.length(), 1, MAX_NAME)
            .finite("x", x)
            .finite("y", y)
            .finite("z", z)
            .finite("mx", mx)
            .finite("my", my)
            .finite("mz", mz);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        FxHooks.sink()
            .spawnAt(name, x, y, z, mx, my, mz);
    }

    /** @return the FX name, for tests */
    String name() {
        return name;
    }

    /** @return the position, for tests */
    double[] position() {
        return new double[] { x, y, z };
    }
}
