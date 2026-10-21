package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Grim Reaper's model boxes and render transforms, as data. Registered in {@link ClientGunDefinitions#ALL}; the
 * placement in hand comes from {@link InHandFit}.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/grim_reaper.md}), by an agent that never saw the original
 * model or texture. Nothing is taken from Techguns code, box lists or textures. The look: a huge olive-drab
 * four-tube rocket launcher 49 texels long. An 11x11 square tube block (24 long) is cut into four rounded bars by 1x1
 * corner notches and mid-side grooves (seven boxes: two lumps, a core bar and four side strips). A 12x12 muzzle cap
 * with four dark 4x4 holes and a divider cross closes the front. A near-black cage of 1 thick plates (lattice on top
 * and bottom, a big window on each flank) with four 1x2 corner rails wraps the rear 16 texels of the tubes. Behind it
 * sit the 17 long rear body (top and bottom plates, a raised top block, two flank panels and a painted skull on the
 * -Z flank), a 12x12 cross vent with four open quadrants, a zero-thickness roof hood with two buttons, two handles (a
 * diamond post and a flat post with a foot) and, on the -Z flank, a side rod of four diamond-section pieces ending in
 * a white lens, held by a slanted strut. The layout lives in
 * {@code tools/asset-gen/gun-paint/grim_reaper.layout.json}; {@code gun_paint.py pack} writes the block below and
 * {@code gun_paint.py paint} paints the sheet from {@code gun-paint/grim_reaper.json}. The cage windows are real
 * cut-outs (transparent texels), so the renderer must draw this sheet with alpha testing. No camo variants.
 *
 * <h2>Glow parts</h2>
 * Deviation: the notes ask for a bright white lens and a single red dot, and the profile has no per-box lighting flag
 * yet, so both are painted into the sheet and carried by the boxes named {@code rod_lens_glow} and
 * {@code rod_eye_glow} (a zero-thickness 1x1 plane 0.05 texel behind the rear band). A later renderer change can draw
 * exactly the boxes named {@code *_glow*} without lighting.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels. Y = 0 and Z = 0 are the tube axis; X = 0 is 3 texels
 * behind the point where the rear body ends and the tube block begins (the cage's rear end). The rear vent reaches
 * X -18 and the muzzle cap X 31. One texel is a sixteenth of a block. Deviation: the notes say the model has no
 * moving parts, so the whole gun is one part.
 *
 * <p>
 * Spec: rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class GrimReaperProfile {

    /** Texture resource paths of the model sheet: only the default, there are no camo variants. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/grim_reaper.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 96;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z: the muzzle cap's front (texel 31) on the tube axis.
     */
    public static final float[] MUZZLE = { 31.0f / 16.0f, 0.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("tube_a", 0, 28, 20, 24, 11, 4, 3f, -5.5f, -4.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_b", 0, 0, 35, 24, 11, 4, 3f, -5.5f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_core", 0, 0, 60, 24, 9, 1, 3f, -4.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_strip_nt", 0, 64, 76, 24, 4, 1, 3f, -4.5f, -5.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_strip_nb", 0, 14, 76, 24, 4, 1, 3f, 0.5f, -5.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_strip_pt", 0, 50, 81, 24, 4, 1, 3f, -4.5f, 4.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_strip_pb", 0, 0, 81, 24, 4, 1, 3f, 0.5f, 4.5f, NO_PIVOT, 0f),
        new BoxSpec("cap", 0, 0, 0, 4, 12, 10, 27f, -6f, -5f, NO_PIVOT, 0f),
        new BoxSpec("cap_side_n", 0, 116, 0, 4, 10, 1, 27f, -5f, -6f, NO_PIVOT, 0f),
        new BoxSpec("cap_side_p", 0, 116, 11, 4, 10, 1, 27f, -5f, 5f, NO_PIVOT, 0f),
        new BoxSpec("cage_top", 0, 56, 46, 16, 1, 10, 0f, -6f, -5f, NO_PIVOT, 0f),
        new BoxSpec("cage_bot", 0, 56, 35, 16, 1, 10, 0f, 5f, -5f, NO_PIVOT, 0f),
        new BoxSpec("cage_side_n", 0, 50, 67, 16, 8, 1, 0f, -4f, -6f, NO_PIVOT, 0f),
        new BoxSpec("cage_side_p", 0, 84, 67, 16, 8, 1, 0f, -4f, 5f, NO_PIVOT, 0f),
        new BoxSpec("cage_rail_nt", 0, 72, 90, 16, 1, 2, 0f, -5f, -6f, NO_PIVOT, 0f),
        new BoxSpec("cage_rail_pt", 0, 36, 93, 16, 1, 2, 0f, -5f, 4f, NO_PIVOT, 0f),
        new BoxSpec("cage_rail_nb", 0, 36, 90, 16, 1, 2, 0f, 4f, -6f, NO_PIVOT, 0f),
        new BoxSpec("cage_rail_pb", 0, 0, 91, 16, 1, 2, 0f, 4f, 4f, NO_PIVOT, 0f),
        new BoxSpec("body", 0, 28, 0, 17, 9, 11, -14f, -4.5f, -5.5f, NO_PIVOT, 0f),
        new BoxSpec("body_plate_t", 0, 52, 57, 17, 1, 9, -14f, -5.5f, -4.5f, NO_PIVOT, 0f),
        new BoxSpec("body_plate_b", 0, 0, 50, 17, 1, 9, -14f, 4.5f, -4.5f, NO_PIVOT, 0f),
        new BoxSpec("top_block", 0, 84, 0, 6, 2, 10, -14f, -6f, -5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("panel_n", 0, 108, 49, 6, 6, 1, -14f, -5f, -6f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("panel_p", 0, 0, 70, 6, 6, 1, -14f, -5f, 5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("vent_top", 0, 84, 23, 4, 1, 10, -18f, -6f, -5f, NO_PIVOT, 0f),
        new BoxSpec("vent_bot", 0, 0, 22, 4, 1, 10, -18f, 5f, -5f, NO_PIVOT, 0f),
        new BoxSpec("vent_side_n", 0, 108, 40, 4, 8, 1, -18f, -4f, -6f, NO_PIVOT, 0f),
        new BoxSpec("vent_side_p", 0, 118, 40, 4, 8, 1, -18f, -4f, 5f, NO_PIVOT, 0f),
        new BoxSpec("vent_rail_nt", 0, 112, 89, 4, 1, 2, -18f, -5f, -6f, NO_PIVOT, 0f),
        new BoxSpec("vent_rail_pt", 0, 72, 93, 4, 1, 2, -18f, -5f, 4f, NO_PIVOT, 0f),
        new BoxSpec("vent_rail_nb", 0, 112, 86, 4, 1, 2, -18f, 4f, -6f, NO_PIVOT, 0f),
        new BoxSpec("vent_rail_pb", 0, 108, 92, 4, 1, 2, -18f, 4f, 4f, NO_PIVOT, 0f),
        new BoxSpec("vent_core", 0, 28, 70, 4, 3, 3, -18f, -1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("vent_bar_v", 0, 112, 31, 4, 8, 1, -18f, -4f, -0.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("vent_bar_h", 0, 104, 57, 4, 1, 8, -18f, -0.5f, -4f, NO_PIVOT, 0f, 0f, 0f, -0.02f),
        new BoxSpec("vent_cap_t", 0, 50, 86, 4, 1, 3, -18f, -5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("vent_cap_b", 0, 36, 86, 4, 1, 3, -18f, 4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("vent_cap_n", 0, 84, 86, 4, 3, 1, -18f, -1.5f, -5f, NO_PIVOT, 0f),
        new BoxSpec("vent_cap_p", 0, 94, 86, 4, 3, 1, -18f, -1.5f, 4f, NO_PIVOT, 0f),
        new BoxSpec("hood_roof", 0, 84, 12, 4, 0, 11, -16f, -7f, -5.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("hood_drop_n", 0, 20, 33, 4, 1, 0, -16f, -7f, -5.5f, NO_PIVOT, 0f),
        new BoxSpec("hood_drop_p", 0, 0, 34, 4, 1, 0, -16f, -7f, 5.5f, NO_PIVOT, 0f),
        new BoxSpec("hood_flare_n", 0, 0, 33, 4, 0, 1, -16f, -6f, -6.5f, NO_PIVOT, 0f),
        new BoxSpec("hood_flare_p", 0, 10, 33, 4, 0, 1, -16f, -6f, 5.5f, NO_PIVOT, 0f),
        new BoxSpec("hood_wall_n", 0, 42, 70, 4, 4, 0, -16f, -6f, -6.5f, NO_PIVOT, 0f),
        new BoxSpec("hood_wall_p", 0, 104, 86, 4, 4, 0, -16f, -6f, 6.5f, NO_PIVOT, 0f),
        new BoxSpec("hood_button_n", 0, 122, 53, 2, 2, 1, -15f, -4.5f, -7f, NO_PIVOT, 0f),
        new BoxSpec("hood_button_p", 0, 122, 81, 2, 2, 1, -15f, -4.5f, 6f, NO_PIVOT, 0f),
        new BoxSpec("hood_post", 0, 64, 86, 2, 1, 3, -14f, -7f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("pad_rear", 0, 0, 86, 4, 1, 4, -10f, 5.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("pad_front", 0, 100, 81, 7, 1, 4, 0f, 6f, -2f, NO_PIVOT, 0f),
        new BoxSpec("grip_rear", 0, 118, 66, 2, 7, 2, -9f, 6.5f, -1f, new float[] { -8f, 6.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("grip_front", 0, 114, 22, 3, 7, 2, 3f, 7f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip_foot", 0, 0, 77, 4, 1, 3, 2.5f, 14f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_band", 0, 118, 75, 2, 3, 3, -2f, -1.5f, -12f, new float[] { -1f, 0f, -10.5f }, 0f, 45f),
        new BoxSpec("rod_neck", 0, 122, 35, 1, 2, 2, 0f, -1f, -11.5f, new float[] { 0.5f, 0f, -10.5f }, 0f, 45f),
        new BoxSpec("rod_body", 0, 14, 70, 4, 3, 3, 1f, -1.5f, -12f, new float[] { 3f, 0f, -10.5f }, 0f, 45f),
        new BoxSpec("rod_tip", 0, 122, 49, 1, 2, 2, 5f, -1f, -11.5f, new float[] { 5.5f, 0f, -10.5f }, 0f, 45f),
        new BoxSpec("rod_lens_glow", 0, 122, 31, 1, 2, 2, 6f, -1f, -11.5f, new float[] { 6.5f, 0f, -10.5f }, 0f, 45f),
        new BoxSpec(
            "rod_eye_glow",
            0,
            126,
            0,
            0,
            1,
            1,
            -2.05f,
            -0.5f,
            -11f,
            new float[] { -2.05f, 0f, -10.5f },
            0f,
            45f),
        new BoxSpec("rod_plate", 0, 16, 86, 4, 4, 1, 1f, 0.5f, -7f, NO_PIVOT, 0f),
        new BoxSpec("rod_bracket", 0, 74, 86, 3, 2, 2, 1.5f, 1.5f, -9f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rod_strut", 0, 26, 86, 3, 3, 2, 1.5f, 0.5f, -9f, new float[] { 3f, 3.5f, -9f }, 0f, 36.5f),
        // END GENERATED BOXES
    };

    // In-hand fit: first-person length, centre and muzzle and third-person length come from the in-hand notes'
    // section 7 row (InHandTargets). The row already accounts for Techguns turning this model 180 degrees: the far
    // tip, our +X muzzle cap, is the muzzle.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("grim_reaper", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, 0 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Grim Reaper. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "grim_reaper",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private GrimReaperProfile() {}

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
        // Firing: a roll of up to 12 degrees about the cross axis and a shift back up to 0.4 block (the notes).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 12.0 * yF(p))
            .translate((p, part) -> FP_X + 0.4 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the gun rolls up to 60 degrees and yaws up to 30 degrees at mid-reload and comes back (the notes).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -60.0 * yR(p))
            .rotY((p, part) -> -30.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225, then the fitted scale and translate (the box centre on the hand). Aimed
        // extra (0, 0.1, 0).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: the gun pitches up 20 degrees about the hand, in the first quarter of the first half (the notes).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -20.0 * kick(p)))
            .build();
        // Reload: 40 degrees pitch plus 60 degrees yaw at mid-animation, then back (the notes).
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 40.0 * yR(p))
            .rotY((p, part) -> 60.0 * yR(p))
            .build();
        // Ground: scale 0.6, the model centred (centre at texel 6.5, 4) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .translate(-0.405, -0.25, 0)
            .build();
        // Icon: side-on, scale 0.3 so the 3 block model fits the slot, centred in it.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.3)
            .translate(-0.405, -0.25, 0)
            .build();
        // The first-person flash is a fixed-frame screen effect; its offset follows the fitted placement so the sprite
        // lands on the muzzle (InHandFit.Placement.flashOffset). Unverified in game. muzzleFlash2 sheet, additive
        // because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
        double[] fo = FIT.flashOffset(MUZZLE, 1.5);
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            fo[0],
            fo[1],
            fo[2],
            1.5,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(4, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
