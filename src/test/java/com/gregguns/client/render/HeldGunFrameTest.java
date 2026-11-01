package com.gregguns.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.client.render.profile.TransformProgram;

/** The item frame a held gun is drawn in: the one a player's gun gets, which every gun's third-person rows assume. */
class HeldGunFrameTest {

    private static final class Recorder implements TransformProgram.OpSink {

        final List<String> ops = new ArrayList<>();

        @Override
        public void translate(double x, double y, double z) {
            ops.add("T " + x + " " + y + " " + z);
        }

        @Override
        public void rotate(double degrees, double x, double y, double z) {
            ops.add("R " + degrees + " " + x + " " + y + " " + z);
        }

        @Override
        public void scale(double x, double y, double z) {
            ops.add("S " + x + " " + y + " " + z);
        }
    }

    @Test
    void frameIsVanillaPlayerIs3dBranch() {
        Recorder r = new Recorder();
        HeldGunFrame.apply(r);
        List<String> expected = new ArrayList<>();
        expected.add("T 0.0 0.1875 -0.3125");
        expected.add("R 20.0 1.0 0.0 0.0");
        expected.add("R 45.0 0.0 1.0 0.0");
        expected.add("S -0.375 -0.375 0.375");
        assertEquals(expected, r.ops);
    }

    @Test
    void scaleIsHalfTimesThreeQuarters() {
        assertEquals(0.5 * 0.75, HeldGunFrame.SCALE, 1e-12);
    }
}
