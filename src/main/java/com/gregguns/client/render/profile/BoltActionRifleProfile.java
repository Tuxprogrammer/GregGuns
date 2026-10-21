package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Bolt Action Rifle's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/bolt-action-rifle.md}: proportions, silhouettes,
 * palettes), by an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists
 * or textures. The look: a slim old-fashioned hunting rifle, 53.5 texels long and 12 tall, with a dark-walnut wooden
 * body (heeled butt, wrist, cheek plates, stepped underside, receiver, thin fore-end) under a flat top line, a thin
 * free-floating 2x2 dark barrel that runs 15.5 texels past the wood, a small black dumbbell scope on two steel posts,
 * and a plain steel bolt lever sticking out of the right side. The layout lives in
 * {@code tools/asset-gen/gun-paint/bolt_action_rifle.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns
 * the UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the 128x32 default,
 * Olive and Tactical Black sheets from the role map {@code gun-paint/bolt_action_rifle.json}.
 * {@code BoltActionRifleProfileTest} checks the boxes against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as
 * authored. Y = 0 is the top of the scope bells and X = 0 the back of the butt. One texel is a sixteenth of a block.
 * The bolt stub and lever are static geometry in the single part (the original has no moving part either; the
 * rechamber is faked by the whole-gun motion).
 *
 * <h2>Scope</h2>
 * The rifle is scoped: while zoomed the renderer hides the model and draws the overlay {@link #SCOPE_TEXTURE}
 * (rendering-framework.md section 9.2). The kick values are fractions of the screen height; the sheet's default scope
 * recoil (0.2, 0.25) is in Techguns' own screen units and would throw the overlay across the screen, so GregGuns uses
 * a smaller shove.
 *
 * <p>
 * Spec: gun-sheets.md "Bolt-action Rifle" (rows and timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class BoltActionRifleProfile {

    /** Texture resource paths of the model sheet: the default and the two camo variants (Olive, Tactical Black). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/bolt_action_rifle.png",
        "gregguns:textures/models/guns/bolt_action_rifle_camo1.png",
        "gregguns:textures/models/guns/bolt_action_rifle_camo2.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Scope overlay texture. */
    public static final String SCOPE_TEXTURE = "gregguns:textures/fx/testscope.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 32;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the barrel's front face (texel 53.5) on its axis. */
    public static final float[] MUZZLE = { 53.5f / 16.0f, 4.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole rifle moves as a unit (the bolt is static geometry, as in the spec). */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("butt", 0, 0, 0, 3, 7, 3, 0f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("wrist", 0, 12, 0, 8, 4, 3, 3f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("cheek_right", 0, 94, 10, 9, 2, 1, 0f, 6f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("cheek_left", 0, 12, 7, 9, 2, 1, 0f, 6f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("bump_rear", 0, 114, 4, 3, 1, 3, 3f, 9f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bump_front", 0, 114, 8, 2, 1, 3, 10f, 9f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("action", 0, 58, 0, 2, 3, 3, 11f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("receiver", 0, 68, 0, 7, 2, 3, 13f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("top_plate", 0, 0, 10, 24, 1, 3, 14f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("fore_plate", 0, 54, 10, 13, 1, 3, 20f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 34, 6, 38, 2, 2, 15.5f, 3.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bolt_stub", 0, 94, 13, 7, 1, 1, 13.5f, 4f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("bolt_lever", 0, 86, 10, 1, 1, 3, 14.5f, 4f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("scope_rear", 0, 46, 0, 3, 3, 3, 12f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("scope_tube", 0, 104, 0, 8, 2, 2, 14f, 0.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("scope_front", 0, 34, 0, 3, 3, 3, 22f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("mount_rear", 0, 96, 0, 1, 2, 3, 16f, 3f, -1.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("mount_front", 0, 88, 0, 1, 2, 3, 20f, 3f, -1.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("cap_rear", 0, 124, 2, 1, 1, 1, 16f, 2f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("cap_front", 0, 124, 0, 1, 1, 1, 20f, 2f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("turret", 0, 114, 12, 2, 2, 1, 17.5f, 0.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("knob", 0, 104, 4, 1, 1, 1, 18f, 1f, -1.5f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the sheet says first-person S(1.5) and T(-0.09, -0.37, 0.07) / idle T(0.1, -0.25, -0.5), measured
    // from the origin of Techguns' model. The scale follows the FirstPersonFit rule with a rifle target of 2 blocks on
    // screen (the pistol's target is 15/16; the rifle is a long gun), and the idle position puts this model's box
    // centre on the family anchor. The sheet's fire and reload offsets are multiplied by K so they move the rifle the
    // same distance on screen as the sheet intends.
    private static final double SHEET_FP_SCALE = 1.5;
    private static final InHandFit.Placement FIT = InHandFit
        .fit("bolt_action_rifle", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.2, -0.25 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double FP_AMP = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The sheet's render transforms, timing, scope and muzzle flash for the rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "bolt_action_rifle",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private BoltActionRifleProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (X-family): rotX180, rotY-45, S(FP_SCALE), then the state row.
        TransformProgram fpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(-45)
            .scale(FP_SCALE)
            .build();
        TransformProgram fpIdle = TransformProgram.builder()
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Firing: p < 0.25 the kick, y = yF(4p): rotZ(25 y), the rifle jumps back. p >= 0.25 the rechamber, s =
        // sin((p - 0.25) 1.333): the rifle cants and pulls in as if the bolt is worked.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> p < 0.25 ? 25.0 * yF(4.0 * p) : 4.0 * rechamber(p))
            .rotY((p, part) -> p < 0.25 ? 0.0 : -16.0 * rechamber(p))
            .rotX((p, part) -> p < 0.25 ? 0.0 : -4.0 * rechamber(p))
            .translate(
                (p, part) -> FP_X + FP_AMP * (p < 0.25 ? yF(4.0 * p) : 0.25 * rechamber(p)),
                (p, part) -> FP_Y + (p < 0.25 ? 0.0 : FP_AMP * 0.05 * rechamber(p)),
                (p, part) -> FP_Z + (p < 0.25 ? 0.0 : FP_AMP * 0.05 * rechamber(p)))
            .build();
        // Reload: y = yR; the rifle tips away and down while the player loads it.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -10.0 * yR(p))
            .rotY((p, part) -> 50.0 * yR(p))
            .rotX((p, part) -> 10.0 * yR(p))
            .translate(
                (p, part) -> FP_X - FP_AMP * yR(p),
                (p, part) -> FP_Y - FP_AMP * 0.5 * yR(p),
                (p, part) -> FP_Z - FP_AMP * 0.25 * yR(p))
            .build();
        // Third person: rotX180, rotY225, S(1.2), T(-0.65, -0.25, 0). Aimed extra translate (0, 0.2, -0.25).
        // Row from the sheet and notes: scale 1.5 there; 1.2 here for the 3.3-block model (checked in game).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil about (0, 0.3, -0.9): rotZ(-20 K).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -20.0 * kick(p)))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: rotX180, rotY90, S(0.55), T(0, -0.25, 0). Icon: the sheet's rows with the scale cut to fit the slot
        // and the offset set to centre this model (centre at texel 26.75, 6).
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(0, -0.25, 0)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.6)
            .translate(-1.67, -0.375, 0)
            .build();
        // Deviation: the sheet's flash offset (-0.4, -3.0, 1.15) is where Techguns' muzzle was on screen; this
        // offset puts the fixed-frame first-person flash on this model's muzzle (solved from the same projection).
        // muzzleFlash2 sheet, additive because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
        double[] fo = FIT.flashOffset(MUZZLE, 1.85);
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            fo[0],
            fo[1],
            fo[2],
            1.85,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(15, 5, 50)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .scope(new ScopeSpec(SCOPE_TEXTURE, 0.04, 0.02))
            .build();
    }

    /** The rechamber swing: 0 at p = 0.25, a half sine to 0 again at p = 1. */
    private static double rechamber(double p) {
        return Math.sin((p - 0.25) * (Math.PI / 0.75));
    }
}
