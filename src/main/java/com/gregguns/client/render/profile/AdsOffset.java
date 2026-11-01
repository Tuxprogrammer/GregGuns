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
 * the item frame, is one translation the renderer applies before the gun's own rows ({@link #itemShift}), scaled by the
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
     * The translation, in the item frame (what the renderer has set before the gun's own rows), that moves the sights
     * to the middle of the screen at their current depth.
     *
     * @param t          the gun's transform rows
     * @param boxes      the model boxes
     * @param modelScale GL units per texel
     * @param spec       the gun's ADS data
     * @param xFamily    true if the model is drawn as authored (the muzzle toward +X)
     * @return {@code {x, y, z}} to pass to {@code glTranslated} scaled by the blend
     */
    public static double[] itemShift(GunTransforms t, BoxSpec[] boxes, float modelScale, AdsSpec spec,
        boolean xFamily) {
        double[] eye = eyeOf(t, sightPart(boxes, spec), sightPoint(boxes, modelScale, spec), xFamily);
        double[] wanted = { 0.0, 0.0, eye[2] };
        InHandFit.Affine inverse = InHandFit.firstPersonItemFrame()
            .invert();
        double[] from = inverse.apply(eye);
        double[] to = inverse.apply(wanted);
        return new double[] { to[0] - from[0], to[1] - from[1], to[2] - from[2] };
    }

    /** Length of a pose array without a pitch: just the shift. */
    public static final int SHIFT_ONLY = 3;
    /** Length of a pose array with a pitch: shift, angle, axis, pivot. */
    public static final int WITH_PITCH = 10;

    /**
     * The whole aimed pose in the item frame: {@code {sx, sy, sz}} when the gun's barrel stays level, or
     * {@code {sx, sy, sz, angle, ax, ay, az, px, py, pz}} when the spec tips it. The renderer, at blend b, does
     * translate(s * b), translate(p), rotate(angle * b about a), translate(-p). The pivot p is the sight point as it
     * is in the normal hold, so the sights end at the screen center whatever the angle; the axis a is the eye's
     * horizontal axis (screen right) expressed in the item frame, so the tip is a pitch as the player sees it. The
     * angle is in GL's sense: negative of the spec's muzzle-down pitch.
     *
     * @see #itemShift
     */
    public static double[] itemPose(GunTransforms t, BoxSpec[] boxes, float modelScale, AdsSpec spec, boolean xFamily) {
        double[] shift = itemShift(t, boxes, modelScale, spec, xFamily);
        if (spec.pitchDegrees() == 0.0) {
            return shift;
        }
        double[] eye = eyeOf(t, sightPart(boxes, spec), sightPoint(boxes, modelScale, spec), xFamily);
        InHandFit.Affine inverse = InHandFit.firstPersonItemFrame()
            .invert();
        double[] from = inverse.apply(eye);
        double[] origin = inverse.apply(new double[] { 0.0, 0.0, 0.0 });
        double[] right = inverse.apply(new double[] { 1.0, 0.0, 0.0 });
        double[] axis = { right[0] - origin[0], right[1] - origin[1], right[2] - origin[2] };
        double len = Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
        return new double[] { shift[0], shift[1], shift[2], -spec.pitchDegrees(), axis[0] / len, axis[1] / len,
            axis[2] / len, from[0], from[1], from[2] };
    }

    /**
     * What the renderer's GL calls do to one point, as arithmetic, so a test can check the pose.
     *
     * @param pose  from {@link #itemPose}
     * @param point a point in the item frame
     * @param blend 0 (normal hold) to 1 (fully aimed)
     * @return the point after the aimed pose, in the item frame
     */
    public static double[] applyPose(double[] pose, double[] point, double blend) {
        double[] q = point.clone();
        if (pose.length >= WITH_PITCH) {
            double a = Math.toRadians(pose[3] * blend);
            double c = Math.cos(a);
            double s = Math.sin(a);
            double[] k = { pose[4], pose[5], pose[6] };
            double[] v = { q[0] - pose[7], q[1] - pose[8], q[2] - pose[9] };
            double dot = k[0] * v[0] + k[1] * v[1] + k[2] * v[2];
            double[] cross = { k[1] * v[2] - k[2] * v[1], k[2] * v[0] - k[0] * v[2], k[0] * v[1] - k[1] * v[0] };
            for (int i = 0; i < 3; i++) {
                // Rodrigues' rotation about the unit axis k, as glRotated turns (right-handed).
                q[i] = pose[7 + i] + v[i] * c + cross[i] * s + k[i] * dot * (1 - c);
            }
        }
        for (int i = 0; i < 3; i++) {
            q[i] += pose[i] * blend;
        }
        return q;
    }
}
