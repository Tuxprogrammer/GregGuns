package com.gregguns.fx;

import java.util.List;
import java.util.Random;

/**
 * The emitter state machine: delays, bursts, lifetime and where each new particle starts. Pure: no Minecraft types,
 * no wall clock; randomness comes from the {@link Random} passed in, so a seeded run repeats.
 *
 * <p>
 * Call {@link #tickFree} once per client tick for an effect at a point, or {@link #tickAttached} for one that follows
 * an entity. Each call returns the particles to create this tick through {@code out}.
 *
 * <p>
 * Behavior per the spec: after the initial delay, a burst on the first active tick and then every
 * {@code SpawnDelay + 1} ticks; an attached emitter with {@code SystemLifetime 0} lives as long as its entity, a free
 * one with 0 or 1 emits once. Deviation: the burst size is passed through a {@link Budget} first (GGS-04-08), and a
 * TRAIL volume with no motion puts every particle at the entity.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.1, 3.2);
// docs/gregguns-spec/04-projectiles-fx/particle-rendering.md
public final class EmitterSim {

    /** The per-tick rate from blocks per second. */
    static final float PER_SECOND_TO_PER_TICK = 0.05F;

    /** Decides how many of the requested particles may be created. */
    public interface Budget {

        /**
         * @param requested what the burst wants
         * @return how many may be created, 0 to {@code requested}
         */
        int grant(int requested);
    }

    /** A budget that grants every request. */
    public static final Budget UNLIMITED = requested -> requested;

    /** What an attached emitter reads from its entity each tick. Mutable; the client refills it. */
    public static final class Owner {

        /** Feet position. */
        public double x;
        public double y;
        public double z;
        /** Rotation in degrees (vanilla convention). */
        public float yaw;
        public float pitch;
        /** Motion in blocks per tick. */
        public double mx;
        public double my;
        public double mz;
        /** True once the entity is gone. */
        public boolean dead;
    }

    private final ParticleSystemDef def;
    private final Random rng;
    private final boolean attached;
    private final int systemLifetime;
    private int initialDelay;
    private float startSizeRate;
    private final float startSizeRateDamping;
    private float startSize;
    private int spawnDelay;
    private int ticksExisted;
    private boolean dead;
    // Free emitters carry their own position and drift.
    private double x;
    private double y;
    private double z;
    private final double mx;
    private final double my;
    private final double mz;

    private EmitterSim(ParticleSystemDef def, Random rng, boolean attached, double x, double y, double z, double mx,
        double my, double mz) {
        this.def = def;
        this.rng = rng;
        this.attached = attached;
        this.systemLifetime = def.systemLifetime.pick(rng);
        this.initialDelay = def.initialDelay.pick(rng);
        this.startSizeRate = def.startSizeRate.pick(rng);
        this.startSizeRateDamping = def.startSizeRateDamping.pick(rng);
        this.x = x;
        this.y = y;
        this.z = z;
        this.mx = mx;
        this.my = my;
        this.mz = mz;
    }

    /**
     * An emitter at a point that drifts with the given motion.
     *
     * @param def the system
     * @param rng randomness
     * @param x   position, blocks
     * @param y   position, blocks
     * @param z   position, blocks
     * @param mx  motion, blocks per tick (also seen by SystemVelocityFactor)
     * @param my  motion, blocks per tick
     * @param mz  motion, blocks per tick
     * @return the emitter
     */
    public static EmitterSim free(ParticleSystemDef def, Random rng, double x, double y, double z, double mx, double my,
        double mz) {
        return new EmitterSim(def, rng, false, x, y, z, mx, my, mz);
    }

    /**
     * An emitter that follows an entity; it reads the position each tick from the {@link Owner}.
     *
     * @param def the system
     * @param rng randomness
     * @return the emitter
     */
    public static EmitterSim attached(ParticleSystemDef def, Random rng) {
        return new EmitterSim(def, rng, true, 0, 0, 0, 0, 0, 0);
    }

    /** @return the system this emits */
    public ParticleSystemDef def() {
        return def;
    }

    /** @return true once the emitter will emit no more */
    public boolean isDead() {
        return dead;
    }

    /**
     * Advances a free emitter one tick.
     *
     * @param budget how many particles may be made
     * @param out    receives the new particles
     */
    public void tickFree(Budget budget, List<ParticleSpec> out) {
        if (attached) {
            throw new IllegalStateException("attached emitter needs an Owner");
        }
        if (dead) {
            return;
        }
        x += mx;
        y += my;
        z += mz;
        step(budget, out, x, y, z, 0F, 0F, mx, my, mz, false);
    }

    /**
     * Advances an attached emitter one tick.
     *
     * @param owner  the entity's current state
     * @param budget how many particles may be made
     * @param out    receives the new particles
     */
    public void tickAttached(Owner owner, Budget budget, List<ParticleSpec> out) {
        if (!attached) {
            throw new IllegalStateException("free emitter has no Owner");
        }
        if (dead) {
            return;
        }
        if (owner.dead) {
            dead = true;
            return;
        }
        step(budget, out, owner.x, owner.y, owner.z, owner.yaw, owner.pitch, owner.mx, owner.my, owner.mz, true);
    }

    private void step(Budget budget, List<ParticleSpec> out, double px, double py, double pz, float yaw, float pitch,
        double emx, double emy, double emz, boolean hasEntity) {
        if (initialDelay-- > 0) {
            return;
        }
        startSize += startSizeRate;
        startSizeRate *= startSizeRateDamping;
        if (spawnDelay-- <= 0) {
            int granted = budget.grant(def.particleCount.pick(rng));
            for (int i = 0; i < granted; i++) {
                out.add(makeParticle(px, py, pz, yaw, pitch, emx, emy, emz, hasEntity, i, granted));
            }
            spawnDelay = def.spawnDelay.pick(rng);
        }
        if (hasEntity) {
            if (systemLifetime > 0 && ticksExisted + 1 >= systemLifetime) {
                dead = true;
            }
        } else if (ticksExisted + 1 >= systemLifetime) {
            dead = true;
        }
        ticksExisted++;
    }

    private ParticleSpec makeParticle(double px, double py, double pz, float yaw, float pitch, double emx, double emy,
        double emz, boolean hasEntity, int index, int count) {
        double[] dir = { 0, 0, 0 };
        double[] pos = volumePosition(dir, emx, emy, emz, hasEntity, index, count);
        pos[0] += def.offsetX;
        pos[1] += def.offsetY;
        pos[2] += def.offsetZ;
        double[] vel = velocity(dir);
        // Local to world: about local Z by the pitch, then about Y by -90 - yaw (spec 3.1).
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(-90.0 - yaw);
        rotateZ(vel, pitchRad);
        rotateY(vel, yawRad);
        if (def.volumeType != VolumeType.TRAIL) {
            rotateZ(pos, pitchRad);
            rotateY(pos, yawRad);
        }
        float factor = def.systemVelocityFactor.pick(rng);
        ParticleSpec s = new ParticleSpec();
        s.x = px + pos[0];
        s.y = py + pos[1];
        s.z = pz + pos[2];
        s.vx = vel[0] * PER_SECOND_TO_PER_TICK + factor * emx;
        s.vy = vel[1] * PER_SECOND_TO_PER_TICK + factor * emy;
        s.vz = vel[2] * PER_SECOND_TO_PER_TICK + factor * emz;
        s.lifetime = Math.max(1, def.lifetime.pick(rng));
        s.size = def.size.pick(rng) + startSize;
        s.sizeRate = def.sizeRate.pick(rng);
        s.sizeRateDamping = def.sizeRateDamping.pick(rng);
        s.animationSpeed = def.animationSpeed.pick(rng);
        s.velocityDamping = def.velocityDamping.pick(rng);
        s.variationFrame = rng.nextInt(def.frames);
        s.rotation = def.randomRotation ? rng.nextInt(4) : 0;
        return s;
    }

    private double[] volumePosition(double[] dir, double emx, double emy, double emz, boolean hasEntity, int index,
        int count) {
        double[] pos = { 0, 0, 0 };
        switch (def.volumeType) {
            case SPHERE:
            case HEMISPHERE: {
                double y = def.volumeType == VolumeType.SPHERE ? rng.nextDouble() * 2 - 1 : rng.nextDouble();
                double az = rng.nextDouble() * Math.PI * 2;
                double ring = Math.sqrt(1 - y * y);
                dir[0] = ring * Math.cos(az);
                dir[1] = y;
                dir[2] = ring * Math.sin(az);
                double r = radius();
                pos[0] = dir[0] * r;
                pos[1] = dir[1] * r;
                pos[2] = dir[2] * r;
                break;
            }
            case CYLINDER: {
                double az = rng.nextDouble() * Math.PI * 2;
                dir[0] = Math.cos(az);
                dir[2] = Math.sin(az);
                double r = radius();
                pos[0] = dir[0] * r;
                pos[1] = rng.nextDouble() * def.volume2;
                pos[2] = dir[2] * r;
                break;
            }
            case CYLINDER2: {
                double az = rng.nextDouble() * Math.PI * 2;
                dir[1] = Math.cos(az);
                dir[2] = Math.sin(az);
                double r = radius();
                pos[0] = rng.nextDouble() * def.volume2;
                pos[1] = dir[1] * r;
                pos[2] = dir[2] * r;
                break;
            }
            case TRAIL: {
                double len = Math.sqrt(emx * emx + emy * emy + emz * emz);
                if (hasEntity && len > 1.0E-9 && count > 0) {
                    double k = -(double) index / count * def.volume1 / len;
                    pos[0] = emx * k;
                    pos[1] = emy * k;
                    pos[2] = emz * k;
                }
                break;
            }
            case POINT:
            default:
                break;
        }
        return pos;
    }

    private double radius() {
        return def.hollow ? def.volume1 : rng.nextDouble() * def.volume1;
    }

    private double[] velocity(double[] dir) {
        boolean hasDir = dir[0] != 0 || dir[1] != 0 || dir[2] != 0;
        double r = def.velocity1.pick(rng);
        switch (def.velocityType) {
            case HEMISPHERICAL:
            case SPHERICAL: {
                if (hasDir) {
                    return new double[] { dir[0] * r, dir[1] * r, dir[2] * r };
                }
                double y = def.velocityType == VelocityType.SPHERICAL ? rng.nextDouble() * 2 - 1 : rng.nextDouble();
                double az = rng.nextDouble() * Math.PI * 2;
                double ring = Math.sqrt(1 - y * y);
                return new double[] { ring * Math.cos(az) * r, y * r, ring * Math.sin(az) * r };
            }
            case OUTWARD: {
                double up = def.velocity2.pick(rng);
                if (hasDir) {
                    return new double[] { dir[0] * r, up, dir[2] * r };
                }
                double az = rng.nextDouble() * Math.PI * 2;
                return new double[] { Math.cos(az) * r, up, Math.sin(az) * r };
            }
            case ORTHO:
            default:
                return new double[] { r, def.velocity2.pick(rng), def.velocity3.pick(rng) };
        }
    }

    /** Rotates about the Z axis; spec formula. */
    static void rotateZ(double[] v, double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        double x = v[0] * c + v[1] * s;
        double y = v[1] * c - v[0] * s;
        v[0] = x;
        v[1] = y;
    }

    /** Rotates about the Y axis; spec formula. */
    static void rotateY(double[] v, double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        double x = v[0] * c + v[2] * s;
        double z = v[2] * c - v[0] * s;
        v[0] = x;
        v[2] = z;
    }
}
