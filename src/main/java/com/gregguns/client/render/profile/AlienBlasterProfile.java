package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Alien Blaster's model boxes and render transforms, as data. The gun is not registered yet: it is not in
 * {@link ClientGunDefinitions#ALL} until the gameplay batch adds it, together with its gun definition.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/alien_blaster.md}), by an agent that never saw the
 * original model or texture. Nothing is taken from Techguns code, box lists or textures. The look: a pale silvery
 * energy pistol made of square bars turned 45 degrees about the long axis (so every ring shows as a diamond from the
 * front): a 12 long main body ({@code body}), four stepped rings ({@code step1} to {@code step4}, the last two dark), a
 * 1.4 across needle barrel with two dark collars and a dark 2 long muzzle block, a gray grid ring ({@code rear_ring}),
 * a blue cell and a small axis-aligned nub at the rear, six blue studs ({@code stud_glow_*}) flush on the upper
 * facets, two thin wings swept back 25 degrees, a dorsal fin raked up and back 25 degrees, a light grip tang, a grip
 * tilted 25.6 degrees back with ridge stripes, and a small tilted trigger with no guard. The layout lives in
 * {@code tools/asset-gen/gun-paint/alien_blaster.layout.json}; {@code gun_paint.py pack} writes the block below and
 * {@code gun_paint.py paint} paints the 128x64 sheet from {@code gun-paint/alien_blaster.json}. No camo variants.
 *
 * <h2>Glow parts</h2>
 * The boxes named {@code cell_glow} and {@code stud_glow_*} (flat blue, #0055FF) are the parts a renderer may draw
 * fullbright; the notes say the original does not, so lighting them like the rest is equally valid.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels. Y = 0 and Z = 0 are the long axis of the diamond bars,
 * X = 0 is the middle of the first stepped ring (the notes' F). One texel is a sixteenth of a block. A bar of side s
 * turned 45 degrees is built as an s x s box centered on the axis with a {@code tiltX} of 45 about that axis. The
 * notes say the model has no moving parts, so the whole gun is one part.
 *
 * <p>
 * Spec: rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class AlienBlasterProfile {

    /** Texture resource paths of the model sheet: only the default, there are no camo variants. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/alien_blaster.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: the muzzle block's front (texel 18) on the barrel axis. */
    public static final float[] MUZZLE = { 18.0f / 16.0f, 0.0f, 0.0f };
    /** Frames of {@code muzzleflash_blue.png} that are painted. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("body", 0, 0, 0, 12, 6, 6, -13f, -3f, -3f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("step1", 0, 64, 0, 2, 5, 5, -1f, -2.5f, -2.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("step2", 0, 78, 0, 2, 4, 4, 1f, -2f, -2f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("step3", 0, 90, 6, 1, 3, 3, 3f, -1.5f, -1.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("step4", 0, 122, 0, 1, 2, 2, 4f, -1f, -1f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("barrel", 0, 0, 12, 12, 1, 1, 5f, -0.5f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("collar_a", 0, 106, 0, 1, 3, 3, 12f, -1.5f, -1.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("collar_b", 0, 114, 0, 1, 3, 3, 14f, -1.5f, -1.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("muzzle", 0, 98, 10, 2, 2, 2, 16f, -1f, -1f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("rear_ring", 0, 50, 0, 2, 5, 5, -15f, -2.5f, -2.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("cell_glow", 0, 118, 6, 2, 2, 2, -17f, -1f, -1f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("nub", 0, 122, 4, 1, 1, 1, -18f, -0.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("stud_glow_a1", 0, 78, 8, 1, 1, 1, -5f, -3.5f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("stud_glow_b1", 0, 86, 10, 1, 1, 1, -5f, -0.5f, 2.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("stud_glow_a2", 0, 82, 8, 1, 1, 1, -7f, -3.5f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("stud_glow_b2", 0, 112, 10, 1, 1, 1, -7f, -0.5f, 2.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("stud_glow_a3", 0, 86, 8, 1, 1, 1, -9f, -3.5f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("stud_glow_b3", 0, 116, 10, 1, 1, 1, -9f, -0.5f, 2.5f, new float[] { 0f, 0f, 0f }, 0f, 45f),
        new BoxSpec("wing_l", 0, 98, 6, 7, 1, 3, -11f, -0.5f, -3.5f, new float[] { -4f, 0f, -3.5f }, 0f, 0f, -25f),
        new BoxSpec("wing_r", 0, 50, 10, 7, 1, 3, -11f, -0.5f, 0.5f, new float[] { -4f, 0f, 3.5f }, 0f, 0f, 25f),
        new BoxSpec("fin", 0, 70, 10, 7, 3, 1, -11f, -3.5f, -0.5f, new float[] { -4f, -3.5f, 0f }, 25f),
        new BoxSpec("tang", 0, 90, 0, 5, 3, 3, -10f, 3f, -1.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("grip", 0, 36, 0, 4, 9, 3, -10f, 5.5f, -1.5f, new float[] { -6f, 5.5f, 0f }, 25.6f),
        new BoxSpec("trigger", 0, 106, 10, 2, 3, 1, -6f, 4f, -0.5f, new float[] { -4f, 4f, 0f }, 15f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.4 and an idle shift measured from the origin of the original
    // model. The scale, idle position and third-person placement are solved by InHandFit from the per-gun targets of
    // the in-hand notes (section 7), so the gun lands where Techguns' sits on screen.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("alien_blaster", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, 0 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] CENTER = FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE);
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Alien Blaster. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "alien_blaster",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private AlienBlasterProfile() {}

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
        // Firing: a small pitch (the notes: up to 5 degrees) and a backward kick along the axis (up to 0.15 block).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 5.0 * yF(p))
            .translate((p, part) -> FP_X + 0.15 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: up to 50 degrees of pitch at mid-animation (the notes), rising and falling smoothly.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 50.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225 (the notes' yaw 135 in the Z-family, turned to the X-family), then the scale
        // and translate InHandFit solved so the box centre lands at the hand and the blaster is the notes' 0.68 world
        // blocks long. Aimed extra (0, 0.1, 0) as in the notes.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: about 25 degrees of pitch in the first quarter of the first half, about the hand (the notes).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0, 0, b -> b.rotZ((p, part) -> -25.0 * kick(p)))
            .build();
        // Reload: 50 degrees of pitch at the middle of the animation, about the same pivot.
        TransformProgram tpReload = TransformProgram.builder()
            .about(0, 0, 0, b -> b.rotZ((p, part) -> -50.0 * yR(p)))
            .build();
        // Ground: the model centered so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.5)
            .translate(-CENTER[0], -CENTER[1], 0)
            .build();
        // Icon: side-on, scaled so the 2.25 block model fits the slot, centered in it.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.4)
            .translate(-CENTER[0], -CENTER[1], 0)
            .build();
        // Deviation: the first-person flash is a fixed-frame screen effect, so its offset follows the gun's solved
        // placement (InHandFit.Placement.flashOffset). Headless work cannot look at the screen, so this is unverified
        // in game. Additive because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
        double[] fo = FIT.flashOffset(MUZZLE, 1.0);
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash_blue.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            fo[0],
            fo[1],
            fo[2],
            1.0,
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
