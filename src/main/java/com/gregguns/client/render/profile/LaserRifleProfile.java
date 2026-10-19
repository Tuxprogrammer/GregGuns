package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Laser Rifle's model boxes and render transforms, as data. Listed in
 * {@link ClientGunDefinitions#ALL}; the gun definition is {@code LaserRifleDef}.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/laser_rifle.md}: proportions, silhouettes, palettes), by
 * an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures.
 * The look: a 53 texel long sci-fi carbine with a light-gray receiver, a 16 long H-section girder barrel whose two red
 * windows per side are painted and also laid over it as separate zero-thickness glow planes, a dark collar and a
 * light ring with a hollow plus-section muzzle tube, a dark red-windowed energy cell that sticks out under the
 * receiver, a gate rear sight with a red dot, a grip raked 20 degrees, and an open wire stock with a kinked lower rod.
 * The layout lives in {@code tools/asset-gen/gun-paint/laser_rifle.layout.json}; {@code tools/asset-gen/gun_paint.py
 * pack} assigns the UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the 64x64
 * sheet from the role map {@code gun-paint/laser_rifle.json}. There are no camo variants.
 *
 * <h2>Glow parts</h2>
 * Deviation: the notes ask for fullbright red parts. The profile has no per-box lighting flag yet, so the red is
 * painted into the sheet and the same red is also carried by separate zero-thickness boxes named {@code gl_*} (barrel
 * sides), {@code gt_*} and {@code gb_*} (barrel top and bottom), {@code cell_glow_*} (energy cell) and {@code dot}
 * (rear sight). A later renderer change can draw exactly those boxes without lighting. They sit 0.02 texel outside the
 * surface they light, so they do not flicker against it.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, as authored. Y = 0 is
 * the receiver's vertical mid-line and the barrel axis, X = 0 a point 10 texels in front of the receiver's rear face
 * (the butt plate is at X -22); Z = 0 is the mid-plane. One texel is a sixteenth of a block. The model has no moving
 * parts (the whole gun moves as one part).
 *
 * <p>
 * Spec: sheets-energy-special.md "Laser Rifle" (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class LaserRifleProfile {

    /** Texture resource paths of the model sheet: only the default, there are no camo variants. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/laser_rifle.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the tube's front (texel 31) on the barrel axis. */
    public static final float[] MUZZLE = { 31.0f / 16.0f, 0.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("butt_mid", 0, 42, 10, 2, 6, 4, -22f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("butt_cap", 0, 52, 40, 2, 1, 3, -22f, -3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt_foot", 0, 52, 28, 2, 4, 3, -22f, 4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_up", 0, 40, 48, 10, 1, 2, -20f, -2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rod_low1", 0, 52, 51, 4, 1, 2, -20f, 3.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rod_diag", 0, 40, 51, 4, 1, 2, -16f, 3.5f, -1f, new float[] { -16f, 4.5f, 0f }, -44f),
        new BoxSpec("rod_low2", 0, 0, 52, 4, 1, 2, -13.8f, 1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("receiver", 0, 0, 12, 17, 6, 4, -10f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("strip_l", 0, 8, 46, 15, 2, 1, -8f, -1f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("strip_r", 0, 8, 46, 15, 2, 1, -8f, -1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("underplate", 0, 0, 42, 7, 1, 3, -9f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_step", 0, 34, 22, 1, 2, 3, -10f, -4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("top_rail", 0, 8, 49, 14, 1, 2, -9f, -4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("sight_base", 0, 34, 41, 6, 2, 3, -5f, -5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("post_l", 0, 0, 46, 3, 3, 1, -2.5f, -7.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("post_r", 0, 0, 46, 3, 3, 1, -2.5f, -7.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("bridge", 0, 12, 52, 3, 1, 2, -2.5f, -7.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("dot", 0, 60, 0, 0, 1, 1, -1f, -6.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 54, 10, 3, 8, 2, -8f, 4f, -1f, new float[] { -5f, 4f, 0f }, 20f),
        new BoxSpec("trigger", 0, 58, 20, 2, 1, 1, -6f, 4f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("cell", 0, 0, 0, 8, 6, 6, -1f, 1f, -3f, NO_PIVOT, 0f),
        new BoxSpec("cell_base", 0, 8, 22, 8, 1, 5, -1f, 7f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("cell_glow_l", 0, 0, 30, 4, 4, 0, 1f, 2f, 3.02f, NO_PIVOT, 0f),
        new BoxSpec("cell_glow_r", 0, 0, 30, 4, 4, 0, 1f, 2f, -3.02f, NO_PIVOT, 0f),
        new BoxSpec("collar", 0, 28, 0, 2, 4, 6, 7f, -2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("collar_top", 0, 34, 35, 2, 1, 5, 7f, -3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("collar_bot", 0, 34, 35, 2, 1, 5, 7f, 2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_l", 0, 0, 35, 16, 6, 1, 9f, -3f, 1f, NO_PIVOT, 0f),
        new BoxSpec("plate_r", 0, 0, 35, 16, 6, 1, 9f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("flange_t", 0, 8, 28, 16, 1, 6, 9f, -2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("flange_b", 0, 8, 28, 16, 1, 6, 9f, 1f, -3f, NO_PIVOT, 0f),
        new BoxSpec("band_post", 0, 0, 22, 2, 6, 2, 16f, -3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("band_cross", 0, 42, 20, 2, 2, 6, 16f, -1f, -3f, NO_PIVOT, 0f),
        new BoxSpec("gl_a_l", 0, 28, 10, 7, 2, 0, 9f, -1f, 2.02f, NO_PIVOT, 0f),
        new BoxSpec("gl_a_r", 0, 28, 10, 7, 2, 0, 9f, -1f, -2.02f, NO_PIVOT, 0f),
        new BoxSpec("gl_b_l", 0, 28, 10, 7, 2, 0, 18f, -1f, 2.02f, NO_PIVOT, 0f),
        new BoxSpec("gl_b_r", 0, 28, 10, 7, 2, 0, 18f, -1f, -2.02f, NO_PIVOT, 0f),
        new BoxSpec("gt_a", 0, 22, 52, 7, 0, 2, 9f, -2.02f, -1f, NO_PIVOT, 0f),
        new BoxSpec("gt_b", 0, 22, 52, 7, 0, 2, 18f, -2.02f, -1f, NO_PIVOT, 0f),
        new BoxSpec("gb_a", 0, 22, 52, 7, 0, 2, 9f, 2.02f, -1f, NO_PIVOT, 0f),
        new BoxSpec("gb_b", 0, 22, 52, 7, 0, 2, 18f, 2.02f, -1f, NO_PIVOT, 0f),
        new BoxSpec("ring", 0, 44, 0, 2, 4, 6, 25f, -2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("ring_top", 0, 48, 35, 2, 1, 4, 25f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("ring_bot", 0, 48, 35, 2, 1, 4, 25f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("tube_t", 0, 20, 42, 4, 1, 3, 27f, -2.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_b", 0, 20, 42, 4, 1, 3, 27f, 1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_l", 0, 52, 44, 4, 3, 1, 27f, -1.5f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_r", 0, 52, 44, 4, 3, 1, 27f, -1.5f, -2.5f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.3 and an idle offset measured from the origin of Techguns' model.
    // InHandFit solves the scale and the idle offset from the per-gun targets in InHandTargets (the in-hand notes,
    // section 7), so this model's length, centre and muzzle land where the original's do on screen.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("laser_rifle", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, -0.2 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Laser Rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "laser_rifle",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private LaserRifleProfile() {}

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
        // Firing: the gun slides back along its axis (the notes: up to 0.25 block) and eases forward; no roll.
        TransformProgram fpFire = TransformProgram.builder()
            .translate((p, part) -> FP_X + 0.25 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the gun rolls 30 degrees about Z and yaws 15 degrees at mid-reload and comes back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -30.0 * yR(p))
            .rotY((p, part) -> -15.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225, scale 1.2 like the other 3.3 block long guns. The grip (about 0.3 block
        // behind the origin, 0.5 down) lands at the hand. Aimed extra (0, 0.1, -0.2), the notes.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: the gun pitches slightly about the hand, in the first quarter of the first half.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -12.0 * kick(p)))
            .build();
        // Reload: about 40 degrees pitch plus 50 degrees yaw at mid-animation, then back (the notes).
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 40.0 * yR(p))
            .rotY((p, part) -> 50.0 * yR(p))
            .build();
        // Ground: scale 0.75 (the notes), the model centred (centre at texel 4.5, 2) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.75)
            .translate(0, -0.15, 0)
            .build();
        // Icon: side-on, scale 0.6 so the 3.3 block model fits the slot, centred in it.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.6)
            .translate(-0.28, -0.125, 0)
            .build();
        // Deviation: the first-person flash is a fixed-frame screen effect, solved from this model's muzzle in the
        // fitted frame. Headless work cannot look at the screen, so this is unverified in game. muzzleFlash2 sheet,
        // additive because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
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
            .thirdPerson(tpBase, new double[] { 0, 0.1, -0.2 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
