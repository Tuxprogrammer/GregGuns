package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Pulse Rifle's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/pulse_rifle.md}) by an agent that never saw the original
 * model or texture. The look: a bullpup-style sci-fi rifle 48 texels long and 18.3 tall: a pale ice-blue upper (spine,
 * stepped three-level scope pyramid between two dark bells, a 5-wide front receiver block with a red stripe) over a
 * dark gunmetal lower (an open triangular grip loop, a swept stock strut, a hook foregrip), a short diamond barrel
 * ending in a forked muzzle, and a raked 5-wide butt pad. The red stripe and the cyan lens dot on the rear bell are
 * painted (no emissive rendering). The layout lives in {@code tools/asset-gen/gun-paint/pulse_rifle.layout.json};
 * {@code gun_paint.py pack} writes the generated block below and {@code gun_paint.py paint} paints the default, Gray
 * and Dark Green sheets from {@code gun-paint/pulse_rifle.json}.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, texels; X = 0 is the back of the butt pad, Y = 0 the top of the scope
 * cap, Z = 0 the mid-plane. One texel is a sixteenth of a block. No moving parts.
 *
 * <p>
 * Spec: model-notes/pulse_rifle.md; rendering-framework.md section 6.1 (X-family), 8 and 9. The gun is not registered
 * yet; a later gameplay batch adds it to the gun definitions.
 */
public final class PulseRifleProfile {

    /** Texture resource paths: the default, Gray (camo 1) and Dark Green (camo 2). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/pulse_rifle.png",
        "gregguns:textures/models/guns/pulse_rifle_camo1.png", "gregguns:textures/models/guns/pulse_rifle_camo2.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: the muzzle block's front face (texel 48) on the barrel axis (y 7). */
    public static final float[] MUZZLE = { 48.0f / 16.0f, 7.0f / 16.0f, 0.0f };
    /** Scope overlay texture. */
    public static final String SCOPE_TEXTURE = "gregguns:textures/fx/techscope.png";
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole rifle moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("barrel", 0, 0, 27, 9, 2, 2, 33f, 6f, -1f, new float[] { 33f, 7f, 0f }, 0f, 45f),
        new BoxSpec("neck_flat", 0, 6, 38, 2, 1, 1, 42f, 6.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("neck_diamond", 0, 0, 38, 2, 1, 1, 42f, 6.5f, -0.5f, new float[] { 42f, 7f, 0f }, 0f, 45f),
        new BoxSpec("muzzle_block", 0, 68, 33, 4, 2, 2, 44f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("prong_r", 0, 106, 37, 3, 1, 1, 45f, 6.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("prong_l", 0, 98, 37, 3, 1, 1, 45f, 6.5f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("gas_block", 0, 124, 0, 1, 1, 1, 40.5f, 9f, -0.5f, new float[] { 40.5f, 9.5f, 0f }, 0f, 45f),
        new BoxSpec("front_recv", 0, 32, 0, 7, 4, 5, 26f, 5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("hg_core", 0, 80, 0, 7, 3, 4, 33f, 5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hg_plate_r", 0, 16, 31, 2, 3, 1, 33f, 6f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("hg_plate_l", 0, 122, 19, 2, 3, 1, 33f, 6f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("hg_lower", 0, 10, 10, 10, 2, 4, 29.5f, 8f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hg_lower2", 0, 32, 26, 10, 1, 3, 29.5f, 10f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("hg_step", 0, 118, 13, 1, 2, 4, 28.5f, 9f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hg_step_bar", 0, 110, 33, 1, 1, 3, 28.5f, 10.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("hg_cap", 0, 24, 0, 1, 7, 3, 39.5f, 4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_light", 0, 0, 31, 5, 1, 3, 33f, 4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_dark", 0, 22, 34, 9, 1, 2, 27f, 3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("sight_wedge", 0, 22, 27, 3, 1, 2, 37.4f, 3.8f, -1f, new float[] { 38.9f, 4.3f, 0f }, 36f),
        new BoxSpec("spine", 0, 62, 7, 24, 2, 4, 2f, 6.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("recv_cap", 0, 56, 0, 7, 2, 5, 19f, 5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("step1", 0, 0, 17, 14, 1, 4, 19f, 4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("step2", 0, 86, 19, 8, 1, 4, 19f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("step3", 0, 36, 21, 7, 1, 4, 19f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("scope_cap", 0, 84, 13, 6, 2, 4, 19f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rear_stack", 0, 62, 13, 7, 2, 4, 12f, 4.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rear_bar", 0, 58, 29, 8, 1, 3, 11f, 3.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_hump", 0, 104, 24, 2, 2, 3, 10f, 4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_thin", 0, 114, 24, 1, 1, 4, 11f, 5.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_top_a", 0, 80, 29, 8, 1, 3, 2f, 5.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_top_b", 0, 0, 22, 6, 1, 4, 3f, 5.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_inset", 0, 22, 30, 6, 1, 3, 3f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bell_rear", 0, 0, 11, 2, 3, 3, 17f, 0.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bell_front", 0, 118, 7, 2, 3, 3, 24.5f, 0.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("mount_top", 0, 70, 24, 2, 1, 4, 16f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("mount_bottom", 0, 58, 24, 2, 1, 4, 16f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("mount_side_r", 0, 86, 37, 2, 2, 1, 16f, 1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("mount_side_l", 0, 118, 36, 2, 2, 1, 16f, 1f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 0, 0, 3, 9, 2, 18.6f, 8.25f, -1f, new float[] { 20.1f, 12.75f, 0f }, 35f),
        new BoxSpec("grip_front", 0, 56, 7, 1, 5, 2, 25.15f, 12.25f, -1f, new float[] { 25.65f, 14.75f, 0f }, 35f),
        new BoxSpec("trigger_housing", 0, 100, 33, 3, 2, 2, 23f, 8.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 92, 37, 2, 2, 1, 22.3f, 9.95f, -0.5f, new float[] { 23.3f, 10.95f, 0f }, 23f),
        new BoxSpec("under_rail", 0, 0, 35, 9, 1, 2, 14f, 8.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut_a", 0, 52, 14, 2, 4, 2, 12.5f, 9f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut_b", 0, 38, 15, 2, 4, 2, 13.55f, 8.6f, -1f, new float[] { 14.55f, 10.6f, 0f }, 35f),
        new BoxSpec("strut_c", 0, 116, 0, 2, 5, 2, 13.85f, 11.85f, -1f, new float[] { 14.85f, 14.35f, 0f }, -38f),
        new BoxSpec("loop_block", 0, 94, 24, 3, 3, 2, 15.5f, 14.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("loop_floor", 0, 40, 37, 7, 1, 2, 18f, 16.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("fg_plate", 0, 22, 37, 7, 1, 2, 30f, 11f, -1f, NO_PIVOT, 0f),
        new BoxSpec("fg_post", 0, 46, 15, 1, 3, 2, 30f, 12f, -1f, NO_PIVOT, 0f),
        new BoxSpec("fg_foot", 0, 118, 33, 2, 1, 2, 31f, 14f, -1f, NO_PIVOT, 0f),
        new BoxSpec("fg_slab_dark", 0, 58, 37, 5, 1, 2, 32.2f, 12.55f, -1f, new float[] { 34.7f, 13.05f, 0f }, -36f),
        new BoxSpec("fg_slab_light", 0, 72, 37, 5, 1, 2, 32.2f, 13.65f, -1f, new float[] { 34.7f, 14.15f, 0f }, -36f),
        new BoxSpec("fg_slab_wide", 0, 40, 30, 5, 1, 3, 32.2f, 13.15f, -1.5f, new float[] { 34.7f, 13.65f, 0f }, -36f),
        new BoxSpec("fg_bar_a", 0, 120, 29, 1, 1, 3, 30.5f, 14.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("fg_bar_b", 0, 80, 33, 2, 1, 3, 31f, 14.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("fg_tip", 0, 48, 34, 2, 1, 2, 31f, 15f, -1f, NO_PIVOT, 0f),
        new BoxSpec("butt_core", 0, 10, 0, 2, 5, 5, 0.6f, 6.1f, -2.5f, new float[] { 2f, 8.75f, 0f }, -10f),
        new BoxSpec("butt_top", 0, 20, 22, 2, 1, 4, 0.6f, 5f, -2f, new float[] { 2.2f, 8.65f, 0f }, -10f),
        new BoxSpec("butt_bottom", 0, 110, 19, 2, 1, 4, 0.6f, 11.2f, -2f, new float[] { 1.1f, 8.95f, 0f }, -10f),
        new BoxSpec("cheek", 0, 38, 9, 3, 2, 4, 2f, 8.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("shelf", 0, 60, 19, 9, 1, 4, 5f, 8.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("shelf_tab", 0, 82, 24, 2, 1, 4, 11f, 9.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("low_a", 0, 56, 33, 3, 1, 3, 2f, 10.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("low_b", 0, 102, 29, 6, 1, 3, 5f, 9.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("low_c", 0, 104, 13, 5, 4, 2, 5f, 9.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("low_d", 0, 90, 33, 2, 1, 3, 10f, 10.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_strut", 0, 102, 0, 5, 5, 2, 5.5f, 12.75f, -1f, new float[] { 8f, 15.25f, 0f }, -15f),
        new BoxSpec("rail_dark2", 0, 110, 39, 1, 1, 2, 37f, 3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trig_block", 0, 98, 39, 3, 2, 3, 25.5f, 9f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("trig_block2", 0, 12, 38, 1, 2, 2, 26.5f, 11f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_floor", 0, 116, 39, 3, 0, 2, 21.5f, 12.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_ramp", 0, 0, 40, 3, 0, 2, 24.05f, 11.45f, -1f, new float[] { 25.55f, 11.45f, 0f }, -45f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 2.1 and an idle offset measured from the origin of Techguns' model.
    // The scale follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's
    // precedent), and the idle position puts this model's box centre on the family anchor. The notes' fire kick
    // (up to 7.5 degrees roll, 0.15 back) is applied at that scale: a screen shift d is d / FP_SCALE in model axes.
    /** On-screen length of the rifle in first person, blocks of the scaled frame. */
    private static final double RIFLE_LENGTH = 2.0;
    private static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), RIFLE_LENGTH);
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "pulse_rifle",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private PulseRifleProfile() {}

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
        // Firing: a quick roll spike, 7.5 degrees at the dip (yF = -0.5), and 0.15 block back on screen.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 15.0 * yF(p))
            .translate((p, part) -> FP_X + 0.3 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the rifle rolls 25 degrees and yaws 15 at mid-reload and comes back, over the 45 ticks.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -25.0 * yR(p))
            .rotY((p, part) -> -15.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225 (the notes' Z-family yaw 135, turned to the X-family), scaled for the
        // 3-block model, the grip (texel 20) in the hand. Aimed extra translate (0, 0.1, 0.2).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(1.1)
            .translate(-1.25, -0.6, 0)
            .build();
        // Recoil: the gun pitches up 15 degrees about the hand, in the first quarter of the first half.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .build();
        // Reload: about 30 degrees pitch plus 40 degrees yaw at mid-animation, then back.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: scale 0.8 (the notes), the model centred (centre at texel 24, 9.15) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(-1.5, -0.57, 0)
            .build();
        // Icon: side-on, scale 0.7 (the notes), the model centred in the slot.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.7)
            .translate(-1.5, -0.57, 0)
            .build();
        // Deviation: the first-person flash is a fixed-frame screen effect. Its offset is the M4's solved offset:
        // both rifles are fitted to 2 blocks on screen, so the muzzle projects to nearly the same spot. The notes ask
        // for a blue flash; the shared sheet is used until the blue one is wired. muzzleFlash2 sheet, additive because
        // the
        // GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.023,
            0.005,
            0.372,
            1.85,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(8, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, 0.2 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .scope(new ScopeSpec(SCOPE_TEXTURE, 0.04, 0.02))
            .build();
    }
}
