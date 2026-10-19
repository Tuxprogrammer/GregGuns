package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Checks every M2 gun's in-hand placement against the targets of the in-hand notes (section 7), using the gun's own
 * transform rows replayed through the same item frame the renderer uses.
 */
class InHandFitTest {

    /** Replays a program's matrix calls onto an affine map. */
    private static final class Replay implements TransformProgram.OpSink {

        InHandFit.Affine map;

        Replay(InHandFit.Affine start) {
            this.map = start;
        }

        @Override
        public void translate(double x, double y, double z) {
            map = map.translate(x, y, z);
        }

        @Override
        public void rotate(double degrees, double x, double y, double z) {
            map = x != 0 ? map.rotX(degrees * x) : y != 0 ? map.rotY(degrees * y) : map.rotZ(degrees * z);
        }

        @Override
        public void scale(double x, double y, double z) {
            map = map.scale(x, y, z);
        }
    }

    private static double dist(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < 3; i++) {
            s += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return Math.sqrt(s);
    }

    /** The gun frame point to the first-person camera coordinates, through the gun's own rows. */
    private static double[] fpCamera(ClientGunDefinition d, double[] gunPoint) {
        Replay r = new Replay(InHandFit.firstPersonItemFrame());
        d.transforms()
            .fpBase()
            .apply(0.0, 0, r);
        d.transforms()
            .fpIdle()
            .apply(0.0, 0, r);
        return InHandFit.cameraOf(r.map.apply(gunPoint));
    }

    /** The gun frame point to the hand frame in H-blocks, through the aimed rotation and the gun's own rows. */
    private static double[] tpHand(ClientGunDefinition d, double[] gunPoint) {
        Replay r = new Replay(
            InHandFit.Affine.identity()
                .translate(-0.5, -0.5, -0.5));
        d.transforms()
            .applyAimed(r);
        d.transforms()
            .tpBase()
            .apply(0.0, 0, r);
        return r.map.apply(gunPoint);
    }

    @Test
    void theFrameReproducesTheTechgunsHandCannonNumbers() {
        // The notes' measured Techguns Hand Cannon: x -17..22, y -3..5, z -2..3 texels, S 2.5, T (0.1, 0, -0.5),
        // rotY -45. Centre (0.75, -0.78, 0.98) and muzzle depth 2.20 in camera blocks.
        InHandFit.Affine f = InHandFit.firstPersonFrame(-45, 2.5);
        double[] t = { 0.1, 0, -0.5 };
        double[] c = InHandFit.cameraOf(f.apply(new double[] { 2.5 / 16 + t[0], 1.0 / 16 + t[1], 0.5 / 16 + t[2] }));
        double[] m = InHandFit.cameraOf(f.apply(new double[] { 22.0 / 16 + t[0], 1.0 / 16 + t[1], 0.5 / 16 + t[2] }));
        assertEquals(0.75, c[0], 0.01);
        assertEquals(-0.78, c[1], 0.01);
        assertEquals(0.98, c[2], 0.01);
        assertEquals(2.20, m[2], 0.01);
    }

    @Test
    void everyGunMeetsItsFirstPersonTargets() {
        int checked = 0;
        for (ClientGunDefinition d : ClientGunDefinitions.ALL) {
            InHandTargets.Target t = InHandTargets.of(d.gunName());
            if (t == null || d.akimbo()) {
                continue;
            }
            checked++;
            boolean alongX = !InHandTestSupport.zFamily(d);
            double[][] b = InHandFit.gunFrameBounds(InHandTestSupport.fitBoxes(d), d.modelScale(), alongX);
            double[] centre = fpCamera(d, InHandFit.mid(b));
            double[] front = fpCamera(d, InHandFit.muzzlePoint(b, alongX));
            double[] rear = fpCamera(d, InHandFit.rearPoint(b, alongX));
            String n = d.gunName();
            assertEquals(t.fpLength(), dist(front, rear), 0.1 * t.fpLength(), n + " first-person length");
            for (int i = 0; i < 3; i++) {
                assertEquals(t.fpCentre()[i], centre[i], 0.1, n + " centre axis " + i);
            }
            // The muzzle: the profile's own muzzle point, which is the barrel tip rather than the box slice.
            float[] m0 = d.muzzle();
            double[] muzzle = fpCamera(d, new double[] { m0[0], m0[1], m0[2] });
            assertEquals(t.fpMuzzle()[0], muzzle[0], 0.15, n + " muzzle right");
            assertEquals(t.fpMuzzle()[1], muzzle[1], 0.15, n + " muzzle up");
            assertEquals(t.fpMuzzle()[2], muzzle[2], 0.1 * t.fpLength(), n + " muzzle depth");
        }
        int onTable = 0;
        for (String ignored : InHandTargets.names()) {
            onTable++;
        }
        assertEquals(onTable, checked, "every gun on the table is registered and was checked");
        assertTrue(checked >= 24, "the M2 guns, the M5 tool guns and the Bio Gun are on the table");
    }

    @Test
    void longGunButtsReachTheCameraPlaneAndHandgunsStayVisible() {
        for (ClientGunDefinition d : ClientGunDefinitions.ALL) {
            InHandTargets.Target t = InHandTargets.of(d.gunName());
            if (t == null || d.akimbo()) {
                continue;
            }
            boolean alongX = !InHandTestSupport.zFamily(d);
            double[][] b = InHandFit.gunFrameBounds(InHandTestSupport.fitBoxes(d), d.modelScale(), alongX);
            double rear = fpCamera(d, InHandFit.rearPoint(b, alongX))[2];
            if (t.fpLength() >= 2.0) {
                assertTrue(rear < 0.8, d.gunName() + " butt depth " + rear);
            } else if (t.fpLength() <= 1.1) {
                assertTrue(rear > 0.5, d.gunName() + " stays fully visible, rear depth " + rear);
            }
        }
    }

    @Test
    void everyGunMeetsItsThirdPersonLengthAndSitsOnTheHand() {
        for (ClientGunDefinition d : ClientGunDefinitions.ALL) {
            InHandTargets.Target t = InHandTargets.of(d.gunName());
            if (t == null || d.akimbo()) {
                continue;
            }
            boolean alongX = !InHandTestSupport.zFamily(d);
            double[][] b = InHandFit.gunFrameBounds(InHandTestSupport.fitBoxes(d), d.modelScale(), alongX);
            double[] front = tpHand(d, InHandFit.muzzlePoint(b, alongX));
            double[] rear = tpHand(d, InHandFit.rearPoint(b, alongX));
            double lengthWorld = dist(front, rear) * InHandFit.TP_FRAME_SCALE;
            assertEquals(t.tpLength(), lengthWorld, 0.1 * t.tpLength(), d.gunName() + " third-person length");
            double fromHand = dist(tpHand(d, InHandFit.mid(b)), new double[3]) * InHandFit.TP_FRAME_SCALE;
            assertTrue(fromHand < 0.25, d.gunName() + " box centre is " + fromHand + " world blocks from the hand");
        }
    }

    @Test
    void theInfiltratorUsesTheM4sRatio() {
        InHandTargets.Target inf = InHandTargets.of("infiltrator");
        InHandTargets.Target m4 = InHandTargets.of("m4");
        assertNotNull(inf);
        assertEquals(m4.fpLength() / 69.0, inf.fpLength() / 76.0, 0.01);
    }

    @Test
    void akimboLeftGunIsTheMirrorAtEqualDepthAndHeight() {
        for (String single : new String[] { "pistol_9mm", "revolver", "mac10", "golden_revolver" }) {
            ClientGunDefinition d = ClientGunDefinitions.byName(single);
            double[][] b = InHandFit.gunFrameBounds(d.boxes(), d.modelScale(), !InHandTestSupport.zFamily(d));
            Replay r = new Replay(InHandFit.Affine.identity());
            d.transforms()
                .fpBase()
                .apply(0.0, 0, r);
            d.transforms()
                .fpIdle()
                .apply(0.0, 0, r);
            double[] itemPoint = r.map.apply(InHandFit.mid(b));
            double[] right = InHandFit.cameraOf(
                InHandFit.firstPersonItemFrame()
                    .apply(itemPoint));
            double[] left = InHandFit.cameraOf(
                InHandFit.firstPersonItemFrame()
                    .apply(AkimboLayout.mirrored(itemPoint, 1)));
            assertEquals(-right[0], left[0], 1e-9, single);
            assertEquals(right[1], left[1], 1e-9, single);
            assertEquals(right[2], left[2], 1e-9, single);
            assertTrue(left[2] > 0.5, single + " left gun is in front of the camera");
        }
    }

    @Test
    void theFlashOffsetPutsTheSpriteOnTheMuzzle() {
        ClientGunDefinition d = ClientGunDefinitions.byName("handcannon");
        InHandFit.Placement p = InHandTestSupport.placement(d);
        double[] off = p.flashOffset(d.muzzle(), 1.85);
        double[] viaFlash = InHandFit.flashFrame(1.85)
            .apply(off);
        float[] m = d.muzzle();
        double[] viaGun = InHandFit.firstPersonFrame(-45, p.fpScale)
            .apply(new double[] { m[0] + p.fpIdle[0], m[1] + p.fpIdle[1], m[2] + p.fpIdle[2] });
        for (int i = 0; i < 3; i++) {
            assertEquals(viaGun[i], viaFlash[i], 1e-9);
        }
    }
}
