package com.gregguns.armor.model;

/**
 * One box of a worn-armor model, in the space of a vanilla biped part. Pure data (no Minecraft types), so a unit
 * test, the exporter and the client model all read the same numbers.
 *
 * <p>
 * Coordinates are Minecraft model units (1/16 block, Y down) relative to the part's rotation point, exactly as
 * {@code ModelRenderer.addBox} takes them. A box with a {@link #pivot()} rotates about that point (part-local, Z then
 * Y then X, in degrees) and its {@link #offset()} is then relative to the pivot; a box without one hangs directly off
 * the part.
 */
// Spec: docs/gregguns-spec/05-armor/implementation-notes.md ARM-DEV-20
public final class ArmorBoxDef {

    /** The six parts of a vanilla {@code ModelBiped} that armor draws. */
    public enum Part {

        HEAD("head"),
        BODY("body"),
        RIGHT_ARM("rightArm"),
        LEFT_ARM("leftArm"),
        RIGHT_LEG("rightLeg"),
        LEFT_LEG("leftLeg");

        private final String json;

        Part(String json) {
            this.json = json;
        }

        /** @return the name used in the model files */
        public String json() {
            return json;
        }

        /**
         * @param json a part name from a model file
         * @return the part
         * @throws IllegalArgumentException for an unknown name
         */
        public static Part fromJson(String json) {
            for (Part p : values()) {
                if (p.json.equals(json)) {
                    return p;
                }
            }
            throw new IllegalArgumentException("unknown biped part: " + json);
        }
    }

    /** The armor pieces; {@link #armorType()} is the vanilla {@code ItemArmor.armorType} (0 helmet to 3 boots). */
    public enum Slot {

        HELMET("helmet", 0),
        CHESTPLATE("chestplate", 1),
        LEGGINGS("leggings", 2),
        BOOTS("boots", 3);

        private final String json;
        private final int armorType;

        Slot(String json, int armorType) {
            this.json = json;
            this.armorType = armorType;
        }

        /** @return the vanilla armor type index */
        public int armorType() {
            return armorType;
        }

        /**
         * @param armorType a vanilla armor type, 0 to 3
         * @return the slot
         * @throws IllegalArgumentException when out of range
         */
        public static Slot fromArmorType(int armorType) {
            for (Slot s : values()) {
                if (s.armorType == armorType) {
                    return s;
                }
            }
            throw new IllegalArgumentException("armor type out of range: " + armorType);
        }

        /**
         * @param json a slot name from a model file
         * @return the slot
         * @throws IllegalArgumentException for an unknown name
         */
        public static Slot fromJson(String json) {
            for (Slot s : values()) {
                if (s.json.equals(json)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("unknown armor slot: " + json);
        }
    }

    private final String name;
    private final Part part;
    private final Slot slot;
    private final String material;
    private final int u;
    private final int v;
    private final float[] offset;
    private final int[] size;
    private final float inflate;
    private final boolean mirror;
    private final float[] pivot;
    private final float[] rotateDegrees;

    /**
     * @param name          a short name, unique within a part
     * @param part          the biped part it hangs from
     * @param slot          the armor piece that draws it
     * @param material      the texture generator's material tag (documentation only; the game ignores it)
     * @param u             texture origin u
     * @param v             texture origin v
     * @param offset        x, y, z of the box corner; relative to the pivot when there is one, else to the part
     * @param size          width, height, depth in whole units (the sheet unwrap needs integers)
     * @param inflate       the amount added on every side
     * @param mirror        whether the texture is mirrored (left limbs)
     * @param pivot         the rotation point (part-local), or null for an unrotated box
     * @param rotateDegrees x, y, z rotation in degrees, or null
     */
    public ArmorBoxDef(String name, Part part, Slot slot, String material, int u, int v, float[] offset, int[] size,
        float inflate, boolean mirror, float[] pivot, float[] rotateDegrees) {
        this.name = name;
        this.part = part;
        this.slot = slot;
        this.material = material;
        this.u = u;
        this.v = v;
        this.offset = offset.clone();
        this.size = size.clone();
        this.inflate = inflate;
        this.mirror = mirror;
        this.pivot = pivot == null ? null : pivot.clone();
        this.rotateDegrees = rotateDegrees == null ? null : rotateDegrees.clone();
    }

    /** @return the box name */
    public String name() {
        return name;
    }

    /** @return the part it hangs from */
    public Part part() {
        return part;
    }

    /** @return the piece that draws it */
    public Slot slot() {
        return slot;
    }

    /** @return the generator's material tag */
    public String material() {
        return material;
    }

    /** @return texture origin u, in sheet pixels */
    public int u() {
        return u;
    }

    /** @return texture origin v, in sheet pixels */
    public int v() {
        return v;
    }

    /** @return a copy of x, y, z of the box corner */
    public float[] offset() {
        return offset.clone();
    }

    /** @return a copy of width, height, depth */
    public int[] size() {
        return size.clone();
    }

    /** @return the inflate amount added on every side (model units) */
    public float inflate() {
        return inflate;
    }

    /** @return true when the texture is mirrored (left limbs) */
    public boolean mirror() {
        return mirror;
    }

    /** @return the rotation point, or null when the box is unrotated */
    public float[] pivot() {
        return pivot == null ? null : pivot.clone();
    }

    /** @return x, y, z rotation in degrees, or null */
    public float[] rotateDegrees() {
        return rotateDegrees == null ? null : rotateDegrees.clone();
    }

    /** @return the width of the sheet area this box's unwrap covers: 2 * (depth + width) */
    public int footprintWidth() {
        return 2 * (size[2] + size[0]);
    }

    /** @return the height of the sheet area this box's unwrap covers: depth + height */
    public int footprintHeight() {
        return size[2] + size[1];
    }
}
