package com.gregguns.client.render.profile;

/**
 * The first-person pose of a gun aiming down its sights, as the pieces the renderer plays back with the blend (0 to 1):
 * a rotation about the sight point that turns the barrel to point straight down the view, then a slide that puts the
 * sight point on the screen center. All in the item frame (what the renderer has set before the gun's own rows).
 * Pure data; built by {@link AdsOffset#pose}.
 *
 * <p>
 * Playing it back is, in order: {@code glTranslated(shift * blend)}, {@code glTranslated(pivot)},
 * {@code glRotated(angle * blend, axis)}, {@code glTranslated(-pivot)}. At blend 1 the sight point is on the screen
 * center at its own depth and the barrel runs along the view axis, so a long gun lies along the line of sight instead
 * of across the screen.
 *
 * <p>
 * Spec: docs/gregguns-spec/03-gun-visuals-audio/ads.md.
 */
public final class AdsPose {

    private final double[] shift;
    private final double[] pivot;
    private final double[] axis;
    private final double angleDegrees;

    AdsPose(double[] shift, double[] pivot, double[] axis, double angleDegrees) {
        this.shift = shift.clone();
        this.pivot = pivot.clone();
        this.axis = axis.clone();
        this.angleDegrees = angleDegrees;
    }

    /** @return the slide to the screen center, item frame, blocks (a copy) */
    public double[] shift() {
        return shift.clone();
    }

    /** @return the sight point the barrel turns about, item frame (a copy) */
    public double[] pivot() {
        return pivot.clone();
    }

    /** @return the unit rotation axis, item frame (a copy) */
    public double[] axis() {
        return axis.clone();
    }

    /** @return the turn that lines the barrel up with the view, degrees */
    public double angleDegrees() {
        return angleDegrees;
    }
}
