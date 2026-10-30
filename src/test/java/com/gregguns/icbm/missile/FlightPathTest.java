package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** D287: the vertical climb to the build height, then the arc from that waypoint to the target. */
class FlightPathTest {

    /** A silo pad 64 blocks down, a target 300 blocks south on the surface. */
    private static FlightPath siloShot(double tx, double ty, double tz, double speed) {
        return new FlightPath(10.5, 52.2, -7.5, FlightPath.waypointY(52.2, 256), tx, ty, tz, speed);
    }

    @Test
    void theWaypointIsTheBuildHeightOrTheShortestClimbAboveTheLaunchPoint() {
        assertEquals(256.0, FlightPath.waypointY(64.2, 256), 1e-9);
        assertEquals(128.0, FlightPath.waypointY(40.0, 128), 1e-9, "a dimension with a lower build height");
        assertEquals(250.0 + FlightPath.MIN_CLIMB, FlightPath.waypointY(250.0, 256), 1e-9, "a pad near the top");
    }

    @Test
    void theClimbIsStraightUpUntilTheWaypoint() {
        FlightPath f = siloShot(10.5, 70, 292.5, 0.8);
        double lastY = 52.2;
        for (int n = 0; n < f.climbTicks(); n++) {
            double[] p = f.position(n);
            assertEquals(10.5, p[0], 0.0, "no sideways move in the climb at tick " + n);
            assertEquals(-7.5, p[2], 0.0, "no sideways move in the climb at tick " + n);
            assertTrue(p[1] >= lastY, "never sinks in the climb");
            assertTrue(p[1] < f.waypointY(), "below the waypoint until the climb ends");
            lastY = p[1];
        }
        double[] top = f.position(f.climbTicks());
        assertEquals(10.5, top[0], 1e-9);
        assertEquals(256.0, top[1], 1e-9, "the arc starts at the build height");
        assertEquals(-7.5, top[2], 1e-9);
    }

    @Test
    void nothingMovesSidewaysBelowTheWaypoint() {
        // Whatever the target, every point of the flight below the build height near the launch point is on the LP's
        // column: the shaft, the doors and the roof are passed straight up.
        for (double[] t : new double[][] { { 10.5, 70, 292.5 }, { 30.5, 52, -7.5 }, { -2000, 120, 1500 } }) {
            FlightPath f = siloShot(t[0], t[1], t[2], 1.6);
            for (int n = 0; n <= f.ticks(); n++) {
                double[] p = f.position(n);
                double off = Math.hypot(p[0] - 10.5, p[2] + 7.5);
                if (off > 0.0) {
                    assertTrue(n > f.climbTicks(), "sideways only on the arc (tick " + n + ")");
                    // The first sideways step starts from the build height; the path is above every block there.
                    assertTrue(off > 2.0 || p[1] > 200.0, "it has cleared the column before it nears the ground");
                }
            }
        }
    }

    @Test
    void theClimbAcceleratesThenHoldsItsTopSpeed() {
        FlightPath f = siloShot(10.5, 70, 292.5, 0.8);
        double maxStep = 0.0;
        for (int n = 1; n < f.climbTicks(); n++) {
            maxStep = Math.max(maxStep, f.position(n)[1] - f.position(n - 1)[1]);
        }
        assertTrue(maxStep <= FlightPath.CLIMB_MAX_SPEED + 1e-9, "the climb never exceeds its top speed");
        assertEquals(FlightPath.CLIMB_MAX_SPEED, maxStep, 1e-9, "a 200-block climb reaches it");
        // 37.5 ticks to reach 3 blocks per tick over 56.25 blocks, then the other 147.55 at 3 per tick.
        assertEquals(87, FlightPath.climbTicks(256.0 - 52.2));
    }

    @Test
    void theClimbEndsOnTheFirstTickAtTheWaypoint() {
        for (double h = 0.5; h < 300.0; h += 7.3) {
            int n = FlightPath.climbTicks(h);
            assertTrue(FlightPath.climbed(n) >= h, "reached at " + n + " for " + h);
            assertTrue(n == 1 || FlightPath.climbed(n - 1) < h, "not reached a tick earlier for " + h);
        }
        assertEquals(1, FlightPath.climbTicks(0.0));
        assertEquals(1, FlightPath.climbTicks(Double.NaN));
    }

    @Test
    void theArcFromTheWaypointLandsOnTheTargetAndTheClimbAddsToTheFlightTime() {
        FlightPath f = siloShot(10.5, 70, 292.5, 0.8);
        double[] end = f.position(f.ticks());
        assertEquals(10.5, end[0], 1e-9);
        assertEquals(70.0, end[1], 1e-9);
        assertEquals(292.5, end[2], 1e-9);
        assertEquals(
            f.climbTicks() + f.arc()
                .ticks(),
            f.ticks());
        assertTrue(f.climbTicks() > 0);
        // The arc is the one from the waypoint: same flat distance as from the launch point.
        double[] arcStart = f.arc()
            .position(0);
        assertEquals(10.5, arcStart[0], 1e-9);
        assertEquals(256.0, arcStart[1], 1e-9);
        assertEquals(-7.5, arcStart[2], 1e-9);
        // Past the arrival it keeps descending, so it still hits a ground below the aim.
        assertTrue(f.position(f.ticks() + 20)[1] < 70.0);
    }

    @Test
    void aScatteredAimLandsWithinTheScatterOfTheTarget() {
        // The launch scatters the aim at the LP (siloRangeFactor and siloScatterFactor apply there); the arc from the
        // waypoint lands on that aim, so the impact is within the half-width of the requested target.
        Random rng = new Random(1234L);
        MissileTier tier = MissileTier.T2;
        double range = tier.baseMaxRange() * 1.25;
        for (int i = 0; i < 200; i++) {
            double tx = 10.5 + 2500.0 * Math.cos(i);
            double tz = -7.5 + 2500.0 * Math.sin(i);
            double dist = Math.hypot(tx - 10.5, tz + 7.5);
            double half = Scatter.halfWidth(tier.baseScatter(), 0, dist, range, 0.5);
            double ax = tx + Scatter.offset(rng, half);
            double az = tz + Scatter.offset(rng, half);
            FlightPath f = siloShot(ax, 64, az, tier.speed());
            double[] hit = f.position(f.ticks());
            assertTrue(Math.abs(hit[0] - tx) <= half + 1e-9 && Math.abs(hit[2] - tz) <= half + 1e-9, "within scatter");
            assertEquals(64.0, hit[1], 1e-9);
        }
    }

    @Test
    void aWaypointBelowTheLaunchPointIsClampedToIt() {
        FlightPath f = new FlightPath(0, 100, 0, 50, 100, 64, 0, 1.0);
        assertEquals(100.0, f.waypointY(), 1e-9);
        assertEquals(1, f.climbTicks());
    }
}
