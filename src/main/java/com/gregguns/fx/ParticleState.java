package com.gregguns.fx;

/**
 * One particle's physics and look, advanced once per client tick. Pure: the client entity holds one and copies its
 * position out each tick.
 *
 * <p>
 * Motion per tick: the stored velocity is read, gravity is subtracted from its vertical part (it does not
 * accumulate, as in Techguns), the position moves by that, then the stored velocity is damped. Particles do not
 * collide.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.3)
public final class ParticleState {

    private final ParticleSystemDef def;
    private final ParticleSpec spec;
    private double x;
    private double y;
    private double z;
    private double prevX;
    private double prevY;
    private double prevZ;
    private double vx;
    private double vy;
    private double vz;
    private float size;
    private float prevSize;
    private float sizeRate;
    private int age;
    private boolean dead;

    /**
     * @param def  the system the particle belongs to
     * @param spec what the emitter decided
     */
    public ParticleState(ParticleSystemDef def, ParticleSpec spec) {
        this.def = def;
        this.spec = spec;
        this.x = spec.x;
        this.y = spec.y;
        this.z = spec.z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.vx = spec.vx;
        this.vy = spec.vy;
        this.vz = spec.vz;
        this.size = spec.size;
        this.prevSize = size;
        this.sizeRate = spec.sizeRate;
    }

    /**
     * Advances one tick.
     *
     * @return true while the particle is still alive
     */
    public boolean tick() {
        if (dead) {
            return false;
        }
        if (age++ >= spec.lifetime) {
            dead = true;
            return false;
        }
        prevX = x;
        prevY = y;
        prevZ = z;
        prevSize = size;
        x += vx;
        y += vy - def.gravity;
        z += vz;
        vx *= spec.velocityDamping;
        vy *= spec.velocityDamping;
        vz *= spec.velocityDamping;
        size += sizeRate;
        sizeRate *= spec.sizeRateDamping;
        return true;
    }

    /** @return the position, blocks */
    public double x() {
        return x;
    }

    /** @return the position, blocks */
    public double y() {
        return y;
    }

    /** @return the position, blocks */
    public double z() {
        return z;
    }

    /**
     * @param partial fraction of a tick, 0 to 1
     * @return the interpolated x
     */
    public double x(float partial) {
        return prevX + (x - prevX) * partial;
    }

    /**
     * @param partial fraction of a tick, 0 to 1
     * @return the interpolated y
     */
    public double y(float partial) {
        return prevY + (y - prevY) * partial;
    }

    /**
     * @param partial fraction of a tick, 0 to 1
     * @return the interpolated z
     */
    public double z(float partial) {
        return prevZ + (z - prevZ) * partial;
    }

    /**
     * @param partial fraction of a tick, 0 to 1
     * @return the interpolated size; never negative
     */
    public float size(float partial) {
        return Math.max(0F, prevSize + (size - prevSize) * partial);
    }

    /** @return ticks lived */
    public int age() {
        return age;
    }

    /** @return true once the particle has lived its lifetime */
    public boolean isDead() {
        return dead;
    }

    /** @return the system this particle belongs to */
    public ParticleSystemDef def() {
        return def;
    }

    /** @return the quarter turns of the sprite, 0 to 3 */
    public int rotation() {
        return spec.rotation;
    }

    /**
     * @param partial fraction of a tick, 0 to 1
     * @return normalized lifetime, 0 to 1
     */
    public float progress(float partial) {
        float p = (age + partial) / spec.lifetime;
        return p < 0 ? 0 : (p > 1 ? 1 : p);
    }

    /**
     * @param partial fraction of a tick, 0 to 1
     * @return the sprite to draw: the fixed variation frame, or the animated frame for the current progress
     */
    public int frame(float partial) {
        if (def.hasVariations) {
            return spec.variationFrame;
        }
        int frame = (int) Math.floor(def.frames * progress(partial) * spec.animationSpeed);
        return Math.floorMod(frame, def.frames);
    }

    /**
     * @param partial fraction of a tick, 0 to 1
     * @return opacity, 0 to 1
     */
    public float alpha(float partial) {
        return ParticleCurve.alpha(def.alphaKeys, progress(partial));
    }

    /**
     * @param partial fraction of a tick, 0 to 1
     * @return packed 0xRRGGBB
     */
    public int rgb(float partial) {
        return ParticleCurve.rgb(def.colorKeys, progress(partial));
    }
}
