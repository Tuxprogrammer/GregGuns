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
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("receiver", 0, 22, 0, 27, 5, 5, 14f, 1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("top_plate", 0, 0, 18, 27, 1, 4, 15f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("top_rail", 0, 72, 30, 12, 1, 2, 23f, -1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("lower_body", 0, 68, 16, 15, 3, 3, 18f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("side_front_p", 0, 0, 27, 6, 3, 1, 41f, 1.5f, 1.5f, new float[] { 41f, 3f, 2f }, 0f, 0f, 5f),
        new BoxSpec("side_front_n", 0, 88, 22, 6, 3, 1, 41f, 1.5f, -2.5f, new float[] { 41f, 3f, -2f }, 0f, 0f, -5f),
        new BoxSpec("side_rear_p", 0, 28, 27, 6, 3, 1, 8f, 1.5f, 1.5f, new float[] { 14f, 3f, 2f }, 0f, 0f, -10f),
        new BoxSpec("side_rear_n", 0, 14, 27, 6, 3, 1, 8f, 1.5f, -2.5f, new float[] { 14f, 3f, -2f }, 0f, 0f, 10f),
        new BoxSpec("front_box", 0, 22, 10, 19, 4, 4, 41f, 1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("ridge_p", 0, 24, 31, 6, 1, 1, 42f, 0f, 0.5f, new float[] { 42f, 0.5f, 1f }, 10f),
        new BoxSpec("ridge_n", 0, 104, 7, 6, 1, 1, 42f, 0f, -1.5f, new float[] { 42f, 0.5f, -1f }, 10f),
        new BoxSpec("handguard", 0, 104, 0, 8, 4, 3, 35f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("pipe", 0, 0, 23, 16, 2, 2, 33f, 5.9f, -1f, new float[] { 33f, 6.9f, 0f }, 0f, 45f),
        new BoxSpec("pipe_cap", 0, 64, 27, 2, 2, 2, 49f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut", 0, 114, 30, 5, 1, 2, 50f, 4f, -1f, new float[] { 55f, 4.5f, 0f }, -36.5f),
        new BoxSpec("hood", 0, 8, 12, 3, 1, 4, 60f, 1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("slant", 0, 104, 16, 4, 1, 4, 59f, 1.5f, -2f, new float[] { 63f, 2f, 0f }, -45f),
        new BoxSpec("stair_a", 0, 118, 21, 1, 1, 4, 60.8f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stair_b", 0, 68, 10, 1, 2, 4, 59.8f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stair_c", 0, 62, 22, 1, 1, 4, 59.2f, 4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_block", 0, 86, 9, 15, 4, 3, 0f, 1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_cover", 0, 36, 23, 6, 1, 3, 9f, 0f, -1.5f, new float[] { 15f, 0.5f, 0f }, -10f),
        new BoxSpec("spine_a", 0, 78, 10, 3, 5, 1, 9f, 5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("spine_b", 0, 98, 0, 2, 7, 1, 7f, 5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("spine_c", 0, 86, 0, 5, 8, 1, 2f, 5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_front", 0, 0, 12, 2, 4, 2, 12f, 5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bottom_bar", 0, 42, 27, 4, 2, 2, 2f, 12f, -1f, NO_PIVOT, 0f),
        new BoxSpec("butt_plate", 0, 14, 0, 2, 10, 2, 0f, 4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("brace", 0, 88, 26, 15, 2, 2, 4f, 6f, -1f, new float[] { 19f, 7f, 0f }, -27.5f),
        new BoxSpec("grip", 0, 0, 0, 4, 9, 3, 18f, 9f, -1.5f, new float[] { 22f, 9f, 0f }, 25.6f),
        new BoxSpec("guard_slope", 0, 100, 30, 5, 1, 2, 24f, 9f, -1f, new float[] { 29f, 9f, 0f }, -36f),
        new BoxSpec("guard_floor", 0, 72, 27, 5, 1, 2, 20f, 11f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 62, 18, 1, 2, 1, 22.5f, 8.5f, -0.5f, new float[] { 23f, 8.5f, 0f }, -15f),
        new BoxSpec("sight_base", 0, 104, 21, 3, 1, 4, 24f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("sight_post_p", 0, 84, 22, 1, 4, 1, 25f, -4.5f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_post_n", 0, 80, 22, 1, 4, 1, 25f, -4.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_glow_window", 0, 122, 7, 0, 3, 3, 25.5f, -4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("batt_plate", 0, 54, 27, 4, 3, 1, 25.3f, 2f, 2.5f, NO_PIVOT, 0f),
        new BoxSpec("batt_cap_top", 0, 12, 31, 4, 1, 2, 25.3f, 0.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("batt_cap_bot", 0, 0, 31, 4, 1, 2, 25.3f, 5.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec(
            "batt_rod_glow_1",
            0,
            122,
            13,
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
            72,
            22,
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
            76,
            22,
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
        new BoxSpec("batt_foot", 0, 54, 23, 3, 3, 1, 29f, 1.9f, 2.5f, new float[] { 29f, 3.4f, 3f }, 0f, 0f, 20f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.3 on a model measured from its own origin. The scale follows
    // the FirstPersonFit rule with the 2 blocks on screen of the other long guns, and the idle position puts this
    // model's box centre on the family anchor.
    /** On-screen length of the gun in first person, blocks of the scaled frame. */
    private static final double GUN_LENGTH = 2.0;
    private static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), GUN_LENGTH);
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));
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
        // Third person: rotX180, rotY225, scale 1.0 (the notes' 0.8 is for their own origin; this rifle is 3.9 blocks
        // long, so it is scaled like the other long guns; unverified in game). The grip (about 1.2 blocks ahead of the
        // butt, 0.8 down)
        // lands at the hand.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(1.0)
            .translate(-1.16, -0.81, 0)
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
        // Deviation: the notes ask for a laser-style flash; the shared muzzleFlash2 sheet is used, additive (the
        // sheet is black-backed). The offset is the bolt-action rifle's,
        // whose muzzle sits at the same place on screen (same 2-block length and anchor). Unverified in game.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.055,
            0.005,
            0.357,
            1.85,
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
