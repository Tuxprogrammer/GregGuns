package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MinigunProfileTest {

    private static final class Recorder implements TransformProgram.OpSink {

        final List<double[]> rotations = new ArrayList<>();

        @Override
        public void translate(double x, double y, double z) {}

        @Override
        public void rotate(double degrees, double x, double y, double z) {
            rotations.add(new double[] { degrees, x, y, z });
        }

        @Override
        public void scale(double x, double y, double z) {}
    }

    private static int count(String prefix, int part) {
        int n = 0;
        for (BoxSpec b : MinigunProfile.BOXES) {
            if (b.name()
                .startsWith(prefix) && b.part() == part) {
                n++;
            }
        }
        return n;
    }

    @Test
    void rotorHoldsSixBarrelsSixTipsAndThreeDiscs() {
        assertEquals(6, count("barrel_r", 1));
        assertEquals(6, count("barrel_f", 1));
        assertEquals(6, count("tip", 1));
        assertEquals(5, count("hub_", 1));
        assertEquals(5, count("clamp_", 1));
        assertEquals(5, count("front_", 1));
        for (BoxSpec b : MinigunProfile.BOXES) {
            if (b.part() == 1 && b.name()
                .startsWith("barrel_r")) {
                assertEquals(12, b.w());
                assertEquals(2, b.h());
                assertEquals(5f, b.x(), 1e-6);
            }
            if (b.name()
                .startsWith("tip")) {
                assertEquals(12, b.w());
                assertEquals(1, b.h());
                assertEquals(26f, b.x(), 1e-6);
            }
        }
    }

    @Test
    void barrelsSitAtSixtyDegreeSteps() {
        List<Float> angles = new ArrayList<>();
        for (BoxSpec b : MinigunProfile.BOXES) {
            if (b.name()
                .startsWith("barrel_r")) {
                angles.add(b.tiltXDegrees());
                assertEquals(-3.5f, b.y(), 1e-6, "one barrel straight up at rest, ring radius 2.5");
            }
        }
        for (int k = 0; k < 6; k++) {
            assertTrue(angles.contains(60f * k) || (k == 0 && angles.contains(0f)));
        }
    }

    @Test
    void lengthAndHeightMatchTheNotes() {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (BoxSpec b : MinigunProfile.BOXES) {
            if (b.tiltXDegrees() != 0f || b.tiltYDegrees() != 0f || b.tiltZDegrees() != 0f) {
                continue;
            }
            minX = Math.min(minX, b.x());
            maxX = Math.max(maxX, b.x() + b.w());
            minY = Math.min(minY, b.y());
            maxY = Math.max(maxY, b.y() + b.h());
        }
        assertEquals(38f, maxX, 1e-6, "tips end at X 38");
        assertEquals(-11f, minY, 1e-6, "carry handle is the highest point");
        assertEquals(10.5f, maxY, 1e-6, "drum is the lowest point");
        assertTrue(maxX - minX >= 61f);
    }

    @Test
    void definitionHasTwoPartsAndOneTexture() {
        assertEquals(2, MinigunProfile.DEFINITION.parts());
        assertEquals(1, MinigunProfile.DEFINITION.textures().length);
        assertEquals(38f / 16f, MinigunProfile.MUZZLE[0], 1e-6);
    }

    @Test
    void onlyTheRotorSpinsAndHalfATurnIs180Degrees() {
        Recorder rotor = new Recorder();
        MinigunProfile.TRANSFORMS.rotor()
            .apply(0.5, 1, rotor);
        Recorder housing = new Recorder();
        MinigunProfile.TRANSFORMS.rotor()
            .apply(0.5, 0, housing);
        assertEquals(180.0, Math.abs(rotor.rotations.get(rotor.rotations.size() - 1)[0]), 1e-6);
        assertEquals(0.0, housing.rotations.get(housing.rotations.size() - 1)[0], 1e-6);
        assertEquals(5, MinigunProfile.TRANSFORMS.recoilTicks());
        assertEquals(1.0, MinigunProfile.RECOIL_TICKS * MinigunProfile.FULL_TURNS_PER_TICK, 1e-12);
        assertEquals(0.0, MinigunProfile.rotorTurnDegrees(0.0), 0.0);
        assertEquals(360.0, MinigunProfile.rotorTurnDegrees(1.0), 1e-9);
    }

    @Test
    void rotorSpinsAboutTheBarrelAxisThroughTheClusterCentre() {
        // Spin step: last rotation of the rotor row, about model X (the barrel axis), nothing else turning with it.
        Recorder rotor = new Recorder();
        MinigunProfile.TRANSFORMS.rotor()
            .apply(0.25, 1, rotor);
        double[] spin = rotor.rotations.get(rotor.rotations.size() - 1);
        assertEquals(1.0, Math.abs(spin[1]), 1e-9, "axis is model X");
        assertEquals(0.0, spin[2], 1e-9);
        assertEquals(0.0, spin[3], 1e-9);
        Recorder third = new Recorder();
        MinigunProfile.TRANSFORMS.rotor()
            .apply(0.25, 1, third);
        double[] tspin = third.rotations.get(third.rotations.size() - 1);
        assertEquals(1.0, Math.abs(tspin[1]), 1e-9);
        // Pivot: every rotor barrel box is centred on the axis ring (radius 2.5) about the origin, which is the pivot.
        for (BoxSpec b : MinigunProfile.BOXES) {
            if (b.part() == 1 && b.name()
                .startsWith("barrel_r")) {
                assertEquals(0f, b.pivotX(), 1e-6);
                assertEquals(0f, b.pivotY(), 1e-6);
                assertEquals(0f, b.pivotZ(), 1e-6);
                assertEquals(-2.5f, b.y() + b.h() / 2f, 1e-6);
                assertEquals(0f, b.z() + b.d() / 2f, 1e-6);
            }
        }
    }
}
