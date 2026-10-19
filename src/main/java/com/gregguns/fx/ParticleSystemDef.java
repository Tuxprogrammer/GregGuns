package com.gregguns.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One emitter definition: what a particle looks like and how an emitter spawns it. Immutable once built; made by
 * {@link FxParser} or by tests through {@link Builder}. Pure: no Minecraft types.
 *
 * <p>
 * Every field is documented by the key it comes from in the data format, see {@link FxParser}. Defaults follow the
 * Techguns spec.
 *
 * <p>
 * Deviations from Techguns: {@code Rows} is honored (Techguns reads it but divides the sheet by columns twice);
 * {@code EXTENDS} copies every field (Techguns copies part and shares lists); {@code VolumeType} defaults to
 * {@code POINT} (Techguns crashes without it); {@code RemoveOnGround} and {@code VelocityDampingOnGround} are parsed
 * and ignored because Techguns never triggers them either (PFX-17).
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (2.3, 2.4);
// docs/gregguns-spec/04-projectiles-fx/particle-rendering.md
public final class ParticleSystemDef {

    /** Default texture file name, under {@code textures/fx}. */
    public static final String DEFAULT_TEXTURE = "fireball.png";

    final String name;
    final String texture;
    final RenderMode renderMode;
    final int rows;
    final int columns;
    final int frames;
    final boolean hasVariations;
    final boolean randomRotation;
    final FxIntRange lifetime;
    final FxIntRange systemLifetime;
    final FxIntRange initialDelay;
    final FxIntRange spawnDelay;
    final FxIntRange particleCount;
    final FxRange animationSpeed;
    final FxRange size;
    final FxRange sizeRate;
    final FxRange sizeRateDamping;
    final FxRange startSizeRate;
    final FxRange startSizeRateDamping;
    final List<CurveKey> alphaKeys;
    final List<CurveKey> colorKeys;
    final VelocityType velocityType;
    final FxRange velocity1;
    final FxRange velocity2;
    final FxRange velocity3;
    final FxRange velocityDamping;
    final float gravity;
    final FxRange systemVelocityFactor;
    final VolumeType volumeType;
    final float volume1;
    final float volume2;
    final boolean hollow;
    final float offsetX;
    final float offsetY;
    final float offsetZ;

    private ParticleSystemDef(Builder b) {
        name = b.name;
        texture = b.texture;
        renderMode = b.renderMode;
        columns = Math.max(1, b.columns);
        rows = Math.max(1, b.rows > 0 ? b.rows : columns);
        frames = Math.max(1, Math.min(b.frames, columns * rows));
        hasVariations = b.hasVariations;
        randomRotation = b.randomRotation;
        lifetime = b.lifetime;
        systemLifetime = b.systemLifetime;
        initialDelay = b.initialDelay;
        spawnDelay = b.spawnDelay;
        particleCount = b.particleCount;
        animationSpeed = b.animationSpeed;
        size = b.size;
        sizeRate = b.sizeRate;
        sizeRateDamping = b.sizeRateDamping;
        startSizeRate = b.startSizeRate;
        startSizeRateDamping = b.startSizeRateDamping;
        alphaKeys = Collections.unmodifiableList(sorted(b.alphaKeys));
        colorKeys = Collections.unmodifiableList(sorted(b.colorKeys));
        velocityType = b.velocityType;
        velocity1 = b.velocity1;
        velocity2 = b.velocity2;
        velocity3 = b.velocity3;
        velocityDamping = b.velocityDamping;
        gravity = b.gravity;
        systemVelocityFactor = b.systemVelocityFactor;
        volumeType = b.volumeType;
        volume1 = b.volume1;
        volume2 = b.volume2;
        hollow = b.hollow;
        offsetX = b.offsetX;
        offsetY = b.offsetY;
        offsetZ = b.offsetZ;
    }

    private static List<CurveKey> sorted(List<CurveKey> keys) {
        List<CurveKey> copy = new ArrayList<>(keys);
        // Stable: equal times keep file order.
        copy.sort((x, y) -> Float.compare(x.time, y.time));
        return copy;
    }

    /** @return the system's name as written in the data file (case kept) */
    public String name() {
        return name;
    }

    /** @return the texture: a file name under {@code textures/fx}, or a full resource location with a colon */
    public String texture() {
        return texture;
    }

    /** @return the blend mode */
    public RenderMode renderMode() {
        return renderMode;
    }

    /** @return sheet columns, at least 1 */
    public int columns() {
        return columns;
    }

    /** @return sheet rows, at least 1 */
    public int rows() {
        return rows;
    }

    /** @return the number of sprites used, numbered left to right then top to bottom */
    public int frames() {
        return frames;
    }

    /** @return the particle count range per burst */
    public FxIntRange particleCount() {
        return particleCount;
    }

    /** @return the emitter's volume type */
    public VolumeType volumeType() {
        return volumeType;
    }

    /** @return the emitter lifetime range in ticks */
    public FxIntRange systemLifetime() {
        return systemLifetime;
    }

    /** @return the alpha keys, ascending by time */
    public List<CurveKey> alphaKeys() {
        return alphaKeys;
    }

    /** @return the color keys, ascending by time */
    public List<CurveKey> colorKeys() {
        return colorKeys;
    }

    /**
     * @param name the system's name
     * @return a new builder with every default set
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Mutable builder; every setter returns the builder. Use from one thread. */
    public static final class Builder {

        String name;
        String texture = DEFAULT_TEXTURE;
        RenderMode renderMode = RenderMode.SOLID;
        int rows = 0;
        int columns = 1;
        int frames = 1;
        boolean hasVariations;
        boolean randomRotation;
        FxIntRange lifetime = FxIntRange.of(20);
        FxIntRange systemLifetime = FxIntRange.of(5);
        FxIntRange initialDelay = FxIntRange.of(0);
        FxIntRange spawnDelay = FxIntRange.of(1);
        FxIntRange particleCount = FxIntRange.of(1);
        FxRange animationSpeed = FxRange.of(1);
        FxRange size = new FxRange(1, 2);
        FxRange sizeRate = FxRange.of(1);
        FxRange sizeRateDamping = FxRange.of(1);
        FxRange startSizeRate = FxRange.of(0);
        FxRange startSizeRateDamping = FxRange.of(1);
        List<CurveKey> alphaKeys = new ArrayList<>();
        List<CurveKey> colorKeys = new ArrayList<>();
        VelocityType velocityType = VelocityType.SPHERICAL;
        FxRange velocity1 = FxRange.of(0);
        FxRange velocity2 = FxRange.of(0);
        FxRange velocity3 = FxRange.of(0);
        FxRange velocityDamping = FxRange.of(0.98F);
        float gravity = 0.02F;
        FxRange systemVelocityFactor = FxRange.of(0);
        VolumeType volumeType = VolumeType.POINT;
        float volume1;
        float volume2;
        boolean hollow;
        float offsetX;
        float offsetY;
        float offsetZ;

        Builder(String name) {
            this.name = name;
        }

        /**
         * Copies every field of {@code parent}, deep, keeping this builder's name.
         *
         * @param parent the system to copy
         * @return this
         */
        public Builder copyFrom(ParticleSystemDef parent) {
            texture = parent.texture;
            renderMode = parent.renderMode;
            rows = parent.rows;
            columns = parent.columns;
            frames = parent.frames;
            hasVariations = parent.hasVariations;
            randomRotation = parent.randomRotation;
            lifetime = parent.lifetime;
            systemLifetime = parent.systemLifetime;
            initialDelay = parent.initialDelay;
            spawnDelay = parent.spawnDelay;
            particleCount = parent.particleCount;
            animationSpeed = parent.animationSpeed;
            size = parent.size;
            sizeRate = parent.sizeRate;
            sizeRateDamping = parent.sizeRateDamping;
            startSizeRate = parent.startSizeRate;
            startSizeRateDamping = parent.startSizeRateDamping;
            alphaKeys = new ArrayList<>(parent.alphaKeys);
            colorKeys = new ArrayList<>(parent.colorKeys);
            velocityType = parent.velocityType;
            velocity1 = parent.velocity1;
            velocity2 = parent.velocity2;
            velocity3 = parent.velocity3;
            velocityDamping = parent.velocityDamping;
            gravity = parent.gravity;
            systemVelocityFactor = parent.systemVelocityFactor;
            volumeType = parent.volumeType;
            volume1 = parent.volume1;
            volume2 = parent.volume2;
            hollow = parent.hollow;
            offsetX = parent.offsetX;
            offsetY = parent.offsetY;
            offsetZ = parent.offsetZ;
            return this;
        }

        /** @param v texture file name under textures/fx, or a resource location @return this */
        public Builder texture(String v) {
            texture = v;
            return this;
        }

        /** @param v blend mode @return this */
        public Builder renderMode(RenderMode v) {
            renderMode = v;
            return this;
        }

        /** @param v columns @return this */
        public Builder columns(int v) {
            columns = v;
            return this;
        }

        /** @param v rows; 0 means "same as columns" @return this */
        public Builder rows(int v) {
            rows = v;
            return this;
        }

        /** @param v sprite count @return this */
        public Builder frames(int v) {
            frames = v;
            return this;
        }

        /** @param v true to pick one random frame per particle @return this */
        public Builder hasVariations(boolean v) {
            hasVariations = v;
            return this;
        }

        /** @param v true to give each particle a random quarter turn @return this */
        public Builder randomRotation(boolean v) {
            randomRotation = v;
            return this;
        }

        /** @param v particle lifetime, ticks @return this */
        public Builder lifetime(FxIntRange v) {
            lifetime = v;
            return this;
        }

        /** @param v emitter lifetime, ticks @return this */
        public Builder systemLifetime(FxIntRange v) {
            systemLifetime = v;
            return this;
        }

        /** @param v ticks before the emitter starts @return this */
        public Builder initialDelay(FxIntRange v) {
            initialDelay = v;
            return this;
        }

        /** @param v pause between bursts; the interval is value + 1 ticks @return this */
        public Builder spawnDelay(FxIntRange v) {
            spawnDelay = v;
            return this;
        }

        /** @param v particles per burst @return this */
        public Builder particleCount(FxIntRange v) {
            particleCount = v;
            return this;
        }

        /** @param v animation cycles per lifetime @return this */
        public Builder animationSpeed(FxRange v) {
            animationSpeed = v;
            return this;
        }

        /** @param v start size @return this */
        public Builder size(FxRange v) {
            size = v;
            return this;
        }

        /** @param v size added per tick @return this */
        public Builder sizeRate(FxRange v) {
            sizeRate = v;
            return this;
        }

        /** @param v multiplier applied to the size rate each tick @return this */
        public Builder sizeRateDamping(FxRange v) {
            sizeRateDamping = v;
            return this;
        }

        /** @param v per-emitter growth of the start size @return this */
        public Builder startSizeRate(FxRange v) {
            startSizeRate = v;
            return this;
        }

        /** @param v multiplier applied to the start size rate each tick @return this */
        public Builder startSizeRateDamping(FxRange v) {
            startSizeRateDamping = v;
            return this;
        }

        /**
         * Adds an alpha key.
         *
         * @param alpha opacity, 0 to 1
         * @param time  normalized lifetime, 0 to 1
         * @return this
         */
        public Builder alphaKey(float alpha, float time) {
            alphaKeys.add(new CurveKey(time, alpha, 0, 0));
            return this;
        }

        /**
         * Adds a color key.
         *
         * @param r    red, 0 to 255
         * @param g    green, 0 to 255
         * @param b    blue, 0 to 255
         * @param time normalized lifetime, 0 to 1
         * @return this
         */
        public Builder colorKey(float r, float g, float b, float time) {
            colorKeys.add(new CurveKey(time, r, g, b));
            return this;
        }

        /** @param v how the starting velocity is chosen @return this */
        public Builder velocityType(VelocityType v) {
            velocityType = v;
            return this;
        }

        /** @param v first velocity parameter, blocks per second @return this */
        public Builder velocity1(FxRange v) {
            velocity1 = v;
            return this;
        }

        /** @param v second velocity parameter, blocks per second @return this */
        public Builder velocity2(FxRange v) {
            velocity2 = v;
            return this;
        }

        /** @param v third velocity parameter, blocks per second @return this */
        public Builder velocity3(FxRange v) {
            velocity3 = v;
            return this;
        }

        /** @param v multiplier applied to velocity each tick @return this */
        public Builder velocityDamping(FxRange v) {
            velocityDamping = v;
            return this;
        }

        /** @param v constant downward drift, blocks per tick @return this */
        public Builder gravity(float v) {
            gravity = v;
            return this;
        }

        /** @param v fraction of the emitter's motion added to each particle @return this */
        public Builder systemVelocityFactor(FxRange v) {
            systemVelocityFactor = v;
            return this;
        }

        /** @param v where particles spawn @return this */
        public Builder volumeType(VolumeType v) {
            volumeType = v;
            return this;
        }

        /** @param v first volume parameter, blocks @return this */
        public Builder volume1(float v) {
            volume1 = v;
            return this;
        }

        /** @param v second volume parameter, blocks @return this */
        public Builder volume2(float v) {
            volume2 = v;
            return this;
        }

        /** @param v true to spawn on the surface @return this */
        public Builder hollow(boolean v) {
            hollow = v;
            return this;
        }

        /**
         * @param x local offset
         * @param y local offset
         * @param z local offset
         * @return this
         */
        public Builder offset(float x, float y, float z) {
            offsetX = x;
            offsetY = y;
            offsetZ = z;
            return this;
        }

        /** @return the immutable definition */
        public ParticleSystemDef build() {
            return new ParticleSystemDef(this);
        }
    }
}
