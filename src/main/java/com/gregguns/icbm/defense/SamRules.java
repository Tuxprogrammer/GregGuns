package com.gregguns.icbm.defense;

/**
 * The surface-to-air missile and the battery that holds it as plain data (D15, D92): flight numbers, damage, the
 * battery's capacity, and the rule that decides how many more SAMs a threat needs. The SAM steers straight at its
 * target each tick (pure pursuit, as ICBM-Classic's does) and is clean-room: no ported code. No Minecraft types, both
 * sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (Numbers)
public final class SamRules {

    /** SAM flight speed, blocks per tick; faster than every missile tier (T3 flies 1.6). */
    public static final double SPEED = 3.0;
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
     * @param samsInFlight SAMs already chasing it
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
     * @param distance the distance from the SAM to its target
     * @return true when the SAM bursts
     */
    public static boolean bursts(double distance) {
        return distance <= KILL_RADIUS;
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
