package com.gregguns.client.render.profile;

/**
 * Solves a gun's first-person and third-person scale and idle offset from its {@link InHandTargets.Target}, and
 * evaluates where a placed gun lands on screen. Pure; no Minecraft types, so unit tests check the same arithmetic the
 * renderer performs.
 *
 * <h2>The frames</h2>
 * The renderer hands a gun the shared item frame, then the gun's own rows. All of it is an affine map
 * ({@link Affine}):
 * <ul>
 * <li><b>First person</b>: camera offset {@code (0.56, -0.52, -0.72)}, rotY 45, scale 0.4, shift
 * {@code (-0.5, -0.5, -0.5)} (the render helper's), then the gun's {@code rotX 180, rotY a, scale S, translate T}.
 * Camera coordinates are {@code (right, up, depth)} with depth {@code = -z}.</li>
 * <li><b>Third person</b>: the hand frame (scaled 0.375 to world blocks), the same shift, the aimed pre-rotation of
 * {@link GunTransforms#applyAimed}, then the gun's rows. The "hand" is the item-frame origin, before the shift.</li>
 * </ul>
 * Because the translate {@code T} sits inside the scale, it is solved as {@code T = M^-1(target) - point}.
 *
 * <p>
 * Spec: model-notes/in-hand-transforms.md sections 0, 2, 3 and 7.
 */
public final class InHandFit {

    /** The first-person camera offset of the item frame. */
    private static final double[] FP_OFFSET = { 0.56, -0.52, -0.72 };
    /** First-person item-frame scale: camera blocks per H-block. */
    public static final double FP_FRAME_SCALE = 0.4;
    /** Third-person hand-frame scale: world blocks per H-block. */
    public static final double TP_FRAME_SCALE = 0.375;

    private InHandFit() {}

    /** The solved placement of one gun. */
    public static final class Placement {

        /** First-person scale. */
        public final double fpScale;
        /** First-person idle translate, after the scale, in the gun frame. */
        public final double[] fpIdle;
        /** Third-person scale. */
        public final double tpScale;
        /** Third-person translate, after the scale, in the gun frame. */
        public final double[] tpTranslate;

        private final double fpRotY;

        Placement(double fpScale, double[] fpIdle, double tpScale, double[] tpTranslate, double fpRotY) {
            this.fpScale = fpScale;
            this.fpIdle = fpIdle;
            this.tpScale = tpScale;
            this.tpTranslate = tpTranslate;
            this.fpRotY = fpRotY;
        }

        /**
         * The first-person muzzle flash offset that puts the flash sprite on the muzzle. The flash is drawn in the item
         * frame (rotY 45, rotZ 90, T(1.5, -1, -0.5), then scale), not on the gun, so its offset must follow the gun's
         * placement; see {@code GlFx.drawFirstPersonFlash}.
         *
         * @param muzzle     the muzzle in the gun frame, blocks (the profile's {@code MUZZLE})
         * @param flashScale the flash sprite scale
         * @return the {@code (offX, offY, offZ)} of the flash spec
         */
        public double[] flashOffset(float[] muzzle, double flashScale) {
            double[] gun = { muzzle[0] + fpIdle[0], muzzle[1] + fpIdle[1], muzzle[2] + fpIdle[2] };
            double[] eye = firstPersonFrame(fpRotY, fpScale).apply(gun);
            return flashFrame(flashScale).invert()
                .apply(eye);
        }
    }

    /**
     * @param gunName      the gun's registry name; must be on {@link InHandTargets}
     * @param boxes        the model boxes whose bounding box is placed (x along the barrel)
     * @param modelScale   GL units per texel
     * @param muzzleAlongX true if the gun frame is the box frame (muzzle +x); false if the model is turned a quarter
     *                     turn to the Z family, so (x, y, z) becomes (z, y, -x)
     * @param fpRotY       the first-person rotY row (-45 for the X family, 225 for the Z family)
     * @param tpRotY       the third-person rotY row (225 for the X family, 135 for the Z family)
     * @param tpAim        the gun's extra aimed translate
     * @return the solved scales and offsets
     */
    public static Placement fit(String gunName, BoxSpec[] boxes, float modelScale, boolean muzzleAlongX, double fpRotY,
        double tpRotY, double[] tpAim) {
        InHandTargets.Target t = InHandTargets.of(gunName);
        if (t == null) {
            throw new IllegalArgumentException("no in-hand targets for " + gunName);
        }
        double[][] b = gunFrameBounds(boxes, modelScale, muzzleAlongX);
        double len = barrelLength(b, muzzleAlongX);
        double[] centre = mid(b);

        double fpS = t.fpLength() / FP_FRAME_SCALE / len;
        double[] fpT = solve(firstPersonFrame(fpRotY, fpS), eyeOf(t.fpCentre()), centre);

        double tpS = t.tpLength() / TP_FRAME_SCALE / len;
        double[] tpT = solve(thirdPersonFrame(tpRotY, tpS, tpAim), new double[] { 0, 0, 0 }, centre);
        return new Placement(fpS, fpT, tpS, tpT, fpRotY);
    }

    /**
     * @param boxes        the boxes
     * @param modelScale   GL units per texel
     * @param muzzleAlongX see {@link #fit}
     * @return {min, max} of the bounding box in the gun frame, blocks
     */
    public static double[][] gunFrameBounds(BoxSpec[] boxes, float modelScale, boolean muzzleAlongX) {
        double[] lo = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        double[] hi = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        for (BoxSpec bx : boxes) {
            double[] a = { bx.x(), bx.y(), bx.z() };
            double[] c = { bx.x() + bx.w(), bx.y() + bx.h(), bx.z() + bx.d() };
            for (int i = 0; i < 3; i++) {
                lo[i] = Math.min(lo[i], a[i]);
                hi[i] = Math.max(hi[i], c[i]);
            }
        }
        for (int i = 0; i < 3; i++) {
            lo[i] *= modelScale;
            hi[i] *= modelScale;
        }
        if (muzzleAlongX) {
            return new double[][] { lo, hi };
        }
        // (x, y, z) -> (z, y, -x)
        return new double[][] { { lo[2], lo[1], -hi[0] }, { hi[2], hi[1], -lo[0] } };
    }

    private static double barrelLength(double[][] b, boolean muzzleAlongX) {
        return muzzleAlongX ? b[1][0] - b[0][0] : b[1][2] - b[0][2];
    }

    /** @return the centre of a bounding box */
    public static double[] mid(double[][] b) {
        return new double[] { (b[0][0] + b[1][0]) / 2, (b[0][1] + b[1][1]) / 2, (b[0][2] + b[1][2]) / 2 };
    }

    /**
     * @param b            the gun-frame bounds from {@link #gunFrameBounds}
     * @param muzzleAlongX see {@link #fit}
     * @return the muzzle tip: the centre of the barrel-end slice of the bounding box, gun frame
     */
    public static double[] muzzlePoint(double[][] b, boolean muzzleAlongX) {
        double[] m = mid(b);
        if (muzzleAlongX) {
            m[0] = b[1][0];
        } else {
            m[2] = b[0][2];
        }
        return m;
    }

    /**
     * @param b            the gun-frame bounds from {@link #gunFrameBounds}
     * @param muzzleAlongX see {@link #fit}
     * @return the rear tip: the centre of the slice opposite the muzzle, gun frame
     */
    public static double[] rearPoint(double[][] b, boolean muzzleAlongX) {
        double[] m = mid(b);
        if (muzzleAlongX) {
            m[0] = b[0][0];
        } else {
            m[2] = b[1][2];
        }
        return m;
    }

    private static double[] eyeOf(double[] camera) {
        return new double[] { camera[0], camera[1], -camera[2] };
    }

    /**
     * @param eye an eye-space point
     * @return its camera coordinates (right, up, depth)
     */
    public static double[] cameraOf(double[] eye) {
        return new double[] { eye[0], eye[1], -eye[2] };
    }

    private static double[] solve(Affine frame, double[] target, double[] point) {
        double[] q = frame.invert()
            .apply(target);
        return new double[] { q[0] - point[0], q[1] - point[1], q[2] - point[2] };
    }

    /**
     * @param flashScale the flash sprite scale
     * @return the map from a flash spec offset to eye space
     */
    public static Affine flashFrame(double flashScale) {
        return Affine.identity()
            .translate(FP_OFFSET[0], FP_OFFSET[1], FP_OFFSET[2])
            .rotY(45)
            .scale(FP_FRAME_SCALE)
            .translate(-0.5, -0.5, -0.5)
            .rotY(45)
            .rotZ(90)
            .translate(1.5, -1.0, -0.5)
            .scale(flashScale);
    }

    /** @return the map from the item frame the renderer hands a gun (after the helper shift) to eye space */
    public static Affine firstPersonItemFrame() {
        return Affine.identity()
            .translate(FP_OFFSET[0], FP_OFFSET[1], FP_OFFSET[2])
            .rotY(45)
            .scale(FP_FRAME_SCALE)
            .translate(-0.5, -0.5, -0.5);
    }

    /**
     * @param rotY  the first-person rotY row
     * @param scale the first-person scale
     * @return the map from the gun frame (before the idle translate) to eye space
     */
    public static Affine firstPersonFrame(double rotY, double scale) {
        return firstPersonItemFrame().rotX(180)
            .rotY(rotY)
            .scale(scale);
    }

    /**
     * @param rotY  the third-person rotY row
     * @param scale the third-person scale
     * @param aim   the gun's extra aimed translate
     * @return the map from the gun frame (before the row's translate) to the hand frame in H-blocks, whose origin is
     *         the hand
     */
    public static Affine thirdPersonFrame(double rotY, double scale, double[] aim) {
        return Affine.identity()
            .translate(-0.5, -0.5, -0.5)
            .rotX(-45)
            .rotY(-18)
            .rotZ(45)
            .rotY(45)
            .translate(0, -1.3, -0.4)
            .translate(aim[0], aim[1], aim[2])
            .rotY(-45)
            .rotX(180)
            .rotY(rotY)
            .scale(scale);
    }

    /** A minimal affine map: a 3x3 matrix and a translation, composed the way OpenGL stacks calls. */
    public static final class Affine {

        private final double[][] m;
        private final double[] t;

        private Affine(double[][] m, double[] t) {
            this.m = m;
            this.t = t;
        }

        /** @return the identity map */
        public static Affine identity() {
            return new Affine(new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }, new double[3]);
        }

        /**
         * @param x translate x
         * @param y translate y
         * @param z translate z
         * @return this map with a translate inside, like glTranslate
         */
        public Affine translate(double x, double y, double z) {
            double[] p = applyLinear(new double[] { x, y, z });
            return new Affine(m, new double[] { t[0] + p[0], t[1] + p[1], t[2] + p[2] });
        }

        /**
         * @param s the factor
         * @return this map with a uniform scale inside, like glScale
         */
        public Affine scale(double s) {
            return scale(s, s, s);
        }

        /**
         * @param sx X factor
         * @param sy Y factor
         * @param sz Z factor
         * @return this map with a scale inside, like glScale
         */
        public Affine scale(double sx, double sy, double sz) {
            double[] f = { sx, sy, sz };
            double[][] n = new double[3][3];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    n[i][j] = m[i][j] * f[j];
                }
            }
            return new Affine(n, t);
        }

        /**
         * @param deg degrees
         * @return this map with a rotation about X inside, like glRotate
         */
        public Affine rotX(double deg) {
            double c = Math.cos(Math.toRadians(deg));
            double s = Math.sin(Math.toRadians(deg));
            return mul(new double[][] { { 1, 0, 0 }, { 0, c, -s }, { 0, s, c } });
        }

        /**
         * @param deg degrees
         * @return this map with a rotation about Y inside, like glRotate
         */
        public Affine rotY(double deg) {
            double c = Math.cos(Math.toRadians(deg));
            double s = Math.sin(Math.toRadians(deg));
            return mul(new double[][] { { c, 0, s }, { 0, 1, 0 }, { -s, 0, c } });
        }

        /**
         * @param deg degrees
         * @return this map with a rotation about Z inside, like glRotate
         */
        public Affine rotZ(double deg) {
            double c = Math.cos(Math.toRadians(deg));
            double s = Math.sin(Math.toRadians(deg));
            return mul(new double[][] { { c, -s, 0 }, { s, c, 0 }, { 0, 0, 1 } });
        }

        private Affine mul(double[][] r) {
            double[][] n = new double[3][3];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 3; k++) {
                        n[i][j] += m[i][k] * r[k][j];
                    }
                }
            }
            return new Affine(n, t);
        }

        private double[] applyLinear(double[] p) {
            double[] o = new double[3];
            for (int i = 0; i < 3; i++) {
                o[i] = m[i][0] * p[0] + m[i][1] * p[1] + m[i][2] * p[2];
            }
            return o;
        }

        /**
         * @param p a point
         * @return its image
         */
        public double[] apply(double[] p) {
            double[] o = applyLinear(p);
            return new double[] { o[0] + t[0], o[1] + t[1], o[2] + t[2] };
        }

        /** @return the inverse map */
        public Affine invert() {
            double det = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
            double[][] c = new double[3][3];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int i1 = (i + 1) % 3;
                    int i2 = (i + 2) % 3;
                    int j1 = (j + 1) % 3;
                    int j2 = (j + 2) % 3;
                    c[j][i] = (m[i1][j1] * m[i2][j2] - m[i1][j2] * m[i2][j1]) / det;
                }
            }
            Affine inv = new Affine(c, new double[3]);
            double[] p = inv.applyLinear(t);
            return new Affine(c, new double[] { -p[0], -p[1], -p[2] });
        }
    }
}
