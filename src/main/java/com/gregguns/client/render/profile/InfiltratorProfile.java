package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

/**
 * The Infiltrator's model boxes and render transforms, as data: the M4 skeleton with a sniper scope, a long silencer,
 * a laser module and a laser beam.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/m4.md}, the "Infiltrator delta" section on top of the M4
 * notes), by an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or
 * textures. The layout is the M4's ({@code gun-paint/m4.layout.json}) with the red-dot sight removed and: a scope (a
 * 3-wide mount, a 4x4x8 tube, a diamond objective bell and a diamond eyepiece, each with a light-gray lens face, its
 * top
 * at texel -2), an 18-long diamond silencer over the barrel tip (muzzle at texel 76), a laser box under the handguard
 * front, the foregrip moved back by one, and two zero-thickness red planes crossing as a cross-section, 60 long, which
 * are the laser beam. The beam is its own model part (part 1) so the inventory icon, whose transform program scales
 * that part to nothing, never draws it. The layout lives in {@code tools/asset-gen/gun-paint/infiltrator.layout.json};
 * {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins and writes the generated block below, and
 * {@code gun_paint.py paint} paints the one 128x64 sheet (no camo) from {@code gun-paint/infiltrator.json}.
 * {@code InfiltratorProfileTest} checks the boxes against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family, as the M4: muzzle toward +X, Y growing downward, in texels; X = 0 is the back of the buttpad; Y = 0 is
 * where the M4's red-dot roof was (the scope now rises to about -2); Z = 0 is the mid-plane. One texel is a sixteenth
 * of a block. Square bars shown as diamonds use a 45 degree turn about the barrel axis ({@code tiltX}).
 *
 * <h2>Scope</h2>
 * Aiming (zoom x0.5) shows a sniper-scope overlay, shared with the bolt-action rifle, which kicks with recoil. The gun
 * is silenced, so it draws no muzzle flash at all.
 *
 * <p>
 * Spec: model-notes/m4.md "Infiltrator delta"; rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class InfiltratorProfile {

    /** Texture resource paths of the model sheet: one skin, no camo. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/infiltrator.png" };
    /** The texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the silencer's tip (texel 76) on the bore axis. */
    public static final float[] MUZZLE = { 76.0f / 16.0f, 8.0f / 16.0f, 0.0f };

    /** The sniper-scope overlay shared with the bolt-action rifle. */
    public static final String SCOPE_TEXTURE = "gregguns:textures/fx/testscope.png";

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Part 0: the rifle. Part 1: the laser beam planes, which the icon hides. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("barrel", 0, 70, 20, 18, 2, 2, 51f, 7f, -1f, new float[] { 51f, 8f, 0f }, 0f, 45f),
        new BoxSpec("collar", 0, 116, 0, 2, 4, 4, 34f, 6f, -2f, new float[] { 34f, 8f, 0f }, 0f, 45f),
        new BoxSpec("hg_core", 0, 78, 0, 15, 4, 4, 36f, 6f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hg_top", 0, 34, 25, 13, 1, 2, 37f, 5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("hg_bottom", 0, 70, 24, 13, 1, 2, 37f, 10f, -1f, NO_PIVOT, 0f),
        new BoxSpec("hg_side_l", 0, 100, 24, 13, 2, 1, 37f, 7f, 2f, NO_PIVOT, 0f),
        new BoxSpec("hg_side_r", 0, 0, 26, 13, 2, 1, 37f, 7f, -3f, NO_PIVOT, 0f),
        new BoxSpec("fs_post", 0, 48, 21, 1, 3, 1, 53f, 4f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("fs_hood", 0, 28, 26, 2, 2, 1, 52f, 2f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("fs_brace", 0, 30, 18, 1, 4, 1, 51.25f, 2.75f, -0.5f, new float[] { 51.75f, 4.75f, 0f }, 23f),
        new BoxSpec("fg_mount", 0, 78, 15, 4, 2, 3, 38.5f, 9.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("fg_post", 0, 30, 0, 2, 8, 2, 39.5f, 11.5f, -1f, new float[] { 40.5f, 15.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("receiver", 0, 38, 0, 16, 4, 4, 18f, 6f, -2f, NO_PIVOT, 0f),
        new BoxSpec("lower", 0, 92, 8, 15, 3, 3, 18f, 10f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_upper", 0, 80, 8, 2, 3, 4, 16f, 7f, -2f, NO_PIVOT, 0f),
        new BoxSpec("plate_lower", 0, 110, 20, 2, 1, 3, 16f, 10f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("ch_bar", 0, 60, 20, 1, 1, 4, 16f, 6f, -2f, NO_PIVOT, 0f),
        new BoxSpec("ch_stem", 0, 52, 21, 1, 1, 2, 17f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("top_rail", 0, 0, 23, 15, 1, 2, 18.5f, 5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rear_sight", 0, 16, 18, 4, 2, 3, 19f, 4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec(
            "fwd_assist",
            0,
            22,
            10,
            4,
            1,
            1,
            17f,
            7.2f,
            -2.49f,
            new float[] { 19f, 7.7f, -1.99f },
            0f,
            0f,
            -15f),
        new BoxSpec("grip", 0, 0, 0, 4, 9, 3, 18f, 12f, -1.5f, new float[] { 20f, 12f, 0f }, 25f),
        new BoxSpec("trigger", 0, 98, 27, 1, 2, 1, 22f, 12.5f, -0.5f, new float[] { 22.5f, 12.5f, 0f }, 15f),
        new BoxSpec("guard", 0, 64, 27, 5, 1, 2, 20f, 15f, -1f, NO_PIVOT, 0f),
        new BoxSpec("magwell", 0, 92, 14, 8, 3, 3, 25f, 13f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("mag_upper", 0, 64, 8, 6, 5, 2, 26.5f, 16f, -1f, NO_PIVOT, 0f),
        new BoxSpec("mag_lower", 0, 36, 15, 6, 4, 2, 26.4f, 20f, -1f, new float[] { 32.4f, 20f, 0f }, -9f),
        new BoxSpec("tube", 0, 0, 12, 15, 3, 3, 0f, 7f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("nut", 0, 42, 21, 1, 2, 2, 15f, 7.5f, -1f, new float[] { 15.5f, 8.5f, 0f }, 0f, 45f),
        new BoxSpec("buttpad", 0, 22, 0, 2, 8, 2, 0f, 10f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rear_web", 0, 114, 14, 3, 5, 1, 1f, 9f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_web", 0, 60, 15, 8, 4, 1, 4f, 9f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("bulkhead", 0, 122, 14, 1, 3, 2, 12f, 10f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut_rear", 0, 14, 0, 2, 9, 2, 2.5f, 9.25f, -1f, new float[] { 3.5f, 13.75f, 0f }, 34f),
        new BoxSpec("strut_front", 0, 52, 15, 2, 4, 2, 10f, 13f, -1f, new float[] { 12f, 13f, 0f }, 60f),
        new BoxSpec("joint_a", 0, 120, 20, 2, 2, 2, 7f, 13f, -1f, NO_PIVOT, 0f),
        new BoxSpec("joint_b", 0, 34, 21, 2, 2, 2, 10f, 11f, -1f, NO_PIVOT, 0f),
        new BoxSpec("sc_mount", 0, 42, 28, 6, 3, 3, 24f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("sc_tube", 0, 102, 27, 8, 4, 4, 23f, -1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("sc_bell", 0, 78, 27, 3, 4, 4, 31f, -1.1f, -2f, new float[] { 32.5f, 0.9f, 0f }, 0f, 45f),
        new BoxSpec("sc_eye", 0, 54, 8, 2, 3, 3, 21f, -0.5f, -1.5f, new float[] { 22f, 1f, 0f }, 0f, 45f),
        new BoxSpec("silencer", 0, 0, 29, 18, 3, 3, 58f, 6.5f, -1.5f, new float[] { 67f, 8f, 0f }, 0f, 45f),
        new BoxSpec("laser", 0, 38, 8, 5, 4, 3, 44.5f, 9.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("beam_v", 1, 0, 36, 60, 1, 0, 49f, 11.5f, 0f, NO_PIVOT, 0f),
        new BoxSpec("beam_h", 1, 0, 35, 60, 0, 1, 49f, 12f, -0.5f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.4 and an idle offset measured from the origin of Techguns' model.
    // The scale follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's
    // precedent), and the idle position puts this model's box centre on the family anchor. The notes' small fire kick
    // (5 degrees, 0.15 back) is applied at that scale: a screen shift d is d / FP_SCALE in model axes.
    /** On-screen length of the rifle in first person, blocks of the scaled frame. */
    private static final double RIFLE_LENGTH = 2.0;
    // The fit is taken on the rifle alone (part 0): the 60-long beam planes must not shrink or shift the gun.
    private static final BoxSpec[] RIFLE = java.util.Arrays.stream(BOXES)
        .filter(b -> b.part() == 0)
        .toArray(BoxSpec[]::new);
    private static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(RIFLE, MODEL_SCALE), RIFLE_LENGTH);
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(RIFLE, MODEL_SCALE));
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "infiltrator",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private InfiltratorProfile() {}

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
        // Firing: a light muzzle-up kick, 5 degrees at the dip (yF = -0.5), and 0.15 block back on screen.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 10.0 * yF(p))
            .translate((p, part) -> FP_X + 0.3 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the barrel dips 25 degrees at mid-reload and comes back, over the 45 ticks.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -25.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225 (the notes' Z-family yaw 135, turned to the X-family), scaled for the
        // 4.3-block model (the bolt-action's 1.2 for its 3.3 blocks). Aimed extra translate (0, 0.1, 0).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.9)
            .translate(-1.2, -0.75, 0)
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
        // Ground: scale 0.57 (the notes' 0.6, the rifle being 76 long), the model centred (texel 38, 11.5) so it lies
        // flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.57)
            .translate(-2.375, -0.72, 0)
            .build();
        // Icon: side-on, scale 0.44 (the notes' 0.48, 76 long), the model centred in the slot.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.44)
            .translate(-2.375, -0.72, 0)
            // The laser beam (part 1) is drawn in every view except the icon: scale it to nothing here.
            .scale((p, part) -> part == 1 ? 0.0 : 1.0)
            .build();
        // Silenced: no muzzle flash in any view (the notes), so no MuzzleFlashSpec.
        return GunTransforms.builder()
            .parts(2)
            .timing(5, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .scope(new ScopeSpec(SCOPE_TEXTURE, 0.04, 0.02))
            .build();
    }
}
