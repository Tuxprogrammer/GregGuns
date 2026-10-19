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
 * The boxes are original GregGuns geometry, designed first and painted second. The target look is the one the spec
 * records (a long, straight iron tube, a lighter steel breech band with a raised vent notch, and a short wooden handle
 * continuing straight back, no pistol grip), matched by eye and by a silhouette comparison of rendered images. Nothing
 * is taken from Techguns code, box lists or textures. The layout lives in
 * {@code tools/asset-gen/gun-paint/handcannon.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV
 * origins and writes the generated block below, and {@code gun_paint.py paint} paints
 * {@code textures/models/guns/handcannon.png} (64x64) for exactly these boxes from the role map
 * {@code gun-paint/handcannon.json}. {@code HandCannonProfileTest} checks the boxes against the layout file.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out muzzle toward +X, Y growing downward, in texels, and drawn as authored. One texel is
 * a sixteenth of a block, so the 40-texel gun is two and a half blocks of model space before the rows scale it.
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
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z. The muzzle ring's face is 20 texels forward. */
    public static final float[] MUZZLE = { 20.0f / 16.0f, -0.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash4x4.png} that are painted: 0 to 8 (see the Revolver). */
    public static final int MUZZLE_FLASH_FRAMES = 9;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole cannon moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("handle_butt", 0, 0, 0, 3, 6, 5, -20f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("handle_mid", 0, 30, 0, 2, 5, 5, -17f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("handle_neck", 0, 44, 0, 3, 4, 5, -15f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("breech", 0, 30, 10, 8, 4, 5, -12f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("vent_rear", 0, 14, 11, 2, 2, 5, -12f, -4f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("vent_front", 0, 0, 11, 2, 2, 5, -6f, -4f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel_core", 0, 0, 27, 22, 5, 3, -4f, -3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel_flank", 0, 0, 19, 22, 3, 5, -4f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("muzzle", 0, 16, 0, 2, 6, 5, 18f, -3.5f, -2.5f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    /**
     * Deviation: at the sheet's full travel (yF and sin(p) each move the gun a whole unit) the cannon left the screen
     * during the shot and the reload (first-person screenshots), so its fire and reload translation deltas are trimmed
     * to
     * 40 per cent. The rotations are the sheet's.
     */
    private static final double TRAVEL = 0.4;
    /** The sheet's first-person scale, recorded for the smaller Techguns model. */
    private static final double SHEET_FP_SCALE = 2.5;
    /** The first-person scale from {@link FirstPersonFit}: as long on screen as the 9mm Pistol. */
    public static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), FirstPersonFit.HANDGUN_LENGTH);
    /**
     * The sheet's fire and reload translation deltas are multiplied by this, because the scale changed, and by
     * {@link #TRAVEL}.
     */
    private static final double K = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE) * TRAVEL;
    /** The idle first-person translation from {@link FirstPersonFit}: this model's box centre on the family anchor. */
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));

    /** The sheet's render transforms, timing and muzzle flash for the hand cannon. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "handcannon",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

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
        // Deviation: the sheet's first-person flash offset was placed for Techguns' model. The flash is drawn in a
        // fixed
        // frame, not on the gun, so after the fit-rule rescale it floated above the muzzle. Measured on screen
        // (960x540 sweep): +0.5 on x moves it 33 px up, +0.5 on z moves it 49 px right. (-1.25, -2.5, 2.0) puts it on
        // the muzzle of an X-family handgun held at FirstPersonFit.ANCHOR_X_FAMILY.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash4x4.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -1.25,
            -2.5,
            2.0,
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
