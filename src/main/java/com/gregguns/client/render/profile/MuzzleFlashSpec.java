package com.gregguns.client.render.profile;

import com.gregguns.client.anim.SpriteSheet;

/**
 * Data for a gun's muzzle flash: the FX sheet, where it is drawn in the muzzle frame, and how. Pure data.
 *
 * <p>
 * The first-person offset follows the Techguns ScreenEffect rule: the scale is applied first, so the effective
 * offset is {@code scale x offset} and the effective side length is {@code scale squared} (rendering-framework.md
 * section 7).
 */
public final class MuzzleFlashSpec {

    /** No tint: the sheet's own colors. */
    private static final float[] WHITE = { 1.0f, 1.0f, 1.0f };

    private final String texture;
    private final SpriteSheet sheet;
    private final double offX;
    private final double offY;
    private final double offZ;
    private final double scale;
    private final double rotX;
    private final double rotY;
    private final double rotZ;
    private final boolean additive;
    private final double thirdPersonLength;
    private final float[] tint;

    /**
     * @param texture           resource path such as {@code gregguns:textures/fx/muzzleflash2.png}
     * @param sheet             the sprite grid
     * @param offX              first-person offset in the muzzle frame
     * @param offY              first-person offset
     * @param offZ              first-person offset
     * @param scale             first-person scale
     * @param rot               extra rotations in degrees about X, Y, Z (three values)
     * @param additive          true if the sheet has a black background and must be drawn additively
     * @param thirdPersonLength length in blocks of the third-person flash planes
     */
    public MuzzleFlashSpec(String texture, SpriteSheet sheet, double offX, double offY, double offZ, double scale,
        double[] rot, boolean additive, double thirdPersonLength) {
        this(texture, sheet, offX, offY, offZ, scale, rot, additive, thirdPersonLength, WHITE);
    }

    /**
     * As above, with a vertex tint. Spec: gun-sheets.md (Alien Blaster muzzle FX row: tinted RGB, color reset to white
     * after); the renderer applies the tint to the sprite's vertices and leaves GL color white.
     *
     * @param tint red, green, blue multipliers, 0 to 1 (three values); {@code {1, 1, 1}} is untinted
     */
    public MuzzleFlashSpec(String texture, SpriteSheet sheet, double offX, double offY, double offZ, double scale,
        double[] rot, boolean additive, double thirdPersonLength, float[] tint) {
        this.tint = tint.clone();
        this.texture = texture;
        this.sheet = sheet;
        this.offX = offX;
        this.offY = offY;
        this.offZ = offZ;
        this.scale = scale;
        this.rotX = rot[0];
        this.rotY = rot[1];
        this.rotZ = rot[2];
        this.additive = additive;
        this.thirdPersonLength = thirdPersonLength;
    }

    /** @return the texture resource path */
    public String texture() {
        return texture;
    }

    /** @return the sprite grid */
    public SpriteSheet sheet() {
        return sheet;
    }

    /** @return first-person X offset */
    public double offX() {
        return offX;
    }

    /** @return first-person Y offset */
    public double offY() {
        return offY;
    }

    /** @return first-person Z offset */
    public double offZ() {
        return offZ;
    }

    /** @return first-person scale */
    public double scale() {
        return scale;
    }

    /** @return extra rotation about X, degrees */
    public double rotX() {
        return rotX;
    }

    /** @return extra rotation about Y, degrees */
    public double rotY() {
        return rotY;
    }

    /** @return extra rotation about Z, degrees */
    public double rotZ() {
        return rotZ;
    }

    /** @return true if drawn with additive blending */
    public boolean additive() {
        return additive;
    }

    /** @return length of the third-person flash planes, blocks */
    public double thirdPersonLength() {
        return thirdPersonLength;
    }

    /** @return a copy of the red, green, blue tint multipliers; all 1 means untinted */
    public float[] tint() {
        return tint.clone();
    }
}
