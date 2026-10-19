package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The 9mm Pistol's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry. Nothing was taken from Techguns code or box lists. The model texture
 * {@code textures/models/guns/pistol_9mm.png} (128x64) is a packed atlas painted by {@code tools/asset-gen/models.py},
 * and {@code tools/asset-gen/model_layouts.md} lists the seven boxes it was painted for, each as {@code u v w h d} in
 * texels (slide, frame, barrel, grip, magazine, sight, guard). Those sizes and texture origins are used here exactly,
 * so every unwrap rectangle lands on painted pixels (checked by {@code Pistol9mmProfileTest}, which reads the PNG).
 * The positions are authored from the sizes and the one fact the spec records about the shape: a modern pistol with a
 * moving slide over a frame, a barrel poking past the slide, a rear-tilted grip holding a magazine, and a trigger
 * guard (gun-models.md, "Pistol": two parts, frame and slide).
 *
 * <h2>Frame of reference</h2>
 * The boxes are laid out with the muzzle toward +X and Y growing downward (model space), in texels. The model class
 * turns that into the Z-family convention of the pistol sheet (muzzle toward -Z) with a quarter turn about Y, so the
 * transform rows below are the sheet's numbers unchanged. The model is scaled so its overall length matches the
 * 15-unit extent of the sheet's pistol.
 *
 * <p>
 * Spec: gun-sheets.md "Pistol"; rendering-framework.md section 6.1 (Z-family) and section 8 (texture size).
 */
public final class Pistol9mmProfile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/pistol_9mm.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel: the 43-texel pistol is 15 sixteenths of a block long, as the sheet's pistol. */
    public static final float MODEL_SCALE = 15.0f / 43.0f / 16.0f;
    /** The muzzle in the gun frame (after the model's own orientation), blocks: x, y down, z. */
    public static final float[] MUZZLE = { 0.0f, 0.1f, -0.2f };

    private static final double FP_SCALE = 1.0;
    private static final double FP_X = -1.2;
    private static final double FP_Y = -1.0;
    private static final double FP_Z = -1.7;
    /**
     * Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Frame 7 is empty (the sheet's cell 7 is fully
     * transparent), so playing eight frames made the flash vanish for the last eighth of its window.
     */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };
    // The grip tilts back about its top edge, and the magazine tilts with it.
    private static final float[] GRIP_PIVOT = { -29.5f, 9.0f, 0.0f };
    private static final float GRIP_TILT = 12.0f;

    /** Part 0 is the frame group, part 1 the slide group, which the firing transform moves further. */
    public static final BoxSpec[] BOXES = {
        // part 0: frame, barrel, grip, magazine, trigger guard
        new BoxSpec("frame", 0, 0, 43, 29, 6, 6, -32f, 9f, -3f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 70, 43, 15, 5, 5, -6f, 2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 0, 0, 9, 20, 7, -34f, 9f, -3.5f, GRIP_PIVOT, GRIP_TILT),
        new BoxSpec("magazine", 0, 32, 0, 8, 18, 6, -33.5f, 14f, -3f, GRIP_PIVOT, GRIP_TILT),
        new BoxSpec("guard", 0, 0, 55, 12, 6, 3, -22f, 15f, -1.5f, NO_PIVOT, 0f),
        // part 1: slide and sight
        new BoxSpec("slide", 1, 0, 27, 35, 9, 7, -34f, 0f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("sight", 1, 30, 55, 9, 3, 3, -9f, -3f, -1.5f, NO_PIVOT, 0f), };

    /** The sheet's render transforms, timing and muzzle flash for the pistol. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "pistol_9mm",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        false,
        TRANSFORMS);

    private Pistol9mmProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (Z-family): rotX180, rotY225, S(FP_SCALE), then the state row.
        // Deviation: the sheet says S(2.0) and T(-0.55, -0.45, -0.8). With the GregGuns model (a 15/16 block long
        // pistol) that drew a rifle-sized gun running off the screen (M1 visual autotest). Halving the scale and
        // moving the offset to (-1.2, -1.0, -1.7) gives a compact pistol in the lower right, muzzle forward; the
        // fixed-frame muzzle flash (section 6.2) still lands on the muzzle.
        TransformProgram fpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FP_SCALE)
            .build();
        TransformProgram fpIdle = TransformProgram.builder()
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Firing: muzzle climbs 10 degrees at the dip, the whole gun pushes back, the slide (part 1) further.
        TransformProgram fpFire = TransformProgram.builder()
            .rotX((p, part) -> 10.0 * yF(p))
            .translate(
                TransformProgram.c(FP_X),
                TransformProgram.c(FP_Y),
                (p, part) -> FP_Z - yF(p) * (0.3 + 0.2 * part))
            .build();
        // Reload: tilt the muzzle up to 50 degrees and back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotX((p, part) -> -50.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY135, S(1.5), T(0.05, -0.55, 0.2). Aimed extra translate (0, 0, 0.2).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(135)
            .scale(1.5)
            .translate(0.05, -0.55, 0.2)
            .build();
        // Recoil about (0, 0, 0.9): rotX(-15 K); the slide also slides back 0.15 x 2p while p < 0.5.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0, 0.9, b -> b.rotX((p, part) -> -15.0 * kick(p)))
            .translate(
                TransformProgram.c(0),
                TransformProgram.c(0),
                (p, part) -> part == 1 && p < 0.5 ? 0.15 * 2.0 * p : 0.0)
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .about(0, 0.3, 0.9, b -> b.rotX((p, part) -> -40.0 * yR(p)))
            .build();
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .translate(0, -0.35, -0.3)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .scale(1.5)
            .rotY(90)
            .translate(0.3, -0.5, 0)
            .build();
        // Deviation: the sheet says "default blend" for the pistol flash, but the GregGuns sheet muzzleflash2.png is
        // an additive black-background sheet (docs/assets/MANIFEST.md), so it is drawn additively.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.65,
            -0.75,
            1.55,
            1.0,
            new double[] { 0, 0, 0 },
            true,
            0.55);
        return GunTransforms.builder()
            .parts(2)
            .timing(3, 5, 35)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0, 0.2 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
