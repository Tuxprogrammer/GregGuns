package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The M4 Assault Rifle's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/m4.md}: proportions, silhouettes, palettes), by an agent
 * that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The look:
 * a near-black tactical carbine 69 texels long and 24 tall: a collapsible wedge stock, a 4-wide receiver on a 3-wide
 * lower with a T-shaped charging handle, a stepped flat top (rear-sight stub, a tall red-dot sight with an open window
 * and one red texel, a flat rail), a quad-rail handguard ending in an A-frame front sight and a bare 18-long diamond
 * barrel, a diamond-section vertical foregrip, a raked checkered pistol grip, a flat guard plank, and the one light
 * element, a ribbed gray magazine curving slightly forward. The layout lives in
 * {@code tools/asset-gen/gun-paint/m4.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins
 * and writes the generated block below, and {@code gun_paint.py paint} paints the 128x64 default sheet and the four
 * camo sheets (Swamp, Desert, Ice, Hazard) from the role map {@code gun-paint/m4.json}. {@code M4ProfileTest} checks
 * the boxes against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * X = 0 is the back of the buttpad and Y = 0 the roof of the red-dot sight; Z = 0 is the mid-plane with +Z the
 * shooter's left. One texel is a sixteenth of a block. The model has no moving parts (the whole gun moves as one
 * part). Square bars shown as diamonds use a 45 degree turn about the barrel axis ({@code tiltX}); the foregrip post
 * uses one about the vertical ({@code tiltY}).
 *
 * <h2>Scope</h2>
 * The red-dot sight is the sight: aiming only narrows the field of view (zoom x0.75), so the profile sets no scope
 * overlay.
 *
 * <p>
 * Spec: gun-sheets.md "M4" (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class M4Profile {

    /** Texture resource paths of the model sheet: the default and the four camos (Swamp, Desert, Ice, Hazard). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/m4.png",
        "gregguns:textures/models/guns/m4_camo1.png", "gregguns:textures/models/guns/m4_camo2.png",
        "gregguns:textures/models/guns/m4_camo3.png", "gregguns:textures/models/guns/m4_camo4.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the barrel's tip (texel 69) on its axis. */
    public static final float[] MUZZLE = { 69.0f / 16.0f, 8.0f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole rifle moves as a unit. */
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
        new BoxSpec(
            "fs_brace",
            0,
            30,
            18,
            1,
            4,
            1,
            51.25f,
            2.75f,
            -0.5f,
            new float[] { 51.75f, 4.75f, 0f },
            23f,
            0f,
            0f,
            -0.01f),
        new BoxSpec("fg_mount", 0, 78, 15, 4, 2, 3, 39.5f, 9.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("fg_post", 0, 30, 0, 2, 8, 2, 40.5f, 11.5f, -1f, new float[] { 41.5f, 15.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("receiver", 0, 38, 0, 16, 4, 4, 18f, 6f, -2f, NO_PIVOT, 0f),
        new BoxSpec("lower", 0, 92, 8, 15, 3, 3, 18f, 10f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_upper", 0, 80, 8, 2, 3, 4, 16f, 7f, -2f, NO_PIVOT, 0f),
        new BoxSpec("plate_lower", 0, 110, 20, 2, 1, 3, 16f, 10f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("ch_bar", 0, 60, 20, 1, 1, 4, 16f, 6f, -2f, NO_PIVOT, 0f),
        new BoxSpec("ch_stem", 0, 52, 21, 1, 1, 2, 17f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("top_rail", 0, 0, 23, 15, 1, 2, 18.5f, 5f, -1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
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
        new BoxSpec("dot_base", 0, 38, 8, 9, 3, 4, 24f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("dot_roof", 0, 0, 18, 4, 1, 4, 25f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("dot_ear_l", 0, 78, 27, 4, 2, 1, 25f, 1f, 1f, NO_PIVOT, 0f),
        new BoxSpec("dot_ear_r", 0, 88, 27, 4, 2, 1, 25f, 1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("red_dot", 0, 32, 10, 0, 1, 1, 27f, 1.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 0, 0, 4, 9, 3, 18f, 12f, -1.5f, new float[] { 20f, 12f, 0f }, 25f, 0f, 0f, -0.01f),
        new BoxSpec("trigger", 0, 98, 27, 1, 2, 1, 22f, 12.5f, -0.5f, new float[] { 22.5f, 12.5f, 0f }, 15f),
        new BoxSpec("guard", 0, 64, 27, 5, 1, 2, 20f, 15f, -1f, NO_PIVOT, 0f),
        new BoxSpec("magwell", 0, 92, 14, 8, 3, 3, 25f, 13f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("mag_upper", 0, 64, 8, 6, 5, 2, 26.5f, 16f, -1f, NO_PIVOT, 0f),
        new BoxSpec(
            "mag_lower",
            0,
            36,
            15,
            6,
            4,
            2,
            26.4f,
            20f,
            -1f,
            new float[] { 32.4f, 20f, 0f },
            -9f,
            0f,
            0f,
            -0.01f),
        new BoxSpec("tube", 0, 0, 12, 15, 3, 3, 0f, 7f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("nut", 0, 42, 21, 1, 2, 2, 15f, 7.5f, -1f, new float[] { 15.5f, 8.5f, 0f }, 0f, 45f),
        new BoxSpec("buttpad", 0, 22, 0, 2, 8, 2, 0f, 10f, -1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rear_web", 0, 114, 14, 3, 5, 1, 1f, 9f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_web", 0, 60, 15, 8, 4, 1, 4f, 9f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("bulkhead", 0, 122, 14, 1, 3, 2, 12f, 10f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut_rear", 0, 14, 0, 2, 9, 2, 2.5f, 9.25f, -1f, new float[] { 3.5f, 13.75f, 0f }, 34f),
        new BoxSpec("strut_front", 0, 52, 15, 2, 4, 2, 10f, 13f, -1f, new float[] { 12f, 13f, 0f }, 60f),
        new BoxSpec("joint_a", 0, 120, 20, 2, 2, 2, 7f, 13f, -1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("joint_b", 0, 34, 21, 2, 2, 2, 10f, 11f, -1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.4 and an idle offset measured from the origin of Techguns' model.
    // The scale follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's
    // precedent), and the idle position puts this model's box centre on the family anchor. The notes' small fire kick
    // (5 degrees, 0.15 back) is applied at that scale: a screen shift d is d / FP_SCALE in model axes.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("m4", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, 0 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "m4",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private M4Profile() {}

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
            .translate((p, part) -> FP_X + 0.42 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
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
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
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
        // Ground: scale 0.6 (the notes), the model centred (centre at texel 34.5, 12) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .translate(-2.156, -0.75, 0)
            .build();
        // Icon: side-on, scale 0.48 (the notes), the model centred in the slot.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.48)
            .translate(-2.156, -0.75, 0)
            .build();
        // Deviation: the first-person flash is a fixed-frame screen effect. Its offset is the bolt-action's solved
        // offset moved by the difference between the two muzzles' projected positions (this model is longer and
        // taller, so FP_SCALE differs and the muzzle sits elsewhere). muzzleFlash2 sheet, additive because the
        // GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
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
            .timing(5, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
