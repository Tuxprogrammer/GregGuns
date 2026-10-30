package com.gregguns.client.render.profile;

/**
 * Where one gun's sights are, as plain data: the model boxes that make up its iron sights or red dot, and an optional
 * hand nudge. The renderer moves the gun so the middle of those boxes lands on the screen center. Pure.
 *
 * <p>
 * The sight point defaults to the center of the bounding box of the named boxes, in the model's own axes. A nudge in
 * texels (x along the barrel, y down, z across) corrects it by eye after a client check: it is the per-gun tuning
 * knob, and it lives in {@link AdsTable} next to the box names.
 */
public final class AdsSpec {

    private final String gunName;
    private final String[] sightBoxes;
    private final double[] nudgeTexels;
    private final double pitchDegrees;

    /**
     * @param gunName     the gun's registry name without the mod id
     * @param nudgeTexels extra shift of the sight point, texels, in the model's axes; {@code {0, 0, 0}} for none
     * @param sightBoxes  names of the boxes that form the sights (at least one)
     */
    AdsSpec(String gunName, double[] nudgeTexels, String... sightBoxes) {
        this(gunName, nudgeTexels, 0.0, sightBoxes);
    }

    /**
     * @param gunName      the gun's registry name without the mod id
     * @param nudgeTexels  extra shift of the sight point, texels, in the model's axes; {@code {0, 0, 0}} for none
     * @param pitchDegrees how far the gun tips muzzle-down about the sight point while aimed, degrees; 0 keeps the
     *                     barrel level. A negative value tips the muzzle up, which drops a tall rear sight out of the
     *                     line of sight when the eye aims through a front blade.
     * @param sightBoxes   names of the boxes that form the sights (at least one)
     */
    AdsSpec(String gunName, double[] nudgeTexels, double pitchDegrees, String... sightBoxes) {
        if (sightBoxes.length == 0) {
            throw new IllegalArgumentException("an ADS spec needs at least one sight box: " + gunName);
        }
        this.gunName = gunName;
        this.sightBoxes = sightBoxes.clone();
        this.nudgeTexels = nudgeTexels.clone();
        this.pitchDegrees = pitchDegrees;
    }

    /** @return the gun's registry name without the mod id */
    public String gunName() {
        return gunName;
    }

    /** @return the names of the sight boxes (a copy) */
    public String[] sightBoxes() {
        return sightBoxes.clone();
    }

    /** @return the nudge in texels (a copy) */
    public double[] nudgeTexels() {
        return nudgeTexels.clone();
    }

    /** @return the aimed pitch in degrees, positive tipping the muzzle down; 0 for a level barrel */
    public double pitchDegrees() {
        return pitchDegrees;
    }
}
