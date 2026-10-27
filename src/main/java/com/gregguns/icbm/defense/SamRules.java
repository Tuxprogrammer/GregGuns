package com.gregguns.icbm.defense;

/**
 * The surface-to-air missile and the battery that holds it as plain data (D15, D92): flight numbers, damage, the
 * battery's capacity, and the rule that decides how many more SAMs a threat needs. The SAM steers straight at its
 * target each tick (lead pursuit toward the predicted intercept point, D126) and is clean-room: no ported code. No
 * Minecraft types, both
 * sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (Numbers)
public final class SamRules {

    /**
     * SAM flight speed, blocks per tick (D126). Above the steepest dive any missile makes: the end of an arc moves at
     * {@code speed * (80 / flat + 1.2)} (see {@code ArcPath.terminalSpeed}), which is 8.1 for a T3 on a level 20-block
     * flight, the shortest a missile may go; a unit test holds this above that for every tier. A fried missile falls
     * at 3 at most.
     */
    public static final double SPEED = 9.0;
    /** Ticks a SAM's range to its target may fail to fall before the battery stops counting it as a defense. */
    public static final int STALL_TICKS = 20;
    /** Longest lead the SAM aims, ticks: a missile's path bends, so a long extrapolation is not trusted. */
    public static final int MAX_LEAD_TICKS = 40;
    /** The SAM bursts when it comes this close to its target, blocks. */
    public static final double KILL_RADIUS = 3.5;
    /** Ticks a SAM flies before it fizzles. */
    public static final int LIFETIME = 300;
    /** Damage one SAM does to a missile; T1 (20 hit points) and T2 (40) fall to one, T3 (60) needs two. */
    public static final float DAMAGE = 40.0F;
    /** SAM slots in the battery. */
    public static final int CAPACITY = 4;
    /** Ticks between launches. */
    public static final int FIRE_INTERVAL = 20;
    /** EU one launch costs, paid when the SAM leaves. */
    public static final int EU_PER_SHOT = 400;
    /** The battery engages threats within this distance, blocks. */
    public static final double ENGAGE_RANGE = 256.0;
    /** Ticks between the battery's looks at its radar. */
    public static final int THINK_TICKS = 5;

    private SamRules() {}

    /**
     * How many more SAMs a threat needs, counting those already in the air at it.
     *
     * @param health       the missile's hit points now
     * @param samsInFlight SAMs already chasing it that are still closing (a SAM whose range has not fallen for
     *                     {@link #STALL_TICKS} no longer counts)
     * @return 0 when the SAMs in the air will kill it, else how many more to send
     */
    public static int stillNeeded(float health, int samsInFlight) {
        float left = health - samsInFlight * DAMAGE;
        if (left <= 0.0F) {
            return 0;
        }
        return (int) Math.ceil(left / DAMAGE);
    }

    /**
     * @param sx SAM X
     * @param sy SAM Y
     * @param sz SAM Z
     * @param tx target X
     * @param ty target Y
     * @param tz target Z
     * @return the next position: one {@link #SPEED} step toward the target, or the target itself when it is closer
     */
    public static double[] step(double sx, double sy, double sz, double tx, double ty, double tz) {
        double dx = tx - sx;
        double dy = ty - sy;
        double dz = tz - sz;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d <= SPEED) {
            return new double[] { tx, ty, tz };
        }
        double f = SPEED / d;
        return new double[] { sx + dx * f, sy + dy * f, sz + dz * f };
    }

    /**
     * Lead pursuit (D126): the point where a SAM of {@link #SPEED} meets a target moving in a straight line. Solves
     * {@code |r + v t| = SPEED t} for the smallest positive {@code t}, with {@code r} the target relative to the SAM.
     * Falls back to the target's own position when there is no solution (a target faster than the SAM), and caps the
     * lead at {@link #MAX_LEAD_TICKS}.
     *
     * @param sx SAM X
     * @param sy SAM Y
     * @param sz SAM Z
     * @param tx target X
     * @param ty target Y
     * @param tz target Z
     * @param vx target velocity X, blocks per tick
     * @param vy target velocity Y
     * @param vz target velocity Z
     * @return x, y, z of the point to fly at
     */
    public static double[] intercept(double sx, double sy, double sz, double tx, double ty, double tz, double vx,
        double vy, double vz) {
        double rx = tx - sx;
        double ry = ty - sy;
        double rz = tz - sz;
        double a = vx * vx + vy * vy + vz * vz - SPEED * SPEED;
        double b = 2.0 * (rx * vx + ry * vy + rz * vz);
        double c = rx * rx + ry * ry + rz * rz;
        double t = 0.0;
        if (a < -1e-9) {
            double disc = b * b - 4.0 * a * c;
            if (disc >= 0.0) {
                // a < 0 and c >= 0: the product of the roots c / a is not positive, so the larger root is positive.
                t = (-b - Math.sqrt(disc)) / (2.0 * a);
            }
        }
        t = Math.max(0.0, Math.min(MAX_LEAD_TICKS, t));
        return new double[] { tx + vx * t, ty + vy * t, tz + vz * t };
    }

    /**
     * The closest the SAM and its target come during one tick, both moving in a straight line between their
     * positions at the start and the end of the tick. At the 9 blocks a tick the SAM flies, a head-on pass can skip
     * the burst sphere between two samples, so the burst tests this and not only the end positions.
     *
     * @param dx0 SAM minus target, X, at the start of the tick
     * @param dy0 Y
     * @param dz0 Z
     * @param dx1 the same at the end of the tick
     * @param dy1 Y
     * @param dz1 Z
     * @return the smallest distance between them in the tick
     */
    public static double closestApproach(double dx0, double dy0, double dz0, double dx1, double dy1, double dz1) {
        double ex = dx1 - dx0;
        double ey = dy1 - dy0;
        double ez = dz1 - dz0;
        double len2 = ex * ex + ey * ey + ez * ez;
        double u = len2 < 1e-12 ? 0.0 : -(dx0 * ex + dy0 * ey + dz0 * ez) / len2;
        u = Math.max(0.0, Math.min(1.0, u));
        double px = dx0 + ex * u;
        double py = dy0 + ey * u;
        double pz = dz0 + ez * u;
        return Math.sqrt(px * px + py * py + pz * pz);
    }

    /**
     * @param distance the distance from the SAM to its target
     * @return true when the SAM bursts
     */
    public static boolean bursts(double distance) {
        return distance <= KILL_RADIUS;
    }

    /**
     * The checks a computer-ordered shot passes, in order, the same ones the battery's own engagement applies.
     *
     * @param moduleOn    the ICBM module is on
     * @param linked      a stick with a radar link is in the slot
     * @param radarOnline the linked radar is loaded and online
     * @param sams        SAMs loaded
     * @param enoughEu    the battery holds {@link #EU_PER_SHOT}
     * @param cooldown    ticks until the next launch
     * @param threat      the contact is in the linked radar's incoming list
     * @param inRange     the contact is within {@link #ENGAGE_RANGE}
     * @return the refusal word, or null when the shot may go
     */
    public static String fireRefusal(boolean moduleOn, boolean linked, boolean radarOnline, int sams, boolean enoughEu,
        int cooldown, boolean threat, boolean inRange) {
        if (!moduleOn) {
            return "module_off";
        }
        if (!linked || !radarOnline) {
            return "no_link";
        }
        if (sams <= 0) {
            return "no_sams";
        }
        if (!threat) {
            return "no_contact";
        }
        if (!inRange) {
            return "out_of_range";
        }
        if (!enoughEu) {
            return "no_power";
        }
        return cooldown > 0 ? "cooling" : null;
    }

    /** What the battery reports; one lang key per value, {@code gregguns.icbm.battery.state.<name lowercased>}. */
    public enum State {

        /** Not enough EU for a shot. */
        NO_POWER,
        /** No data stick with a radar link in the slot. */
        NO_LINK,
        /** The linked radar is not loaded, not a radar, or off. */
        LINK_LOST,
        /** The battery holds no SAM. */
        NO_SAMS,
        /** Listening, no threat. */
        STANDBY,
        /** A threat is in range and SAMs are on their way or being sent. */
        ENGAGING;

        /** @return the lang key of the state's name */
        public String langKey() {
            return "gregguns.icbm.battery.state." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
