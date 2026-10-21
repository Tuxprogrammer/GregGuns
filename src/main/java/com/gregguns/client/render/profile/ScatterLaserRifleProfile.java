package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Scatter Laser Rifle's model boxes and render transforms, as data. The gun is not registered yet: it is not in
 * {@link ClientGunDefinitions#ALL} until the gameplay batch adds it, together with its gun definition.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/scatter_laser_rifle.md}: proportions, silhouettes,
 * palettes), by an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists
 * or textures, and the Laser Rifle model is not reused. The look: a 63 texel long, 21.5 tall dark-gray boxy energy
 * rifle. A 5 wide receiver (a top plate, a rail and a lower body) carries a holographic sight (base, two posts and a
 * transparent window with one red dot) and feeds a 4 wide front box with two sloping ridges and a handguard. The
 * muzzle is a 3 long hood over a 45-degree slanted plate and a three-block stair. A light-gray diamond pipe hangs
 * under the front with a square cap and a slanted strut. The stock is a 3 wide dark block with a cheek cover, three
 * stepped light-gray spine panels with cut-outs, a bottom bar, a tall butt plate and a diagonal brace that leaves an
 * open triangle. The grip is raked 25.6 degrees inside an open guard with a small trigger. A battery kit on the +Z
 * side only has a light plate, two caps, a mount foot and three yellow-green diamond rods. Orange bar lights and
 * vent slits are painted on the receiver and front box.
 * The layout lives in {@code tools/asset-gen/gun-paint/scatter_laser_rifle.layout.json}; {@code
 * tools/asset-gen/gun_paint.py pack} assigns the UV origins and writes the generated block below, and {@code
 * gun_paint.py paint} paints the 128x128 sheet from the role map {@code gun-paint/scatter_laser_rifle.json}. The
 * notes list no camo variants.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels, as authored. X = 0 is the butt plate's rear face, Y = 0
 * the top face of the receiver's top plate, Z = 0 the mid-plane; +Z is the battery side. One texel is a sixteenth of
 * a block. The model has no moving parts (the whole gun moves as one part).
 *
 * <h2>Glow and cut-outs</h2>
 * The three battery rods ({@code batt_rod_glow_*}, yellow-green) and the sight window ({@code sight_glow_window}, a
 * zero-thickness plane whose texels are transparent except one red dot) are meant to be drawn fullbright. The profile
 * has no emissive flag yet; the names are stable so the renderer can pick them out later. The window must keep its
 * alpha: it is a cut-out, never an opaque fill. The spine panels also carry a few transparent 1x1 holes.
 *
 * <p>
 * Spec: gun-sheets.md (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class ScatterLaserRifleProfile {

    /** Texture resource paths of the model sheet: the default only (the notes list no camo). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/scatter_laser_rifle.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 128;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the hood tip (texel 63) on the front box axis (3). */
    public static final float[] MUZZLE = { 63.0f / 16.0f, 3.0f / 16.0f, 0.0f };
    /** Frames of {@code laserflare02.png} that are painted (checked by MuzzleSheetsTest). */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("receiver", 0, 22, 0, 27, 5, 5, 14f, 1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("top_plate", 0, 0, 18, 27, 1, 4, 15f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("top_rail", 0, 82, 27, 12, 1, 2, 23f, -1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("lower_body", 0, 68, 14, 15, 3, 3, 18f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("side_front_p", 0, 50, 25, 6, 3, 1, 41f, 1.5f, 1.5f, new float[] { 41f, 3f, 2f }, 0f, 0f, 5f),
        new BoxSpec("side_front_n", 0, 36, 23, 6, 3, 1, 41f, 1.5f, -2.5f, new float[] { 41f, 3f, -2f }, 0f, 0f, -5f),
        new BoxSpec("side_rear_p", 0, 0, 27, 6, 3, 1, 8f, 1.5f, 1.5f, new float[] { 14f, 3f, 2f }, 0f, 0f, -10f),
        new BoxSpec("side_rear_n", 0, 64, 25, 6, 3, 1, 8f, 1.5f, -2.5f, new float[] { 14f, 3f, -2f }, 0f, 0f, 10f),
        new BoxSpec("front_box", 0, 22, 10, 19, 4, 4, 41f, 1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("ridge_p", 0, 14, 31, 6, 1, 1, 42f, 0f, 0.5f, new float[] { 42f, 0.5f, 1f }, 10f),
        new BoxSpec("ridge_n", 0, 0, 31, 6, 1, 1, 42f, 0f, -1.5f, new float[] { 42f, 0.5f, -1f }, 10f),
        new BoxSpec("handguard", 0, 86, 7, 8, 4, 3, 35f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("pipe", 0, 0, 23, 16, 2, 2, 33f, 5.9f, -1f, new float[] { 33f, 6.9f, 0f }, 0f, 45f),
        new BoxSpec("pipe_cap", 0, 34, 27, 2, 2, 2, 49f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut", 0, 64, 29, 5, 1, 2, 50f, 3.25f, -1f, new float[] { 55f, 3.75f, 0f }, -36.5f),
        new BoxSpec("hood", 0, 108, 13, 3, 1, 4, 60f, 1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("slant", 0, 0, 12, 4, 1, 4, 58.65f, 1.3f, -2f, new float[] { 62.65f, 1.8f, 0f }, -45f),
        new BoxSpec("stair_a", 0, 118, 18, 1, 1, 4, 60.8f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stair_b", 0, 108, 7, 1, 2, 4, 59.8f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stair_c", 0, 62, 20, 1, 1, 4, 59.2f, 4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_block", 0, 86, 0, 15, 4, 3, 0f, 1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_cover", 0, 68, 10, 6, 1, 3, 9f, 0f, -1.5f, new float[] { 15f, 0.5f, 0f }, -10f),
        new BoxSpec("spine_top", 0, 42, 29, 10, 2, 1, 2f, 5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("spine_up_1", 0, 104, 14, 1, 3, 1, 2f, 7f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("spine_up_2", 0, 78, 25, 1, 3, 1, 6f, 7f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("spine_up_3", 0, 124, 27, 1, 2, 1, 9f, 7f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("spine_low_c", 0, 102, 30, 5, 2, 1, 2f, 10f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("spine_low_b", 0, 98, 20, 2, 2, 1, 7f, 10f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("spine_low_a", 0, 50, 23, 3, 1, 1, 9f, 9f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_front", 0, 118, 7, 2, 4, 2, 12f, 5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bottom_bar", 0, 14, 27, 4, 2, 2, 2f, 12f, -1f, NO_PIVOT, 0f),
        new BoxSpec("butt_plate", 0, 14, 0, 2, 10, 2, 0f, 4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("brace", 0, 84, 23, 15, 2, 2, 4f, 5f, -1f, new float[] { 19f, 6f, 0f }, -27.5f),
        new BoxSpec("grip", 0, 0, 0, 4, 9, 3, 18f, 9f, -1.5f, new float[] { 22f, 9f, 0f }, 25.6f),
        new BoxSpec("guard_slope", 0, 110, 27, 5, 1, 2, 24f, 8.25f, -1f, new float[] { 29f, 8.25f, 0f }, -36f),
        new BoxSpec("guard_floor", 0, 84, 20, 5, 1, 2, 20f, 11f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 114, 30, 1, 2, 1, 22.5f, 8.5f, -0.5f, new float[] { 23f, 8.5f, 0f }, -15f),
        new BoxSpec("sight_base", 0, 104, 18, 3, 1, 4, 24f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("sight_post_p", 0, 80, 20, 1, 4, 1, 25f, -4.5f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_post_n", 0, 76, 20, 1, 4, 1, 25f, -4.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_glow_window", 0, 122, 0, 0, 3, 3, 25.5f, -4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("batt_plate", 0, 118, 23, 4, 3, 1, 25.3f, 2f, 2.5f, NO_PIVOT, 0f),
        new BoxSpec("batt_cap_top", 0, 90, 30, 4, 1, 2, 25.3f, 0.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("batt_cap_bot", 0, 78, 30, 4, 1, 2, 25.3f, 5.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec(
            "batt_rod_glow_1",
            0,
            16,
            12,
            1,
            4,
            1,
            27.8f,
            1.5f,
            2f,
            new float[] { 28.3f, 3.5f, 2.5f },
            0f,
            0f,
            45f),
        new BoxSpec(
            "batt_rod_glow_2",
            0,
            122,
            13,
            1,
            4,
            1,
            26.8f,
            1.5f,
            2f,
            new float[] { 27.3f, 3.5f, 2.5f },
            0f,
            0f,
            45f),
        new BoxSpec(
            "batt_rod_glow_3",
            0,
            72,
            20,
            1,
            4,
            1,
            25.8f,
            1.5f,
            2f,
            new float[] { 26.3f, 3.5f, 2.5f },
            0f,
            0f,
            45f),
        new BoxSpec("batt_foot", 0, 26, 27, 3, 3, 1, 29f, 1.9f, 2.5f, new float[] { 29f, 3.4f, 3f }, 0f, 0f, 20f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.3 on a model measured from its own origin. The scale, idle
    // position and third-person placement are solved by InHandFit from the per-gun targets of the in-hand notes
    // (section 7).
    private static final InHandFit.Placement FIT = InHandFit
        .fit("scatter_laser_rifle", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.2, -0.25 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Scatter Laser Rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "scatter_laser_rifle",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private ScatterLaserRifleProfile() {}

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
        // Firing: the notes give a pitch kick of up to 5 degrees and a pushback of up to 0.15 block.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> -5.0 * yF(p))
            .translate((p, part) -> FP_X - 0.15 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the notes give a pitch of about 50 degrees at mid-animation, rising and falling smoothly.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -50.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225, then the scale and translate InHandFit solved so the box centre lands at the
        // hand and the rifle is the notes' 1.18 world blocks long.
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
        // Ground: scale 0.6 (the notes), the model centred (centre at texel 31.5, 6.25) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .translate(-1.97, -0.39, 0)
            .build();
        // Icon: side-on, the model centred in the slot; 0.45 so the 3.9 block model fits (the notes' 0.48).
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.45)
            .translate(-1.97, -0.39, 0)
            .build();
        // Laser-style flash: laserflare02, additive, scale 1.5 (model-notes/scatter_laser_rifle.md). The offset follows
        // the solved placement (InHandFit.Placement.flashOffset).
        double[] fo = FIT.flashOffset(MUZZLE, 1.5);
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/laserflare02.png",
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
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
