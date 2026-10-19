package com.gregguns.client.render.profile;

/**
 * One textured box of a gun model, as plain data: its texture origin and size (the vanilla box unwrap), where it sits
 * in model space, which part it belongs to, and an optional tilt. Units are texels of the model sheet; the model class
 * multiplies by its scale. Pure.
 *
 * <p>
 * Unwrap (vanilla layout, matches {@code ModelRenderer}): for a box w x h x d at texture (u, v) the top is at
 * (u+d, v) w x d, the bottom at (u+d+w, v) w x d, the right at (u, v+d) d x h, the front at (u+d, v+d) w x h, the
 * left at (u+d+w, v+d) d x h and the back at (u+2d+w, v+d) w x h.
 */
public final class BoxSpec {

    private final String name;
    private final int part;
    private final int u;
    private final int v;
    private final int w;
    private final int h;
    private final int d;
    private final float x;
    private final float y;
    private final float z;
    private final float pivotX;
    private final float pivotY;
    private final float pivotZ;
    private final float tiltZDegrees;
    private final float tiltXDegrees;
    private final float tiltYDegrees;

    /**
     * @param name         label, matches the layout sheet
     * @param part         the model part this box is drawn in
     * @param u            texture origin
     * @param v            texture origin
     * @param w            width (model X) in texels
     * @param h            height (model Y, down) in texels
     * @param d            depth (model Z) in texels
     * @param x            model-space corner
     * @param y            model-space corner
     * @param z            model-space corner
     * @param pivot        model-space tilt pivot (three values); ignored when both tilts are 0
     * @param tiltZDegrees tilt about the model Z axis through the pivot, degrees
     */
    public BoxSpec(String name, int part, int u, int v, int w, int h, int d, float x, float y, float z, float[] pivot,
        float tiltZDegrees) {
        this(name, part, u, v, w, h, d, x, y, z, pivot, tiltZDegrees, 0.0f);
    }

    /**
     * As the main constructor, with a second tilt: a turn about the model X axis through the pivot (the gun's long
     * axis), used to show a square bar edge-on as a diamond. Use it with one of the two tilts at a time.
     *
     * @param tiltXDegrees tilt about the model X axis through the pivot, degrees; the pivot is used when this or
     *                     {@code tiltZDegrees} is not 0
     */
    public BoxSpec(String name, int part, int u, int v, int w, int h, int d, float x, float y, float z, float[] pivot,
        float tiltZDegrees, float tiltXDegrees) {
        this(name, part, u, v, w, h, d, x, y, z, pivot, tiltZDegrees, tiltXDegrees, 0.0f);
    }

    /**
     * As the two-tilt constructor, with a third: a turn about the model Y axis (vertical) through the pivot, used to
     * show a square post as a diamond from above or to yaw a small side fitting. Use it with one tilt at a time.
     *
     * @param tiltYDegrees tilt about the model Y axis through the pivot, degrees; the pivot is used when any of the
     *                     three tilts is not 0
     */
    public BoxSpec(String name, int part, int u, int v, int w, int h, int d, float x, float y, float z, float[] pivot,
        float tiltZDegrees, float tiltXDegrees, float tiltYDegrees) {
        this.tiltXDegrees = tiltXDegrees;
        this.tiltYDegrees = tiltYDegrees;
        this.name = name;
        this.part = part;
        this.u = u;
        this.v = v;
        this.w = w;
        this.h = h;
        this.d = d;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pivotX = pivot[0];
        this.pivotY = pivot[1];
        this.pivotZ = pivot[2];
        this.tiltZDegrees = tiltZDegrees;
    }

    /**
     * @return the six unwrap rectangles as {u, v, width, height}, in the order top, bottom, right, front, left, back
     */
    public int[][] faces() {
        return new int[][] { { u + d, v, w, d }, { u + d + w, v, w, d }, { u, v + d, d, h }, { u + d, v + d, w, h },
            { u + d + w, v + d, d, h }, { u + 2 * d + w, v + d, w, h } };
    }

    /** @return the bounding rectangle of the whole unwrap as {u, v, width, height} */
    public int[] unwrapBounds() {
        return new int[] { u, v, 2 * d + 2 * w, d + h };
    }

    /** @return label */
    public String name() {
        return name;
    }

    /** @return part index */
    public int part() {
        return part;
    }

    /** @return texture U */
    public int u() {
        return u;
    }

    /** @return texture V */
    public int v() {
        return v;
    }

    /** @return width */
    public int w() {
        return w;
    }

    /** @return height */
    public int h() {
        return h;
    }

    /** @return depth */
    public int d() {
        return d;
    }

    /** @return corner X */
    public float x() {
        return x;
    }

    /** @return corner Y */
    public float y() {
        return y;
    }

    /** @return corner Z */
    public float z() {
        return z;
    }

    /** @return pivot X */
    public float pivotX() {
        return pivotX;
    }

    /** @return pivot Y */
    public float pivotY() {
        return pivotY;
    }

    /** @return pivot Z */
    public float pivotZ() {
        return pivotZ;
    }

    /** @return tilt about X in degrees, 0 for none */
    public float tiltXDegrees() {
        return tiltXDegrees;
    }

    /** @return tilt about Y in degrees, 0 for none */
    public float tiltYDegrees() {
        return tiltYDegrees;
    }

    /** @return tilt about Z in degrees, 0 for none */
    public float tiltZDegrees() {
        return tiltZDegrees;
    }
}
