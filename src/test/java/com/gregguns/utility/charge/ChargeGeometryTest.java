package com.gregguns.utility.charge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The tunnel shape: blast direction, columns of each radius, cell positions and the plate bounds. */
class ChargeGeometryTest {

    @Test
    void blastDirectionPointsIntoTheMountingBlock() {
        assertArrayEquals(new int[] { 0, 1, 0 }, ChargeGeometry.direction(0));
        assertArrayEquals(new int[] { 0, -1, 0 }, ChargeGeometry.direction(1));
        assertArrayEquals(new int[] { 0, 0, 1 }, ChargeGeometry.direction(2));
        assertArrayEquals(new int[] { 0, 0, -1 }, ChargeGeometry.direction(3));
        assertArrayEquals(new int[] { 1, 0, 0 }, ChargeGeometry.direction(4));
        assertArrayEquals(new int[] { -1, 0, 0 }, ChargeGeometry.direction(5));
    }

    @Test
    void anOutOfRangeOrientationReadsAsAFloorCharge() {
        assertArrayEquals(ChargeGeometry.direction(1), ChargeGeometry.direction(9));
        assertArrayEquals(ChargeGeometry.direction(1), ChargeGeometry.direction(-3));
    }

    @Test
    void columnCountsFollowTheRadiusTable() {
        assertEquals(
            9,
            ChargeGeometry.columns(3)
                .size());
        assertEquals(
            21,
            ChargeGeometry.columns(4)
                .size());
        assertEquals(
            25,
            ChargeGeometry.columns(5)
                .size());
        assertEquals(
            45,
            ChargeGeometry.columns(6)
                .size());
        assertEquals(
            49,
            ChargeGeometry.columns(7)
                .size());
    }

    @Test
    void evenRadiiDropExactlyTheFourCorners() {
        for (int radius : new int[] { 4, 6 }) {
            int k = radius / 2;
            Set<String> have = keys(ChargeGeometry.columns(radius));
            for (int u : new int[] { -k, k }) {
                for (int v : new int[] { -k, k }) {
                    assertFalse(have.contains(u + "," + v), radius + " drops corner " + u + "," + v);
                }
            }
            assertTrue(have.contains(k + ",0") && have.contains("0," + k));
        }
        // Odd radii keep their corners.
        assertTrue(keys(ChargeGeometry.columns(5)).contains("2,2"));
    }

    @Test
    void stepOneIsTheBlockBeyondTheCharge() {
        // A charge on a floor at (10, 64, 10) blasts down: step 1 of the centre column is directly below it.
        assertArrayEquals(new int[] { 10, 63, 10 }, ChargeGeometry.cell(1, 10, 64, 10, 0, 0, 1));
        assertArrayEquals(new int[] { 10, 61, 10 }, ChargeGeometry.cell(1, 10, 64, 10, 0, 0, 3));
        assertArrayEquals(new int[] { 11, 63, 12 }, ChargeGeometry.cell(1, 10, 64, 10, 1, 2, 1));
        // On a north-facing wall (orientation 2) the blast goes +Z and the across-axes are X and Y.
        assertArrayEquals(new int[] { 11, 65, 11 }, ChargeGeometry.cell(2, 10, 64, 10, 1, 1, 1));
        // West-face charge (orientation 4) blasts +X; across-axes are Y and Z.
        assertArrayEquals(new int[] { 11, 65, 11 }, ChargeGeometry.cell(4, 10, 64, 10, 1, 1, 1));
    }

    @Test
    void everyColumnStaysInThePlaneThroughTheCharge() {
        for (int o = 0; o < 6; o++) {
            int[] d = ChargeGeometry.direction(o);
            for (int[] c : ChargeGeometry.columns(7)) {
                int[] p0 = ChargeGeometry.cell(o, 0, 0, 0, c[0], c[1], 0);
                assertEquals(0, p0[0] * d[0] + p0[1] * d[1] + p0[2] * d[2], "orientation " + o);
            }
        }
    }

    @Test
    void theSelectionBoxIsAPlateFlushAgainstTheMountingFace() {
        double[] floor = ChargeGeometry.bounds(1);
        assertArrayEquals(new double[] { 0.2, 0.0, 0.2, 0.8, 0.3, 0.8 }, floor, 1e-9);
        double[] ceiling = ChargeGeometry.bounds(0);
        assertArrayEquals(new double[] { 0.2, 0.7, 0.2, 0.8, 1.0, 0.8 }, ceiling, 1e-9);
        // Every orientation's plate touches the cell face in the blast direction.
        for (int o = 0; o < 6; o++) {
            int[] d = ChargeGeometry.direction(o);
            double[] b = ChargeGeometry.bounds(o);
            for (int axis = 0; axis < 3; axis++) {
                if (d[axis] > 0) {
                    assertEquals(1.0, b[axis + 3], 1e-9);
                } else if (d[axis] < 0) {
                    assertEquals(0.0, b[axis], 1e-9);
                }
            }
        }
    }

    private static Set<String> keys(List<int[]> columns) {
        Set<String> out = new HashSet<>();
        for (int[] c : columns) {
            out.add(c[0] + "," + c[1]);
        }
        return out;
    }
}
