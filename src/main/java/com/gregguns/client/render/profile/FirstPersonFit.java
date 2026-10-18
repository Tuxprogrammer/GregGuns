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
 * <li><b>Offsets.</b> The sheet's {@code T(...)} rows translate <i>after</i> the scale, so a translation is multiplied
 * by the scale on screen. To keep the gun where the sheet puts it (the pistol's tuned offset is the sheet's offset
 * times
 * {@code 2.0 / 1.0}, within 0.1), multiply every translation number of the first-person rows, idle, fire and reload
 * alike, by {@link #translationFactor} {@code = sheetScale / S}. Angles are untouched.</li>
 * <li><b>Nudge.</b> The model origin is not the same point on every model, so a gun may need a small hand adjustment of
 * the offsets after a screenshot. Record it next to the numbers.</li>
 * </ol>
 */
public final class FirstPersonFit {

    /** On-screen length of a handgun in first person, in blocks of the scaled frame: the 9mm Pistol's 15/16. */
    public static final double HANDGUN_LENGTH = 15.0 / 16.0;

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
     * @param sheetScale the sheet's first-person {@code S(n)}
     * @param scale      the scale from {@link #scale}
     * @return the factor to multiply every first-person translation number of the sheet by
     */
    public static double translationFactor(double sheetScale, double scale) {
        if (sheetScale <= 0.0 || scale <= 0.0) {
            throw new IllegalArgumentException("scales must be positive");
        }
        return sheetScale / scale;
    }
}
