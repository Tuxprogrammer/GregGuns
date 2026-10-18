package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Revolver's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built the way {@link Pistol9mmProfile} was: nothing is taken from
 * Techguns code or box lists. The model texture {@code textures/models/guns/revolver.png} (64x32) is a packed atlas
 * painted by {@code tools/asset-gen/models.py}, and {@code tools/asset-gen/model_layouts.md} lists the six boxes it was
 * painted for, each as {@code u v w h d} in texels (frame, barrel, cylinder, grip, hammer, sight). Those sizes and
 * texture origins are used here exactly, so every unwrap rectangle lands on painted pixels (checked by
 * {@code RevolverProfileTest}, which reads the PNG). The positions are authored from the sizes and the one fact the
 * spec records about the shape: a long-barreled six-shooter with a drum cylinder in front of a hammer, a trigger-side
 * frame and a grip that curves down and back (gun-models.md, "Revolver", in words only).
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored,
 * because the sheet's X-family transform rows (first-person rotX 180, rotY -45; third-person rotY 225) expect a muzzle
 * along +X. One texel is a sixteenth of a block, so the 31-texel gun is a little under two blocks of model space
 * before the rows scale it.
 *
 * <p>
 * Spec: gun-sheets.md "Revolver" (rows and timing); rendering-framework.md section 6.1 (X-family) and section 8.
 */
public final class RevolverProfile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/revolver.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 32;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z. The barrel's front face is 24 texels forward of the
     * model origin and its bore axis sits two texels above it.
     */
    public static final float[] MUZZLE = { 24.0f / 16.0f, -2.0f / 16.0f, 0.0f };
    /**
     * Frames of {@code muzzleflash4x4.png} that are painted: 0 to 8. Cell 9 and later are empty, so the sheet's
     * "10 frames" in the asset manifest would show nothing for the last tenth of the window.
     */
    public static final int MUZZLE_FLASH_FRAMES = 9;

    private static final float[] GRIP_PIVOT = { -3.5f, 3.5f, 0.0f };
    private static final float[] NO_PIVOT = { 0, 0, 0 };
    private static final float GRIP_TILT = 12.0f;

    /** One part: the whole revolver moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // frame: the receiver the cylinder sits in, above the grip
        new BoxSpec("frame", 0, 0, 15, 13, 7, 4, -5f, -3.5f, -2f, NO_PIVOT, 0f),
        // barrel: out of the front of the frame, top flush with the frame top
        new BoxSpec("barrel", 0, 0, 26, 16, 3, 3, 8f, -3.5f, -1.5f, NO_PIVOT, 0f),
        // cylinder: wider than the frame, so it shows on both sides
        new BoxSpec("cylinder", 0, 18, 0, 7, 7, 7, -2f, -3.5f, -3.5f, NO_PIVOT, 0f),
        // grip: hangs from the rear of the frame, tilted back about its top edge
        new BoxSpec("grip", 0, 0, 0, 5, 11, 4, -6f, 3.5f, -2f, GRIP_PIVOT, GRIP_TILT),
        // hammer: on top of the rear of the frame
        new BoxSpec("hammer", 0, 38, 26, 3, 3, 2, -7f, -5.5f, -1f, NO_PIVOT, 0f),
        // front sight: on top of the muzzle end
        new BoxSpec("sight", 0, 48, 26, 2, 2, 2, 22f, -5.5f, -1f, NO_PIVOT, 0f), };

    /** The sheet's first-person scale, recorded for Techguns' smaller model. */
    private static final double SHEET_FP_SCALE = 1.5;
    /**
     * The first-person scale from {@link FirstPersonFit}: the 31-texel revolver is drawn as long on screen as the
     * 9mm Pistol (15/16 of a block), which is about S(0.48), not the sheet's S(1.5).
     */
    public static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), FirstPersonFit.HANDGUN_LENGTH);
    /** The sheet's fire and reload translation deltas are multiplied by this, because the scale changed. */
    private static final double K = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    /** The idle first-person translation from {@link FirstPersonFit}: this model's box centre on the family anchor. */
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));

    /** The sheet's render transforms, timing and muzzle flash for the revolver. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "revolver",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

    private RevolverProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (X-family): rotX180, rotY-45, S(FP_SCALE), then the state row.
        // Deviation: the sheet says S(1.5), which drew the revolver far too big (our model is larger than the one the
        // sheet was written for). The scale and the idle position follow the FirstPersonFit rule; the sheet's fire
        // and reload deltas are multiplied by K so the gun moves as far on screen as the sheet intends.
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
        // Firing: a strong muzzle flip, rotZ(35 yF), the gun jumping up and back.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 35.0 * yF(p))
            .translate((p, part) -> x0 + K * 0.5 * yF(p), (p, part) -> y0 + K * 0.4 * yF(p), TransformProgram.c(z0))
            .build();
        // Reload: s = P66(p); the gun tips out to the side to open the cylinder, then back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 25.0 * p66(p))
            .rotY((p, part) -> -20.0 * p66(p))
            .translate(
                (p, part) -> x0 + K * 0.6 * p66(p),
                (p, part) -> y0 + K * 0.3 * p66(p),
                (p, part) -> z0 + K * 0.25 * p66(p))
            .build();
        // Third person: rotX180, rotY225, S(0.85), T(-1.4, -0.8, 0). Aimed extra translate (0, -0.05, 0.2).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.85)
            .translate(-1.4, -0.8, 0)
            .build();
        // Recoil about (0, 0.25, 1.0): while p < 0.5, rotZ(-30 K).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.25, 1.0, b -> b.rotZ((p, part) -> p < 0.5 ? -30.0 * kick(p) : 0.0))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .about(0, 0.25, 1.0, b -> b.rotZ((p, part) -> -40.0 * yR(p)))
            .build();
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .translate(0, -0.35, 0)
            .scale(0.8)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(1.0)
            .translate(-0.5, -0.2, 0)
            .build();
        // muzzleFlash sheet, (-0.4, -2.5, 0.7), scale 1.85; additive because the GregGuns sheet is black-backed.
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
            .timing(6, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, -0.05, 0.2 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
