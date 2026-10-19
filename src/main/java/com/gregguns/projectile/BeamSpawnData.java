package com.gregguns.projectile;

import io.netty.buffer.ByteBuf;

/**
 * What a client needs to draw a beam: length, lifetime, seed and style. Sent through
 * {@code IEntityAdditionalSpawnData}. Immutable.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.15 LaserBeam2: spawn data is the length)
// Deviation: Techguns synced only the length; the lifetime, seed and style are synced too so a beam can vary.
public final class BeamSpawnData {

    private final float length;
    private final int lifetime;
    private final int seed;
    private final int style;

    /**
     * @param length   beam length in blocks
     * @param lifetime ticks the beam is shown
     * @param seed     seed of the flicker
     * @param style    texture variant, 0 for the laser and 1 for the blue beam
     */
    public BeamSpawnData(float length, int lifetime, int seed, int style) {
        this.length = length;
        this.lifetime = lifetime;
        this.seed = seed;
        this.style = style;
    }

    /** @return beam length in blocks */
    public float length() {
        return length;
    }

    /** @return ticks the beam is shown */
    public int lifetime() {
        return lifetime;
    }

    /** @return flicker seed */
    public int seed() {
        return seed;
    }

    /** @return texture variant */
    public int style() {
        return style;
    }

    /** @param buf where to write: a float, two ints and a byte */
    public void write(ByteBuf buf) {
        buf.writeFloat(length);
        buf.writeInt(lifetime);
        buf.writeInt(seed);
        buf.writeByte(style);
    }

    /**
     * @param buf where to read the values written by {@link #write}
     * @return the decoded data
     */
    public static BeamSpawnData read(ByteBuf buf) {
        return new BeamSpawnData(buf.readFloat(), buf.readInt(), buf.readInt(), buf.readByte());
    }
}
