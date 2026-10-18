package com.gregguns.projectile;

import io.netty.buffer.ByteBuf;

/**
 * The extra numbers a client needs beyond the spawn packet's position and velocity to fly the same path as the server:
 * remaining and total lifetime, and gravity. Sent through {@code IEntityAdditionalSpawnData}. Immutable.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2.7 Network sync, 3.1)
// Bug PFX-34: Techguns never synced SimpleProjectile's gravity, so the client copy used a hard-coded default.
public final class BulletSpawnData {

    private final int ticksToLive;
    private final int lifetime;
    private final float gravity;

    /**
     * @param ticksToLive ticks of flight left
     * @param lifetime    total lifetime in ticks
     * @param gravity     blocks per tick squared pulled downwards
     */
    public BulletSpawnData(int ticksToLive, int lifetime, float gravity) {
        this.ticksToLive = ticksToLive;
        this.lifetime = lifetime;
        this.gravity = gravity;
    }

    /** @return ticks of flight left */
    public int ticksToLive() {
        return ticksToLive;
    }

    /** @return total lifetime in ticks */
    public int lifetime() {
        return lifetime;
    }

    /** @return gravity in blocks per tick squared */
    public float gravity() {
        return gravity;
    }

    /**
     * @param buf where to write: two ints and a float
     */
    public void write(ByteBuf buf) {
        buf.writeInt(ticksToLive);
        buf.writeInt(lifetime);
        buf.writeFloat(gravity);
    }

    /**
     * @param buf where to read the three values written by {@link #write}
     * @return the decoded data
     */
    public static BulletSpawnData read(ByteBuf buf) {
        return new BulletSpawnData(buf.readInt(), buf.readInt(), buf.readFloat());
    }
}
