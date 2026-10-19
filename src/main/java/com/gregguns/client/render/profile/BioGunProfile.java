package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

/**
 * The Bio Gun's model boxes and render transforms, as data. Registered in {@link ClientGunDefinitions}
 * with its gun definition (M5 B3a).
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/bio_gun.md}), by an agent that never saw the original
 * model or texture. Nothing is taken from Techguns code, box lists or textures. The look: a green-and-gunmetal
 * projector. A 38 texel rifle with an 8 long glowing core seen between six light rails (a hexagonal cage), a flange and
 * a glowing disc, a wide near-black muzzle drum with gray top and bottom plates, a steel breech, a charcoal receiver
 * with a lighter top plate and a stepped rear cap, an open carry handle on four posts, a thin skeleton stock of 1x1
 * dark-green rods, a grip leaning back 20 degrees, a trigger housing with a guard floor, a hose and a light casing
 * tank tilted 30 degrees with its bottom forward. The layout lives in
 * {@code tools/asset-gen/gun-paint/bio_gun.layout.json}; {@code gun_paint.py pack} writes the block below and
 * {@code gun_paint.py paint} paints the 128x64 sheet from {@code gun-paint/bio_gun.json}. No camo variants.
 *
 * <h2>Glow parts</h2>
 * Deviation: the notes ask for fullbright green parts and the profile has no per-box lighting flag yet, so the green
 * is painted into the sheet and carried by the boxes named {@code core_glow_a} and {@code core_glow_b} (the cross
 * section of the core), {@code disc_glow} and {@code can_glow_a} and {@code can_glow_b} (the canister windows, 0.5
 * texel proud of the casing). A later renderer change can draw exactly the boxes named {@code *_glow*} without
 * lighting. The second core box is shifted 0.02 texel along the barrel so the two never share a coplanar end face.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels. Y = 0 is the barrel axis (the core's mid-height), X = 0
 * sits 9 texels ahead of the receiver rear cap's front (about the grip), Z = 0 the mid-plane. One texel is a sixteenth
 * of a block. Tilted boxes: the grip slab (20 degrees, lower end toward the rear), the hose (30 degrees, front end
 * up) and the canister casing with its two windows (30 degrees, lower end forward). Deviation: the notes say the
 * model has no moving parts, so the whole gun is one part. Deviation: the sheet is 128x64, not the original's 64x64,
 * because 41 boxes (six of them 8x1x1 rails) do not fit in 64x64.
 *
 * <p>
 * Spec: rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class BioGunProfile {

    /** Texture resource paths of the model sheet: only the default, there are no camo variants. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/bio_gun.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z: the muzzle drum's front face (texel 24) on the barrel
     * axis.
     */
    public static final float[] MUZZLE = { 24.0f / 16.0f, 0.0f, 0.0f };

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("core_glow_a", 0, 94, 0, 8, 3, 6, 9f, -1.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("core_glow_b", 0, 94, 9, 8, 4, 5, 9.02f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_top_l", 0, 42, 30, 8, 1, 1, 9f, -2.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rail_top_r", 0, 60, 30, 8, 1, 1, 9f, -2.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("rail_side_l", 0, 60, 28, 8, 1, 1, 9f, -0.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_side_r", 0, 24, 30, 8, 1, 1, 9f, -0.5f, 2.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_bot_l", 0, 24, 28, 8, 1, 1, 9f, 1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rail_bot_r", 0, 42, 28, 8, 1, 1, 9f, 1.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("flange_main", 0, 78, 0, 1, 3, 7, 17f, -1.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("flange_top", 0, 114, 18, 1, 1, 6, 17f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("flange_bot", 0, 0, 12, 1, 1, 6, 17f, 1.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("disc_glow", 0, 70, 10, 1, 4, 5, 18f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("muzzle_main", 0, 32, 0, 5, 3, 7, 19f, -1.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("muzzle_top", 0, 0, 19, 5, 1, 6, 19f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("muzzle_bot", 0, 92, 18, 5, 1, 6, 19f, 1.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("breech_main", 0, 56, 0, 4, 3, 7, 5f, -1.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("breech_top", 0, 42, 19, 4, 1, 6, 5f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("breech_bot", 0, 22, 19, 4, 1, 6, 5f, 1.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("rear_body", 0, 32, 10, 6, 4, 5, -1f, -1.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("top_plate", 0, 0, 26, 8, 1, 4, -3f, -2.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cap_a", 0, 16, 11, 2, 4, 4, -3f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cap_b", 0, 120, 9, 1, 4, 3, -4f, -2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("handle_rail_l", 0, 44, 26, 8, 1, 1, -2f, -4.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("post_front_l", 0, 28, 16, 1, 2, 1, 6f, -4.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("post_rear_l", 0, 86, 19, 1, 2, 1, -3f, -4.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("handle_rail_r", 0, 62, 26, 8, 1, 1, -2f, -4.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("post_front_r", 0, 82, 19, 1, 2, 1, 6f, -4.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("post_rear_r", 0, 82, 22, 1, 2, 1, -3f, -4.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("butt_plate", 0, 122, 0, 1, 7, 2, -14f, -2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("stock_upper", 0, 24, 26, 9, 1, 1, -13f, -1.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_lower", 0, 116, 29, 4, 1, 1, -13f, 3.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_strut", 0, 28, 11, 1, 4, 1, -9f, 0.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_middle", 0, 0, 31, 4, 1, 1, -8f, 0.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("grip_root", 0, 102, 25, 4, 3, 3, -2f, 2.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("grip_slab", 0, 82, 10, 3, 7, 2, -2f, 4f, -1f, new float[] { -2f, 4f, 0f }, 20f),
        new BoxSpec("housing", 0, 82, 25, 6, 3, 4, 2f, 2.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("guard_floor", 0, 116, 25, 3, 1, 3, 2f, 5.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("hose", 0, 62, 19, 5, 2, 5, 3.1f, 2.2f, -2.5f, new float[] { 8.1f, 3.2f, 0f }, -30f),
        new BoxSpec("can_casing", 0, 0, 0, 4, 8, 4, 4.8f, 6f, -2f, new float[] { 6.8f, 6f, 0f }, -30f),
        new BoxSpec("can_glow_a", 0, 54, 10, 5, 6, 3, 4.3f, 7f, -1.5f, new float[] { 6.8f, 6f, 0f }, -30f),
        new BoxSpec("can_glow_b", 0, 16, 0, 3, 6, 5, 5.3f, 7.02f, -2.5f, new float[] { 6.8f, 6f, 0f }, -30f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.3 and an idle offset measured from the origin of the original
    // model. The scale follows the FirstPersonFit rule with the 2 blocks on screen of the other long guns, and the
    // Deviation: the in-hand targets (in-hand-transforms.md, section 7) set the first-person length and the
    // third-person scale, and the idle position puts this model's box centre on the target centre. The older rule of a
    // fixed 2-block length is replaced by the fit the M2 guns use.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("bio_gun", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0, -0.15 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing for the Bio Gun (no muzzle flash). */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "bio_gun",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private BioGunProfile() {}

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
        // Firing: the gun slides back along its axis by up to 0.25 block and returns, with a roll of up to 7.5
        // degrees about Z at mid-animation (the notes). The pulse is symmetric: 0, maximum, 0.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> -7.5 * yF(p))
            .translate((p, part) -> FP_X + 0.25 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: roll 20 degrees about Z, yaw -10 degrees and a small shift at mid-reload, then back (the notes).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -20.0 * yR(p))
            .rotY((p, part) -> -10.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225, scaled and shifted by the fit to the in-hand targets (the model is 2.4
        // blocks long). The grip, about 0.1 block behind the origin, lands at the hand. Aimed extra (0, 0, -0.15).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: the gun pitches up about 30 degrees about a point just behind the origin, then settles back.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -30.0 * kick(p)))
            .build();
        // Reload: 30 degrees about the long axis and 40 degrees about the vertical at mid-animation, then back.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: the notes' scale 1.0 is scaled to 0.8 to match the other long guns, lying flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(0, -0.3, 0)
            .build();
        // Icon: side-on, scale 0.8 (the notes), the model centred in the slot: centre at texel 5 along the barrel.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.8)
            .translate(-0.31, -0.4, 0)
            .build();
        // No muzzle flash: the notes say the gun has none, the glow is its only light effect.
        return GunTransforms.builder()
            .parts(1)
            .timing(4, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0, -0.15 }, tpRecoil, tpReload)
            .world(ground, icon)
            .build();
    }
}
