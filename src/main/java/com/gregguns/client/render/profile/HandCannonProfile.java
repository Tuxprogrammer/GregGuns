package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.sin;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Hand Cannon's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry. Nothing is taken from Techguns code or box lists. The model texture
 * {@code textures/models/guns/handcannon.png} (64x32) is a packed atlas painted by {@code tools/asset-gen/models.py},
 * and {@code tools/asset-gen/model_layouts.md} lists the six boxes it was painted for, each as {@code u v w h d} in
 * texels (frame, barrel, cylinder, grip, hammer, sight). Those sizes and texture origins are used here exactly, so
 * every
 * unwrap rectangle lands on painted pixels (checked by {@code HandCannonProfileTest}, which reads the PNG). The
 * positions are authored from the sizes and the one fact the spec records: a stubby single-shot cannon of stone and
 * wood. Here the 7x7x7 "cylinder" box is the breech block at the rear, the 13x7x4 frame is the lock block under the
 * barrel, and the 16-texel barrel runs forward over it, with the grip hanging from the rear and the hammer on top of
 * the
 * breech.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out muzzle toward +X, Y growing downward, in texels, and drawn as authored. One texel is
 * a
 * sixteenth of a block, so the 32-texel gun is two blocks of model space before the rows scale it.
 *
 * <p>
 * Spec: gun-sheets.md "Hand Cannon" (rows and timing); rendering-framework.md section 6.1 (X-family) and section 8.
 */
public final class HandCannonProfile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/handcannon.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 32;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z. The barrel's front is 19 texels forward. */
    public static final float[] MUZZLE = { 19.0f / 16.0f, -2.0f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash4x4.png} that are painted: 0 to 8 (see the Revolver). */
    public static final int MUZZLE_FLASH_FRAMES = 9;

    private static final float[] GRIP_PIVOT = { -6.5f, 3.5f, 0.0f };
    private static final float[] NO_PIVOT = { 0, 0, 0 };
    private static final float GRIP_TILT = 12.0f;

    /** One part: the whole cannon moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // frame: the lock block, under the barrel and forward of the breech
        new BoxSpec("frame", 0, 0, 15, 13, 7, 4, -6f, -0.5f, -2f, NO_PIVOT, 0f),
        // barrel: runs forward over the frame, its front 19 texels from the origin
        new BoxSpec("barrel", 0, 0, 26, 16, 3, 3, 3f, -3.5f, -1.5f, NO_PIVOT, 0f),
        // cylinder: the breech block at the rear, wider than the frame
        new BoxSpec("cylinder", 0, 18, 0, 7, 7, 7, -13f, -3.5f, -3.5f, NO_PIVOT, 0f),
        // grip: hangs from under the breech, tilted back about its top edge
        new BoxSpec("grip", 0, 0, 0, 5, 11, 4, -11f, 3.5f, -2f, GRIP_PIVOT, GRIP_TILT),
        // hammer: on top of the breech
        new BoxSpec("hammer", 0, 38, 26, 3, 3, 2, -11f, -6.5f, -1f, NO_PIVOT, 0f),
        // front sight: on top of the muzzle end
        new BoxSpec("sight", 0, 48, 26, 2, 2, 2, 17f, -5.5f, -1f, NO_PIVOT, 0f), };

    /** The sheet's first-person scale, recorded for the smaller Techguns model. */
    private static final double SHEET_FP_SCALE = 2.5;
    /** The first-person scale from {@link FirstPersonFit}: as long on screen as the 9mm Pistol. */
    public static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), FirstPersonFit.HANDGUN_LENGTH);
    /** The sheet's fire and reload translation deltas are multiplied by this, because the scale changed. */
    private static final double K = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    /** The idle first-person translation from {@link FirstPersonFit}: this model's box centre on the family anchor. */
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));

    /** The sheet's render transforms, timing and muzzle flash for the hand cannon. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    private HandCannonProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (X-family): rotX180, rotY-45, S(FP_SCALE), then the state row. Sheet: S(2.5), idle
        // T(0.1, 0, -0.5). Deviation (FirstPersonFit): the scale and the idle position follow the fit rule (the sheet's
        // offset put the gun off screen); the sheet's fire and reload deltas are multiplied by K.
        double x0 = IDLE[0];
        double y0 = IDLE[1];
        double z0 = IDLE[2];
        TransformProgram fpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(-45)
            .scale(FP_SCALE)
            .build();
        TransformProgram fpIdle = TransformProgram.builder()
            .translate(x0, y0, z0)
            .build();
        // Firing: the barrel tips up and the gun pulls back: rotZ(30 yF), T(0.1 + yF, 0.5 yF, -0.5).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 30.0 * yF(p))
            .translate((p, part) -> x0 + K * yF(p), (p, part) -> y0 + K * 0.5 * yF(p), TransformProgram.c(z0))
            .build();
        // Reload: s = sin(p): rotZ(70 s), T(0.1 + s, -s/2, -0.5): swings far up and out, then back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 70.0 * sin(p))
            .translate((p, part) -> x0 + K * sin(p), (p, part) -> y0 - K * sin(p) / 2.0, TransformProgram.c(z0))
            .build();
        // Third person: rotX180, rotY225, S(1.5), T(-0.3, -0.45, 0) on the sheet. Deviation: our model is larger than
        // the sheet's, so the third-person scale and offset use the Revolver's tuned values (S(0.85), T(-1.4, -0.8,
        // 0)).
        // Aimed extra translate (0, -0.1, 0).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.85)
            .translate(-1.4, -0.8, 0)
            .build();
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(-0.2, -0.1, 0, b -> b.rotZ((p, part) -> p < 0.5 ? -40.0 * kick(p) : 0.0))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .about(-0.2, -0.1, 0, b -> b.rotZ((p, part) -> -40.0 * yR(p)))
            .build();
        // Ground: rotX180, rotY90. Icon: rotX180, rotY180, S(0.7). Offsets follow the Revolver's tuned ones.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .translate(0, -0.35, 0)
            .scale(0.8)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.7)
            .translate(-0.5, -0.2, 0)
            .build();
        // muzzleFlash sheet, (-0.75, -2.5, 1.2), scale 1.85; additive because the GregGuns sheet is black-backed.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash4x4.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.75,
            -2.5,
            1.2,
            1.85,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(5, 5, 30)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, -0.1, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
