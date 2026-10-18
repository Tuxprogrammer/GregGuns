package com.gregguns.projectile;

/**
 * The numbers of a thrown grenade as pure logic: the throw charge, launch speed and gravity, the fuse, and what a
 * block contact does (bounce, come to rest or detonate). No Minecraft types, both sides.
 *
 * <p>
 * Deviations from Techguns, recorded in {@code docs/gregguns-spec/04-projectiles-fx/grenades.md}: gravity never
 * divides by zero (GUN-22: the charge is floored at {@link #MIN_CHARGE}); a contact slower than {@link #REST_SPEED}
 * neither costs a bounce nor detonates, so a grenade that settles waits for its fuse instead of spending its bounces
 * in a few ticks.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.7); docs/techguns-spec/02-guns/per-gun-behavior.md
public final class GrenadeMath {

    /** Ticks from throw to detonation. */
    public static final int FUSE_TICKS = 200;
    /** Hard bounces a Stielgranate survives. */
    public static final int STIELGRANATE_BOUNCES = 3;
    /** Ticks from throw to detonation of a Frag Grenade. */
    public static final int FRAG_FUSE_TICKS = 120;
    /** Hard bounces a Frag Grenade survives. */
    public static final int FRAG_BOUNCES = 2;
    /** Ticks from throw to detonation of a Flashbang. */
    public static final int FLASHBANG_FUSE_TICKS = 40;
    /** Hard bounces a Flashbang survives. */
    public static final int FLASHBANG_BOUNCES = 2;
    /** Share of the reflected speed that is kept per bounce. */
    public static final double BOUNCE_SPEED_FACTOR = 0.5;
    /** Speed into the surface, blocks per tick, below which a contact is a rest rather than a bounce. */
    public static final double REST_SPEED = 0.08;
    /** Share of the sliding speed kept by a grenade that settles on a surface. */
    public static final double REST_FRICTION = 0.7;
    /** The smallest charge used for gravity. */
    public static final double MIN_CHARGE = 0.1;
    /** Ticks of holding for a full throw. */
    public static final double FULL_CHARGE_TICKS = 20.0;
    /** Base gravity of a full throw, blocks per tick squared. */
    public static final double BASE_GRAVITY = 0.015;

    /** What a contact with a block does. */
    public enum Contact {
        /** Reflect, damp, and lose one bounce. */
        BOUNCE,
        /** Stop moving and wait for the fuse. */
        REST,
        /** Out of bounces: detonate. */
        DETONATE
    }

    private GrenadeMath() {}

    /**
     * The vanilla bow curve.
     *
     * @param heldTicks ticks the use button was held
     * @return the charge, 0 to 1 (full at 20 ticks)
     */
    public static double charge(int heldTicks) {
        double f = Math.max(0, heldTicks) / FULL_CHARGE_TICKS;
        f = (f * f + 2.0 * f) / 3.0;
        return Math.min(1.0, f);
    }

    /**
     * @param charge the throw charge, 0 to 1
     * @return launch speed, blocks per tick: {@code 0.5 + 0.5 * charge}
     */
    public static double launchSpeed(double charge) {
        return 0.5 + 0.5 * charge;
    }

    /**
     * @param charge the throw charge, 0 to 1
     * @return gravity, blocks per tick squared: {@code 0.015 / charge}, with the charge floored (GUN-22)
     */
    public static double gravity(double charge) {
        return BASE_GRAVITY / Math.max(charge, MIN_CHARGE);
    }

    /**
     * @param fuseTicksLeft ticks until detonation, counted down once per tick
     * @return true on the tick the grenade detonates
     */
    public static boolean fuseExpired(int fuseTicksLeft) {
        return fuseTicksLeft <= 0;
    }

    /**
     * Classifies a contact.
     *
     * @param bouncesLeft      bounces remaining
     * @param speedIntoSurface the velocity component into the surface, blocks per tick, zero or more
     * @return what the grenade does
     */
    public static Contact contact(int bouncesLeft, double speedIntoSurface) {
        if (speedIntoSurface < REST_SPEED) {
            return Contact.REST;
        }
        return bouncesLeft <= 0 ? Contact.DETONATE : Contact.BOUNCE;
    }

    /**
     * The face normal for a vanilla {@code sideHit} (0 down, 1 up, 2 north, 3 south, 4 west, 5 east).
     *
     * @param side the side index
     * @return {x, y, z} of the unit normal pointing out of the block
     */
    public static int[] faceNormal(int side) {
        switch (side) {
            case 0:
                return new int[] { 0, -1, 0 };
            case 1:
                return new int[] { 0, 1, 0 };
            case 2:
                return new int[] { 0, 0, -1 };
            case 3:
                return new int[] { 0, 0, 1 };
            case 4:
                return new int[] { -1, 0, 0 };
            case 5:
                return new int[] { 1, 0, 0 };
            default:
                throw new IllegalArgumentException("side " + side);
        }
    }

    /**
     * Reflects a velocity off a face and damps it: {@code r = (v - 2 (v.n) n) * factor}.
     *
     * @param vx     velocity X
     * @param vy     velocity Y
     * @param vz     velocity Z
     * @param normal the unit face normal from {@link #faceNormal}
     * @return the new {x, y, z} velocity
     */
    public static double[] bounce(double vx, double vy, double vz, int[] normal) {
        double dot = vx * normal[0] + vy * normal[1] + vz * normal[2];
        return new double[] { (vx - 2.0 * dot * normal[0]) * BOUNCE_SPEED_FACTOR,
            (vy - 2.0 * dot * normal[1]) * BOUNCE_SPEED_FACTOR, (vz - 2.0 * dot * normal[2]) * BOUNCE_SPEED_FACTOR };
    }

    /**
     * @param vx     velocity X
     * @param vy     velocity Y
     * @param vz     velocity Z
     * @param normal the unit face normal pointing out of the block
     * @return the speed into the surface, zero when moving away
     */
    public static double speedIntoSurface(double vx, double vy, double vz, int[] normal) {
        return Math.max(0.0, -(vx * normal[0] + vy * normal[1] + vz * normal[2]));
    }

    /**
     * The velocity of a grenade that settles: the part into the surface is removed and the sliding part is slowed, so
     * it rolls to a stop over a few ticks.
     *
     * @param vx     velocity X
     * @param vy     velocity Y
     * @param vz     velocity Z
     * @param normal the unit face normal pointing out of the block
     * @return the new {x, y, z} velocity
     */
    public static double[] settle(double vx, double vy, double vz, int[] normal) {
        double dot = vx * normal[0] + vy * normal[1] + vz * normal[2];
        return new double[] { (vx - dot * normal[0]) * REST_FRICTION, (vy - dot * normal[1]) * REST_FRICTION,
            (vz - dot * normal[2]) * REST_FRICTION };
    }
}
