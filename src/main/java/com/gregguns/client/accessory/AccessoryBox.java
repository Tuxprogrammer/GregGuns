package com.gregguns.client.accessory;

/**
 * One textured box of a worn accessory model, as plain data. Units are texels of the model sheet (the model class
 * multiplies by its scale). Model space is Minecraft's biped space: X to the wearer's left, Y down, and the wearer's
 * face and chest toward -Z. Pure.
 *
 * <p>
 * Unwrap (vanilla layout, matches {@code ModelRenderer}): for a box w x h x d at texture (u, v) the top is at
 * (u+d, v) w x d, the bottom at (u+d+w, v) w x d, the right at (u, v+d) d x h, the front at (u+d, v+d) w x h, the
 * left at (u+d+w, v+d) d x h and the back at (u+2d+w, v+d) w x h. The origin {@code (u, v)} is assigned by
 * {@link SheetPacker}.
 *
 * <p>
 * A box turns about its pivot by Euler angles applied the way {@code ModelRenderer} applies them (Z, then Y, then X
 * in GL call order, so a vertex is turned about X first).
 */
public final class AccessoryBox {

    private final String name;
    private final int part;
    private final int w;
    private final int h;
    private final int d;
    private final float x;
    private final float y;
    private final float z;
    private final float[] pivot;
    private final float[] rotationDegrees;
    private int u;
    private int v;

    /**
     * @param name     label, matches the painter in {@code tools/asset-gen/accessory_models.py}
     * @param part     0 for the fixed body of the model; 1 and 2 for the hinged parts ({@link AccessoryDef#hinge})
     * @param w        width in texels (model X)
     * @param h        height in texels (model Y, down)
     * @param d        depth in texels (model Z)
     * @param x        model-space corner
     * @param y        model-space corner
     * @param z        model-space corner
     * @param pivot    model-space turn pivot, or null for the box center
     * @param rotation Euler angles about X, Y, Z in degrees, or null for none
     */
    AccessoryBox(String name, int part, int w, int h, int d, float x, float y, float z, float[] pivot,
        float[] rotation) {
        this.name = name;
        this.part = part;
        this.w = w;
        this.h = h;
        this.d = d;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pivot = pivot != null ? pivot.clone() : new float[] { x + w / 2.0f, y + h / 2.0f, z + d / 2.0f };
        this.rotationDegrees = rotation != null ? rotation.clone() : new float[3];
    }

    void place(int texU, int texV) {
        this.u = texU;
        this.v = texV;
    }

    /** @return the label */
    public String name() {
        return name;
    }

    /** @return the part index, 0 for the fixed body */
    public int part() {
        return part;
    }

    /** @return width in texels */
    public int w() {
        return w;
    }

    /** @return height in texels */
    public int h() {
        return h;
    }

    /** @return depth in texels */
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

    /** @return the turn pivot as {x, y, z} */
    public float[] pivot() {
        return pivot.clone();
    }

    /** @return the Euler angles about X, Y, Z in degrees */
    public float[] rotationDegrees() {
        return rotationDegrees.clone();
    }

    /** @return texture U of the unwrap origin */
    public int u() {
        return u;
    }

    /** @return texture V of the unwrap origin */
    public int v() {
        return v;
    }

    /** @return the width of the whole unwrap in texels */
    public int unwrapWidth() {
        return 2 * d + 2 * w;
    }

    /** @return the height of the whole unwrap in texels */
    public int unwrapHeight() {
        return d + h;
    }
}
