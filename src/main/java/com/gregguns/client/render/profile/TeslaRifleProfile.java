package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Tesla Rifle's model boxes and render transforms, as data. Listed in
 * {@link ClientGunDefinitions#ALL}; the gun definition is {@code TeslaRifleDef}.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/tesla_rifle.md}: proportions, silhouettes, palettes), by
 * an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures.
 * The look: a bulky light-gray laboratory rifle 49.5 texels long and 18.8 tall. A short plus-section barrel (8 long,
 * four dark corner rails running 2 further) wears two coil stations of four 45-degree cubes, each cube carrying a pure
 * blue glow tip; it ends in a navy diamond bushing and a light diamond tip with black ends. Behind sit a washer stack
 * and a 5-cube front block, a foregrip post, a stepped body with two purple-gray capacitor boxes on a spine, two
 * sight posts with a red dot, a grip raked 18 degrees with a heel pad, four diamond rods under the body (the gun is
 * 10.8 wide there) and an open frame stock (two rails with a slot, two diagonal gussets, a stepped butt plate).
 * The layout lives in {@code tools/asset-gen/gun-paint/tesla_rifle.layout.json}; {@code tools/asset-gen/gun_paint.py
 * pack} assigns the UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the
 * 128x64 sheet from the role map {@code gun-paint/tesla_rifle.json}. The notes list no camo variants.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels, as authored. Y = 0.5 is the barrel axis; X = 0 is a point
 * 9 texels behind the barrel's first face; Z = 0 is the mid-plane (the gun is mirror-symmetric). One texel is a
 * sixteenth of a block. The model has no moving parts (the whole gun moves as one part).
 *
 * <h2>Glow</h2>
 * The eight coil tips (boxes named {@code s11_glow_*} and {@code s15_glow_*}, pure #0000FF) and the red dot
 * ({@code red_dot}, a zero-thickness plane) are meant to be drawn fullbright. The profile has no emissive flag yet;
 * the names are stable so the renderer can pick them out later (and pulse the tips if wanted).
 *
 * <p>
 * Spec: gun-sheets.md (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class TeslaRifleProfile {

    /** Texture resource paths of the model sheet: the default only (the notes list no camo). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/tesla_rifle.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the tip (texel 22) on the barrel axis (0.5). */
    public static final float[] MUZZLE = { 22.0f / 16.0f, 0.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("bar_v", 0, 0, 10, 8, 5, 2, 9f, -2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bar_h", 0, 78, 9, 8, 2, 5, 9f, -0.5f, -2.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rail_tl", 0, 72, 28, 10, 1, 1, 9f, -1.5f, -2f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rail_tr", 0, 94, 28, 10, 1, 1, 9f, -1.5f, 1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rail_bl", 0, 28, 28, 10, 1, 1, 9f, 1.5f, -2f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rail_br", 0, 50, 28, 10, 1, 1, 9f, 1.5f, 1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("bushing", 0, 54, 10, 2, 3, 3, 17f, -1f, -1.5f, new float[] { 17f, 0.5f, 0f }, 0f, 45f),
        new BoxSpec("tip", 0, 94, 21, 3, 2, 2, 19f, -0.5f, -1f, new float[] { 19f, 0.5f, 0f }, 0f, 45f),
        new BoxSpec("s11_top", 0, 8, 22, 2, 2, 2, 10f, -3.5f, -1f, new float[] { 11f, -2.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("s11_bot", 0, 120, 20, 2, 2, 2, 10f, 2.5f, -1f, new float[] { 11f, 3.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("s11_side_l", 0, 104, 21, 2, 2, 2, 10f, -0.5f, -4f, new float[] { 11f, 0.5f, -3f }, 45f),
        new BoxSpec("s11_side_r", 0, 0, 22, 2, 2, 2, 10f, -0.5f, 2f, new float[] { 11f, 0.5f, 3f }, 45f),
        new BoxSpec(
            "s11_glow_top",
            0,
            116,
            25,
            1,
            1,
            1,
            10.5f,
            -4f,
            -0.5f,
            new float[] { 11f, -3.5f, 0f },
            0f,
            0f,
            45f),
        new BoxSpec("s11_glow_bot", 0, 124, 24, 1, 1, 1, 10.5f, 4f, -0.5f, new float[] { 11f, 4.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("s11_glow_l", 0, 108, 25, 1, 1, 1, 10.5f, 0f, -4.5f, new float[] { 11f, 0.5f, -4f }, 45f),
        new BoxSpec("s11_glow_r", 0, 112, 25, 1, 1, 1, 10.5f, 0f, 3.5f, new float[] { 11f, 0.5f, 4f }, 45f),
        new BoxSpec("s15_top", 0, 40, 22, 2, 2, 2, 14f, -3.5f, -1f, new float[] { 15f, -2.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("s15_bot", 0, 16, 22, 2, 2, 2, 14f, 2.5f, -1f, new float[] { 15f, 3.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("s15_side_l", 0, 24, 22, 2, 2, 2, 14f, -0.5f, -4f, new float[] { 15f, 0.5f, -3f }, 45f),
        new BoxSpec("s15_side_r", 0, 32, 22, 2, 2, 2, 14f, -0.5f, 2f, new float[] { 15f, 0.5f, 3f }, 45f),
        new BoxSpec("s15_glow_top", 0, 40, 26, 1, 1, 1, 14.5f, -4f, -0.5f, new float[] { 15f, -3.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("s15_glow_bot", 0, 28, 26, 1, 1, 1, 14.5f, 4f, -0.5f, new float[] { 15f, 4.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("s15_glow_l", 0, 32, 26, 1, 1, 1, 14.5f, 0f, -4.5f, new float[] { 15f, 0.5f, -4f }, 45f),
        new BoxSpec("s15_glow_r", 0, 36, 26, 1, 1, 1, 14.5f, 0f, 3.5f, new float[] { 15f, 0.5f, 4f }, 45f),
        new BoxSpec("navy_plate", 0, 104, 0, 1, 4, 4, 8f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("washer_light", 0, 64, 10, 1, 3, 3, 7f, -1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("washer_dark", 0, 114, 0, 1, 4, 4, 6f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("front_block", 0, 32, 0, 5, 5, 5, 1f, -1.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("top_rib", 0, 96, 25, 4, 1, 2, 1.5f, -2.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("belly_block", 0, 0, 17, 3, 2, 3, 3.5f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("foregrip", 0, 104, 8, 2, 6, 2, 4f, 5f, -1f, new float[] { 5f, 8f, 0f }, 0f, 0f, 45f),
        new BoxSpec("lower_body", 0, 0, 0, 10, 4, 6, -8f, 0.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("belly_plate", 0, 54, 16, 8, 1, 5, -7f, 4.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("spine", 0, 80, 16, 8, 2, 3, -7f, -1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("cap1", 0, 112, 8, 3, 3, 4, -6.5f, -2f, -2f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("cap2", 0, 20, 10, 3, 3, 4, -2.5f, -2f, -2f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rear_cheek", 0, 42, 17, 2, 2, 3, -10f, -3f, -1.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rear_rib", 0, 48, 22, 8, 1, 2, -15.5f, -2.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rear_a", 0, 52, 0, 3, 5, 5, -10f, -1.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_b", 0, 78, 0, 4, 5, 4, -14f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rear_c", 0, 34, 10, 3, 4, 3, -17f, -1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("navy_under", 0, 112, 15, 4, 2, 3, -13.5f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("post_l", 0, 124, 0, 1, 3, 1, -7.5f, -6f, 1f, NO_PIVOT, 0f),
        new BoxSpec("post_r", 0, 124, 4, 1, 3, 1, -7.5f, -6f, -2f, NO_PIVOT, 0f),
        new BoxSpec(
            "strut_l",
            0,
            72,
            10,
            1,
            2,
            1,
            -8f,
            -3.5f,
            1.5f,
            new float[] { -7.5f, -2.5f, 2f },
            45f,
            0f,
            0f,
            -0.01f),
        new BoxSpec("strut_r", 0, 72, 13, 1, 2, 1, -8f, -3.5f, -1.5f, new float[] { -7.5f, -2.5f, -1f }, 45f),
        new BoxSpec("red_dot", 0, 124, 26, 0, 1, 2, -7f, -5.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 94, 0, 3, 7, 2, -13f, 5f, -1f, new float[] { -10f, 5f, 0f }, 18f),
        new BoxSpec("heel", 0, 80, 21, 4, 1, 3, -13.5f, 12f, -1.5f, new float[] { -10f, 5f, 0f }, 18f),
        new BoxSpec("trigger", 0, 120, 24, 1, 2, 1, -10f, 4f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_l", 0, 0, 26, 13, 1, 1, -9.5f, 2.7f, 4.2f, new float[] { -9.5f, 3.2f, 4.7f }, 0f, 45f),
        new BoxSpec("rod_r", 0, 0, 28, 13, 1, 1, -9.5f, 2.7f, -5.2f, new float[] { -9.5f, 3.2f, -4.7f }, 0f, 45f),
        new BoxSpec(
            "conn_rear_l",
            0,
            22,
            17,
            1,
            1,
            4,
            -9.5f,
            1.3f,
            0.85f,
            new float[] { -9.5f, 1.8f, 2.85f },
            0f,
            -45f,
            0f,
            -0.01f),
        new BoxSpec(
            "conn_front_l",
            0,
            102,
            16,
            1,
            1,
            4,
            2.5f,
            1.3f,
            0.85f,
            new float[] { 2.5f, 1.8f, 2.85f },
            0f,
            -45f,
            0f,
            -0.01f),
        new BoxSpec(
            "conn_rear_r",
            0,
            32,
            17,
            1,
            1,
            4,
            -9.5f,
            1.3f,
            -4.85f,
            new float[] { -9.5f, 1.8f, -2.85f },
            0f,
            45f,
            0f,
            -0.01f),
        new BoxSpec(
            "conn_front_r",
            0,
            12,
            17,
            1,
            1,
            4,
            2.5f,
            1.3f,
            -4.85f,
            new float[] { 2.5f, 1.8f, -2.85f },
            0f,
            45f,
            0f,
            -0.01f),
        new BoxSpec("butt", 0, 68, 0, 1, 6, 4, -27.5f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("butt_tab", 0, 112, 20, 1, 2, 3, -27.5f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt_back", 0, 46, 10, 1, 4, 3, -26.5f, 1f, -1.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("stock_rail_l", 0, 48, 25, 8, 2, 1, -26.5f, 2f, 1f, NO_PIVOT, 0f),
        new BoxSpec(
            "gusset_l",
            0,
            68,
            22,
            5,
            2,
            1,
            -18.5f,
            2f,
            1f,
            new float[] { -18.5f, 4f, 1.5f },
            -45f,
            0f,
            0f,
            -0.01f),
        new BoxSpec("stock_rail_r", 0, 66, 25, 8, 2, 1, -26.5f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec(
            "gusset_r",
            0,
            84,
            25,
            5,
            2,
            1,
            -18.5f,
            2f,
            -2f,
            new float[] { -18.5f, 4f, -1.5f },
            -45f,
            0f,
            0f,
            -0.01f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale and an idle offset measured from the origin of Techguns' model.
    // InHandFit solves the scale and the idle offset from the per-gun targets in InHandTargets (the in-hand notes,
    // section 7), so this model's length, centre and muzzle land where the original's do on screen.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("tesla_rifle", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.2, -0.25 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Tesla Rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "tesla_rifle",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private TeslaRifleProfile() {}

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
        // Firing: the notes give a slide back along the gun axis of up to 0.25 block and no roll.
        TransformProgram fpFire = TransformProgram.builder()
            .translate((p, part) -> FP_X - 0.25 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the notes give a roll up to 30 degrees, a yaw up to -15 and a shift up to (0.5, 0.25, 0.25).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -30.0 * yR(p))
            .rotY((p, part) -> -15.0 * yR(p))
            .translate(
                (p, part) -> FP_X + 0.5 / FP_SCALE * yR(p),
                (p, part) -> FP_Y + 0.25 / FP_SCALE * yR(p),
                (p, part) -> FP_Z + 0.25 / FP_SCALE * yR(p))
            .build();
        // Third person: rotX180, rotY225, scale 1.25 (the notes). The grip (about 0.75 block behind the origin,
        // 0.5 down) lands at the hand.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -12.0 * kick(p)))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 15.0 * yR(p))
            .rotY((p, part) -> 20.0 * yR(p))
            .build();
        // Ground: scale 0.8 (the notes), the model centred (centre at texel -2.75, 3.4) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(0.17, -0.21, 0)
            .build();
        // Icon: side-on, the model centred in the slot; 0.55 is the notes' scale.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.55)
            .translate(0.17, -0.21, 0)
            .build();
        // Deviation: the notes ask for a lightning-style blue flash; the flash offset is solved from this model's
        // muzzle in the
        // fitted frame. Unverified in game.
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
            .timing(5, 10, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
