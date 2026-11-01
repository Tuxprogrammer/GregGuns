package com.gregguns.icbm.missile;

/**
 * The whole powered flight of a missile as a closed-form function of the tick since ignition: a vertical climb from
 * the launch point (the LP) straight up to a waypoint at the world's build height, then the {@link ArcPath} from that
 * waypoint to the target (D287). The climb never moves sideways, so a missile leaves a silo shaft and its open doors
 * in a straight line and is above every block of the world before it turns; the arc is computed from the waypoint,
 * which stands directly above the LP, so the flat distance (and with it the range, the scatter and the fuel bonus) is
 * the one measured at the LP. The climb accelerates at {@link #CLIMB_ACCEL} up to {@link #CLIMB_MAX_SPEED}, then holds
 * that speed. Built from the description, not ported. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/flight.md (Phases, The climb)
public final class FlightPath {

    /** Climb acceleration, blocks per tick per tick. */
    public static final double CLIMB_ACCEL = 0.08;
    /** Fastest climb, blocks per tick; reached after {@code CLIMB_MAX_SPEED / CLIMB_ACCEL} ticks. */
    public static final double CLIMB_MAX_SPEED = 3.0;
    /**
     * The shortest climb, blocks: a launch point within this of the build height still climbs this far. It is also the
     * height of the column the launch checks for blocks above the pad (with the body's height on top).
     */
    public static final double MIN_CLIMB = 12.0;

    /** Ticks the climb accelerates before it holds {@link #CLIMB_MAX_SPEED}. */
    private static final double ACCEL_TICKS = CLIMB_MAX_SPEED / CLIMB_ACCEL;
    /** Height climbed by the end of the acceleration, blocks. */
    private static final double ACCEL_HEIGHT = 0.5 * CLIMB_ACCEL * ACCEL_TICKS * ACCEL_TICKS;

    private final double lx;
    private final double ly;
    private final double lz;
    private final double waypointY;
    private final int climbTicks;
    private final ArcPath arc;

    /**
     * @param lx        launch point X (the missile's column center)
     * @param ly        launch point Y (the missile's feet on the pad)
     * @param lz        launch point Z
     * @param waypointY the top of the climb, from {@link #waypointY(double, int)}
     * @param tx        target X
     * @param ty        target Y
     * @param tz        target Z
     * @param speed     horizontal blocks per tick on the arc, above 0
     */
    public FlightPath(double lx, double ly, double lz, double waypointY, double tx, double ty, double tz,
        double speed) {
        this.lx = lx;
        this.ly = ly;
        this.lz = lz;
        this.waypointY = Math.max(waypointY, ly);
        this.climbTicks = climbTicks(this.waypointY - ly);
        this.arc = new ArcPath(lx, this.waypointY, lz, tx, ty, tz, speed);
    }

    /**
     * The top of the climb: the world's build height (so the missile's feet clear the highest block the world can
     * hold), or {@link #MIN_CLIMB} above the launch point when that is higher.
     *
     * @param launchY     the launch point's Y
     * @param buildHeight the world's build height ({@code World.getHeight()}: 256 in most dimensions)
     * @return Y of the waypoint
     */
    public static double waypointY(double launchY, int buildHeight) {
        return Math.max(buildHeight, launchY + MIN_CLIMB);
    }

    /**
     * @param n ticks since ignition, 0 or more
     * @return blocks climbed by then: {@code a n^2 / 2} while accelerating, then {@link #CLIMB_MAX_SPEED} per tick
     */
    public static double climbed(int n) {
        if (n <= 0) {
            return 0.0;
        }
        if (n <= ACCEL_TICKS) {
            return 0.5 * CLIMB_ACCEL * n * n;
        }
        return ACCEL_HEIGHT + CLIMB_MAX_SPEED * (n - ACCEL_TICKS);
    }

    /**
     * @param height blocks to climb
     * @return the first tick at which {@link #climbed} reaches the height; at least 1
     */
    public static int climbTicks(double height) {
        if (!(height > 0.0)) {
            return 1;
        }
        double t = height <= ACCEL_HEIGHT ? Math.sqrt(2.0 * height / CLIMB_ACCEL)
            : ACCEL_TICKS + (height - ACCEL_HEIGHT) / CLIMB_MAX_SPEED;
        int n = Math.max(1, (int) Math.ceil(t) - 1);
        // The closed form can land one tick either side through rounding; settle it on the integers.
        while (climbed(n) < height) {
            n++;
        }
        while (n > 1 && climbed(n - 1) >= height) {
            n--;
        }
        return n;
    }

    /** @return ticks of the climb: at this tick the missile stands at the waypoint and turns onto the arc */
    public int climbTicks() {
        return climbTicks;
    }

    /** @return Y of the waypoint, the top of the climb and the start of the arc */
    public double waypointY() {
        return waypointY;
    }

    /** @return the arc from the waypoint to the target */
    public ArcPath arc() {
        return arc;
    }

    /** @return ticks from ignition until the arc reaches the target */
    public int ticks() {
        return climbTicks + arc.ticks();
    }

    /**
     * @param n ticks since ignition, 0 or more; may exceed {@link #ticks()}
     * @return x, y, z at that tick: on the LP's column during the climb, on the arc after it
     */
    public double[] position(int n) {
        if (n < climbTicks) {
            return new double[] { lx, ly + climbed(n), lz };
        }
        return arc.position(n - climbTicks);
    }
}
