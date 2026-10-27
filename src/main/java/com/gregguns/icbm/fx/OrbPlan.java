package com.gregguns.icbm.fx;

/**
 * The numbers of the annihilation orbs, as pure functions (cheap by construction: one sphere per orb, no per-tick block
 * scan on the client, at most {@link #MAX_ACTIVE} orbs). No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Orbs)
public final class OrbPlan {

    /** Most orbs a client draws at once; a new one replaces the oldest. */
    public static final int MAX_ACTIVE = 4;
    /** The antimatter flash lasts this long, ticks. */
    public static final int FLASH_TICKS = 60;
    /** Players farther than this (blocks) from the orb are sent nothing. */
    public static final double BROADCAST_RANGE = 256.0;
    /** The sphere is built from this many latitude and longitude segments. */
    public static final int SEGMENTS = 24;

    private OrbPlan() {}

    /**
     * @param kind   the orb
     * @param radius the radius the server announced, blocks
     * @param age    ticks since the client first saw it
     * @param life   the whole length, ticks
     * @return the radius to draw, blocks: a flash swells with an ease-out curve, a body shows its radius at once
     */
    public static double drawRadius(OrbKind kind, int radius, int age, int life) {
        if (!kind.swells()) {
            return radius;
        }
        double t = Math.max(0.0, Math.min(1.0, age / (double) Math.max(1, life)));
        return radius * (1.0 - (1.0 - t) * (1.0 - t));
    }

    /**
     * @param kind the orb
     * @param age  ticks since the client first saw it
     * @param life the whole length, ticks
     * @return alpha, 0 to 1: a flash fades out, a body is steady and fades in its last 20 ticks
     */
    public static float alpha(OrbKind kind, int age, int life) {
        int left = Math.max(0, life - age);
        if (kind.swells()) {
            return Math.max(0.0F, Math.min(0.85F, left / (float) Math.max(1, life)));
        }
        return Math.max(0.0F, Math.min(0.9F, left / 20.0F * 0.9F));
    }
}
