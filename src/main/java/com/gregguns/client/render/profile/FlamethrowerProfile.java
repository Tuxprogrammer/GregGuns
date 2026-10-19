package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.tri;

/**
 * The Flamethrower's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/flamethrower.md}: proportions, silhouettes, palette), by
 * an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures.
 * The look: a long light-gray pipe (a "+" of boxes) over a fat white fuel tank with red stripes and a hazard roundel; a
 * maroon open-loop pistol grip, a zigzag skeleton stock and a small maroon nozzle with a black mouth and four prongs,
 * with a bent pilot tube hooked up under it. The layout lives in
 * {@code tools/asset-gen/gun-paint/flamethrower.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV
 * origins and writes the generated block below, and {@code gun_paint.py paint} paints {@code flamethrower.png}
 * (128x128) from the role map {@code gun-paint/flamethrower.json}. {@code FlamethrowerProfileTest} checks the boxes
 * against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the muzzle is toward +X and Y grows downward, in texels, drawn as authored. X = 0 is the back face of the
 * stock's butt plate; Y = 0 is the top of the dome; the bore axis is at Y = 3, Z = 0 (the notes' own frame, taken
 * unchanged). One texel is a sixteenth of a block.
 *
 * <h2>Pilot tube</h2>
 * Geometry only: two tilted 1x1 rods (21 degrees down, then a hook 29 degrees from vertical). The notes leave the pilot
 * flame unresolved (section 10), so this profile draws no flame and no muzzle flash; the fire is the flame particles
 * leaving the nozzle at {@link #MUZZLE}. One part: nothing on the model moves.
 *
 * <p>
 * Spec: model-notes/flamethrower.md sections 6 and 7 (framing, no recoil); rendering-framework.md section 6.1
 * (X-family) and section 8.
 */
public final class FlamethrowerProfile {

    /** Texture resource path of the model sheet (one texture, no camo). */
    public static final String TEXTURE = "gregguns:textures/models/guns/flamethrower.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 128;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: the centre of the nozzle tip's front face (texel 42, 3, 0). */
    public static final float[] MUZZLE = { 42.0f / 16.0f, 3.0f / 16.0f, 0.0f };

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: nothing moves. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("butt", 0, 90, 0, 2, 5, 3, 0f, 2.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt_ext", 0, 120, 8, 2, 3, 2, 0f, 7.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bar_top", 0, 110, 14, 7, 1, 2, 2f, 3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bar_mid", 0, 0, 18, 4, 1, 2, 5f, 5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bar_low", 0, 110, 17, 4, 1, 2, 2f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("receiver", 0, 64, 0, 10, 5, 3, 9f, 1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("spine", 0, 56, 15, 10, 1, 2, 9f, 0.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("pipe_l", 0, 56, 18, 9, 1, 1, 10f, 4f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("pipe_r", 0, 76, 19, 9, 1, 1, 10f, 4f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("pump", 0, 46, 0, 4, 4, 5, 19f, 1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("dome", 0, 46, 9, 4, 1, 3, 19f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel_core", 0, 80, 8, 13, 4, 2, 23f, 1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rail_l", 0, 0, 15, 13, 2, 1, 23f, 2f, 1f, NO_PIVOT, 0f),
        new BoxSpec("rail_r", 0, 28, 17, 13, 2, 1, 23f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("tank", 0, 10, 0, 14, 6, 4, 20f, 5.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("plate_l", 0, 10, 10, 14, 4, 1, 20f, 6.5f, 2f, NO_PIVOT, 0f),
        new BoxSpec("plate_r", 0, 80, 14, 14, 4, 1, 20f, 6.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("tank_rear", 0, 110, 0, 1, 4, 4, 19f, 6.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("nose1", 0, 100, 0, 1, 4, 4, 34f, 6.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("nose2", 0, 64, 8, 1, 3, 4, 35f, 7f, -2f, NO_PIVOT, 0f),
        new BoxSpec("valve_a", 0, 20, 18, 2, 1, 2, 16f, 6.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("valve_b", 0, 40, 10, 1, 1, 2, 16f, 7.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("valve_c", 0, 40, 13, 2, 2, 2, 17f, 7.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bulkhead", 0, 0, 0, 1, 7, 4, 36f, 1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("collar", 0, 110, 8, 2, 3, 3, 37f, 1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("tip", 0, 0, 11, 3, 2, 2, 39f, 2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("prong_t", 0, 96, 19, 2, 1, 1, 39f, 1.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("prong_b", 0, 28, 15, 2, 1, 1, 39f, 3.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("prong_l", 0, 34, 15, 2, 1, 1, 39f, 2.5f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("prong_r", 0, 122, 17, 2, 1, 1, 39f, 2.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stub", 0, 48, 13, 1, 2, 2, 37f, 4.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("pilot_run", 0, 54, 13, 4, 1, 1, 38f, 5f, -0.5f, new float[] { 38f, 5.5f, 0f }, 21f),
        new BoxSpec("pilot_hook", 0, 60, 9, 1, 3, 1, 40.8f, 4f, -0.5f, new float[] { 41.3f, 7f, 0f }, 29f),
        new BoxSpec("grip_rear", 0, 120, 0, 2, 6, 2, 10f, 6.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip_front", 0, 74, 8, 1, 5, 2, 14f, 6.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip_foot", 0, 12, 18, 2, 1, 2, 12f, 11.5f, -1f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first person S(2.0) with an offset measured from Techguns' own origin. The scale
    // follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's precedent), and
    // the idle position puts this model's box centre on the family anchor. The reload shift is multiplied by FP_AMP so
    // it moves the gun as far on screen as the notes intend.
    private static final double SHEET_FP_SCALE = 2.0;
    private static final InHandFit.Placement FIT = InHandFit
        .fit("flamethrower", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.2, -0.25 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double FP_AMP = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] CENTER = FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE);
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms and timing for the Flamethrower (no recoil, no muzzle flash). */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, texture and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "flamethrower",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

    private FlamethrowerProfile() {}

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
        // Firing: no recoil at all, the same pose as idle (the notes, section 6).
        TransformProgram fpFire = fpIdle;
        // Reload: a roll up to 20 degrees, a yaw of -10 and a shift of up to (0.5, 0.25, 0.25), on the flat-topped
        // sine arc.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 20.0 * p66(p))
            .rotY((p, part) -> -10.0 * p66(p))
            .translate(
                (p, part) -> FP_X + FP_AMP * 0.5 * p66(p),
                (p, part) -> FP_Y + FP_AMP * 0.25 * p66(p),
                (p, part) -> FP_Z + FP_AMP * 0.25 * p66(p))
            .build();
        // Third person: rotX180, rotY225, S(1.1), then T = minus the grip (texel 12, 9.5), so the pistol grip sits in
        // the hand and the gun extends forward. Aimed extra translate (0, 0.2, -0.25), as the long guns.
        // Deviation: the notes' S(1.5) is for Techguns' frame; 1.1 keeps the 2.7-block gun about 3 blocks long, as
        // the other long guns.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Reload: up to -30 degrees about X and -40 about Y at mid-animation, on a triangle.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> -30.0 * tri(p))
            .rotY((p, part) -> -40.0 * tri(p))
            .build();
        // Ground: scale 0.85 as the notes say, centred on the model's middle so it lies on the ground.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.85)
            .translate(-CENTER[0], -CENTER[1], 0)
            .build();
        // Icon: side-on, muzzle to the right, scale 0.7 as the notes say, centred on the model's middle.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.7)
            .translate(-CENTER[0], -CENTER[1], 0)
            .build();
        return GunTransforms.builder()
            .parts(1)
            .timing(5, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, TransformProgram.EMPTY, tpReload)
            .world(ground, icon)
            .build();
    }
}
