package com.gregguns.mob.model;

/**
 * One box of a mob model part. The numbers are exactly what {@code ModelRenderer.addBox} takes: Minecraft model units
 * (1/16 block, Y down), the corner is relative to the owning part's pivot, and the box size must be whole units
 * because the sheet unwrap needs integers. Same field names as the armor box files ({@code uv}, {@code offset},
 * {@code size}, {@code inflate}, {@code mirror}); a box never rotates on its own, a tilted box is a child part.
 */
// Spec: tools/asset-gen/entity_models.md; docs/techguns-spec/10-entities-npcs/rendering-and-models.md
public final class MobBoxDef {

    private final String name;
    private final int u;
    private final int v;
    private final float[] offset;
    private final int[] size;
    private final float inflate;
    private final boolean mirror;
    private final String material;

    /**
     * @param name     a short name, for messages
     * @param u        texture origin u
     * @param v        texture origin v
     * @param offset   x, y, z of the box corner, relative to the part's pivot
     * @param size     width, height, depth in whole units; none negative, not all zero
     * @param inflate  the amount added on every side
     * @param mirror   whether the texture is mirrored (left limbs)
     * @param material the texture painter's tag (documentation only; the game ignores it)
     */
    public MobBoxDef(String name, int u, int v, float[] offset, int[] size, float inflate, boolean mirror,
        String material) {
        this.name = name;
        this.u = u;
        this.v = v;
        this.offset = offset.clone();
        this.size = size.clone();
        this.inflate = inflate;
        this.mirror = mirror;
        this.material = material;
    }

    /** @return the box name */
    public String name() {
        return name;
    }

    /** @return texture origin u, in sheet pixels */
    public int u() {
        return u;
    }

    /** @return texture origin v, in sheet pixels */
    public int v() {
        return v;
    }

    /** @return a copy of x, y, z of the corner */
    public float[] offset() {
        return offset.clone();
    }

    /** @return a copy of width, height, depth */
    public int[] size() {
        return size.clone();
    }

    /** @return the inflate amount (model units) */
    public float inflate() {
        return inflate;
    }

    /** @return true when the texture is mirrored */
    public boolean mirror() {
        return mirror;
    }

    /** @return the painter's material tag, possibly empty */
    public String material() {
        return material;
    }
}
