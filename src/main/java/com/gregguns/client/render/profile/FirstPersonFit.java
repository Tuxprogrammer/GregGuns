package com.gregguns.client.render.profile;

/**
 * The first-person size rule: how to turn a spec sheet's first-person scale and offsets, recorded for Techguns' smaller
 * models, into numbers that fit a GregGuns model. Pure; no Minecraft types.
 *
 * <h2>Why a rule</h2>
 * A sheet's {@code S(n)} row was authored for a Techguns model whose size we do not copy. Our boxes are laid out from
 * {@code tools/asset-gen/model_layouts.md} and are larger, so the sheet's scale drew the Revolver far too big (the
 * M1 pistol needed {@code S(1.0)} against the sheet's {@code S(2.0)}). The rule makes the on-screen length independent
 * of how big the model happens to be:
 *
 * <ol>
 * <li><b>Scale.</b> {@code S = targetLength / modelLength}, where {@code modelLength} is the model's extent along the
 * barrel in blocks ({@link #lengthBlocks}) and {@code targetLength} is the length the gun's class should have on
 * screen. A handgun's target is {@link #HANDGUN_LENGTH} (15/16 of a block): the 9mm Pistol's tuned size, so the rule
 * reproduces the pistol's {@code S(1.0)} exactly. A longer gun class picks its own target once, by eye, and records it
 * in {@code docs/guides/adding-a-gun.md}.</li>
 * <li><b>Idle position.</b> The sheet's {@code T(...)} offsets were measured from the origin of Techguns' model, and
 * our models' origins sit elsewhere, so they are not reused (the Hand Cannon's sheet offset put it off screen). The
 * {@code T(...)} rows translate <i>after</i> the scale, in the model's own axes, so the gun's bounding-box centre lands
 * at {@code S x (centre + T)} in the rotated frame. Pick the idle offset so that point is the same for every gun of a
 * family: {@code T = anchor / S - centre} ({@link #idleOffset}, {@link #centerBlocks}), where {@link #ANCHOR_X_FAMILY}
 * and {@link #ANCHOR_Z_FAMILY} are the places the Revolver and the 9mm Pistol were tuned to.</li>
 * <li><b>Motion.</b> The sheet's fire and reload rows move the gun by deltas around its idle offset. Keep the sheet's
 * deltas, multiplied by {@link #translationFactor} {@code = sheetScale / S} (a translation is multiplied by the scale
 * on screen, so the same screen motion needs this factor), and add them to the idle offset. Angles are untouched.</li>
 * <li><b>Nudge.</b> If a screenshot still shows the gun off, nudge the idle offset by hand and record it.</li>
 * </ol>
 */
public final class FirstPersonFit {

    /** On-screen length of a handgun in first person, in blocks of the scaled frame: the 9mm Pistol's 15/16. */
    public static final double HANDGUN_LENGTH = 15.0 / 16.0;

    /**
     * Where the bounding-box centre of an X-family handgun sits in the scaled model frame, blocks: the Revolver's tuned
     * place (rotX 180, rotY -45), which the first-person screenshots confirm is a comfortable lower-right hold.
     */
    public static final double[] ANCHOR_X_FAMILY = { 0.557, -0.614, -0.675 };

    /**
     * The same for a Z-family handgun (rotX 180, rotY 225): the 9mm Pistol's tuned place, {@code S(1.0)} with
     * {@code T(-1.2, -1.0, -1.7)}.
     */
    public static final double[] ANCHOR_Z_FAMILY = { -1.4725, -0.684, -1.7 };

    private FirstPersonFit() {}

    /**
     * @param boxes      the model's boxes, x along the barrel
     * @param modelScale GL units per texel (the profile's {@code MODEL_SCALE})
     * @return the model's extent along X (the barrel axis, muzzle toward +X) in blocks
     */
    public static double lengthBlocks(BoxSpec[] boxes, float modelScale) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (BoxSpec b : boxes) {
            min = Math.min(min, b.x());
            max = Math.max(max, b.x() + b.w());
        }
        return (max - min) * (double) modelScale;
    }

    /**
     * @param boxes      the model's boxes
     * @param modelScale GL units per texel
     * @return the centre of the boxes' bounding box in model axes (x forward, y down, z), in blocks
     */
    public static double[] centerBlocks(BoxSpec[] boxes, float modelScale) {
        double[] min = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        double[] max = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        for (BoxSpec b : boxes) {
            double[] lo = { b.x(), b.y(), b.z() };
            double[] hi = { b.x() + b.w(), b.y() + b.h(), b.z() + b.d() };
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], lo[i]);
                max[i] = Math.max(max[i], hi[i]);
            }
        }
        double[] centre = new double[3];
        for (int i = 0; i < 3; i++) {
            centre[i] = (min[i] + max[i]) / 2.0 * modelScale;
        }
        return centre;
    }

    /**
     * @param modelLengthBlocks the model's length along the barrel, in blocks
     * @param targetLength      the on-screen length for the gun's class, for example {@link #HANDGUN_LENGTH}
     * @return the first-person scale
     */
    public static double scale(double modelLengthBlocks, double targetLength) {
        if (modelLengthBlocks <= 0.0 || targetLength <= 0.0) {
            throw new IllegalArgumentException("lengths must be positive");
        }
        return targetLength / modelLengthBlocks;
    }

    /**
     * @param anchor where the box centre should land after the scale (for example {@link #ANCHOR_X_FAMILY})
     * @param scale  the first-person scale from {@link #scale}
     * @param centre the model's centre from {@link #centerBlocks}
     * @return the idle translation to use after the scale: {@code anchor / scale - centre}
     */
    public static double[] idleOffset(double[] anchor, double scale, double[] centre) {
        double[] t = new double[3];
        for (int i = 0; i < 3; i++) {
            t[i] = anchor[i] / scale - centre[i];
        }
        return t;
    }

    /**
     * @param sheetScale the sheet's first-person {@code S(n)}
     * @param scale      the scale from {@link #scale}
     * @return the factor to multiply the sheet's fire and reload translation deltas by
     */
    public static double translationFactor(double sheetScale, double scale) {
        if (sheetScale <= 0.0 || scale <= 0.0) {
            throw new IllegalArgumentException("scales must be positive");
        }
        return sheetScale / scale;
    }
}
