package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Nether Blaster's model boxes and render transforms, as data. The gun is not registered yet: it is not in
 * {@link ClientGunDefinitions#ALL} until the gameplay batch adds it, together with its gun definition.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/nether_blaster.md}), by an agent that never saw the
 * original model or texture. Nothing is taken from Techguns code, box lists or textures. The look: a stubby neutral
 * gray cannon, a body of three 6x5 blocks with 1 texel slots that show orange glow, side plates with a painted vent on
 * the center block, a 4x4 muzzle plate with a 2x2 orange eye, a top plate with two dark rails and two zero-thickness
 * fins, a 19 long spine with two dark rails, a 7x7x6 rear housing and a skeletal stock (solid collar and an open cage
 * of four 2x2 rails with a ridge plate above and below) pitched 22 degrees up at the rear. The layout lives in
 * {@code tools/asset-gen/gun-paint/nether_blaster.layout.json}; {@code gun_paint.py pack} writes the block below and
 * {@code gun_paint.py paint} paints the 64x64 sheet from {@code gun-paint/nether_blaster.json}. No camo variants.
 *
 * <h2>Glow parts</h2>
 * Deviation: the notes ask for fullbright orange parts and the profile has no per-box lighting flag yet, so the orange
 * is painted into the sheet and carried by the boxes named {@code slot_glow_*} (the two body slots) and
 * {@code eye_glow} (the muzzle eye, 0.05 texel proud of the plate). A later renderer change can draw exactly the boxes
 * named {@code *_glow*} without lighting. The notes' buried glow core and spine bar are left out: they are never
 * visible except through the slots, which the slot boxes fill.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels. Y = 0 is the body barrel axis, X = 0 the middle of the
 * center block, Z = 0 the mid-plane. One texel is a sixteenth of a block. The stock boxes share one pivot (X -14,
 * Y 3.7) and a 22 degree tilt so the rear rises. Deviation: the notes say the model has no moving parts, so the whole
 * gun is one part.
 *
 * <p>
 * Spec: rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class NetherBlasterProfile {

    /** Texture resource paths of the model sheet: only the default, there are no camo variants. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/nether_blaster.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z: the muzzle plate's front (texel 9) on the barrel axis.
     */
    public static final float[] MUZZLE = { 9.0f / 16.0f, 0.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("blk_front", 0, 0, 16, 4, 5, 6, 4f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("blk_center", 0, 22, 14, 6, 5, 6, -3f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("blk_rear", 0, 20, 25, 4, 5, 6, -8f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("slot_glow_f", 0, 48, 0, 1, 4, 5, 3f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("slot_glow_r", 0, 48, 9, 1, 4, 5, -4f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_l", 0, 40, 31, 6, 4, 1, -3f, -2f, 3f, NO_PIVOT, 0f),
        new BoxSpec("plate_r", 0, 46, 36, 6, 4, 1, -3f, -2f, -4f, NO_PIVOT, 0f),
        new BoxSpec("muzzle_plate", 0, 46, 18, 1, 4, 4, 8f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("eye_glow", 0, 56, 18, 1, 2, 2, 8.05f, -1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("top_plate", 0, 0, 43, 14, 1, 4, -8f, -3.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rail_top_l", 0, 36, 50, 13, 1, 1, -7.5f, -3f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_top_r", 0, 0, 54, 13, 1, 1, -7.5f, -3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("fin_l", 0, 28, 54, 10, 1, 0, -4.5f, -4f, 1f, NO_PIVOT, 0f),
        new BoxSpec("fin_r", 0, 28, 55, 10, 1, 0, -4.5f, -4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("spine", 0, 0, 36, 19, 3, 4, -11f, 2.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rail_low_l", 0, 0, 52, 15, 1, 1, -7.5f, 2f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_low_r", 0, 32, 52, 15, 1, 1, -7.5f, 2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("tail", 0, 46, 41, 2, 1, 4, -13f, 3.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("housing", 0, 22, 0, 6, 7, 7, -14f, -3.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("collar", 0, 0, 0, 3, 8, 8, -17f, -4.3f, -4f, new float[] { -14f, 3.7f, 0f }, 22f),
        new BoxSpec("cage_tl", 0, 0, 48, 7, 2, 2, -24f, -4.3f, 2f, new float[] { -14f, 3.7f, 0f }, 22f),
        new BoxSpec("cage_tr", 0, 18, 48, 7, 2, 2, -24f, -4.3f, -4f, new float[] { -14f, 3.7f, 0f }, 22f),
        new BoxSpec("cage_bl", 0, 0, 32, 7, 2, 2, -24f, 1.7f, 2f, new float[] { -14f, 3.7f, 0f }, 22f),
        new BoxSpec("cage_br", 0, 36, 46, 7, 2, 2, -24f, 1.7f, -4f, new float[] { -14f, 3.7f, 0f }, 22f),
        new BoxSpec("ridge_top", 0, 0, 27, 6, 1, 4, -21f, -5.3f, -2f, new float[] { -14f, 3.7f, 0f }, 22f),
        new BoxSpec("ridge_bot", 0, 40, 26, 6, 1, 4, -21f, 3.7f, -2f, new float[] { -14f, 3.7f, 0f }, 22f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.3 and an idle offset measured from the origin of the original
    // model. The scale follows the FirstPersonFit rule with the 2 blocks on screen of the other long guns, and the
    // idle position puts this model's box centre on the family anchor.
    /** On-screen length of the gun in first person, blocks of the scaled frame. */
    private static final double GUN_LENGTH = 2.0;
    private static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), GUN_LENGTH);
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Nether Blaster. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "nether_blaster",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private NetherBlasterProfile() {}

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
            .scale(1.2)
            .translate(-0.55, -0.4, 0)
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
        // Deviation: the first-person flash is a fixed-frame screen effect. Its offset is the bolt-action rifle's,
        // whose muzzle sits at the same place on screen (same 2-block length and anchor). Headless work cannot look at
        // the screen, so this is unverified in game. muzzleFlash2 sheet, additive because the GregGuns sheet is
        // black-backed (docs/assets/MANIFEST.md).
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.055,
            0.005,
            0.357,
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
