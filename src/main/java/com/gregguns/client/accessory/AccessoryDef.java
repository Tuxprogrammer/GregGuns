package com.gregguns.client.accessory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The worn model of one accessory: its boxes, texture sheet size, draw scale and the textures it can wear. Plain data,
 * safe to load on either side; {@code AccessoryModel} (client) turns it into {@code ModelRenderer}s and the offline
 * exporter writes it as JSON for the silhouette check. Pure.
 */
public final class AccessoryDef {

    /** Where the model is mounted on the wearer. */
    public enum Mount {
        /** In the head's frame: origin at the head's rotation point, face toward -Z. */
        HEAD,
        /** In the torso's frame: origin at the torso's rotation point, the model's z = 0 on the back surface. */
        BACK
    }

    private final String id;
    private final Mount mount;
    private final int textureWidth;
    private final int textureHeight;
    private final float scale;
    private final List<String> textures;
    private final List<AccessoryBox> boxes;
    private final float[][] hinges;

    private AccessoryDef(Builder b) {
        this.id = b.id;
        this.mount = b.mount;
        this.textureWidth = b.textureWidth;
        this.textureHeight = b.textureHeight;
        this.scale = b.scale;
        this.textures = Collections.unmodifiableList(new ArrayList<>(b.textures));
        this.boxes = Collections.unmodifiableList(new ArrayList<>(b.boxes));
        this.hinges = b.hinges;
        SheetPacker.pack(this.boxes, textureWidth, textureHeight);
    }

    /** @return the registry-style name, for example {@code gas_mask} */
    public String id() {
        return id;
    }

    /** @return where it is mounted */
    public Mount mount() {
        return mount;
    }

    /** @return sheet width in texels */
    public int textureWidth() {
        return textureWidth;
    }

    /** @return sheet height in texels */
    public int textureHeight() {
        return textureHeight;
    }

    /** @return blocks per texel when drawn; a head model is drawn at 1/16 */
    public float scale() {
        return scale;
    }

    /** @return the texture base names under {@code textures/models/armor/}; index 0 is the default, the rest camos */
    public List<String> textures() {
        return textures;
    }

    /** @return the boxes, in author order */
    public List<AccessoryBox> boxes() {
        return boxes;
    }

    /** @return the number of parts: 1, or more when some boxes hinge ({@link #hinge}) */
    public int parts() {
        return hinges.length;
    }

    /**
     * @param part a part index, 1 or higher
     * @return the pivot {x, y, z} a hinged part turns about
     */
    public float[] hinge(int part) {
        return hinges[part].clone();
    }

    /**
     * @param id            the id for a new model
     * @param mount         where it is mounted
     * @param textureWidth  sheet width
     * @param textureHeight sheet height
     * @param scale         blocks per texel
     * @return a builder
     */
    static Builder builder(String id, Mount mount, int textureWidth, int textureHeight, float scale) {
        return new Builder(id, mount, textureWidth, textureHeight, scale);
    }

    /** Fluent builder; each box call may be followed by {@link #pivot}, {@link #rot} and {@link #part}. */
    static final class Builder {

        private final String id;
        private final Mount mount;
        private final int textureWidth;
        private final int textureHeight;
        private final float scale;
        private final List<String> textures = new ArrayList<>();
        private final List<AccessoryBox> boxes = new ArrayList<>();
        private float[][] hinges = new float[1][];
        // The pending box: attributes set after box() apply to it, and it is committed by the next call.
        private String name;
        private int[] size;
        private float[] corner;
        private float[] pivot;
        private float[] rot;
        private float inflate;
        private int part;

        private Builder(String id, Mount mount, int textureWidth, int textureHeight, float scale) {
            this.id = id;
            this.mount = mount;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.scale = scale;
        }

        Builder textures(String... names) {
            Collections.addAll(textures, names);
            return this;
        }

        Builder box(String boxName, int w, int h, int d, float x, float y, float z) {
            commit();
            this.name = boxName;
            this.size = new int[] { w, h, d };
            this.corner = new float[] { x, y, z };
            this.pivot = null;
            this.rot = null;
            this.inflate = 0.0f;
            this.part = 0;
            return this;
        }

        Builder pivot(float x, float y, float z) {
            this.pivot = new float[] { x, y, z };
            return this;
        }

        Builder rot(float rx, float ry, float rz) {
            this.rot = new float[] { rx, ry, rz };
            return this;
        }

        /** Grows the box just added by {@code g} texels on every side; the unwrap keeps its size. */
        Builder inflate(float g) {
            this.inflate = g;
            return this;
        }

        Builder part(int p) {
            this.part = p;
            return this;
        }

        Builder hinge(int p, float x, float y, float z) {
            if (hinges.length <= p) {
                hinges = java.util.Arrays.copyOf(hinges, p + 1);
            }
            hinges[p] = new float[] { x, y, z };
            return this;
        }

        private void commit() {
            if (name != null) {
                boxes.add(
                    new AccessoryBox(
                        name,
                        part,
                        size[0],
                        size[1],
                        size[2],
                        corner[0],
                        corner[1],
                        corner[2],
                        pivot,
                        rot,
                        inflate));
                name = null;
            }
        }

        AccessoryDef build() {
            commit();
            return new AccessoryDef(this);
        }
    }
}
