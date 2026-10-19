package com.gregguns.mob.model;

/**
 * Evaluates the animation list of a {@link MobModelDef} into one set of part angles. Pure math (radians, no
 * Minecraft types), so the formulas are unit-tested and the client model only copies the result onto its parts.
 *
 * <p>
 * Every call starts from the parts' rest rotations and runs the entries in file order:
 * <ul>
 * <li>{@code look}: adds {@code clamp(netHeadYaw, maxYaw) * yaw} to Y and {@code clamp(headPitch, maxPitch) * pitch}
 * to X;</li>
 * <li>{@code swing}: adds {@code amplitude * limbSwingAmount * cos(limbSwing * speed + phase)} on the axis;</li>
 * <li>{@code idle}: adds {@code offset + amplitude * sin(ageInTicks * speed + phase)};</li>
 * <li>{@code spin}: adds {@code rate * ageInTicks}, with the rate times {@code deathScale} once the death progress
 * is above 0;</li>
 * <li>{@code pose}: with weight {@code w} (see {@link #poseWeight}) adds, or for {@code set} blends toward, the
 * given angles, plus {@code followYaw * netHeadYaw} on Y and {@code followPitch * headPitch} on X.</li>
 * </ul>
 */
// Spec: docs/techguns-spec/10-entities-npcs/rendering-and-models.md section 7 (vanilla biped and alien bug motion)
public final class MobAnimator {

    private MobAnimator() {}

    /**
     * @param def the model
     * @param in  this frame's inputs
     * @param out one {@code float[3]} (x, y, z radians) per part, in {@link MobModelDef#parts()} order; overwritten
     */
    public static void evaluate(MobModelDef def, AnimInput in, float[][] out) {
        for (int i = 0; i < out.length; i++) {
            float[] rest = def.parts()
                .get(i)
                .rotateDegrees();
            out[i][0] = rad(rest[0]);
            out[i][1] = rad(rest[1]);
            out[i][2] = rad(rest[2]);
        }
        for (MobAnimDef a : def.animations()) {
            float[] r = out[def.indexOf(a.part())];
            switch (a.type()) {
                case LOOK:
                    r[1] += rad(clamp(in.netHeadYaw, a.maxYaw())) * a.yawFactor();
                    r[0] += rad(clamp(in.headPitch, a.maxPitch())) * a.pitchFactor();
                    break;
                case SWING:
                    r[a.axis()] += swing(a, in);
                    break;
                case IDLE:
                    r[a.axis()] += idle(a, in);
                    break;
                case SPIN:
                    r[a.axis()] += spin(a, in);
                    break;
                default:
                    pose(a, in, r);
                    break;
            }
        }
    }

    /**
     * @param a  a swing entry
     * @param in the inputs
     * @return the angle it adds, radians
     */
    public static float swing(MobAnimDef a, AnimInput in) {
        return rad(a.amplitude()) * in.limbSwingAmount * (float) Math.cos(in.limbSwing * a.speed() + rad(a.phase()));
    }

    /**
     * @param a  a spin entry
     * @param in the inputs
     * @return the angle it adds, radians; the rate drops to {@code rate * deathScale} while dying
     */
    public static float spin(MobAnimDef a, AnimInput in) {
        float scale = in.deathProgress > 0.0F ? a.deathScale() : 1.0F;
        return rad(a.rate() * scale * in.ageInTicks);
    }

    /**
     * @param a  an idle entry
     * @param in the inputs
     * @return the angle it adds, radians
     */
    public static float idle(MobAnimDef a, AnimInput in) {
        return rad(a.offset()) + rad(a.amplitude()) * (float) Math.sin(in.ageInTicks * a.speed() + rad(a.phase()));
    }

    /**
     * The share of a pose that applies this frame.
     *
     * @param a  a pose entry
     * @param in the inputs
     * @return 0 to 1
     */
    public static float poseWeight(MobAnimDef a, AnimInput in) {
        switch (a.when()) {
            case AIMING:
                return in.aiming ? 1.0F : 0.0F;
            case HOLDING:
                return in.holding ? 1.0F : 0.0F;
            case DYING:
                return Math.max(0.0F, Math.min(1.0F, in.deathProgress));
            default:
                float p = (in.attackProgress - a.windowFrom()) / (a.windowTo() - a.windowFrom());
                if (in.attackProgress <= 0.0F || p < 0.0F || p > 1.0F) {
                    return 0.0F;
                }
                switch (a.blend()) {
                    case COSINE:
                        return 0.5F * (1.0F - (float) Math.cos(Math.PI * p));
                    case SINE:
                        return (float) Math.sin(Math.PI * p);
                    default:
                        return 1.0F;
                }
        }
    }

    private static void pose(MobAnimDef a, AnimInput in, float[] r) {
        float w = poseWeight(a, in);
        if (w <= 0.0F) {
            return;
        }
        float[] ang = a.angles();
        for (int axis = 0; axis < 3; axis++) {
            float follow = 0.0F;
            if (axis == 0) {
                follow = rad(in.headPitch) * a.followPitch();
            } else if (axis == 1) {
                follow = rad(in.netHeadYaw) * a.followYaw();
            }
            boolean has = !Float.isNaN(ang[axis]);
            if (!has && follow == 0.0F) {
                continue;
            }
            float target = (has ? rad(ang[axis]) : 0.0F) + follow;
            if (a.isSet() && has) {
                r[axis] += (target - r[axis]) * w;
            } else {
                r[axis] += target * w;
            }
        }
    }

    private static float clamp(float v, float limit) {
        return Math.max(-limit, Math.min(limit, v));
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }
}
