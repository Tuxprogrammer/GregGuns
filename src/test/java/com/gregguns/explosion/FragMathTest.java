package com.gregguns.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FragMathTest {

    @Test
    void everyDirectionIsAUnitVector() {
        for (int i = 0; i < FragMath.COUNT; i++) {
            double[] d = FragMath.direction(i, FragMath.COUNT);
            assertEquals(1.0, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]), 1.0E-9, "fragment " + i);
        }
    }

    @Test
    void theSpreadIsEvenAcrossTheSphere() {
        double sx = 0;
        double sy = 0;
        double sz = 0;
        int up = 0;
        for (int i = 0; i < FragMath.COUNT; i++) {
            double[] d = FragMath.direction(i, FragMath.COUNT);
            sx += d[0];
            sy += d[1];
            sz += d[2];
            if (d[1] > 0) {
                up++;
            }
        }
        // The centroid of an even spread sits at the middle, and half the fragments go up.
        assertEquals(0.0, sx / FragMath.COUNT, 0.05);
        assertEquals(0.0, sy / FragMath.COUNT, 0.05);
        assertEquals(0.0, sz / FragMath.COUNT, 0.05);
        assertEquals(FragMath.COUNT / 2, up);
    }

    @Test
    void theSpreadIsDeterministic() {
        assertEquals(FragMath.direction(17, 64)[0], FragMath.direction(17, 64)[0], 0.0);
        assertEquals(FragMath.direction(17, 64)[2], FragMath.direction(17, 64)[2], 0.0);
    }

    @Test
    void noTwoFragmentsShareADirection() {
        for (int i = 0; i < FragMath.COUNT; i++) {
            for (int j = i + 1; j < FragMath.COUNT; j++) {
                double[] a = FragMath.direction(i, FragMath.COUNT);
                double[] b = FragMath.direction(j, FragMath.COUNT);
                double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
                assertTrue(dot < 0.999, i + " and " + j);
            }
        }
    }

    @Test
    void outOfRangeIndexesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> FragMath.direction(-1, 64));
        assertThrows(IllegalArgumentException.class, () -> FragMath.direction(64, 64));
        assertThrows(IllegalArgumentException.class, () -> FragMath.direction(0, 0));
    }

    @Test
    void fragmentDamageFallsLinearlyToTheMinimumAtRange() {
        assertEquals(FragMath.DAMAGE_MAX, FragMath.damage(0.0), 1.0E-9);
        assertEquals(2.0, FragMath.damage(5.0), 1.0E-9);
        assertEquals(FragMath.DAMAGE_MIN, FragMath.damage(FragMath.RANGE), 1.0E-9);
        assertEquals(0.0, FragMath.damage(FragMath.RANGE + 0.1), 0.0);
        assertEquals(0.0, FragMath.damage(Double.NaN), 0.0);
    }

    @Test
    void aBodySizedTargetCloseUpIsHitByManyFragmentsAndFarOffByFew() {
        // A 0.6 x 1.8 block target: the share of the sphere it covers falls with the square of the distance.
        double area = 0.6 * 1.8;
        double close = FragMath.COUNT * area / (4.0 * Math.PI * 1.5 * 1.5);
        double far = FragMath.COUNT * area / (4.0 * Math.PI * 8.0 * 8.0);
        assertTrue(close > 2.0, "close " + close);
        assertTrue(far < 0.2, "far " + far);
    }
}
