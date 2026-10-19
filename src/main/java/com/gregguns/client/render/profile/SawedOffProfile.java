package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.sin;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Sawed Off's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/sawed-off.md}: proportions, silhouettes, palette), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures.
 * The look: a cut-down side-by-side double barrel. Two square-section tubes (28 long) joined by three ribs, so the
 * block is 7 wide with a groove down the middle and a half-height ridge on each flank, two bead sights at the muzzle,
 * a small flat lever where the barrels meet the wood, and a stepped dark wooden stub (11 long) that ends in a taller,
 * wider butt plate. No pump, no grip, no trigger guard, no hammer. The layout lives in
 * {@code tools/asset-gen/gun-paint/sawed_off.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV
 * origins and writes the generated block below, and {@code gun_paint.py paint} paints {@code sawed_off.png} (64x64,
 * the bores are painted faces) from the role map {@code gun-paint/sawed_off.json}. {@code SawedOffProfileTest} checks
 * the boxes against the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * X = 0 is the barrels' rear face, Y = 0 their top face and Z = 0 the gun's mid-plane. One texel is a sixteenth of a
 * block.
 *
 * <p>
 * Spec: gun-sheets.md "Sawed-off" (rows and timing); rendering-framework.md section 6.1 (X-family) and section 8.
 */
public final class SawedOffProfile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/sawed_off.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z. The centre of the barrels' front face, between the
     * two bores: 28 texels forward of the rear face, 2 below the top, on the mid-plane.
     */
    public static final float[] MUZZLE = { 28.0f / 16.0f, 2.0f / 16.0f, 0.0f };
    /**
     * Frames of {@code muzzleflash4x4.png} that are painted: 0 to 8 (see {@link RevolverProfile#MUZZLE_FLASH_FRAMES}).
     */
    public static final int MUZZLE_FLASH_FRAMES = 9;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("tube_l", 0, 0, 11, 28, 4, 2, 0f, 0f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_r", 0, 0, 17, 28, 4, 2, 0f, 0f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("rib_c", 0, 0, 23, 28, 2, 1, 0f, 1f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("rib_l", 0, 0, 26, 28, 2, 1, 0f, 1f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("rib_r", 0, 0, 29, 28, 2, 1, 0f, 1f, 2.5f, NO_PIVOT, 0f),
        new BoxSpec("bead_l", 0, 60, 7, 1, 1, 1, 27f, -1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bead_r", 0, 60, 9, 1, 1, 1, 27f, -1f, 0f, NO_PIVOT, 0f),
        new BoxSpec("nub_l", 0, 58, 4, 1, 2, 1, -1f, 1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("nub_r", 0, 30, 7, 1, 2, 1, -1f, 1f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("nubtop_l", 0, 60, 11, 1, 1, 1, -1f, 0f, -1f, NO_PIVOT, 0f),
        new BoxSpec("nubtop_r", 0, 60, 13, 1, 1, 1, -1f, 0f, 0f, NO_PIVOT, 0f),
        new BoxSpec("lever", 0, 44, 4, 4, 1, 3, -4f, 1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("lock", 0, 34, 0, 2, 3, 3, -2f, 2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("hand", 0, 14, 0, 7, 4, 3, -9f, 2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_l", 0, 14, 7, 7, 2, 1, -9f, 3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_r", 0, 44, 8, 7, 2, 1, -9f, 3f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("chin", 0, 34, 6, 2, 1, 3, -9f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt", 0, 0, 0, 2, 6, 5, -11f, 2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("strip", 0, 44, 0, 6, 1, 3, 0f, 4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("sliver", 0, 0, 32, 8, 1, 1, 0f, 3f, -0.5f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    /** The sheet's first-person scale, recorded for Techguns' smaller model. */
    private static final double SHEET_FP_SCALE = 2.2;
    /**
     * The first-person scale from {@link FirstPersonFit}: the 39-texel gun is drawn 15/16 of a block long on screen.
     * The sheet's S(2.2) runs the gun off the frame.
     */
    public static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), FirstPersonFit.HANDGUN_LENGTH);
    /** The sheet's fire and reload translation deltas are multiplied by this, because the scale changed. */
    private static final double K = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    /** The idle first-person translation from {@link FirstPersonFit}: this model's box centre on the family anchor. */
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));

    /** The sheet's render transforms, timing and muzzle flash for the Sawed Off. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "sawed_off",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

    private SawedOffProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (X-family): rotX180, rotY-45, S(FP_SCALE), then the state row.
        // Deviation: the sheet says S(2.2) with idle T(0.1, -0.1, -0.5), recorded for Techguns' model, which ran off
        // the frame. The scale and idle position follow the FirstPersonFit rule; the sheet's fire and reload deltas
        // are multiplied by K so the gun moves as far on screen as the sheet intends.
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
        // Firing: one quick kick, rotZ(30 yF), the gun jumping up and back.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 30.0 * yF(p))
            .translate((p, part) -> x0 + K * 0.5 * yF(p), (p, part) -> y0 + K * 0.3 * yF(p), TransformProgram.c(z0))
            .build();
        // Reload: s = sin(p); the gun tips up 70 degrees and swings out, then comes back (no hinge in the model).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 70.0 * sin(p))
            .translate((p, part) -> x0 + K * sin(p), (p, part) -> y0 - K * sin(p) / 2.0, TransformProgram.c(z0))
            .build();
        // Third person: rotX180, rotY225, S(0.7), T(-1.4, -0.8, 0) (sheet: S(1.5), for a larger gun frame). Aimed extra
        // translate (0, 0.15, 0).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.7)
            .translate(-1.4, -0.8, 0)
            .build();
        // Recoil about (-0.2, -0.1, 0): rotZ(-40 K).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(-0.2, -0.1, 0, b -> b.rotZ((p, part) -> -40.0 * kick(p)))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .about(-0.2, -0.1, 0, b -> b.rotZ((p, part) -> -40.0 * yR(p)))
            .build();
        // Deviation: the sheet has no scale for the ground row; the 2.4 block long model is drawn at 0.6 so a dropped
        // Sawed Off is about a block and a half long.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.7)
            .build();
        // muzzleFlash sheet, (-0.55, -2.15, 1.05), scale 1.85; additive because the GregGuns sheet is black-backed.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash4x4.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.55,
            -2.15,
            1.05,
            1.85,
            new double[] { 0, 0, 0 },
            true,
            0.1);
        return GunTransforms.builder()
            .parts(1)
            .timing(5, 5, 28)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.15, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
