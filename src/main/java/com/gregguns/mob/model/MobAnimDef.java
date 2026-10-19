package com.gregguns.mob.model;

import org.jetbrains.annotations.Nullable;

/**
 * One animation entry of a mob model. Every angle in the file is in degrees; {@link MobAnimator} converts to radians.
 * An entry targets one part, and the entries run in file order, each adding to (or, for a {@code set} pose,
 * overriding) the angles the earlier ones left. Fields that a type does not use stay at their defaults.
 */
// Spec: tools/asset-gen/entity_models.md; docs/techguns-spec/10-entities-npcs/rendering-and-models.md
public final class MobAnimDef {

    /** The animation types. */
    public enum Type {

        /** Head follows the look yaw and pitch, with limits. */
        LOOK("look"),
        /** An oscillation driven by walk distance and speed. */
        SWING("swing"),
        /** A sine of the age in ticks. */
        IDLE("idle"),
        /** A constant rate about an axis. */
        SPIN("spin"),
        /** Angles that apply while aiming, attacking, holding an item or dying. */
        POSE("pose");

        private final String json;

        Type(String json) {
            this.json = json;
        }

        /** @return the name used in the model files */
        public String json() {
            return json;
        }

        /**
         * @param json a type name from a model file
         * @return the type
         * @throws IllegalArgumentException for an unknown name
         */
        public static Type fromJson(String json) {
            for (Type t : values()) {
                if (t.json.equals(json)) {
                    return t;
                }
            }
            throw new IllegalArgumentException(
                "unknown animation type '" + json + "' (expected look, swing, idle, spin or pose)");
        }
    }

    /** When a pose applies. */
    public enum When {

        /** The mob is aiming a gun. */
        AIMING("aiming"),
        /** The mob is in an attack; the weight follows the attack progress and the blend. */
        ATTACKING("attacking"),
        /** The mob holds an item in its right hand. */
        HOLDING("holding"),
        /** The mob is dying; the weight is the death progress. */
        DYING("dying");

        private final String json;

        When(String json) {
            this.json = json;
        }

        /** @return the name used in the model files */
        public String json() {
            return json;
        }

        /**
         * @param json a name from a model file
         * @return the condition
         * @throws IllegalArgumentException for an unknown name
         */
        public static When fromJson(String json) {
            for (When w : values()) {
                if (w.json.equals(json)) {
                    return w;
                }
            }
            throw new IllegalArgumentException(
                "unknown pose condition '" + json + "' (expected aiming, attacking, holding or dying)");
        }
    }

    /** How an attacking pose's weight follows the attack progress {@code p} (after the window is applied). */
    public enum Blend {

        /** 1 whenever the progress is inside the window, else 0. */
        STEP("step"),
        /** {@code 0.5 * (1 - cos(pi * p))}: eases from 0 to 1. */
        COSINE("cosine"),
        /** {@code sin(pi * p)}: rises to 1 at the middle and returns to 0 (a swing out and back). */
        SINE("sine");

        private final String json;

        Blend(String json) {
            this.json = json;
        }

        /** @return the name used in the model files */
        public String json() {
            return json;
        }

        /**
         * @param json a name from a model file
         * @return the blend
         * @throws IllegalArgumentException for an unknown name
         */
        public static Blend fromJson(String json) {
            for (Blend b : values()) {
                if (b.json.equals(json)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("unknown blend '" + json + "' (expected step, cosine or sine)");
        }
    }

    /** The vanilla walk-cycle rate, radians per unit of limb swing. */
    public static final float DEFAULT_SWING_SPEED = 0.6662F;

    private final Type type;
    private final String part;
    private final int axis; // 0 x, 1 y, 2 z; -1 when the type has no single axis
    private final float amplitude;
    private final float speed;
    private final float phase;
    private final float offset;
    private final float rate;
    private final float deathScale;
    private final float yawFactor;
    private final float pitchFactor;
    private final float maxYaw;
    private final float maxPitch;
    private final @Nullable When when;
    private final boolean set;
    private final float[] angles; // x, y, z degrees; NaN where the pose leaves the axis alone
    private final float followYaw;
    private final float followPitch;
    private final Blend blend;
    private final float windowFrom;
    private final float windowTo;

    private MobAnimDef(Builder b) {
        this.type = b.type;
        this.part = b.part;
        this.axis = b.axis;
        this.amplitude = b.amplitude;
        this.speed = b.speed;
        this.phase = b.phase;
        this.offset = b.offset;
        this.rate = b.rate;
        this.deathScale = b.deathScale;
        this.yawFactor = b.yawFactor;
        this.pitchFactor = b.pitchFactor;
        this.maxYaw = b.maxYaw;
        this.maxPitch = b.maxPitch;
        this.when = b.when;
        this.set = b.set;
        this.angles = b.angles.clone();
        this.followYaw = b.followYaw;
        this.followPitch = b.followPitch;
        this.blend = b.blend;
        this.windowFrom = b.windowFrom;
        this.windowTo = b.windowTo;
    }

    /** @return the animation type */
    public Type type() {
        return type;
    }

    /** @return the target part's name */
    public String part() {
        return part;
    }

    /** @return the axis, 0 x, 1 y, 2 z, or -1 for look and pose */
    public int axis() {
        return axis;
    }

    /** @return swing and idle: the peak angle, degrees */
    public float amplitude() {
        return amplitude;
    }

    /** @return swing: radians per unit of limb swing; idle: radians per tick */
    public float speed() {
        return speed;
    }

    /** @return swing and idle: the phase, degrees */
    public float phase() {
        return phase;
    }

    /** @return idle: the constant added to the sine, degrees */
    public float offset() {
        return offset;
    }

    /** @return spin: degrees per tick */
    public float rate() {
        return rate;
    }

    /** @return spin: the factor on the rate while the mob is dying (1 = unchanged) */
    public float deathScale() {
        return deathScale;
    }

    /** @return look: how much of the head yaw is applied (1 = all) */
    public float yawFactor() {
        return yawFactor;
    }

    /** @return look: how much of the head pitch is applied (1 = all) */
    public float pitchFactor() {
        return pitchFactor;
    }

    /** @return look: the yaw limit either way, degrees */
    public float maxYaw() {
        return maxYaw;
    }

    /** @return look: the pitch limit either way, degrees */
    public float maxPitch() {
        return maxPitch;
    }

    /** @return pose: the condition */
    public @Nullable When when() {
        return when;
    }

    /** @return pose: true when the angles replace the current ones, false when they add to them */
    public boolean isSet() {
        return set;
    }

    /** @return pose: a copy of x, y, z degrees; NaN marks an axis the pose leaves alone */
    public float[] angles() {
        return angles.clone();
    }

    /** @return pose: the share of the head yaw added to the Y angle */
    public float followYaw() {
        return followYaw;
    }

    /** @return pose: the share of the head pitch added to the X angle */
    public float followPitch() {
        return followPitch;
    }

    /** @return pose: the attacking weight curve */
    public Blend blend() {
        return blend;
    }

    /** @return pose: the attack progress at which the weight starts */
    public float windowFrom() {
        return windowFrom;
    }

    /** @return pose: the attack progress at which the weight ends */
    public float windowTo() {
        return windowTo;
    }

    /** Fills a {@link MobAnimDef}; the parser and tests use it. */
    public static final class Builder {

        private final Type type;
        private final String part;
        private int axis = -1;
        private float amplitude;
        private float speed;
        private float phase;
        private float offset;
        private float rate;
        private float deathScale = 1.0F;
        private float yawFactor = 1.0F;
        private float pitchFactor = 1.0F;
        private float maxYaw = 360.0F;
        private float maxPitch = 360.0F;
        private When when;
        private boolean set;
        private float[] angles = { Float.NaN, Float.NaN, Float.NaN };
        private float followYaw;
        private float followPitch;
        private Blend blend = Blend.STEP;
        private float windowFrom;
        private float windowTo = 1.0F;

        /**
         * @param type the type
         * @param part the target part
         */
        public Builder(Type type, String part) {
            this.type = type;
            this.part = part;
            if (type == Type.SWING) {
                this.speed = DEFAULT_SWING_SPEED;
            } else if (type == Type.IDLE) {
                this.speed = 0.1F;
            }
        }

        /**
         * @param a 0, 1 or 2
         * @return this
         */
        public Builder axis(int a) {
            this.axis = a;
            return this;
        }

        /**
         * @param v degrees
         * @return this
         */
        public Builder amplitude(float v) {
            this.amplitude = v;
            return this;
        }

        /**
         * @param v radians per unit
         * @return this
         */
        public Builder speed(float v) {
            this.speed = v;
            return this;
        }

        /**
         * @param v degrees
         * @return this
         */
        public Builder phase(float v) {
            this.phase = v;
            return this;
        }

        /**
         * @param v degrees
         * @return this
         */
        public Builder offset(float v) {
            this.offset = v;
            return this;
        }

        /**
         * @param v degrees per tick
         * @return this
         */
        public Builder rate(float v) {
            this.rate = v;
            return this;
        }

        /**
         * @param v factor on the spin rate while dying
         * @return this
         */
        public Builder deathScale(float v) {
            this.deathScale = v;
            return this;
        }

        /**
         * @param yaw   head yaw share
         * @param pitch head pitch share
         * @return this
         */
        public Builder lookFactors(float yaw, float pitch) {
            this.yawFactor = yaw;
            this.pitchFactor = pitch;
            return this;
        }

        /**
         * @param yaw   yaw limit, degrees
         * @param pitch pitch limit, degrees
         * @return this
         */
        public Builder lookLimits(float yaw, float pitch) {
            this.maxYaw = yaw;
            this.maxPitch = pitch;
            return this;
        }

        /**
         * @param w the condition
         * @return this
         */
        public Builder when(When w) {
            this.when = w;
            return this;
        }

        /**
         * @param s true to replace the current angles
         * @return this
         */
        public Builder set(boolean s) {
            this.set = s;
            return this;
        }

        /**
         * @param x degrees or NaN
         * @param y degrees or NaN
         * @param z degrees or NaN
         * @return this
         */
        public Builder angles(float x, float y, float z) {
            this.angles = new float[] { x, y, z };
            return this;
        }

        /**
         * @param yaw   share of head yaw added to Y
         * @param pitch share of head pitch added to X
         * @return this
         */
        public Builder follow(float yaw, float pitch) {
            this.followYaw = yaw;
            this.followPitch = pitch;
            return this;
        }

        /**
         * @param b the weight curve
         * @return this
         */
        public Builder blend(Blend b) {
            this.blend = b;
            return this;
        }

        /**
         * @param from progress where the weight starts
         * @param to   progress where it ends
         * @return this
         */
        public Builder window(float from, float to) {
            this.windowFrom = from;
            this.windowTo = to;
            return this;
        }

        /** @return the entry */
        public MobAnimDef build() {
            return new MobAnimDef(this);
        }
    }
}
