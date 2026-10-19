package com.gregguns.gun.logic;

import java.util.Random;

/**
 * How one round becomes several projectiles, beyond the plain pellet spread. Pure maths over a caller-supplied
 * {@link Random}; the server rolls the offsets and the launcher loop spawns one projectile per offset.
 *
 * <p>
 * Three shapes:
 * <ul>
 * <li>{@link #burstLine(int)}: the round fires 1 + n projectiles down one line, projectile i shifted forward by
 * {@code i / n} of a tick's travel (PDW n = 1, Pulse Rifle n = 2). All share the first projectile's spread roll, so
 * they form a tight train.</li>
 * <li>{@link #ringVolley(int, int, double)}: a centre projectile plus rings; ring j holds {@code perRing * j}
 * projectiles evenly spaced around the axis at a cone angle of {@code coneRadians * j} (Sonic Rifle: 2 rings, 5 per
 * step, 0.075 rad, so 1 + 5 + 10). The spread stats, zoom and armor are ignored, as in Techguns. Every projectile
 * carries one {@link VolleyHits} so an entity is hit once per volley.</li>
 * <li>{@link #fan(int, double)}: n beams, each with its own uniform offset of +-{@code spread * 40} degrees per axis
 * (Scatter Laser Rifle: 5 beams, spread 0.15). The spread is scaled by the shooter's modifiers like any pellet.</li>
 * </ul>
 * Immutable.
 */
// Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (Sonic Rifle ring angles); projectiles.md 3.13
// Deviation: burst-line projectiles share one spread roll instead of rolling independently (a train, not a cloud).
public final class ShotPattern {

    /** The default: no pattern; {@link SpreadModel} rolls the main projectile and the pellets. */
    public static final ShotPattern NONE = new ShotPattern(Shape.NONE, 0, 0, 0.0);

    /** Pattern shapes. */
    public enum Shape {
        NONE,
        BURST_LINE,
        RING_VOLLEY,
        FAN
    }

    private final Shape shape;
    private final int count;
    private final int rings;
    private final double angle;

    private ShotPattern(Shape shape, int count, int rings, double angle) {
        this.shape = shape;
        this.count = count;
        this.rings = rings;
        this.angle = angle;
    }

    /**
     * @param extra projectiles after the first, at least 1
     * @return a burst line of 1 + extra projectiles
     */
    public static ShotPattern burstLine(int extra) {
        require(extra >= 1, "burstLine extra must be at least 1");
        return new ShotPattern(Shape.BURST_LINE, extra, 0, 0.0);
    }

    /**
     * @param rings   number of rings, at least 1
     * @param perRing projectiles in the first ring (ring j has perRing * j), at least 1
     * @param coneRad cone angle of the first ring in radians, above 0 (ring j is j times this)
     * @return a ring volley
     */
    public static ShotPattern ringVolley(int rings, int perRing, double coneRad) {
        require(rings >= 1 && perRing >= 1 && coneRad > 0, "ringVolley needs rings, perRing >= 1 and coneRad > 0");
        return new ShotPattern(Shape.RING_VOLLEY, perRing, rings, coneRad);
    }

    /**
     * @param beams  beams per round, at least 2
     * @param spread per-beam spread value (degrees = spread * 40 per axis), at least 0
     * @return a multi-beam fan
     */
    public static ShotPattern fan(int beams, double spread) {
        require(beams >= 2 && spread >= 0, "fan needs beams >= 2 and spread >= 0");
        return new ShotPattern(Shape.FAN, beams, 0, spread);
    }

    private static void require(boolean ok, String what) {
        if (!ok) {
            throw new IllegalArgumentException("Invalid ShotPattern: " + what);
        }
    }

    /** @return the shape */
    public Shape shape() {
        return shape;
    }

    /** @return true unless this is {@link #NONE} */
    public boolean isActive() {
        return shape != Shape.NONE;
    }

    /** @return how many projectiles one round makes (1 for {@link #NONE}) */
    public int projectileCount() {
        switch (shape) {
            case BURST_LINE:
                return 1 + count;
            case RING_VOLLEY:
                return 1 + count * rings * (rings + 1) / 2;
            case FAN:
                return count;
            default:
                return 1;
        }
    }

    /**
     * Rolls the offsets for one round.
     *
     * @param random the random source
     * @param stats  the gun (main spread for burst lines, hand handling for akimbo)
     * @param ctx    the shooter's modifiers
     * @param hand   the hand from {@link ShotResult#attackType()} (1 means left)
     * @return {@link #projectileCount()} offsets
     */
    public ShotOffset[] roll(Random random, GunStats stats, ShotContext ctx, int hand) {
        boolean left = stats.akimbo() && !stats.doubleShot() && hand == 1;
        ShotOffset[] out = new ShotOffset[projectileCount()];
        switch (shape) {
            case BURST_LINE: {
                ShotOffset first = SpreadModel.one(random, SpreadModel.mainSpread(stats, ctx), 0.0, left);
                out[0] = first;
                for (int i = 1; i <= count; i++) {
                    out[i] = new ShotOffset(first.yawDegrees(), first.pitchDegrees(), (double) i / count, left);
                }
                break;
            }
            case RING_VOLLEY: {
                VolleyHits shared = new VolleyHits();
                int n = 0;
                out[n++] = new ShotOffset(0.0, 0.0, 0.0, left, shared);
                for (int j = 1; j <= rings; j++) {
                    int size = count * j;
                    double cone = angle * j;
                    for (int k = 0; k < size; k++) {
                        out[n++] = ring(cone, 2.0 * Math.PI * k / size, left, shared);
                    }
                }
                break;
            }
            case FAN: {
                double spread = angle * SpreadModel.multiplier(stats, ctx);
                for (int i = 0; i < count; i++) {
                    out[i] = SpreadModel.one(random, spread, 0.0, left);
                }
                break;
            }
            default:
                out[0] = SpreadModel.one(random, SpreadModel.mainSpread(stats, ctx), 0.0, left);
        }
        return out;
    }

    /**
     * An offset {@code cone} radians off the aim axis, rotated {@code ring} radians around it. Camera frame: forward
     * is the aim, yaw is sideways, pitch is vertical.
     */
    private static ShotOffset ring(double cone, double ring, boolean left, VolleyHits shared) {
        double forward = Math.cos(cone);
        double side = Math.sin(cone) * Math.cos(ring);
        double up = Math.sin(cone) * Math.sin(ring);
        double yaw = Math.toDegrees(Math.atan2(side, forward));
        double pitch = Math.toDegrees(Math.atan2(up, Math.hypot(forward, side)));
        return new ShotOffset(yaw, pitch, 0.0, left, shared);
    }
}
