package com.gregguns.client.render.profile;

/**
 * The offset math of aiming down the sights: where a gun's sights are on screen in the normal hold, and how far to
 * shift the gun so they land on the screen center. Pure; no Minecraft types, so a unit test runs the same arithmetic
 * the renderer does (and checks the sights really land at the center).
 *
 * <h2>How it works</h2>
 * The sight point is the center of the named sight boxes, in the model's own axes ({@link #sightPoint}). The gun's
 * first-person placement is the shared item frame ({@link InHandFit#firstPersonItemFrame}) followed by the profile's
 * {@code fpBase} and {@code fpIdle} rows; evaluating those rows gives the sight point in eye space ({@link #eyeOf}).
 * The wanted eye-space point is straight ahead: the same depth, x and y zero. The shift between them, taken back into
 * the item frame, is one translation the renderer applies before the gun's own rows ({@link #pose}), scaled by the
 * blend (0 to 1). Keeping the depth means the gun neither grows nor shrinks, it only slides to the middle.
 *
 * <p>
 * Spec: docs/gregguns-spec/03-gun-visuals-audio/ads.md.
 */
public final class AdsOffset {

    private AdsOffset() {}

    /**
     * @param boxes      the model boxes
     * @param modelScale GL units per texel
     * @param spec       the gun's ADS data
     * @return the sight point in the model's axes, blocks: the center of the sight boxes plus the spec's nudge
     * @throws IllegalArgumentException if a named box does not exist
     */
    public static double[] sightPoint(BoxSpec[] boxes, float modelScale, AdsSpec spec) {
        double[] lo = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        double[] hi = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        for (String name : spec.sightBoxes()) {
            BoxSpec b = boxNamed(boxes, name, spec);
            double[] a = { b.x(), b.y(), b.z() };
            double[] c = { b.x() + b.w(), b.y() + b.h(), b.z() + b.d() };
            for (int i = 0; i < 3; i++) {
                lo[i] = Math.min(lo[i], a[i]);
                hi[i] = Math.max(hi[i], c[i]);
            }
        }
        double[] nudge = spec.nudgeTexels();
        double[] p = new double[3];
        for (int i = 0; i < 3; i++) {
            p[i] = ((lo[i] + hi[i]) / 2.0 + nudge[i]) * modelScale;
        }
        return p;
    }

    /** @return the part index the sight boxes belong to (the first named box's part) */
    public static int sightPart(BoxSpec[] boxes, AdsSpec spec) {
        return boxNamed(boxes, spec.sightBoxes()[0], spec).part();
    }

    private static BoxSpec boxNamed(BoxSpec[] boxes, String name, AdsSpec spec) {
        for (BoxSpec b : boxes) {
            if (b.name()
                .equals(name)) {
                return b;
            }
        }
        throw new IllegalArgumentException("gun " + spec.gunName() + " has no box named " + name);
    }

    /**
     * @param t     the gun's transform rows
     * @param part  the part the sights are on
     * @param point a point in the model's axes, blocks
     * @return that point in eye space (x right, y up, negative z forward) in the normal idle hold
     */
    public static double[] eyeOf(GunTransforms t, int part, double[] point) {
        return eyeOf(t, part, point, true);
    }

    /**
     * As {@link #eyeOf(GunTransforms, int, double[])}, for either model family: a Z-family model is drawn turned a
     * quarter turn about Y after its rows ({@code BoxGunModel#render}), which moves the sights.
     *
     * @param xFamily true if the model is drawn as authored (the muzzle toward +X)
     */
    public static double[] eyeOf(GunTransforms t, int part, double[] point, boolean xFamily) {
        final InHandFit.Affine[] frame = { InHandFit.firstPersonItemFrame() };
        TransformProgram.OpSink sink = new TransformProgram.OpSink() {

            @Override
            public void translate(double x, double y, double z) {
                frame[0] = frame[0].translate(x, y, z);
            }

            @Override
            public void rotate(double degrees, double x, double y, double z) {
                if (x != 0.0 && y == 0.0 && z == 0.0) {
                    frame[0] = frame[0].rotX(degrees * Math.signum(x));
                } else if (y != 0.0 && x == 0.0 && z == 0.0) {
                    frame[0] = frame[0].rotY(degrees * Math.signum(y));
                } else if (z != 0.0 && x == 0.0 && y == 0.0) {
                    frame[0] = frame[0].rotZ(degrees * Math.signum(z));
                } else {
                    throw new IllegalArgumentException("ADS math handles axis-aligned rotations only");
                }
            }

            @Override
            public void scale(double x, double y, double z) {
                frame[0] = frame[0].scale(x, y, z);
            }
        };
        t.fpBase()
            .apply(0.0, part, sink);
        t.fpIdle()
            .apply(0.0, part, sink);
        if (!xFamily) {
            frame[0] = frame[0].rotY(90);
        }
        return frame[0].apply(point);
    }

    /**
     * The pose that lines the gun up with the view and puts its sights on the middle of the screen. The barrel (model
     * +X, the muzzle direction) is turned by the smallest rotation about the sight point that makes it run straight
     * ahead; then the sight point slides to the center at its current depth. Keeping the depth means the gun neither
     * grows nor shrinks. Without the turn, a long gun that sits slanted across the screen (the LMG) would be dragged
     * with its butt across the camera.
     *
     * @param t          the gun's transform rows
     * @param boxes      the model boxes
     * @param modelScale GL units per texel
     * @param spec       the gun's ADS data
     * @param xFamily    true if the model is drawn as authored (the muzzle toward +X)
     * @return the pose, in the item frame
     */
    public static AdsPose pose(GunTransforms t, BoxSpec[] boxes, float modelScale, AdsSpec spec, boolean xFamily) {
        int part = sightPart(boxes, spec);
        double[] point = sightPoint(boxes, modelScale, spec);
        double[] eye = eyeOf(t, part, point, xFamily);
        double[] ahead = eyeOf(t, part, new double[] { point[0] + 1.0, point[1], point[2] }, xFamily);
        double[] d = { ahead[0] - eye[0], ahead[1] - eye[1], ahead[2] - eye[2] };
        double len = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        for (int i = 0; i < 3; i++) {
            d[i] /= len;
        }
        // Smallest turn from d to straight ahead (0, 0, -1): axis d x target, angle acos(d . target).
        double[] axisEye = { d[1] * -1.0 - d[2] * 0.0, d[2] * 0.0 - d[0] * -1.0, 0.0 };
        double cos = Math.max(-1.0, Math.min(1.0, -d[2]));
        double angle = Math.toDegrees(Math.acos(cos));
        double axisLen = Math.sqrt(axisEye[0] * axisEye[0] + axisEye[1] * axisEye[1]);
        if (axisLen < 1e-9 || angle < 1e-6) {
            axisEye = new double[] { 0.0, 1.0, 0.0 };
            angle = 0.0;
        } else {
            axisEye[0] /= axisLen;
            axisEye[1] /= axisLen;
        }
        // The item frame is a rotation and a uniform scale of eye space, so a turn keeps its angle and a direction
        // maps through the inverse of the linear part.
        InHandFit.Affine inverse = InHandFit.firstPersonItemFrame()
            .invert();
        double[] zero = inverse.apply(new double[3]);
        double[] axisItem = linear(inverse, zero, axisEye);
        double norm = Math.sqrt(axisItem[0] * axisItem[0] + axisItem[1] * axisItem[1] + axisItem[2] * axisItem[2]);
        for (int i = 0; i < 3; i++) {
            axisItem[i] /= norm;
        }
        double[] wanted = { 0.0, 0.0, eye[2] };
        double[] from = inverse.apply(eye);
        double[] to = inverse.apply(wanted);
        // Rotation about the sight point leaves it where it is, so the slide is the same as without the turn.
        return new AdsPose(
            new double[] { to[0] - from[0], to[1] - from[1], to[2] - from[2] },
            from,
            axisItem,
            angle);
    }

    private static double[] linear(InHandFit.Affine map, double[] zero, double[] v) {
        double[] a = map.apply(v);
        return new double[] { a[0] - zero[0], a[1] - zero[1], a[2] - zero[2] };
    }
}
