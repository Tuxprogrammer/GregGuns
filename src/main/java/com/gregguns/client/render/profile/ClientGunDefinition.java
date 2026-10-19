package com.gregguns.client.render.profile;

import java.util.Arrays;

/**
 * The client half of one gun's definition, as plain data: which item it is for, the model boxes, the model sheet and
 * its camo variants, the muzzle, and the transform rows. {@code GunRenderers.register(ClientGunDefinition)} builds the
 * model and binds the item renderer from it. Each gun's profile class ({@code Pistol9mmProfile} and so on) exposes one
 * as {@code DEFINITION}, and {@link ClientGunDefinitions#ALL} lists them: that list is the only client list a new gun
 * touches.
 *
 * <p>
 * No Minecraft or GL types, so unit tests can read the whole list. The common half is
 * {@code com.gregguns.gun.def.GunDefinition}, which owns the camo count; {@link #textures()} must have exactly that
 * many entries (a test checks it, and the files on disk). Client data, but safe to load on either side.
 */
// Spec: rendering-framework.md section 1 (registration), section 8 (texture size)
public final class ClientGunDefinition {

    private final String gunName;
    private final BoxSpec[] boxes;
    private final int parts;
    private final int textureWidth;
    private final int textureHeight;
    private final float modelScale;
    private final String[] textures;
    private final float[] muzzle;
    private final boolean xFamily;
    private final GunTransforms transforms;

    /**
     * @param gunName       the gun's registry name without the mod id (its {@code GunDefinition#name()})
     * @param boxes         the model boxes, each tagged with its part
     * @param parts         number of parts
     * @param textureWidth  model sheet width in texels
     * @param textureHeight model sheet height in texels
     * @param modelScale    GL units per texel
     * @param textures      texture resource paths, the default first and then one per camo
     * @param muzzle        the muzzle in the gun frame, blocks
     * @param xFamily       true if the boxes are authored with the muzzle toward +X (the X-family transform rows)
     * @param transforms    timing, transform rows, muzzle flash and optional scope
     */
    public ClientGunDefinition(String gunName, BoxSpec[] boxes, int parts, int textureWidth, int textureHeight,
        float modelScale, String[] textures, float[] muzzle, boolean xFamily, GunTransforms transforms) {
        if (textures.length < 1) {
            throw new IllegalArgumentException("a gun model needs at least one texture: " + gunName);
        }
        this.gunName = gunName;
        this.boxes = boxes.clone();
        this.parts = parts;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.modelScale = modelScale;
        this.textures = textures.clone();
        this.muzzle = muzzle.clone();
        this.xFamily = xFamily;
        this.transforms = transforms;
    }

    /** @return the gun's registry name without the mod id */
    public String gunName() {
        return gunName;
    }

    /** @return the model boxes (a copy) */
    public BoxSpec[] boxes() {
        return boxes.clone();
    }

    /** @return number of parts */
    public int parts() {
        return parts;
    }

    /** @return model sheet width in texels */
    public int textureWidth() {
        return textureWidth;
    }

    /** @return model sheet height in texels */
    public int textureHeight() {
        return textureHeight;
    }

    /** @return GL units per texel */
    public float modelScale() {
        return modelScale;
    }

    /** @return the texture resource paths, default first then the camos (a copy); one per camo of the gun */
    public String[] textures() {
        return Arrays.copyOf(textures, textures.length);
    }

    /** @return the muzzle in the gun frame, blocks (a copy) */
    public float[] muzzle() {
        return muzzle.clone();
    }

    /** @return true if the boxes are authored with the muzzle toward +X */
    public boolean xFamily() {
        return xFamily;
    }

    /** @return timing, transform rows, muzzle flash and optional scope */
    public GunTransforms transforms() {
        return transforms;
    }
}
